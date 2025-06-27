package org.alloy.models.dto.mapper;

import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.dto.WeldingMachineTypeDTO;

public class WeldingMachineTypeMapper {
    public static WeldingMachineTypeDTO toDTO(WeldingMachineType type) {
        if (type == null) return null;
        WeldingMachineTypeDTO dto = new WeldingMachineTypeDTO();
        dto.setId(type.getId());
        dto.setName(type.getName());
        dto.setDescription(type.getDescription());
        // ... другие нужные поля
        return dto;
    }
} 