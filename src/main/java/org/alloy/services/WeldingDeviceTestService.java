package org.alloy.services;

import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.alloy.models.WeldingMachineStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WeldingDeviceTestService {
    
    @Value("${welding.test.enabled:false}")
    private boolean testEnabled;
    
    @Value("${welding.test.simulate_data:false}")
    private boolean simulateData;
    
    @Value("${welding.test.interval_ms:5000}")
    private long intervalMs;
    
    @Autowired
    private WeldingDeviceManagerService deviceManager;
    
    private ScheduledExecutorService scheduler;
    private Random random = new Random();
    private boolean isRunning = false;
    
    @PostConstruct
    public void start() {
        if (testEnabled && simulateData) {
            System.out.println("[TEST-SERVICE] 🧪 Запуск тестового режима для сварочного аппарата");
            System.out.println("[TEST-SERVICE] Интервал генерации данных: " + intervalMs + "мс");
            
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::generateTestData, 0, intervalMs, TimeUnit.MILLISECONDS);
            isRunning = true;
        }
    }
    
    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            isRunning = false;
            System.out.println("[TEST-SERVICE] Тестовый режим остановлен");
        }
    }
    
    private void generateTestData() {
        if (!isRunning) return;
        
        try {
            // Генерируем тестовые данные
            StateSummary testState = createTestState();
            
            // Отправляем данные через device manager
            deviceManager.processDeviceData(createTestRawData(), "8CAAB579425A");
            
            System.out.println("[TEST-SERVICE] 📊 Сгенерированы тестовые данные: " + testState.getStatus());
            
        } catch (Exception e) {
            System.err.println("[TEST-SERVICE] Ошибка генерации тестовых данных: " + e.getMessage());
        }
    }
    
    private StateSummary createTestState() {
        StateSummary state = new StateSummary();
        state.setDateCreated(LocalDateTime.now());
        state.setLastDatetimeUpdate(LocalDateTime.now());
        
        // Случайный статус
        WeldingMachineStatus[] statuses = {
            WeldingMachineStatus.Idle,
            WeldingMachineStatus.Welding,
            WeldingMachineStatus.Online,
            WeldingMachineStatus.Error
        };
        state.setStatus(statuses[random.nextInt(statuses.length)]);
        
        // Генерируем параметры
        Map<String, StateSummaryPropertyValue> properties = new HashMap<>();
        
        // Ток (150-200 А)
        addTestProperty(properties, "Current", String.valueOf(150 + random.nextInt(50)), "A");
        
        // Напряжение (20-25 В)
        addTestProperty(properties, "Voltage", String.valueOf(20 + random.nextInt(5)), "V");
        
        // Расход газа (15-20 л/мин)
        addTestProperty(properties, "GasFlow", String.valueOf(15 + random.nextInt(5)), "L/min");
        
        // Температура (20-80°C)
        addTestProperty(properties, "Temperature", String.valueOf(20 + random.nextInt(60)), "°C");
        
        // Модель
        addTestProperty(properties, "Model", "MP-500", "String");
        
        // Версия
        addTestProperty(properties, "Version", "2.1.4", "String");
        
        // Материал
        addTestProperty(properties, "Material", "Steel", "String");
        
        // Состояние управления
        String ctrlValue = state.getStatus() == WeldingMachineStatus.Welding ? "01" : 
                          state.getStatus() == WeldingMachineStatus.Error ? "02" : "00";
        addTestProperty(properties, "State.Ctrl", ctrlValue, "Control");
        
        state.setProperties(properties);
        return state;
    }
    
    private void addTestProperty(Map<String, StateSummaryPropertyValue> properties, 
                               String code, String value, String type) {
        StateSummaryPropertyValue prop = new StateSummaryPropertyValue();
        prop.setPropertyCode(code);
        prop.setValue(value);
        prop.setPropertyType(type);
        prop.setRawValue(value);
        properties.put(code, prop);
    }
    
    private String createTestRawData() {
        // Создаем тестовые сырые данные в формате :MAC;DATA
        StateSummary state = createTestState();
        StringBuilder data = new StringBuilder();
        data.append(":8CAAB579425A;");
        
        // Добавляем параметры в формате raw data
        data.append("CURRENT=").append(state.getProperties().get("Current").getValue()).append(";");
        data.append("VOLTAGE=").append(state.getProperties().get("Voltage").getValue()).append(";");
        data.append("GASFLOW=").append(state.getProperties().get("GasFlow").getValue()).append(";");
        data.append("TEMP=").append(state.getProperties().get("Temperature").getValue()).append(";");
        data.append("MODEL=").append(state.getProperties().get("Model").getValue()).append(";");
        data.append("VERSION=").append(state.getProperties().get("Version").getValue()).append(";");
        data.append("MATERIAL=").append(state.getProperties().get("Material").getValue()).append(";");
        data.append("CTRL=").append(state.getProperties().get("State.Ctrl").getValue());
        
        return data.toString();
    }
    
    public boolean isTestEnabled() {
        return testEnabled && simulateData;
    }
} 