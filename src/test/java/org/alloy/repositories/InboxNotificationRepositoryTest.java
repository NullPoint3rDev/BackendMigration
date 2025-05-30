package org.alloy.repositories;

import org.alloy.models.entities.InboxNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности InboxNotificationRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
public class InboxNotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InboxNotificationRepository inboxNotificationRepository;

    private InboxNotification testNotification;

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовый объект InboxNotification для использования в тестах
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый объект InboxNotification
        testNotification = new InboxNotification();
        
        // Сохраняем объект в тестовой базе данных
        testNotification = entityManager.persist(testNotification);
        entityManager.flush();
    }

    /**
     * Тест сохранения нового уведомления
     * Проверяет, что уведомление корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveInboxNotification() {
        // Создаем новое уведомление
        InboxNotification newNotification = new InboxNotification();
        
        // Сохраняем уведомление через репозиторий
        InboxNotification savedNotification = inboxNotificationRepository.save(newNotification);
        
        // Проверяем, что уведомление было сохранено
        assertNotNull(savedNotification, "Сохраненное уведомление не должно быть null");
        assertNotNull(savedNotification.getId(), "ID сохраненного уведомления не должен быть null");
        
        // Проверяем, что уведомление можно найти в базе данных
        InboxNotification foundNotification = entityManager.find(InboxNotification.class, savedNotification.getId());
        assertNotNull(foundNotification, "Уведомление должно быть найдено в базе данных");
    }

    /**
     * Тест поиска уведомления по ID
     * Проверяет, что метод findById возвращает корректное уведомление
     */
    @Test
    void findById_ShouldReturnNotification() {
        // Ищем уведомление по ID
        Optional<InboxNotification> foundNotification = inboxNotificationRepository.findById(testNotification.getId());
        
        // Проверяем результаты
        assertTrue(foundNotification.isPresent(), "Уведомление должно быть найдено");
        assertEquals(testNotification.getId(), foundNotification.get().getId(), 
            "ID найденного уведомления должен совпадать с ID тестового уведомления");
    }

    /**
     * Тест поиска несуществующего уведомления
     * Проверяет, что метод findById возвращает пустой Optional для несуществующего ID
     */
    @Test
    void findById_ShouldReturnEmptyForNonExistentId() {
        // Ищем уведомление с несуществующим ID
        Optional<InboxNotification> foundNotification = inboxNotificationRepository.findById(999);
        
        // Проверяем, что уведомление не найдено
        assertTrue(foundNotification.isEmpty(), "Уведомление не должно быть найдено");
    }

    /**
     * Тест получения всех уведомлений
     * Проверяет, что метод findAll возвращает список всех уведомлений
     */
    @Test
    void findAll_ShouldReturnAllNotifications() {
        // Создаем второе уведомление
        InboxNotification secondNotification = new InboxNotification();
        entityManager.persist(secondNotification);
        entityManager.flush();
        
        // Получаем все уведомления
        List<InboxNotification> notifications = inboxNotificationRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(notifications, "Список уведомлений не должен быть null");
        assertTrue(notifications.size() >= 2, "Должно быть найдено как минимум 2 уведомления");
        assertTrue(notifications.stream().anyMatch(n -> n.getId().equals(testNotification.getId())), 
            "Список должен содержать первое тестовое уведомление");
        assertTrue(notifications.stream().anyMatch(n -> n.getId().equals(secondNotification.getId())), 
            "Список должен содержать второе тестовое уведомление");
    }

    /**
     * Тест удаления уведомления
     * Проверяет, что метод delete корректно удаляет уведомление из базы данных
     */
    @Test
    void delete_ShouldRemoveNotification() {
        // Удаляем уведомление
        inboxNotificationRepository.delete(testNotification);
        
        // Проверяем, что уведомление было удалено
        InboxNotification deletedNotification = entityManager.find(InboxNotification.class, testNotification.getId());
        assertNull(deletedNotification, "Уведомление должно быть удалено из базы данных");
    }

    /**
     * Тест удаления уведомления по ID
     * Проверяет, что метод deleteById корректно удаляет уведомление по ID
     */
    @Test
    void deleteById_ShouldRemoveNotificationById() {
        // Удаляем уведомление по ID
        inboxNotificationRepository.deleteById(testNotification.getId());
        
        // Проверяем, что уведомление было удалено
        InboxNotification deletedNotification = entityManager.find(InboxNotification.class, testNotification.getId());
        assertNull(deletedNotification, "Уведомление должно быть удалено из базы данных");
    }

    /**
     * Тест проверки существования уведомления
     * Проверяет, что метод existsById корректно определяет наличие уведомления
     */
    @Test
    void existsById_ShouldReturnCorrectResult() {
        // Проверяем существование тестового уведомления
        boolean exists = inboxNotificationRepository.existsById(testNotification.getId());
        assertTrue(exists, "Тестовое уведомление должно существовать");
        
        // Проверяем существование несуществующего уведомления
        boolean notExists = inboxNotificationRepository.existsById(999);
        assertFalse(notExists, "Несуществующее уведомление не должно быть найдено");
    }

    /**
     * Тест подсчета уведомлений
     * Проверяет, что метод count возвращает корректное количество уведомлений
     */
    @Test
    void count_ShouldReturnCorrectCount() {
        // Создаем второе уведомление
        InboxNotification secondNotification = new InboxNotification();
        entityManager.persist(secondNotification);
        entityManager.flush();
        
        // Получаем количество уведомлений
        long count = inboxNotificationRepository.count();
        
        // Проверяем результат
        assertTrue(count >= 2, "Должно быть как минимум 2 уведомления");
    }
}
