package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class WorkReportDTO {
    private Integer weldingMachineId;
    private String weldingMachineName;
    private String weldingMachineSerialNumber;
    private Integer welderId;
    private String welderName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal weldingTime; // в минутах
    private BigDecimal current; // в А
    private BigDecimal voltage; // в В
    private String weldingMode;
    private String weldingType;
    private BigDecimal wireConsumption; // в кг
    private BigDecimal wireFeedRate; // в м/мин
    private String organizationUnitName;
    private String notes;
} 