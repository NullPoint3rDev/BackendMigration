package org.alloy;

import org.alloy.config.MethodSecurityTestConfig;
import org.alloy.exception.GlobalExceptionHandler;
import org.alloy.metrics.BackendErrorMetrics;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

/**
 * Минимальная конфигурация для {@code @AlloyWebMvcTest}: method security + exception handler.
 */
@TestConfiguration
@Import({MethodSecurityTestConfig.class, GlobalExceptionHandler.class})
public class MvcTestConfig {

    @MockBean
    private BackendErrorMetrics backendErrorMetrics;
}
