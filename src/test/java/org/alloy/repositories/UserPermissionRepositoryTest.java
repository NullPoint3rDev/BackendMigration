package org.alloy.repositories;

import org.alloy.TestConfig;
import org.alloy.models.entities.UserPermission;
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
 * Тестовый класс для проверки функциональности UserPermissionRepository
 * Этот класс тестирует все методы репозитория для работы с разрешениями пользователей
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class UserPermissionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    private UserPermission testPermission;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает тестовое разрешение пользователя
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовое разрешение
        testPermission = new UserPermission();
        testPermission.setName("TEST_PERMISSION");
        
        // Сохраняем разрешение в базе данных
        testPermission = entityManager.persist(testPermission);
        entityManager.flush();
    }

    /**
     * Тест проверяет сохранение нового разрешения
     * Проверяет, что:
     * 1. Разрешение успешно сохраняется в базе данных
     * 2. После сохранения разрешение имеет корректный ID
     * 3. Все поля разрешения сохраняются правильно
     */
    @Test
    void save_ShouldSaveNewPermission() {
        // Создаем новое разрешение
        UserPermission newPermission = new UserPermission();
        newPermission.setName("NEW_PERMISSION");

        // Сохраняем разрешение
        UserPermission savedPermission = userPermissionRepository.save(newPermission);
        entityManager.flush();

        // Проверяем результаты
        assertNotNull(savedPermission.getId(), "ID разрешения не должен быть null");
        assertEquals("NEW_PERMISSION", savedPermission.getName(), "Имя разрешения должно совпадать");

        // Проверяем, что разрешение действительно сохранено в базе
        UserPermission foundPermission = entityManager.find(UserPermission.class, savedPermission.getId());
        assertNotNull(foundPermission, "Разрешение должно быть найдено в базе данных");
        assertEquals("NEW_PERMISSION", foundPermission.getName(), "Имя разрешения в базе должно совпадать");
    }

    /**
     * Тест проверяет поиск разрешения по ID
     * Проверяет, что:
     * 1. Разрешение успешно находится по существующему ID
     * 2. При поиске несуществующего разрешения возвращается пустой Optional
     */
    @Test
    void findById_ShouldReturnPermission() {
        // Ищем существующее разрешение
        Optional<UserPermission> foundPermission = userPermissionRepository.findById(testPermission.getId());
        
        // Проверяем результаты
        assertTrue(foundPermission.isPresent(), "Разрешение должно быть найдено");
        assertEquals(testPermission.getId(), foundPermission.get().getId(), "ID должен совпадать");
        assertEquals(testPermission.getName(), foundPermission.get().getName(), "Имя должно совпадать");

        // Проверяем поиск несуществующего разрешения
        Optional<UserPermission> notFoundPermission = userPermissionRepository.findById(999);
        assertFalse(notFoundPermission.isPresent(), "Несуществующее разрешение не должно быть найдено");
    }

    /**
     * Тест проверяет получение всех разрешений
     * Проверяет, что:
     * 1. Возвращается список всех разрешений
     * 2. Список содержит правильное количество разрешений
     * 3. Все разрешения имеют корректные данные
     */
    @Test
    void findAll_ShouldReturnAllPermissions() {
        // Создаем дополнительные разрешения
        UserPermission permission1 = new UserPermission();
        permission1.setName("PERMISSION_1");
        entityManager.persist(permission1);

        UserPermission permission2 = new UserPermission();
        permission2.setName("PERMISSION_2");
        entityManager.persist(permission2);

        entityManager.flush();

        // Получаем все разрешения
        List<UserPermission> permissions = userPermissionRepository.findAll();

        // Проверяем результаты
        assertNotNull(permissions, "Список разрешений не должен быть null");
        assertEquals(3, permissions.size(), "Должно быть найдено 3 разрешения");
        assertTrue(permissions.stream().anyMatch(p -> p.getName().equals("TEST_PERMISSION")), 
            "Должно быть найдено тестовое разрешение");
        assertTrue(permissions.stream().anyMatch(p -> p.getName().equals("PERMISSION_1")), 
            "Должно быть найдено разрешение 1");
        assertTrue(permissions.stream().anyMatch(p -> p.getName().equals("PERMISSION_2")), 
            "Должно быть найдено разрешение 2");
    }

    /**
     * Тест проверяет обновление существующего разрешения
     * Проверяет, что:
     * 1. Разрешение успешно обновляется
     * 2. Все поля обновляются корректно
     * 3. Обновленные данные сохраняются в базе
     */
    @Test
    void save_ShouldUpdateExistingPermission() {
        // Проверяем, что тестовое разрешение существует
        assertNotNull(testPermission.getId(), "ID тестового разрешения не должен быть null");
        
        // Изменяем имя разрешения
        String newName = "UPDATED_PERMISSION";
        testPermission.setName(newName);

        // Сохраняем изменения
        UserPermission updatedPermission = userPermissionRepository.save(testPermission);
        entityManager.flush();
        entityManager.clear();

        // Проверяем результаты
        assertNotNull(updatedPermission, "Обновленное разрешение не должно быть null");
        assertEquals(testPermission.getId(), updatedPermission.getId(), "ID не должен измениться");
        assertEquals(newName, updatedPermission.getName(), "Имя должно быть обновлено");

        // Проверяем, что изменения сохранились в базе
        UserPermission foundPermission = entityManager.find(UserPermission.class, testPermission.getId());
        assertNotNull(foundPermission, "Разрешение должно быть найдено в базе данных");
        assertEquals(newName, foundPermission.getName(), "Имя в базе данных должно быть обновлено");

        // Дополнительная проверка через репозиторий
        Optional<UserPermission> repositoryFoundPermission = userPermissionRepository.findById(testPermission.getId());
        assertTrue(repositoryFoundPermission.isPresent(), "Разрешение должно быть найдено через репозиторий");
        assertEquals(newName, repositoryFoundPermission.get().getName(), 
            "Имя должно быть обновлено в данных из репозитория");
    }

    /**
     * Тест проверяет удаление разрешения
     * Проверяет, что:
     * 1. Разрешение успешно удаляется
     * 2. После удаления разрешение не находится в базе
     */
    @Test
    void delete_ShouldRemovePermission() {
        // Удаляем разрешение
        userPermissionRepository.delete(testPermission);
        entityManager.flush();

        // Проверяем, что разрешение удалено
        UserPermission foundPermission = entityManager.find(UserPermission.class, testPermission.getId());
        assertNull(foundPermission, "Разрешение должно быть удалено из базы данных");

        // Проверяем, что разрешение не находится через репозиторий
        Optional<UserPermission> notFoundPermission = userPermissionRepository.findById(testPermission.getId());
        assertFalse(notFoundPermission.isPresent(), "Удаленное разрешение не должно быть найдено");
    }
}
