package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO для генерации отчета "По работе оборудования"
 */
@Data
@NoArgsConstructor
public class EquipmentWorkReportGenerationDTO {

    private Long templateId;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private LocalTime periodStartTime;
    private LocalTime periodEndTime;
    private List<String> selectedColumns;

    /** Выбранные ID аппаратов (если передаются в запросе при использовании общего шаблона) */
    private List<Integer> selectedEquipmentIds;

    /** Минимальный интервал между швами, с (0–10). Передаётся с формы при генерации. */
    private Integer minSeamInterval;
    /** Минимальный учитываемый шов, с (0–10). Передаётся с формы при генерации. */
    private Integer minSeamDuration;
    /** Галочка «Мин. интервал между швами» включена. Если false — в отчёт не попадает. */
    private Boolean minSeamIntervalEnabled;
    /** Галочка «Мин. учитываемый шов» включена. Если false — в отчёт не попадает. */
    private Boolean minSeamDurationEnabled;
}
