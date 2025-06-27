package org.alloy.services;

import org.alloy.controllers.DeviceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Service
public class DeviceTcpClientService {
    private static final String DEVICE_IP = "89.109.8.59";
    private static final int DEVICE_PORT = 3000;

    private final DeviceController deviceController;

    @Autowired
    public DeviceTcpClientService(DeviceController deviceController) {
        this.deviceController = deviceController;
    }

    @PostConstruct
    public void start() {
        new Thread(this::runClient).start();
    }

    private void runClient() {
        while (true) {
            try (Socket socket = new Socket(DEVICE_IP, DEVICE_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    deviceController.sendDeviceData(line);
                }
            } catch (Exception e) {
                // Логируем ошибку и пробуем переподключиться через 5 секунд
                System.err.println("Ошибка TCP-клиента: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
