package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для шаблона отчета "По работе оборудования"
 */
@Data
@NoArgsConstructor
public class EquipmentWorkReportTemplateDTO {

    private Long templateId;
    private String templateName;

    /** Выбранные ID аппаратов (WeldingMachine.id) */
    private List<Integer> selectedEquipmentIds;

    /** Данные оборудования для шапки отчёта (заполняются при генерации) */
    private String equipmentModel;
    private String equipmentName;
    private String equipmentDepartment;

    private Boolean includeActualCurrentRange = false;
    private Integer actualCurrentMin;
    private Integer actualCurrentMax;
    private Integer minIntervalBetweenWeldsSec;
    private Integer minWeldDurationSec;

    /** Выбранные колонки: welderFullName, welderTabNumber, profession, equipmentModel, equipmentName, wireFeedSpeed, consumption, energyConsumed, gasConsumption */
    private List<String> selectedColumns;
}
