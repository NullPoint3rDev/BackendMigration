package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO для генерации отчета "По работе сварщика"
 */
@Data
@NoArgsConstructor
public class WelderWorkReportGenerationDTO {
    /**
     * ID шаблона отчета
     */
    private Long templateId;

    /**
     * Период формирования отчета - дата начала
     */
    private LocalDate periodStartDate;

    /**
     * Период формирования отчета - дата окончания
     */
    private LocalDate periodEndDate;

    /**
     * Время начала периода
     */
    private LocalTime periodStartTime;

    /**
     * Время окончания периода
     */
    private LocalTime periodEndTime;

    /**
     * Выбранные колонки отчёта (ключи: equipmentModel, equipmentName, wireFeedSpeed, consumption, energyConsumed, gasConsumption).
     * Если переданы при генерации — подменяют список из шаблона (актуальное состояние галочек на форме).
     */
    private java.util.List<String> selectedColumns;
}


