package org.alloy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class WeldingMachineDailyStatsScheduler {

    @Value("${monitor.daily-stats.timezone:Europe/Moscow}")
    private String timezoneId;

    @Autowired
    private WeldingMachineDailyStatsService dailyStatsService;

    /** Догоняет суточную статистику по аппаратам с телеметрией за последние 2 часа. */
    @Scheduled(fixedDelayString = "${monitor.daily-stats.scheduler-delay-ms:180000}")
    public void refreshRecentDailyStats() {
        ZoneId zone = ZoneId.of(timezoneId);
        LocalDateTime since = LocalDateTime.now(zone).minusHours(2);
        dailyStatsService.recomputeStaleMachinesSince(since);
    }
}
