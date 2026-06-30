package org.alloy.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeldingMachineDailyStatsGasTest {

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    @Test
    void smallGlitchDrop_isNotPowerOnReset() {
        assertFalse(WeldingMachineDailyStatsService.isGasCounterPowerOnReset(bd("2767.5"), bd("2775.7")));
        assertFalse(WeldingMachineDailyStatsService.isGasCounterPowerOnReset(bd("2817.2"), bd("3106.0")));
    }

    @Test
    void largeDropNearZero_isPowerOnReset() {
        assertTrue(WeldingMachineDailyStatsService.isGasCounterPowerOnReset(bd("50"), bd("3000")));
    }

    @Test
    void glitchDrop_doesNotInflateDailySum() {
        BigDecimal last = bd("2701");
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal current : new BigDecimal[]{
                bd("2750"), bd("2767.5"), bd("2780"), bd("3106")}) {
            sum = sum.add(WeldingMachineDailyStatsService.sumGasCumulativeDelta(last, current));
            if (current.compareTo(last) >= 0
                    || WeldingMachineDailyStatsService.isGasCounterPowerOnReset(current, last)) {
                last = current;
            }
        }
        assertEquals(0, sum.compareTo(bd("405")));
    }

    @Test
    void realReset_addsConsumptionSincePowerOn() {
        BigDecimal last = bd("3000");
        BigDecimal sum = WeldingMachineDailyStatsService.sumGasCumulativeDelta(last, bd("50"));
        last = bd("50");
        sum = sum.add(WeldingMachineDailyStatsService.sumGasCumulativeDelta(last, bd("80")));
        assertEquals(0, sum.compareTo(bd("80")));
    }

    @Test
    void weldWindow_sumsCumulativeWithGlitchIgnored() {
        java.time.LocalDateTime t0 = java.time.LocalDateTime.of(2026, 6, 30, 9, 59, 0);
        java.time.LocalDateTime t1 = java.time.LocalDateTime.of(2026, 6, 30, 10, 5, 0);
        java.time.LocalDateTime t2 = java.time.LocalDateTime.of(2026, 6, 30, 10, 8, 0);
        java.time.LocalDateTime t3 = java.time.LocalDateTime.of(2026, 6, 30, 10, 9, 0);
        java.time.LocalDateTime weldStart = java.time.LocalDateTime.of(2026, 6, 30, 10, 0, 0);
        java.time.LocalDateTime weldEnd = java.time.LocalDateTime.of(2026, 6, 30, 10, 10, 0);

        org.alloy.models.entities.WeldingMachineState s0 = state(1L, t0);
        org.alloy.models.entities.WeldingMachineState s1 = state(2L, t1);
        org.alloy.models.entities.WeldingMachineState s2 = state(3L, t2);
        org.alloy.models.entities.WeldingMachineState s3 = state(4L, t3);

        java.util.Map<Long, BigDecimal> cum = new java.util.HashMap<>();
        cum.put(1L, bd("100"));
        cum.put(2L, bd("110"));
        cum.put(3L, bd("107")); // глюк
        cum.put(4L, bd("115"));

        BigDecimal liters = WeldingMachineDailyStatsService.sumGasCumulativeLitersInWindow(
                java.util.List.of(s0, s1, s2, s3), cum, weldStart, weldEnd);
        assertEquals(0, liters.compareTo(bd("15")));
    }

    private static org.alloy.models.entities.WeldingMachineState state(Long id, java.time.LocalDateTime created) {
        org.alloy.models.entities.WeldingMachineState s = new org.alloy.models.entities.WeldingMachineState();
        s.setId(id);
        s.setDateCreated(created);
        return s;
    }
}
