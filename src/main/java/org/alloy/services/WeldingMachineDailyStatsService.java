package org.alloy.services;

import org.alloy.models.MonitorActivityMode;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.dto.WeldingMachineDailyStatsDTO;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineDailyStats;
import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.repositories.WeldingMachineDailyStatsRepository;
import org.alloy.repositories.WeldingMachineParameterValueRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class WeldingMachineDailyStatsService {

    private static String recomputeLockKey(Integer weldingMachineId, LocalDate statDate) {
        return weldingMachineId + "|" + statDate;
    }

    private static final String WIRE_PARAM = "Расход проволоки";
    private static final String GAS_CUMULATIVE_PARAM = "Core.GasConsumptionSincePowerOn";
    private static final List<String> STATE_TEXT_KEYS = List.of(
            "Состояние аппарата", "WeldingMachineState", "State.WeldingMachineState");
    private static final List<String> CURRENT_KEYS = List.of("State.I", "Ток", "Current", "current");
    private static final List<String> VOLTAGE_KEYS = List.of("State.U", "Напряжение", "Voltage", "voltage");
    private static final List<String> GAS_FLOW_KEYS = List.of("State.GasFlow", "GasFlow", "gasFlow");

    @Value("${monitor.daily-stats.timezone:Europe/Moscow}")
    private String timezoneId;

    @Value("${monitor.daily-stats.stale-seconds:60}")
    private long staleSeconds;

    @Value("${monitor.daily-stats.recompute-debounce-ms:15000}")
    private long recomputeDebounceMs;

    @Value("${report.wire.linear-density-kg-per-meter:0.000089}")
    private BigDecimal wireLinearDensityKgPerMeter;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private WeldingMachineParameterValueRepository parameterValueRepository;

    @Autowired
    private WeldingMachineDailyStatsRepository dailyStatsRepository;

    @Autowired
    private WeldingMachineDailyStatsAsyncExecutor asyncExecutor;

    /** machineId|statDate → монитор: один пересчёт суток на JVM, без перезаписи устаревшим async. */
    private final ConcurrentHashMap<String, Object> recomputeLocks = new ConcurrentHashMap<>();

    /** machineId|statDate → время последней постановки пересчёта в очередь */
    private final ConcurrentHashMap<String, Long> lastRecomputeScheduledMs = new ConcurrentHashMap<>();

    /**
     * UI: для сегодня при устаревшем кэше — синхронный пересчёт (ответ = то, что в БД).
     * Прошлые даты — async по расписанию/дебаунсу.
     */
    @Transactional
    public WeldingMachineDailyStatsDTO getDailyStatsByMac(String mac, LocalDate statDate) {
        WeldingMachine machine = resolveMachine(mac);
        LocalDate day = statDate != null ? statDate : today();
        WeldingMachineDailyStats row = dailyStatsRepository
                .findByWeldingMachineIdAndStatDate(machine.getId(), day)
                .orElseGet(() -> emptyStats(machine.getId(), day));
        if (day.equals(today()) && row.getGasBaselineAtDayStartL() == null) {
            row = recomputeDay(machine.getId(), day);
        } else if (shouldScheduleRecompute(row, day)) {
            if (isInvalidatedCache(row) || day.equals(today())) {
                row = recomputeDay(machine.getId(), day);
            } else {
                scheduleRecompute(machine.getId(), day);
            }
        }
        return toDto(machine, row);
    }

    /** Помечено миграцией V1_17 — нужен синхронный пересчёт, иначе плитка показывает старый gas_consumption_l. */
    private static boolean isInvalidatedCache(WeldingMachineDailyStats row) {
        return row.getComputedAt() != null && row.getComputedAt().getYear() < 2000;
    }

    public void scheduleRecompute(Integer weldingMachineId, LocalDate statDate) {
        if (weldingMachineId == null || statDate == null) {
            return;
        }
        if (statDate.equals(today())) {
            recomputeDay(weldingMachineId, statDate);
            return;
        }
        String key = recomputeLockKey(weldingMachineId, statDate);
        long now = System.currentTimeMillis();
        Long prev = lastRecomputeScheduledMs.get(key);
        if (prev != null && now - prev < recomputeDebounceMs) {
            return;
        }
        lastRecomputeScheduledMs.put(key, now);
        asyncExecutor.recomputeDayAsync(weldingMachineId, statDate);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WeldingMachineDailyStats recomputeDay(Integer weldingMachineId, LocalDate statDate) {
        Object lock = recomputeLocks.computeIfAbsent(recomputeLockKey(weldingMachineId, statDate), k -> new Object());
        synchronized (lock) {
            Optional<WeldingMachineDailyStats> cached = dailyStatsRepository
                    .findByWeldingMachineIdAndStatDate(weldingMachineId, statDate);
            if (cached.isPresent()) {
                WeldingMachineDailyStats row = cached.get();
                if (!isInvalidatedCache(row) && !shouldScheduleRecompute(row, statDate)) {
                    return row;
                }
            }
            return recomputeDayAndSave(weldingMachineId, statDate);
        }
    }

    private WeldingMachineDailyStats recomputeDayAndSave(Integer weldingMachineId, LocalDate statDate) {
        ZoneId zone = ZoneId.of(timezoneId);
        // Сутки для плитки «Расход за сутки» — с 00:01 (не с полуночи).
        LocalDateTime dayStart = statDate.atTime(0, 1);
        ZonedDateTime dayEndZ = statDate.plusDays(1).atStartOfDay(zone);
        LocalDateTime dayEnd = dayEndZ.toLocalDateTime();
        ZonedDateTime nowZ = ZonedDateTime.now(zone);
        LocalDateTime effectiveEnd = statDate.equals(nowZ.toLocalDate())
                ? (nowZ.toLocalDateTime().isBefore(dayEnd) ? nowZ.toLocalDateTime() : dayEnd)
                : dayEnd;

        List<WeldingMachineState> states = weldingMachineStateRepository.findByWeldingMachineIdAndDateRangeAsc(
                weldingMachineId, dayStart, dayEnd);
        states.sort(Comparator.comparing(WeldingMachineState::getDateCreated, Comparator.nullsLast(Comparator.naturalOrder())));

        long offMs = 0L;
        long errorMs = 0L;
        long onMs = 0L;
        long weldingMs = 0L;

        Map<Long, Map<String, String>> propsByStateId = loadPropsByStateId(states);
        Map<Long, BigDecimal> wireFeedByStateId = loadWireFeedByStateId(states);

        // ponytail: не тянем последний poll до now() — таймеры скачут при смене статуса на последнем опросе
        LocalDateTime openEndIfLast = null;

        for (WeldingMachineState s : states) {
            long overlapMs = WeldingStateDurationUtil.overlapDurationMs(
                    s, dayStart, effectiveEnd, states, openEndIfLast);
            if (overlapMs <= 0) {
                continue;
            }
            Map<String, String> props = propsByStateId.getOrDefault(s.getId(), Collections.emptyMap());
            String stateText = MonitorActivityClassifier.pickMachineStateText(props);
            BigDecimal current = MonitorActivityClassifier.pickCurrentAmps(props);
            MonitorActivityMode mode = MonitorActivityClassifier.classify(s, stateText, current);
            switch (mode) {
                case welding:
                    weldingMs += overlapMs;
                    break;
                case error:
                    errorMs += overlapMs;
                    break;
                case on:
                    onMs += overlapMs;
                    break;
                default:
                    offMs += overlapMs;
                    break;
            }
        }

        BigDecimal wireKg = calculateWireKg(states, dayStart, effectiveEnd, wireFeedByStateId, openEndIfLast);

        LocalDateTime gasLookback = dayStart.minusDays(1);
        List<WeldingMachineState> statesForGas = weldingMachineStateRepository.findByWeldingMachineIdAndDateRangeAsc(
                weldingMachineId, gasLookback, dayEnd);
        statesForGas.sort(Comparator.comparing(WeldingMachineState::getDateCreated, Comparator.nullsLast(Comparator.naturalOrder())));
        Map<Long, BigDecimal> gasCumulativeByStateId = loadGasCumulativeByStateId(statesForGas);
        GasDayTotals gasTotals = calculateGasLiters(statesForGas, gasCumulativeByStateId, dayStart);

        Long lastStateId = states.isEmpty() ? null : states.get(states.size() - 1).getId();

        WeldingMachineDailyStats row = dailyStatsRepository
                .findByWeldingMachineIdAndStatDate(weldingMachineId, statDate)
                .orElseGet(WeldingMachineDailyStats::new);
        applyMonotonicTodayIfNeeded(row, statDate, offMs, errorMs, onMs, weldingMs, wireKg);
        row.setWeldingMachineId(weldingMachineId);
        row.setStatDate(statDate);
        row.setGasConsumptionL(gasTotals.consumptionL);
        row.setGasBaselineAtDayStartL(gasTotals.baselineAtDayStartL);
        row.setLastStateId(lastStateId);
        row.setComputedAt(LocalDateTime.now(zone));
        return dailyStatsRepository.save(row);
    }

    /** За сегодня таймеры и проволока в кэше только растут — без откатов на UI. */
    private void applyMonotonicTodayIfNeeded(
            WeldingMachineDailyStats row,
            LocalDate statDate,
            long offMs,
            long errorMs,
            long onMs,
            long weldingMs,
            BigDecimal wireKg) {
        if (!statDate.equals(today())) {
            row.setOffMs(offMs);
            row.setStandbyMs(errorMs);
            row.setOnMs(onMs);
            row.setWeldingMs(weldingMs);
            row.setWireConsumptionKg(wireKg != null ? wireKg : BigDecimal.ZERO);
            return;
        }
        row.setOffMs(Math.max(nullToZero(row.getOffMs()), offMs));
        row.setStandbyMs(Math.max(nullToZero(row.getStandbyMs()), errorMs));
        row.setOnMs(Math.max(nullToZero(row.getOnMs()), onMs));
        row.setWeldingMs(Math.max(nullToZero(row.getWeldingMs()), weldingMs));
        BigDecimal prevWire = row.getWireConsumptionKg() != null ? row.getWireConsumptionKg() : BigDecimal.ZERO;
        BigDecimal nextWire = wireKg != null ? wireKg : BigDecimal.ZERO;
        row.setWireConsumptionKg(prevWire.max(nextWire).setScale(5, RoundingMode.HALF_UP));
    }

    private static long nullToZero(Long v) {
        return v != null ? v : 0L;
    }

    /** Планировщик: по одной транзакции на аппарат, без удержания одного connection на весь цикл. */
    public void recomputeStaleMachinesSince(LocalDateTime since) {
        List<Integer> machineIds = dailyStatsRepository.findMachineIdsWithStatesSince(since);
        LocalDate today = today();
        for (Integer machineId : machineIds) {
            scheduleRecompute(machineId, today);
        }
    }

    private boolean shouldScheduleRecompute(WeldingMachineDailyStats row, LocalDate statDate) {
        if (isInvalidatedCache(row)) {
            return true;
        }
        if (!statDate.equals(today())) {
            return row.getComputedAt() == null;
        }
        if (row.getComputedAt() == null) {
            return true;
        }
        ZoneId zone = ZoneId.of(timezoneId);
        return row.getComputedAt().isBefore(LocalDateTime.now(zone).minusSeconds(staleSeconds));
    }

    private WeldingMachineDailyStats emptyStats(Integer weldingMachineId, LocalDate statDate) {
        WeldingMachineDailyStats row = new WeldingMachineDailyStats();
        row.setWeldingMachineId(weldingMachineId);
        row.setStatDate(statDate);
        row.setWireConsumptionKg(BigDecimal.ZERO);
        row.setGasConsumptionL(BigDecimal.ZERO);
        row.setOffMs(0L);
        row.setStandbyMs(0L);
        row.setOnMs(0L);
        row.setWeldingMs(0L);
        return row;
    }

    private BigDecimal calculateWireKg(
            List<WeldingMachineState> states,
            LocalDateTime dayStart,
            LocalDateTime dayEnd,
            Map<Long, BigDecimal> wireFeedByStateId,
            LocalDateTime openEndIfLast) {
        if (states == null || states.isEmpty() || wireLinearDensityKgPerMeter == null) {
            return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        }
        BigDecimal density = wireLinearDensityKgPerMeter;
        BigDecimal sum = BigDecimal.ZERO;
        for (WeldingMachineState s : states) {
            if (s.getWeldingMachineStatus() != WeldingMachineStatus.Welding) {
                continue;
            }
            long overlapMs = WeldingStateDurationUtil.overlapDurationMs(
                    s, dayStart, dayEnd, states, openEndIfLast);
            if (overlapMs <= 0) {
                continue;
            }
            BigDecimal mpm = wireFeedByStateId.get(s.getId());
            if (mpm == null || mpm.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal minutes = BigDecimal.valueOf(overlapMs)
                    .divide(BigDecimal.valueOf(60_000), 8, RoundingMode.HALF_UP);
            sum = sum.add(mpm.multiply(density).multiply(minutes));
        }
        return sum.setScale(5, RoundingMode.HALF_UP);
    }

    private Map<Long, Map<String, String>> loadPropsByStateId(List<WeldingMachineState> states) {
        Map<Long, Map<String, String>> out = new HashMap<>();
        if (states == null || states.isEmpty()) {
            return out;
        }
        List<Long> ids = states.stream().map(WeldingMachineState::getId).filter(id -> id != null).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return out;
        }
        List<String> keys = new ArrayList<>(STATE_TEXT_KEYS);
        keys.addAll(CURRENT_KEYS);
        keys.addAll(VOLTAGE_KEYS);
        keys.addAll(GAS_FLOW_KEYS);
        final int batchSize = 5000;
        for (int i = 0; i < ids.size(); i += batchSize) {
            List<Long> batch = ids.subList(i, Math.min(i + batchSize, ids.size()));
            List<WeldingMachineParameterValue> rows = parameterValueRepository.findByStateIdsAndPropertyCodes(batch, keys);
            mergeParameterRows(out, rows);
        }
        return out;
    }

    private Map<Long, BigDecimal> loadWireFeedByStateId(List<WeldingMachineState> states) {
        Map<Long, BigDecimal> out = new HashMap<>();
        if (states == null || states.isEmpty()) {
            return out;
        }
        List<Long> ids = states.stream().map(WeldingMachineState::getId).filter(id -> id != null).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return out;
        }
        final int batchSize = 5000;
        for (int i = 0; i < ids.size(); i += batchSize) {
            List<Long> batch = ids.subList(i, Math.min(i + batchSize, ids.size()));
            List<WeldingMachineParameterValue> rows = parameterValueRepository.findByStateIdsAndPropertyCode(batch, WIRE_PARAM);
            for (WeldingMachineParameterValue pv : rows) {
                putWireFeedValue(out, pv);
            }
            List<Long> missing = batch.stream().filter(id -> !out.containsKey(id)).collect(Collectors.toList());
            if (!missing.isEmpty()) {
                try {
                    List<Object[]> nativeRows = parameterValueRepository.findStateIdAndValueNativeCoalesce(missing, WIRE_PARAM);
                    for (Object[] row : nativeRows) {
                        if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                            continue;
                        }
                        try {
                            long stateId = ((Number) row[0]).longValue();
                            String valueStr = row[1].toString().trim().replace(',', '.');
                            if (!valueStr.isEmpty()) {
                                out.put(stateId, new BigDecimal(valueStr));
                            }
                        } catch (NumberFormatException ignored) {
                            /* skip */
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[DAILY-STATS] native wire load: " + e.getMessage());
                }
            }
        }
        return out;
    }

    private void mergeParameterRows(Map<Long, Map<String, String>> out, List<WeldingMachineParameterValue> rows) {
        for (WeldingMachineParameterValue pv : rows) {
            if (pv.getWeldingMachineStateId() == null) {
                continue;
            }
            String val = pv.getValue();
            if (val == null || val.isBlank()) {
                val = pv.getRawValue();
            }
            if (val == null || val.isBlank()) {
                continue;
            }
            out.computeIfAbsent(pv.getWeldingMachineStateId(), k -> new HashMap<>())
                    .put(pv.getPropertyCode(), val.trim());
        }
    }

    private Map<Long, BigDecimal> loadGasCumulativeByStateId(List<WeldingMachineState> states) {
        Map<Long, BigDecimal> out = new HashMap<>();
        if (states == null || states.isEmpty()) {
            return out;
        }
        List<Long> ids = states.stream().map(WeldingMachineState::getId).filter(id -> id != null).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return out;
        }
        final int batchSize = 5000;
        for (int i = 0; i < ids.size(); i += batchSize) {
            List<Long> batch = ids.subList(i, Math.min(i + batchSize, ids.size()));
            List<WeldingMachineParameterValue> rows = parameterValueRepository.findByStateIdsAndPropertyCode(batch, GAS_CUMULATIVE_PARAM);
            for (WeldingMachineParameterValue pv : rows) {
                putGasCumulativeValue(out, pv);
            }
        }
        return out;
    }

    private void putGasCumulativeValue(Map<Long, BigDecimal> out, WeldingMachineParameterValue pv) {
        if (pv.getWeldingMachineStateId() == null) {
            return;
        }
        String s = pv.getValue();
        if (s == null || s.isBlank()) {
            s = pv.getRawValue();
        }
        if (s == null || s.isBlank()) {
            return;
        }
        try {
            out.put(pv.getWeldingMachineStateId(), new BigDecimal(s.trim().replace(',', '.')));
        } catch (NumberFormatException ignored) {
            /* skip */
        }
    }

    /** Минимальное падение (л), чтобы считать сбросом счётчика, а не глюком связи. */
    private static final BigDecimal GAS_COUNTER_RESET_MIN_DROP_L = new BigDecimal("100");
    /** После сброса новое значение должно быть заметно ниже предыдущего (доля). */
    private static final BigDecimal GAS_COUNTER_RESET_MAX_RATIO = new BigDecimal("0.5");

    private GasDayTotals calculateGasLiters(
            List<WeldingMachineState> states,
            Map<Long, BigDecimal> gasCumulativeByStateId,
            LocalDateTime dayStart) {
        if (states == null || states.isEmpty() || gasCumulativeByStateId.isEmpty()) {
            return GasDayTotals.zero();
        }
        BigDecimal baselineAtDayStart = null;
        BigDecimal lastCumulative = null;
        BigDecimal sumDelta = BigDecimal.ZERO;
        for (WeldingMachineState s : states) {
            if (s.getId() == null || s.getDateCreated() == null) {
                continue;
            }
            BigDecimal current = gasCumulativeByStateId.get(s.getId());
            if (current == null) {
                continue;
            }
            if (s.getDateCreated().isBefore(dayStart)) {
                baselineAtDayStart = current;
                lastCumulative = current;
                continue;
            }
            if (baselineAtDayStart == null) {
                baselineAtDayStart = current;
            }
            if (lastCumulative != null) {
                sumDelta = sumDelta.add(sumGasCumulativeDelta(lastCumulative, current));
                if (current.compareTo(lastCumulative) >= 0 || isGasCounterPowerOnReset(current, lastCumulative)) {
                    lastCumulative = current;
                }
            } else {
                lastCumulative = current;
            }
        }
        if (baselineAtDayStart == null) {
            return GasDayTotals.zero();
        }
        return new GasDayTotals(sumDelta.setScale(3, RoundingMode.HALF_UP), baselineAtDayStart.setScale(3, RoundingMode.HALF_UP));
    }

    /**
     * ponytail: мелкие просадки накопительного счётчика (обрыв связи) не считаем сбросом.
     * Реальный сброс «с включения» — крупное падение и новое значение &lt; 50% от предыдущего.
     */
    static boolean isGasCounterPowerOnReset(BigDecimal current, BigDecimal lastCumulative) {
        if (current == null || lastCumulative == null) {
            return false;
        }
        BigDecimal drop = lastCumulative.subtract(current);
        if (drop.compareTo(GAS_COUNTER_RESET_MIN_DROP_L) < 0) {
            return false;
        }
        return current.compareTo(lastCumulative.multiply(GAS_COUNTER_RESET_MAX_RATIO)) < 0;
    }

    static BigDecimal sumGasCumulativeDelta(BigDecimal lastCumulative, BigDecimal current) {
        if (lastCumulative == null || current == null) {
            return BigDecimal.ZERO;
        }
        if (current.compareTo(lastCumulative) >= 0) {
            return current.subtract(lastCumulative);
        }
        if (isGasCounterPowerOnReset(current, lastCumulative)) {
            return current;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Расход газа (л) за полуинтервал [windowStart, windowEnd) по дельтам счётчика «с включения».
     * Используется в отчётах по швам и в суточной статистике.
     */
    static BigDecimal sumGasCumulativeLitersInWindow(
            List<WeldingMachineState> states,
            Map<Long, BigDecimal> gasCumulativeByStateId,
            LocalDateTime windowStart,
            LocalDateTime windowEnd) {
        if (states == null || states.isEmpty()
                || gasCumulativeByStateId == null || gasCumulativeByStateId.isEmpty()
                || windowStart == null || windowEnd == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        List<WeldingMachineState> sorted = new ArrayList<>(states);
        sorted.sort(Comparator.comparing(WeldingMachineState::getDateCreated, Comparator.nullsLast(Comparator.naturalOrder())));
        BigDecimal lastCumulative = null;
        BigDecimal sumDelta = BigDecimal.ZERO;
        for (WeldingMachineState s : sorted) {
            if (s.getId() == null || s.getDateCreated() == null) {
                continue;
            }
            BigDecimal current = gasCumulativeByStateId.get(s.getId());
            if (current == null) {
                continue;
            }
            LocalDateTime t = s.getDateCreated();
            if (t.isBefore(windowStart)) {
                lastCumulative = current;
                continue;
            }
            if (!t.isBefore(windowEnd)) {
                break;
            }
            if (lastCumulative != null) {
                sumDelta = sumDelta.add(sumGasCumulativeDelta(lastCumulative, current));
                if (current.compareTo(lastCumulative) >= 0 || isGasCounterPowerOnReset(current, lastCumulative)) {
                    lastCumulative = current;
                }
            } else {
                lastCumulative = current;
            }
        }
        return sumDelta.setScale(3, RoundingMode.HALF_UP);
    }

    private static final class GasDayTotals {
        final BigDecimal consumptionL;
        final BigDecimal baselineAtDayStartL;

        GasDayTotals(BigDecimal consumptionL, BigDecimal baselineAtDayStartL) {
            this.consumptionL = consumptionL != null ? consumptionL : BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
            this.baselineAtDayStartL = baselineAtDayStartL;
        }

        static GasDayTotals zero() {
            return new GasDayTotals(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP), null);
        }
    }

    private void putWireFeedValue(Map<Long, BigDecimal> out, WeldingMachineParameterValue pv) {
        if (pv.getWeldingMachineStateId() == null) {
            return;
        }
        String s = pv.getValue();
        if (s == null || s.isBlank()) {
            s = pv.getRawValue();
        }
        if (s == null || s.isBlank()) {
            return;
        }
        try {
            out.put(pv.getWeldingMachineStateId(), new BigDecimal(s.trim().replace(',', '.')));
        } catch (NumberFormatException ignored) {
            /* skip */
        }
    }

    private WeldingMachine resolveMachine(String mac) {
        if (mac == null || mac.isBlank()) {
            throw new IllegalArgumentException("mac is required");
        }
        String normalized = mac.trim();
        Optional<WeldingMachine> opt = weldingMachineRepository.findByMac(normalized);
        if (!opt.isPresent()) {
            opt = weldingMachineRepository.findByMac(normalized.toUpperCase());
        }
        if (!opt.isPresent()) {
            opt = weldingMachineRepository.findByMac(normalized.toLowerCase());
        }
        return opt.orElseThrow(() -> new IllegalArgumentException("Welding machine not found for mac: " + mac));
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(timezoneId));
    }

    private WeldingMachineDailyStatsDTO toDto(WeldingMachine machine, WeldingMachineDailyStats row) {
        WeldingMachineDailyStatsDTO dto = new WeldingMachineDailyStatsDTO();
        dto.setWeldingMachineId(machine.getId());
        dto.setMac(machine.getMac());
        dto.setStatDate(row.getStatDate());
        dto.setWireConsumptionKg(row.getWireConsumptionKg() != null ? row.getWireConsumptionKg() : BigDecimal.ZERO);
        dto.setGasConsumptionL(row.getGasConsumptionL() != null ? row.getGasConsumptionL() : BigDecimal.ZERO);
        dto.setGasBaselineAtDayStartL(row.getGasBaselineAtDayStartL());
        dto.setOffMs(row.getOffMs() != null ? row.getOffMs() : 0L);
        long errorMs = row.getStandbyMs() != null ? row.getStandbyMs() : 0L;
        dto.setErrorMs(errorMs);
        dto.setStandbyMs(0L);
        dto.setOnMs(row.getOnMs() != null ? row.getOnMs() : 0L);
        dto.setWeldingMs(row.getWeldingMs() != null ? row.getWeldingMs() : 0L);
        if (row.getComputedAt() != null) {
            dto.setComputedAtEpochMs(row.getComputedAt().atZone(ZoneId.of(timezoneId)).toInstant().toEpochMilli());
        }
        return dto;
    }
}
