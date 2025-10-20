package org.alloy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class TcpCoreDeviceClient {
    
    private static final Logger log = LoggerFactory.getLogger(TcpCoreDeviceClient.class);
    
    @Value("${welding.core.host:192.168.10.137}")
    private String host;
    
    @Value("${welding.core.port:3000}")
    private int port;
    
    @Value("${welding.core.mac:E09806083396}")
    private String coreMac;
    
    @Value("${welding.core.connection.timeout_ms:10000}")
    private int timeoutMs;
    
    @Value("${welding.core.connection.retry_interval_ms:5000}")
    private int retryIntervalMs;
    
    @Value("${welding.core.connection.max_retries:5}")
    private int maxRetries;
    
    private volatile boolean running = true;
    private Thread clientThread;
    private Thread heartbeatThread;
    private int retryCount = 0;
    private volatile long lastDataReceived = 0;
    private volatile boolean isConnected = false;

    @Autowired
    private WeldingDeviceManagerService deviceManager;

    // @PostConstruct - отключен, Core устройства подключаются напрямую к TCP серверу
    public void start() {
        System.out.println("[TCP-CORE] 🚀 Запуск TCP клиента для Core");
        System.out.println("[TCP-CORE] Хост: " + host + ":" + port);
        System.out.println("[TCP-CORE] Ожидаемый MAC: " + coreMac);
        System.out.println("[TCP-CORE] Таймаут: " + timeoutMs + "мс");
        
        clientThread = new Thread(() -> {
            while (running) {
                Socket socket = null;
                try {
                    System.out.println("[TCP-CORE] 🔌 Попытка подключения к " + host + ":" + port + " (попытка " + (retryCount + 1) + ")");
                    
                    socket = new Socket();
                    socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
                    
                    System.out.println("[TCP-CORE] ✅ Подключение установлено к " + host + ":" + port);
                    isConnected = true;
                    lastDataReceived = System.currentTimeMillis();
                    
                    try (InputStream in = socket.getInputStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        
                        while (running && (bytesRead = in.read(buffer)) != -1) {
                            lastDataReceived = System.currentTimeMillis();
                            String data = new String(buffer, 0, bytesRead, StandardCharsets.US_ASCII);
                            
                            // Убрали логирование для ускорения
                            
                            String mac = extractMacFromPacket(data);
                            if (mac != null) {
                                if (coreMac.equalsIgnoreCase(mac)) {
                                    if (!data.startsWith("PING:")) {
                                        processWeldingData(data, mac);
                                    }
                                }
                            }
                        }
                        
                        System.out.println("[TCP-CORE] 🔌 Соединение закрыто устройством");
                        deviceManager.markDeviceDisconnected(coreMac);
                    }
                    
                } catch (java.net.ConnectException e) {
                    retryCount++;
                    System.err.println("[TCP-CORE] ❌ Ошибка подключения: " + e.getMessage());
                    log.error("[TCP-CORE] Ошибка подключения", e);
                    System.err.println("[TCP-CORE] 🔄 Повторная попытка через " + retryIntervalMs + "мс (попытка " + retryCount + "/" + maxRetries + ")");
                    deviceManager.markDeviceDisconnected(coreMac);
                    if (retryCount >= maxRetries) {
                        System.err.println("[TCP-CORE] ⚠️ Достигнуто максимальное количество попыток. Останавливаемся.");
                        break;
                    }
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ignored) {}
                } catch (Exception e) {
                    System.err.println("[TCP-CORE] ❌ Ошибка: " + e.getMessage());
                    log.error("[TCP-CORE] Ошибка", e);
                    deviceManager.markDeviceDisconnected(coreMac);
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ignored) {}
                } finally {
                    if (socket != null) {
                        try { socket.close(); } catch (Exception ignore) {}
                    }
                }
            }
            System.out.println("[TCP-CORE] 🔚 TCP клиент остановлен");
        });
        clientThread.setDaemon(true);
        clientThread.start();
        
        startHeartbeatThread();
    }
    
    private void startHeartbeatThread() {
        heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(10000);
                    if (isConnected && lastDataReceived > 0) {
                        long timeSinceLastData = System.currentTimeMillis() - lastDataReceived;
                        if (timeSinceLastData > 30000) {
                            System.out.println("[TCP-CORE] ⚠️ Данных не было " + (timeSinceLastData / 1000) + " секунд, отмечаем как отключенный");
                            deviceManager.markDeviceDisconnected(coreMac);
                            isConnected = false;
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private String extractMacFromPacket(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        int colonPos = data.indexOf(':');
        int semicolonPos = data.indexOf(';');
        if (colonPos >= 0 && semicolonPos > colonPos) {
            String mac = data.substring(colonPos + 1, semicolonPos);
            if (mac.length() == 12 && mac.matches("[0-9A-Fa-f]{12}")) {
                return mac.toUpperCase();
            }
        }
        if (colonPos >= 0) {
            String afterColon = data.substring(colonPos + 1);
            if (afterColon.length() >= 12) {
                String possibleMac = afterColon.substring(0, 12);
                if (possibleMac.matches("[0-9A-Fa-f]{12}")) {
                    return possibleMac.toUpperCase();
                }
            }
            String beforeColon = data.substring(0, colonPos);
            if (beforeColon.length() == 12 && beforeColon.matches("[0-9A-Fa-f]{12}")) {
                return beforeColon.toUpperCase();
            }
        }
        return null;
    }

    private void processWeldingData(String data, String mac) {
        try {
            deviceManager.processDeviceData(data, mac);
        } catch (Exception e) {
            // Молча игнорируем ошибки
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (clientThread != null) {
            clientThread.interrupt();
        }
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
        System.out.println("[TCP-CORE] Stopped.");
    }
}
