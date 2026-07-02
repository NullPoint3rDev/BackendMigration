//package org.alloy.services;
//
//import org.springframework.stereotype.Service;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//@Service
//public class TcpDeviceListener {
//    private final int port = 3000;
//    private volatile boolean running = true;
//    private final ExecutorService clientPool = Executors.newCachedThreadPool();
//    private ServerSocket serverSocket;
//
//    @PostConstruct
//    public void start() {
//        Thread serverThread = new Thread(() -> {
//            try {
//                serverSocket = new ServerSocket(port);
//                System.out.println("[TCP] Listener started on port " + port);
//                while (running) {
//                    Socket clientSocket = serverSocket.accept();
//                    clientPool.submit(() -> handleClient(clientSocket));
//                }
//            } catch (IOException e) {
//                if (running) {
//                    System.err.println("[TCP] Error in server: " + e.getMessage());
//                }
//            }
//        });
//        serverThread.setDaemon(true);
//        serverThread.start();
//    }
//
//    private void handleClient(Socket socket) {
//        String clientIp = socket.getInetAddress().getHostAddress();
//        try (InputStream in = socket.getInputStream();
//             OutputStream out = socket.getOutputStream()) {
//
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = in.read(buffer)) != -1) {
//                String data = new String(buffer, 0, bytesRead, StandardCharsets.US_ASCII);
//                System.out.println("[TCP] Received from " + clientIp + ": " + data);
//
//                // Извлечение MAC-адреса из пакета
//                String mac = "";
//                int pos1 = data.indexOf(':');
//                if (pos1 >= 0) {
//                    int pos2 = data.indexOf(';', pos1);
//                    if (pos2 > 0 && pos2 - pos1 == 13) {
//                        mac = data.substring(pos1 + 1, pos1 + 13);
//                        System.out.println("[TCP] MAC from packet: " + mac);
//                    }
//                }
//
//                // Здесь можно добавить обработку пакета, очередь, ответ и т.д.
//                // out.write(...); // если нужно отправить ответ
//            }
//        } catch (IOException e) {
//            System.err.println("[TCP] Error handling client " + clientIp + ": " + e.getMessage());
//        }
//    }
//
//    @PreDestroy
//    public void stop() {
//        running = false;
//        try {
//            if (serverSocket != null && !serverSocket.isClosed()) {
//                serverSocket.close();
//            }
//        } catch (IOException ignored) {}
//        clientPool.shutdownNow();
//        System.out.println("[TCP] Listener stopped.");
//    }
//}
