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

@Service
public class TcpDeviceClient {
    
    @Value("${welding.device.host:95.172.58.219}")
    private String host;
    
    @Value("${welding.device.port:3000}")
    private int port;
    
    @Value("${welding.device.mac:8CAAB579425A}")
    private String expectedMac;
    
    private volatile boolean running = true;
    private Thread clientThread;

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
                        String mac = extractMacFromPacket(data);
                        if (mac != null) {
                            System.out.println("[TCP-CLIENT] MAC from packet: " + mac);
                            
                            // Проверяем, что это наша плата
                            if (expectedMac.equals(mac)) {
                                System.out.println("[TCP-CLIENT] ✅ Данные от нашей платы: " + data);
                                processWeldingData(data, mac);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[TCP-CLIENT] Error: " + e.getMessage());
                    // Отмечаем аппарат как отключенный
                    deviceManager.markDeviceDisconnected(expectedMac);
                    try {
                        Thread.sleep(5000); // Пауза перед повторным подключением
                    } catch (InterruptedException ignored) {}
                }
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();
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
        System.out.println("[TCP-CLIENT] Stopped.");
    }
}
