package org.alloy.services;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Парсер пакетов в стиле archive проекта
 * Обрабатывает только блоки мониторинга, Core устройства обрабатываются отдельно
 */
@Service
public class ArchiveStylePacketParser {
    // Отдельный логгер для сырых CORE-посылок (маршрутизируется в отдельный файл конфигом)
    private static final org.slf4j.Logger CORE_RAW_LOG = org.slf4j.LoggerFactory.getLogger("org.alloy.core.raw");
    
    @Value("${welding.archive.parser.debug:false}")
    private boolean debugMode;
    
    @Autowired
    private DeviceModelService deviceModelService;

    @Autowired
    private WeldingDeviceManagerService deviceManager;
    
    /**
     * Обработка пакета в стиле archive
     */
    public void processPacket(ArchivePacket packet) {
        if (packet == null || packet.getData() == null || packet.getMac() == null) {
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] 🔍 Обработка пакета от " + packet.getMac() + " в " + startTime);
                System.out.println("[ARCHIVE-PARSER] 📦 Данные: " + packet.getData());
            }
            
            // Core устройства: сразу передаем данные в общий менеджер (как в archive, без WS)
            if (deviceModelService.shouldUseCoreParser(packet.getMac(), packet.getData())) {
                if (debugMode) {
                    System.out.println("[ARCHIVE-PARSER] 🔄 Передача CORE пакета напрямую в deviceManager: " + packet.getMac());
                }
                // Логируем сырую CORE-посылку в отдельный файл
                try {
                    CORE_RAW_LOG.info("mac={}, data={}", packet.getMac(), packet.getData());
                } catch (Exception ignore) {}
                deviceManager.processDeviceData(packet.getData(), packet.getMac());
                return;
            }
            
            // Парсим данные блока мониторинга
            StateSummary stateSummary = parseMonitoringBlockData(packet.getData(), packet.getMac());
            
