package org.alloy;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SpringBootTest} только с одним сервисом, без JPA (exclude в service-test.properties).
 * Использование: {@code @AlloyServiceTest(OrganizationService.class)}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import(ServiceTestConfig.class)
@TestPropertySource("classpath:service-test.properties")
public @interface AlloyServiceTest {

    @AliasFor(annotation = SpringBootTest.class, attribute = "classes")
    Class<?>[] value();
}
