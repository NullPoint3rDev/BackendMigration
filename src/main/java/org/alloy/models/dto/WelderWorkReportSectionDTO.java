package org.alloy.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Один блок отчёта по работе сварщика: заголовок (ФИО, подразделение, таб. №) + строки таблицы.
 * Используется для формирования одного Excel с несколькими сварщиками подряд.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WelderWorkReportSectionDTO {
    private Long welderId;
    private String welderFullName;
    private String welderTabNumber;
    private String welderDepartment;
    private List<WelderWorkReportDTO> rows;
}
