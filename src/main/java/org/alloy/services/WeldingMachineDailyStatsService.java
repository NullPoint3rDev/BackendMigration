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

    private static final String WIRE_PARAM = "Расход проволоки";
    private static final List<String> STATE_TEXT_KEYS = List.of(
            "Состояние аппарата", "WeldingMachineState", "State.WeldingMachineState");
    private static final List<String> CURRENT_KEYS = List.of("State.I", "Ток", "Current", "current");

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

    /** machineId → время последней постановки пересчёта в очередь */
    private final ConcurrentHashMap<Integer, Long> lastRecomputeScheduledMs = new ConcurrentHashMap<>();

    /**
     * Быстрый ответ для UI: только чтение кэша. Пересчёт — асинхронно, если кэш устарел.
     */
    @Transactional(readOnly = true)
    public WeldingMachineDailyStatsDTO getDailyStatsByMac(String mac, LocalDate statDate) {
        WeldingMachine machine = resolveMachine(mac);
        LocalDate day = statDate != null ? statDate : today();
        WeldingMachineDailyStats row = dailyStatsRepository
                .findByWeldingMachineIdAndStatDate(machine.getId(), day)
                .orElseGet(() -> emptyStats(machine.getId(), day));
        if (shouldScheduleRecompute(row, day)) {
            scheduleRecompute(machine.getId(), day);
        }
        return toDto(machine, row);
    }

    public void scheduleRecompute(Integer weldingMachineId, LocalDate statDate) {
        if (weldingMachineId == null || statDate == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long prev = lastRecomputeScheduledMs.get(weldingMachineId);
        if (prev != null && now - prev < recomputeDebounceMs) {
            return;
        }
        lastRecomputeScheduledMs.put(weldingMachineId, now);
        asyncExecutor.recomputeDayAsync(weldingMachineId, statDate);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WeldingMachineDailyStats recomputeDay(Integer weldingMachineId, LocalDate statDate) {
        ZoneId zone = ZoneId.of(timezoneId);
        ZonedDateTime dayStartZ = statDate.atStartOfDay(zone);
        ZonedDateTime dayEndZ = statDate.plusDays(1).atStartOfDay(zone);
        LocalDateTime dayStart = dayStartZ.toLocalDateTime();
        LocalDateTime dayEnd = dayEndZ.toLocalDateTime();
        ZonedDateTime nowZ = ZonedDateTime.now(zone);
        LocalDateTime effectiveEnd = statDate.equals(nowZ.toLocalDate())
                ? (nowZ.toLocalDateTime().isBefore(dayEnd) ? nowZ.toLocalDateTime() : dayEnd)
                : dayEnd;

        List<WeldingMachineState> states = weldingMachineStateRepository.findByWeldingMachineIdAndDateRangeAsc(
                weldingMachineId, dayStart, dayEnd);
        states.sort(Comparator.comparing(WeldingMachineState::getDateCreated, Comparator.nullsLast(Comparator.naturalOrder())));

        long offMs = 0L;
        long standbyMs = 0L;
        long onMs = 0L;
        long weldingMs = 0L;

        Map<Long, Map<String, String>> propsByStateId = loadPropsByStateId(states);
        Map<Long, BigDecimal> wireFeedByStateId = loadWireFeedByStateId(states);

        LocalDateTime openEndIfLast = statDate.equals(nowZ.toLocalDate()) ? effectiveEnd : null;

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
                case standby:
                    standbyMs += overlapMs;
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

        Long lastStateId = states.isEmpty() ? null : states.get(states.size() - 1).getId();

        WeldingMachineDailyStats row = dailyStatsRepository
                .findByWeldingMachineIdAndStatDate(weldingMachineId, statDate)
                .orElseGet(WeldingMachineDailyStats::new);
        row.setWeldingMachineId(weldingMachineId);
        row.setStatDate(statDate);
        row.setWireConsumptionKg(wireKg);
        row.setOffMs(offMs);
        row.setStandbyMs(standbyMs);
        row.setOnMs(onMs);
        row.setWeldingMs(weldingMs);
        row.setLastStateId(lastStateId);
        row.setComputedAt(LocalDateTime.now(zone));
        return dailyStatsRepository.save(row);
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
        dto.setOffMs(row.getOffMs() != null ? row.getOffMs() : 0L);
        dto.setStandbyMs(row.getStandbyMs() != null ? row.getStandbyMs() : 0L);
        dto.setOnMs(row.getOnMs() != null ? row.getOnMs() : 0L);
        dto.setWeldingMs(row.getWeldingMs() != null ? row.getWeldingMs() : 0L);
        if (row.getComputedAt() != null) {
            dto.setComputedAtEpochMs(row.getComputedAt().atZone(ZoneId.of(timezoneId)).toInstant().toEpochMilli());
        }
        return dto;
    }
}
