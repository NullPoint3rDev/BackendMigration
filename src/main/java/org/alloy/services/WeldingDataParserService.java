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

        // Простой парсинг по позициям (можно улучшить с конфигурацией)
        if (payload.length() >= 2) {
            // Модель аппарата (первые 2 символа)
            addProperty(properties, "MODEL", payload.substring(0, 2), "number");
        }

        if (payload.length() >= 4) {
            // Версия прошивки (позиции 2-3)
            addProperty(properties, "VERSION", payload.substring(2, 4), "number");
        }

        if (payload.length() >= 6) {
            // Материал (позиции 4-5)
            addProperty(properties, "State.material", payload.substring(4, 6), "enum");
        }

        if (payload.length() >= 8) {
            // Ток (позиции 6-7)
            addProperty(properties, "State.I", payload.substring(6, 8), "number");
        }

        if (payload.length() >= 10) {
            // Напряжение (позиции 8-9)
            addProperty(properties, "State.U", payload.substring(8, 10), "number");
        }

        if (payload.length() >= 12) {
            // Расход газа (позиции 10-11)
            addProperty(properties, "State.GasFlow", payload.substring(10, 12), "number");
        }

        if (payload.length() >= 14) {
            // Температура (позиции 12-13)
            addProperty(properties, "State.Temperature", payload.substring(12, 14), "number");
        }

        if (payload.length() >= 16) {
            // Статус управления (позиции 14-15)
            addProperty(properties, "State.Ctrl", payload.substring(14, 16), "enum");
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