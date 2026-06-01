package org.alloy;

import org.alloy.config.MethodSecurityTestConfig;
import org.alloy.metrics.BackendErrorMetrics;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Конфигурация для {@code @AlloyWebMvcTest}.
 * Не {@code @TestConfiguration} — иначе Boot подхватывает из родительского пакета {@code org.alloy} дважды.
 * {@link MethodSecurityTestConfig} включает {@code @PreAuthorize} вместо исключённого {@link org.alloy.config.SecurityConfig}.
 * {@link org.alloy.exception.GlobalExceptionHandler} поднимается {@code @WebMvcTest} как {@code @ControllerAdvice}.
 */
@Configuration
@Import(MethodSecurityTestConfig.class)
public class MvcTestConfig {

    @MockBean
    private BackendErrorMetrics backendErrorMetrics;
}
