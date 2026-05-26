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
    private long offMs;
    /** @deprecated используйте {@link #errorMs}; в БД — колонка {@code standby_ms}. */
    private long standbyMs;
    /** Суммарное время в состоянии ошибки за сутки (мс). */
    private long errorMs;
    private long onMs;
    private long weldingMs;
    private Long computedAtEpochMs;
}
