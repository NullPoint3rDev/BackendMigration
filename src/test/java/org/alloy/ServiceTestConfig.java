package org.alloy;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Подключать в {@code @SpringBootTest}: {@code @Import(ServiceTestConfig.class)}.
 */
@TestConfiguration
@TestPropertySource("classpath:service-test.properties")
public class ServiceTestConfig {
}
