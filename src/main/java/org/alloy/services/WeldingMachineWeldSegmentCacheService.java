package org.alloy.services;

import org.alloy.models.dto.WeldSegmentDTO;
import org.alloy.models.entities.WeldingMachineWeldSegment;
import org.alloy.models.entities.WeldingMachineWeldSegmentDayMark;
import org.alloy.repositories.WeldingMachineDailyStatsRepository;
import org.alloy.repositories.WeldingMachineWeldSegmentDayMarkRepository;
import org.alloy.repositories.WeldingMachineWeldSegmentRepository;
import org.alloy.services.report.ReportGenerationProgressContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Материализованные сегменты швов для отчётов (вариант B): пересчёт в фоне, без изменений ingestion.
 */
@Service
public class WeldingMachineWeldSegmentCacheService {

    @Value("${monitor.weld-segments.enabled:true}")
    private boolean enabled;

    @Value("${monitor.weld-segments.report-use-precomputed:true}")
    private boolean reportUsePrecomputed;

    @Value("${monitor.weld-segments.timezone:Europe/Moscow}")
    private String timezoneId;

    @Value("${monitor.weld-segments.recompute-debounce-ms:300000}")
    private long recomputeDebounceMs;

    @Value("${monitor.weld-segments.today-stale-seconds:3900}")
    private long todayStaleSeconds;

    @Autowired
    private WeldingMachineWeldSegmentRepository segmentRepository;

    @Autowired
    private WeldingMachineWeldSegmentDayMarkRepository dayMarkRepository;

    @Autowired
    private WeldingMachineDailyStatsRepository dailyStatsRepository;

    @Autowired
    private WeldingReportCalculationService calculationService;

    @Autowired
    private WeldingMachineWeldSegmentAsyncExecutor asyncExecutor;

    private final ConcurrentHashMap<String, Long> lastRecomputeScheduledMs = new ConcurrentHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isReportUsePrecomputed() {
        return enabled && reportUsePrecomputed;
    }

    /**
     * Сегменты из кэша для отчёта, если все сутки периода помечены как пересчитанные.
     * Иначе — empty → вызывающий код делает live-расчёт.
     */
    public Optional<List<WeldSegmentDTO>> findSegmentsForReportIfReady(
            Integer machineId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        if (!isReportUsePrecomputed() || machineId == null || periodStart == null || periodEnd == null) {
            return Optional.empty();
        }
        if (!hasDayMarkCoverage(machineId, periodStart, periodEnd)) {
            scheduleRecomputePeriod(machineId, periodStart, periodEnd);
            return Optional.empty();
        }
        List<WeldingMachineWeldSegment> rows = segmentRepository
                .findByWeldingMachineIdInAndStartTimeGreaterThanEqualAndStartTimeLessThanEqualOrderByStartTimeAsc(
                        List.of(machineId), periodStart, periodEnd);
        return Optional.of(toDtos(rows));
    }

    public void scheduleRecomputePeriod(Integer machineId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        if (!enabled || machineId == null || periodStart == null || periodEnd == null
                || ReportGenerationProgressContext.isReportGenerationActive()) {
            return;
        }
        String key = machineId + "|" + periodStart.toLocalDate() + "|" + periodEnd.toLocalDate();
        long now = System.currentTimeMillis();
        Long prev = lastRecomputeScheduledMs.get(key);
        if (prev != null && now - prev < recomputeDebounceMs) {
            return;
        }
        lastRecomputeScheduledMs.put(key, now);
        asyncExecutor.recomputePeriodAsync(machineId, periodStart, periodEnd);
    }

    /** Скользящее окно «вчера 00:00 — сейчас» для hourly job. */
    public void recomputeRecentWindow(Integer weldingMachineId) {
        if (!enabled || weldingMachineId == null) {
            return;
        }
        asyncExecutor.recomputeRecentWindowAsync(weldingMachineId);
    }

