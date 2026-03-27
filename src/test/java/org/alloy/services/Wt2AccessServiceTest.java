package org.alloy.services;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.Organization;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRole;
import org.alloy.repositories.OrganizationRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.alloy.repositories.UserAccountRepository;
import org.alloy.repositories.UserRoleRepository;
import org.alloy.repositories.WelderRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Wt2AccessServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationUnitRepository organizationUnitRepository;
    @Mock
    private WeldingMachineRepository weldingMachineRepository;
    @Mock
    private WelderRepository welderRepository;

    private Wt2AccessService service;

    @BeforeEach
    void setUp() {
        service = new Wt2AccessService(
                userAccountRepository,
                userRoleRepository,
                organizationRepository,
                organizationUnitRepository,
                weldingMachineRepository,
                welderRepository);
    }

    @Test
    void filterOrganizations_enterpriseAdmin_returnsOnlyOwnOrganization() {
        Organization a = new Organization();
        a.setId(1);
        a.setName("Other");
        Organization b = new Organization();
        b.setId(2);
        b.setName("Mine");

        UserAccount actor = new UserAccount();
        actor.setUserRoleId(50);
        actor.setOrganization(b);

        when(userAccountRepository.findActiveByUserNameWithOrganization(eq("ent"), eq(GeneralStatus.Deleted)))
                .thenReturn(Optional.of(actor));
        when(userRoleRepository.findById(50)).thenReturn(Optional.of(role("ADMIN_ENTERPRISE")));

        List<Organization> out = service.filterOrganizations(Arrays.asList(a, b), "ent");
        assertEquals(1, out.size());
        assertEquals(2, out.get(0).getId());
    }

    @Test
    void filterOrganizations_nonAlloyAdmin_hidesAlloyOrganization() {
        Organization alloy = new Organization();
        alloy.setId(1);
        alloy.setName("Alloy");
        Organization plant = new Organization();
        plant.setId(2);
        plant.setName("Plant");

        UserAccount actor = new UserAccount();
        actor.setUserRoleId(40);
        actor.setOrganization(plant);

        when(userAccountRepository.findActiveByUserNameWithOrganization(eq("dealer"), eq(GeneralStatus.Deleted)))
                .thenReturn(Optional.of(actor));
        when(userRoleRepository.findById(40)).thenReturn(Optional.of(role("ADMIN_DEALER")));

        List<Organization> out = service.filterOrganizations(Arrays.asList(alloy, plant), "dealer");
        assertEquals(1, out.size());
        assertEquals(2, out.get(0).getId());
    }

    @Test
    void assertCanCreateOrUpdateUserAccount_nonAdminAlloy_cannotAssignAlloyRoles() {
        UserAccount actor = new UserAccount();
        actor.setUserRoleId(40);
        actor.setOrganization(org(2, "X"));

        UserAccount target = new UserAccount();
        target.setUserRoleId(1);

        UserRole alloyRole = role("USER_ALLOY");
        alloyRole.setId(1);

        when(userAccountRepository.findActiveByUserNameWithOrganization(eq("dealer"), eq(GeneralStatus.Deleted)))
                .thenReturn(Optional.of(actor));
        when(userRoleRepository.findById(40)).thenReturn(Optional.of(role("ADMIN_DEALER")));
        when(userRoleRepository.findById(1)).thenReturn(Optional.of(alloyRole));

        assertThrows(AccessDeniedException.class,
                () -> service.assertCanCreateOrUpdateUserAccount(target, "dealer", null));
    }

    @Test
    void assertCanAssignRoleName_nonAdminAlloy_cannotAssignAlloyRole() {
        UserAccount actor = new UserAccount();
        actor.setUserRoleId(40);

        when(userAccountRepository.findActiveByUserNameWithOrganization(eq("dealer"), eq(GeneralStatus.Deleted)))
                .thenReturn(Optional.of(actor));
        when(userRoleRepository.findById(40)).thenReturn(Optional.of(role("ADMIN_DEALER")));

        assertThrows(AccessDeniedException.class,
                () -> service.assertCanAssignRoleName("ADMIN_ALLOY", "dealer"));
    }

    private static UserRole role(String name) {
        UserRole r = new UserRole();
        r.setName(name);
        return r;
    }

    private static Organization org(int id, String name) {
        Organization o = new Organization();
        o.setId(id);
        o.setName(name);
        return o;
    }
}
