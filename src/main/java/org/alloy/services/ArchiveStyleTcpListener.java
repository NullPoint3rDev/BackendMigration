package org.alloy.services;

import org.alloy.logging.UnknownMacLog;
import org.alloy.metrics.WeldingMetrics;
import org.alloy.protocol.v2.V2ConnectionState;
import org.alloy.protocol.v2.V2ProtocolConstants;
import org.alloy.protocol.v2.V2ProtocolService;
import org.alloy.repositories.WeldingMachineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP Listener в стиле archive проекта с отдельными потоками для каждого подключения
 * и таймаутом соединения 30 секунд
 */
@Service
public class ArchiveStyleTcpListener {

    private static final Logger log = LoggerFactory.getLogger(ArchiveStyleTcpListener.class);

    // Константы из archive
    private static final int TIMEOUT_SECONDS = 30;
    private static final int BUFFER_SIZE = 4096;
    private static final int SLEEP_MS = 100;
    /** TTL кэша allowlist MAC — не бить БД на каждый TCP-пакет. */
    private static final long MAC_ALLOW_CACHE_TTL_MS = 60_000L;
    private static final int CLIENT_POOL_CORE = 4;
    private static final int CLIENT_POOL_MAX = 32;
    private static final int CLIENT_POOL_QUEUE = 64;

    @Value("${welding.archive.server.port:3001}")
    private int serverPort;

    @Value("${welding.archive.server.ip:0.0.0.0}")
    private String serverIp;

    // Маппинг IP адресов на MAC адреса для случаев, когда MAC не извлекается из данных
    private final Map<String, String> ipToMacMapping = Map.of(
            "192.168.10.137", "E09806083396", // Core
            "192.168.10.1", "8CAAB50C4254",   // Блок мониторинга (через роутер)
            "192.168.10.104", "E09806083396", // Core (альтернативный IP)
            "89.109.8.59", "8CAAB50C4254"     // Блок мониторинга (внешний IP)
    );

    private volatile boolean running = true;
    private Thread listenerThread;
    private ServerSocket serverSocket;
    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private ExecutorService clientExecutor;
    // ponytail: in-memory TTL; ceiling — до 60s stale после add/delete аппарата
    private final ConcurrentHashMap<String, CachedMacAllow> macAllowCache = new ConcurrentHashMap<>();

    // Эти сервисы используются через статические методы очередей
    // @Autowired
    // private ArchiveStylePacketParser packetParser;

    // @Autowired
    // private ArchiveStyleOutboundService outboundService;

    @Autowired(required = false)
    private CoreOutboundService coreOutboundService;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WeldingDeviceManagerService deviceManager;

    @Autowired
    private WeldingMetrics weldingMetrics;

    @Autowired
    private DeviceLivenessRegistry deviceLivenessRegistry;

    @Autowired
    private DeviceModelService deviceModelService;

    @Autowired
    @Lazy
    private MacAddressRegistryService macAddressRegistryService;

    @Autowired(required = false)
    private V2ProtocolService v2ProtocolService;

    @Value("${welding.archive.allowed.macs:}")
    private String archiveAllowedMacsConfig;

    private final WeldingMachineRepository weldingMachineRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Конструктор для инжекции репозитория
    public ArchiveStyleTcpListener(WeldingMachineRepository weldingMachineRepository) {
        this.weldingMachineRepository = weldingMachineRepository;
    }

    @PostConstruct
    public void start() {
        System.out.println("[ARCHIVE-TCP-LISTENER] 🚀 Запуск TCP сервера в стиле archive");
        System.out.println("[ARCHIVE-TCP-LISTENER] Порт: " + serverPort);
        System.out.println("[ARCHIVE-TCP-LISTENER] IP: " + serverIp);
        System.out.println("[ARCHIVE-TCP-LISTENER] Проверка MAC-адресов: по реестру MAC (MacAddressRegistry)");
        System.out.println("[ARCHIVE-TCP-LISTENER] Таймаут: " + TIMEOUT_SECONDS + " секунд");

        log.info("[ARCHIVE-TCP-LISTENER] Запуск сервера. Порт: {}, IP: {}, Проверка MAC: реестр (кэш {}ms)",
                serverPort, serverIp, MAC_ALLOW_CACHE_TTL_MS);

        AtomicInteger poolThreadNo = new AtomicInteger(0);
        clientExecutor = new ThreadPoolExecutor(
                CLIENT_POOL_CORE,
                CLIENT_POOL_MAX,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(CLIENT_POOL_QUEUE),
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName("ClientHandler-" + poolThreadNo.incrementAndGet());
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        listenerThread = new Thread(this::runListener);
        listenerThread.setDaemon(true);
        listenerThread.setName("ArchiveTcpListener");
        listenerThread.start();
    }

    private void runListener() {
        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("[ARCHIVE-TCP-LISTENER] ✅ TCP сервер запущен на " + serverIp + ":" + serverPort);
            log.info("[ARCHIVE-TCP-LISTENER] Сервер запущен на {}:{}", serverIp, serverPort);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();

                    log.info("[ARCHIVE-TCP-LISTENER] Подключение от {}", clientIp);

                    threadCounter.incrementAndGet();
                    clientExecutor.execute(() -> handleClientConnection(clientSocket));

                } catch (IOException e) {
                    if (running) {
                        System.err.println("[ARCHIVE-TCP-LISTENER] ❌ Ошибка принятия подключения: " + e.getMessage());
                        log.error("[ARCHIVE-TCP-LISTENER] Ошибка принятия подключения", e);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ARCHIVE-TCP-LISTENER] ❌ Ошибка запуска сервера: " + e.getMessage());
            log.error("[ARCHIVE-TCP-LISTENER] Ошибка запуска сервера", e);
        }
    }

