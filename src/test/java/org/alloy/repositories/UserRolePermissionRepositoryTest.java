package org.alloy.repositories;

import org.alloy.TestConfig;
import org.alloy.models.entities.UserRole;
import org.alloy.models.entities.UserPermission;
import org.alloy.models.entities.UserRolePermission;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности UserRolePermissionRepository
 * Этот класс тестирует все методы репозитория для работы с разрешениями ролей пользователей
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class UserRolePermissionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRolePermissionRepository userRolePermissionRepository;

    private UserRole testUserRole;
    private UserPermission testUserPermission;
    private UserRolePermission testUserRolePermission;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает тестовую роль, разрешение и связь между ними
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую роль
        testUserRole = new UserRole();
        testUserRole.setName("TestRole");
        testUserRole.setDescription("Test Role Description");
        testUserRole.setStatus(GeneralStatus.Active);
        testUserRole = entityManager.persist(testUserRole);

        // Создаем тестовое разрешение
        testUserPermission = new UserPermission();
        testUserPermission.setName("TEST_PERMISSION");
        testUserPermission = entityManager.persist(testUserPermission);

        // Создаем связь между ролью и разрешением
        testUserRolePermission = new UserRolePermission();
        testUserRolePermission.setUserRoleId(testUserRole.getId());
        testUserRolePermission.setUserPermissionId(testUserPermission.getId());
        testUserRolePermission.setRead(true);
        testUserRolePermission.setWrite(false);
        
        // Сохраняем связь в базе данных
        testUserRolePermission = entityManager.persist(testUserRolePermission);
        entityManager.flush();
    }

    /**
     * Тест проверяет сохранение новой связи между ролью и разрешением
     * Проверяет, что:
     * 1. Связь успешно сохраняется в базе данных
     * 2. После сохранения связь имеет корректный ID
     * 3. Все поля связи сохраняются правильно
     */
    @Test
    void save_ShouldSaveNewUserRolePermission() {
        // Создаем новую связь
        UserRolePermission newUserRolePermission = new UserRolePermission();
        newUserRolePermission.setUserRoleId(testUserRole.getId());
        newUserRolePermission.setUserPermissionId(testUserPermission.getId());
        newUserRolePermission.setRead(true);
        newUserRolePermission.setWrite(true);

        // Сохраняем связь
        UserRolePermission savedUserRolePermission = userRolePermissionRepository.save(newUserRolePermission);
        entityManager.flush();
        entityManager.clear();

        // Проверяем результаты
        assertNotNull(savedUserRolePermission, "Сохраненная связь не должна быть null");
        assertNotNull(savedUserRolePermission.getId(), "ID связи не должен быть null");
        assertEquals(testUserRole.getId(), savedUserRolePermission.getUserRoleId(), "ID роли должен совпадать");
        assertEquals(testUserPermission.getId(), savedUserRolePermission.getUserPermissionId(), "ID разрешения должен совпадать");
        assertTrue(savedUserRolePermission.getRead(), "Флаг чтения должен быть true");
        assertTrue(savedUserRolePermission.getWrite(), "Флаг записи должен быть true");

        // Проверяем, что связь действительно сохранена в базе
        UserRolePermission foundUserRolePermission = entityManager.find(UserRolePermission.class, savedUserRolePermission.getId());
        assertNotNull(foundUserRolePermission, "Связь должна быть найдена в базе данных");
        assertEquals(testUserRole.getId(), foundUserRolePermission.getUserRoleId(), "ID роли в базе должен совпадать");
        assertEquals(testUserPermission.getId(), foundUserRolePermission.getUserPermissionId(), "ID разрешения в базе должен совпадать");
        assertTrue(foundUserRolePermission.getRead(), "Флаг чтения в базе должен быть true");
        assertTrue(foundUserRolePermission.getWrite(), "Флаг записи в базе должен быть true");

        // Дополнительная проверка через репозиторий
        Optional<UserRolePermission> foundByRepo = userRolePermissionRepository.findById(savedUserRolePermission.getId());
        assertTrue(foundByRepo.isPresent(), "Связь должна быть найдена через репозиторий");
        assertEquals(savedUserRolePermission.getId(), foundByRepo.get().getId(), "ID должен совпадать при поиске через репозиторий");
    }

    /**
     * Тест проверяет поиск связи по ID
     * Проверяет, что:
     * 1. Связь успешно находится по существующему ID
     * 2. При поиске несуществующей связи возвращается пустой Optional
     */
    @Test
    void findById_ShouldReturnUserRolePermission() {
        // Ищем существующую связь
        Optional<UserRolePermission> foundUserRolePermission = userRolePermissionRepository.findById(testUserRolePermission.getId());
        
        // Проверяем результаты
        assertTrue(foundUserRolePermission.isPresent(), "Связь должна быть найдена");
        assertEquals(testUserRolePermission.getId(), foundUserRolePermission.get().getId(), "ID должен совпадать");
        assertEquals(testUserRole.getId(), foundUserRolePermission.get().getUserRoleId(), "ID роли должен совпадать");
        assertEquals(testUserPermission.getId(), foundUserRolePermission.get().getUserPermissionId(), "ID разрешения должен совпадать");

        // Проверяем поиск несуществующей связи
        Optional<UserRolePermission> notFoundUserRolePermission = userRolePermissionRepository.findById(999);
        assertFalse(notFoundUserRolePermission.isPresent(), "Несуществующая связь не должна быть найдена");
    }

    /**
     * Тест проверяет получение всех связей
     * Проверяет, что:
     * 1. Возвращается список всех связей
     * 2. Список содержит правильное количество связей
     * 3. Все связи имеют корректные данные
     */
    @Test
    void findAll_ShouldReturnAllUserRolePermissions() {
        // Создаем дополнительные связи
        UserRolePermission userRolePermission1 = new UserRolePermission();
        userRolePermission1.setUserRoleId(testUserRole.getId());
        userRolePermission1.setUserPermissionId(testUserPermission.getId());
        userRolePermission1.setRead(true);
        userRolePermission1.setWrite(true);
        entityManager.persist(userRolePermission1);

        UserRolePermission userRolePermission2 = new UserRolePermission();
        userRolePermission2.setUserRoleId(testUserRole.getId());
        userRolePermission2.setUserPermissionId(testUserPermission.getId());
        userRolePermission2.setRead(false);
        userRolePermission2.setWrite(true);
        entityManager.persist(userRolePermission2);

        entityManager.flush();

        // Получаем все связи
        List<UserRolePermission> userRolePermissions = userRolePermissionRepository.findAll();

        // Проверяем результаты
        assertNotNull(userRolePermissions, "Список связей не должен быть null");
        assertEquals(3, userRolePermissions.size(), "Должно быть найдено 3 связи");
        assertTrue(userRolePermissions.stream().anyMatch(urp -> urp.getId().equals(testUserRolePermission.getId())), 
            "Должна быть найдена тестовая связь");
        assertTrue(userRolePermissions.stream().anyMatch(urp -> urp.getRead() && urp.getWrite()), 
            "Должна быть найдена связь с правами на чтение и запись");
        assertTrue(userRolePermissions.stream().anyMatch(urp -> !urp.getRead() && urp.getWrite()), 
            "Должна быть найдена связь только с правом на запись");
    }

    /**
     * Тест проверяет обновление существующей связи
     * Проверяет, что:
     * 1. Связь успешно обновляется
     * 2. Все поля обновляются корректно
     * 3. Обновленные данные сохраняются в базе
     */
    @Test
    void save_ShouldUpdateExistingUserRolePermission() {
        // Проверяем, что тестовая связь существует
        assertNotNull(testUserRolePermission.getId(), "ID тестовой связи не должен быть null");
        
        // Изменяем права доступа
        testUserRolePermission.setRead(false);
        testUserRolePermission.setWrite(true);

        // Сохраняем изменения
        UserRolePermission updatedUserRolePermission = userRolePermissionRepository.save(testUserRolePermission);
        entityManager.flush();
        entityManager.clear();

        // Проверяем результаты
        assertNotNull(updatedUserRolePermission, "Обновленная связь не должна быть null");
        assertEquals(testUserRolePermission.getId(), updatedUserRolePermission.getId(), "ID не должен измениться");
        assertFalse(updatedUserRolePermission.getRead(), "Флаг чтения должен быть false");
        assertTrue(updatedUserRolePermission.getWrite(), "Флаг записи должен быть true");

        // Проверяем, что изменения сохранились в базе
        UserRolePermission foundUserRolePermission = entityManager.find(UserRolePermission.class, testUserRolePermission.getId());
        assertNotNull(foundUserRolePermission, "Связь должна быть найдена в базе данных");
        assertFalse(foundUserRolePermission.getRead(), "Флаг чтения в базе данных должен быть false");
        assertTrue(foundUserRolePermission.getWrite(), "Флаг записи в базе данных должен быть true");

        // Дополнительная проверка через репозиторий
        Optional<UserRolePermission> foundByRepo = userRolePermissionRepository.findById(testUserRolePermission.getId());
        assertTrue(foundByRepo.isPresent(), "Связь должна быть найдена через репозиторий");
        assertFalse(foundByRepo.get().getRead(), "Флаг чтения должен быть false при поиске через репозиторий");
        assertTrue(foundByRepo.get().getWrite(), "Флаг записи должен быть true при поиске через репозиторий");
    }

    /**
     * Тест проверяет удаление связи
     * Проверяет, что:
     * 1. Связь успешно удаляется
     * 2. После удаления связь не находится в базе
     */
    @Test
    void delete_ShouldRemoveUserRolePermission() {
        // Удаляем связь
        userRolePermissionRepository.delete(testUserRolePermission);
        entityManager.flush();

        // Проверяем, что связь удалена
        UserRolePermission foundUserRolePermission = entityManager.find(UserRolePermission.class, testUserRolePermission.getId());
        assertNull(foundUserRolePermission, "Связь должна быть удалена из базы данных");

        // Проверяем, что связь не находится через репозиторий
        Optional<UserRolePermission> notFoundUserRolePermission = userRolePermissionRepository.findById(testUserRolePermission.getId());
        assertFalse(notFoundUserRolePermission.isPresent(), "Удаленная связь не должна быть найдена");
    }
}
