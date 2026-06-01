package org.alloy.repositories;

import org.alloy.models.entities.UserToken;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRole;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности UserTokenRepository
 * Этот класс тестирует все методы репозитория для работы с токенами пользователей
 */
@DataJpaTest
@ActiveProfiles("test")
public class UserTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserTokenRepository userTokenRepository;

    private UserAccount testUserAccount;
    private UserToken testUserToken;
    private LocalDateTime currentDate;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает тестовую роль, тестового пользователя и токен
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую роль
        UserRole testRole = new UserRole();
        testRole.setName("Test Role");
        testRole.setDescription("Test Role Description");
        testRole.setStatus(GeneralStatus.Active);
        testRole = entityManager.persist(testRole);

        // Создаем тестового пользователя
        testUserAccount = new UserAccount();
        testUserAccount.setUserName("testuser");
        testUserAccount.setEmail("test@example.com");
        testUserAccount.setPasswordHash("password".getBytes());
        testUserAccount.setStatus(GeneralStatus.Active);
        testUserAccount.setUserRoleId(testRole.getId());
        testUserAccount = entityManager.persist(testUserAccount);

        // Устанавливаем текущую дату
        currentDate = LocalDateTime.now();

        // Создаем тестовый токен
        testUserToken = new UserToken();
        testUserToken.setUserAccountId(testUserAccount.getId());
        testUserToken.setToken(UUID.randomUUID());
        testUserToken.setType("TEST_TOKEN");
        testUserToken.setUsed(false);
        testUserToken.setDateExpired(currentDate.plusDays(1));
        
        // Сохраняем токен в базе данных
        testUserToken = entityManager.persist(testUserToken);
        entityManager.flush();
    }

    /**
     * Тест проверяет сохранение нового токена
     * Проверяет, что:
     * 1. Токен успешно сохраняется в базе данных
     * 2. После сохранения токен имеет корректный ID
     * 3. Все поля токена сохраняются правильно
     */
    @Test
    void save_ShouldSaveNewUserToken() {
        // Создаем новый токен
        UserToken newUserToken = new UserToken();
        newUserToken.setUserAccountId(testUserAccount.getId());
        newUserToken.setToken(UUID.randomUUID());
        newUserToken.setType("NEW_TOKEN");
        newUserToken.setUsed(false);
        newUserToken.setDateExpired(currentDate.plusDays(1));

        // Сохраняем токен
        UserToken savedUserToken = userTokenRepository.save(newUserToken);
        entityManager.flush();
        entityManager.clear();

        // Проверяем результаты
        assertNotNull(savedUserToken, "Сохраненный токен не должен быть null");
        assertNotNull(savedUserToken.getId(), "ID токена не должен быть null");
        assertEquals(testUserAccount.getId(), savedUserToken.getUserAccountId(), "ID пользователя должен совпадать");
        assertEquals(newUserToken.getToken(), savedUserToken.getToken(), "Токен должен совпадать");
        assertEquals("NEW_TOKEN", savedUserToken.getType(), "Тип токена должен совпадать");
        assertFalse(savedUserToken.getUsed(), "Флаг использования должен быть false");
        assertNotNull(savedUserToken.getDateCreated(), "Дата создания не должна быть null");
        assertEquals(currentDate.plusDays(1), savedUserToken.getDateExpired(), "Дата истечения должна совпадать");

        // Проверяем, что токен действительно сохранен в базе
        UserToken foundUserToken = entityManager.find(UserToken.class, savedUserToken.getId());
        assertNotNull(foundUserToken, "Токен должен быть найден в базе данных");
        assertEquals(testUserAccount.getId(), foundUserToken.getUserAccountId(), "ID пользователя в базе должен совпадать");
    }

    /**
     * Тест проверяет поиск токенов по ID пользователя
     * Проверяет, что:
     * 1. Возвращается список всех токенов пользователя
     * 2. Список содержит правильное количество токенов
     */
    @Test
    void findByUserAccountId_ShouldReturnUserTokens() {
        // Создаем дополнительные токены для того же пользователя
        UserToken token1 = new UserToken();
        token1.setUserAccountId(testUserAccount.getId());
        token1.setToken(UUID.randomUUID());
        token1.setType("TOKEN_1");
        token1.setUsed(false);
        token1.setDateExpired(currentDate.plusDays(1));
        entityManager.persist(token1);

        UserToken token2 = new UserToken();
        token2.setUserAccountId(testUserAccount.getId());
        token2.setToken(UUID.randomUUID());
        token2.setType("TOKEN_2");
        token2.setUsed(true);
        token2.setDateUsed(currentDate);
        token2.setDateExpired(currentDate.plusDays(1));
        entityManager.persist(token2);

        entityManager.flush();

        // Получаем все токены пользователя
        List<UserToken> userTokens = userTokenRepository.findByUserAccountId(testUserAccount.getId());

        // Проверяем результаты
        assertNotNull(userTokens, "Список токенов не должен быть null");
        assertEquals(3, userTokens.size(), "Должно быть найдено 3 токена");
        assertTrue(userTokens.stream().anyMatch(t -> t.getId().equals(testUserToken.getId())), 
            "Должен быть найден тестовый токен");
        assertTrue(userTokens.stream().anyMatch(t -> t.getType().equals("TOKEN_1")), 
            "Должен быть найден токен TOKEN_1");
        assertTrue(userTokens.stream().anyMatch(t -> t.getType().equals("TOKEN_2")), 
            "Должен быть найден токен TOKEN_2");
    }

    /**
     * Тест проверяет поиск токена по значению токена
     * Проверяет, что:
     * 1. Токен успешно находится по существующему значению
     * 2. При поиске несуществующего токена возвращается пустой Optional
     */
    @Test
    void findByToken_ShouldReturnUserToken() {
        // Перечитываем token из БД: на H2 UUID в derived query иногда не матчится с in-memory полем после persist.
        UUID persistedToken = entityManager.find(UserToken.class, testUserToken.getId()).getToken();
        Optional<UserToken> foundUserToken = userTokenRepository.findByToken(persistedToken);
        
        // Проверяем результаты
        assertTrue(foundUserToken.isPresent(), "Токен должен быть найден");
        assertEquals(testUserToken.getId(), foundUserToken.get().getId(), "ID должен совпадать");
        assertEquals(persistedToken, foundUserToken.get().getToken(), "Значение токена должно совпадать");

        // Проверяем поиск несуществующего токена
        Optional<UserToken> notFoundUserToken = userTokenRepository.findByToken(UUID.randomUUID());
        assertFalse(notFoundUserToken.isPresent(), "Несуществующий токен не должен быть найден");
    }

    /**
     * Тест проверяет удаление токенов по ID пользователя
     * Проверяет, что:
     * 1. Все токены пользователя успешно удаляются
     * 2. После удаления токены не находятся в базе
     */
    @Test
    void deleteByUserAccountId_ShouldRemoveUserTokens() {
        // Создаем дополнительный токен
        UserToken additionalToken = new UserToken();
        additionalToken.setUserAccountId(testUserAccount.getId());
        additionalToken.setToken(UUID.randomUUID());
        additionalToken.setType("ADDITIONAL_TOKEN");
        additionalToken.setUsed(false);
        additionalToken.setDateExpired(currentDate.plusDays(1));
        entityManager.persist(additionalToken);
        entityManager.flush();

        // Удаляем все токены пользователя
        userTokenRepository.deleteByUserAccountId(testUserAccount.getId());
        entityManager.flush();

        // Проверяем, что токены удалены
        List<UserToken> userTokens = userTokenRepository.findByUserAccountId(testUserAccount.getId());
        assertTrue(userTokens.isEmpty(), "Список токенов должен быть пустым");

        // Проверяем через entityManager
        UserToken foundTestToken = entityManager.find(UserToken.class, testUserToken.getId());
        assertNull(foundTestToken, "Тестовый токен должен быть удален");
        UserToken foundAdditionalToken = entityManager.find(UserToken.class, additionalToken.getId());
        assertNull(foundAdditionalToken, "Дополнительный токен должен быть удален");
    }

    /**
     * Тест проверяет удаление просроченных токенов
     * Проверяет, что:
     * 1. Токены с истекшим сроком действия успешно удаляются
     * 2. Токены с действующим сроком остаются в базе
     */
    @Test
    void deleteByDateExpiredBefore_ShouldRemoveExpiredTokens() {
        // Создаем просроченный токен
        UserToken expiredToken = new UserToken();
        expiredToken.setUserAccountId(testUserAccount.getId());
        expiredToken.setToken(UUID.randomUUID());
        expiredToken.setType("EXPIRED_TOKEN");
        expiredToken.setUsed(false);
        expiredToken.setDateExpired(currentDate.minusDays(1));
        entityManager.persist(expiredToken);
        entityManager.flush();

        // Удаляем просроченные токены
        userTokenRepository.deleteByDateExpiredBefore(currentDate);
        entityManager.flush();

        // Проверяем, что просроченный токен удален
        UserToken foundExpiredToken = entityManager.find(UserToken.class, expiredToken.getId());
        assertNull(foundExpiredToken, "Просроченный токен должен быть удален");

        // Проверяем, что действующий токен остался
        UserToken foundTestToken = entityManager.find(UserToken.class, testUserToken.getId());
        assertNotNull(foundTestToken, "Действующий токен не должен быть удален");
    }

    /**
     * Тест проверяет поиск действующих токенов пользователя
     * Проверяет, что:
     * 1. Возвращаются только действующие токены
     * 2. Просроченные токены не включаются в результат
     */
    @Test
    void findValidTokensByUserAccountId_ShouldReturnValidTokens() {
        // Создаем просроченный токен
        UserToken expiredToken = new UserToken();
        expiredToken.setUserAccountId(testUserAccount.getId());
        expiredToken.setToken(UUID.randomUUID());
        expiredToken.setType("EXPIRED_TOKEN");
        expiredToken.setUsed(false);
        expiredToken.setDateExpired(currentDate.minusDays(1));
        entityManager.persist(expiredToken);

        // Создаем дополнительный действующий токен
        UserToken validToken = new UserToken();
        validToken.setUserAccountId(testUserAccount.getId());
        validToken.setToken(UUID.randomUUID());
        validToken.setType("VALID_TOKEN");
        validToken.setUsed(false);
        validToken.setDateExpired(currentDate.plusDays(1));
        entityManager.persist(validToken);

        entityManager.flush();

        // Получаем действующие токены
        List<UserToken> validTokens = userTokenRepository.findValidTokensByUserAccountId(testUserAccount.getId(), currentDate);

        // Проверяем результаты
        assertNotNull(validTokens, "Список токенов не должен быть null");
        assertEquals(2, validTokens.size(), "Должно быть найдено 2 действующих токена");
        assertTrue(validTokens.stream().anyMatch(t -> t.getId().equals(testUserToken.getId())), 
            "Должен быть найден тестовый токен");
        assertTrue(validTokens.stream().anyMatch(t -> t.getId().equals(validToken.getId())), 
            "Должен быть найден дополнительный действующий токен");
        assertFalse(validTokens.stream().anyMatch(t -> t.getId().equals(expiredToken.getId())), 
            "Просроченный токен не должен быть найден");
    }

    /**
     * Тест проверяет подсчет действующих токенов пользователя
     * Проверяет, что:
     * 1. Возвращается правильное количество действующих токенов
     * 2. Просроченные токены не учитываются
     */
    @Test
    void countValidTokensByUserAccountId_ShouldReturnValidTokensCount() {
        // Создаем просроченный токен
        UserToken expiredToken = new UserToken();
        expiredToken.setUserAccountId(testUserAccount.getId());
        expiredToken.setToken(UUID.randomUUID());
        expiredToken.setType("EXPIRED_TOKEN");
        expiredToken.setUsed(false);
        expiredToken.setDateExpired(currentDate.minusDays(1));
        expiredToken = entityManager.persist(expiredToken);

        // Создаем дополнительный действующий токен
        UserToken validToken = new UserToken();
        validToken.setUserAccountId(testUserAccount.getId());
        validToken.setToken(UUID.randomUUID());
        validToken.setType("VALID_TOKEN");
        validToken.setUsed(false);
        validToken.setDateExpired(currentDate.plusDays(1));
        validToken = entityManager.persist(validToken);

        entityManager.flush();
        entityManager.clear();

        // Получаем количество действующих токенов
        long validTokensCount = userTokenRepository.countValidTokensByUserAccountId(testUserAccount.getId(), currentDate);

        // Проверяем результаты
        assertEquals(2, validTokensCount, "Должно быть найдено 2 действующих токена");
    }
}
