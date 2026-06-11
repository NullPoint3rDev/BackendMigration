package org.alloy.services;

import org.alloy.services.report.ReportGenerationProgressContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class WeldingMachineWeldSegmentAsyncExecutor {

    @Autowired
    @Lazy
    private WeldingMachineWeldSegmentCacheService weldSegmentCacheService;

    @Value("${monitor.weld-segments.timezone:Europe/Moscow}")
    private String timezoneId;

    @Async
    public void recomputeRecentWindowAsync(Integer weldingMachineId) {
        if (weldingMachineId == null || ReportGenerationProgressContext.isReportGenerationActive()) {
            return;
        }
        try {
            ZoneId zone = ZoneId.of(timezoneId);
            ZonedDateTime nowZ = ZonedDateTime.now(zone);
            LocalDateTime windowStart = nowZ.toLocalDate().minusDays(1).atStartOfDay();
            LocalDateTime windowEnd = nowZ.toLocalDateTime();
            weldSegmentCacheService.recomputePeriod(weldingMachineId, windowStart, windowEnd);
        } catch (Exception e) {
            System.err.println("[WELD-CACHE] async recent machineId=" + weldingMachineId + ": " + e.getMessage());
        }
    }

    @Async
    public void recomputeCalendarDayAsync(Integer weldingMachineId, LocalDate statDate) {
        if (weldingMachineId == null || statDate == null
                || ReportGenerationProgressContext.isReportGenerationActive()) {
            return;
        }
        try {
            ZoneId zone = ZoneId.of(timezoneId);
            LocalDateTime dayStart = statDate.atStartOfDay();
            LocalDateTime dayEnd = statDate.plusDays(1).atStartOfDay();
            ZonedDateTime nowZ = ZonedDateTime.now(zone);
            if (statDate.equals(nowZ.toLocalDate())) {
                dayEnd = nowZ.toLocalDateTime().isBefore(dayEnd) ? nowZ.toLocalDateTime() : dayEnd;
            }
            weldSegmentCacheService.recomputePeriod(weldingMachineId, dayStart, dayEnd);
        } catch (Exception e) {
            System.err.println("[WELD-CACHE] async day machineId=" + weldingMachineId
                    + " date=" + statDate + ": " + e.getMessage());
        }
    }

    @Async
    public void recomputePeriodAsync(Integer weldingMachineId, LocalDateTime start, LocalDateTime end) {
        if (ReportGenerationProgressContext.isReportGenerationActive()) {
            return;
        }
        try {
            weldSegmentCacheService.recomputePeriod(weldingMachineId, start, end);
        } catch (Exception e) {
            System.err.println("[WELD-CACHE] async period machineId=" + weldingMachineId + ": " + e.getMessage());
        }
    }
}
