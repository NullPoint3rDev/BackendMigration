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
        dto.setUserRoleId(entity.getUserRoleId());
        dto.setPosition(entity.getPosition());
        dto.setPhone(entity.getPhone());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setPhoto(entity.getPhoto() != null ? entity.getPhoto().toString() : null);
        dto.setWorkplace(entity.getWorkplace());
        dto.setCategory(entity.getCategory());
        dto.setPersonnelNumber(entity.getPersonnelNumber());
        dto.setRfid(entity.getRfid());
        dto.setRecruitmentDate(entity.getRecruitmentDate() != null ? entity.getRecruitmentDate().toString() : null);
        dto.setBirthDate(entity.getBirthDate() != null ? entity.getBirthDate().toString() : null);
        dto.setEducation(entity.getEducation());
        dto.setAddress(entity.getAddress());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    public static UserAccountShortDTO toShortDTO(UserAccount entity) {
        if (entity == null) return null;
        UserAccountShortDTO dto = new UserAccountShortDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUserName());
        return dto;
    }

    public static UserAccount toEntity(UserAccountDTO dto) {
        if (dto == null) return null;
        UserAccount entity = new UserAccount();
        entity.setId(dto.getId());
        entity.setUserName(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setName(dto.getFullName());
        if (dto.getOrganizationUnit() != null) {
            entity.setOrganizationUnitId(dto.getOrganizationUnit().getId());
        }
        entity.setUserRoleId(dto.getUserRoleId());
        entity.setPosition(dto.getPosition());
        entity.setPhone(dto.getPhone());
        entity.setStatus(dto.getStatus() != null ? org.alloy.models.GeneralStatus.valueOf(dto.getStatus()) : null);
        if (dto.getPhoto() != null) entity.setPhoto(java.util.UUID.fromString(dto.getPhoto()));
        entity.setWorkplace(dto.getWorkplace());
        entity.setCategory(dto.getCategory());
        entity.setPersonnelNumber(dto.getPersonnelNumber());
        entity.setRfid(dto.getRfid());
        if (dto.getRecruitmentDate() != null) entity.setRecruitmentDate(java.time.LocalDateTime.parse(dto.getRecruitmentDate()));
        if (dto.getBirthDate() != null) entity.setBirthDate(java.time.LocalDateTime.parse(dto.getBirthDate()));
        entity.setEducation(dto.getEducation());
        entity.setAddress(dto.getAddress());
        entity.setDescription(dto.getDescription());
        return entity;
    }
} 