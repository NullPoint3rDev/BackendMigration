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
    
    @Value("${welding.device.mac:8CAAB579425A}")
    private String expectedMac;
    
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
        System.out.println("[WELDING-SERVER] 🚀 Запуск TCP сервера для сварочного аппарата");
        System.out.println("[WELDING-SERVER] Порт: " + port);
        System.out.println("[WELDING-SERVER] Ожидаемый MAC: " + expectedMac);
        
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
                    
                    // Отправляем приветственную команду плате
                    try {
                        java.io.PrintWriter out = new java.io.PrintWriter(clientSocket.getOutputStream(), true);
                        String welcomeCommand = "HELLO:8CAAB579425A\n";
                        out.println(welcomeCommand);
                        System.out.println("[WELDING-SERVER] 📤 Отправлена команда: " + welcomeCommand.trim());
                    } catch (Exception e) {
                        System.err.println("[WELDING-SERVER] ❌ Ошибка отправки команды: " + e.getMessage());
                    }
                    
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
                    
                    // Проверяем, что это наша плата
                    if (expectedMac.equals(mac)) {
                        System.out.println("[WELDING-SERVER] ✅ Данные от нашей платы: " + line);
                        deviceManager.processDeviceData(line, mac);
                    } else {
                        System.out.println("[WELDING-SERVER] ⚠️ Неизвестный MAC: " + mac + " (ожидался: " + expectedMac + ")");
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
        heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(10000); // Отправляем команду каждые 10 секунд
                    
                    if (currentClientSocket != null && !currentClientSocket.isClosed()) {
                        try {
                            java.io.PrintWriter out = new java.io.PrintWriter(currentClientSocket.getOutputStream(), true);
                            String heartbeatCommand = "PING:8CAAB579425A\n";
                            out.println(heartbeatCommand);
                            System.out.println("[WELDING-SERVER] 📤 Отправлен ping: " + heartbeatCommand.trim());
                        } catch (Exception e) {
                            System.err.println("[WELDING-SERVER] ❌ Ошибка отправки ping: " + e.getMessage());
                            currentClientSocket = null;
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