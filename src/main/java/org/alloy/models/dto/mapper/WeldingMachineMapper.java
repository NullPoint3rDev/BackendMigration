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
        dto.setModel(entity.getModel());
        dto.setMac(entity.getMac());
        dto.setSerialNumber(entity.getSerialNumber());
        dto.setInventoryNumber(entity.getInventoryNumber());
        dto.setImageUrl(entity.getImageUrl());
        dto.setDepartment(entity.getDepartment());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setCommissionDate(entity.getCommissionDate());
        dto.setManufactureYear(entity.getManufactureYear());
        dto.setLastService(entity.getLastService());
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
} 