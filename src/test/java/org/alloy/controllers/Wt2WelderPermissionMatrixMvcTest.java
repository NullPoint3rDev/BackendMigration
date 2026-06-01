package org.alloy.controllers;

import org.alloy.MvcTestConfig;
import org.alloy.services.WelderService;
import org.alloy.services.Wt2AccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WelderController.class)
@Import(MvcTestConfig.class)
class Wt2WelderPermissionMatrixMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WelderService welderService;
    @MockBean
    private Wt2AccessService wt2AccessService;

    @BeforeEach
    void setup() {
        when(welderService.getAllWelders()).thenReturn(Collections.emptyList());
        when(wt2AccessService.filterWelders(any(), anyString())).thenReturn(Collections.emptyList());
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "denied"))
                .when(wt2AccessService).assertCanReadWelders(eq("u"));
    }

    static Stream<Arguments> listWeldersAccess() {
        return Stream.of(
                Arguments.of(userWithAuthority("PERMISSION_ADD_DELETE_EDIT_WELDERS"), status().isOk()),
                Arguments.of(userWithAuthority("PERMISSION_VIEW_EQUIPMENT_HISTORY_GRAPHS"), status().isOk()),
                Arguments.of(userWithAuthority("PERMISSION_BIND_WELDERS_TO_EQUIPMENT"), status().isOk()),
                Arguments.of(user(User.builder().username("u").password("p").roles("USER").build()), status().isForbidden())
        );
    }

    @ParameterizedTest
    @MethodSource("listWeldersAccess")
    void getAllWelders_matrixRelatedAuthorities(RequestPostProcessor auth, ResultMatcher expected) throws Exception {
        mockMvc.perform(get("/welders").with(auth))
                .andExpect(expected);
    }

    private static RequestPostProcessor userWithAuthority(String authority) {
        UserDetails ud = User.builder()
                .username("weldUser")
                .password("p")
                .authorities(authority)
                .build();
        return user(ud);
    }
}
