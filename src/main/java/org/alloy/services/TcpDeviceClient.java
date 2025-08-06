package org.alloy.services;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// @Service
public class TcpDeviceClient {
    private final String host = "95.172.58.219";
    private final int port = 3000;
    private volatile boolean running = true;
    private Thread clientThread;

    @PostConstruct
    public void start() {
        clientThread = new Thread(() -> {
            while (running) {
                try (Socket socket = new Socket(host, port);
                     InputStream in = socket.getInputStream()) {

                    System.out.println("[TCP-CLIENT] Connected to device " + host + ":" + port);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while (running && (bytesRead = in.read(buffer)) != -1) {
                        String data = new String(buffer, 0, bytesRead, StandardCharsets.US_ASCII);
                        System.out.println("[TCP-CLIENT] Received: " + data);

                        // Извлечение MAC-адреса из пакета
                        String mac = "";
                        int pos1 = data.indexOf(':');
                        if (pos1 >= 0) {
                            int pos2 = data.indexOf(';', pos1);
                            if (pos2 > 0 && pos2 - pos1 == 13) {
                                mac = data.substring(pos1 + 1, pos1 + 13);
                                System.out.println("[TCP-CLIENT] MAC from packet: " + mac);
                                
                                // Проверяем, что это наша плата
                                if ("8CAAB579425A".equals(mac)) {
                                    System.out.println("[TCP-CLIENT] ✅ Данные от нашей платы: " + data);
                                    // Здесь можно добавить обработку данных сварочного аппарата
                                    processWeldingData(data);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[TCP-CLIENT] Error: " + e.getMessage());
                    try {
                        Thread.sleep(5000); // Пауза перед повторным подключением
                    } catch (InterruptedException ignored) {}
                }
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();
    }

    private void processWeldingData(String data) {
        try {
            // Парсим данные сварочного аппарата
            // Формат данных может быть: MAC:PARAM1=VAL1;PARAM2=VAL2;...
            String[] parts = data.split(";");
            for (String part : parts) {
                if (part.contains("=")) {
                    String[] keyValue = part.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        System.out.println("[TCP-CLIENT] Параметр: " + key + " = " + value);
                        
                        // Здесь можно добавить сохранение в базу данных
                        // или отправку через WebSocket на фронтенд
                    }
                }
            }
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
        System.out.println("[TCP-CLIENT] Stopped.");
    }
}
