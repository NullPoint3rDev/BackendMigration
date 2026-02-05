package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO для одной строки отчёта "По работе сварщика" (швы).
 * Обязательные колонки: № п/п, Дата, Время начала шва, Режим работы оборудования,
 * Рабочий ток А, Рабочее напряжение В, Время шва с.
 * Остальные — опциональные (включаются по настройкам шаблона).
 */
@Data
@NoArgsConstructor
public class WelderWorkReportDTO {

    /** Порядковый номер (№ п/п) */
    private Integer index;

    /** Дата (колонка "Дата") */
    private LocalDate date;

    /** Время начала шва */
    private LocalTime weldStartTime;

    /** Модель оборудования (опционально) */
    private String equipmentModel;

    /** Наименование оборудования (опционально) */
    private String equipmentName;

    /** Режим работы оборудования */
    private String workMode;

    /** Рабочий ток, А */
    private BigDecimal currentAmps;

    /** Рабочее напряжение, В */
    private BigDecimal voltageVolts;

    /** Скорость подачи проволоки, м/мин (опционально) */
    private BigDecimal wireFeedSpeedMpm;

    /** Время шва, с (длительность сварки) */
    private BigDecimal weldDurationSec;

    /** Расход проволоки, кг (опционально) */
    private BigDecimal wireConsumptionKg;

    /** Затраченная энергия на шов, кВт*ч (опционально) */
    private BigDecimal energyConsumedKwh;

    /** Расход газа, л (опционально) */
    private BigDecimal gasConsumptionL;

    /**
     * Флаг: ток по шву вне разрешённого диапазона (для подсветки строки в отчёте).
     */
    private Boolean currentOutOfRange;

    /** Идентификатор сварщика */
    private Long welderId;

    /** Идентификатор аппарата */
    private Integer weldingMachineId;

    /** Наименование аппарата (дублируем для удобства) */
    private String weldingMachineName;
}
