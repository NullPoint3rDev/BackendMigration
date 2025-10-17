package org.alloy.components;

import org.alloy.repositories.UserPermissionRepository;
import org.alloy.repositories.UserRoleRepository;
import org.alloy.repositories.UserRolePermissionRepository;
import org.alloy.services.UserRolePermissionService;
import org.alloy.services.UserRoleService;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.UserPermission;
import org.alloy.models.entities.UserRole;
import org.alloy.models.entities.UserRolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component  // Временно включен для исправления ошибок 500
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserRolePermissionService userRolePermissionService;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserRolePermissionRepository userRolePermissionRepository;

    @Override
    public void run (String... args) throws Exception {
        createDefaultRoles();
        createDefaultPermissions();
        assignPermissionsToRoles();
    }

    private void createDefaultRoles() {
        if(userRoleRepository.findByName("ADMIN").isEmpty()) {
            UserRole adminRole = new UserRole();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Администратор системы");
            adminRole.setStatus(GeneralStatus.Active);
            userRoleRepository.save(adminRole);

            UserRole managerRole = new UserRole();
            managerRole.setName("MANAGER");
            managerRole.setDescription("Менеджер");
            managerRole.setStatus(GeneralStatus.Active);
            userRoleRepository.save(managerRole);

            UserRole welderRole = new UserRole();
            welderRole.setName("WELDER");
            welderRole.setDescription("Сварщик");
            welderRole.setStatus(GeneralStatus.Active);
            userRoleRepository.save(welderRole);

            UserRole technologistRole = new UserRole();
            technologistRole.setName("TECHNOLOGIST");
            technologistRole.setDescription("Технолог предприятия");
            technologistRole.setStatus(GeneralStatus.Active);
            userRoleRepository.save(technologistRole);

            UserRole guestRole = new UserRole();
            guestRole.setName("GUEST");
            guestRole.setDescription("Гость системы");
            guestRole.setStatus(GeneralStatus.Active);
            userRoleRepository.save(guestRole);
        }
    }

    private void createDefaultPermissions() {
        List<String> defaultPermissions = Arrays.asList(
            // Управление пользователями
            "USER_READ", "USER_WRITE", "USER_DELETE", "USER_ROLE_ASSIGN",
            
            // Отчеты
            "REPORT_READ", "REPORT_CREATE", "REPORT_EDIT", "REPORT_DELETE", "REPORT_EXPORT",
            
            // Устройства
            "DEVICE_READ", "DEVICE_WRITE", "DEVICE_MAINTENANCE", "DEVICE_CALIBRATION",
            
            // Система
            "SYSTEM_SETTINGS", "SYSTEM_LOGS", "SYSTEM_BACKUP", "SYSTEM_USERS",
            
            // Организация
            "ORGANIZATION_READ", "ORGANIZATION_WRITE", "ORGANIZATION_UNITS",
            
            // Сварочные машины
            "WELDING_MACHINE_READ", "WELDING_MACHINE_WRITE", "WELDING_MACHINE_MAINTENANCE",
            
            // Сотрудники
            "EMPLOYEE_READ", "EMPLOYEE_WRITE", "EMPLOYEE_DELETE",
            
            // Уведомления
            "NOTIFICATION_READ", "NOTIFICATION_WRITE", "NOTIFICATION_DELETE",
            
            // Библиотека документов
            "LIBRARY_READ", "LIBRARY_WRITE", "LIBRARY_DELETE"
        );

        for (String permissionName : defaultPermissions) {
            if (userPermissionRepository.findByName(permissionName).isEmpty()) {
                UserPermission permission = new UserPermission();
                permission.setName(permissionName);
                userPermissionRepository.save(permission);
            }
        }
    }

    private void assignPermissionsToRoles() {
        // Получаем роли
        Optional<UserRole> adminRole = userRoleRepository.findByName("ADMIN");
        Optional<UserRole> managerRole = userRoleRepository.findByName("MANAGER");
        Optional<UserRole> welderRole = userRoleRepository.findByName("WELDER");
        Optional<UserRole> technologistRole = userRoleRepository.findByName("TECHNOLOGIST");
        Optional<UserRole> guestRole = userRoleRepository.findByName("GUEST");

        // ADMIN - все права
        if (adminRole.isPresent()) {
            assignAllPermissionsToRole(adminRole.get().getId());
        }

        // MANAGER - управленческие права
        if (managerRole.isPresent()) {
            assignManagerPermissions(managerRole.get().getId());
        }

        // TECHNOLOGIST - технические права
        if (technologistRole.isPresent()) {
            assignTechnologistPermissions(technologistRole.get().getId());
        }

        // WELDER - права сварщика
        if (welderRole.isPresent()) {
            assignWelderPermissions(welderRole.get().getId());
        }

        // GUEST - минимальные права
        if (guestRole.isPresent()) {
            assignGuestPermissions(guestRole.get().getId());
        }
    }

    private void assignAllPermissionsToRole(Integer roleId) {
        List<String> allPermissions = Arrays.asList(
            "USER_READ", "USER_WRITE", "USER_DELETE", "USER_ROLE_ASSIGN",
            "REPORT_READ", "REPORT_CREATE", "REPORT_EDIT", "REPORT_DELETE", "REPORT_EXPORT",
            "DEVICE_READ", "DEVICE_WRITE", "DEVICE_MAINTENANCE", "DEVICE_CALIBRATION",
            "SYSTEM_SETTINGS", "SYSTEM_LOGS", "SYSTEM_BACKUP", "SYSTEM_USERS",
            "ORGANIZATION_READ", "ORGANIZATION_WRITE", "ORGANIZATION_UNITS",
            "WELDING_MACHINE_READ", "WELDING_MACHINE_WRITE", "WELDING_MACHINE_MAINTENANCE",
            "EMPLOYEE_READ", "EMPLOYEE_WRITE", "EMPLOYEE_DELETE",
            "NOTIFICATION_READ", "NOTIFICATION_WRITE", "NOTIFICATION_DELETE",
            "LIBRARY_READ", "LIBRARY_WRITE", "LIBRARY_DELETE"
        );

        for (String permissionName : allPermissions) {
            assignPermissionToRole(roleId, permissionName, true, true);
        }
    }

    private void assignManagerPermissions(Integer roleId) {
        // Управление пользователями (без удаления)
        assignPermissionToRole(roleId, "USER_READ", true, true);
        assignPermissionToRole(roleId, "USER_WRITE", true, true);
        assignPermissionToRole(roleId, "USER_ROLE_ASSIGN", true, true);

        // Отчеты
        assignPermissionToRole(roleId, "REPORT_READ", true, true);
        assignPermissionToRole(roleId, "REPORT_CREATE", true, true);
        assignPermissionToRole(roleId, "REPORT_EDIT", true, true);
        assignPermissionToRole(roleId, "REPORT_EXPORT", true, true);

        // Устройства
        assignPermissionToRole(roleId, "DEVICE_READ", true, true);
        assignPermissionToRole(roleId, "DEVICE_WRITE", true, true);

        // Система (только просмотр логов)
        assignPermissionToRole(roleId, "SYSTEM_LOGS", true, false);

        // Организация
        assignPermissionToRole(roleId, "ORGANIZATION_READ", true, true);
        assignPermissionToRole(roleId, "ORGANIZATION_WRITE", true, true);
        assignPermissionToRole(roleId, "ORGANIZATION_UNITS", true, true);

        // Сварочные машины
        assignPermissionToRole(roleId, "WELDING_MACHINE_READ", true, true);
        assignPermissionToRole(roleId, "WELDING_MACHINE_WRITE", true, true);

        // Сотрудники
        assignPermissionToRole(roleId, "EMPLOYEE_READ", true, true);
        assignPermissionToRole(roleId, "EMPLOYEE_WRITE", true, true);

        // Уведомления
        assignPermissionToRole(roleId, "NOTIFICATION_READ", true, true);
        assignPermissionToRole(roleId, "NOTIFICATION_WRITE", true, true);

        // Библиотека
        assignPermissionToRole(roleId, "LIBRARY_READ", true, true);
        assignPermissionToRole(roleId, "LIBRARY_WRITE", true, true);
    }

    private void assignTechnologistPermissions(Integer roleId) {
        // Устройства и обслуживание
        assignPermissionToRole(roleId, "DEVICE_READ", true, true);
        assignPermissionToRole(roleId, "DEVICE_WRITE", true, true);
        assignPermissionToRole(roleId, "DEVICE_MAINTENANCE", true, true);
        assignPermissionToRole(roleId, "DEVICE_CALIBRATION", true, true);

        // Сварочные машины
        assignPermissionToRole(roleId, "WELDING_MACHINE_READ", true, true);
        assignPermissionToRole(roleId, "WELDING_MACHINE_WRITE", true, true);
        assignPermissionToRole(roleId, "WELDING_MACHINE_MAINTENANCE", true, true);

        // Отчеты (создание и просмотр)
        assignPermissionToRole(roleId, "REPORT_READ", true, true);
        assignPermissionToRole(roleId, "REPORT_CREATE", true, true);
        assignPermissionToRole(roleId, "REPORT_EXPORT", true, true);

        // Организация (только просмотр)
        assignPermissionToRole(roleId, "ORGANIZATION_READ", true, false);

        // Сотрудники (только просмотр)
        assignPermissionToRole(roleId, "EMPLOYEE_READ", true, false);

        // Библиотека
        assignPermissionToRole(roleId, "LIBRARY_READ", true, true);
        assignPermissionToRole(roleId, "LIBRARY_WRITE", true, true);
    }

    private void assignWelderPermissions(Integer roleId) {
        // Устройства (только просмотр и базовое использование)
        assignPermissionToRole(roleId, "DEVICE_READ", true, false);
        assignPermissionToRole(roleId, "DEVICE_WRITE", true, false);

        // Сварочные машины (только просмотр)
        assignPermissionToRole(roleId, "WELDING_MACHINE_READ", true, false);

        // Отчеты (только создание и просмотр своих)
        assignPermissionToRole(roleId, "REPORT_READ", true, false);
        assignPermissionToRole(roleId, "REPORT_CREATE", true, false);

        // Организация (только просмотр)
        assignPermissionToRole(roleId, "ORGANIZATION_READ", true, false);

        // Библиотека (только просмотр)
        assignPermissionToRole(roleId, "LIBRARY_READ", true, false);
    }

    private void assignGuestPermissions(Integer roleId) {
        // Минимальные права - только просмотр
        assignPermissionToRole(roleId, "ORGANIZATION_READ", true, false);
        assignPermissionToRole(roleId, "LIBRARY_READ", true, false);
    }

    private void assignPermissionToRole(Integer roleId, String permissionName, Boolean read, Boolean write) {
        Optional<UserPermission> permission = userPermissionRepository.findByName(permissionName);
        if (permission.isPresent()) {
            // Проверяем, не назначено ли уже это разрешение роли (оптимизированно)
            boolean alreadyAssigned = userRolePermissionRepository
                .findByUserRoleIdAndUserPermissionId(roleId, permission.get().getId())
                .isPresent();

            if (!alreadyAssigned) {
                UserRolePermission rolePermission = new UserRolePermission();
                rolePermission.setUserRoleId(roleId);
                rolePermission.setUserPermissionId(permission.get().getId());
                rolePermission.setRead(read);
                rolePermission.setWrite(write);
                userRolePermissionRepository.save(rolePermission);
            }
        }
    }
}