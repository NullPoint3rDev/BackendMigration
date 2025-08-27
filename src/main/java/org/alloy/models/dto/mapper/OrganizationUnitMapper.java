package org.alloy.models.dto.mapper;

import org.alloy.models.dto.OrganizationUnitDTO;
import org.alloy.models.dto.OrganizationShortDTO;
import org.alloy.models.entities.OrganizationUnit;

public class OrganizationUnitMapper {
    public static OrganizationUnitDTO toDTO(OrganizationUnit entity) {
        if (entity == null) return null;
        OrganizationUnitDTO dto = new OrganizationUnitDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setAddress(entity.getAddress());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setLevel(entity.getLevel() != null ? entity.getLevel() : 1);
        if (entity.getParent() != null) {
            OrganizationUnitDTO parentDto = new OrganizationUnitDTO();
            parentDto.setId(entity.getParent().getId());
            parentDto.setName(entity.getParent().getName());
            dto.setParentDepartment(parentDto);
        } else {
            dto.setParentDepartment(null);
        }
        if (entity.getOrganization() != null) {
            OrganizationShortDTO orgDto = new OrganizationShortDTO();
            orgDto.setId(entity.getOrganization().getId());
            orgDto.setName(entity.getOrganization().getName());
            dto.setOrganization(orgDto);
        }
        return dto;
    }

    public static OrganizationUnit toEntity(OrganizationUnitDTO dto) {
        if (dto == null) return null;
        OrganizationUnit entity = new OrganizationUnit();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setAddress(dto.getAddress());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        if (dto.getOrganization() != null) {
            entity.setOrganizationId(dto.getOrganization().getId());
        } else {
            // Устанавливаем дефолтную организацию (ID = 1)
            entity.setOrganizationId(1);
        }
        // Устанавливаем уровень
        entity.setLevel(dto.getLevel() != null ? dto.getLevel() : 1);
        
        // Устанавливаем родительское подразделение
        if (dto.getParentDepartment() != null && dto.getParentDepartment().getId() != null) {
            entity.setParentId(dto.getParentDepartment().getId());
        } else {
            entity.setParentId(null);
        }
        
        // Устанавливаем дефолтный статус
        entity.setStatus(org.alloy.models.GeneralStatus.Active);
        return entity;
    }
} 