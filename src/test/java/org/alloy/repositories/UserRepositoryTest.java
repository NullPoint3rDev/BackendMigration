package org.alloy.repositories;

import org.alloy.TestConfig;
import org.alloy.models.User;
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
 * Тестовый класс для проверки функциональности UserRepository
 * Этот класс тестирует все методы репозитория для работы с пользователями
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает тестового пользователя
     */
    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");

        // Сохраняем пользователя в базе данных
        testUser = entityManager.persist(testUser);
        entityManager.flush();
    }

    /**
     * Тест проверяет сохранение нового пользователя
     * Проверяет, что:
     * 1. Пользователь успешно сохраняется в базе данных
     * 2. После сохранения пользователь имеет корректный ID
     * 3. Все поля пользователя сохраняются правильно
     */
    @Test
    void save_ShouldSaveNewUser() {
        // Создаем нового пользователя
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("newpassword");

        // Сохраняем пользователя
        User savedUser = userRepository.save(newUser);
        entityManager.flush();

        // Проверяем результаты
        assertNotNull(savedUser.getId(), "ID пользователя не должен быть null");
        assertEquals("newuser", savedUser.getUsername(), "Имя пользователя должно совпадать");
        assertEquals("new@example.com", savedUser.getEmail(), "Email должен совпадать");
        assertEquals("newpassword", savedUser.getPassword(), "Пароль должен совпадать");

        // Проверяем, что пользователь действительно сохранен в базе
        User foundUser = entityManager.find(User.class, savedUser.getId());
        assertNotNull(foundUser, "Пользователь должен быть найден в базе данных");
        assertEquals("newuser", foundUser.getUsername(), "Имя пользователя в базе должно совпадать");
    }

    /**
     * Тест проверяет поиск пользователя по имени пользователя
     * Проверяет, что:
     * 1. Пользователь успешно находится по существующему имени
     * 2. При поиске несуществующего пользователя возвращается null
     */
    @Test
    void findByUsername_ShouldReturnUser() {
        Optional<User> foundOpt = userRepository.findByUsername("testuser");
        assertTrue(foundOpt.isPresent(), "Пользователь должен быть найден");
        User foundUser = foundOpt.get();
        assertEquals(testUser.getId(), foundUser.getId(), "ID должен совпадать");
        assertEquals("testuser", foundUser.getUsername(), "Имя пользователя должно совпадать");
        assertEquals("test@example.com", foundUser.getEmail(), "Email должен совпадать");

        assertTrue(userRepository.findByUsername("nonexistent").isEmpty(),
                "Несуществующий пользователь не должен быть найден");
    }

    /**
     * Тест проверяет поиск пользователя по email
     * Проверяет, что:
     * 1. Пользователь успешно находится по существующему email
     * 2. При поиске несуществующего email возвращается null
     */
    @Test
    void findByEmail_ShouldReturnUser() {
        Optional<User> foundOpt = userRepository.findByEmail("test@example.com");
        assertTrue(foundOpt.isPresent(), "Пользователь должен быть найден");
        User foundUser = foundOpt.get();
        assertEquals(testUser.getId(), foundUser.getId(), "ID должен совпадать");
        assertEquals("test@example.com", foundUser.getEmail(), "Email должен совпадать");
        assertEquals("testuser", foundUser.getUsername(), "Имя пользователя должно совпадать");

        assertTrue(userRepository.findByEmail("nonexistent@example.com").isEmpty(),
                "Пользователь с несуществующим email не должен быть найден");
    }

    /**
     * Тест проверяет получение всех пользователей
     * Проверяет, что:
     * 1. Возвращается список всех пользователей
     * 2. Список содержит правильное количество пользователей
     * 3. Все пользователи имеют корректные данные
     */
    @Test
    void findAll_ShouldReturnAllUsers() {
        // Создаем дополнительных пользователей
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword("password1");
        entityManager.persist(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword("password2");
        entityManager.persist(user2);

        entityManager.flush();

        // Получаем всех пользователей
        List<User> users = userRepository.findAll();

        // Проверяем результаты
        assertNotNull(users, "Список пользователей не должен быть null");
        assertEquals(3, users.size(), "Должно быть найдено 3 пользователя");
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("testuser")),
                "Должен быть найден тестовый пользователь");
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user1")),
                "Должен быть найден пользователь 1");
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user2")),
                "Должен быть найден пользователь 2");
    }

    /**
     * Тест проверяет обновление существующего пользователя
     * Проверяет, что:
     * 1. Пользователь успешно обновляется
     * 2. Все поля обновляются корректно
     * 3. Обновленные данные сохраняются в базе
     */
    @Test
    void save_ShouldUpdateExistingUser() {
        // Проверяем, что тестовый пользователь существует
        assertNotNull(testUser.getId(), "ID тестового пользователя не должен быть null");

        // Изменяем данные пользователя
        String newEmail = "updated@example.com";
        String newPassword = "newpassword";
        testUser.setEmail(newEmail);
        testUser.setPassword(newPassword);

        // Сохраняем изменения
        User updatedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Проверяем результаты
        assertNotNull(updatedUser, "Обновленный пользователь не должен быть null");
        assertEquals(testUser.getId(), updatedUser.getId(), "ID не должен измениться");
        assertEquals(newEmail, updatedUser.getEmail(), "Email должен быть обновлен");
        assertEquals(newPassword, updatedUser.getPassword(), "Пароль должен быть обновлен");

        // Проверяем, что изменения сохранились в базе
        User foundUser = entityManager.find(User.class, testUser.getId());
        assertNotNull(foundUser, "Пользователь должен быть найден в базе данных");
        assertEquals(newEmail, foundUser.getEmail(), "Email в базе данных должен быть обновлен");
        assertEquals(newPassword, foundUser.getPassword(), "Пароль в базе данных должен быть обновлен");
    }

    /**
     * Тест проверяет удаление пользователя
     * Проверяет, что:
     * 1. Пользователь успешно удаляется
     * 2. После удаления пользователь не находится в базе
     */
    @Test
    void delete_ShouldRemoveUser() {
        // Удаляем пользователя
        userRepository.delete(testUser);
        entityManager.flush();

        // Проверяем, что пользователь удален
        User foundUser = entityManager.find(User.class, testUser.getId());
        assertNull(foundUser, "Пользователь должен быть удален из базы данных");

        assertTrue(userRepository.findByUsername(testUser.getUsername()).isEmpty(),
                "Удаленный пользователь не должен быть найден");
    }
}
