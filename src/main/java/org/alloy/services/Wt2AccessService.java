package org.alloy.services;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.Organization;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRole;
import org.alloy.models.entities.Welder;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.repositories.OrganizationRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.alloy.repositories.UserAccountRepository;
import org.alloy.repositories.UserRoleRepository;
import org.alloy.repositories.WelderRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Серверная модель доступа WT2: скоуп по организации для ролей предприятия,
 * скрытие организации Alloy для не-Admin-Alloy, запрет ролей Alloy без Admin Alloy.
 */
@Service
public class Wt2AccessService {

    private static final String ROLE_ADMIN_ALLOY = "ADMIN_ALLOY";
    private static final String ROLE_USER_ALLOY = "USER_ALLOY";
    private static final String ROLE_ADMIN_ENTERPRISE = "ADMIN_ENTERPRISE";
    private static final String ROLE_USER_ENTERPRISE = "USER_ENTERPRISE";

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final WeldingMachineRepository weldingMachineRepository;
    private final WelderRepository welderRepository;

    public Wt2AccessService(UserAccountRepository userAccountRepository,
                            UserRoleRepository userRoleRepository,
                            OrganizationRepository organizationRepository,
                            OrganizationUnitRepository organizationUnitRepository,
                            WeldingMachineRepository weldingMachineRepository,
                            WelderRepository welderRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.organizationRepository = organizationRepository;
        this.organizationUnitRepository = organizationUnitRepository;
        this.weldingMachineRepository = weldingMachineRepository;
        this.welderRepository = welderRepository;
    }

    public Optional<UserAccount> findActiveByUserName(String userName) {
        if (userName == null || userName.isEmpty()) {
            return Optional.empty();
        }
        return userAccountRepository.findByUserNameAndStatusNot(userName, GeneralStatus.Deleted);
    }

    public Optional<UserAccount> findActiveByUserNameWithOrganization(String userName) {
        return userAccountRepository.findActiveByUserNameWithOrganization(userName, GeneralStatus.Deleted);
    }

    public boolean isAdminAlloy(UserAccount ua) {
        return roleName(ua).map(ROLE_ADMIN_ALLOY::equals).orElse(false);
    }

    public boolean isEnterpriseScopedRole(UserAccount ua) {
        return roleName(ua).map(n -> ROLE_ADMIN_ENTERPRISE.equals(n) || ROLE_USER_ENTERPRISE.equals(n)).orElse(false);
    }

    private Optional<String> roleName(UserAccount ua) {
        if (ua == null || ua.getUserRoleId() == null) {
            return Optional.empty();
        }
        return userRoleRepository.findById(ua.getUserRoleId()).map(UserRole::getName);
    }

    /**
     * ID организации пользователя: напрямую или через подразделение.
     */
    public Integer resolveOrganizationId(UserAccount ua) {
        if (ua == null) {
            return null;
        }
        if (ua.getOrganization() != null) {
            return ua.getOrganization().getId();
        }
        if (ua.getOrganizationUnitId() != null) {
            return organizationUnitRepository.findById(ua.getOrganizationUnitId())
                    .map(OrganizationUnit::getOrganizationId)
                    .orElse(null);
        }
        return null;
    }

    public static boolean isAlloyOrganizationName(String name) {
        return name != null && "alloy".equals(name.trim().toLowerCase(Locale.ROOT));
    }

    public List<Integer> organizationUnitIdsForOrganization(Integer organizationId) {
        if (organizationId == null) {
            return Collections.emptyList();
        }
        return organizationUnitRepository.findByOrganizationId(organizationId).stream()
                .map(OrganizationUnit::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Organization> filterOrganizations(List<Organization> all, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return all;
        }
        UserAccount ua = uaOpt.get();
        if (isEnterpriseScopedRole(ua)) {
            Integer orgId = resolveOrganizationId(ua);
            if (orgId == null) {
                return Collections.emptyList();
            }
            return all.stream().filter(o -> orgId.equals(o.getId())).collect(Collectors.toList());
        }
        if (!isAdminAlloy(ua)) {
            return all.stream().filter(o -> !isAlloyOrganizationName(o.getName())).collect(Collectors.toList());
        }
        return all;
    }

    public void assertCanViewOrganization(Integer organizationId, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return;
        }
        UserAccount ua = uaOpt.get();
        if (isEnterpriseScopedRole(ua)) {
            Integer oid = resolveOrganizationId(ua);
            if (oid == null || !oid.equals(organizationId)) {
                throw new AccessDeniedException("Нет доступа к этой организации");
            }
            return;
        }
        if (!isAdminAlloy(ua)) {
            organizationRepository.findById(organizationId).ifPresent(o -> {
                if (isAlloyOrganizationName(o.getName())) {
                    throw new AccessDeniedException("Нет доступа к организации Alloy");
                }
            });
        }
    }

