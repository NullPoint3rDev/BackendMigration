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
    private long standbyMs;
    private long onMs;
    private long weldingMs;
    private Long computedAtEpochMs;
}
