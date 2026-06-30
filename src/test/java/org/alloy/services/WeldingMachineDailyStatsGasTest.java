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
}
