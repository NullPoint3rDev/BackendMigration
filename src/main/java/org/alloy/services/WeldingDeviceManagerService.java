package org.alloy.services;

import org.alloy.controllers.DeviceController;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;

@Service
public class WeldingDeviceManagerService {

    @Autowired
    private WeldingDataParserService dataParser;

    @Autowired
    private WeldingMachineStateService stateService;

    @Autowired
    private DeviceController deviceController;

    // Хранилище состояний всех аппаратов
    private final Map<String, StateSummary> deviceStates = new ConcurrentHashMap<>();

    // Хранилище статусов подключения
    private final Map<String, Boolean> connectionStatus = new ConcurrentHashMap<>();

    // Последнее время сохранения состояния по MAC (для троттлинга)
    private final Map<String, Long> lastSaveTimestampMs = new ConcurrentHashMap<>();

    // Отдельный executor для операций с БД, чтобы не блокировать общий пул
    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(3);

    @PostConstruct
    public void init() {
        System.out.println("[DEVICE-MANAGER] Сервис управления аппаратами инициализирован");
    }

    /**
     * Обрабатывает данные от сварочного аппарата
     */
    public void processDeviceData(String data, String mac) {
        try {
            // Горячий путь: без подробных логов

            // Парсим данные
            StateSummary stateSummary = dataParser.parseWeldingData(data, mac);

            // Подробные логи отключены для производительности

            // Обновляем локальное состояние (даже если сохранение в БД не удалось)
            deviceStates.put(mac, stateSummary);
            connectionStatus.put(mac, true);

            // WebSocket отключен - все устройства работают через polling API (как в archive проекте)
            // deviceController.sendDeviceState(stateSummary, mac);

            // Короткий лог
            // System.out.println("[DEVICE-MANAGER] ✅ Обновлено состояние для " + mac);

            // Троттлинг сохранений в БД: не чаще раза в 400мс на устройство
            long now = System.currentTimeMillis();
            Long lastSaved = lastSaveTimestampMs.getOrDefault(mac, 0L);
            if (now - lastSaved >= 400) {
                lastSaveTimestampMs.put(mac, now);
                // Сохраняем в БД асинхронно (выделенный executor)
                CompletableFuture.runAsync(() -> {
                    try {
                        stateService.saveMachineState(mac, stateSummary);
                    } catch (Exception dbError) {
                        System.err.println("[DEVICE-MANAGER] ⚠️ Ошибка сохранения в БД: " + dbError.getMessage());
                    }
                }, dbExecutor);
            }

        } catch (Exception e) {
            System.err.println("[DEVICE-MANAGER] ❌ Ошибка обработки данных от " + mac + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Получает статус подключения аппарата
     */
    public boolean isDeviceConnected(String mac) {
        StateSummary state = deviceStates.get(mac);
        if (state == null) {
            System.out.println("[DEVICE-MANAGER] isDeviceConnected для MAC: " + mac + " - нет данных");
            return false;
        }
        
        // Проверяем, есть ли данные в последние 10 секунд (как в archive проекте)
        long now = System.currentTimeMillis();
        long lastUpdate = state.getLastDatetimeUpdate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long timeDiff = now - lastUpdate;
        
        boolean connected = timeDiff < 10000; // 10 секунд (как в archive)
        System.out.println("[DEVICE-MANAGER] isDeviceConnected для MAC: " + mac + " - разница времени: " + timeDiff + "мс, подключен: " + connected);
        
        return connected;
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

        // Обновляем состояние как Offline
        StateSummary state = deviceStates.get(mac);
        if (state != null) {
            state.setStatus(WeldingMachineStatus.Offline);
            deviceStates.put(mac, state);

            // WebSocket отключен в текущей архитектуре (polling)
        }

        System.out.println("[DEVICE-MANAGER] Аппарат " + mac + " отмечен как отключенный");
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
        StateSummary state = deviceStates.get(mac);
        System.out.println("[DEVICE-MANAGER] getDeviceState для MAC: " + mac + ", результат: " + (state != null ? "найден" : "не найден"));
        if (state != null) {
            long now = System.currentTimeMillis();
            long lastUpdate = state.getLastDatetimeUpdate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long timeDiff = now - lastUpdate;
            System.out.println("[DEVICE-MANAGER] Время последнего обновления: " + lastUpdate + ", разница: " + timeDiff + "мс");
        }
        return state;
    }
    
    
} 