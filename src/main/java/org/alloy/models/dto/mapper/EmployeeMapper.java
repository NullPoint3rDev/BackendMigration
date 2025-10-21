package org.alloy.models.dto.mapper;

import org.alloy.models.entities.Employee;
import org.alloy.models.dto.EmployeeDTO;
import org.alloy.models.dto.OrganizationUnitShortDTO;
import org.alloy.models.dto.UserRoleShortDTO;
import org.alloy.models.EmployeeType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.format.DateTimeFormatter;

public class EmployeeMapper {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static Employee toEntity(EmployeeDTO dto, PasswordEncoder passwordEncoder) {
        Employee entity = new Employee();
        entity.setId(dto.getId());
        entity.setUsername(dto.getUsername());
        
        // Хешируем пароль только если он предоставлен
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        entity.setFullName(dto.getFullName());
        entity.setEmail(dto.getEmail());
        entity.setEmployeeType(dto.getEmployeeType() != null ? dto.getEmployeeType() : EmployeeType.PROGRAMMER);
        entity.setPosition(dto.getPosition());
        entity.setPhone(dto.getPhone());
        entity.setPhoto(dto.getPhoto());
        entity.setStatus(dto.getStatus());
        
        // Устанавливаем подразделение
        if (dto.getOrganizationUnit() != null) {
            // Здесь нужно будет загрузить OrganizationUnit по ID
            // Пока оставляем null, будет установлено в сервисе
        }
        
        // Устанавливаем роль пользователя
        if (dto.getUserRole() != null) {
            // Здесь нужно будет загрузить UserRole по ID
            // Пока оставляем null, будет установлено в сервисе
        }
        
        return entity;
    }
    
    public static EmployeeDTO toDTO(Employee entity) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setPassword(null); // Не возвращаем пароль в DTO
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setEmployeeType(entity.getEmployeeType() != null ? entity.getEmployeeType() : EmployeeType.PROGRAMMER);
        dto.setPosition(entity.getPosition());
        dto.setPhone(entity.getPhone());
        dto.setPhoto(entity.getPhoto());
        dto.setStatus(entity.getStatus());
        
        // Устанавливаем подразделение
        if (entity.getOrganizationUnit() != null) {
            OrganizationUnitShortDTO orgDto = new OrganizationUnitShortDTO();
            orgDto.setId(entity.getOrganizationUnit().getId());
            orgDto.setName(entity.getOrganizationUnit().getName());
            dto.setOrganizationUnit(orgDto);
        }
        
        // Устанавливаем роль пользователя
        if (entity.getUserRole() != null) {
            UserRoleShortDTO roleDto = new UserRoleShortDTO();
            roleDto.setId(entity.getUserRole().getId());
            roleDto.setName(entity.getUserRole().getName());
            dto.setUserRole(roleDto);
        }
        
        // Форматируем даты
        if (entity.getDateCreated() != null) {
            dto.setDateCreated(entity.getDateCreated().format(DATE_FORMATTER));
        }
        if (entity.getDateUpdated() != null) {
            dto.setDateUpdated(entity.getDateUpdated().format(DATE_FORMATTER));
        }
        
        return dto;
    }
}
