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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
public class WeldingDeviceManagerService {

    @Autowired
    private WeldingDataParserService dataParser;

    @Autowired
    private WeldingMachineStateService stateService;

    @Autowired
    private DeviceController deviceController;

    @Autowired
    private DeviceMessageHistoryService messageHistoryService;

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
            // Парсим данные (БЕЗ ЛОГИРОВАНИЯ!)
            StateSummary stateSummary = dataParser.parseWeldingData(data, mac);
            
            // Обновляем локальное состояние (даже если сохранение в БД не удалось)
            deviceStates.put(mac, stateSummary);
            connectionStatus.put(mac, true);
            
            // Отправляем через WebSocket с MAC адресом (ПРИОРИТЕТ!)
            deviceController.sendDeviceState(stateSummary, mac);
            
            // Сохраняем в базу данных СИНХРОННО (исправляем утечку соединений)
            try {
                stateService.saveMachineState(mac, stateSummary);
                messageHistoryService.addMessage(mac, data, "received");
            } catch (Exception dbError) {
                // Молча игнорируем ошибки БД
            }

        } catch (Exception e) {
            System.err.println("[DEVICE-MANAGER] ❌ Ошибка обработки данных от " + mac + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Получает текущее состояние аппарата
     */
    @Cacheable(value = "deviceStates", key = "#mac")
    public StateSummary getDeviceState(String mac) {
        return deviceStates.get(mac);
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
            deviceController.sendDeviceState(state);
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
} 