package org.alloy.controllers;

import org.alloy.AlloyWebMvcTest;
import org.alloy.repositories.UserPermissionGrantRepository;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API грантов настраиваемых прав («наст N») — доступ к списку грантов пользователя.
 */
@AlloyWebMvcTest(UserPermissionGrantController.class)
class UserPermissionGrantMatrixMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPermissionGrantRepository grantRepository;

    @BeforeEach
    void setup() {
        when(grantRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    }

    static Stream<Arguments> getGrantsAccess() {
        return Stream.of(
                Arguments.of(userWithAuthority("PERMISSION_CREATE_EDIT_USER_DEALER"), status().isOk()),
                Arguments.of(userWithRoles("ADMIN_ALLOY"), status().isOk()),
                Arguments.of(user(User.builder().username("u").password("p").roles("USER").build()), status().isForbidden())
        );
    }

    @ParameterizedTest
    @MethodSource("getGrantsAccess")
    void getGrantsForUser_requiresCreateOrEditPermissionOrAdminAlloy(RequestPostProcessor auth, ResultMatcher expected)
            throws Exception {
        mockMvc.perform(get("/user-permission-grants/user/1").with(auth))
                .andExpect(expected);
    }

    private static RequestPostProcessor userWithAuthority(String authority) {
        UserDetails ud = User.builder()
                .username("gUser")
                .password("p")
                .authorities(authority)
                .build();
        return user(ud);
    }

    private static RequestPostProcessor userWithRoles(String role) {
        UserDetails ud = User.builder()
                .username("roleUser")
                .password("p")
                .roles(role)
                .build();
        return user(ud);
    }
}
