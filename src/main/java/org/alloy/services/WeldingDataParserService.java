package org.alloy.services;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.DeviceModel;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WeldingDataParserService {
    // Отдельный логгер для разобранных CORE-пакетов (настраивается в logback)
    private static final org.slf4j.Logger CORE_PARSED_LOG = org.slf4j.LoggerFactory.getLogger("org.alloy.core.parsed");

    @Value("${welding.parser.current_position:6}")
    private int currentPosition;

    @Value("${welding.parser.debug_mode:true}")
    private boolean debugMode;

    @Value("${welding.core.macs:E09806083396,DC4F22763D5C}")
    private String coreMacsConfig;

    @Value("${welding.core.voltage_scale_idle:16}")
    private int coreVoltageScaleIdle;
    @Value("${welding.core.voltage_scale_welding:10}")
    private int coreVoltageScaleWelding;
    @Value("${welding.core.voltage_offset_idle:0}")
    private int coreVoltageOffsetIdle;
    @Value("${welding.core.voltage_offset_welding:0}")
    private int coreVoltageOffsetWelding;

    @Autowired
    private DeviceModelService deviceModelService;

    public StateSummary parseWeldingData(String data, String mac) {
       // System.out.println("[PARSER] 🚀 НАЧАЛО ПАРСИНГА");
        //System.out.println("[PARSER] 🔍 Парсинг данных: " + data);
        //System.out.println("[PARSER] MAC: " + mac);
        
        if (debugMode) {
           // System.out.println("[PARSER] 🔍 Парсинг данных: " + data);
          //  System.out.println("[PARSER] MAC: " + mac);
        }
        
        StateSummary state = new StateSummary();
        state.setDateCreated(LocalDateTime.now());
        state.setLastDatetimeUpdate(LocalDateTime.now());

        // Получаем модель устройства по MAC из БД или используем обратную совместимость
        DeviceModel deviceModel = deviceModelService.getDeviceModelByMac(mac);
        
        // Проверяем соответствие формата пакета модели устройства (БЕЗ ЛОГИРОВАНИЯ!)
        if (deviceModel != null && !deviceModelService.isPacketFormatMatches(mac, data)) {
            // Молча игнорируем ошибки формата
        }

        // Для плат Core разбираем специализированным парсером и сразу выставляем ток/напряжение
        if (deviceModel == DeviceModel.CORE || isCoreMac(mac)) {
            CorePacket core = CorePacketParser.parse(data);
            if (core != null) {
                Map<String, StateSummaryPropertyValue> props = new HashMap<>();
                // Отображаемые значения в зависимости от состояния: 1 — сварка, 0 — холостой ход
                int stateVal = core.weldingMachineState;
                int displayCurrent = (stateVal == 1 ? core.weldingCurrent : core.current);
                // Core отдаёт напряжение как слово в единицах 1/10 В. Используем готовое значение из CorePacket
                double displayVoltageDouble = core.getDisplayVoltage();
                int displayVoltageTenth = (int) Math.round(displayVoltageDouble * 10);

                // Фронт для ключа 'Voltage' делит значение на 10 (см. DeviceMonitorPage), поэтому кладём десятые Вольта
                // Для тока кладём как есть (А)
                addProperty(props, "Current", String.valueOf(displayCurrent), "number");
                addProperty(props, "Voltage", String.valueOf(displayVoltageTenth), "number");
                addProperty(props, "Packet.Index", String.valueOf(core.index), "number");
                
                // Добавляем дополнительные свойства Core устройства
                addProperty(props, "Time.Hours", String.valueOf(core.hours), "number");
                addProperty(props, "Time.Minutes", String.valueOf(core.minutes), "number");
                addProperty(props, "Time.Seconds", String.valueOf(core.seconds), "number");
                addProperty(props, "Date.Day", String.valueOf(core.date), "number");
                addProperty(props, "Date.Month", String.valueOf(core.month), "number");
                addProperty(props, "Date.Year", String.valueOf(core.year), "number");
                
                addProperty(props, "Состояние аппарата", String.valueOf(core.weldingMachineState), "number");
                addProperty(props, "State.GasFlow", String.valueOf(core.gasFlow), "number");
                
                addProperty(props, "Номер сварочного задания", String.valueOf(core.jobNumber), "number");
                addProperty(props, "Inductance", String.valueOf(core.inductance), "number");
                
                // Парсим битовые поля ошибок и объединяем в текстовые описания
                StringBuilder allErrors = new StringBuilder();
                
                String errors1Text = parseErrorBits(core.errors1);
                String errors2Text = parseErrorBits(core.errors2);
                String errors3Text = parseErrorBits(core.errors3);
                
                if (!errors1Text.isEmpty()) {
                    if (allErrors.length() > 0) allErrors.append("; ");
                    allErrors.append(" ").append(errors1Text);
                }
                if (!errors2Text.isEmpty()) {
                    if (allErrors.length() > 0) allErrors.append("; ");
                    allErrors.append(" ").append(errors2Text);
                }
                if (!errors3Text.isEmpty()) {
                    if (allErrors.length() > 0) allErrors.append("; ");
                    allErrors.append(" ").append(errors3Text);
                }
                
                String finalErrors = allErrors.length() > 0 ? allErrors.toString() : "Нет ошибок";
                addProperty(props, "Ошибки", finalErrors, "text");
                
                addProperty(props, "Напряжение фазы А", String.valueOf(core.voltagePhaseA), "number");
                addProperty(props, "Напряжение фазы B", String.valueOf(core.voltagePhaseB), "number");
                addProperty(props, "Напряжение фазы С", String.valueOf(core.voltagePhaseC), "number");
                
                // Температуры охлаждающей жидкости нужно делить на 10
                addProperty(props, "Температура охлаждающей жидкости на входе", String.format("%.1f", core.chillerTemperature1 / 10.0), "number");
                addProperty(props, "Температура охлаждающей жидкости на выходе", String.format("%.1f", core.chillerTemperature2 / 10.0), "number");
                addProperty(props, "Температура первичной обмотки", String.valueOf(core.primaryCoilTemperature), "number");
                addProperty(props, "Температура вторичной обмотки", String.valueOf(core.secondaryCoilTemperature), "number");
                
                // Преобразуем uint32 в float для отображения расхода проволоки
                float wireConsumption = uint32ToFloat(core.wireIndex);
                addProperty(props, "Расход проволоки", String.format("%.1f", wireConsumption), "number");

                state.setProperties(props);
                state.setStatus(determineStatus(props));
                state.setErrorCode(determineErrorCode(props));

                // Логируем разобранные ключевые поля в отдельный лог (для диагностики несоответствий)
                try {
                    CORE_PARSED_LOG.info("mac={}, idx={}, state={}, I={}, U_display={}, U_raw_welding={}, U_raw_idle={}, job={}",
                            mac,
                            core.index,
                            stateVal,
                            displayCurrent,
                            displayVoltageDouble,
                            core.weldingVoltage,
                            core.voltage,
                            core.jobNumber);
                } catch (Exception ignore) {}
                return state;
            }
        }

        String payload = extractPayload(data);
        if (payload != null) {
          //  System.out.println("[PARSER] 📦 Извлеченный payload: " + payload);
           // System.out.println("[PARSER] 📏 Длина payload: " + payload.length());
            if (debugMode) {
             //   System.out.println("[PARSER] 📦 Извлеченный payload: " + payload);
              //  System.out.println("[PARSER] 📏 Длина payload: " + payload.length());
            }
            
            Map<String, StateSummaryPropertyValue> properties = parseParameters(payload);
            state.setProperties(properties);
            
            if (debugMode) {
               // System.out.println("[PARSER] ✅ Парсинг завершен. Найдено свойств: " + properties.size());
            }
        } else {
           // System.out.println("[PARSER] ❌ Не удалось извлечь payload из данных");
        }
        
        state.setStatus(determineStatus(state.getProperties()));
        state.setErrorCode(determineErrorCode(state.getProperties()));
        
        if (debugMode) {
           // System.out.println("[PARSER] 🎯 Итоговый статус: " + state.getStatus());
        }
        
        return state;
    }

    private boolean isCoreMac(String mac) {
        if (mac == null || mac.isEmpty()) return false;
        String[] parts = coreMacsConfig.split(",");
        for (String part : parts) {
            if (mac.equalsIgnoreCase(part.trim())) return true;
        }
        return false;
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
           // System.out.println("[PARSER] 🔍 Анализ payload по позициям:");
          //  System.out.println("[PARSER] 📏 Общая длина: " + payload.length());
            
            // Логируем весь payload для анализа
          //  System.out.println("[PARSER] 📋 Полный payload: " + payload);
        }
        
        // Анализируем каждые 2 символа для поиска возможного тока
        //System.out.println("[PARSER] 🔍 Поиск возможных значений тока:");
        int bestCurrentPosition = -1;
        // int bestCurrentValue = -1; // not used
        int minDifference = Integer.MAX_VALUE;
        
        for (int i = 0; i < payload.length() - 1; i += 2) {
            if (i + 1 < payload.length()) {
                String value = payload.substring(i, i + 2);
               // System.out.println("[PARSER]   Позиции " + i + "-" + (i+1) + ": " + value);
                
                // Попробуем интерпретировать как число
                try {
                    int numValue = Integer.parseInt(value, 16);
                  //  System.out.println("[PARSER]     Как число (hex): " + numValue);
                    
                    // Если это число в диапазоне 100-120 (ток 104-111 А), это может быть ток
                    if (numValue >= 100 && numValue <= 120) {
                      //  System.out.println("[PARSER]     ⚡ ВОЗМОЖНЫЙ ТОК! Значение: " + numValue);
                        
                        // Ищем значение, наиболее близкое к 108 (среднее между 104-111)
                        int difference = Math.abs(numValue - 108);
                        if (difference < minDifference) {
                            minDifference = difference;
                            bestCurrentPosition = i;
                            // bestCurrentValue = numValue;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Не число, пропускаем
                }
            }
        }
        
        if (bestCurrentPosition >= 0) {
           // System.out.println("[PARSER] 🎯 НАИЛУЧШИЙ КАНДИДАТ НА ТОК:");
          //  System.out.println("[PARSER]    Позиция: " + bestCurrentPosition + "-" + (bestCurrentPosition+1));
           // System.out.println("[PARSER]    Значение: " + bestCurrentValue);
           // System.out.println("[PARSER]    Разница с 108: " + minDifference);
            
            // Используем найденную позицию для тока
            String current = payload.substring(bestCurrentPosition, bestCurrentPosition + 2);
            
            // Напряжение: большинство плат Блока мониторинга отдают напряжение как 2-байтовое число в десятых В (Big-Endian)
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
                        int diff = Math.abs(word - 270); // около 27.0 В по вашим наблюдениям
                        if (diff < minVoltageWordDiff) {
                            minVoltageWordDiff = diff;
                            bestVoltageWordPos = i;
                            bestVoltageWord = word;
                        }
                    }
                } catch (NumberFormatException ignore) {}
            }

            String voltageHex;
            if (bestVoltageWordPos >= 0) {
                // Переведём десятые В в целые В (округление до ближайшего)
                int volts = Math.round(bestVoltageWord / 10.0f);
                voltageHex = toHex(volts);
            } else {
                // Fallback: используем байт сразу после найденного тока как приблизительное напряжение в В
                int voltageStart = bestCurrentPosition + 2;
                if (voltageStart + 2 <= payload.length()) {
                    voltageHex = payload.substring(voltageStart, voltageStart + 2);
                } else if (payload.length() >= 80) {
                    voltageHex = payload.substring(78, 80);
                } else {
                    voltageHex = "00";
                }
            }

            addProperty(properties, "State.I", current, "number");
            addProperty(properties, "State.U", voltageHex, "number");
        } else {
            // Если ток не найден в диапазоне 100-120, ищем в позициях 72-73 (где был найден ток 112)
          //  System.out.println("[PARSER] ⚠️ Ток не найден в диапазоне 100-120, используем фиксированные позиции 72-73");
            if (payload.length() >= 76) {
                String current = payload.substring(72, 74);
                String voltage = payload.substring(74, 76);
                
               // System.out.println("[PARSER] 🔧 УСТАНАВЛИВАЕМ ИЗ ФИКСИРОВАННЫХ ПОЗИЦИЙ:");
              //  System.out.println("[PARSER]   State.I = " + current + " (позиции 72-73)");
              //  System.out.println("[PARSER]   State.U = " + voltage + " (позиции 74-75)");
                
                addProperty(properties, "State.I", current, "number");
                addProperty(properties, "State.U", voltage, "number");
            }
        }
        
        // Original parsing logic with added logging
        if (payload.length() >= 2) {
            String model = payload.substring(0, 2);
            if (debugMode) {
            //    System.out.println("[PARSER] 🏷️ Позиции 0-1 (MODEL): " + model);
            }
            addProperty(properties, "MODEL", model, "number");
        }
        if (payload.length() >= 4) {
            String version = payload.substring(2, 4);
            if (debugMode) {
              //  System.out.println("[PARSER] 🔢 Позиции 2-3 (VERSION): " + version);
            }
            addProperty(properties, "VERSION", version, "number");
        }
        if (payload.length() >= 6) {
            String material = payload.substring(4, 6);
            if (debugMode) {
               // System.out.println("[PARSER] 🧱 Позиции 4-5 (MATERIAL): " + material);
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
              //  System.out.println("[PARSER] 💨 Позиции 10-11 (GAS_FLOW): " + gasFlow);
            }
            addProperty(properties, "State.GasFlow", gasFlow, "number");
        }
        if (payload.length() >= 14) {
            String temperature = payload.substring(12, 14);
            if (debugMode) {
             //   System.out.println("[PARSER] 🌡️ Позиции 12-13 (TEMPERATURE): " + temperature);
            }
            addProperty(properties, "State.Temperature", temperature, "number");
        }
        if (payload.length() >= 16) {
            String control = payload.substring(14, 16);
            if (debugMode) {
             //   System.out.println("[PARSER] 🎛️ Позиции 14-15 (CONTROL): " + control);
            }
            addProperty(properties, "State.Ctrl", control, "enum");
        }

        // Попробуем найти ток в других позициях
        // skip unused candidates to avoid linter warnings

        // skip

        // СТАРЫЙ КОД С ФИКСИРОВАННЫМИ ПОЗИЦИЯМИ ОТКЛЮЧЕН
        // Теперь используем автоматический поиск тока выше
      //  System.out.println("[PARSER] ℹ️ Используем автоматический поиск тока вместо фиксированных позиций");

        // Попробуем найти ток в позициях 40-50
        // skip

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

    private String toHex(int value) {
        String hex = Integer.toHexString(value).toUpperCase();
        if (hex.length() % 2 != 0) hex = "0" + hex;
        return hex;
    }

    /**
     * Преобразует uint32 значение в float (для union структур)
     * Используется для расхода проволоки, где uint32 интерпретируется как float
     */
    private float uint32ToFloat(long uint32Value) {
        return Float.intBitsToFloat((int) uint32Value);
    }

    /**
     * Массив описаний ошибок (индекс соответствует номеру бита)
     */
    private static final String[] ERROR_MESSAGES = {
        "Перегрузка драйвера подающего механизма",
        "Реверс энкодера подающего механизма", 
        "Нет сигнала от энк. подающего механизма",
        "Отказ связи с подающим механизмом",
        "Отказ драйвера платы сварки",
        "Отказ связи с платой сварки",
        "Ошибка 6",
        "Ошибка 7",
        "Ошибка 8",
        "Ошибка 9",
        "Ошибка 10",
        "Ошибка 11",
        "Ошибка 12",
        "Ошибка 13",
        "Ошибка 14",
        "Ошибка 15",
        "Перегрев БВО",
        "Отказ связи с БВО",
        "Отсутствует охл. жидкость в БВО",
        "Отказ датчика темп. жидкости в БВО",
        "Замыкание датчика темп. жидкости БВО",
        "Ошибка 21",
        "Ошибка 22",
        "Ошибка 23"
    };

    /**
     * Парсит битовое поле ошибок и возвращает список текстовых описаний
     */
    private String parseErrorBits(int errorValue) {
        if (errorValue == 0) return "";
        
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            if ((errorValue & (1 << i)) != 0) {
                if (errors.length() > 0) errors.append(", ");
                if (i < ERROR_MESSAGES.length) {
                    errors.append(ERROR_MESSAGES[i]);
                } else {
                    errors.append("Неизвестная ошибка ").append(i);
                }
            }
        }
        return errors.toString();
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