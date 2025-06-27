package org.alloy.models.dto.mapper;

import org.alloy.models.entities.Maintenance;
import org.alloy.models.dto.MaintenanceDTO;
import org.alloy.models.dto.WeldingMachineShortDTO;
import org.alloy.models.dto.UserAccountShortDTO;

public class MaintenanceMapper {
    public static MaintenanceDTO toDTO(Maintenance maintenance) {
        if (maintenance == null) return null;
        MaintenanceDTO dto = new MaintenanceDTO();
        dto.setId(maintenance.getId());
        dto.setDescription(maintenance.getDescription());
        if (maintenance.getWeldingMachine() != null) {
            WeldingMachineShortDTO wmDto = new WeldingMachineShortDTO();
            wmDto.setId(maintenance.getWeldingMachine().getId());
            wmDto.setName(maintenance.getWeldingMachine().getName());
            dto.setWeldingMachine(wmDto);
        }
        if (maintenance.getUserAccount() != null) {
            UserAccountShortDTO userDto = new UserAccountShortDTO();
            userDto.setId(maintenance.getUserAccount().getId());
            userDto.setUsername(maintenance.getUserAccount().getUserName());
            dto.setUserAccount(userDto);
        }
        // ... другие нужные поля
        return dto;
    }

    public static Maintenance toEntity(MaintenanceDTO dto) {
        if (dto == null) return null;
        Maintenance entity = new Maintenance();
        entity.setId(dto.getId());
        entity.setDescription(dto.getDescription());
        if (dto.getWeldingMachine() != null) {
            entity.setWeldingMachineId(dto.getWeldingMachine().getId());
        }
        if (dto.getUserAccount() != null) {
            entity.setUserAccountId(dto.getUserAccount().getId());
        }
        // ... другие нужные поля
        return entity;
    }
} 