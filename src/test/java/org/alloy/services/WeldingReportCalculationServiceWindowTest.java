package org.alloy.services;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WeldingReportCalculationServiceWindowTest {

    @Test
    void resolveServerWindowEnd_prefersServerEndOverDeviceDuration() {
        LocalDateTime weldStart = LocalDateTime.of(2026, 6, 29, 12, 0);
        LocalDateTime serverEnd = LocalDateTime.of(2026, 6, 29, 12, 5);
        assertEquals(serverEnd, WeldingReportCalculationService.resolveServerWindowEnd(weldStart, serverEnd, 97_000L));
    }

    /**
     * ponytail: device clock 09:50 must not cut off server states at 12:00 when searching I/U.
     */
    @Test
    void statesInTimeWindow_usesServerEndNotDeviceClock() throws Exception {
        LocalDateTime weldStart = LocalDateTime.of(2026, 6, 29, 12, 0);
        LocalDateTime serverEnd = LocalDateTime.of(2026, 6, 29, 12, 5);
        LocalDateTime deviceEnd = LocalDateTime.of(2026, 6, 29, 9, 50);

        List<org.alloy.models.entities.WeldingMachineState> sorted = new ArrayList<>();
        org.alloy.models.entities.WeldingMachineState duringWeld = new org.alloy.models.entities.WeldingMachineState();
        duringWeld.setId(100L);
        duringWeld.setDateCreated(LocalDateTime.of(2026, 6, 29, 12, 2));
        sorted.add(duringWeld);

        java.lang.reflect.Method m = WeldingReportCalculationService.class.getDeclaredMethod(
                "statesInTimeWindow", List.class, LocalDateTime.class, LocalDateTime.class);
        m.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<org.alloy.models.entities.WeldingMachineState> wrongWindow = (List<org.alloy.models.entities.WeldingMachineState>) m.invoke(
                null, sorted, weldStart.minusMinutes(5), deviceEnd.plusMinutes(5));
        assertEquals(0, wrongWindow.size(), "device end must not bound server state search");

        @SuppressWarnings("unchecked")
        List<org.alloy.models.entities.WeldingMachineState> correctWindow = (List<org.alloy.models.entities.WeldingMachineState>) m.invoke(
                null, sorted, weldStart.minusMinutes(5), serverEnd.plusMinutes(5));
        assertFalse(correctWindow.isEmpty(), "server end must include weld states");
    }
}
