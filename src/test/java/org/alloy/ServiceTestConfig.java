package org.alloy;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Маркер-конфигурация для {@link AlloyServiceTest} (при необходимости — общие {@code @MockBean}).
 * JPA отключается через {@code service-test.properties} на {@link AlloyServiceTest}.
 */
@TestConfiguration
public class ServiceTestConfig {
}
