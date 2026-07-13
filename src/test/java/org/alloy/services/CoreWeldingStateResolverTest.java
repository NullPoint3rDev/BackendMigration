package org.alloy.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreWeldingStateResolverTest {

    @Test
    void resolveCoreWeldingStateVal_respectsExplicitWeldingState() {
        CorePacket core = new CorePacket();
        core.weldingMachineState = 1;
        core.weldingCurrent = 130;
        core.current = 152;
        assertEquals(1, WeldingDataParserService.resolveCoreWeldingStateVal(core));
    }

    @Test
    void resolveCoreWeldingStateVal_promotesArcWhenStateByteIdleButWeldCurrentDiffersFromSetpoint() {
        CorePacket core = new CorePacket();
        core.weldingMachineState = 0;
        core.weldingCurrent = 130;
        core.current = 152;
        assertEquals(1, WeldingDataParserService.resolveCoreWeldingStateVal(core));
    }

    @Test
    void resolveCoreWeldingStateVal_keepsIdleWhenSetpointEqualsWeldingCurrentField() {
        CorePacket core = new CorePacket();
        core.weldingMachineState = 0;
        core.weldingCurrent = 152;
        core.current = 152;
        assertEquals(0, WeldingDataParserService.resolveCoreWeldingStateVal(core));
    }

    @Test
    void coreArcLooksActive_requiresMeaningfulWeldCurrent() {
        CorePacket core = new CorePacket();
        core.weldingCurrent = 5;
        core.current = 152;
        assertFalse(WeldingDataParserService.coreArcLooksActive(core));
    }
}