    /** Пересчёт одних календарных суток (для nightly backfill). */
    public void recomputeCalendarDay(Integer weldingMachineId, LocalDate statDate) {
        if (!enabled || weldingMachineId == null || statDate == null) {
            return;
        }
        asyncExecutor.recomputeCalendarDayAsync(weldingMachineId, statDate);
    }

    /**
     * Удаляет сегменты в окне, пересчитывает через {@link WeldingReportCalculationService}, сохраняет.
     * Отдельная транзакция — короткое удержание connection.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recomputePeriod(Integer weldingMachineId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        if (!enabled || weldingMachineId == null || periodStart == null || periodEnd == null
                || periodStart.isAfter(periodEnd)
                || ReportGenerationProgressContext.isReportGenerationActive()) {
            return;
        }
        long t0 = System.currentTimeMillis();
        segmentRepository.deleteByMachineAndStartTimeRange(weldingMachineId, periodStart, periodEnd);

        List<org.alloy.models.dto.WeldSegmentDTO> segments =
                calculationService.calculateWeldSegments(weldingMachineId, periodStart, periodEnd);

        ZoneId zone = ZoneId.of(timezoneId);
        LocalDateTime computedAt = LocalDateTime.now(zone);
        List<WeldingMachineWeldSegment> toSave = new ArrayList<>();

        for (WeldSegmentDTO dto : segments) {
            if (dto.getStartTime() == null || dto.getDurationSeconds() == null || dto.getStartStateId() == null) {
                continue;
            }
            if (dto.getStartTime().isBefore(periodStart) || dto.getStartTime().isAfter(periodEnd)) {
                continue;
            }
            WeldingMachineWeldSegment row = new WeldingMachineWeldSegment();
            row.setWeldingMachineId(weldingMachineId);
            row.setStatDate(dto.getStartTime().atZone(zone).toLocalDate());
            row.setStartTime(dto.getStartTime());
            long durSec = dto.getDurationSeconds().longValue();
            row.setEndTime(dto.getStartTime().plusSeconds(durSec));
            row.setDurationSeconds(dto.getDurationSeconds());
            row.setAvgCurrent(dto.getAverageCurrent() != null ? dto.getAverageCurrent() : BigDecimal.ZERO);
            row.setAvgVoltage(dto.getAverageVoltage() != null ? dto.getAverageVoltage() : BigDecimal.ZERO);
            row.setStartStateId(dto.getStartStateId());
            row.setEndStateId(dto.getEndStateId());
            row.setComputedAt(computedAt);
            toSave.add(row);
        }
        if (!toSave.isEmpty()) {
            segmentRepository.saveAll(toSave);
        }

        LocalDate markFrom = periodStart.atZone(zone).toLocalDate();
        LocalDate markTo = periodEnd.atZone(zone).toLocalDate();
        for (LocalDate day = markFrom; !day.isAfter(markTo); day = day.plusDays(1)) {
            final LocalDate markDay = day;
            long count = toSave.stream().filter(s -> markDay.equals(s.getStatDate())).count();
            upsertDayMark(weldingMachineId, markDay, (int) count, computedAt);
        }

        System.out.println("[WELD-CACHE] recompute machineId=" + weldingMachineId
                + " period=" + periodStart + ".." + periodEnd
                + " segments=" + toSave.size()
                + " ms=" + (System.currentTimeMillis() - t0));
    }

    /** Hourly: аппараты с телеметрией за последние N часов. */
    public void recomputeActiveMachinesRecentWindow(int activityLookbackHours) {
        if (!enabled) {
            return;
        }
        ZoneId zone = ZoneId.of(timezoneId);
        LocalDateTime since = LocalDateTime.now(zone).minusHours(Math.max(1, activityLookbackHours));
        List<Integer> machineIds = dailyStatsRepository.findMachineIdsWithStatesSince(since);
        for (Integer machineId : machineIds) {
            asyncExecutor.recomputeRecentWindowAsync(machineId);
        }
        System.out.println("[WELD-CACHE] hourly scheduled machines=" + machineIds.size());
    }

