package org.alloy.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorePacketArcTest {

    @Test
    void arcActive_whenStateWelding() {
        CorePacket p = new CorePacket();
        p.weldingMachineState = 1;
        p.weldingCurrent = 180;
        p.current = 150;
        assertTrue(p.isArcActive());
        assertEquals(180, p.getDisplayCurrent(), 0.01);
    }

    @Test
    void arcActive_whenWeldingCurrentAboveSetpointDespiteStateZero() {
        CorePacket p = new CorePacket();
        p.weldingMachineState = 0;
        p.weldingCurrent = 180;
        p.current = 150;
        p.weldingVoltage = 180;
        p.voltage = 50;
        assertTrue(p.isArcActive());
        assertEquals(180, p.getDisplayCurrent(), 0.01);
        assertEquals(18.0, p.getDisplayVoltage(), 0.01);
    }

    @Test
    void notArc_whenIdleCurrent() {
        CorePacket p = new CorePacket();
        p.weldingMachineState = 0;
        p.weldingCurrent = 0;
        p.current = 152;
        assertFalse(p.isArcActive());
        assertEquals(152, p.getDisplayCurrent(), 0.01);
    }

    @Test
    void notArc_whenStandbyStateDespiteHighWeldingCurrent() {
        CorePacket p = new CorePacket();
        p.weldingMachineState = 4;
        p.weldingCurrent = 337;
        p.current = 270;
        p.weldingVoltage = 307;
        p.voltage = 50;
        assertTrue(p.isStandbyState());
        assertFalse(p.isArcActive());
        assertEquals(270, p.getDisplayCurrent(), 0.01);
        assertEquals(5.0, p.getDisplayVoltage(), 0.01);
    }
}
