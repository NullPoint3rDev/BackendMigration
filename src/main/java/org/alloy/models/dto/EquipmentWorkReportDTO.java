package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO для одной строки отчёта "По работе оборудования" (швы).
 * Обязательные колонки: № п/п, Дата, Время начала шва, Режим работы оборудования,
 * Рабочий ток А, Рабочее напряжение В, Время шва с.
 * Остальные — опциональные (включаются по настройкам шаблона).
 */
@Data
@NoArgsConstructor
public class EquipmentWorkReportDTO {

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

    /** Установленный ток, А (опционально; с последнего холостого хода перед швом) */
    private BigDecimal setCurrentAmps;

    /** Установленное напряжение, В (опционально; с последнего холостого хода перед швом) */
    private BigDecimal setVoltageVolts;

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

    /** Флаг: ток по шву вне разрешённого диапазона */
    private Boolean currentOutOfRange;

    /** Идентификатор аппарата */
    private Integer weldingMachineId;

    /** Наименование аппарата */
    private String weldingMachineName;

    /** ФИО сварщика (опциональная колонка) */
    private String welderFullName;

    /** Табельный номер сварщика (опциональная колонка) */
    private String welderTabNumber;

    /** Профессия сварщика (опциональная колонка) */
    private String welderProfession;

    /** Идентификатор сварщика (для группировки) */
    private Long welderId;
}
