package org.alloy.services;

import org.alloy.models.entities.UserToken;
import org.alloy.repositories.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
public class UserTokenServiceTest {

    @MockBean
    private UserTokenRepository userTokenRepository;

    private UserTokenService userTokenService;
    private UserToken testUserToken;
    private final UUID testToken = UUID.randomUUID();
    private final LocalDateTime testExpirationDate = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        userTokenService = new UserTokenService(userTokenRepository);
        
        // Создаем тестовый токен
        testUserToken = new UserToken();
        testUserToken.setId(1);
        testUserToken.setUserAccountId(100);
        testUserToken.setToken(testToken);
        testUserToken.setDateExpired(testExpirationDate);
        testUserToken.setType("AUTH");
        testUserToken.setUsed(false);
    }

    /**
     * Тест получения всех токенов пользователей
     * Проверяет корректность получения списка всех токенов
     */
    @Test
    void getAllUserTokens_ShouldReturnAllTokens() {
        // Подготавливаем тестовые данные
        List<UserToken> expectedTokens = Arrays.asList(testUserToken);
        when(userTokenRepository.findAll()).thenReturn(expectedTokens);

        // Вызываем тестируемый метод
        List<UserToken> result = userTokenService.getAllUserTokens();

        // Проверяем результаты
        assertNotNull(result, "Список токенов не должен быть null");
        assertEquals(expectedTokens.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testUserToken.getId(), result.get(0).getId(), "ID должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(userTokenRepository, times(1)).findAll();
    }

    /**
     * Тест получения токена по ID
     * Проверяет корректность получения токена по существующему ID
     */
    @Test
    void getUserTokenById_WhenExists_ShouldReturnToken() {
        // Подготавливаем тестовые данные
        when(userTokenRepository.findById(1)).thenReturn(Optional.of(testUserToken));

        // Вызываем тестируемый метод
        Optional<UserToken> result = userTokenService.getUserTokenById(1);

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать токен");
        assertEquals(testUserToken.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testUserToken.getToken(), result.get().getToken(), "Токен должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(userTokenRepository, times(1)).findById(1);
    }

    /**
     * Тест получения токенов по ID пользователя
     * Проверяет корректность получения всех токенов конкретного пользователя
     */
    @Test
    void getUserTokensByUserId_ShouldReturnUserTokens() {
        // Подготавливаем тестовые данные
        List<UserToken> expectedTokens = Arrays.asList(testUserToken);
        when(userTokenRepository.findByUserAccountId(100)).thenReturn(expectedTokens);

        // Вызываем тестируемый метод
        List<UserToken> result = userTokenService.getUserTokensByUserId(100);

        // Проверяем результаты
        assertNotNull(result, "Список токенов не должен быть null");
        assertEquals(expectedTokens.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testUserToken.getUserAccountId(), result.get(0).getUserAccountId(), "ID пользователя должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(userTokenRepository, times(1)).findByUserAccountId(100);
    }

    /**
     * Тест получения токена по значению токена
     * Проверяет корректность получения токена по его значению
     */
    @Test
    void getUserTokenByToken_WhenExists_ShouldReturnToken() {
        // Подготавливаем тестовые данные
        when(userTokenRepository.findByToken(testToken.toString())).thenReturn(Optional.of(testUserToken));

        // Вызываем тестируемый метод
        Optional<UserToken> result = userTokenService.getUserTokenByToken(testToken.toString());

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать токен");
        assertEquals(testToken, result.get().getToken(), "Значение токена должно совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(userTokenRepository, times(1)).findByToken(testToken.toString());
    }

    /**
     * Тест создания нового токена
     * Проверяет корректность создания нового токена
     */
    @Test
    void createUserToken_WithValidData_ShouldCreateToken() {
        // Подготавливаем тестовые данные
        when(userTokenRepository.findByToken(testToken.toString())).thenReturn(Optional.empty());
        when(userTokenRepository.save(any(UserToken.class))).thenReturn(testUserToken);

        // Вызываем тестируемый метод
        UserToken result = userTokenService.createUserToken(testUserToken);

        // Проверяем результаты
        assertNotNull(result, "Созданный токен не должен быть null");
        assertEquals(testUserToken.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testUserToken.getToken(), result.getToken(), "Токен должен совпадать");
        assertNotNull(result.getDateCreated(), "Дата создания должна быть установлена");
        
        // Проверяем, что методы репозитория были вызваны
        verify(userTokenRepository, times(1)).findByToken(testToken.toString());
        verify(userTokenRepository, times(1)).save(testUserToken);
    }

    /**
     * Тест создания токена с существующим значением
     * Проверяет, что создание токена с существующим значением вызывает исключение
     */
    @Test
    void createUserToken_WithExistingToken_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(userTokenRepository.findByToken(testToken.toString())).thenReturn(Optional.of(testUserToken));

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            userTokenService.createUserToken(testUserToken);
        }, "Должно быть выброшено исключение при попытке создать токен с существующим значением");
    }

    /**
     * Тест обновления токена
     * Проверяет корректность обновления существующего токена
     */
    @Test
    void updateUserToken_WithValidData_ShouldUpdateToken() {
        // Подготавливаем тестовые данные
        when(userTokenRepository.existsById(1)).thenReturn(true);
        when(userTokenRepository.findByToken(testToken.toString())).thenReturn(Optional.empty());
        when(userTokenRepository.save(any(UserToken.class))).thenReturn(testUserToken);

        // Вызываем тестируемый метод
        UserToken result = userTokenService.updateUserToken(testUserToken);

        // Проверяем результаты
        assertNotNull(result, "Обновленный токен не должен быть null");
        assertEquals(testUserToken.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testUserToken.getToken(), result.getToken(), "Токен должен совпадать");
        
        // Проверяем, что методы репозитория были вызваны
        verify(userTokenRepository, times(1)).existsById(1);
        verify(userTokenRepository, times(1)).findByToken(testToken.toString());
        verify(userTokenRepository, times(1)).save(testUserToken);
    }

    /**
     * Тест удаления токена
     * Проверяет корректность удаления существующего токена
     */
    @Test
    void deleteUserToken_WhenExists_ShouldDeleteToken() {
        // Подготавливаем тестовые данные
        when(userTokenRepository.existsById(1)).thenReturn(true);

        // Вызываем тестируемый метод
        userTokenService.deleteUserToken(1);

        // Проверяем, что методы репозитория были вызваны
        verify(userTokenRepository, times(1)).existsById(1);
        verify(userTokenRepository, times(1)).deleteById(1);
    }

    /**
     * Тест удаления всех токенов пользователя
     * Проверяет корректность удаления всех токенов конкретного пользователя
     */
    @Test
    void deleteAllUserTokens_ShouldDeleteAllUserTokens() {
        // Вызываем тестируемый метод
        userTokenService.deleteAllUserTokens(100);

        // Проверяем, что метод репозитория был вызван
        verify(userTokenRepository, times(1)).deleteByUserAccountId(100);
    }

    /**
     * Тест удаления истекших токенов
     * Проверяет корректность удаления всех истекших токенов
     */
    @Test
    void deleteExpiredTokens_ShouldDeleteExpiredTokens() {
        // Вызываем тестируемый метод
        userTokenService.deleteExpiredTokens();

        // Проверяем, что метод репозитория был вызван с текущей датой
        verify(userTokenRepository, times(1)).deleteByDateExpiredBefore(any(LocalDateTime.class));
    }
}
