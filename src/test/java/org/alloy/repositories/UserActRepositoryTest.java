package org.alloy.repositories;

import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserAct;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности UserActRepository
 * Этот класс тестирует все методы репозитория для работы с действиями пользователей
 */
@DataJpaTest
@ActiveProfiles("test")
public class UserActRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserActRepository userActRepository;

    private UserAccount testUserAccount;
    private UserAct testUserAct;
    private LocalDateTime testDate;
    private UserRole testUserRole;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает тестовую роль пользователя, учетную запись и тестовое действие
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую роль пользователя
        testUserRole = new UserRole();
        testUserRole.setName("Test Role");
        testUserRole.setDescription("Test Role Description");
        testUserRole.setStatus(GeneralStatus.Active);
        testUserRole = entityManager.persist(testUserRole);
        entityManager.flush();

        // Создаем тестовую учетную запись пользователя
        testUserAccount = new UserAccount();
        testUserAccount.setUserName("testuser");
        testUserAccount.setName("Test User");
        testUserAccount.setEmail("test@example.com");
        testUserAccount.setStatus(GeneralStatus.Active);
        testUserAccount.setUserRoleId(testUserRole.getId());
        testUserAccount.setUserRole(testUserRole);
        testUserAccount = entityManager.persist(testUserAccount);
        entityManager.flush();

        // Создаем тестовое действие пользователя
        testDate = LocalDateTime.now();
        testUserAct = new UserAct();
        testUserAct.setUserAccountId(testUserAccount.getId());
        testUserAct.setType("LOGIN");
        testUserAct.setDescription("User logged in");
        testUserAct.setIpAddress("127.0.0.1");
        testUserAct.setUserAgent("Test Browser");
        testUserAct.setDateCreated(testDate);
        
        // Сохраняем тестовое действие
        testUserAct = entityManager.persist(testUserAct);
        entityManager.flush();
    }

    /**
     * Тест проверяет поиск действий пользователя по ID учетной записи
     * Проверяет, что:
     * 1. Находятся все действия для указанной учетной записи
     * 2. Найденные действия содержат правильные данные
     */
    @Test
    void findByUserAccountId_ShouldReturnUserActs() {
        // Создаем еще одно действие для того же пользователя
        UserAct anotherAct = new UserAct();
        anotherAct.setUserAccountId(testUserAccount.getId());
        anotherAct.setType("LOGOUT");
        anotherAct.setDescription("User logged out");
        anotherAct.setDateCreated(testDate.plusHours(1));
        entityManager.persist(anotherAct);
        entityManager.flush();

        // Ищем действия по ID учетной записи
        List<UserAct> foundActs = userActRepository.findByUserAccountId(testUserAccount.getId());
        
        // Проверяем результаты
        assertNotNull(foundActs, "Список действий не должен быть null");
        assertEquals(2, foundActs.size(), "Должно быть найдено 2 действия");
        assertTrue(foundActs.stream().anyMatch(act -> act.getType().equals("LOGIN")), 
            "Должно быть найдено действие входа");
        assertTrue(foundActs.stream().anyMatch(act -> act.getType().equals("LOGOUT")), 
            "Должно быть найдено действие выхода");
    }

    /**
     * Тест проверяет поиск действий пользователя по ID учетной записи и типу
     * Проверяет, что:
     * 1. Находятся все действия указанного типа для указанной учетной записи
     * 2. Найденные действия содержат правильные данные
     */
    @Test
    void findByUserAccountIdAndType_ShouldReturnUserActs() {
        // Создаем еще одно действие того же типа
        UserAct anotherAct = new UserAct();
        anotherAct.setUserAccountId(testUserAccount.getId());
        anotherAct.setType("LOGIN");
        anotherAct.setDescription("User logged in again");
        anotherAct.setDateCreated(testDate.plusHours(1));
        entityManager.persist(anotherAct);
        entityManager.flush();

        // Ищем действия по ID учетной записи и типу
        List<UserAct> foundActs = userActRepository.findByUserAccountIdAndType(testUserAccount.getId(), "LOGIN");
        
        // Проверяем результаты
        assertNotNull(foundActs, "Список действий не должен быть null");
        assertEquals(2, foundActs.size(), "Должно быть найдено 2 действия");
        assertTrue(foundActs.stream().allMatch(act -> act.getType().equals("LOGIN")), 
            "Все найденные действия должны быть типа LOGIN");
    }

    /**
     * Тест проверяет поиск действий пользователя по ID учетной записи и диапазону дат
     * Проверяет, что:
     * 1. Находятся все действия в указанном диапазоне дат
     * 2. Найденные действия содержат правильные данные
     */
    @Test
    void findUserActsByUserAccountIdAndDateRange_ShouldReturnUserActs() {
        // Создаем действия в разных временных точках
        UserAct act1 = new UserAct();
        act1.setUserAccountId(testUserAccount.getId());
        act1.setType("ACTION1");
        act1.setDescription("Action 1");
        act1.setDateCreated(testDate.minusHours(1));
        entityManager.persist(act1);

        UserAct act2 = new UserAct();
        act2.setUserAccountId(testUserAccount.getId());
        act2.setType("ACTION2");
        act2.setDescription("Action 2");
        act2.setDateCreated(testDate.plusHours(1));
        entityManager.persist(act2);

        entityManager.flush();

        // Ищем действия в диапазоне дат
        LocalDateTime startDate = testDate.minusHours(2);
        LocalDateTime endDate = testDate.plusHours(2);
        List<UserAct> foundActs = userActRepository.findUserActsByUserAccountIdAndDateRange(
            testUserAccount.getId(), startDate, endDate);
        
        // Проверяем результаты
        assertNotNull(foundActs, "Список действий не должен быть null");
        assertEquals(3, foundActs.size(), "Должно быть найдено 3 действия");
        assertTrue(foundActs.stream().anyMatch(act -> act.getType().equals("LOGIN")), 
            "Должно быть найдено действие входа");
        assertTrue(foundActs.stream().anyMatch(act -> act.getType().equals("ACTION1")), 
            "Должно быть найдено действие 1");
        assertTrue(foundActs.stream().anyMatch(act -> act.getType().equals("ACTION2")), 
            "Должно быть найдено действие 2");
    }

    /**
     * Тест проверяет поиск действий пользователя по ID учетной записи, типу и диапазону дат
     * Проверяет, что:
     * 1. Находятся все действия указанного типа в указанном диапазоне дат
     * 2. Найденные действия содержат правильные данные
     */
    @Test
    void findUserActsByUserAccountIdAndTypeAndDateRange_ShouldReturnUserActs() {
        // Создаем действия разных типов в разных временных точках
        UserAct act1 = new UserAct();
        act1.setUserAccountId(testUserAccount.getId());
        act1.setType("LOGIN");
        act1.setDescription("Login 1");
        act1.setDateCreated(testDate.minusHours(1));
        entityManager.persist(act1);

        UserAct act2 = new UserAct();
        act2.setUserAccountId(testUserAccount.getId());
        act2.setType("LOGIN");
        act2.setDescription("Login 2");
        act2.setDateCreated(testDate.plusHours(1));
        entityManager.persist(act2);

        entityManager.flush();

        // Ищем действия по типу в диапазоне дат
        LocalDateTime startDate = testDate.minusHours(2);
        LocalDateTime endDate = testDate.plusHours(2);
        List<UserAct> foundActs = userActRepository.findUserActsByUserAccountIdAndTypeAndDateRange(
            testUserAccount.getId(), "LOGIN", startDate, endDate);
        
        // Проверяем результаты
        assertNotNull(foundActs, "Список действий не должен быть null");
        assertEquals(3, foundActs.size(), "Должно быть найдено 3 действия");
        assertTrue(foundActs.stream().allMatch(act -> act.getType().equals("LOGIN")), 
            "Все найденные действия должны быть типа LOGIN");
    }

    /**
     * Тест проверяет подсчет действий пользователя по ID учетной записи, типу и диапазону дат
     * Проверяет, что:
     * 1. Возвращается правильное количество действий
     * 2. Учитываются только действия указанного типа в указанном диапазоне дат
     */
    @Test
    void countUserActsByUserAccountIdAndTypeAndDateRange_ShouldReturnCorrectCount() {
        // Создаем действия разных типов в разных временных точках
        UserAct act1 = new UserAct();
        act1.setUserAccountId(testUserAccount.getId());
        act1.setType("LOGIN");
        act1.setDescription("Login 1");
        act1.setDateCreated(testDate.minusHours(1));
        entityManager.persist(act1);

        UserAct act2 = new UserAct();
        act2.setUserAccountId(testUserAccount.getId());
        act2.setType("LOGOUT");
        act2.setDescription("Logout 1");
        act2.setDateCreated(testDate.plusHours(1));
        entityManager.persist(act2);

        entityManager.flush();

        // Подсчитываем действия по типу в диапазоне дат
        LocalDateTime startDate = testDate.minusHours(2);
        LocalDateTime endDate = testDate.plusHours(2);
        long count = userActRepository.countUserActsByUserAccountIdAndTypeAndDateRange(
            testUserAccount.getId(), "LOGIN", startDate, endDate);
        
        // Проверяем результаты
        assertEquals(2, count, "Должно быть найдено 2 действия типа LOGIN");
    }

    /**
     * Тест проверяет удаление действий пользователя по ID учетной записи
     * Проверяет, что:
     * 1. Все действия пользователя удаляются
     * 2. После удаления действий они не находятся в базе данных
     */
    @Test
    void deleteByUserAccountId_ShouldDeleteAllUserActs() {
        // Создаем еще одно действие
        UserAct anotherAct = new UserAct();
        anotherAct.setUserAccountId(testUserAccount.getId());
        anotherAct.setType("LOGOUT");
        anotherAct.setDescription("User logged out");
        anotherAct.setDateCreated(testDate.plusHours(1));
        entityManager.persist(anotherAct);
        entityManager.flush();

        // Удаляем все действия пользователя
        userActRepository.deleteByUserAccountId(testUserAccount.getId());
        entityManager.flush();

        // Проверяем, что действия удалены
        List<UserAct> foundActs = userActRepository.findByUserAccountId(testUserAccount.getId());
        assertTrue(foundActs.isEmpty(), "Список действий должен быть пустым");
    }

    /**
     * Тест проверяет удаление действий по дате создания
     * Проверяет, что:
     * 1. Удаляются только действия, созданные до указанной даты
     * 2. Действия, созданные после указанной даты, остаются в базе данных
     */
    @Test
    void deleteByDateCreatedBefore_ShouldDeleteOldActs() {
        // Создаем действия в разных временных точках
        UserAct oldAct = new UserAct();
        oldAct.setUserAccountId(testUserAccount.getId());
        oldAct.setType("OLD");
        oldAct.setDescription("Old action");
        oldAct.setDateCreated(testDate.minusDays(1));
        oldAct.setIpAddress("127.0.0.1");
        oldAct.setUserAgent("Test Browser");
        oldAct.setEntityType("TEST");
        oldAct.setEntityId(1);
        entityManager.persist(oldAct);

        UserAct newAct = new UserAct();
        newAct.setUserAccountId(testUserAccount.getId());
        newAct.setType("NEW");
        newAct.setDescription("New action");
        newAct.setDateCreated(testDate.plusDays(1));
        newAct.setIpAddress("127.0.0.1");
        newAct.setUserAgent("Test Browser");
        newAct.setEntityType("TEST");
        newAct.setEntityId(2);
        entityManager.persist(newAct);

        // Убедимся, что все действия сохранены
        entityManager.flush();
        entityManager.clear();

        // Проверяем, что все действия созданы
        List<UserAct> beforeDelete = userActRepository.findByUserAccountId(testUserAccount.getId());
        assertEquals(3, beforeDelete.size(), "Должно быть 3 действия перед удалением");
        assertTrue(beforeDelete.stream().anyMatch(act -> act.getType().equals("LOGIN")), 
            "Должно быть действие входа");
        assertTrue(beforeDelete.stream().anyMatch(act -> act.getType().equals("OLD")), 
            "Должно быть старое действие");
        assertTrue(beforeDelete.stream().anyMatch(act -> act.getType().equals("NEW")), 
            "Должно быть новое действие");

        // Удаляем старые действия
        userActRepository.deleteByDateCreatedBefore(testDate);
        entityManager.flush();
        entityManager.clear();

        // Проверяем результаты
        List<UserAct> afterDelete = userActRepository.findByUserAccountId(testUserAccount.getId());
        assertEquals(2, afterDelete.size(), "Должно остаться 2 действия");
        
        // Проверяем, что остались только действия, созданные после testDate
        assertTrue(afterDelete.stream().allMatch(act -> 
            !act.getType().equals("OLD") && 
            (act.getType().equals("LOGIN") || act.getType().equals("NEW"))), 
            "Должны остаться только действия LOGIN и NEW");
        
        // Проверяем конкретные действия
        assertTrue(afterDelete.stream().anyMatch(act -> 
            act.getType().equals("LOGIN") && 
            act.getDateCreated().equals(testDate)), 
            "Должно остаться действие входа с текущей датой");
        
        assertTrue(afterDelete.stream().anyMatch(act -> 
            act.getType().equals("NEW") && 
            act.getDateCreated().equals(testDate.plusDays(1))), 
            "Должно остаться новое действие с датой +1 день");
    }
}
