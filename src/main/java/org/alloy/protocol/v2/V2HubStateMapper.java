package org.alloy.protocol.v2;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;

/**
 * Hub payload → StateSummary в диалекте Core-панели.
 * <p>
 * Не пишет {@code State.I}/{@code State.U}: фронт для них делает {@code parseInt(..., 16)}
 * (archive). V2 кладёт decimal в {@code Current}/{@code Voltage} — как Core.
 */
public final class V2HubStateMapper {
    private V2HubStateMapper() {}

    public static StateSummary toStateSummary(V2HubPayload hub, boolean historyRecord) {
        if (hub == null) {
            return null;
        }
        boolean welding = hub.machineState == 1;
        StateSummary s = new StateSummary();
        s.setStatus(mapStatus(hub.machineState));
        s.setControl("HUB");
        s.setControlState(String.valueOf(hub.machineState));
        s.setStateDurationMs(0);
        s.setOfflineData(historyRecord);
        s.setErrorCode(formatErrorCode(hub));

        LocalDateTime ts = unixToUtc(hub.unixTime);
        s.setDateCreated(ts);
        s.setLastDatetimeUpdate(ts);
        s.setLocalServerPacketDatetime(LocalDateTime.now(ZoneOffset.UTC));

        // Как Core.getDisplayCurrent/Voltage: вне дуги в Current/Voltage — уставка.
        int displayCurrentA = welding ? hub.actualCurrentA : hub.setCurrentA;
        double displayVoltageV = welding ? hub.actualVoltageV : hub.setVoltageV;
        int voltageTenths = (int) Math.round(displayVoltageV * 10.0);

        Map<String, StateSummaryPropertyValue> props = new HashMap<>();
        add(props, "Current", String.valueOf(displayCurrentA), "number");
        add(props, "Voltage", String.valueOf(voltageTenths), "number");
        add(props, "State.I.set", String.valueOf(hub.setCurrentA), "number");
        add(props, "State.U.set", formatOneDecimal(hub.setVoltageV), "number");
        add(props, "WeldingCurrent", String.valueOf(hub.actualCurrentA), "number");

        String stateText = machineStateText(hub.machineState);
        add(props, "Состояние аппарата", stateText, "text");
        add(props, "WeldingMachineState", stateText, "text");

        add(props, "Packet.Index", String.valueOf(hub.packetIndex), "number");
        add(props, "Номер сварочного задания", String.valueOf(hub.jobNumber), "number");
        add(props, "JobNumber", String.valueOf(hub.jobNumber), "number");
        add(props, "Inductance", String.valueOf(hub.setInductance), "number");
        add(props, "Метод сварки", mapWeldingMode(hub.weldMode), "enum");
        add(props, "Материал проволоки", mapWeldingMaterial(hub.wireMaterial), "enum");
        add(props, "Газ", mapWeldingGas(hub.weldGas), "enum");
        add(props, "Диаметр проволоки", mapWireDiameter(hub.wireDiameter), "enum");

        add(props, "State.GasFlow", formatOneDecimal(hub.gasFlowLPerMin), "number");
        add(props, "Расход газа с включения", String.valueOf(hub.gasTotalL), "number");
        add(props, "Core.GasConsumptionSincePowerOn", String.valueOf(hub.gasTotalL), "number");
        // Core: «Расход проволоки» = скорость подачи м/мин
        add(props, "Расход проволоки", formatOneDecimal(hub.setWireSpeedMPerMin), "number");
        add(props, "State.WFS", formatOneDecimal(hub.setWireSpeedMPerMin), "number");

        add(props, "Напряжение фазы А", String.valueOf(hub.mainsPhaseAV), "number");
        add(props, "Напряжение фазы B", String.valueOf(hub.mainsPhaseBV), "number");
        add(props, "Напряжение фазы С", String.valueOf(hub.mainsPhaseCV), "number");
        add(props, "VoltagePhaseA", String.valueOf(hub.mainsPhaseAV), "number");
        add(props, "VoltagePhaseB", String.valueOf(hub.mainsPhaseBV), "number");
        add(props, "VoltagePhaseC", String.valueOf(hub.mainsPhaseCV), "number");

        add(props, "Температура охлаждающей жидкости на входе", formatOneDecimal(hub.tempBvoInC), "number");
        add(props, "Температура охлаждающей жидкости на выходе", formatOneDecimal(hub.tempBvoOutC), "number");
        add(props, "Температура первичной обмотки", formatOneDecimal(hub.tempRadiatorC), "number");
        add(props, "Температура вторичной обмотки", formatOneDecimal(hub.tempTerminalPlusC), "number");
        add(props, "ChillerTemperature1", formatOneDecimal(hub.tempBvoInC), "number");
        add(props, "ChillerTemperature2", formatOneDecimal(hub.tempBvoOutC), "number");
        add(props, "PrimaryCoilTemperature", formatOneDecimal(hub.tempRadiatorC), "number");
        add(props, "SecondaryCoilTemperature", formatOneDecimal(hub.tempTerminalPlusC), "number");

        add(props, "Ошибки", formatErrorsText(hub), "text");
        add(props, "Предупреждения", formatWarningsText(hub), "text");

        add(props, "Hub.Job", String.valueOf(hub.jobNumber), "number");
        add(props, "Hub.Mode", String.valueOf(hub.weldMode), "number");
        add(props, "Hub.PacketIndex", String.valueOf(hub.packetIndex), "number");

        s.setProperties(props);
        return s;
    }

