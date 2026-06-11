package org.alloy.services;

import org.alloy.services.report.ReportGenerationProgressContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly — «сегодня + вчера» для активных аппаратов.
 * Nightly — догон закрытых суток (backfill).
 */
@Component
public class WeldingMachineWeldSegmentScheduler {

    @Value("${monitor.weld-segments.activity-lookback-hours:2}")
    private int activityLookbackHours;

    @Value("${monitor.weld-segments.backfill-from-days-ago:8}")
    private int backfillFromDaysAgo;

    @Value("${monitor.weld-segments.backfill-to-days-ago:2}")
    private int backfillToDaysAgo;

    @Autowired
    private WeldingMachineWeldSegmentCacheService weldSegmentCacheService;

    @Scheduled(fixedDelayString = "${monitor.weld-segments.scheduler-delay-ms:3600000}")
    public void refreshRecentWeldSegments() {
        if (ReportGenerationProgressContext.isReportGenerationActive()) {
            return;
        }
        weldSegmentCacheService.recomputeActiveMachinesRecentWindow(activityLookbackHours);
    }

    /** 03:15 — пересчёт закрытых дней, которых ещё нет в day_mark. */
    @Scheduled(cron = "${monitor.weld-segments.backfill-cron:0 15 3 * * *}")
    public void backfillClosedWeldSegmentDays() {
        if (ReportGenerationProgressContext.isReportGenerationActive()) {
            return;
        }
        weldSegmentCacheService.backfillClosedDays(backfillFromDaysAgo, backfillToDaysAgo);
    }
}
