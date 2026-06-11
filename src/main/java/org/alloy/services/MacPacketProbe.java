package org.alloy.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Автономный TCP-зонд на том же порту, что и archive-листенер (3003).
 * Включить: mac.probe.enabled=true — на время теста основной листенер отключается.
 */
@Component
@ConditionalOnProperty(name = "mac.probe.enabled", havingValue = "true")
public class MacPacketProbe {

    private static final String MAC = "E072A1D43F18";

    @Value("${welding.archive.server.port:3003}")
    private int port;

    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private Thread listenerThread;

    @PostConstruct
    void start() {
        listenerThread = new Thread(this::listen, "MacPacketProbe");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listen() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[MAC-PROBE] Слушаю порт " + port + ", жду посылки от " + MAC);
            while (running) {
                Socket client = serverSocket.accept();
                String ip = client.getInetAddress().getHostAddress();
                new Thread(() -> handleClient(client, ip), "MacPacketProbe-" + ip).start();
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[MAC-PROBE] Ошибка: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket client, String ip) {
        try (InputStream in = client.getInputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) {
                String data = new String(buffer, 0, read, StandardCharsets.US_ASCII);
                if (data.toUpperCase().contains(MAC)) {
                    System.out.println("[MAC-PROBE] " + ip + " -> " + data);
                } else {
                    System.out.println("[MAC-PROBE] " + ip + " -> (другой MAC) " + data);
                }
            }
        } catch (IOException e) {
            System.out.println("[MAC-PROBE] " + ip + " отключился");
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    @PreDestroy
    void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}
