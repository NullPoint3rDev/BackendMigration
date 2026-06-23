package org.alloy.models.dto.mapper;

import org.alloy.models.dto.WeldingMachineDTO;
import org.alloy.models.dto.OrganizationUnitShortDTO;
import org.alloy.models.dto.WeldingMachineTypeShortDTO;
import org.alloy.models.entities.WeldingMachine;

public class WeldingMachineMapper {
    public static WeldingMachineDTO toDTO(WeldingMachine entity) {
        if (entity == null) return null;
        WeldingMachineDTO dto = new WeldingMachineDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setMac(entity.getMac());
        dto.setDeviceModel(entity.getDeviceModel());
        dto.setSerialNumber(entity.getSerialNumber());
        dto.setInventoryNumber(entity.getInventoryNumber());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setCommissionDate(entity.getDateStartedUsing());
        if (entity.getYearManufactured() != null) {
            try {
                dto.setManufactureYear(Integer.parseInt(entity.getYearManufactured()));
            } catch (NumberFormatException e) {
                dto.setManufactureYear(null);
            }
        }
        dto.setLastService(entity.getLastServiceOn());
        dto.setLastPoweredOnAt(entity.getLastPoweredOnAt());
        dto.setLastWeldAt(entity.getLastWeldAt());
        // OrganizationUnitShortDTO
        if (entity.getOrganizationUnit() != null) {
            OrganizationUnitShortDTO orgDto = new OrganizationUnitShortDTO();
            orgDto.setId(entity.getOrganizationUnit().getId());
            orgDto.setName(entity.getOrganizationUnit().getName());
            dto.setOrganizationUnit(orgDto);
        }
        // WeldingMachineTypeShortDTO
        if (entity.getWeldingMachineType() != null) {
            WeldingMachineTypeShortDTO typeDto = new WeldingMachineTypeShortDTO();
            typeDto.setId(entity.getWeldingMachineType().getId());
            typeDto.setName(entity.getWeldingMachineType().getName());
            dto.setWeldingMachineType(typeDto);
        }
        return dto;
    }

    public static WeldingMachine toEntity(WeldingMachineDTO dto) {
        if (dto == null) return null;
        WeldingMachine entity = new WeldingMachine();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setMac(dto.getMac());
        entity.setDeviceModel(dto.getDeviceModel());
        entity.setSerialNumber(dto.getSerialNumber());
        entity.setInventoryNumber(dto.getInventoryNumber());
        entity.setYearManufactured(dto.getManufactureYear() != null ? dto.getManufactureYear().toString() : null);
        entity.setDateStartedUsing(dto.getCommissionDate());
        entity.setLastServiceOn(dto.getLastService());
        if (dto.getOrganizationUnit() != null) {
            entity.setOrganizationUnitId(dto.getOrganizationUnit().getId());
        }
        if (dto.getWeldingMachineType() != null) {
            entity.setWeldingMachineTypeId(dto.getWeldingMachineType().getId());
        }
        // Можно добавить дополнительные поля, если они появятся в DTO
        return entity;
    }
} 