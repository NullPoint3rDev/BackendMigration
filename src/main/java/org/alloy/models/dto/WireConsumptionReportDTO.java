package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * DTO для отчета по расходу проволоки
 * Соответствует новому формату отчета согласно требованиям
 */
@Data
@NoArgsConstructor
public class WireConsumptionReportDTO {
    /**
     * Сварщик
     */
    private Integer welderId;
    private String welderName;

    /**
     * Табельный номер
     */
    private String tabNumber;

    /**
     * Профессия
     */
    private String profession;

    /**
     * Подразделение
     */
    private Integer organizationUnitId;
    private String organizationUnitName;

    /**
     * Наименование оборудования (ИП)
     */
    private Integer weldingMachineId;
    private String weldingMachineName;

    /**
     * Модель оборудования
     */
    private String equipmentModel;

    /**
     * Время в сети (общее время работы оборудования)
     */
    private Duration timeInNetwork;

    /**
     * Время горения дуги
     */
    private Duration arcBurningTime;

    /**
     * Эффективность использования оборудования (%)
     */
    private BigDecimal equipmentEfficiency;

    /**
     * Время работы вне диапазона устанавливаемого тока
     */
    private Duration timeOutsideSetCurrentRange;

    /**
     * Время работы вне диапазона фактического тока
     */
    private Duration timeOutsideActualCurrentRange;

    /**
     * Затраченная энергия (кВт*ч)
     */
    private BigDecimal energyConsumed;

    /**
     * Проволока (тип и диаметр)
     */
    private String wire;

    /**
     * Расход проволоки (кг)
     */
    private BigDecimal wireConsumption;

    /**
     * Флаг для группировки: если true, это суммарная строка для сварщика
     */
    private Boolean isSummaryRow = false;

    /**
     * Порядковый номер для сортировки
     */
    private Integer sortOrder;

}