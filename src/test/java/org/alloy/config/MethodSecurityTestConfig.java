package org.alloy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Включает {@link org.springframework.security.access.prepost.PreAuthorize} в срезе {@code @WebMvcTest}
 * без поднятия полного {@link SecurityConfig} (JWT-фильтр и т.д.).
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityTestConfig {
}
