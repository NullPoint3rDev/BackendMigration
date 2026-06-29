package org.alloy.services;

import org.alloy.models.MonitorActivityMode;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.entities.WeldingMachineState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitorActivityClassifierTest {

    @Test
    void standbyCountsAsOff_notOn() {
        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Idle);
        MonitorActivityMode mode = MonitorActivityClassifier.classify(
                state, "Аппарат включен в дежурном режиме", null);
        assertEquals(MonitorActivityMode.off, mode);
    }

    @Test
    void waitingCountsAsOn() {
        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Idle);
        MonitorActivityMode mode = MonitorActivityClassifier.classify(
                state, "Аппарат в режиме ожидания", null);
        assertEquals(MonitorActivityMode.on, mode);
    }

    @Test
    void svarkaTextCountsAsWelding() {
        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Idle);
        assertTrue(MonitorActivityClassifier.isWelding(state, "Сварка", null));
        assertEquals(MonitorActivityMode.welding,
                MonitorActivityClassifier.classify(state, "Сварка", new BigDecimal("314")));
    }

    @Test
    void onWithHighCurrentAndGasIsNotWelding() {
        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Idle);
        MonitorActivityMode mode = MonitorActivityClassifier.classify(
                state,
                "Аппарат включен",
                new BigDecimal("314"),
                new BigDecimal("2.5"),
                new BigDecimal("31.5"));
        assertEquals(MonitorActivityMode.on, mode);
        assertFalse(MonitorActivityClassifier.isWelding(state, "Аппарат включен", new BigDecimal("314")));
    }

    @Test
    void pickVoltagePrefersVoltageOverZeroStateU() {
        Map<String, String> props = new java.util.HashMap<>();
        props.put("State.U", "0");
        props.put("Voltage", "304");
        assertEquals(0, new BigDecimal("30.4").compareTo(MonitorActivityClassifier.pickVoltageVolts(props)));
    }
}
