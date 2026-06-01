package org.alloy.repositories;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности UserAccountRepository
 * Этот класс тестирует все методы репозитория для работы с учетными записями пользователей
 */
@DataJpaTest
@ActiveProfiles("test")
public class UserAccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount testUserAccount;
    private Organization testOrganization;
    private OrganizationUnit testOrganizationUnit;
    private UserRole testUserRole;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает тестовую учетную запись пользователя и связанные сущности
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую организацию
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setStatus(GeneralStatus.Active);
        testOrganization = entityManager.persist(testOrganization);

        // Создаем тестовое подразделение
        testOrganizationUnit = new OrganizationUnit();
        testOrganizationUnit.setName("Test Unit");
        testOrganizationUnit.setOrganizationId(testOrganization.getId());
        testOrganizationUnit.setStatus(GeneralStatus.Active);
        testOrganizationUnit = entityManager.persist(testOrganizationUnit);

        // Создаем тестовую роль пользователя
        testUserRole = new UserRole();
        testUserRole.setName("Test Role");
        testUserRole.setStatus(GeneralStatus.Active);
        testUserRole = entityManager.persist(testUserRole);

        // Создаем тестовую учетную запись
        testUserAccount = new UserAccount();
        testUserAccount.setUserName("testuser");
        testUserAccount.setName("Test User");
        testUserAccount.setEmail("test@example.com");
        testUserAccount.setOrganizationUnitId(testOrganizationUnit.getId());
        testUserAccount.setUserRoleId(testUserRole.getId());
        testUserAccount.setStatus(GeneralStatus.Active);
        testUserAccount.setPasswordHash(new byte[]{1, 2, 3});
        testUserAccount.setPasswordSalt("salt");
        testUserAccount.setFailedLoginsCount(0);
        testUserAccount.setAllowEmailNotifications(true);
        
        // Сохраняем тестовую учетную запись
        testUserAccount = entityManager.persist(testUserAccount);
        entityManager.flush();
    }

    /**
     * Тест проверяет успешное сохранение новой учетной записи
     * Проверяет, что:
     * 1. Учетная запись успешно сохраняется
     * 2. ID генерируется корректно
     * 3. Все поля сохраняются правильно
     */
    @Test
    void save_ShouldSaveUserAccount() {
        // Создаем новую учетную запись
        UserAccount newUserAccount = new UserAccount();
        newUserAccount.setUserName("newuser");
        newUserAccount.setName("New User");
        newUserAccount.setEmail("new@example.com");
        newUserAccount.setOrganizationUnitId(testOrganizationUnit.getId());
        newUserAccount.setUserRoleId(testUserRole.getId());
        newUserAccount.setStatus(GeneralStatus.Active);
        
        // Сохраняем учетную запись
        UserAccount savedUserAccount = userAccountRepository.save(newUserAccount);
        
        // Проверяем результаты
        assertNotNull(savedUserAccount, "Сохраненная учетная запись не должна быть null");
        assertNotNull(savedUserAccount.getId(), "ID сохраненной учетной записи не должен быть null");
        assertEquals("newuser", savedUserAccount.getUserName(), "Имя пользователя должно совпадать");
        assertEquals("New User", savedUserAccount.getName(), "Имя должно совпадать");
        assertEquals("new@example.com", savedUserAccount.getEmail(), "Email должен совпадать");
    }

    /**
     * Тест проверяет поиск учетной записи по имени пользователя
     * Проверяет, что:
     * 1. Учетная запись находится по имени пользователя
     * 2. Найденная учетная запись содержит правильные данные
     */
    @Test
    void findByUserName_ShouldReturnUserAccount() {
        // Ищем учетную запись по имени пользователя
        Optional<UserAccount> foundUserAccount = userAccountRepository.findByUserName("testuser");
        
        // Проверяем результаты
        assertTrue(foundUserAccount.isPresent(), "Учетная запись должна быть найдена");
        UserAccount found = foundUserAccount.get();
        assertEquals(testUserAccount.getId(), found.getId(), "ID должен совпадать");
        assertEquals("testuser", found.getUserName(), "Имя пользователя должно совпадать");
        assertEquals("test@example.com", found.getEmail(), "Email должен совпадать");
    }

    /**
     * Тест проверяет поиск учетной записи по email
     * Проверяет, что:
     * 1. Учетная запись находится по email
     * 2. Найденная учетная запись содержит правильные данные
     */
    @Test
    void findByEmail_ShouldReturnUserAccount() {
        // Ищем учетную запись по email
        Optional<UserAccount> foundUserAccount = userAccountRepository.findByEmail("test@example.com");
        
        // Проверяем результаты
        assertTrue(foundUserAccount.isPresent(), "Учетная запись должна быть найдена");
        UserAccount found = foundUserAccount.get();
        assertEquals(testUserAccount.getId(), found.getId(), "ID должен совпадать");
        assertEquals("testuser", found.getUserName(), "Имя пользователя должно совпадать");
        assertEquals("test@example.com", found.getEmail(), "Email должен совпадать");
    }

    /**
     * Тест проверяет поиск учетных записей по ID подразделения
     * Проверяет, что:
     * 1. Находятся все учетные записи для указанного подразделения
     * 2. Найденные учетные записи содержат правильные данные
     */
    @Test
    void findByOrganizationUnitId_ShouldReturnUserAccounts() {
        // Создаем еще одну учетную запись в том же подразделении
        UserAccount anotherUserAccount = new UserAccount();
        anotherUserAccount.setUserName("anotheruser");
        anotherUserAccount.setOrganizationUnitId(testOrganizationUnit.getId());
        anotherUserAccount.setUserRoleId(testUserRole.getId());
        anotherUserAccount.setStatus(GeneralStatus.Active);
        entityManager.persist(anotherUserAccount);
        entityManager.flush();

        // Ищем учетные записи по ID подразделения
        List<UserAccount> foundUserAccounts = userAccountRepository.findByOrganizationUnitId(testOrganizationUnit.getId());
        
        // Проверяем результаты
        assertNotNull(foundUserAccounts, "Список учетных записей не должен быть null");
        assertEquals(2, foundUserAccounts.size(), "Должно быть найдено 2 учетные записи");
        assertTrue(foundUserAccounts.stream().anyMatch(ua -> ua.getUserName().equals("testuser")), 
            "Должна быть найдена тестовая учетная запись");
        assertTrue(foundUserAccounts.stream().anyMatch(ua -> ua.getUserName().equals("anotheruser")), 
            "Должна быть найдена дополнительная учетная запись");
    }

    /**
     * Тест проверяет поиск учетных записей по ID роли пользователя
     * Проверяет, что:
     * 1. Находятся все учетные записи с указанной ролью
     * 2. Найденные учетные записи содержат правильные данные
     */
    @Test
    void findByUserRoleId_ShouldReturnUserAccounts() {
        // Создаем еще одну учетную запись с той же ролью
        UserAccount anotherUserAccount = new UserAccount();
        anotherUserAccount.setUserName("anotheruser");
        anotherUserAccount.setOrganizationUnitId(testOrganizationUnit.getId());
        anotherUserAccount.setUserRoleId(testUserRole.getId());
        anotherUserAccount.setStatus(GeneralStatus.Active);
        entityManager.persist(anotherUserAccount);
        entityManager.flush();

        // Ищем учетные записи по ID роли
        List<UserAccount> foundUserAccounts = userAccountRepository.findByUserRoleId(testUserRole.getId());
        
        // Проверяем результаты
        assertNotNull(foundUserAccounts, "Список учетных записей не должен быть null");
        assertEquals(2, foundUserAccounts.size(), "Должно быть найдено 2 учетные записи");
        assertTrue(foundUserAccounts.stream().anyMatch(ua -> ua.getUserName().equals("testuser")), 
            "Должна быть найдена тестовая учетная запись");
        assertTrue(foundUserAccounts.stream().anyMatch(ua -> ua.getUserName().equals("anotheruser")), 
            "Должна быть найдена дополнительная учетная запись");
    }

    /**
     * Тест проверяет поиск учетных записей по подразделению и поисковому запросу
     * Проверяет, что:
     * 1. Находятся все учетные записи, соответствующие критериям поиска
     * 2. Поиск работает по имени пользователя, имени и email
     */
    @Test
    void searchUserAccounts_ShouldReturnMatchingUserAccounts() {
        // Создаем дополнительные учетные записи для тестирования поиска
        UserAccount user1 = new UserAccount();
        user1.setUserName("john_doe");
        user1.setName("John Doe");
        user1.setEmail("john@example.com");
        user1.setOrganizationUnitId(testOrganizationUnit.getId());
        user1.setUserRoleId(testUserRole.getId());
        user1.setStatus(GeneralStatus.Active);
        entityManager.persist(user1);

        UserAccount user2 = new UserAccount();
        user2.setUserName("jane_smith");
        user2.setName("Jane Smith");
        user2.setEmail("jane@example.com");
        user2.setOrganizationUnitId(testOrganizationUnit.getId());
        user2.setUserRoleId(testUserRole.getId());
        user2.setStatus(GeneralStatus.Active);
        entityManager.persist(user2);

        entityManager.flush();

        // Тестируем поиск по имени пользователя
        List<UserAccount> searchResults = userAccountRepository.searchUserAccounts(testOrganizationUnit.getId(), "john");
        assertEquals(1, searchResults.size(), "Должен быть найден один пользователь");
        assertEquals("john_doe", searchResults.get(0).getUserName(), "Должен быть найден John Doe");

        // Тестируем поиск по имени
        searchResults = userAccountRepository.searchUserAccounts(testOrganizationUnit.getId(), "Smith");
        assertEquals(1, searchResults.size(), "Должен быть найден один пользователь");
        assertEquals("jane_smith", searchResults.get(0).getUserName(), "Должен быть найден Jane Smith");

        // Тестируем поиск по email
        searchResults = userAccountRepository.searchUserAccounts(testOrganizationUnit.getId(), "jane@example.com");
        assertEquals(1, searchResults.size(), "Должен быть найден один пользователь");
        assertEquals("jane_smith", searchResults.get(0).getUserName(), "Должен быть найден Jane Smith");
    }

    /**
     * Тест проверяет поиск учетных записей по списку ID
     * Проверяет, что:
     * 1. Находятся все учетные записи с указанными ID
     * 2. Найденные учетные записи содержат правильные данные
     */
    @Test
    void findByIds_ShouldReturnUserAccounts() {
        // Создаем еще одну учетную запись
        UserAccount anotherUserAccount = new UserAccount();
        anotherUserAccount.setUserName("anotheruser");
        anotherUserAccount.setOrganizationUnitId(testOrganizationUnit.getId());
        anotherUserAccount.setUserRoleId(testUserRole.getId());
        anotherUserAccount.setStatus(GeneralStatus.Active);
        anotherUserAccount = entityManager.persist(anotherUserAccount);
        entityManager.flush();

        // Ищем учетные записи по списку ID
        List<UserAccount> foundUserAccounts = userAccountRepository.findByIds(
            Arrays.asList(testUserAccount.getId(), anotherUserAccount.getId())
        );
        
        // Проверяем результаты
        assertNotNull(foundUserAccounts, "Список учетных записей не должен быть null");
        assertEquals(2, foundUserAccounts.size(), "Должно быть найдено 2 учетные записи");
        assertTrue(foundUserAccounts.stream().anyMatch(ua -> ua.getUserName().equals("testuser")), 
            "Должна быть найдена тестовая учетная запись");
        assertTrue(foundUserAccounts.stream().anyMatch(ua -> ua.getUserName().equals("anotheruser")), 
            "Должна быть найдена дополнительная учетная запись");
    }

    /**
     * Тест проверяет поиск учетной записи по имени пользователя и хешу пароля
     * Проверяет, что:
     * 1. Учетная запись находится по имени пользователя и хешу пароля
     * 2. Найденная учетная запись содержит правильные данные
     */
    @Test
    void findByUserNameAndPasswordHash_ShouldReturnUserAccount() {
        // Ищем учетную запись по имени пользователя и хешу пароля
        Optional<UserAccount> foundUserAccount = userAccountRepository.findByUserNameAndPasswordHash(
            "testuser", 
            new byte[]{1, 2, 3}
        );
        
        // Проверяем результаты
        assertTrue(foundUserAccount.isPresent(), "Учетная запись должна быть найдена");
        UserAccount found = foundUserAccount.get();
        assertEquals(testUserAccount.getId(), found.getId(), "ID должен совпадать");
        assertEquals("testuser", found.getUserName(), "Имя пользователя должно совпадать");
        assertArrayEquals(new byte[]{1, 2, 3}, found.getPasswordHash(), "Хеш пароля должен совпадать");
    }
}
