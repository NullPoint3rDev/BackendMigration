package org.alloy.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Одна строка таблицы "За период по датам" отчёта по неисправностям оборудования.
 * Неисправность, Дата, Количество, Продолжительность.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentMalfunctionByDateRowDTO {

    private String malfunctionName;
    private LocalDate date;
    private String time;
    private int count;
    private long durationSeconds;
}
