package org.alloy;

/**
 * @deprecated Используйте {@link AlloyWebMvcTest}.
 * Для {@code @DataJpaTest} не импортируйте этот класс — достаточно {@code @ActiveProfiles("test")}.
 */
@Deprecated
public class TestConfig extends MvcTestConfig {
}
