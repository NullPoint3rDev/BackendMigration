package org.alloy.models.dto.mapper;

import org.alloy.models.entities.Organization;
import org.alloy.models.dto.OrganizationShortDTO;

public class OrganizationMapper {
    public static OrganizationShortDTO toShortDTO(Organization organization) {
        if (organization == null) return null;
        OrganizationShortDTO dto = new OrganizationShortDTO();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        return dto;
    }
    // Если потребуется OrganizationDTO с расширенными полями, добавить аналогичный метод toDTO
} 