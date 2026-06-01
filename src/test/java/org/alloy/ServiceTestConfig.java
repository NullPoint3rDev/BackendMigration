package org.alloy;

import org.springframework.context.annotation.Configuration;

/**
 * Маркер-конфигурация для {@link AlloyServiceTest} (при необходимости — общие {@code @MockBean}).
 * JPA отключается через {@code service-test.properties} на {@link AlloyServiceTest}.
 * Не {@code @TestConfiguration}, чтобы не импортироваться дважды из пакета {@code org.alloy}.
 */
@Configuration
public class ServiceTestConfig {
}
