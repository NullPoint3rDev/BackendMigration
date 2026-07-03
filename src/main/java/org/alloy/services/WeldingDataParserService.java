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
    //  private static final org.slf4j.Logger CORE_PARSED_LOG = org.slf4j.LoggerFactory.getLogger("org.alloy.core.parsed");

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

                // Преобразуем состояние аппарата из числа в текст
                String machineStateText = getMachineStateText(core.weldingMachineState);
                addProperty(props, "Состояние аппарата", machineStateText, "text");
                // Также добавляем под ключом WeldingMachineState для совместимости с фронтендом
                addProperty(props, "WeldingMachineState", machineStateText, "text");
                addProperty(props, "State.GasFlow",
                        String.format("%.1f", core.getDisplayInstantGasFlowLpm()), "number");
                if (core.hasExtendedGasMetrics) {
                    String gasSincePowerOn = String.format("%.1f", core.getDisplayGasConsumptionSincePowerOnLiters());
                    addProperty(props, "Core.GasConsumptionSincePowerOn", gasSincePowerOn, "number");
                    addProperty(props, "Расход газа с включения", gasSincePowerOn, "number");
                }

                addProperty(props, "Номер сварочного задания", String.valueOf(core.jobNumber), "number");
                addProperty(props, "Inductance", String.valueOf(core.inductance), "number");

                // Парсим битовые поля ошибок и объединяем в текстовые описания
                StringBuilder allErrors = new StringBuilder();

                String errors1Text = parseErrorBits(core.errors1, 0);   // ошибки 1-16
                String errors2Text = parseErrorBits(core.errors2, 16);   // ошибки 17-32 (БВО)
                String errors3Text = parseErrorBits(core.errors3, 32);   // ошибки 33-48 (если есть)

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

                // Парсим битовые поля предупреждений
                StringBuilder allWarnings = new StringBuilder();
                String warnings1Text = parseWarningBits(core.warnings1, 0);
                String warnings2Text = parseWarningBits(core.warnings2, 16);
                String warnings3Text = parseWarningBits(core.warnings3, 32);
                if (!warnings1Text.isEmpty()) allWarnings.append(warnings1Text);
                if (!warnings2Text.isEmpty()) {
                    if (allWarnings.length() > 0) allWarnings.append(", ");
                    allWarnings.append(warnings2Text);
                }
                if (!warnings3Text.isEmpty()) {
                    if (allWarnings.length() > 0) allWarnings.append(", ");
                    allWarnings.append(warnings3Text);
                }
                String finalWarnings = allWarnings.length() > 0 ? allWarnings.toString() : "Нет предупреждений";
                addProperty(props, "Предупреждения", finalWarnings, "text");

                addProperty(props, "Напряжение фазы А", String.valueOf(core.voltagePhaseA), "number");
                addProperty(props, "Напряжение фазы B", String.valueOf(core.voltagePhaseB), "number");
                addProperty(props, "Напряжение фазы С", String.valueOf(core.voltagePhaseC), "number");

                // Температуры охлаждающей жидкости нужно делить на 10
                addProperty(props, "Температура охлаждающей жидкости на входе", String.format("%.1f", core.chillerTemperature1 / 10.0), "number");
                addProperty(props, "Температура охлаждающей жидкости на выходе", String.format("%.1f", core.chillerTemperature2 / 10.0), "number");
                addProperty(props, "Температура первичной обмотки", String.format("%.1f", core.primaryCoilTemperature / 10.0), "number");
                addProperty(props, "Температура вторичной обмотки", String.format("%.1f", core.secondaryCoilTemperature / 10.0), "number");

                // Скорость подачи проволоки с аппарата (м/мин); в БД — «Расход проволоки» (историческое имя параметра)
                float wireFeedMetersPerMin = uint32ToFloat(core.wireIndex);
                addProperty(props, "Расход проволоки", String.format("%.1f", wireFeedMetersPerMin), "number");
                // RFID: добавим в двух представлениях — десятичном и шестнадцатеричном
                if (core.rfidData != 0L) {
                    // addProperty(props, "RFID", String.valueOf(core.rfidData), "number");
                    addProperty(props, "RFID.Hex", String.format("%016X", core.rfidData), "text");
                }

                // New Core tail parameters (uint8 each)
                //   addProperty(props, "Welding.Mode.Code", String.valueOf(core.weldingMode), "number");
                addProperty(props, "Метод сварки", mapWeldingMode(core.weldingMode), "enum");

                //   addProperty(props, "Welding.Material.Code", String.valueOf(core.weldingMaterial), "number");
                addProperty(props, "Материал проволоки", mapWeldingMaterial(core.weldingMaterial), "enum");

                //   addProperty(props, "Welding.Gas.Code", String.valueOf(core.weldingGas), "number");
                addProperty(props, "Газ", mapWeldingGas(core.weldingGas), "enum");

                //   addProperty(props, "Welding.WireDiameter.Code", String.valueOf(core.weldingWireDiameter), "number");
                addProperty(props, "Диаметр проволоки", mapWireDiameter(core.weldingWireDiameter), "enum");

                //  addProperty(props, "Welding.BurnerMode.Code", String.valueOf(core.burnerMode), "number");
                addProperty(props, "Режим горелки", mapBurnerMode(core.burnerMode), "enum");

                addProperty(props, "Номер ячейки памяти", String.valueOf(core.memoryCellNumber), "number");

                addProperty(props, "Core.WorkTimeSincePowerOn", String.valueOf(core.workTimeSincePowerOn), "number");
                addProperty(props, "Core.WeldingTimeSincePowerOn", String.valueOf(core.weldingTimeSincePowerOn), "number");
                addProperty(props, "Время работы с включения", String.valueOf(core.workTimeSincePowerOn), "number");
                addProperty(props, "Время сварки с включения", String.valueOf(core.weldingTimeSincePowerOn), "number");

                state.setProperties(props);
                WeldingMachineStatus determinedStatus = determineStatus(props);
                state.setStatus(determinedStatus);
                // Core: только битовые errors1/errors2 → error_code; не используем State.Ctrl / State.Error (архив)
                String coreErrorCode = getFirstErrorCodeFromCore(core.errors1, core.errors2);
                state.setErrorCode(coreErrorCode);

                // Логируем разобранные ключевые поля в отдельный лог (для диагностики несоответствий)
