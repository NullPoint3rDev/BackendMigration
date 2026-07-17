package org.alloy.models.dto;

import lombok.Data;
import org.alloy.models.MacRegistryStatus;

import java.time.LocalDateTime;

@Data
public class MacAddressRegistryDTO {
    private Integer id;
    private String mac;
    private Integer equipmentTypeId;
    private String equipmentTypeName;
    private MacRegistryStatus status;
    private String statusLabel;
    private LocalDateTime dateCreated;
    /** Дата/время занесения для UI (МСК), формат DD.MM.YY/HH:mm */
    private String dateCreatedDisplay;
    private String enteredByName;
    private Long sessionCount;
    private Integer weldingMachineId;
    private String weldingMachineName;
    private String organizationUnitName;
}
