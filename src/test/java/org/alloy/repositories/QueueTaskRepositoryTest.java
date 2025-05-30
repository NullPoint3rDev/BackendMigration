package org.alloy.repositories;

import org.alloy.models.entities.QueueTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности QueueTaskRepository
 * Этот класс тестирует основные операции CRUD для сущности QueueTask
 * QueueTask представляет собой задачу в очереди с различными параметрами выполнения
 */
@DataJpaTest
@ActiveProfiles("test")
public class QueueTaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QueueTaskRepository queueTaskRepository;

    private QueueTask testQueueTask;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает тестовый объект QueueTask с базовыми параметрами
     */
    @BeforeEach
    void setUp() {
        // Создаем новый тестовый объект QueueTask
        testQueueTask = new QueueTask();
        testQueueTask.setTaskName("Test Task");
        testQueueTask.setTaskParametersJson("{\"param1\": \"value1\"}");
        testQueueTask.setStatus(0); // Начальный статус
        testQueueTask.setPriority(1);
        testQueueTask.setScheduledOn(LocalDateTime.now().plusHours(1));
        
        // Сохраняем объект в тестовой базе данных
        testQueueTask = entityManager.persist(testQueueTask);
        entityManager.flush();
    }

    /**
     * Тест проверяет успешное сохранение нового объекта QueueTask
     * Проверяет, что:
     * 1. Объект успешно сохраняется
     * 2. ID объекта корректно генерируется
     * 3. Дата создания автоматически устанавливается
     * 4. Объект можно найти в базе данных
     */
    @Test
    void save_ShouldSaveQueueTask() {
        // Создаем новый объект для сохранения
        QueueTask newQueueTask = new QueueTask();
        newQueueTask.setTaskName("New Task");
        newQueueTask.setTaskParametersJson("{\"param2\": \"value2\"}");
        newQueueTask.setStatus(0);
        newQueueTask.setPriority(2);
        newQueueTask.setScheduledOn(LocalDateTime.now().plusHours(2));
        
        // Сохраняем объект через репозиторий
        QueueTask savedQueueTask = queueTaskRepository.save(newQueueTask);
        
        // Проверяем, что объект был сохранен
        assertNotNull(savedQueueTask, "Сохраненный объект не должен быть null");
        assertNotNull(savedQueueTask.getId(), "ID сохраненного объекта не должен быть null");
        assertNotNull(savedQueueTask.getDateCreated(), "Дата создания должна быть установлена");
        
        // Проверяем, что объект можно найти в базе данных
        Optional<QueueTask> foundQueueTask = queueTaskRepository.findById(savedQueueTask.getId());
        assertTrue(foundQueueTask.isPresent(), "Сохраненный объект должен быть найден в базе данных");
        assertEquals(savedQueueTask.getId(), foundQueueTask.get().getId(), 
            "ID найденного объекта должен совпадать с ID сохраненного объекта");
    }

    /**
     * Тест проверяет успешное получение объекта QueueTask по ID
     * Проверяет, что:
     * 1. Объект успешно находится по ID
     * 2. Найденный объект имеет правильные значения всех полей
     */
    @Test
    void findById_ShouldReturnQueueTask() {
        // Ищем объект по ID
        Optional<QueueTask> foundQueueTask = queueTaskRepository.findById(testQueueTask.getId());
        
        // Проверяем результаты
        assertTrue(foundQueueTask.isPresent(), "Объект должен быть найден");
        assertEquals(testQueueTask.getId(), foundQueueTask.get().getId(), 
            "ID найденного объекта должен совпадать с ID тестового объекта");
        assertEquals(testQueueTask.getTaskName(), foundQueueTask.get().getTaskName(),
            "Имя задачи должно совпадать");
        assertEquals(testQueueTask.getTaskParametersJson(), foundQueueTask.get().getTaskParametersJson(),
            "Параметры задачи должны совпадать");
        assertEquals(testQueueTask.getStatus(), foundQueueTask.get().getStatus(),
            "Статус задачи должен совпадать");
    }

    /**
     * Тест проверяет получение всех объектов QueueTask
     * Проверяет, что:
     * 1. Список всех объектов не пустой
     * 2. Тестовый объект присутствует в списке
     * 3. Все объекты имеют корректные значения полей
     */
    @Test
    void findAll_ShouldReturnAllQueueTasks() {
        // Получаем все объекты
        List<QueueTask> allQueueTasks = queueTaskRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(allQueueTasks, "Список объектов не должен быть null");
        assertFalse(allQueueTasks.isEmpty(), "Список объектов не должен быть пустым");
        assertTrue(allQueueTasks.stream()
                .anyMatch(task -> task.getId().equals(testQueueTask.getId())),
            "Список должен содержать тестовый объект");
        
        // Проверяем значения полей тестового объекта в списке
        QueueTask foundTask = allQueueTasks.stream()
            .filter(task -> task.getId().equals(testQueueTask.getId()))
            .findFirst()
            .orElse(null);
            
        assertNotNull(foundTask, "Тестовый объект должен быть найден в списке");
        assertEquals(testQueueTask.getTaskName(), foundTask.getTaskName(),
            "Имя задачи должно совпадать");
        assertEquals(testQueueTask.getPriority(), foundTask.getPriority(),
            "Приоритет задачи должен совпадать");
    }

    /**
     * Тест проверяет успешное удаление объекта QueueTask
     * Проверяет, что:
     * 1. Объект успешно удаляется
     * 2. После удаления объект нельзя найти в базе данных
     */
    @Test
    void delete_ShouldRemoveQueueTask() {
        // Удаляем тестовый объект
        queueTaskRepository.delete(testQueueTask);
        
        // Проверяем, что объект больше не существует в базе данных
        Optional<QueueTask> deletedQueueTask = queueTaskRepository.findById(testQueueTask.getId());
        assertFalse(deletedQueueTask.isPresent(), "Удаленный объект не должен быть найден в базе данных");
    }

    /**
     * Тест проверяет обновление статуса задачи
     * Проверяет, что:
     * 1. Статус задачи успешно обновляется
     * 2. Дата начала выполнения устанавливается при старте задачи
     * 3. Дата завершения устанавливается при завершении задачи
     */
    @Test
    void update_ShouldUpdateTaskStatus() {
        // Обновляем статус задачи
        testQueueTask.setStatus(1); // Статус "В процессе"
        testQueueTask.setDateStarted(LocalDateTime.now());
        QueueTask updatedTask = queueTaskRepository.save(testQueueTask);
        
        // Проверяем обновление
        assertNotNull(updatedTask.getDateStarted(), "Дата начала выполнения должна быть установлена");
        assertEquals(1, updatedTask.getStatus(), "Статус должен быть обновлен");
        
        // Завершаем задачу
        updatedTask.setStatus(2); // Статус "Завершено"
        updatedTask.setDateFinished(LocalDateTime.now());
        updatedTask.setStatusResult("Success");
        updatedTask.setStatusMessage("Task completed successfully");
        QueueTask finishedTask = queueTaskRepository.save(updatedTask);
        
        // Проверяем завершение
        assertNotNull(finishedTask.getDateFinished(), "Дата завершения должна быть установлена");
        assertEquals(2, finishedTask.getStatus(), "Статус должен быть обновлен на 'Завершено'");
        assertEquals("Success", finishedTask.getStatusResult(), "Результат должен быть установлен");
        assertEquals("Task completed successfully", finishedTask.getStatusMessage(), 
            "Сообщение о статусе должно быть установлено");
    }
}
