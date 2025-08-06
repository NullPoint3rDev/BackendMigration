package org.alloy.services;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WeldingDataParserService {

    public StateSummary parseWeldingData(String data, String mac) {
        System.out.println("[PARSER] 🔍 Парсинг данных: " + data);
        System.out.println("[PARSER] MAC: " + mac);
        
        StateSummary state = new StateSummary();
        state.setDateCreated(LocalDateTime.now());
        state.setLastDatetimeUpdate(LocalDateTime.now());
        
        String payload = extractPayload(data);
        if (payload != null) {
            System.out.println("[PARSER] 📦 Извлеченный payload: " + payload);
            System.out.println("[PARSER] 📏 Длина payload: " + payload.length());
            
            Map<String, StateSummaryPropertyValue> properties = parseParameters(payload);
            state.setProperties(properties);
            
            System.out.println("[PARSER] ✅ Парсинг завершен. Найдено свойств: " + properties.size());
            for (Map.Entry<String, StateSummaryPropertyValue> entry : properties.entrySet()) {
                System.out.println("[PARSER]   " + entry.getKey() + " = " + entry.getValue().getValue());
            }
        } else {
            System.out.println("[PARSER] ❌ Не удалось извлечь payload из данных");
        }
        
        state.setStatus(determineStatus(state.getProperties()));
        state.setErrorCode(determineErrorCode(state.getProperties()));
        
        System.out.println("[PARSER] 🎯 Итоговый статус: " + state.getStatus());
        
        return state;
    }

    private String extractPayload(String data) {
        int semicolonIndex = data.indexOf(';');
        if (semicolonIndex > 0 && semicolonIndex < data.length() - 1) {
            return data.substring(semicolonIndex + 1);
        }
        return null;
    }

    private Map<String, StateSummaryPropertyValue> parseParameters(String payload) {
        Map<String, StateSummaryPropertyValue> properties = new HashMap<>();

        System.out.println("[PARSER] 🔍 Анализ payload по позициям:");
        System.out.println("[PARSER] 📏 Общая длина: " + payload.length());
        
        // Логируем первые 50 символов для анализа
        System.out.println("[PARSER] 📋 Первые 50 символов: " + payload.substring(0, Math.min(50, payload.length())));
        
        // Анализируем структуру данных
        if (payload.length() >= 2) {
            String model = payload.substring(0, 2);
            System.out.println("[PARSER] 🏷️ Позиции 0-1 (MODEL): " + model);
            addProperty(properties, "MODEL", model, "number");
        }

        if (payload.length() >= 4) {
            String version = payload.substring(2, 4);
            System.out.println("[PARSER] 🔢 Позиции 2-3 (VERSION): " + version);
            addProperty(properties, "VERSION", version, "number");
        }

        if (payload.length() >= 6) {
            String material = payload.substring(4, 6);
            System.out.println("[PARSER] 🧱 Позиции 4-5 (MATERIAL): " + material);
            addProperty(properties, "State.material", material, "enum");
        }

        if (payload.length() >= 8) {
            String current = payload.substring(6, 8);
            System.out.println("[PARSER] ⚡ Позиции 6-7 (CURRENT): " + current);
            addProperty(properties, "State.I", current, "number");
        }

        if (payload.length() >= 10) {
            String voltage = payload.substring(8, 10);
            System.out.println("[PARSER] 🔌 Позиции 8-9 (VOLTAGE): " + voltage);
            addProperty(properties, "State.U", voltage, "number");
        }

        if (payload.length() >= 12) {
            String gasFlow = payload.substring(10, 12);
            System.out.println("[PARSER] 💨 Позиции 10-11 (GAS_FLOW): " + gasFlow);
            addProperty(properties, "State.GasFlow", gasFlow, "number");
        }

        if (payload.length() >= 14) {
            String temperature = payload.substring(12, 14);
            System.out.println("[PARSER] 🌡️ Позиции 12-13 (TEMPERATURE): " + temperature);
            addProperty(properties, "State.Temperature", temperature, "number");
        }

        if (payload.length() >= 16) {
            String control = payload.substring(14, 16);
            System.out.println("[PARSER] 🎛️ Позиции 14-15 (CONTROL): " + control);
            addProperty(properties, "State.Ctrl", control, "enum");
        }

        // Попробуем найти ток в других позициях
        if (payload.length() >= 20) {
            String possibleCurrent1 = payload.substring(16, 18);
            String possibleCurrent2 = payload.substring(18, 20);
            System.out.println("[PARSER] 🔍 Позиции 16-17 (возможный ток 1): " + possibleCurrent1);
            System.out.println("[PARSER] 🔍 Позиции 18-19 (возможный ток 2): " + possibleCurrent2);
        }

        if (payload.length() >= 24) {
            String possibleCurrent3 = payload.substring(20, 22);
            String possibleCurrent4 = payload.substring(22, 24);
            System.out.println("[PARSER] 🔍 Позиции 20-21 (возможный ток 3): " + possibleCurrent3);
            System.out.println("[PARSER] 🔍 Позиции 22-23 (возможный ток 4): " + possibleCurrent4);
        }

        // Попробуем найти ток в позициях, где он может быть (на основе анализа данных)
        if (payload.length() >= 40) {
            // Позиции 30-31 могут содержать ток
            String possibleCurrent5 = payload.substring(30, 32);
            String possibleCurrent6 = payload.substring(32, 34);
            System.out.println("[PARSER] 🔍 Позиции 30-31 (возможный ток 5): " + possibleCurrent5);
            System.out.println("[PARSER] 🔍 Позиции 32-33 (возможный ток 6): " + possibleCurrent6);
            
            // Попробуем интерпретировать как ток
            try {
                int currentValue5 = Integer.parseInt(possibleCurrent5, 16);
                int currentValue6 = Integer.parseInt(possibleCurrent6, 16);
                System.out.println("[PARSER] 🔢 Позиции 30-31 как число: " + currentValue5);
                System.out.println("[PARSER] 🔢 Позиции 32-33 как число: " + currentValue6);
            } catch (NumberFormatException e) {
                System.out.println("[PARSER] ⚠️ Не удалось преобразовать в число");
            }
        }

        // Попробуем найти ток в позициях 40-50
        if (payload.length() >= 50) {
            String possibleCurrent7 = payload.substring(40, 42);
            String possibleCurrent8 = payload.substring(42, 44);
            System.out.println("[PARSER] 🔍 Позиции 40-41 (возможный ток 7): " + possibleCurrent7);
            System.out.println("[PARSER] 🔍 Позиции 42-43 (возможный ток 8): " + possibleCurrent8);
        }

        return properties;
    }

    private void addProperty(Map<String, StateSummaryPropertyValue> properties, 
                           String propertyCode, String value, String propertyType) {
        StateSummaryPropertyValue prop = new StateSummaryPropertyValue();
        prop.setPropertyCode(propertyCode);
        prop.setValue(value);
        prop.setPropertyType(propertyType);
        prop.setRawValue(value);
        properties.put(propertyCode, prop);
    }

    private WeldingMachineStatus determineStatus(Map<String, StateSummaryPropertyValue> properties) {
        StateSummaryPropertyValue ctrlProp = properties.get("State.Ctrl");
        if (ctrlProp != null) {
            String ctrlValue = ctrlProp.getValue();
            switch (ctrlValue) {
                case "00":
                    return WeldingMachineStatus.Idle;
                case "01":
                    return WeldingMachineStatus.Welding;
                case "02":
                    return WeldingMachineStatus.Error;
                default:
                    return WeldingMachineStatus.Offline;
            }
        }
        return WeldingMachineStatus.Offline;
    }

    private String determineErrorCode(Map<String, StateSummaryPropertyValue> properties) {
        StateSummaryPropertyValue ctrlProp = properties.get("State.Ctrl");
        if (ctrlProp != null && "02".equals(ctrlProp.getValue())) {
            // Если статус ошибки, возвращаем код ошибки
            StateSummaryPropertyValue errorProp = properties.get("State.Error");
            if (errorProp != null) {
                return errorProp.getValue();
            }
        }
        return null;
    }
} 