package org.alloy.services;

import org.alloy.models.MonitorActivityMode;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.entities.WeldingMachineState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
