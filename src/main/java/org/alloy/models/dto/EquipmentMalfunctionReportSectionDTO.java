package org.alloy.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Один блок отчёта по неисправностям оборудования: заголовок (модель, наименование, подразделение, серийный №, инв. №)
 * и две таблицы: суммарно за период и за период по датам.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentMalfunctionReportSectionDTO {

    private Integer weldingMachineId;
    private String equipmentModel;
    private String equipmentName;
    private String equipmentDepartment;
    private String serialNumber;
    private String inventoryNumber;

    /** Суммарно за период: неисправность, кол-во, продолжительность */
    private List<EquipmentMalfunctionSummaryRowDTO> summaryRows;

    /** За период по датам: неисправность, дата, количество, продолжительность */
    private List<EquipmentMalfunctionByDateRowDTO> byDateRows;
}
