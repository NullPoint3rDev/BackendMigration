package org.alloy.services;

import org.alloy.models.entities.WeldingMachineState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeldingStateDurationUtilTest {

    @Test
    void lastState_withoutOpenEnd_hasZeroDuration() {
        WeldingMachineState a = state(1L, LocalDateTime.of(2026, 7, 14, 9, 0, 0));
        WeldingMachineState b = state(2L, LocalDateTime.of(2026, 7, 14, 9, 10, 0));
        List<WeldingMachineState> list = List.of(a, b);
        assertEquals(600_000L, WeldingStateDurationUtil.effectiveStateDurationMs(a, list, null));
        assertEquals(0L, WeldingStateDurationUtil.effectiveStateDurationMs(b, list, null));
    }

    @Test
    void lastState_withOpenEnd_extendsToNow() {
        WeldingMachineState a = state(1L, LocalDateTime.of(2026, 7, 14, 9, 0, 0));
        WeldingMachineState b = state(2L, LocalDateTime.of(2026, 7, 14, 9, 10, 0));
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 12, 0);
        List<WeldingMachineState> list = List.of(a, b);
        long lastMs = WeldingStateDurationUtil.effectiveStateDurationMs(b, list, now);
        assertEquals(120_000L, lastMs);
        assertTrue(lastMs > 0);
    }

    private static WeldingMachineState state(long id, LocalDateTime created) {
        WeldingMachineState s = new WeldingMachineState();
        s.setId(id);
        s.setDateCreated(created);
        return s;
    }
}
