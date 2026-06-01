package org.alloy;

import org.alloy.metrics.BackendErrorMetrics;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

/**
 * Минимальная конфигурация для {@code @AlloyWebMvcTest}.
 * Не {@code @TestConfiguration} — иначе Boot подхватывает из родительского пакета {@code org.alloy} дважды.
 * Method security не импортируем: {@code @WebMvcTest} + security starter уже регистрируют {@code metaDataSourceAdvisor};
 * повторный {@link org.alloy.config.MethodSecurityTestConfig} даёт {@code BeanDefinitionOverrideException}.
 * {@link org.alloy.exception.GlobalExceptionHandler} поднимается {@code @WebMvcTest} как {@code @ControllerAdvice}.
 */
@Configuration
public class MvcTestConfig {

    @MockBean
    private BackendErrorMetrics backendErrorMetrics;
}
