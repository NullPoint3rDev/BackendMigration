package org.alloy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class WeldingDeviceServer {
    
    private static final Logger log = LoggerFactory.getLogger(WeldingDeviceServer.class);
    
    @Value("${welding.device.port:3000}")
    private int port;

    @Value("${welding.device.macs:8CAAB50C4254,E09806083396}")
    private String macsConfig;
    
    private volatile boolean running = true;
    private Thread serverThread;
    private Thread heartbeatThread;
    private ServerSocket serverSocket;
    // private volatile Socket currentClientSocket = null; // not used
    
    @Autowired
    private WeldingDeviceManagerService deviceManager;

    @Autowired
    private CoreOutboundService coreOutboundService;

    @Autowired
    private DeviceModelService deviceModelService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // @Autowired
    // private DeviceController deviceController; // not used here

    @PostConstruct
    public void start() {
        System.out.println("[WELDING-SERVER] 🚀 Запуск TCP сервера для Блока мониторинга ОГК");
        System.out.println("[WELDING-SERVER] Порт: " + port);
        System.out.println("[WELDING-SERVER] Разрешенные MAC: " + macsConfig);
        log.info("[WELDING-SERVER] Запуск сервера. Порт: {}, Разрешенные MAC: {}", port, macsConfig);
        
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
            log.info("[WELDING-SERVER] Сервер запущен на порту {}", port);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("[WELDING-SERVER] 🔌 Подключение от: " + clientIp);
                    log.info("[WELDING-SERVER] Подключение от {}", clientIp);
                    
                    // Соединение принято
                    
                    // Обрабатываем подключение в отдельном потоке
                    new Thread(() -> handleClient(clientSocket)).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[WELDING-SERVER] ❌ Ошибка принятия подключения: " + e.getMessage());
                        log.error("[WELDING-SERVER] Ошибка принятия подключения", e);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[WELDING-SERVER] ❌ Ошибка запуска сервера: " + e.getMessage());
            log.error("[WELDING-SERVER] Ошибка запуска сервера", e);
        }
    }

    private void handleClient(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        
        try {
            clientSocket.setTcpNoDelay(true);
        } catch (Exception ignored) {}

        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            System.out.println("[WELDING-SERVER] 🔄 Ожидание данных от " + clientIp + "...");

            while (running) {
                int read = in.read(buffer);
                if (read == -1) {
                    break;
                }

                if (read > 0) {
                    String data = new String(buffer, 0, read, StandardCharsets.US_ASCII);
                    System.out.println("[WELDING-SERVER] 📨 Получены данные от " + clientIp + ": " + data);
                    log.info("[WELDING-SERVER] Получены данные от {}: {}", clientIp, data);

                    String mac = extractMacFromPacket(data);
                    if (mac != null) {
                        System.out.println("[WELDING-SERVER] MAC из пакета: " + mac);
                        log.debug("[WELDING-SERVER] MAC из пакета: {}", mac);

                        if (isAllowedMac(mac)) {

                            // 1) Сначала проверяем запрос синхронизации времени и отвечаем сразу
                            if (isTimeSyncRequest(data, mac)) {
                                String ts = coreOutboundService.buildTimeSyncMessage(mac, true);
                                if (ts != null) {
                                    try {
                                        out.write(ts.getBytes(StandardCharsets.US_ASCII));
                                        out.flush();
                                        System.out.println("[WELDING-SERVER] ⏫ Отправлена синхронизация времени: " + ts);
                                    } catch (IOException ex) {
                                        log.error("[WELDING-SERVER] Ошибка отправки синхронизации времени", ex);
                                    }
                                }
                                // Не выполняем остальную логику ответа для этого кадра
                                continue;
                            }

                            if (!data.startsWith("PING:")) {
                                // Определяем модель устройства по MAC из БД
                                org.alloy.models.DeviceModel deviceModel = deviceModelService.getDeviceModelByMac(mac);
                                String source = deviceModel != null ? deviceModel.getDisplayName() : 
                                              (mac.equalsIgnoreCase("E09806083396") ? "Core" : "Блока мониторинга ОГК");
                                String msg = "[WELDING-SERVER] ✅ Данные от " + source + " (" + mac + "): " + data;
                                System.out.println(msg);
                                log.info(msg);

                                // Проверяем соответствие формата пакета модели устройства
                                if (deviceModel != null && !deviceModelService.isPacketFormatMatches(mac, data)) {
                                    System.out.println("[WELDING-SERVER] ❌ ОШИБКА: Формат пакета не соответствует модели устройства " + deviceModel.getDisplayName() + " для MAC " + mac);
                                    
                                    // Отправляем событие ошибки соответствия модели
                                    try {
                                        String errorMessage = "Формат пакета не соответствует модели устройства " + deviceModel.getDisplayName();
                                        String errorJson = objectMapper.writeValueAsString(Map.of(
                                            "mac", mac,
                                            "expectedModel", deviceModel.getDisplayName(),
                                            "message", errorMessage,
                                            "timestamp", System.currentTimeMillis()
                                        ));
                                        messagingTemplate.convertAndSend("/topic/device-model-error", errorJson);
                                    } catch (Exception e) {
                                        log.error("[WELDING-SERVER] Ошибка отправки события ошибки модели", e);
                                    }
                                }

                                deviceManager.processDeviceData(data, mac);

                                // Отправляем событие обновления статуса устройства
                                try {
                                    String statusJson = objectMapper.writeValueAsString(Map.of(
                                        "mac", mac,
                                        "status", "online",
                                        "timestamp", System.currentTimeMillis(),
                                        "model", deviceModel != null ? deviceModel.name() : "UNKNOWN"
                                    ));
                                    messagingTemplate.convertAndSend("/topic/device-status", statusJson);
                                } catch (Exception e) {
                                    log.error("[WELDING-SERVER] Ошибка отправки статуса устройства", e);
                                }

                                // Положим входящий пакет в очередь (для будущей асинхронной обработки)
                                IncomingPacketsQueue.Packet p = new IncomingPacketsQueue.Packet();
                                p.ip = clientIp;
                                p.mac = mac;
                                p.data = data;
                                p.serverDatetime = java.time.Instant.now();
                                IncomingPacketsQueue.enqueue(p);

                                // Парсинг пакета Core и вывод удобочитаемо
                                if (deviceModel == org.alloy.models.DeviceModel.CORE || mac.equalsIgnoreCase("E09806083396")) {
                                    CorePacket parsed = CorePacketParser.parse(data);
                                    if (parsed != null) {
                                        System.out.println("[WELDING-SERVER] 📦 Core parsed: " + parsed);
                                        log.info("[WELDING-SERVER] Core parsed: {}", parsed);
                                        // Можно дальше передавать parsed на фронт/в сервисы
                                        // Сформировать и отправить ответ (если включено)
                                        String reply = coreOutboundService.buildMessageForMac(mac);
                                        if (reply != null && !reply.isEmpty()) {
                                            byte[] respBytes = reply.getBytes(StandardCharsets.US_ASCII);
                                            try {
                                                out.write(respBytes);
                                                out.flush();
                                                System.out.println("[WELDING-SERVER] ⏫ Core reply sent: " + reply);
                                            } catch (IOException ex) {
                                                log.error("[WELDING-SERVER] Ошибка отправки Core ответа", ex);
                                            }
                                        }
                                    }
                                }
                            }

                            // timesync уже обработан выше

                            // Немедленная отправка ответа, если он есть
                            OutboundPacketsRepository.Packet outbound = OutboundPacketsRepository.tryGet(mac);
                            if (outbound != null && outbound.data != null && !outbound.data.isEmpty()) {
                                byte[] response = outbound.data.getBytes(StandardCharsets.US_ASCII);
                                try {
                                    out.write(response);
                                    out.flush();
                                    System.out.println("[WELDING-SERVER] ⏫ Отправлен ответ устройству (" + mac + "): " + outbound.data);
                                    log.debug("[WELDING-SERVER] Отправлен ответ {}", outbound.data);
                                } catch (IOException ex) {
                                    log.error("[WELDING-SERVER] Ошибка отправки ответа устройству {}", mac, ex);
                                }
                            }
                        } else {
                            String warn = "[WELDING-SERVER] ⚠️ Неизвестный MAC: " + mac + " (разрешены: " + macsConfig + ")";
                            System.out.println(warn);
                            log.warn(warn);
                        }
                    }
                }
            }

            System.out.println("[WELDING-SERVER] 🔌 Клиент " + clientIp + " закрыл соединение");
            log.info("[WELDING-SERVER] Клиент {} закрыл соединение", clientIp);
        } catch (IOException e) {
            System.err.println("[WELDING-SERVER] ❌ Ошибка обработки клиента " + clientIp + ": " + e.getMessage());
            log.error("[WELDING-SERVER] Ошибка обработки клиента " + clientIp, e);
        } finally {
            try {
                clientSocket.close();
                System.out.println("[WELDING-SERVER] 🔌 Соединение с " + clientIp + " закрыто");
                log.info("[WELDING-SERVER] Соединение с {} закрыто", clientIp);
            } catch (IOException e) {
                System.err.println("[WELDING-SERVER] ❌ Ошибка закрытия соединения: " + e.getMessage());
                log.error("[WELDING-SERVER] Ошибка закрытия соединения", e);
            }
        }
    }
    
    private void startHeartbeatThread() {
        // Блок мониторинга сам отправляет данные, heartbeat не нужен
        System.out.println("[WELDING-SERVER] ℹ️ Heartbeat отключен - блок мониторинга сам отправляет данные");
        log.info("[WELDING-SERVER] Heartbeat отключен - блок мониторинга сам отправляет данные");
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

    private boolean isTimeSyncRequest(String data, String mac) {
        if (data == null) return false;
        String[] needles = new String[] { ";0101010D0A", ";010101" };
        // допускаем варианты с ведущим ':'
        int semicolon = data.indexOf(';');
        if (semicolon < 0) return false;
        String macIn = extractMacFromPacket(data);
        if (macIn == null || !macIn.equalsIgnoreCase(mac)) return false;
        String tail = data.substring(semicolon).toUpperCase();
        for (String n : needles) {
            if (tail.contains(n)) return true;
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
                log.info("[WELDING-SERVER] Сервер остановлен");
            } catch (IOException e) {
                System.err.println("[WELDING-SERVER] ❌ Ошибка остановки сервера: " + e.getMessage());
                log.error("[WELDING-SERVER] Ошибка остановки сервера", e);
            }
        }
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
    }
} 