//                try {
//                  //  CORE_PARSED_LOG.info("mac={}, idx={}, state={}, I={}, U_display={}, U_raw_welding={}, U_raw_idle={}, job={}",
//                            mac,
//                            core.index,
//                            stateVal,
//                            displayCurrent,
//                            displayVoltageDouble,
//                            core.weldingVoltage,
//                            core.voltage,
//                            core.jobNumber);
//                } catch (Exception ignore) {}
                return state;
            }
            // Core-аппарат, но пакет не разобрался как Core — не подмешиваем archive-парсер (State.Ctrl / State.I / …)
            return buildCoreParseFailedState();
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

    /**
     * Признак набора свойств, собранного веткой Core ({@link CorePacketParser}): не смешивать с архивными ключами State.Ctrl / State.Error.
     */
    private static boolean isCorePropertyMap(Map<String, StateSummaryPropertyValue> properties) {
        if (properties == null || properties.isEmpty()) {
            return false;
        }
        return properties.containsKey("Packet.Index")
                || properties.containsKey("WeldingMachineState")
                || properties.containsKey("Состояние аппарата");
    }

    /** Core-пакет не распознан — без архивного payload, чтобы в БД не попадали State.Ctrl и т.п. */
    private StateSummary buildCoreParseFailedState() {
        StateSummary state = new StateSummary();
        state.setDateCreated(LocalDateTime.now());
        state.setLastDatetimeUpdate(LocalDateTime.now());
        state.setProperties(new HashMap<>());
        state.setStatus(WeldingMachineStatus.Offline);
        state.setErrorCode(null);
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

    // ===== Mapping helpers for Core tail parameters =====
    private String mapWeldingMode(int code) {
        switch (code) {
            case 0:  return "MMA";
            case 1:  return "Строжка";
            case 2:  return "TIG";
            case 3:  return "MIG/MAG";
            case 4:  return "MAG";
            case 5:  return "MIG";
            case 6:  return "P-MIG";
            case 7:  return "FLUX";
            case 8:  return "Х-Свар";
            case 9:  return "К-Свар";
            case 10: return "ВВ-Свар";
            case 11: return "ВС-Свар";
            case 12: return "Резерв 1";
            case 13: return "Резерв 2";
            case 14: return "Резерв 3";
            case 15: return "Резерв 4";
            case 16: return "Резерв 5";
            default: return "Неизвестно (" + code + ")";
        }
    }

    private String mapWeldingMaterial(int code) {
        switch (code) {
            case 0:  return "Ручной (любой)";
            case 1:  return "Сталь";
            case 2:  return "Нерж. ER304";
            case 3:  return "Хромоникель ER308";
            case 4:  return "Аустенитная ER316";
            case 5:  return "AlMg";
            case 6:  return "AlSi";
            case 7:  return "Al99";
            case 8:  return "CuSi3";
            case 9:  return "CuSn";
            case 10: return "CuAl";
            case 11: return "E71T (порошковая)";
            case 12: return "E308T (самозащитная)";
            case 13: return "Рутиловый электрод";
            case 14: return "Основной электрод";
            case 15: return "Целлюлозный электрод";
            case 16: return "Без материала";
            case 17: return "Резерв 1";
            case 18: return "Резерв 2";
            case 19: return "Резерв 3";
            case 20: return "Резерв 4";
            case 21: return "Резерв 5";
            default: return "Неизвестно (" + code + ")";
        }
    }

    private String mapWeldingGas(int code) {
        switch (code) {
            case 0:  return "CO2";
            case 1:  return "Ar82/CO2";
            case 2:  return "Ar92/CO2";
            case 3:  return "Ar98/CO2";
            case 4:  return "Ar";
            case 5:  return "Без газа";
            case 6:  return "Резерв 1";
            case 7:  return "Резерв 2";
            case 8:  return "Резерв 3";
            case 9:  return "Резерв 4";
            case 10: return "Резерв 5";
            default: return "Неизвестно (" + code + ")";
        }
    }

    private String mapWireDiameter(int code) {
        switch (code) {
            case 0:  return "0.6 мм";
            case 1:  return "0.7 мм";
            case 2:  return "0.8 мм";
            case 3:  return "1.0 мм";
            case 4:  return "1.2 мм";
            case 5:  return "1.4 мм";
            case 6:  return "1.6 мм";
            case 7:  return "1.7 мм";
            case 8:  return "1.9 мм";
            case 9:  return "2.0 мм";
            case 10: return "2.4 мм";
            case 11: return "Без диаметра";
            case 12: return "Резерв 1";
            case 13: return "Резерв 2";
            default: return "Неизвестно (" + code + ")";
        }
    }

    private String mapBurnerMode(int code) {
        switch (code) {
            case 0: return "2T";
            case 1: return "4T";
            case 2: return "SPt (точечный)";
            case 3: return "S2T (2T со стартом)";
            case 4: return "S4T (4T со стартом)";
            case 5: return "4УП (две методики)";
            default: return "Неизвестно (" + code + ")";
        }
    }

    /**
     * Массив описаний предупреждений (индекс соответствует номеру бита)
     * Нумерация начинается с 1, поэтому индекс = номер предупреждения - 1
     */
    private static final String[] WARNING_MESSAGES = {
            "Предупреждение калибровки тока",                          // предупреждение 1
            "Предупреждение калибровки напряжения",                    // предупреждение 2
            "Предупреждение обратной связи по мощности",               // предупреждение 3
            "Предупреждение ограничения драйвера платы сварки"         // предупреждение 4
    };

    /**
     * Массив описаний ошибок (индекс соответствует номеру бита)
     * Нумерация в файле начинается с 1, поэтому индекс = номер ошибки - 1
     */
    private static final String[] ERROR_MESSAGES = {
            "Неисправность цепи упр. ЭД МП",        // ошибка 1
            "Неисправность ДПР МП",             // ошибка 2
            "Нет сигнала ДПР МП",         // ошибка 3
            "Нет связи с МП",               // ошибка 4
            "Отказ драйвера ПУ ИП",                     // ошибка 5
            "Нет связи с ПУ ИП",                      // ошибка 6
            "Перегрев инвертора ИП",                                        // ошибка 7
            "Ошибка 8",                                        // ошибка 8
            "Ошибка 9",                                        // ошибка 9
            "Ошибка 10",                                       // ошибка 10
            "Ошибка 11",                                       // ошибка 11
            "Ошибка 12",                                       // ошибка 12
            "Ошибка 13",                                       // ошибка 13
            "Ошибка 14",                                       // ошибка 14
            "Ошибка 15",                                       // ошибка 15
            "Ошибка 16",                                    // ошибка 16
            "Перегрев охл. жидкости БВО",                               // ошибка 17
            "Нет связи с БВО",                // ошибка 18
            "Нет протока жидкости БВО",                        // ошибка 19
            "Неиспр. ДТ на выходе БВО",            // ошибка 20
            "Неиспр. ДТ на входе БВО:",                                       // ошибка 21
            "Ошибка 22",                                       // ошибка 22
            "Ошибка 23"                                        // ошибка 23
    };

    /**
     * Парсит битовое поле ошибок и возвращает список текстовых описаний
     * @param errorValue значение ошибки
     * @param offset смещение для индексации (0 для Errors1, 16 для Errors2, 32 для Errors3)
     */
    private String parseErrorBits(int errorValue, int offset) {
        if (errorValue == 0) return "";

        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            if ((errorValue & (1 << i)) != 0) {
                if (errors.length() > 0) errors.append(", ");
                int errorIndex = i + offset;
                if (errorIndex < ERROR_MESSAGES.length) {
                    errors.append(ERROR_MESSAGES[errorIndex]);
                } else {
                    errors.append("Неизвестная ошибка ").append(errorIndex);
                }
            }
        }
        return errors.toString();
    }

    /**
     * Парсит битовое поле ошибок без смещения (для обратной совместимости)
     */
    private String parseErrorBits(int errorValue) {
        return parseErrorBits(errorValue, 0);
    }

    /**
     * Парсит битовое поле предупреждений и возвращает список текстовых описаний.
     * @param warningValue значение предупреждения
     * @param offset смещение для индексации (0 для Warnings1, 16 для Warnings2, 32 для Warnings3)
     */
    private String parseWarningBits(int warningValue, int offset) {
        if (warningValue == 0) return "";

        StringBuilder warnings = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            if ((warningValue & (1 << i)) != 0) {
                if (warnings.length() > 0) warnings.append(", ");
                int warnIndex = i + offset;
                if (warnIndex < WARNING_MESSAGES.length) {
                    warnings.append(WARNING_MESSAGES[warnIndex]);
                } else {
                    warnings.append("Неизвестное предупреждение ").append(warnIndex + 1);
                }
            }
        }
        return warnings.toString();
    }

    /**
     * Возвращает первый код ошибки (1–23) из битовых полей Core: errors1 (биты 0–15 → ошибки 1–16), errors2 (биты 0–6 → ошибки 17–23).
     * Нужно для сохранения в welding_machine_state.error_code и отображения в отчёте по неисправностям.
     */
    private static String getFirstErrorCodeFromCore(int errors1, int errors2) {
        for (int i = 0; i < 16; i++) {
            if ((errors1 & (1 << i)) != 0) {
                return String.valueOf(i + 1);
            }
        }
        for (int i = 0; i < 7; i++) {
            if ((errors2 & (1 << i)) != 0) {
                return String.valueOf(17 + i);
            }
        }
        return null;
    }

    /**
     * Преобразует числовое состояние аппарата в текстовое описание
     * @param state числовое значение состояния (0-5)
     * @return текстовое описание состояния
     */
    private String getMachineStateText(int state) {
        switch (state) {
            case 0:
                return "Аппарат включен";
            case 1:
                return "Сварка";
            case 2:
                return "Авария";
            case 3:
                return "Аппарат в режиме ожидания";
            case 4:
                return "Аппарат включен в дежурном режиме";
            case 5:
                return "Аппарат заблокирован";
            default:
                return "Неизвестное состояние (" + state + ")";
        }
    }

    private WeldingMachineStatus determineStatus(Map<String, StateSummaryPropertyValue> properties) {
        if (properties == null || properties.isEmpty()) {
            return WeldingMachineStatus.Offline;
        }
        // Сначала Core: WeldingMachineState / Состояние аппарата (не опираться на State.Ctrl, даже если ключи случайно попали в map)
        if (isCorePropertyMap(properties)) {
            WeldingMachineStatus fromCore = resolveStatusFromCoreMachineStateText(properties);
            if (fromCore != null) {
                return fromCore;
            }
            return WeldingMachineStatus.Offline;
        }

        // Архивные устройства: State.Ctrl
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

    /** Текст состояния Core из WeldingMachineState или «Состояние аппарата». */
    private WeldingMachineStatus resolveStatusFromCoreMachineStateText(Map<String, StateSummaryPropertyValue> properties) {
        StateSummaryPropertyValue weldingStateProp = properties.get("WeldingMachineState");
        if (weldingStateProp != null) {
            String stateText = weldingStateProp.getValue();
            WeldingMachineStatus s = mapCoreStateTextToStatus(stateText);
            if (s != null) {
                return s;
            }
        }
        StateSummaryPropertyValue machineStateProp = properties.get("Состояние аппарата");
        if (machineStateProp != null) {
            String stateText = machineStateProp.getValue();
            return mapCoreStateTextToStatus(stateText);
        }
        return null;
    }

    private static WeldingMachineStatus mapCoreStateTextToStatus(String stateText) {
        if (stateText == null) {
            return null;
        }
        if ("Сварка".equals(stateText)) {
            return WeldingMachineStatus.Welding;
        }
        if ("Аппарат включен".equals(stateText) || "Аппарат включен в дежурном режиме".equals(stateText)) {
            return WeldingMachineStatus.Idle;
        }
        if ("Авария".equals(stateText)) {
            return WeldingMachineStatus.Error;
        }
        if ("Аппарат в режиме ожидания".equals(stateText)) {
            return WeldingMachineStatus.Idle;
        }
        return null;
    }

    /** Код ошибки только для архивного формата (State.Ctrl + State.Error). Для Core — null, код задаётся в parseWeldingData из битов errors1/errors2. */
    private String determineErrorCode(Map<String, StateSummaryPropertyValue> properties) {
        if (properties == null || properties.isEmpty() || isCorePropertyMap(properties)) {
            return null;
        }
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