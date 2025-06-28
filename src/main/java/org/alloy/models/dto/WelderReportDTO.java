package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class WelderReportDTO {
    private Integer welderId;
    private String welderName;
    private String welderEmail;
    private LocalDate date;
    private BigDecimal totalWireConsumption; // в кг
    private BigDecimal totalWeldingTime; // в минутах
    private Integer totalWeldingSessions;
    private BigDecimal averageCurrent; // в А
    private BigDecimal averageVoltage; // в В
    private BigDecimal averageWireFeedRate; // в м/мин
    private String organizationUnitName;
    private String weldingMachineName;
} 