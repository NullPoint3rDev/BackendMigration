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

    @Test
    void flowFallback_estimatesLitersWhenCounterHasNoDelta() {
        java.time.LocalDateTime t0 = java.time.LocalDateTime.of(2026, 6, 30, 17, 9, 58);
        java.time.LocalDateTime weldStart = t0;
        java.time.LocalDateTime weldEnd = t0.plusSeconds(9);

        org.alloy.models.entities.WeldingMachineState s0 = state(1L, t0);
        java.util.Map<Long, BigDecimal> cum = new java.util.HashMap<>();
        cum.put(1L, bd("100.0")); // нет дельты — один сэмпл
        java.util.Map<Long, BigDecimal> flow = new java.util.HashMap<>();
        flow.put(1L, bd("12.0")); // 12 л/мин × 9 с / 60 = 1.8 л

        BigDecimal fromCounter = WeldingMachineDailyStatsService.sumGasCumulativeLitersInWindow(
                java.util.List.of(s0), cum, weldStart, weldEnd);
        assertEquals(0, fromCounter.compareTo(bd("0.000")));

        BigDecimal estimated = WeldingMachineDailyStatsService.estimateGasLitersFromInstantFlow(
                java.util.List.of(s0), flow, weldStart, weldEnd, bd("9"));
        assertEquals(0, estimated.compareTo(bd("1.800")));
    }

    @Test
    void flowFallback_prefersInWindowAverage_andIgnoresZeroFlow() {
        java.time.LocalDateTime weldStart = java.time.LocalDateTime.of(2026, 6, 30, 10, 0, 0);
        java.time.LocalDateTime weldEnd = weldStart.plusSeconds(10);
        org.alloy.models.entities.WeldingMachineState before = state(1L, weldStart.minusSeconds(3));
        org.alloy.models.entities.WeldingMachineState in = state(2L, weldStart.plusSeconds(2));
        org.alloy.models.entities.WeldingMachineState zero = state(3L, weldStart.plusSeconds(4));

        java.util.Map<Long, BigDecimal> flow = new java.util.HashMap<>();
        flow.put(1L, bd("30.0")); // вне окна — не должен тянуть среднее, если есть in-window
        flow.put(2L, bd("12.0"));
        flow.put(3L, bd("0"));

        BigDecimal avg = WeldingMachineDailyStatsService.averagePositiveGasFlowLpm(
                java.util.List.of(before, in, zero), flow, weldStart, weldEnd);
        assertEquals(0, avg.compareTo(bd("12.0000")));

        BigDecimal liters = WeldingMachineDailyStatsService.estimateGasLitersFromInstantFlow(
                java.util.List.of(before, in, zero), flow, weldStart, weldEnd, bd("10"));
        assertEquals(0, liters.compareTo(bd("2.000")));
    }

    @Test
    void flowFallback_usesPaddedStatesWhenWindowEmpty() {
        java.time.LocalDateTime weldStart = java.time.LocalDateTime.of(2026, 6, 30, 10, 0, 0);
        java.time.LocalDateTime weldEnd = weldStart.plusSeconds(9);
        // точка чуть до окна (типичный ±5s pad)
        org.alloy.models.entities.WeldingMachineState near = state(1L, weldStart.minusSeconds(2));
        java.util.Map<Long, BigDecimal> flow = java.util.Map.of(1L, bd("15.0"));

        BigDecimal liters = WeldingMachineDailyStatsService.estimateGasLitersFromInstantFlow(
                java.util.List.of(near), flow, weldStart, weldEnd, bd("9"));
        // 15 × 9 / 60 = 2.25
        assertEquals(0, liters.compareTo(bd("2.250")));
    }

    @Test
    void resolveGas_overridesTinyCounterWhenFlowEstimateMuchLarger() {
        // кейс 17с / 0.3 л при GasFlow ~18 л/мин → оценка 5.1 л (≥3×0.3)
        java.time.LocalDateTime t0 = java.time.LocalDateTime.of(2026, 6, 30, 17, 6, 39);
        java.time.LocalDateTime t1 = t0.plusSeconds(8);
        java.time.LocalDateTime weldStart = t0;
        java.time.LocalDateTime weldEnd = t0.plusSeconds(17);

        org.alloy.models.entities.WeldingMachineState s0 = state(1L, t0);
        org.alloy.models.entities.WeldingMachineState s1 = state(2L, t1);
        java.util.Map<Long, BigDecimal> cum = new java.util.HashMap<>();
        cum.put(1L, bd("100.0"));
        cum.put(2L, bd("100.3")); // дельта 0.3
        java.util.Map<Long, BigDecimal> flow = new java.util.HashMap<>();
        flow.put(1L, bd("18.0"));
        flow.put(2L, bd("18.0"));

        BigDecimal liters = WeldingMachineDailyStatsService.resolveGasLitersForWeldSegment(
                java.util.List.of(s0, s1), cum, flow, weldStart, weldEnd, bd("17"));
        assertEquals(0, liters.compareTo(bd("5.100")));
    }

    @Test
    void resolveGas_keepsCounterWhenCloseToFlowEstimate() {
        java.time.LocalDateTime t0 = java.time.LocalDateTime.of(2026, 6, 30, 10, 0, 0);
        java.time.LocalDateTime t1 = t0.plusSeconds(30);
        java.time.LocalDateTime weldStart = t0;
        java.time.LocalDateTime weldEnd = t0.plusSeconds(60);

        org.alloy.models.entities.WeldingMachineState s0 = state(1L, t0);
        org.alloy.models.entities.WeldingMachineState s1 = state(2L, t1);
        java.util.Map<Long, BigDecimal> cum = new java.util.HashMap<>();
        cum.put(1L, bd("100.0"));
        cum.put(2L, bd("115.0")); // дельта 15 л
        java.util.Map<Long, BigDecimal> flow = new java.util.HashMap<>();
        flow.put(1L, bd("18.0")); // 18×60/60 = 18 л — меньше 3×15
        flow.put(2L, bd("18.0"));

        BigDecimal liters = WeldingMachineDailyStatsService.resolveGasLitersForWeldSegment(
                java.util.List.of(s0, s1), cum, flow, weldStart, weldEnd, bd("60"));
        assertEquals(0, liters.compareTo(bd("15.000")));
    }

    @Test
    void dayBounds_moscowStatDate_convertsToUtcForDb() {
        java.time.ZoneId moscow = java.time.ZoneId.of("Europe/Moscow");
        java.time.LocalDate statDate = java.time.LocalDate.of(2026, 7, 7);
        java.time.ZonedDateTime now = java.time.ZonedDateTime.of(2026, 7, 7, 0, 30, 0, 0, moscow);
        WeldingMachineDailyStatsService.DayBoundsUtc bounds =
                WeldingMachineDailyStatsService.dayBoundsForStatDate(statDate, moscow, now);
        assertEquals(java.time.LocalDateTime.of(2026, 7, 6, 21, 1, 0), bounds.dayStart);
        assertEquals(java.time.LocalDateTime.of(2026, 7, 7, 21, 0, 0), bounds.dayEnd);
        assertEquals(java.time.LocalDateTime.of(2026, 7, 6, 21, 30, 0), bounds.effectiveEnd);
    }

    @Test
    void offTiling_headTailAndFullDayClampCorrectly() {
        java.time.LocalDateTime dayStart = java.time.LocalDateTime.of(2026, 6, 30, 0, 1, 0);
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 6, 30, 20, 0, 0);
        java.time.LocalDateTime firstPacket = java.time.LocalDateTime.of(2026, 6, 30, 3, 0, 0);
        java.time.LocalDateTime lastCovered = java.time.LocalDateTime.of(2026, 6, 30, 19, 13, 0);

        // Голова: [00:01, 03:00] = 2ч59м.
        assertEquals(2L * 3600_000 + 59L * 60_000,
                WeldingMachineDailyStatsService.gapMsWithin(dayStart, firstPacket, dayStart, now));
        // Хвост: [19:13, 20:00] = 47м.
        assertEquals(47L * 60_000,
                WeldingMachineDailyStatsService.gapMsWithin(lastCovered, now, dayStart, now));
        // Пустой день: весь интервал [00:01, 20:00] = 19ч59м.
        assertEquals(19L * 3600_000 + 59L * 60_000,
                WeldingMachineDailyStatsService.gapMsWithin(dayStart, now, dayStart, now));
        // Кламп: интервал вне окна → 0.
        assertEquals(0L, WeldingMachineDailyStatsService.gapMsWithin(now, dayStart, dayStart, now));
        // Кламп: часть до окна отсекается (состояние началось до 00:01).
        assertEquals(0L, WeldingMachineDailyStatsService.gapMsWithin(
                dayStart.minusHours(1), dayStart, dayStart, now));
    }

    @Test
    void wireCumulative_sumsRiseIgnoresNoiseHandlesReset() {
        // Монотонный рост (как в БД сегодня): 2387.1 → 2406.4 → 2417.1 = +30.0 м.
        assertEquals(0, WeldingMachineDailyStatsService.sumWireCumulativeMeters(
                java.util.List.of(bd("2387.1"), bd("2406.4"), bd("2417.1"))).compareTo(bd("30.0")));
        // Реальный сброс «с включения» (падение >2x): 2800 → 50 → 80 = 50 + 30 = 80 м.
        assertEquals(0, WeldingMachineDailyStatsService.sumWireCumulativeMeters(
                java.util.List.of(bd("2800"), bd("50"), bd("80"))).compareTo(bd("80")));
        // Шумовые V-просадки (это раздувало сумму до сотен кг) не должны накручивать:
        // 2500 → 2400 → 2500 → 2400 → 2500 = 0 м (счётчик вернулся к тому же уровню).
        assertEquals(0, WeldingMachineDailyStatsService.sumWireCumulativeMeters(
                java.util.List.of(bd("2500"), bd("2400"), bd("2500"), bd("2400"), bd("2500")))
                .compareTo(BigDecimal.ZERO));
        // Шум + реальный рост: 2500 → 2400(шум) → 2600 = +100 (от исходных 2500), не +200.
        assertEquals(0, WeldingMachineDailyStatsService.sumWireCumulativeMeters(
                java.util.List.of(bd("2500"), bd("2400"), bd("2600"))).compareTo(bd("100")));
        // Одно значение / пусто → 0.
        assertEquals(0, WeldingMachineDailyStatsService.sumWireCumulativeMeters(
                java.util.List.of(bd("100"))).compareTo(BigDecimal.ZERO));
        assertEquals(0, WeldingMachineDailyStatsService.sumWireCumulativeMeters(
                java.util.List.of()).compareTo(BigDecimal.ZERO));
    }

    private static org.alloy.models.entities.WeldingMachineState state(Long id, java.time.LocalDateTime created) {
        org.alloy.models.entities.WeldingMachineState s = new org.alloy.models.entities.WeldingMachineState();
        s.setId(id);
        s.setDateCreated(created);
        return s;
    }
}