    /** Nightly: закрытые сутки без свежей отметки. */
    public void backfillClosedDays(int fromDaysAgo, int toDaysAgo) {
        if (!enabled) {
            return;
        }
        ZoneId zone = ZoneId.of(timezoneId);
        LocalDate today = ZonedDateTime.now(zone).toLocalDate();
        for (int d = fromDaysAgo; d >= toDaysAgo; d--) {
            LocalDate statDate = today.minusDays(d);
            if (!statDate.isBefore(today)) {
                continue;
            }
            LocalDateTime dayStart = statDate.atStartOfDay();
            LocalDateTime dayEnd = statDate.plusDays(1).atStartOfDay();
            List<Integer> machineIds = dayMarkRepository.findMachineIdsWithStatesOnDay(dayStart, dayEnd);
            for (Integer machineId : machineIds) {
                if (dayMarkRepository.findByWeldingMachineIdAndStatDate(machineId, statDate).isPresent()) {
                    continue;
                }
                asyncExecutor.recomputeCalendarDayAsync(machineId, statDate);
            }
        }
    }

    private boolean hasDayMarkCoverage(Integer machineId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        ZoneId zone = ZoneId.of(timezoneId);
        LocalDate from = periodStart.atZone(zone).toLocalDate();
        LocalDate to = periodEnd.atZone(zone).toLocalDate();
        List<WeldingMachineWeldSegmentDayMark> marks = dayMarkRepository
                .findByMachineAndStatDateBetween(machineId, from, to);
        Set<LocalDate> marked = marks.stream()
                .filter(m -> isMarkFresh(m, periodEnd))
                .map(WeldingMachineWeldSegmentDayMark::getStatDate)
                .collect(Collectors.toSet());

        LocalDate today = ZonedDateTime.now(zone).toLocalDate();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (!marked.contains(day)) {
                return false;
            }
            if (day.equals(today)) {
                WeldingMachineWeldSegmentDayMark todayMark = marks.stream()
                        .filter(m -> today.equals(m.getStatDate()))
                        .findFirst()
                        .orElse(null);
                if (todayMark == null || todayMark.getComputedAt() == null) {
                    return false;
                }
                long ageSec = java.time.Duration.between(
                        todayMark.getComputedAt(), LocalDateTime.now(zone)).getSeconds();
                if (ageSec > todayStaleSeconds) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isMarkFresh(WeldingMachineWeldSegmentDayMark mark, LocalDateTime periodEnd) {
        if (mark == null || mark.getComputedAt() == null) {
            return false;
        }
        ZoneId zone = ZoneId.of(timezoneId);
        LocalDate today = ZonedDateTime.now(zone).toLocalDate();
        if (!mark.getStatDate().isBefore(today)) {
            long ageSec = java.time.Duration.between(mark.getComputedAt(), LocalDateTime.now(zone)).getSeconds();
            return ageSec <= todayStaleSeconds;
        }
        return true;
    }

    private void upsertDayMark(Integer machineId, LocalDate statDate, int segmentCount, LocalDateTime computedAt) {
        WeldingMachineWeldSegmentDayMark mark = dayMarkRepository
                .findByWeldingMachineIdAndStatDate(machineId, statDate)
                .orElseGet(WeldingMachineWeldSegmentDayMark::new);
        mark.setWeldingMachineId(machineId);
        mark.setStatDate(statDate);
        mark.setSegmentCount(segmentCount);
        mark.setComputedAt(computedAt);
        dayMarkRepository.save(mark);
    }

    private static List<WeldSegmentDTO> toDtos(List<WeldingMachineWeldSegment> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<WeldSegmentDTO> out = new ArrayList<>(rows.size());
        for (WeldingMachineWeldSegment row : rows) {
            WeldSegmentDTO dto = new WeldSegmentDTO();
            dto.setStartTime(row.getStartTime());
            dto.setDurationSeconds(row.getDurationSeconds());
            dto.setAverageCurrent(row.getAvgCurrent());
            dto.setAverageVoltage(row.getAvgVoltage());
            dto.setStartStateId(row.getStartStateId());
            dto.setEndStateId(row.getEndStateId());
            out.add(dto);
        }
        return out;
    }
}
