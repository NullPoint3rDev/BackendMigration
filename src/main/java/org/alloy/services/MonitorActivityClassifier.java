package org.alloy.services;

import org.alloy.models.MonitorActivityMode;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.entities.WeldingMachineState;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * Классификация режима мониторинга: сварка — по тексту состояния аппарата или статусу Welding.
 */
public final class MonitorActivityClassifier {

    private MonitorActivityClassifier() {
    }

    public static MonitorActivityMode classify(
            WeldingMachineState state,
            String machineStateText,
            BigDecimal currentAmps) {
        if (state == null) {
            return MonitorActivityMode.off;
        }
        if (isWelding(state, machineStateText, currentAmps)) {
            return MonitorActivityMode.welding;
        }
        String stateLower = normalize(machineStateText);
        if (isErrorState(state, stateLower)) {
            return MonitorActivityMode.error;
        }
        if (stateLower.contains("выключ") || "off".equals(stateLower) || stateLower.contains("offline")
                || stateLower.contains("не в сети")) {
            return MonitorActivityMode.off;
        }
        if (stateLower.contains("дежур") || stateLower.contains("standby")) {
            return MonitorActivityMode.off;
        }
        if (stateLower.contains("waiting") || stateLower.contains("ожидан")
                || stateLower.contains("включ") || "on".equals(stateLower) || stateLower.contains("idle")
                || stateLower.contains("ready")) {
            return MonitorActivityMode.on;
        }
        if (state.getWeldingMachineStatus() == WeldingMachineStatus.Offline) {
            return MonitorActivityMode.off;
        }
        if (state.getWeldingMachineStatus() == WeldingMachineStatus.Error) {
            return MonitorActivityMode.error;
        }
        return MonitorActivityMode.on;
    }

    /** Совместимость: газ и напряжение не участвуют в классификации. */
    public static MonitorActivityMode classify(
            WeldingMachineState state,
            String machineStateText,
            BigDecimal currentAmps,
            BigDecimal gasFlowLpm,
            BigDecimal voltageVolts) {
        return classify(state, machineStateText, currentAmps);
    }

    private static boolean isErrorState(WeldingMachineState state, String stateLower) {
        if (stateLower.contains("авария") || stateLower.contains("error")
                || stateLower.contains("ошибка") || stateLower.contains("emergency")
                || stateLower.contains("failure")) {
            return true;
        }
        String errorCode = state.getErrorCode();
        if (errorCode == null || errorCode.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(errorCode.trim()) > 0;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static boolean isWelding(
            WeldingMachineState state,
            String machineStateText,
            BigDecimal currentAmps) {
        return isWelding(state, machineStateText, currentAmps, null);
    }

    /**
     * Сварка: текст «Сварка» / Welding или weldingCurrent >> уставки (Core state=0 при дуге).
     */
    public static boolean isWelding(
            WeldingMachineState state,
            String machineStateText,
            BigDecimal currentAmps,
            Map<String, String> propsByCode) {
        if (state == null) {
            return false;
        }
        String stateLower = normalize(machineStateText);
        if (stateLower.equals("сварка") || stateLower.equals("welding")
                || stateLower.contains("сварка") || stateLower.contains("welding")) {
            return true;
        }
        if (state.getWeldingMachineStatus() == WeldingMachineStatus.Welding) {
            return true;
        }
        BigDecimal weldI = propsByCode != null ? parseDecimal(propsByCode.get("WeldingCurrent")) : null;
        if (weldI != null && weldI.compareTo(new BigDecimal("10")) > 0) {
            BigDecimal setI = currentAmps != null ? currentAmps : parseDecimal(propsByCode.get("Current"));
            if (setI == null || weldI.compareTo(setI.add(new BigDecimal("5"))) > 0) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).trim();
    }

    public static String pickMachineStateText(Map<String, String> propsByCode) {
        if (propsByCode == null || propsByCode.isEmpty()) {
            return null;
        }
        for (String key : new String[]{"Состояние аппарата", "WeldingMachineState", "State.WeldingMachineState"}) {
            String v = propsByCode.get(key);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    public static BigDecimal pickCurrentAmps(Map<String, String> propsByCode) {
        if (propsByCode == null) {
            return null;
        }
        for (String key : new String[]{"State.I", "Ток", "Current", "current"}) {
            BigDecimal v = parseDecimal(propsByCode.get(key));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    public static BigDecimal pickVoltageVolts(Map<String, String> propsByCode) {
        if (propsByCode == null) {
            return null;
        }
        // Сначала Voltage (Core: десятые вольта), как на фронте — State.U часто 0 и не должен перекрывать.
        for (String key : new String[]{"Voltage", "voltage", "Напряжение", "State.U"}) {
            BigDecimal v = parseVoltageProperty(key, propsByCode.get(key));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static BigDecimal parseVoltageProperty(String key, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if ("State.U".equals(key)) {
            String t = raw.trim();
            try {
                if (t.matches("(?i)[0-9A-F]+")) {
                    int dec = Integer.parseInt(t, 16);
                    if (dec > 0) {
                        return new BigDecimal(dec).movePointLeft(1);
                    }
                    return null;
                }
            } catch (NumberFormatException ignored) {
                /* decimal fallback below */
            }
        }
        BigDecimal v = parseDecimal(raw);
        if (v == null) {
            return null;
        }
        if (v.compareTo(new BigDecimal("100")) > 0) {
            return v.movePointLeft(1);
        }
        if (v.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return v;
    }

    public static BigDecimal pickGasFlowLpm(Map<String, String> propsByCode) {
        if (propsByCode == null) {
            return null;
        }
        for (String key : new String[]{"State.GasFlow", "GasFlow", "gasFlow"}) {
            BigDecimal v = parseDecimal(propsByCode.get(key));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
