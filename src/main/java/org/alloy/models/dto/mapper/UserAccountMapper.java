package org.alloy.models.dto.mapper;

import org.alloy.models.dto.UserAccountDTO;
import org.alloy.models.dto.OrganizationUnitShortDTO;
import org.alloy.models.dto.UserAccountShortDTO;
import org.alloy.models.entities.UserAccount;

public class UserAccountMapper {
    public static UserAccountDTO toDTO(UserAccount entity) {
        if (entity == null) return null;
        UserAccountDTO dto = new UserAccountDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUserName());
        dto.setEmail(entity.getEmail());
        dto.setFullName(entity.getName());
        if (entity.getOrganizationUnit() != null) {
            OrganizationUnitShortDTO orgDto = new OrganizationUnitShortDTO();
            orgDto.setId(entity.getOrganizationUnit().getId());
            orgDto.setName(entity.getOrganizationUnit().getName());
            dto.setOrganizationUnit(orgDto);
        }
        return dto;
    }

    public static UserAccountShortDTO toShortDTO(UserAccount entity) {
        if (entity == null) return null;
        UserAccountShortDTO dto = new UserAccountShortDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUserName());
        return dto;
    }
} 