package org.alloy.repositories;

import org.alloy.models.entities.QueuePushEvent;
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
 * Тестовый класс для проверки функциональности QueuePushEventRepository
 * Этот класс тестирует основные операции CRUD для сущности QueuePushEvent
 */
@DataJpaTest
@ActiveProfiles("test")
public class QueuePushEventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QueuePushEventRepository queuePushEventRepository;

    private QueuePushEvent testQueuePushEvent;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает тестовый объект QueuePushEvent для использования в тестах
     */
    @BeforeEach
    void setUp() {
        // Создаем новый тестовый объект QueuePushEvent
        testQueuePushEvent = new QueuePushEvent();
        
        // Сохраняем объект в тестовой базе данных
        testQueuePushEvent = entityManager.persist(testQueuePushEvent);
        entityManager.flush();
    }

    /**
     * Тест проверяет успешное сохранение нового объекта QueuePushEvent
     * Проверяет, что:
     * 1. Объект успешно сохраняется
     * 2. ID объекта корректно генерируется
     * 3. Объект можно найти в базе данных
     */
    @Test
    void save_ShouldSaveQueuePushEvent() {
        // Создаем новый объект для сохранения
        QueuePushEvent newQueuePushEvent = new QueuePushEvent();
        
        // Сохраняем объект через репозиторий
        QueuePushEvent savedQueuePushEvent = queuePushEventRepository.save(newQueuePushEvent);
        
        // Проверяем, что объект был сохранен
        assertNotNull(savedQueuePushEvent, "Сохраненный объект не должен быть null");
        assertNotNull(savedQueuePushEvent.getId(), "ID сохраненного объекта не должен быть null");
        
        // Проверяем, что объект можно найти в базе данных
        Optional<QueuePushEvent> foundQueuePushEvent = queuePushEventRepository.findById(savedQueuePushEvent.getId());
        assertTrue(foundQueuePushEvent.isPresent(), "Сохраненный объект должен быть найден в базе данных");
        assertEquals(savedQueuePushEvent.getId(), foundQueuePushEvent.get().getId(), 
            "ID найденного объекта должен совпадать с ID сохраненного объекта");
    }

    /**
     * Тест проверяет успешное получение объекта QueuePushEvent по ID
     * Проверяет, что:
     * 1. Объект успешно находится по ID
     * 2. Найденный объект имеет правильный ID
     */
    @Test
    void findById_ShouldReturnQueuePushEvent() {
        // Ищем объект по ID
        Optional<QueuePushEvent> foundQueuePushEvent = queuePushEventRepository.findById(testQueuePushEvent.getId());
        
        // Проверяем результаты
        assertTrue(foundQueuePushEvent.isPresent(), "Объект должен быть найден");
        assertEquals(testQueuePushEvent.getId(), foundQueuePushEvent.get().getId(), 
            "ID найденного объекта должен совпадать с ID тестового объекта");
    }

    /**
     * Тест проверяет получение всех объектов QueuePushEvent
     * Проверяет, что:
     * 1. Список всех объектов не пустой
     * 2. Тестовый объект присутствует в списке
     */
    @Test
    void findAll_ShouldReturnAllQueuePushEvents() {
        // Получаем все объекты
        List<QueuePushEvent> allQueuePushEvents = queuePushEventRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(allQueuePushEvents, "Список объектов не должен быть null");
        assertFalse(allQueuePushEvents.isEmpty(), "Список объектов не должен быть пустым");
        assertTrue(allQueuePushEvents.stream()
                .anyMatch(event -> event.getId().equals(testQueuePushEvent.getId())),
            "Список должен содержать тестовый объект");
    }

    /**
     * Тест проверяет успешное удаление объекта QueuePushEvent
     * Проверяет, что:
     * 1. Объект успешно удаляется
     * 2. После удаления объект нельзя найти в базе данных
     */
    @Test
    void delete_ShouldRemoveQueuePushEvent() {
        // Удаляем тестовый объект
        queuePushEventRepository.delete(testQueuePushEvent);
        
        // Проверяем, что объект больше не существует в базе данных
        Optional<QueuePushEvent> deletedQueuePushEvent = queuePushEventRepository.findById(testQueuePushEvent.getId());
        assertFalse(deletedQueuePushEvent.isPresent(), "Удаленный объект не должен быть найден в базе данных");
    }
}
