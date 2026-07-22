package org.alloy.services;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.alloy.protocol.v2.V2ProtocolConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;

@Service
public class WeldingDeviceManagerService {

    private static final Logger log = LoggerFactory.getLogger(WeldingDeviceManagerService.class);

    @Autowired
    private WeldingDataParserService dataParser;

    @Autowired
    private WeldingMachineStateService stateService;

    @Autowired
    private DeviceLivenessRegistry deviceLivenessRegistry;

    @Autowired
    private DeviceModelService deviceModelService;

    @Autowired
    private WeldingMachineLastWeldService weldingMachineLastWeldService;

    // Хранилище состояний всех аппаратов
    private final Map<String, StateSummary> deviceStates = new ConcurrentHashMap<>();

    // Хранилище статусов подключения
    private final Map<String, Boolean> connectionStatus = new ConcurrentHashMap<>();

    // Последнее время сохранения состояния по MAC (для троттлинга)
    private final Map<String, Long> lastSaveTimestampMs = new ConcurrentHashMap<>();

    // Coalescing: один in-flight save на MAC, в БД уходит последний snapshot
    private final Map<String, StateSummary> pendingSaveByMac = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> saveInFlightByMac = new ConcurrentHashMap<>();

    // Отдельный executor для операций с БД, чтобы не блокировать общий пул
    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(3);

    @PostConstruct
    public void init() {
        log.info("Сервис управления аппаратами инициализирован");
    }

