package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.AlloyWebMvcTest;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.Organization;
import org.alloy.services.OrganizationService;
import org.alloy.services.UserAccountService;
import org.alloy.services.Wt2AccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.alloy.controllers.MethodSecurityRequestPostProcessors.authn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Параметризованные проверки {@link PreAuthorize} для организаций + сценарий гранта
 * {@code create_delete_enterprises} ({@link UserAccountService#hasAllowedUserAction}).
 */
@AlloyWebMvcTest(OrganizationController.class)
class Wt2OrganizationPermissionMatrixMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationService organizationService;
    @MockBean
    private Wt2AccessService wt2AccessService;
    // SpEL в контроллере ссылается на bean ровно как @userAccountService
    // Поэтому важно зафиксировать имя mock-бина.
    @MockBean(name = "userAccountService")
    private UserAccountService userAccountService;

    @BeforeEach
    void setup() {
        when(organizationService.getAllOrganizations()).thenReturn(Collections.emptyList());
        when(wt2AccessService.filterOrganizations(any(), any())).thenReturn(Collections.emptyList());
        // AccessDeniedException → 403 через GlobalExceptionHandler; ResponseStatusException даёт 500 в slice-тестах.
        doThrow(new AccessDeniedException("denied"))
                .when(wt2AccessService).assertCanReadOrganizations(eq("deniedOrgReader"));
        doThrow(new AccessDeniedException("denied"))
                .when(wt2AccessService).assertCanWriteOrganizations(eq("plain"));
    }

    @AfterEach
    void clearCtx() {
        MethodSecurityRequestPostProcessors.clear();
    }

    static Stream<Arguments> getAllOrganizationsAccess() {
        return Stream.of(
                Arguments.of(userWithAuthorities("PERMISSION_VISIBILITY_EDIT_DEALERS"), status().isOk()),
                Arguments.of(userWithRoles("ADMIN_ENTERPRISE"), status().isOk()),
                Arguments.of(userWithRoles("USER"), status().isForbidden())
        );
    }

    @ParameterizedTest
    @MethodSource("getAllOrganizationsAccess")
    void getAllOrganizations_respectsMethodSecurity(RequestPostProcessor auth, ResultMatcher expected) throws Exception {
        mockMvc.perform(get("/organizations").with(auth))
                .andExpect(expected);
    }

    @Test
    void createOrganization_adminAlloyRole_allowed() throws Exception {
        Organization created = minimalOrg(1);
        when(organizationService.createOrganization(any(Organization.class))).thenReturn(created);

        mockMvc.perform(post("/organizations")
                        .with(userWithRoles("ADMIN_ALLOY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalOrg(null))))
                .andExpect(status().isCreated());

        verify(organizationService).createOrganization(any(Organization.class));
    }

    @Test
    void createOrganization_grantCreateDeleteEnterprises_allowed() throws Exception {
        Organization created = minimalOrg(1);
        when(organizationService.createOrganization(any(Organization.class))).thenReturn(created);
        when(userAccountService.hasAllowedUserAction(eq("grantUser"), eq("create_delete_enterprises")))
                .thenReturn(true);

        mockMvc.perform(post("/organizations")
                        .with(authn(User.builder().username("grantUser").password("p").roles("USER_DEALER").build()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalOrg(null))))
                .andExpect(status().isCreated());
    }

    @Test
    void createOrganization_noGrant_forbidden() throws Exception {
        when(userAccountService.hasAllowedUserAction(eq("plain"), eq("create_delete_enterprises")))
                .thenReturn(false);

        mockMvc.perform(post("/organizations")
                        .with(authn(User.builder().username("plain").password("p").roles("USER_DEALER").build()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalOrg(null))))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor userWithAuthorities(String... authorities) {
        UserDetails ud = User.builder()
                .username("authUser")
                .password("p")
                .authorities(authorities)
                .build();
        return authn(ud);
    }

    private static RequestPostProcessor userWithRoles(String role) {
        // USER — отдельный principal, для которого в setUp застаблен deny на чтение организаций.
        String username = "USER".equals(role) ? "deniedOrgReader" : "roleUser";
        UserDetails ud = User.builder()
                .username(username)
                .password("p")
                .roles(role)
                .build();
        return authn(ud);
    }

    private static Organization minimalOrg(Integer id) {
        Organization o = new Organization();
        if (id != null) {
            o.setId(id);
        }
        o.setName("Org");
        o.setStatus(GeneralStatus.Active);
        return o;
    }
}
