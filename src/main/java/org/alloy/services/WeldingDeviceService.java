package org.alloy.services;

import org.alloy.controllers.DeviceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// @Service
public class WeldingDeviceService {
    private static final String DEVICE_IP = "89.109.8.59";
    private static final int DEVICE_PORT = 3003;
    private static final String EXPECTED_MAC = "8CAAB579425A";

    private volatile boolean running = true;
    private Thread clientThread;
    
    private final DeviceController deviceController;

    @Autowired
    public WeldingDeviceService(DeviceController deviceController) {
        this.deviceController = deviceController;
    }

    @PostConstruct
    public void start() {
        System.out.println("[WELDING-DEVICE] Запуск сервиса для сварочного аппарата");
        System.out.println("[WELDING-DEVICE] Подключение к: " + DEVICE_IP + ":" + DEVICE_PORT);
        System.out.println("[WELDING-DEVICE] Ожидаемый MAC: " + EXPECTED_MAC);
        
        clientThread = new Thread(this::runClient);
        clientThread.setDaemon(true);
        clientThread.start();
    }

    private void runClient() {
        while (running) {
            try (Socket socket = new Socket(DEVICE_IP, DEVICE_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("[WELDING-DEVICE] ✅ Подключено к сварочному аппарату");
                
                String line;
                while (running && (line = in.readLine()) != null) {
                    processDeviceData(line);
                }
            } catch (Exception e) {
                System.err.println("[WELDING-DEVICE] ❌ Ошибка подключения: " + e.getMessage());
                try {
                    Thread.sleep(5000); // Пауза перед повторным подключением
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void processDeviceData(String data) {
        try {
            System.out.println("[WELDING-DEVICE] Получены данные: " + data);
            
            // Проверяем MAC-адрес
            if (data.contains(EXPECTED_MAC)) {
                System.out.println("[WELDING-DEVICE] ✅ Данные от нашей платы");
                
                // Парсим данные сварочного аппарата
                String[] parts = data.split(";");
                StringBuilder parsedData = new StringBuilder();
                
                for (String part : parts) {
                    if (part.contains("=")) {
                        String[] keyValue = part.split("=");
                        if (keyValue.length == 2) {
                            String key = keyValue[0];
                            String value = keyValue[1];
                            parsedData.append(key).append(":").append(value).append(";");
                            System.out.println("[WELDING-DEVICE] Параметр: " + key + " = " + value);
                        }
                    }
                }
                
                // Отправляем данные на фронтенд
                if (parsedData.length() > 0) {
                    String enrichedData = EXPECTED_MAC + ":" + parsedData.toString();
                    deviceController.sendDeviceData(enrichedData);
                }
            } else {
                System.out.println("[WELDING-DEVICE] ⚠️ Неизвестное устройство: " + data);
            }
        } catch (Exception e) {
            System.err.println("[WELDING-DEVICE] Ошибка обработки данных: " + e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (clientThread != null) {
            clientThread.interrupt();
        }
        System.out.println("[WELDING-DEVICE] Сервис остановлен");
    }
} 