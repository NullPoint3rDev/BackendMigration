package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO для шаблона отчета "По работе сварщика"
 * Сейчас фиксируем только те параметры, которые уже известны из требований.
 * Остальные будут добавлены позже по ТЗ.
 */
@Data
@NoArgsConstructor
public class WelderWorkReportTemplateDTO {
    /**
     * ID шаблона (если редактируем существующий)
     */
    private Long templateId;

    /**
     * Название шаблона
     */
    private String templateName;

    /**
     * Сварщик, по которому строится отчёт
     */
    private Long welderId;

    /**
     * Выбранные ID сварщиков (для совместимости с общим шаблоном)
     */
    private List<Integer> selectedWelderIds;

    /**
     * Данные сварщика для вывода в шапке отчёта (не обязаны храниться в БД).
     * Заполняются при генерации/загрузке шаблона.
     */
    private String welderFullName;
    private String welderTabNumber;
    private String welderProfession;
    private String welderDepartment;

    /**
     * Флаг: включать блок "Разрешенный диапазон фактического тока, А"
     */
    private Boolean includeActualCurrentRange = false;

    /**
     * Разрешенный диапазон фактического тока (А)
     */
    private Integer actualCurrentMin;
    private Integer actualCurrentMax;

    /**
     * Минимальный интервал между швами, с
     */
    private Integer minIntervalBetweenWeldsSec;

    /**
     * Минимальный учитываемый шов, с
     */
    private Integer minWeldDurationSec;

    /**
     * Выбранные колонки для вывода в отчёте (опциональные).
     * Ключи: equipmentModel, equipmentName, wireFeedSpeed, consumption, energyConsumed, gasConsumption.
     */
    private List<String> selectedColumns;
}


