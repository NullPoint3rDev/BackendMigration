package org.alloy.controllers;

import org.alloy.MvcTestConfig;
import org.alloy.models.dto.ReportTemplateDTO;
import org.alloy.services.ReportTemplateService;
import org.alloy.services.UserAccountService;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportTemplateController.class)
@Import(MvcTestConfig.class)
class Wt2ReportTemplatePermissionMatrixMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportTemplateService templateService;
    @MockBean
    private UserAccountService userAccountService;
    @MockBean
    private Wt2AccessService wt2AccessService;

    @BeforeEach
    void setup() {
        when(templateService.getAllActiveTemplates()).thenReturn(Collections.<ReportTemplateDTO>emptyList());
    }

    static Stream<Arguments> reportTemplatesAccess() {
        return Stream.of(
                Arguments.of(userWithAuthority("PERMISSION_WORK_WITH_REPORTS"), status().isOk()),
                Arguments.of(user(User.builder().username("u").password("p").roles("USER").build()), status().isForbidden())
        );
    }

    @ParameterizedTest
    @MethodSource("reportTemplatesAccess")
    void getAllTemplates_requiresWorkWithReports(RequestPostProcessor auth, ResultMatcher expected) throws Exception {
        mockMvc.perform(get("/report-templates").with(auth))
                .andExpect(expected);
    }

    private static RequestPostProcessor userWithAuthority(String authority) {
        UserDetails ud = User.builder()
                .username("repUser")
                .password("p")
                .authorities(authority)
                .build();
        return user(ud);
    }
}
