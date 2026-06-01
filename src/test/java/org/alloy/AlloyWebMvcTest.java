package org.alloy;

import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @WebMvcTest} без JPA + {@link MvcTestConfig}.
 * Использование: {@code @AlloyWebMvcTest(SurveyController.class)}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(MvcTestConfig.class)
@TestPropertySource("classpath:mvc-test.properties")
public @interface AlloyWebMvcTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value();

    /** {@link WebMvcTest#excludeAutoConfiguration} → {@code ImportAutoConfiguration.exclude}. */
    @AliasFor(annotation = WebMvcTest.class, attribute = "excludeAutoConfiguration")
    Class<?>[] excludeAutoConfiguration() default {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class
    };
}
