package org.alloy.models.dto.mapper;

import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.models.dto.WeldingMachineParameterValueDTO;

public class WeldingMachineParameterValueMapper {
    public static WeldingMachineParameterValueDTO toDTO(WeldingMachineParameterValue entity) {
        if (entity == null) return null;
        WeldingMachineParameterValueDTO dto = new WeldingMachineParameterValueDTO();
        dto.setId(entity.getId() != null ? entity.getId().intValue() : null);
        dto.setParameterName(entity.getPropertyCode());
        dto.setValue(entity.getValue());
        // ... добавить другие поля по необходимости
        return dto;
    }

    public static WeldingMachineParameterValue toEntity(WeldingMachineParameterValueDTO dto) {
        if (dto == null) return null;
        WeldingMachineParameterValue entity = new WeldingMachineParameterValue();
        entity.setId(dto.getId() != null ? dto.getId().longValue() : null);
        entity.setPropertyCode(dto.getParameterName());
        entity.setValue(dto.getValue());
        // ... добавить другие поля по необходимости
        return entity;
    }
} 