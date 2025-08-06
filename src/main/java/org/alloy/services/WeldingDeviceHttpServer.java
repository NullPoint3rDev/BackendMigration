package org.alloy.services;

import org.alloy.controllers.DeviceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// @Service
public class WeldingDeviceHttpServer {
    private static final int HTTP_PORT = 3000;
    private static final String EXPECTED_MAC = "8CAAB579425A";
    
    private volatile boolean running = true;
    private Thread serverThread;
    private ServerSocket serverSocket;
    
    private final DeviceController deviceController;

    @Autowired
    public WeldingDeviceHttpServer(DeviceController deviceController) {
        this.deviceController = deviceController;
    }

    @PostConstruct
    public void start() {
        System.out.println("[WELDING-HTTP] Запуск HTTP сервера для сварочного аппарата");
        System.out.println("[WELDING-HTTP] Порт: " + HTTP_PORT);
        System.out.println("[WELDING-HTTP] Ожидаемый MAC: " + EXPECTED_MAC);
        
        serverThread = new Thread(this::runServer);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void runServer() {
        try {
            serverSocket = new ServerSocket(HTTP_PORT);
            System.out.println("[WELDING-HTTP] ✅ HTTP сервер запущен на порту " + HTTP_PORT);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("[WELDING-HTTP] Подключение от: " + clientIp);
                    
                    // Обрабатываем запрос в отдельном потоке
                    new Thread(() -> handleClient(clientSocket)).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[WELDING-HTTP] Ошибка принятия подключения: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[WELDING-HTTP] Ошибка запуска сервера: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            // Читаем HTTP заголовки
            String line;
            StringBuilder request = new StringBuilder();
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                request.append(line).append("\n");
            }
            
            // Читаем тело запроса (если есть)
            StringBuilder body = new StringBuilder();
            while (in.ready() && (line = in.readLine()) != null) {
                body.append(line).append("\n");
            }
            
            String fullRequest = request.toString() + body.toString();
            System.out.println("[WELDING-HTTP] Получен запрос от " + clientIp + ":\n" + fullRequest);
            
            // Проверяем, что это наша плата
            if (clientIp.equals("95.172.58.219") || fullRequest.contains("8CAAB579425A")) {
                System.out.println("[WELDING-HTTP] ✅ Это наша плата сварочного аппарата!");
                // Обрабатываем данные
                processDeviceData(fullRequest, clientIp);
            } else {
                System.out.println("[WELDING-HTTP] ⚠️ Неизвестное устройство: " + clientIp);
                System.out.println("[WELDING-HTTP] Ожидаемый IP: 95.172.58.219");
                System.out.println("[WELDING-HTTP] Ожидаемый MAC: 8CAAB579425A");
            }
            
            // Отправляем HTTP ответ
            String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: Content-Type\r\n" +
                            "Content-Length: 2\r\n" +
                            "\r\n" +
                            "OK";
            
            out.print(response);
            out.flush();
            
        } catch (IOException e) {
            System.err.println("[WELDING-HTTP] Ошибка обработки клиента " + clientIp + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private void processDeviceData(String data, String clientIp) {
        try {
            System.out.println("[WELDING-HTTP] Обработка данных от " + clientIp);
            
            // Ищем MAC-адрес в данных
            if (data.contains(EXPECTED_MAC)) {
                System.out.println("[WELDING-HTTP] ✅ Данные от нашей платы");
                
                // Извлекаем данные сварочного аппарата
                String[] lines = data.split("\n");
                for (String line : lines) {
                    if (line.contains("=") && line.contains(";")) {
                        // Формат: PARAM1=VAL1;PARAM2=VAL2
                        String[] parts = line.split(";");
                        StringBuilder parsedData = new StringBuilder();
                        
                        for (String part : parts) {
                            if (part.contains("=")) {
                                String[] keyValue = part.split("=");
                                if (keyValue.length == 2) {
                                    String key = keyValue[0].trim();
                                    String value = keyValue[1].trim();
                                    parsedData.append(key).append(":").append(value).append(";");
                                    System.out.println("[WELDING-HTTP] Параметр: " + key + " = " + value);
                                }
                            }
                        }
                        
                        // Отправляем данные на фронтенд
                        if (parsedData.length() > 0) {
                            String enrichedData = EXPECTED_MAC + ":" + parsedData.toString();
                            deviceController.sendDeviceData(enrichedData);
                        }
                    }
                }
            } else {
                System.out.println("[WELDING-HTTP] ⚠️ Неизвестное устройство или формат данных");
                System.out.println("[WELDING-HTTP] Данные: " + data.substring(0, Math.min(data.length(), 200)) + "...");
            }
        } catch (Exception e) {
            System.err.println("[WELDING-HTTP] Ошибка обработки данных: " + e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
        System.out.println("[WELDING-HTTP] HTTP сервер остановлен");
    }
} 