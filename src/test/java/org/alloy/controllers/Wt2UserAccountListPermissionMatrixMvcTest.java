package org.alloy.controllers;

import org.alloy.AlloyWebMvcTest;
import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.UserRoleRepository;
import org.alloy.services.UserAccountService;
import org.alloy.services.Wt2AccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AlloyWebMvcTest(UserAccountController.class)
class Wt2UserAccountListPermissionMatrixMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccountService userAccountService;
    @MockBean
    private UserRoleRepository userRoleRepository;
    @MockBean
    private Wt2AccessService wt2AccessService;

    @BeforeEach
    void setup() {
        when(userAccountService.getAllUserAccounts()).thenReturn(Collections.<UserAccount>emptyList());
        when(wt2AccessService.filterUserAccounts(any(), anyString())).thenReturn(Collections.emptyList());
    }

    static Stream<Arguments> listUserAccountsAccess() {
        return Stream.of(
                Arguments.of(userWithAuthority("PERMISSION_CREATE_EDIT_USER_DEALER"), status().isOk()),
                Arguments.of(userWithRoles("ADMIN_ALLOY"), status().isOk()),
                Arguments.of(user(User.builder().username("u").password("p").roles("USER").build()), status().isForbidden())
        );
    }

    @ParameterizedTest
    @MethodSource("listUserAccountsAccess")
    void getAllUserAccounts_requiresCreateOrEditPermissionOrAdminAlloy(RequestPostProcessor auth, ResultMatcher expected)
            throws Exception {
        mockMvc.perform(get("/user-accounts").with(auth))
                .andExpect(expected);
    }

    private static RequestPostProcessor userWithAuthority(String authority) {
        UserDetails ud = User.builder()
                .username("uaUser")
                .password("p")
                .authorities(authority)
                .build();
        return user(ud);
    }

    private static RequestPostProcessor userWithRoles(String role) {
        UserDetails ud = User.builder()
                .username("uaRole")
                .password("p")
                .roles(role)
                .build();
        return user(ud);
    }
}
