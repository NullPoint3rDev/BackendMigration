package org.alloy;

import org.alloy.config.MethodSecurityTestConfig;
import org.alloy.metrics.BackendErrorMetrics;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Конфигурация для {@code @AlloyWebMvcTest} (только явный {@code @Import}, без {@code @TestConfiguration} —
 * иначе Boot подхватывает её дважды из родительского пакета {@code org.alloy} и дублирует method security).
 * {@link org.alloy.exception.GlobalExceptionHandler} поднимается самим {@code @WebMvcTest} как {@code @ControllerAdvice}.
 */
@Configuration
@Import(MethodSecurityTestConfig.class)
public class MvcTestConfig {

    @MockBean
    private BackendErrorMetrics backendErrorMetrics;
}