    /**
     * Обработка подключения клиента в отдельном потоке
     */
    private void handleClientConnection(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        int threadId = threadCounter.get();

        // Настройка сокета
        try {
            clientSocket.setTcpNoDelay(true);
            clientSocket.setSoTimeout(TIMEOUT_SECONDS * 1000); // 30 секунд таймаут
        } catch (Exception e) {
            log.warn("[ARCHIVE-TCP-LISTENER] Ошибка настройки сокета для {}: {}", clientIp, e.getMessage());
        }

        // Время начала подключения
        LocalDateTime connectionStartTime = LocalDateTime.now();

        // Время таймаута (обновляется при каждом пакете) — ASCII-путь
        LocalDateTime timeoutTime = connectionStartTime.plusSeconds(TIMEOUT_SECONDS);

        // Пытаемся определить MAC по IP адресу
        String macAddress = ipToMacMapping.get(clientIp);
        if (macAddress == null) {
            macAddress = ""; // MAC будет извлечен из первого пакета
        } else {
            log.debug("[ARCHIVE-TCP-LISTENER] MAC определен по IP {}: {}", clientIp, macAddress);
        }

        log.info("[ARCHIVE-TCP-LISTENER] CONNECTED: IP: {}; Thread: {}", clientIp, threadId);
        weldingMetrics.tcpConnectionOpened();

        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            boolean connected = true;
            V2ConnectionState v2Conn = new V2ConnectionState();
            // ponytail: blocking read только для v2; ASCII остаётся на available()+sleep
            boolean v2BlockingRead = false;

            // Основной цикл обработки данных
            while (connected && running) {
                int bytesRead = 0;

                try {
                    if (v2BlockingRead) {
                        bytesRead = in.read(buffer);
                    } else if (in.available() > 0) {
                        bytesRead = in.read(buffer);
                    } else {
                        Thread.sleep(SLEEP_MS);
                    }
                } catch (SocketTimeoutException e) {
                    // только v2 blocking: SoTimeout = idle disconnect
                    connected = false;
                    log.info("[ARCHIVE-TCP-LISTENER] Таймаут для {}", clientIp);
                    sendConnectionEvent(clientIp, macAddress, "timeout", null);
                    break;
                } catch (IOException e) {
                    log.error("[ARCHIVE-TCP-LISTENER] Ошибка чтения потока для {}", clientIp, e);
                    connected = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    connected = false;
                }

                if (v2BlockingRead && bytesRead < 0) {
                    connected = false;
                    break;
                }

                if (bytesRead > 0) {
                    byte[] rawChunk = Arrays.copyOf(buffer, bytesRead);

                    // Protocol v2 (binary): только если сокет уже v2, или копится хвост v2,
                    // или первый байт = 0x01 (sync новых устройств). Кадры с ':' сюда НЕ попадают.
                    boolean v2Candidate = v2ProtocolService != null
                            && rawChunk.length > 0
                            && (v2Conn.active
                                || (v2Conn.tail != null && v2Conn.tail.length > 0)
                                || rawChunk[0] == V2ProtocolConstants.TYPE_SYNC);

                    if (v2Candidate && v2ProtocolService.shouldHandleAsV2(v2Conn, rawChunk)) {
                        v2BlockingRead = true;
                        String v2Mac = (v2Conn.mac != null && !v2Conn.mac.isEmpty())
                                ? v2Conn.mac
                                : "";
                        if (!v2Mac.isEmpty()) {
                            deviceLivenessRegistry.markSeen(v2Mac);
                        }
                        boolean allowed = v2Mac.isEmpty() || isAllowedMac(v2Mac);
                        // до sync MAC ещё неизвестен — принимаем кадр, allowlist проверим после sync по MAC в хендлере
                        if (v2Mac.isEmpty() || allowed) {
                            if (!v2Mac.isEmpty()) {
                                try {
                                    macAddressRegistryService.recordPacket(v2Mac);
                                } catch (Exception ex) {
                                    log.warn("[ARCHIVE-TCP-LISTENER] MAC registry recordPacket failed for {}: {}",
                                            v2Mac, ex.getMessage());
                                }
                                deviceManager.touchInboundTelemetry(v2Mac);
                            }
                            try {
                                v2ProtocolService.onBytes(v2Conn, rawChunk, out);
                                if (v2Conn.mac != null && !v2Conn.mac.isEmpty()) {
                                    macAddress = v2Conn.mac;
                                    if (!isAllowedMac(macAddress)) {
                                        weldingMetrics.recordUnknownMac();
                                        UnknownMacLog.unknownMac("ArchiveStyleTcpListener", macAddress,
                                                "clientIp=" + clientIp + ", v2 MAC rejected after sync");
                                        v2Conn.active = false;
                                    }
                                }
                                timeoutTime = LocalDateTime.now().plusSeconds(TIMEOUT_SECONDS);
                            } catch (Exception ex) {
                                log.error("[ARCHIVE-TCP-LISTENER] V2 handle failed mac={}", v2Mac, ex);
                            }
                        } else {
                            weldingMetrics.recordUnknownMac();
                            UnknownMacLog.unknownMac("ArchiveStyleTcpListener", v2Mac,
                                    "clientIp=" + clientIp + ", v2 packet rejected");
                        }
                        continue;
                    }

                    // ===== Старый протокол (ASCII :MAC;…) — ниже без изменений логики =====
                    // Преобразуем данные в строку
                    String data = new String(rawChunk, StandardCharsets.US_ASCII);

                    log.debug("[ARCHIVE-TCP-LISTENER] Thread {}, IP={}, MAC={}: {}",
                            threadId, clientIp, macAddress, data);

                    // Извлекаем MAC из пакета
                    if (!data.isEmpty()) {
                        String extractedMac = extractMacFromPacket(data);
                        if (extractedMac != null && !extractedMac.isEmpty()) {
                            macAddress = extractedMac;
                            log.debug("[ARCHIVE-TCP-LISTENER] MAC из пакета: {}", macAddress);
                        }
                    }

                    // Если MAC не извлечен из данных, пытаемся определить по IP
                    if (macAddress == null || macAddress.isEmpty()) {
                        macAddress = ipToMacMapping.get(clientIp);
                        if (macAddress != null) {
                            log.debug("[ARCHIVE-TCP-LISTENER] MAC определен по IP {}: {}", clientIp, macAddress);
                        }
                    }

                    // Фиксируем «живость» ЛЮБОГО MAC до проверки разрешённых — чтобы можно было
                    // проверить соединение ещё не зарегистрированного аппарата при добавлении оборудования.
                    if (macAddress != null && !macAddress.isEmpty()) {
                        deviceLivenessRegistry.markSeen(macAddress);
                    }

                    // Проверяем разрешенные MAC-адреса
                    if (isAllowedMac(macAddress)) {
                        try {
                            macAddressRegistryService.recordPacket(macAddress);
                        } catch (Exception ex) {
                            log.warn("[ARCHIVE-TCP-LISTENER] MAC registry recordPacket failed for {}: {}",
                                    macAddress, ex.getMessage());
                        }
                        deviceManager.touchInboundTelemetry(macAddress);

                        // Немедленно отвечаем на запрос синхронизации времени от Core
                        if (isTimeSyncRequest(data, macAddress) && coreOutboundService != null) {
                            try {
                                String ts = coreOutboundService.buildTimeSyncMessage(macAddress, true);
                                if (ts != null && !ts.isEmpty()) {
                                    byte[] tsBytes = ts.getBytes(StandardCharsets.US_ASCII);
                                    out.write(tsBytes);
                                    out.flush();
                                    log.debug("[ARCHIVE-TCP-LISTENER] ⏱️ Отправлена синхронизация времени {}", ts);
                                }
                                // Продолжаем дальнейшую обработку кадра без возврата
                            } catch (Exception ex) {
                                log.error("[ARCHIVE-TCP-LISTENER] Ошибка отправки синхронизации времени", ex);
                            }
                        }

                        // Эхо-ответ для Core устройств: отправляем те же данные обратно
                        if (coreOutboundService != null && coreOutboundService.isCoreDevice(macAddress)) {
                            try {
                                String echoResponse = coreOutboundService.buildEchoResponse(data);
                                if (echoResponse != null) {
                                    byte[] echoData = echoResponse.getBytes(StandardCharsets.US_ASCII);
                                    out.write(echoData);
                                    out.flush();
                                    log.debug("[ARCHIVE-TCP-LISTENER] 🔄 Эхо-ответ для Core {}: {}", macAddress, echoResponse);
                                }
                            } catch (IOException e) {
                                log.error("[ARCHIVE-TCP-LISTENER] Ошибка отправки эхо-ответа для Core {}: {}", macAddress, e.getMessage());
                            }
                        }

                        // Создаем пакет
                        ArchivePacket packet = new ArchivePacket();
                        packet.setIp(clientIp);
                        packet.setMac(macAddress);
                        packet.setData(data);
                        packet.setServerDatetime(LocalDateTime.now());

                        // Добавляем в очередь для обработки
                        ArchiveIncomingPacketsQueue.enqueue(packet);
                        log.debug("[ARCHIVE-TCP-LISTENER] Пакет добавлен в очередь обработки: MAC={}, IP={}", macAddress, clientIp);

                        // Проверяем исходящие пакеты
                        ArchivePacket outboundPacket = ArchiveOutboundPacketsRepository.tryGet(macAddress);
                        if (outboundPacket != null && outboundPacket.getData() != null && !outboundPacket.getData().isEmpty()) {
                            try {
                                log.debug("[ARCHIVE-TCP-LISTENER] Отправка ответа {}: {}", macAddress, outboundPacket.getData());

                                byte[] responseData = outboundPacket.getData().getBytes(StandardCharsets.US_ASCII);
                                out.write(responseData);
                                out.flush();
                            } catch (IOException e) {
                                log.error("[ARCHIVE-TCP-LISTENER] Ошибка отправки пакета {}: {}", macAddress, outboundPacket.getData(), e);
                            }
                        }

                        // Обновляем время таймаута
                        timeoutTime = LocalDateTime.now().plusSeconds(TIMEOUT_SECONDS);

                        // Отправляем событие через WebSocket
                        sendConnectionEvent(clientIp, macAddress, "data_received", data);

                    } else {
                        weldingMetrics.recordUnknownMac();
                        UnknownMacLog.unknownMac("ArchiveStyleTcpListener", macAddress,
                                "clientIp=" + clientIp + ", packet rejected");
                        sendConnectionEvent(clientIp, macAddress, "unauthorized", data);
                    }
                } else if (!v2BlockingRead) {
                    // ASCII: проверяем таймаут (v2 idle ловится SocketTimeoutException)
                    if (LocalDateTime.now().isAfter(timeoutTime)) {
                        connected = false;
                        log.info("[ARCHIVE-TCP-LISTENER] Таймаут для {}", clientIp);
                        sendConnectionEvent(clientIp, macAddress, "timeout", null);
                    }
                }
            }

        } catch (IOException e) {
            log.error("[ARCHIVE-TCP-LISTENER] Ошибка обработки клиента {}", clientIp, e);
        } finally {
            weldingMetrics.tcpConnectionClosed();
            // Закрываем соединение
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                log.warn("[ARCHIVE-TCP-LISTENER] Ошибка закрытия соединения для {}", clientIp, e);
            }

