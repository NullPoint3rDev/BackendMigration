package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO шаблона отчёта "По неисправностям оборудования".
 * Выбор оборудования — аналогично отчёту по работе оборудования (швы).
 * Колонки по умолчанию: модель, наименование, подразделение, серийный №, инв. №, неисправности.
 */
@Data
@NoArgsConstructor
public class EquipmentMalfunctionReportTemplateDTO {

    private Long templateId;
    private String templateName;

    /** Выбранные ID аппаратов (WeldingMachine.id) */
    private List<Integer> selectedEquipmentIds;

    /** Выбранные колонки для отображения в шапке (equipmentModel, equipmentName, equipmentDepartment, serialNumber, inventoryNumber, malfunctions) — по умолчанию все */
    private List<String> selectedColumns;
}
