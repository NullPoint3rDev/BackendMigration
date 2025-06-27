package org.alloy.services;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class TcpDeviceClient {
    private final String host = "89.109.8.59";
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
                            }
                        }
                        // Здесь можно добавить обработку данных
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

    @PreDestroy
    public void stop() {
        running = false;
        if (clientThread != null) {
            clientThread.interrupt();
        }
        System.out.println("[TCP-CLIENT] Stopped.");
    }
}
