package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO для отчета "По работе сварщика"
 * Пока содержит минимальный набор полей (строки таблицы) — будет расширяться по ТЗ.
 */
@Data
@NoArgsConstructor
public class WelderWorkReportDTO {
    /**
     * Порядковый номер (№ п/п)
     */
    private Integer index;

    /**
     * Дата (колонка "Дата")
     */
    private LocalDate date;

    /**
     * Идентификатор сварщика, к которому относится строка
     */
    private Long welderId;

    /**
     * Идентификатор аппарата (если нужно группировать по аппаратам внутри сварщика)
     */
    private Integer weldingMachineId;
    private String weldingMachineName;
}


