package org.alloy.services;

import org.alloy.models.MonitorActivityMode;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.entities.WeldingMachineState;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * Классификация режима плиток мониторинга по тексту состояния и току (как DeviceMonitorPage).
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
        if (stateLower.contains("дежур") || stateLower.contains("standby")
                || stateLower.contains("waiting") || stateLower.contains("ожидан")
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

    private static boolean isWelding(
            WeldingMachineState state,
            String machineStateText,
            BigDecimal currentAmps) {
        String stateLower = normalize(machineStateText);
        if (stateLower.equals("сварка") || stateLower.equals("welding")
                || stateLower.contains("сварка") || stateLower.contains("welding")
                || stateLower.contains("сварочн") || stateLower.contains("weld")) {
            return true;
        }
        if (state.getWeldingMachineStatus() == WeldingMachineStatus.Welding) {
            return true;
        }
        if ((machineStateText == null || machineStateText.isBlank())
                && currentAmps != null
                && currentAmps.compareTo(BigDecimal.ONE) > 0) {
            return true;
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
