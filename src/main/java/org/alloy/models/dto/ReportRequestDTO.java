package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class ReportRequestDTO {
    private String reportType; // WIRE_CONSUMPTION, WELDER_REPORT, WORK_REPORT
    private String format; // EXCEL, PDF
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String period; // DAY, MONTH, YEAR
    private Integer weldingMachineId;
    private Integer welderId;
    private List<Integer> weldingMachineIds;
    private List<Integer> welderIds;
    private String sortBy; // MACHINE, WELDER, DATE
    private boolean includeDetails;
    private List<String> selectedColumns; // Выбранные столбцы для отчета
} 