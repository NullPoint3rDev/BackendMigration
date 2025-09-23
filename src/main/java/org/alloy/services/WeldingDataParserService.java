package org.alloy.services;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WeldingDataParserService {

    @Value("${welding.parser.current_position:6}")
    private int currentPosition;

    @Value("${welding.parser.debug_mode:true}")
    private boolean debugMode;

    public StateSummary parseWeldingData(String data, String mac) {
        System.out.println("[PARSER] 🚀 НАЧАЛО ПАРСИНГА");
        System.out.println("[PARSER] 🔍 Парсинг данных: " + data);
        System.out.println("[PARSER] MAC: " + mac);
        
        if (debugMode) {
            System.out.println("[PARSER] 🔍 Парсинг данных: " + data);
            System.out.println("[PARSER] MAC: " + mac);
        }
        
        StateSummary state = new StateSummary();
        state.setDateCreated(LocalDateTime.now());
        state.setLastDatetimeUpdate(LocalDateTime.now());
        
        String payload = extractPayload(data);
        if (payload != null) {
            System.out.println("[PARSER] 📦 Извлеченный payload: " + payload);
            System.out.println("[PARSER] 📏 Длина payload: " + payload.length());
            if (debugMode) {
                System.out.println("[PARSER] 📦 Извлеченный payload: " + payload);
                System.out.println("[PARSER] 📏 Длина payload: " + payload.length());
            }
            
            Map<String, StateSummaryPropertyValue> properties = parseParameters(payload);
            state.setProperties(properties);
            
            if (debugMode) {
                System.out.println("[PARSER] ✅ Парсинг завершен. Найдено свойств: " + properties.size());
                for (Map.Entry<String, StateSummaryPropertyValue> entry : properties.entrySet()) {
                    System.out.println("[PARSER]   " + entry.getKey() + " = " + entry.getValue().getValue());
                }
            }
        } else {
            System.out.println("[PARSER] ❌ Не удалось извлечь payload из данных");
        }
        
        state.setStatus(determineStatus(state.getProperties()));
        state.setErrorCode(determineErrorCode(state.getProperties()));
        
        if (debugMode) {
            System.out.println("[PARSER] 🎯 Итоговый статус: " + state.getStatus());
        }
        
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

        if (debugMode) {
            System.out.println("[PARSER] 🔍 Анализ payload по позициям:");
            System.out.println("[PARSER] 📏 Общая длина: " + payload.length());
            
            // Логируем весь payload для анализа
            System.out.println("[PARSER] 📋 Полный payload: " + payload);
        }
        
        // Анализируем каждые 2 символа для поиска возможного тока
        if (debugMode) {
            System.out.println("[PARSER] 🔍 Поиск возможных значений тока:");
            int bestCurrentPosition = -1;
            int bestCurrentValue = -1;
            int minDifference = Integer.MAX_VALUE;
            
            for (int i = 0; i < payload.length() - 1; i += 2) {
                if (i + 1 < payload.length()) {
                    String value = payload.substring(i, i + 2);
                    System.out.println("[PARSER]   Позиции " + i + "-" + (i+1) + ": " + value);
                    
                    // Попробуем интерпретировать как число
                    try {
                        int numValue = Integer.parseInt(value, 16);
                        System.out.println("[PARSER]     Как число (hex): " + numValue);
                        
                        // Если это число в диапазоне 50-200, это может быть ток
                        if (numValue >= 50 && numValue <= 200) {
                            System.out.println("[PARSER]     ⚡ ВОЗМОЖНЫЙ ТОК! Значение: " + numValue);
                            
                            // Ищем значение, наиболее близкое к 65
                            int difference = Math.abs(numValue - 65);
                            if (difference < minDifference) {
                                minDifference = difference;
                                bestCurrentPosition = i;
                                bestCurrentValue = numValue;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Не число, пропускаем
                    }
                }
            }
            
            if (bestCurrentPosition >= 0) {
                System.out.println("[PARSER] 🎯 НАИЛУЧШИЙ КАНДИДАТ НА ТОК:");
                System.out.println("[PARSER]    Позиция: " + bestCurrentPosition + "-" + (bestCurrentPosition+1));
                System.out.println("[PARSER]    Значение: " + bestCurrentValue);
                System.out.println("[PARSER]    Разница с 65: " + minDifference);
            }
        }
        
        // Original parsing logic with added logging
        if (payload.length() >= 2) {
            String model = payload.substring(0, 2);
            if (debugMode) {
                System.out.println("[PARSER] 🏷️ Позиции 0-1 (MODEL): " + model);
            }
            addProperty(properties, "MODEL", model, "number");
        }
        if (payload.length() >= 4) {
            String version = payload.substring(2, 4);
            if (debugMode) {
                System.out.println("[PARSER] 🔢 Позиции 2-3 (VERSION): " + version);
            }
            addProperty(properties, "VERSION", version, "number");
        }
        if (payload.length() >= 6) {
            String material = payload.substring(4, 6);
            if (debugMode) {
                System.out.println("[PARSER] 🧱 Позиции 4-5 (MATERIAL): " + material);
            }
            addProperty(properties, "State.material", material, "enum");
        }
        // Старые позиции тока (неправильные) - закомментированы
        // if (payload.length() >= 8) {
        //     String current = payload.substring(6, 8);
        //     if (debugMode) {
        //         System.out.println("[PARSER] ⚡ Позиции 6-7 (CURRENT): " + current);
        //     }
        //     addProperty(properties, "State.I", current, "number");
        // }
        
        // Конфигурируемая позиция тока отключена - используем фиксированные позиции
        // if (payload.length() >= currentPosition + 2) {
        //     String configurableCurrent = payload.substring(currentPosition, currentPosition + 2);
        //     if (debugMode) {
        //         System.out.println("[PARSER] ⚡ Конфигурируемая позиция " + currentPosition + "-" + (currentPosition+1) + " (CURRENT): " + configurableCurrent);
        //         try {
        //             int currentValue = Integer.parseInt(configurableCurrent, 16);
        //             System.out.println("[PARSER] 🔢 Значение тока как число: " + currentValue);
        //         } catch (NumberFormatException e) {
        //             System.out.println("[PARSER] ⚠️ Не удалось преобразовать ток в число");
        //         }
        //     }
        //     // Обновляем значение тока на конфигурируемое
        //     addProperty(properties, "State.I", configurableCurrent, "number");
        // }
        
        // Автоматический поиск тока отключен - используем фиксированные позиции
        // if (debugMode) {
        //     // Повторяем поиск лучшей позиции для автоматического использования
        //     int bestCurrentPosition = -1;
        //     int bestCurrentValue = -1;
        //     int minDifference = Integer.MAX_VALUE;
        //     
        //     for (int i = 0; i < payload.length() - 1; i += 2) {
        //         if (i + 1 < payload.length()) {
        //             try {
        //                 String value = payload.substring(i, i + 2);
        //                 int numValue = Integer.parseInt(value, 16);
        //                 
        //                 if (numValue >= 50 && numValue <= 200) {
        //                     int difference = Math.abs(numValue - 65);
        //                     if (difference < minDifference) {
        //                         minDifference = difference;
        //                         bestCurrentPosition = i;
        //                         bestCurrentValue = numValue;
        //                     }
        //                 }
        //             } catch (NumberFormatException e) {
        //                 // Пропускаем
        //             }
        //         }
        //     }
        //     
        //     if (bestCurrentPosition >= 0) {
        //         String bestCurrent = payload.substring(bestCurrentPosition, bestCurrentPosition + 2);
        //         System.out.println("[PARSER] 🔄 Автоматически используем лучшую позицию тока: " + bestCurrentPosition + "-" + (bestCurrentPosition+1) + " = " + bestCurrentValue);
        //         addProperty(properties, "State.I", bestCurrent, "number");
        //     }
        // }
        // Старые позиции напряжения (неправильные) - закомментированы
        // if (payload.length() >= 10) {
        //     String voltage = payload.substring(8, 10);
        //     if (debugMode) {
        //         System.out.println("[PARSER] 🔌 Позиции 8-9 (VOLTAGE): " + voltage);
        //     }
        //     addProperty(properties, "State.U", voltage, "number");
        // }
        if (payload.length() >= 12) {
            String gasFlow = payload.substring(10, 12);
            if (debugMode) {
                System.out.println("[PARSER] 💨 Позиции 10-11 (GAS_FLOW): " + gasFlow);
            }
            addProperty(properties, "State.GasFlow", gasFlow, "number");
        }
        if (payload.length() >= 14) {
            String temperature = payload.substring(12, 14);
            if (debugMode) {
                System.out.println("[PARSER] 🌡️ Позиции 12-13 (TEMPERATURE): " + temperature);
            }
            addProperty(properties, "State.Temperature", temperature, "number");
        }
        if (payload.length() >= 16) {
            String control = payload.substring(14, 16);
            if (debugMode) {
                System.out.println("[PARSER] 🎛️ Позиции 14-15 (CONTROL): " + control);
            }
            addProperty(properties, "State.Ctrl", control, "enum");
        }

        // Попробуем найти ток в других позициях
        if (payload.length() >= 20) {
            String possibleCurrent1 = payload.substring(16, 18);
            String possibleCurrent2 = payload.substring(18, 20);
            if (debugMode) {
                System.out.println("[PARSER] 🔍 Позиции 16-17 (возможный ток 1): " + possibleCurrent1);
                System.out.println("[PARSER] 🔍 Позиции 18-19 (возможный ток 2): " + possibleCurrent2);
            }
        }

        if (payload.length() >= 24) {
            String possibleCurrent3 = payload.substring(20, 22);
            String possibleCurrent4 = payload.substring(22, 24);
            if (debugMode) {
                System.out.println("[PARSER] 🔍 Позиции 20-21 (возможный ток 3): " + possibleCurrent3);
                System.out.println("[PARSER] 🔍 Позиции 22-23 (возможный ток 4): " + possibleCurrent4);
            }
        }

        // ПРАВИЛЬНЫЕ ПОЗИЦИИ ТОКА И НАПРЯЖЕНИЯ (на основе анализа данных)
        System.out.println("[PARSER] 🔍 Проверяем длину payload: " + payload.length() + " >= 74?");
        if (payload.length() >= 74) {
            System.out.println("[PARSER] ✅ Длина достаточная, извлекаем ток и напряжение");
            // Позиции 70-71 содержат ТОК
            String current = payload.substring(70, 72);
            // Позиции 72-73 содержат НАПРЯЖЕНИЕ  
            String voltage = payload.substring(72, 74);
            
            System.out.println("[PARSER] 🔍 Извлеченные значения:");
            System.out.println("[PARSER]   Позиции 70-71 (ток): '" + current + "'");
            System.out.println("[PARSER]   Позиции 72-73 (напряжение): '" + voltage + "'");
            
            // Логируем весь payload с позициями для поиска правильных значений
            System.out.println("[PARSER] 🔍 ПОЛНЫЙ PAYLOAD С ПОЗИЦИЯМИ:");
            for (int i = 0; i < payload.length(); i += 20) {
                int end = Math.min(i + 20, payload.length());
                String section = payload.substring(i, end);
                StringBuilder positions = new StringBuilder();
                for (int j = i; j < end; j++) {
                    positions.append(String.format("%2d", j)).append(" ");
                }
                System.out.println("[PARSER]   " + String.format("%3d", i) + "-" + String.format("%3d", end-1) + ": " + section);
                System.out.println("[PARSER]   Позиции: " + positions.toString());
            }
            
            if (debugMode) {
                System.out.println("[PARSER] ⚡ Позиции 70-71 (ТОК): " + current);
                System.out.println("[PARSER] 🔌 Позиции 72-73 (НАПРЯЖЕНИЕ): " + voltage);
                
                // Попробуем интерпретировать как числа
                try {
                    int currentValue = Integer.parseInt(current, 16);
                    int voltageValue = Integer.parseInt(voltage, 16);
                    System.out.println("[PARSER] 🔢 Ток как число: " + currentValue + " А");
                    System.out.println("[PARSER] 🔢 Напряжение как число: " + voltageValue + " В");
                } catch (NumberFormatException e) {
                    System.out.println("[PARSER] ⚠️ Не удалось преобразовать в число");
                }
            }
            
            // Устанавливаем правильные значения тока и напряжения
            System.out.println("[PARSER] 🔧 Устанавливаем State.I = " + current);
            System.out.println("[PARSER] 🔧 Устанавливаем State.U = " + voltage);
            addProperty(properties, "State.I", current, "number");
            addProperty(properties, "State.U", voltage, "number");
        } else {
            System.out.println("[PARSER] ❌ Длина payload недостаточная: " + payload.length() + " < 74");
        }

        // Попробуем найти ток в позициях 40-50
        if (payload.length() >= 50) {
            String possibleCurrent7 = payload.substring(40, 42);
            String possibleCurrent8 = payload.substring(42, 44);
            if (debugMode) {
                System.out.println("[PARSER] 🔍 Позиции 40-41 (возможный ток 7): " + possibleCurrent7);
                System.out.println("[PARSER] 🔍 Позиции 42-43 (возможный ток 8): " + possibleCurrent8);
            }
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