    static WeldingMachineStatus mapStatus(int machineState) {
        return switch (machineState) {
            case 0 -> WeldingMachineStatus.Online;
            case 1 -> WeldingMachineStatus.Welding;
            case 2 -> WeldingMachineStatus.Error;
            case 3, 4 -> WeldingMachineStatus.Idle;
            case 5 -> WeldingMachineStatus.Offline;
            default -> WeldingMachineStatus.Online;
        };
    }

    /** Те же подписи, что WeldingDataParserService.getMachineStateText. */
    static String machineStateText(int state) {
        return switch (state) {
            case 0 -> "Аппарат включен";
            case 1 -> "Сварка";
            case 2 -> "Авария";
            case 3 -> "Аппарат в режиме ожидания";
            case 4 -> "Аппарат включен в дежурном режиме";
            case 5 -> "Аппарат заблокирован";
            default -> "Неизвестное состояние (" + state + ")";
        };
    }

    private static String formatErrorCode(V2HubPayload hub) {
        if (hub.errors1 == 0 && hub.errors2 == 0 && hub.errors3 == 0) {
            return null;
        }
        return "E1:" + hub.errors1 + ";E2:" + hub.errors2 + ";E3:" + hub.errors3;
    }

    private static String formatErrorsText(V2HubPayload hub) {
        if (hub.errors1 == 0 && hub.errors2 == 0 && hub.errors3 == 0) {
            return "Нет ошибок";
        }
        return "E1:" + hub.errors1 + " E2:" + hub.errors2 + " E3:" + hub.errors3;
    }

    private static String formatWarningsText(V2HubPayload hub) {
        if (hub.warnings1 == 0 && hub.warnings2 == 0 && hub.warnings3 == 0) {
            return "Нет предупреждений";
        }
        return "W1:" + hub.warnings1 + " W2:" + hub.warnings2 + " W3:" + hub.warnings3;
    }

    private static String formatOneDecimal(double v) {
        return String.format(java.util.Locale.US, "%.1f", v);
    }

    private static LocalDateTime unixToUtc(long unixSec) {
        if (unixSec <= 0) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixSec), ZoneOffset.UTC);
    }

    private static void add(
            Map<String, StateSummaryPropertyValue> props,
            String code,
            String value,
            String type) {
        StateSummaryPropertyValue p = new StateSummaryPropertyValue();
        p.setPropertyCode(code);
        p.setValue(value);
        p.setPropertyType(type);
        p.setRawValue(value);
        props.put(code, p);
    }

    // --- те же коды, что у Core (WeldingDataParserService) ---

    static String mapWeldingMode(int code) {
        return switch (code) {
            case 0 -> "MMA";
            case 1 -> "Строжка";
            case 2 -> "TIG";
            case 3 -> "MIG/MAG";
            case 4 -> "MAG";
            case 5 -> "MIG";
            case 6 -> "P-MIG";
            case 7 -> "FLUX";
            case 8 -> "Х-Свар";
            case 9 -> "К-Свар";
            case 10 -> "ВВ-Свар";
            case 11 -> "ВС-Свар";
            default -> "Неизвестно (" + code + ")";
        };
    }

    static String mapWeldingMaterial(int code) {
        return switch (code) {
            case 0 -> "Ручной (любой)";
            case 1 -> "Сталь";
            case 2 -> "Нерж. ER304";
            case 3 -> "Хромоникель ER308";
            case 4 -> "Аустенитная ER316";
            case 5 -> "AlMg";
            case 6 -> "AlSi";
            case 7 -> "Al99";
            case 8 -> "CuSi3";
            case 9 -> "CuSn";
            case 10 -> "CuAl";
            case 11 -> "E71T (порошковая)";
            case 12 -> "E308T (самозащитная)";
            case 13 -> "Рутиловый электрод";
            case 14 -> "Основной электрод";
            case 15 -> "Целлюлозный электрод";
            case 16 -> "Без материала";
            default -> "Неизвестно (" + code + ")";
        };
    }

    static String mapWeldingGas(int code) {
        return switch (code) {
            case 0 -> "CO2";
            case 1 -> "Ar82/CO2";
            case 2 -> "Ar92/CO2";
            case 3 -> "Ar98/CO2";
            case 4 -> "Ar";
            case 5 -> "Без газа";
            default -> "Неизвестно (" + code + ")";
        };
    }

    static String mapWireDiameter(int code) {
        return switch (code) {
            case 0 -> "0.6 мм";
            case 1 -> "0.7 мм";
            case 2 -> "0.8 мм";
            case 3 -> "1.0 мм";
            case 4 -> "1.2 мм";
            case 5 -> "1.4 мм";
            case 6 -> "1.6 мм";
            case 7 -> "1.7 мм";
            case 8 -> "1.9 мм";
            case 9 -> "2.0 мм";
            case 10 -> "2.4 мм";
            case 11 -> "Без диаметра";
            default -> "Неизвестно (" + code + ")";
        };
    }
}
