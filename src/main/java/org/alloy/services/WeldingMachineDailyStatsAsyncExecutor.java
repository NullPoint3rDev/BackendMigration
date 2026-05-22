package org.alloy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Тяжёлый пересчёт суточной статистики вне HTTP-потока (отдельная транзакция, своё соединение).
 */
@Component
public class WeldingMachineDailyStatsAsyncExecutor {

    @Autowired
    @Lazy
    private WeldingMachineDailyStatsService dailyStatsService;

    @Async
    public void recomputeDayAsync(Integer weldingMachineId, LocalDate statDate) {
        if (weldingMachineId == null || statDate == null) {
            return;
        }
        try {
            dailyStatsService.recomputeDay(weldingMachineId, statDate);
        } catch (Exception e) {
            System.err.println("[DAILY-STATS] async recompute machineId=" + weldingMachineId
                    + " date=" + statDate + ": " + e.getMessage());
        }
    }
}
