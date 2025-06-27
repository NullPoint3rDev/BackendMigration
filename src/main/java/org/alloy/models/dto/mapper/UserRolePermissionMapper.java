package org.alloy.models.dto.mapper;

import org.alloy.models.entities.UserRolePermission;
import org.alloy.models.dto.UserRolePermissionDTO;

public class UserRolePermissionMapper {
    public static UserRolePermissionDTO toDTO(UserRolePermission entity) {
        if (entity == null) return null;
        UserRolePermissionDTO dto = new UserRolePermissionDTO();
        dto.setId(entity.getId());
        // ... добавить другие поля по необходимости
        return dto;
    }

    public static UserRolePermission toEntity(UserRolePermissionDTO dto) {
        if (dto == null) return null;
        UserRolePermission entity = new UserRolePermission();
        entity.setId(dto.getId());
        // ... добавить другие поля по необходимости
        return entity;
    }
} 