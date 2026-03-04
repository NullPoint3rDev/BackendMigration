package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO запроса на генерацию отчёта "По неисправностям оборудования".
 */
@Data
@NoArgsConstructor
public class EquipmentMalfunctionReportGenerationDTO {

    private Long templateId;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private LocalTime periodStartTime;
    private LocalTime periodEndTime;

    /** Выбранные ID аппаратов (при передаче с формы) */
    private List<Integer> selectedEquipmentIds;

    /** Тип периода из UI: «За 24 часа», «За 7 дней», «Произвольный период» — при «За 24 часа» период считается на сервере (сейчас − 24 ч … сейчас). */
    private String periodType;
}