    public List<OrganizationUnit> filterOrganizationUnits(List<OrganizationUnit> all, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return all;
        }
        UserAccount ua = uaOpt.get();
        if (isEnterpriseScopedRole(ua)) {
            Integer orgId = resolveOrganizationId(ua);
            if (orgId == null) {
                return Collections.emptyList();
            }
            return all.stream().filter(u -> orgId.equals(u.getOrganizationId())).collect(Collectors.toList());
        }
        return all;
    }

    public void assertCanViewOrganizationUnit(Integer organizationUnitId, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return;
        }
        UserAccount ua = uaOpt.get();
        if (isEnterpriseScopedRole(ua)) {
            Integer orgId = resolveOrganizationId(ua);
            if (orgId == null) {
                throw new AccessDeniedException("Нет доступа к подразделению");
            }
            OrganizationUnit unit = organizationUnitRepository.findById(organizationUnitId)
                    .orElseThrow(() -> new AccessDeniedException("Нет доступа к подразделению"));
            if (!orgId.equals(unit.getOrganizationId())) {
                throw new AccessDeniedException("Нет доступа к подразделению");
            }
        }
    }

    public void assertOrganizationUnitBelongsToOrg(Integer organizationUnitId, Integer expectedOrganizationId) {
        if (organizationUnitId == null || expectedOrganizationId == null) {
            return;
        }
        OrganizationUnit unit = organizationUnitRepository.findById(organizationUnitId)
                .orElseThrow(() -> new AccessDeniedException("Недопустимое подразделение"));
        if (!expectedOrganizationId.equals(unit.getOrganizationId())) {
            throw new AccessDeniedException("Подразделение не принадлежит организации");
        }
    }

    /** Создание/изменение подразделения: роли предприятия — только в своей организации. */
    public void assertEnterpriseCanManageOrganizationUnit(OrganizationUnit unit, String principalName) {
        if (unit == null || principalName == null) {
            return;
        }
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return;
        }
        Integer orgId = resolveOrganizationId(ua);
        if (orgId == null) {
            throw new AccessDeniedException("Нет доступа к подразделению");
        }
        if (unit.getOrganizationId() == null || !unit.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Нет доступа к подразделению");
        }
    }

    public List<UserAccount> filterUserAccounts(List<UserAccount> all, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return all;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return all;
        }
        Integer orgId = resolveOrganizationId(ua);
        if (orgId == null) {
            return Collections.emptyList();
        }
        Set<Integer> unitIds = new HashSet<>(organizationUnitIdsForOrganization(orgId));
        return all.stream().filter(row -> {
            Integer rowOrg = resolveOrganizationId(row);
            if (rowOrg != null && rowOrg.equals(orgId)) {
                return true;
            }
            return row.getOrganizationUnitId() != null && unitIds.contains(row.getOrganizationUnitId());
        }).collect(Collectors.toList());
    }

    public void assertCanViewUserAccount(Integer targetUserAccountId, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return;
        }
        UserAccount target = userAccountRepository.findById(targetUserAccountId).orElse(null);
        if (target == null) {
            return;
        }
        List<UserAccount> allowed = filterUserAccounts(Collections.singletonList(target), principalName);
        if (allowed.isEmpty()) {
            throw new AccessDeniedException("Нет доступа к пользователю");
        }
    }

    public List<WeldingMachine> filterWeldingMachines(List<WeldingMachine> all, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return all;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return all;
        }
        Integer orgId = resolveOrganizationId(ua);
        if (orgId == null) {
            return Collections.emptyList();
        }
        Set<Integer> unitIds = new HashSet<>(organizationUnitIdsForOrganization(orgId));
        return all.stream()
                .filter(m -> m.getOrganizationUnitId() != null && unitIds.contains(m.getOrganizationUnitId()))
                .collect(Collectors.toList());
    }

    public boolean canAccessWeldingMachine(Integer weldingMachineId, String principalName) {
        Optional<WeldingMachine> mOpt = weldingMachineRepository.findById(weldingMachineId);
        if (!mOpt.isPresent()) {
            return false;
        }
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return true;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return true;
        }
        Integer orgId = resolveOrganizationId(ua);
        if (orgId == null) {
            return false;
        }
        Set<Integer> unitIds = new HashSet<>(organizationUnitIdsForOrganization(orgId));
        Integer uid = mOpt.get().getOrganizationUnitId();
        return uid != null && unitIds.contains(uid);
    }

    public void assertCanAccessWeldingMachine(Integer weldingMachineId, String principalName) {
        if (!weldingMachineRepository.findById(weldingMachineId).isPresent()) {
            return;
        }
        if (!canAccessWeldingMachine(weldingMachineId, principalName)) {
            throw new AccessDeniedException("Нет доступа к сварочному аппарату");
        }
    }

    /** Создание/перенос аппарата: подразделение должно принадлежать организации пользователя-предприятия. */
    public List<WeldingMachineState> filterWeldingMachineStates(List<WeldingMachineState> all, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return all;
        }
        if (!isEnterpriseScopedRole(uaOpt.get())) {
            return all;
        }
        return all.stream()
                .filter(s -> s.getWeldingMachineId() != null && canAccessWeldingMachine(s.getWeldingMachineId(), principalName))
                .collect(Collectors.toList());
    }

    public void assertEnterpriseCanManageMachineOrgUnit(Integer organizationUnitId, String principalName) {
        if (organizationUnitId == null) {
            return;
        }
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return;
        }
        Integer orgId = resolveOrganizationId(ua);
        if (orgId == null) {
            throw new AccessDeniedException("Нет доступа к аппарату");
        }
        assertOrganizationUnitBelongsToOrg(organizationUnitId, orgId);
    }

    public List<Welder> filterWelders(List<Welder> all, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return all;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return all;
        }
        Integer orgId = resolveOrganizationId(ua);
        if (orgId == null) {
            return Collections.emptyList();
        }
        List<Integer> unitIds = organizationUnitIdsForOrganization(orgId);
        if (unitIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Welder> fromMachines = welderRepository.findDistinctByWeldingMachinesOrganizationUnitIdIn(unitIds);
        Set<Long> seen = new HashSet<>();
        for (Welder w : fromMachines) {
            seen.add(w.getId());
        }
        Set<String> namesLower = organizationUnitRepository.findByOrganizationId(orgId).stream()
                .map(OrganizationUnit::getName)
                .filter(Objects::nonNull)
                .map(n -> n.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<Welder> out = new ArrayList<>(fromMachines);
        for (Welder w : all) {
            if (seen.contains(w.getId())) {
                continue;
            }
            if (w.getDepartment() != null) {
                String d = w.getDepartment().trim().toLowerCase(Locale.ROOT);
                if (namesLower.contains(d)) {
                    out.add(w);
                    seen.add(w.getId());
                }
            }
        }
        return out;
    }

    public void assertCanViewWelder(Long welderId, String principalName) {
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return;
        }
        Welder w = welderRepository.findById(welderId).orElse(null);
        if (w == null) {
            return;
        }
        List<Welder> allowed = filterWelders(Collections.singletonList(w), principalName);
        if (allowed.isEmpty()) {
            throw new AccessDeniedException("Нет доступа к сварщику");
        }
    }

    /**
     * Создание/обновление пользователя: роли Alloy только для Admin Alloy; скоуп организации для ролей предприятия.
     */
    public void assertCanCreateOrUpdateUserAccount(UserAccount newState, String principalName, Integer existingUserId) {
        if (principalName == null || newState == null) {
            return;
        }
        Optional<UserAccount> actorOpt = findActiveByUserNameWithOrganization(principalName);
        if (!actorOpt.isPresent()) {
            return;
        }
        UserAccount actor = actorOpt.get();
        if (newState.getUserRoleId() != null) {
            UserRole targetRole = userRoleRepository.findById(newState.getUserRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Роль не найдена"));
            String tn = targetRole.getName();
            if (ROLE_ADMIN_ALLOY.equals(tn) || ROLE_USER_ALLOY.equals(tn)) {
                if (!isAdminAlloy(actor)) {
                    throw new AccessDeniedException("Только Админ Эллой может назначать роли Эллой");
                }
            }
        }
        if (isEnterpriseScopedRole(actor)) {
            Integer orgId = resolveOrganizationId(actor);
            if (orgId == null) {
                throw new AccessDeniedException("Нет доступа к операции с пользователем");
            }
            Integer orgOnAccount = resolveOrganizationId(newState);
            if (orgOnAccount != null && !orgOnAccount.equals(orgId)) {
                throw new AccessDeniedException("Нельзя привязать пользователя к чужой организации");
            }
            if (newState.getOrganizationUnitId() != null) {
                assertOrganizationUnitBelongsToOrg(newState.getOrganizationUnitId(), orgId);
            }
            if (existingUserId != null) {
                assertCanViewUserAccount(existingUserId, principalName);
            }
        }
    }

    public void assertCanAssignRoleName(String roleName, String principalName) {
        if (roleName == null || principalName == null) {
            return;
        }
        if (ROLE_ADMIN_ALLOY.equals(roleName) || ROLE_USER_ALLOY.equals(roleName)) {
            Optional<UserAccount> actorOpt = findActiveByUserNameWithOrganization(principalName);
            if (actorOpt.isPresent() && !isAdminAlloy(actorOpt.get())) {
                throw new AccessDeniedException("Только Админ Эллой может назначать роли Эллой");
            }
        }
    }

    public void assertCanAssignRoleId(Integer roleId, String principalName) {
        if (roleId == null) {
            return;
        }
        userRoleRepository.findById(roleId).ifPresent(r -> assertCanAssignRoleName(r.getName(), principalName));
    }

    /** Подразделение сварщика (строка) должно совпадать с названием подразделения организации. */
    public void assertEnterpriseWelderDepartmentAllowed(String department, String principalName) {
        if (department == null || department.trim().isEmpty()) {
            return;
        }
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return;
        }
        Integer orgId = resolveOrganizationId(ua);
        if (orgId == null) {
            throw new AccessDeniedException("Нет доступа");
        }
        String d = department.trim().toLowerCase(Locale.ROOT);
        boolean ok = organizationUnitRepository.findByOrganizationId(orgId).stream()
                .map(OrganizationUnit::getName)
                .filter(Objects::nonNull)
                .anyMatch(n -> n.trim().toLowerCase(Locale.ROOT).equals(d));
        if (!ok) {
            throw new AccessDeniedException("Недопустимое подразделение для сварщика");
        }
    }

    /** Привязка сварщика к аппаратам: только аппараты своей организации. */
    public void assertEnterpriseCanAssignWelderMachines(List<Integer> machineIds, String principalName) {
        if (machineIds == null || machineIds.isEmpty()) {
            return;
        }
        Optional<UserAccount> uaOpt = findActiveByUserNameWithOrganization(principalName);
        if (!uaOpt.isPresent()) {
            return;
        }
        UserAccount ua = uaOpt.get();
        if (!isEnterpriseScopedRole(ua)) {
            return;
        }
        Integer orgId = resolveOrganizationId(ua);
        if (orgId == null) {
            throw new AccessDeniedException("Нет доступа");
        }
        Set<Integer> unitIds = new HashSet<>(organizationUnitIdsForOrganization(orgId));
        for (Integer mid : machineIds) {
            WeldingMachine m = weldingMachineRepository.findById(mid)
                    .orElseThrow(() -> new AccessDeniedException("Недопустимый аппарат"));
            Integer uid = m.getOrganizationUnitId();
            if (uid == null || !unitIds.contains(uid)) {
                throw new AccessDeniedException("Нельзя привязать к аппарату вне организации");
            }
        }
    }
}
