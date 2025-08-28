package org.alloy.services;

import org.alloy.controllers.DeviceController;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.services.WeldingDataParserService;
import org.alloy.services.WeldingMachineStateService;
import org.alloy.services.WeldingDeviceManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// @Service
public class TcpDeviceClient {
    
    @Value("${welding.device.host:95.172.58.219}")
    private String host;
    
    @Value("${welding.device.port:3000}")
    private int port;
    
    @Value("${welding.device.mac:8CAAB579425A}")
    private String expectedMac;
    
    @Value("${welding.connection.timeout_ms:10000}")
    private int timeoutMs;
    
    @Value("${welding.connection.retry_interval_ms:5000}")
    private int retryIntervalMs;
    
    @Value("${welding.connection.max_retries:3}")
    private int maxRetries;
    
    private volatile boolean running = true;
    private Thread clientThread;
    private Thread heartbeatThread;
    private int retryCount = 0;
    private volatile long lastDataReceived = 0;
    private volatile boolean isConnected = false;

    @Autowired
    private DeviceController deviceController;
    
    @Autowired
    private WeldingDataParserService dataParser;
    
    @Autowired
    private WeldingMachineStateService stateService;
    
    @Autowired
    private WeldingDeviceManagerService deviceManager;

    @PostConstruct
    public void start() {
        System.out.println("[TCP-CLIENT] 🚀 Запуск TCP клиента для сварочного аппарата");
        System.out.println("[TCP-CLIENT] Хост: " + host + ":" + port);
        System.out.println("[TCP-CLIENT] Ожидаемый MAC: " + expectedMac);
        System.out.println("[TCP-CLIENT] Таймаут: " + timeoutMs + "мс");
        
        clientThread = new Thread(() -> {
            while (running) {
                try {
                    System.out.println("[TCP-CLIENT] 🔌 Попытка подключения к " + host + ":" + port + " (попытка " + (retryCount + 1) + ")");
                    
                    Socket socket = new Socket();
                    // Убираем таймаут на чтение - ждем данные бесконечно
                    // socket.setSoTimeout(timeoutMs);
                    socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
                    
                    System.out.println("[TCP-CLIENT] ✅ Подключение установлено к " + host + ":" + port);
                    System.out.println("[TCP-CLIENT] 🔄 Ожидание данных от сварочного аппарата...");
                    System.out.println("[TCP-CLIENT] 📡 Локальный адрес: " + socket.getLocalAddress() + ":" + socket.getLocalPort());
                    System.out.println("[TCP-CLIENT] 📡 Удаленный адрес: " + socket.getInetAddress() + ":" + socket.getPort());
                    retryCount = 0; // Сбрасываем счетчик при успешном подключении
                    isConnected = true;
                    lastDataReceived = System.currentTimeMillis();
                    
                    try (InputStream in = socket.getInputStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long lastDataTime = System.currentTimeMillis();
                        
                        while (running && (bytesRead = in.read(buffer)) != -1) {
                            lastDataTime = System.currentTimeMillis();
                            lastDataReceived = System.currentTimeMillis();
                            String data = new String(buffer, 0, bytesRead, StandardCharsets.US_ASCII);
                            System.out.println("[TCP-CLIENT] 📨 Получены данные: " + data);
                            System.out.println("[TCP-CLIENT] 📊 Размер данных: " + bytesRead + " байт");
                            System.out.println("[TCP-CLIENT] 📊 Сырые байты: " + java.util.Arrays.toString(java.util.Arrays.copyOfRange(buffer, 0, Math.min(bytesRead, 20))));

                            // Извлечение MAC-адреса из пакета
                            System.out.println("[TCP-CLIENT] 🔍 Поиск MAC в данных: " + data);
                            String mac = extractMacFromPacket(data);
                            if (mac != null) {
                                System.out.println("[TCP-CLIENT] MAC из пакета: " + mac);
                                
                                // Проверяем, что это наша плата
                                if (expectedMac.equals(mac)) {
                                    System.out.println("[TCP-CLIENT] ✅ Данные от нашей платы: " + data);
                                    processWeldingData(data, mac);
                                } else {
                                    System.out.println("[TCP-CLIENT] ⚠️ Неизвестный MAC: " + mac + " (ожидался: " + expectedMac + ")");
                                }
                            }
                        }
                        
                        // Если вышли из цикла чтения, значит соединение закрыто
                        System.out.println("[TCP-CLIENT] 🔌 Соединение закрыто аппаратом");
                        deviceManager.markDeviceDisconnected(expectedMac);
                    }
                    
                } catch (java.net.ConnectException e) {
                    retryCount++;
                    System.err.println("[TCP-CLIENT] ❌ Ошибка подключения: " + e.getMessage());
                    System.err.println("[TCP-CLIENT] 🔄 Повторная попытка через " + retryIntervalMs + "мс (попытка " + retryCount + "/" + maxRetries + ")");
                    
                    // Отмечаем аппарат как отключенный
                    deviceManager.markDeviceDisconnected(expectedMac);
                    
                    if (retryCount >= maxRetries) {
                        System.err.println("[TCP-CLIENT] ⚠️ Достигнуто максимальное количество попыток. Переходим в тестовый режим.");
                        break;
                    }
                    
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ignored) {}
                    
                } catch (Exception e) {
                    System.err.println("[TCP-CLIENT] ❌ Ошибка: " + e.getMessage());
                    deviceManager.markDeviceDisconnected(expectedMac);
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ignored) {}
                }
            }
            
            System.out.println("[TCP-CLIENT] 🔚 TCP клиент остановлен");
        });
        clientThread.setDaemon(true);
        clientThread.start();
        
        // Запускаем поток для проверки состояния соединения
        startHeartbeatThread();
    }
    
    private void startHeartbeatThread() {
        heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(10000); // Проверяем каждые 10 секунд
                    
                    if (isConnected && lastDataReceived > 0) {
                        long timeSinceLastData = System.currentTimeMillis() - lastDataReceived;
                        
                        // Если данных не было более 30 секунд, считаем аппарат отключенным
                        if (timeSinceLastData > 30000) {
                            System.out.println("[TCP-CLIENT] ⚠️ Данных не было " + (timeSinceLastData / 1000) + " секунд, отмечаем как отключенный");
                            deviceManager.markDeviceDisconnected(expectedMac);
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
        
        int pos1 = data.indexOf(':');
        if (pos1 >= 0) {
            int pos2 = data.indexOf(';', pos1);
            if (pos2 > 0 && pos2 - pos1 == 13) {
                return data.substring(pos1 + 1, pos1 + 13);
            }
        }
        return null;
    }

    private void processWeldingData(String data, String mac) {
        try {
            // Используем менеджер для обработки данных
            deviceManager.processDeviceData(data, mac);
            System.out.println("[TCP-CLIENT] ✅ Данные обработаны и сохранены");
        } catch (Exception e) {
            System.err.println("[TCP-CLIENT] Ошибка обработки данных: " + e.getMessage());
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
