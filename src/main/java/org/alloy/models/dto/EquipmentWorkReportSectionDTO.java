package org.alloy.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Один блок отчёта по работе оборудования: заголовок (модель, наименование, подразделение) + строки таблицы.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentWorkReportSectionDTO {

    private Integer weldingMachineId;
    private String equipmentModel;
    private String equipmentName;
    private String equipmentDepartment;
    private List<EquipmentWorkReportDTO> rows;
}
