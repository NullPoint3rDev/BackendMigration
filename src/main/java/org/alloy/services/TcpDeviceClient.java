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
public class TcpDeviceClient {
    
    private static final Logger log = LoggerFactory.getLogger(TcpDeviceClient.class);
    
    @Value("${welding.device.host:95.172.58.219}")
    private String host;
    
    @Value("${welding.device.port:3000}")
    private int port;
    
    @Value("${welding.device.macs:8CAAB50C4254}")
    private String macsConfig;
    
    @Value("${welding.connection.timeout_ms:10000}")
    private int timeoutMs;
    
    @Value("${welding.connection.retry_interval_ms:5000}")
    private int retryIntervalMs;
    
    @Value("${welding.connection.max_retries:5}")
    private int maxRetries;
    
    private volatile boolean running = true;
    private Thread clientThread;
    private Thread heartbeatThread;
    private int retryCount = 0;
    private volatile long lastDataReceived = 0;
    private volatile boolean isConnected = false;
    private volatile String lastSeenMac = null;

    @Autowired
    private WeldingDeviceManagerService deviceManager;

    @PostConstruct
    public void start() {
        System.out.println("[TCP-CLIENT] 🚀 Запуск TCP клиента для Блока мониторинга ОГК");
        System.out.println("[TCP-CLIENT] Хост: " + host + ":" + port);
        System.out.println("[TCP-CLIENT] Разрешенные MAC: " + macsConfig);
        System.out.println("[TCP-CLIENT] Таймаут: " + timeoutMs + "мс");
        
        clientThread = new Thread(() -> {
            while (running) {
                Socket socket = null;
                try {
                    System.out.println("[TCP-CLIENT] 🔌 Попытка подключения к " + host + ":" + port + " (попытка " + (retryCount + 1) + ")");
                    
                    socket = new Socket();
                    // socket.setSoTimeout(timeoutMs);
                    socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
                    
                    System.out.println("[TCP-CLIENT] ✅ Подключение установлено к " + host + ":" + port);
                    System.out.println("[TCP-CLIENT] 🔄 Ожидание данных от сварочного аппарата...");
                    System.out.println("[TCP-CLIENT] 📡 Локальный адрес: " + socket.getLocalAddress() + ":" + socket.getLocalPort());
                    System.out.println("[TCP-CLIENT] 📡 Удаленный адрес: " + socket.getInetAddress() + ":" + socket.getPort());
                    retryCount = 0;
                    isConnected = true;
                    lastDataReceived = System.currentTimeMillis();
                    lastSeenMac = null;
                    
                    try (InputStream in = socket.getInputStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        
                        while (running && (bytesRead = in.read(buffer)) != -1) {
                            lastDataReceived = System.currentTimeMillis();
                            String data = new String(buffer, 0, bytesRead, StandardCharsets.US_ASCII);
                            
                            System.out.println("[TCP-CLIENT] 📨 Получены сырые данные (" + bytesRead + " байт): " + data);
                            
                            String mac = extractMacFromPacket(data);
                            if (mac != null) {
                                System.out.println("[TCP-CLIENT] 🔍 Извлечен MAC: " + mac);
                                if (isAllowedMac(mac)) {
                                    lastSeenMac = mac;
                                    if (!data.startsWith("PING:")) {
                                        String source = mac.equalsIgnoreCase("E09806083396") ? "Core" : "Блок мониторинга ОГК";
                                        String msg = "[TCP-CLIENT] ✅ Данные от " + source + " (" + mac + "): " + data;
                                        System.out.println(msg);
                                        log.info(msg);
                                        processWeldingData(data, mac);
                                    } else {
                                        System.out.println("[TCP-CLIENT] ⏭️ Пропущен ping от Блока мониторинга ОГК");
                                    }
                                } else {
                                    String warn = "[TCP-CLIENT] ⚠️ Неизвестный MAC: " + mac + " (разрешены: " + macsConfig + ")";
                                    System.out.println(warn);
                                    log.warn(warn);
                                }
                            } else {
                                String err = "[TCP-CLIENT] ❌ Не удалось извлечь MAC из данных: " + data;
                                System.out.println(err);
                                log.warn(err);
                            }
                        }
                        
                        System.out.println("[TCP-CLIENT] 🔌 Соединение закрыто аппаратом");
                        if (lastSeenMac != null) {
                            deviceManager.markDeviceDisconnected(lastSeenMac);
                        }
                    }
                    
                } catch (java.net.ConnectException e) {
                    retryCount++;
                    System.err.println("[TCP-CLIENT] ❌ Ошибка подключения: " + e.getMessage());
                    log.error("[TCP-CLIENT] Ошибка подключения", e);
                    System.err.println("[TCP-CLIENT] 🔄 Повторная попытка через " + retryIntervalMs + "мс (попытка " + retryCount + "/" + maxRetries + ")");
                    
                    if (lastSeenMac != null) {
                        deviceManager.markDeviceDisconnected(lastSeenMac);
                    }
                    
                    if (retryCount >= maxRetries) {
                        System.err.println("[TCP-CLIENT] ⚠️ Достигнуто максимальное количество попыток. Переходим в тестовый режим.");
                        break;
                    }
                    
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ignored) {}
                    
                } catch (Exception e) {
                    System.err.println("[TCP-CLIENT] ❌ Ошибка: " + e.getMessage());
                    log.error("[TCP-CLIENT] Ошибка", e);
                    if (lastSeenMac != null) {
                        deviceManager.markDeviceDisconnected(lastSeenMac);
                    }
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ignored) {}
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (Exception ignore) {}
                    }
                }
            }
            
            System.out.println("[TCP-CLIENT] 🔚 TCP клиент остановлен");
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
                            System.out.println("[TCP-CLIENT] ⚠️ Данных не было " + (timeSinceLastData / 1000) + " секунд, отмечаем как отключенный");
                            if (lastSeenMac != null) {
                                deviceManager.markDeviceDisconnected(lastSeenMac);
                            }
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

    private boolean isAllowedMac(String mac) {
        if (mac == null) {
            return false;
        }
        String[] parts = macsConfig.split(",");
        for (String part : parts) {
            if (mac.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private void processWeldingData(String data, String mac) {
        try {
            deviceManager.processDeviceData(data, mac);
            System.out.println("[TCP-CLIENT] ✅ Данные обработаны и сохранены");
            log.info("[TCP-CLIENT] Данные обработаны и сохранены для {}", mac);
        } catch (Exception e) {
            System.err.println("[TCP-CLIENT] Ошибка обработки данных: " + e.getMessage());
            log.error("[TCP-CLIENT] Ошибка обработки данных для " + mac, e);
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
        System.out.println("[TCP-CLIENT] Stopped.");
    }
}
