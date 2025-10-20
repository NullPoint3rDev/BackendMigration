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

    @PostConstruct
    public void init() {
        System.out.println("[DEVICE-MANAGER] Сервис управления аппаратами инициализирован");
    }

    /**
     * Обрабатывает данные от сварочного аппарата
     */
    public void processDeviceData(String data, String mac) {
        try {
            System.out.println("[DEVICE-MANAGER] 🔍 Начинаем обработку данных от " + mac);
            System.out.println("[DEVICE-MANAGER] 📦 Данные: " + data);

            // Парсим данные
            StateSummary stateSummary = dataParser.parseWeldingData(data, mac);

            System.out.println("[DEVICE-MANAGER] 📊 Результат парсинга:");
            if (stateSummary.getProperties() != null) {
                for (Map.Entry<String, StateSummaryPropertyValue> entry : stateSummary.getProperties().entrySet()) {
                    System.out.println("[DEVICE-MANAGER]   " + entry.getKey() + " = " + entry.getValue().getValue());
                }
            }

            // Обновляем локальное состояние (даже если сохранение в БД не удалось)
            deviceStates.put(mac, stateSummary);
            connectionStatus.put(mac, true);

            // WebSocket отключен - все устройства работают через polling API (как в archive проекте)
            // deviceController.sendDeviceState(stateSummary, mac);

            System.out.println("[DEVICE-MANAGER] ✅ Данные от аппарата " + mac + " обработаны");

            // Сохраняем в БД асинхронно (не блокируем основной поток)
            CompletableFuture.runAsync(() -> {
                try {
                    stateService.saveMachineState(mac, stateSummary);
                    System.out.println("[DEVICE-MANAGER] ✅ Данные сохранены в базу данных");
                } catch (Exception dbError) {
                    System.err.println("[DEVICE-MANAGER] ⚠️ Ошибка сохранения в БД: " + dbError.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("[DEVICE-MANAGER] ❌ Ошибка обработки данных от " + mac + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Получает статус подключения аппарата
     */
    public boolean isDeviceConnected(String mac) {
        return connectionStatus.getOrDefault(mac, false);
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

            // Отправляем обновление через WebSocket
            deviceController.sendDeviceState(state, mac);
        }

        System.out.println("[DEVICE-MANAGER] Аппарат " + mac + " отмечен как отключенный");
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
        return deviceStates.get(mac);
    }
    
} 