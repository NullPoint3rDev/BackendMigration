package org.alloy.config;

import org.alloy.models.GeneralStatus;
import org.alloy.models.User;
import org.alloy.models.entities.Organization;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserPermission;
import org.alloy.models.entities.UserRole;
import org.alloy.models.entities.UserRolePermission;
import org.alloy.repositories.OrganizationRepository;
import org.alloy.repositories.UserAccountRepository;
import org.alloy.repositories.UserPermissionRepository;
import org.alloy.repositories.UserRepository;
import org.alloy.repositories.UserRolePermissionRepository;
import org.alloy.repositories.UserRoleRepository;
import org.alloy.security.Wt2Permission;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Минимальная инициализация для профиля staging: предприятие и администратор Alloy.
 */
@Component
@Profile("staging")
public class StagingDataInitializer implements CommandLineRunner {

    public static final String ORGANIZATION_NAME = "Компания Alloy";
    public static final String ADMIN_LOGIN = "Administrator";
    public static final String ADMIN_PASSWORD = "Admin123!@#";
    public static final String ADMIN_EMAIL = "administrator@staging.local";
    public static final String ROLE_ADMIN_ALLOY = "ADMIN_ALLOY";

    private final OrganizationRepository organizationRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserRolePermissionRepository userRolePermissionRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StagingDataInitializer(OrganizationRepository organizationRepository,
                                  UserRoleRepository userRoleRepository,
                                  UserPermissionRepository userPermissionRepository,
                                  UserRolePermissionRepository userRolePermissionRepository,
                                  UserAccountRepository userAccountRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.userRoleRepository = userRoleRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.userRolePermissionRepository = userRolePermissionRepository;
        this.userAccountRepository = userAccountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Organization organization = ensureOrganization();
        UserRole adminRole = ensureAdminAlloyRole();
        ensureWt2PermissionsForRole(adminRole);
        ensureAdministratorUser(organization, adminRole);
    }

    private Organization ensureOrganization() {
        return organizationRepository.findAll().stream()
                .filter(o -> ORGANIZATION_NAME.equalsIgnoreCase(o.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Organization org = new Organization();
                    org.setName(ORGANIZATION_NAME);
                    org.setDescription("Staging-предприятие");
                    org.setStatus(GeneralStatus.Active);
                    org.setDateCreated(LocalDateTime.now());
                    return organizationRepository.save(org);
                });
    }

    private UserRole ensureAdminAlloyRole() {
        return userRoleRepository.findByName(ROLE_ADMIN_ALLOY).orElseGet(() -> {
            UserRole role = new UserRole();
            role.setName(ROLE_ADMIN_ALLOY);
            role.setDescription("Администратор Эллой");
            role.setStatus(GeneralStatus.Active);
            role.setRoleLevel(1);
            return userRoleRepository.save(role);
        });
    }

    private void ensureWt2PermissionsForRole(UserRole adminRole) {
        for (Wt2Permission wt2Permission : Wt2Permission.values()) {
            String permissionName = wt2Permission.name();
            UserPermission permission = userPermissionRepository.findByName(permissionName)
                    .orElseGet(() -> {
                        UserPermission p = new UserPermission();
                        p.setName(permissionName);
                        return userPermissionRepository.save(p);
                    });

            boolean linked = userRolePermissionRepository.findByUserRoleId(adminRole.getId()).stream()
                    .anyMatch(rp -> rp.getUserPermissionId().equals(permission.getId()));
            if (!linked) {
                UserRolePermission rolePermission = new UserRolePermission();
                rolePermission.setUserRoleId(adminRole.getId());
                rolePermission.setUserPermissionId(permission.getId());
                rolePermission.setRead(true);
                rolePermission.setWrite(true);
                rolePermission.setConfigurableByRoleLevel(null);
                userRolePermissionRepository.save(rolePermission);
            }
        }
    }

    private void ensureAdministratorUser(Organization organization, UserRole adminRole) {
        String encodedPassword = passwordEncoder.encode(ADMIN_PASSWORD);

        UserAccount account = userAccountRepository.findByUserName(ADMIN_LOGIN).orElseGet(UserAccount::new);
        account.setUserName(ADMIN_LOGIN);
        account.setEmail(ADMIN_EMAIL);
        account.setName("Administrator");
        account.setUserRoleId(adminRole.getId());
        account.setStatus(GeneralStatus.Active);
        account.setFailedLoginsCount(0);
        account.setEmailVerified(true);
        account.setOrganizationUnitId(null);
        account.setOrganization(organization);
        account.setPasswordHash(encodedPassword.getBytes(StandardCharsets.UTF_8));
        account.setAllowedUserActions("work_with_reports");
        userAccountRepository.save(account);

        User authUser = userRepository.findByUsername(ADMIN_LOGIN).orElseGet(User::new);
        authUser.setUsername(ADMIN_LOGIN);
        authUser.setEmail(ADMIN_EMAIL);
        authUser.setUserRoleId(adminRole.getId());
        authUser.setStatus(0);
        authUser.setPassword(encodedPassword);
        userRepository.save(authUser);
    }
}