            if (stateSummary != null) {
                if (debugMode && stateSummary.getProperties() != null) {
                    System.out.println("[ARCHIVE-PARSER] 📊 Результат парсинга:");
                    for (Map.Entry<String, StateSummaryPropertyValue> entry : stateSummary.getProperties().entrySet()) {
                        System.out.println("[ARCHIVE-PARSER]   " + entry.getKey() + " = " + entry.getValue().getValue());
                    }
                }
                
                // Передаем в существующий менеджер устройств
                long beforeDeviceManager = System.currentTimeMillis();
                if (debugMode) {
                    System.out.println("[ARCHIVE-PARSER] 🔄 Передача данных в deviceManager для MAC: " + packet.getMac() + " в " + beforeDeviceManager);
                }
                deviceManager.processDeviceData(packet.getData(), packet.getMac());
                long afterDeviceManager = System.currentTimeMillis();
                
                if (debugMode) {
                    System.out.println("[ARCHIVE-PARSER] ✅ Пакет от " + packet.getMac() + " обработан. Время обработки: " + (afterDeviceManager - startTime) + "ms, deviceManager: " + (afterDeviceManager - beforeDeviceManager) + "ms");
                }
            } else {
                System.out.println("[ARCHIVE-PARSER] ❌ Не удалось распарсить данные от " + packet.getMac());
            }
            
        } catch (Exception e) {
            System.err.println("[ARCHIVE-PARSER] ❌ Ошибка обработки пакета от " + packet.getMac() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Парсинг данных блока мониторинга
     */
    private StateSummary parseMonitoringBlockData(String data, String mac) {
        StateSummary state = new StateSummary();
        state.setDateCreated(LocalDateTime.now());
        state.setLastDatetimeUpdate(LocalDateTime.now());
        
        // Извлекаем payload из данных
        String payload = extractPayload(data);
        if (payload == null || payload.isEmpty()) {
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] ❌ Не удалось извлечь payload из данных");
            }
            return null;
        }
        
        if (debugMode) {
            System.out.println("[ARCHIVE-PARSER] 📦 Извлеченный payload: " + payload);
            System.out.println("[ARCHIVE-PARSER] 📏 Длина payload: " + payload.length());
        }
        
        // Парсим параметры
        Map<String, StateSummaryPropertyValue> properties = parseMonitoringBlockParameters(payload);
        state.setProperties(properties);
        
        // Определяем статус
        state.setStatus(determineStatus(properties));
        state.setErrorCode(determineErrorCode(properties));
        
        if (debugMode) {
            System.out.println("[ARCHIVE-PARSER] 🎯 Итоговый статус: " + state.getStatus());
        }
        
        return state;
    }
    
    /**
     * Извлечение payload из данных
     * Формат: :MAC;payload
     */
    private String extractPayload(String data) {
        int semicolonIndex = data.indexOf(';');
        if (semicolonIndex > 0 && semicolonIndex < data.length() - 1) {
            return data.substring(semicolonIndex + 1);
        }
        return null;
    }
    
    /**
     * Парсинг параметров блока мониторинга
     */
    private Map<String, StateSummaryPropertyValue> parseMonitoringBlockParameters(String payload) {
        Map<String, StateSummaryPropertyValue> properties = new HashMap<>();
        
        if (debugMode) {
            System.out.println("[ARCHIVE-PARSER] 🔍 Анализ payload по позициям:");
            System.out.println("[ARCHIVE-PARSER] 📏 Общая длина: " + payload.length());
        }
        
        // Парсим основные поля по позициям (как в оригинальном парсере)
        if (payload.length() >= 2) {
            String model = payload.substring(0, 2);
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] 🏷️ Позиции 0-1 (MODEL): " + model);
            }
            addProperty(properties, "MODEL", model, "number");
        }
        
        if (payload.length() >= 4) {
            String version = payload.substring(2, 4);
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] 🔢 Позиции 2-3 (VERSION): " + version);
            }
            addProperty(properties, "VERSION", version, "number");
        }
        
        if (payload.length() >= 6) {
            String material = payload.substring(4, 6);
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] 🧱 Позиции 4-5 (MATERIAL): " + material);
            }
            addProperty(properties, "State.material", material, "enum");
        }
        
        // Автоматический поиск тока (как в оригинальном парсере)
        int bestCurrentPosition = findBestCurrentPosition(payload);
        if (bestCurrentPosition >= 0) {
            String current = payload.substring(bestCurrentPosition, bestCurrentPosition + 2);
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] ⚡ Найден ток в позиции " + bestCurrentPosition + ": " + current);
            }
            addProperty(properties, "State.I", current, "number");
            
            // Напряжение ищем рядом с током
            String voltage = findVoltageNearCurrent(payload, bestCurrentPosition);
            addProperty(properties, "State.U", voltage, "number");
        } else {
            // Fallback: используем фиксированные позиции
            if (payload.length() >= 76) {
                String current = payload.substring(72, 74);
                String voltage = payload.substring(74, 76);
                
                if (debugMode) {
                    System.out.println("[ARCHIVE-PARSER] 🔧 Используем фиксированные позиции:");
                    System.out.println("[ARCHIVE-PARSER]   State.I = " + current + " (позиции 72-73)");
                    System.out.println("[ARCHIVE-PARSER]   State.U = " + voltage + " (позиции 74-75)");
                }
                
                addProperty(properties, "State.I", current, "number");
                addProperty(properties, "State.U", voltage, "number");
            }
        }
        
        // Дополнительные поля
        if (payload.length() >= 12) {
            String gasFlow = payload.substring(10, 12);
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] 💨 Позиции 10-11 (GAS_FLOW): " + gasFlow);
            }
            addProperty(properties, "State.GasFlow", gasFlow, "number");
        }
        
        if (payload.length() >= 14) {
            String temperature = payload.substring(12, 14);
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] 🌡️ Позиции 12-13 (TEMPERATURE): " + temperature);
            }
            addProperty(properties, "State.Temperature", temperature, "number");
        }
        
        if (payload.length() >= 16) {
            String control = payload.substring(14, 16);
            if (debugMode) {
                System.out.println("[ARCHIVE-PARSER] 🎛️ Позиции 14-15 (CONTROL): " + control);
            }
            addProperty(properties, "State.Ctrl", control, "enum");
        }
        
        return properties;
    }
    
    /**
     * Поиск лучшей позиции для тока
     */
    private int findBestCurrentPosition(String payload) {
        int bestPosition = -1;
        int minDifference = Integer.MAX_VALUE;
        
        for (int i = 0; i < payload.length() - 1; i += 2) {
            if (i + 1 < payload.length()) {
                try {
                    String value = payload.substring(i, i + 2);
                    int numValue = Integer.parseInt(value, 16);
                    
                    // Ищем значения в диапазоне 100-120 (ток 104-111 А)
                    if (numValue >= 100 && numValue <= 120) {
                        int difference = Math.abs(numValue - 108);
                        if (difference < minDifference) {
                            minDifference = difference;
                            bestPosition = i;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Пропускаем нечисловые значения
                }
            }
        }
        
        return bestPosition;
    }
    
    /**
     * Поиск напряжения рядом с током
     */
    private String findVoltageNearCurrent(String payload, int currentPosition) {
        // Ищем 2-байтовые значения в разумном диапазоне 150..500 (15.0..50.0 В)
        int bestVoltageWordPos = -1;
        int bestVoltageWord = -1;
        int minVoltageWordDiff = Integer.MAX_VALUE;
        
        for (int i = 0; i + 3 < payload.length(); i += 2) {
            try {
                int hi = Integer.parseInt(payload.substring(i, i + 2), 16);
                int lo = Integer.parseInt(payload.substring(i + 2, i + 4), 16);
                int word = (hi << 8) | lo; // Big-endian
                
                if (word >= 150 && word <= 500) { // 15.0..50.0 В
                    int diff = Math.abs(word - 270); // около 27.0 В
                    if (diff < minVoltageWordDiff) {
                        minVoltageWordDiff = diff;
                        bestVoltageWordPos = i;
                        bestVoltageWord = word;
                    }
                }
            } catch (NumberFormatException ignore) {
                // Пропускаем
            }
        }
        
        if (bestVoltageWordPos >= 0) {
            // Переводим десятые В в целые В
            int volts = Math.round(bestVoltageWord / 10.0f);
            return toHex(volts);
        } else {
            // Fallback: используем байт после тока
            int voltageStart = currentPosition + 2;
            if (voltageStart + 2 <= payload.length()) {
                return payload.substring(voltageStart, voltageStart + 2);
            } else if (payload.length() >= 80) {
                return payload.substring(78, 80);
            } else {
                return "00";
            }
        }
    }
    
    /**
     * Добавление свойства
     */
    private void addProperty(Map<String, StateSummaryPropertyValue> properties, 
                           String propertyCode, String value, String propertyType) {
        StateSummaryPropertyValue prop = new StateSummaryPropertyValue();
        prop.setPropertyCode(propertyCode);
        prop.setValue(value);
        prop.setPropertyType(propertyType);
        prop.setRawValue(value);
        properties.put(propertyCode, prop);
    }
    
    /**
     * Преобразование в hex
     */
    private String toHex(int value) {
        String hex = Integer.toHexString(value).toUpperCase();
        if (hex.length() % 2 != 0) hex = "0" + hex;
        return hex;
    }
    
    /**
     * Определение статуса
     */
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
    
    /**
     * Определение кода ошибки
     */
    private String determineErrorCode(Map<String, StateSummaryPropertyValue> properties) {
        StateSummaryPropertyValue ctrlProp = properties.get("State.Ctrl");
        if (ctrlProp != null && "02".equals(ctrlProp.getValue())) {
            StateSummaryPropertyValue errorProp = properties.get("State.Error");
            if (errorProp != null) {
                return errorProp.getValue();
            }
        }
        return null;
    }
}
