package org.alloy.services;

import org.alloy.controllers.DeviceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class WeldingDeviceServer {
    
    @Value("${welding.device.port:3000}")
    private int port;

    @Value("${welding.device.macs:8CAAB50C4254,E09806083396}")
    private String macsConfig;
    
    private volatile boolean running = true;
    private Thread serverThread;
    private Thread heartbeatThread;
    private ServerSocket serverSocket;
    private volatile Socket currentClientSocket = null;
    
    @Autowired
    private WeldingDeviceManagerService deviceManager;
    
    @Autowired
    private DeviceController deviceController;

    @PostConstruct
    public void start() {
        System.out.println("[WELDING-SERVER] 🚀 Запуск TCP сервера для Блока мониторинга ОГК");
        System.out.println("[WELDING-SERVER] Порт: " + port);
        System.out.println("[WELDING-SERVER] Разрешенные MAC: " + macsConfig);
        
        serverThread = new Thread(this::runServer);
        serverThread.setDaemon(true);
        serverThread.start();
        
        // Запускаем поток для отправки периодических команд
        startHeartbeatThread();
    }

    private void runServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[WELDING-SERVER] ✅ TCP сервер запущен на порту " + port);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("[WELDING-SERVER] 🔌 Подключение от: " + clientIp);
                    
                    // Сохраняем ссылку на клиентский сокет
                    currentClientSocket = clientSocket;
                    
                    // Блок мониторинга сам отправляет данные, мы ничего не отправляем
                    
                    // Обрабатываем подключение в отдельном потоке
                    new Thread(() -> handleClient(clientSocket)).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[WELDING-SERVER] ❌ Ошибка принятия подключения: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[WELDING-SERVER] ❌ Ошибка запуска сервера: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.US_ASCII))) {
            
            String line;
            System.out.println("[WELDING-SERVER] 🔄 Ожидание данных от " + clientIp + "...");
            while (running && (line = in.readLine()) != null) {
                System.out.println("[WELDING-SERVER] 📨 Получены данные от " + clientIp + ": " + line);
                
                // Извлечение MAC-адреса из пакета
                String mac = extractMacFromPacket(line);
                if (mac != null) {
                    System.out.println("[WELDING-SERVER] MAC из пакета: " + mac);
                    
                    // Проверяем, что MAC разрешен
                    if (isAllowedMac(mac)) {
                        // Пропускаем ping сообщения, логируем только полезные данные
                        if (!line.startsWith("PING:")) {
                            String source = mac.equalsIgnoreCase("E09806083396") ? "Core" : "Блока мониторинга ОГК";
                            System.out.println("[WELDING-SERVER] ✅ Данные от " + source + " (" + mac + "): " + line);
                            deviceManager.processDeviceData(line, mac);
                        }
                    } else {
                        System.out.println("[WELDING-SERVER] ⚠️ Неизвестный MAC: " + mac + " (разрешены: " + macsConfig + ")");
                    }
                }
            }
            System.out.println("[WELDING-SERVER] 🔌 Клиент " + clientIp + " закрыл соединение");
        } catch (IOException e) {
            System.err.println("[WELDING-SERVER] ❌ Ошибка обработки клиента " + clientIp + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("[WELDING-SERVER] 🔌 Соединение с " + clientIp + " закрыто");
            } catch (IOException e) {
                System.err.println("[WELDING-SERVER] ❌ Ошибка закрытия соединения: " + e.getMessage());
            }
                }
    }
    
    private void startHeartbeatThread() {
        // Блок мониторинга сам отправляет данные, heartbeat не нужен
        System.out.println("[WELDING-SERVER] ℹ️ Heartbeat отключен - блок мониторинга сам отправляет данные");
    }

    private String extractMacFromPacket(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        // Ищем MAC в различных форматах
        int colonPos = data.indexOf(':');
        int semicolonPos = data.indexOf(';');
        
        if (colonPos >= 0 && semicolonPos > colonPos) {
            // Формат :MAC;data (например, :8CAAB50C4254;data)
            String mac = data.substring(colonPos + 1, semicolonPos);
            if (mac.length() == 12 && mac.matches("[0-9A-Fa-f]{12}")) {
                return mac.toUpperCase();
            }
        }
        
        if (colonPos >= 0) {
            // Проверяем формат COMMAND:MAC (например, HELLO:8CAAB50C4254)
            String afterColon = data.substring(colonPos + 1);
            if (afterColon.length() >= 12) {
                String possibleMac = afterColon.substring(0, 12);
                if (possibleMac.matches("[0-9A-Fa-f]{12}")) {
                    return possibleMac.toUpperCase();
                }
            }
            
            // Проверяем формат MAC:data (например, 8CAAB50C4254:data)
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

    @PreDestroy
    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("[WELDING-SERVER] 🔚 TCP сервер остановлен");
            } catch (IOException e) {
                System.err.println("[WELDING-SERVER] ❌ Ошибка остановки сервера: " + e.getMessage());
            }
        }
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
    }
} 