    /**
     * Обрабатывает данные от сварочного аппарата
     */
    public void processDeviceData(String data, String mac) {
        try {
            String normalizedMac = deviceModelService.normalizeMac(mac);
            if (normalizedMac == null || normalizedMac.isEmpty()) {
                return;
            }
            // Protocol v2 test MAC — только /v2-protocol-test, не боевой мониторинг
            if (V2ProtocolConstants.isTestMac(normalizedMac)) {
                return;
            }

            if (data != null) {
                String packet = data.trim();
                if (!packet.startsWith("PING:")) {
                    log.info("{} {}", normalizedMac, packet);
                }
            }

            // Парсим данные
            StateSummary previous = deviceStates.get(normalizedMac);
            StateSummary stateSummary = dataParser.parseWeldingData(data, normalizedMac);

            if (stateSummary == null) {
                System.err.println("[DEVICE-MANAGER] ⚠️ Парсер вернул null для MAC=" + normalizedMac);
                return;
            }
            preserveCoreGasMetrics(previous, stateSummary);

            // Сначала in-memory — panel-state живёт от lastDatetimeUpdate, не ждём БД/lastWeld.
            deviceStates.put(normalizedMac, stateSummary);
            connectionStatus.put(normalizedMac, true);

            weldingMachineLastWeldService.updateFromPanelState(
                    normalizedMac, previous, stateSummary, LocalDateTime.now(ZoneOffset.UTC));

            // WebSocket отключен - все устройства работают через polling API (как в archive проекте)
            // deviceController.sendDeviceState(stateSummary, mac);

            // Короткий лог
            // System.out.println("[DEVICE-MANAGER] ✅ Обновлено состояние для " + mac);

            // Троттлинг сохранений в БД: не чаще раза в 400мс на устройство
            long now = System.currentTimeMillis();
            Long lastSaved = lastSaveTimestampMs.getOrDefault(normalizedMac, 0L);
            if (now - lastSaved >= 400) {
                lastSaveTimestampMs.put(normalizedMac, now);
                scheduleCoalescedDbSave(normalizedMac, stateSummary);
            }

        } catch (Exception e) {
            System.err.println("[DEVICE-MANAGER] ❌ Ошибка обработки данных от " + mac + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void scheduleCoalescedDbSave(String mac, StateSummary stateSummary) {
        pendingSaveByMac.put(mac, stateSummary);
        tryStartCoalescedSave(mac);
    }

    private void tryStartCoalescedSave(String mac) {
        AtomicBoolean inFlight = saveInFlightByMac.computeIfAbsent(mac, ignored -> new AtomicBoolean(false));
        if (!inFlight.compareAndSet(false, true)) {
            return;
        }
        dbExecutor.execute(() -> runCoalescedSaveLoop(mac, inFlight));
    }

    private void runCoalescedSaveLoop(String mac, AtomicBoolean inFlight) {
        try {
            while (true) {
                StateSummary snapshot = pendingSaveByMac.remove(mac);
                if (snapshot == null) {
                    break;
                }
                try {
                    stateService.saveMachineState(mac, snapshot);
                } catch (Exception dbError) {
                    System.err.println("[DEVICE-MANAGER] ❌ Ошибка сохранения в БД для " + mac + ": "
                            + dbError.getMessage());
                    dbError.printStackTrace();
                }
                if (!pendingSaveByMac.containsKey(mac)) {
                    break;
                }
            }
        } finally {
            inFlight.set(false);
            if (pendingSaveByMac.containsKey(mac)) {
                tryStartCoalescedSave(mac);
            }
        }
    }

    /**
     * Получает статус подключения аппарата
     */
    private StateSummary resolveDeviceState(String mac) {
        if (mac == null || mac.isBlank()) {
            return null;
        }
        String normalizedMac = deviceModelService.normalizeMac(mac);
        if (normalizedMac == null || normalizedMac.isEmpty()) {
            return null;
        }
        StateSummary state = deviceStates.get(normalizedMac);
        if (state != null) {
            return state;
        }
        return deviceStates.get(mac.toUpperCase());
    }

    /** Продлевает свежесть panel-state, пока пакет в очереди воркера (TCP уже принял). */
    public void touchInboundTelemetry(String mac) {
        String normalizedMac = deviceModelService.normalizeMac(mac);
        if (normalizedMac == null || V2ProtocolConstants.isTestMac(normalizedMac)) {
            return;
        }
        StateSummary state = deviceStates.get(normalizedMac);
        if (state != null) {
            state.setLastDatetimeUpdate(LocalDateTime.now(ZoneOffset.UTC));
        }
    }

    private static void preserveCoreGasMetrics(StateSummary previous, StateSummary current) {
        if (previous == null || current == null || previous.getProperties() == null || current.getProperties() == null) {
            return;
        }
        copyPropertyIfMissing(previous, current, "Core.GasConsumptionSincePowerOn");
        copyPropertyIfMissing(previous, current, "Расход газа с включения");
    }

    private static void copyPropertyIfMissing(StateSummary from, StateSummary to, String key) {
        if (to.getProperties().containsKey(key)) {
            return;
        }
        StateSummaryPropertyValue src = from.getProperties().get(key);
        if (src != null) {
            to.getProperties().put(key, src);
        }
    }

    public boolean isDeviceConnected(String mac) {
        long now = System.currentTimeMillis();

        Long seenMs = deviceLivenessRegistry.getLastSeenMs(mac);
        if (seenMs != null && now - seenMs < 30000) {
            return resolveDeviceState(mac) != null;
        }

        StateSummary state = resolveDeviceState(mac);
        if (state == null) {
            return false;
        }

        if (state.getLastDatetimeUpdate() == null) {
            return false;
        }

        long lastUpdate = state.getLastDatetimeUpdate()
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return now - lastUpdate < 10000;
    }

    /**
     * Получает состояния всех аппаратов
     */
    public Map<String, StateSummary> getAllDeviceStates() {
        return new HashMap<>(deviceStates);
    }

    /**
     * Получает статусы подключения всех аппаратов
     */
    public Map<String, Boolean> getAllConnectionStatuses() {
        return new HashMap<>(connectionStatus);
    }

    /**
     * Отмечает аппарат как отключенный
     */
    public void markDeviceDisconnected(String mac) {
        connectionStatus.put(mac, false);

        // Не переписываем status последнего пакета на Offline: online/offline решает
        // свежесть данных (isDeviceConnected, порог 10с). Иначе разрыв одного из
        // параллельных TCP-соединений затирает реальный Welding/Idle на Offline
        // до прихода следующего пакета — на фронте мигает «Не в сети» при живой сварке.
    }

    @PreDestroy
    public void shutdown() {
        dbExecutor.shutdownNow();
    }

    /**
     * Получает статистику по аппаратам
     */
    public Map<String, Object> getDeviceStatistics() {
        Map<String, Object> stats = new HashMap<>();

        int totalDevices = deviceStates.size();
        int connectedDevices = (int) connectionStatus.values().stream().filter(Boolean::booleanValue).count();
        int workingDevices = (int) deviceStates.values().stream()
                .filter(state -> state.getStatus() == WeldingMachineStatus.Welding)
                .count();
        int errorDevices = (int) deviceStates.values().stream()
                .filter(state -> state.getStatus() == WeldingMachineStatus.Error)
                .count();

        stats.put("totalDevices", totalDevices);
        stats.put("connectedDevices", connectedDevices);
        stats.put("workingDevices", workingDevices);
        stats.put("errorDevices", errorDevices);
        stats.put("disconnectedDevices", totalDevices - connectedDevices);

        return stats;
    }

    /**
     * Получает текущее состояние аппарата по MAC адресу
     */
    public StateSummary getDeviceState(String mac) {
        return resolveDeviceState(mac);
    }


} 