            // Удаляем из активных подключений (пока не используется)
            // activeConnections.remove(clientIp);

            log.info("[ARCHIVE-TCP-LISTENER] DISCONNECTED THREAD {}! IP: {}", threadId, clientIp);
            sendConnectionEvent(clientIp, macAddress, "disconnected", null);
        }
    }

    /**
     * Извлечение MAC-адреса из пакета
     * Формат: :MAC;data (например, :8CAAB50C4254;data)
     */
    private String extractMacFromPacket(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        int colonPos = data.indexOf(':');
        int semicolonPos = data.indexOf(';');

        if (colonPos >= 0 && semicolonPos > colonPos) {
            String mac = data.substring(colonPos + 1, semicolonPos);
            if (mac.length() == 12 && mac.matches("[0-9A-Fa-f]{12}")) {
                return mac.toUpperCase();
            }
        }

        return null;
    }

    /**
     * Определение запроса синхронизации времени от Core (например, содержит 01010131 после ';')
     */
    private boolean isTimeSyncRequest(String data, String mac) {
        if (data == null) return false;
        int semicolon = data.indexOf(';');
        if (semicolon < 0) return false;
        String macIn = extractMacFromPacket(data);
        if (macIn == null || !macIn.equalsIgnoreCase(mac)) return false;
        String tail = data.substring(semicolon).toUpperCase();
        return tail.contains("01010131");
    }

    /**
     * Проверка разрешённых MAC по реестру (с TTL-кэшем).
     * Разрешены WAITING и ACTIVE; BLOCKED и отсутствующие в реестре — отклоняются.
     * Fallback: welding.core.macs и welding.archive.allowed.macs.
     */
    private boolean isAllowedMac(String mac) {
        if (mac == null || mac.isEmpty()) {
            return false;
        }
        if (DeviceModelService.isDebugMac(mac)) {
            return false;
        }

        String normalizedMac = mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();

        long now = System.currentTimeMillis();
        CachedMacAllow cached = macAllowCache.get(normalizedMac);
        if (cached != null && now - cached.checkedAtMs < MAC_ALLOW_CACHE_TTL_MS) {
            return cached.allowed;
        }

        boolean allowed;
        try {
            allowed = macAddressRegistryService.isAllowedForTcp(normalizedMac);
        } catch (Exception e) {
            log.warn("[ARCHIVE-TCP-LISTENER] MAC registry lookup failed for {}: {}", normalizedMac, e.getMessage());
            allowed = false;
        }
        if (!allowed) {
            allowed = deviceModelService.isCoreMacInConfig(normalizedMac)
                    || isMacInArchiveAllowedConfig(normalizedMac);
        }
        macAllowCache.put(normalizedMac, new CachedMacAllow(allowed, now));
        return allowed;
    }

    /** Сброс TTL-кэша после изменений в реестре MAC. */
    public void invalidateMacAllowCache(String mac) {
        if (mac == null || mac.isEmpty()) {
            macAllowCache.clear();
            return;
        }
        String normalizedMac = mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        macAllowCache.remove(normalizedMac);
    }

    private boolean isMacInArchiveAllowedConfig(String normalizedMac) {
        if (archiveAllowedMacsConfig == null || archiveAllowedMacsConfig.isBlank()) {
            return false;
        }
        for (String part : archiveAllowedMacsConfig.split(",")) {
            if (normalizedMac.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Отправка события подключения через WebSocket
     */
    private void sendConnectionEvent(String ip, String mac, String eventType, String data) {
        // Обрабатываем отключение устройства
        if ("disconnected".equals(eventType) && mac != null) {
            deviceManager.markDeviceDisconnected(mac);
            log.info("[ARCHIVE-TCP-LISTENER] Устройство {} отмечено как отключенное", mac);
        }

        // Если WebSocket отключен (нет SimpMessagingTemplate), просто выходим
        if (messagingTemplate == null) {
            return;
        }
        try {
            Map<String, Object> event = Map.of(
                    "ip", ip,
                    "mac", mac != null ? mac : "",
                    "eventType", eventType,
                    "timestamp", System.currentTimeMillis(),
                    "data", data != null ? data : ""
            );

            String jsonData = objectMapper.writeValueAsString(event);
            messagingTemplate.convertAndSend("/topic/archive-connection", jsonData);

        } catch (Exception e) {
            log.error("[ARCHIVE-TCP-LISTENER] Ошибка отправки события подключения", e);
        }
    }

    /**
     * Получение статистики активных подключений
     */
    public Map<String, Object> getConnectionStatistics() {
        Map<String, Object> stats = Map.of(
                "activeConnections", 0, // activeConnections.size(), // пока не используется
                "totalThreads", threadCounter.get(),
                "serverPort", serverPort,
                "serverIp", serverIp,
                "timeoutSeconds", TIMEOUT_SECONDS
        );
        return stats;
    }

    @PreDestroy
    public void stop() {
        running = false;

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("[ARCHIVE-TCP-LISTENER] 🔚 TCP сервер остановлен");
                log.info("[ARCHIVE-TCP-LISTENER] Сервер остановлен");
            } catch (IOException e) {
                System.err.println("[ARCHIVE-TCP-LISTENER] ❌ Ошибка остановки сервера: " + e.getMessage());
                log.error("[ARCHIVE-TCP-LISTENER] Ошибка остановки сервера", e);
            }
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        if (clientExecutor != null) {
            clientExecutor.shutdownNow();
            try {
                if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("[ARCHIVE-TCP-LISTENER] Client pool did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class CachedMacAllow {
        final boolean allowed;
        final long checkedAtMs;

        CachedMacAllow(boolean allowed, long checkedAtMs) {
            this.allowed = allowed;
            this.checkedAtMs = checkedAtMs;
        }
    }
}
