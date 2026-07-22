package org.alloy.services;

import org.alloy.models.MonitorActivityMode;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.entities.WeldingMachineState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
    void blankStateTextWithCurrentIsNotWelding() {
        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Idle);
        assertFalse(MonitorActivityClassifier.isWelding(state, null, new BigDecimal("130")));
        assertFalse(MonitorActivityClassifier.isWelding(state, "  ", new BigDecimal("130")));
        assertEquals(MonitorActivityMode.on,
                MonitorActivityClassifier.classify(state, null, new BigDecimal("130")));
    }

    @Test
    void weldingCurrentAboveSetpointCountsAsWelding() {
        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Idle);
        Map<String, String> props = new HashMap<>();
        props.put("Current", "152");
        props.put("WeldingCurrent", "180");
        assertTrue(MonitorActivityClassifier.isWelding(state, "Аппарат включен", new BigDecimal("152"), props));
        assertEquals(MonitorActivityMode.welding,
                MonitorActivityClassifier.classify(state, "Аппарат включен", new BigDecimal("152"), props));
    }

    @Test
    void pickVoltagePrefersVoltageOverZeroStateU() {
        Map<String, String> props = new HashMap<>();
        props.put("State.U", "0");
        props.put("Voltage", "304");
        assertEquals(0, new BigDecimal("30.4").compareTo(MonitorActivityClassifier.pickVoltageVolts(props)));
    }

    @Test
    void activeErrorBlocksWelding_evenWithSvarkaTextAndHighCurrent() {
        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Welding);
        state.setErrorCode("4");
        Map<String, String> props = new HashMap<>();
        props.put("Current", "200");
        props.put("WeldingCurrent", "250");
        assertFalse(MonitorActivityClassifier.isWelding(state, "Сварка", new BigDecimal("200"), props));
        assertEquals(MonitorActivityMode.error,
                MonitorActivityClassifier.classify(state, "Сварка", new BigDecimal("200"), props));
    }
}
