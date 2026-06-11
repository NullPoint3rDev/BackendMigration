package org.alloy.models.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WeldingMachineDailyStatsDTO {
    private Integer weldingMachineId;
    private String mac;
    private LocalDate statDate;
    private BigDecimal wireConsumptionKg;
    /** Суммарный расход газа за сутки, л. */
    private BigDecimal gasConsumptionL;
    /** Счётчик «с включения» на начало суток (л), для расчёта на клиенте между пересчётами. */
    private BigDecimal gasBaselineAtDayStartL;
    private long offMs;
    /** @deprecated используйте {@link #errorMs}; в БД — колонка {@code standby_ms}. */
    private long standbyMs;
    /** Суммарное время в состоянии ошибки за сутки (мс). */
    private long errorMs;
    private long onMs;
    private long weldingMs;
    private Long computedAtEpochMs;
}
