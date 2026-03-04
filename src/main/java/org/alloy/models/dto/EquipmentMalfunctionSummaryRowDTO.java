package org.alloy.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Одна строка таблицы "Суммарно за период" отчёта по неисправностям оборудования.
 * Неисправность, Кол-во, Продолжительность.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentMalfunctionSummaryRowDTO {

    /** Наименование неисправности (errorCode из WeldingMachineState) */
    private String malfunctionName;

    /** Количество непрерывных отрезков нахождения в ошибке (разрыв ≤1 сек не считается) */
    private int count;

    /** Суммарная продолжительность, секунды */
    private long durationSeconds;
}
