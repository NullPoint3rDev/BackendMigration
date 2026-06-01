package org.alloy;

import org.alloy.config.MethodSecurityTestConfig;
import org.alloy.exception.GlobalExceptionHandler;
import org.alloy.metrics.BackendErrorMetrics;
import org.alloy.security.JwtTokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Collections;

/**
 * Минимальная конфигурация для {@code @WebMvcTest}: security + JWT mock.
 * Не подменяет JPA-репозитории (в отличие от устаревшего {@link TestConfig}).
 */
@TestConfiguration
@Import({MethodSecurityTestConfig.class, GlobalExceptionHandler.class})
public class MvcTestConfig {

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private BackendErrorMetrics backendErrorMetrics;

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("test")
                .password("test")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(Collections.singletonList(user));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
