package org.alloy.services;

import org.alloy.AlloyServiceTest;
import org.alloy.models.entities.QueueTask;
import org.alloy.repositories.QueueTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для QueueTaskService
 * Проверяет корректность работы сервиса задач очереди
 */
@AlloyServiceTest(QueueTaskService.class)
public class QueueTaskServiceTest {

    @MockBean
    private QueueTaskRepository queueTaskRepository;

    @Autowired
    private QueueTaskService queueTaskService;

    private QueueTask testQueueTask;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект QueueTask с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testQueueTask = new QueueTask();
        testQueueTask.setId(1);
        testQueueTask.setDateCreated(LocalDateTime.now());
        testQueueTask.setStatus(0);
        testQueueTask.setUserAccountId(1);
        testQueueTask.setDateStarted(LocalDateTime.now());
        testQueueTask.setDateFinished(LocalDateTime.now());
        testQueueTask.setScheduledOn(LocalDateTime.now());
        testQueueTask.setTaskName("Test Task");
        testQueueTask.setTaskParametersJson("{\"param\": \"value\"}");
        testQueueTask.setStatusResult("SUCCESS");
        testQueueTask.setStatusMessage("Task completed successfully");
        testQueueTask.setPriority(1);
        testQueueTask.setTaskResultJson("{\"result\": \"success\"}");
        testQueueTask.setScheduleTaskId(1);
    }

    /**
     * Тест метода findAll()
     * Проверяет корректность получения всех задач очереди
     */
    @Test
    void findAll_ShouldReturnAllTasks() {
        // Подготовка данных
        List<QueueTask> expectedTasks = Arrays.asList(testQueueTask);
        when(queueTaskRepository.findAll()).thenReturn(expectedTasks);

        // Выполнение теста
        List<QueueTask> actualTasks = queueTaskService.findAll();

        // Проверка результатов
        assertNotNull(actualTasks);
        assertEquals(expectedTasks.size(), actualTasks.size());
        assertEquals(expectedTasks.get(0).getId(), actualTasks.get(0).getId());
        assertEquals(expectedTasks.get(0).getTaskName(), actualTasks.get(0).getTaskName());
        assertEquals(expectedTasks.get(0).getStatus(), actualTasks.get(0).getStatus());

        // Проверка вызова метода репозитория
        verify(queueTaskRepository, times(1)).findAll();
    }

    /**
     * Тест метода findById() с существующим ID
     * Проверяет корректность получения задачи по существующему ID
     */
    @Test
    void findById_WhenExists_ShouldReturnTask() {
        // Подготовка данных
        when(queueTaskRepository.findById(1)).thenReturn(Optional.of(testQueueTask));

        // Выполнение теста
        Optional<QueueTask> result = queueTaskService.findById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testQueueTask.getId(), result.get().getId());
        assertEquals(testQueueTask.getTaskName(), result.get().getTaskName());
        assertEquals(testQueueTask.getStatus(), result.get().getStatus());
        assertEquals(testQueueTask.getTaskParametersJson(), result.get().getTaskParametersJson());

        // Проверка вызова метода репозитория
        verify(queueTaskRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() с несуществующим ID
     * Проверяет корректность получения пустого результата при поиске несуществующей задачи
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготовка данных
        when(queueTaskRepository.findById(999)).thenReturn(Optional.empty());

        // Выполнение теста
        Optional<QueueTask> result = queueTaskService.findById(999);

        // Проверка результатов
        assertFalse(result.isPresent());

        // Проверка вызова метода репозитория
        verify(queueTaskRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения новой задачи
     */
    @Test
    void save_ShouldSaveTask() {
        // Подготовка данных
        when(queueTaskRepository.save(any(QueueTask.class))).thenReturn(testQueueTask);

        // Выполнение теста
        QueueTask savedTask = queueTaskService.save(testQueueTask);

        // Проверка результатов
        assertNotNull(savedTask);
        assertEquals(testQueueTask.getId(), savedTask.getId());
        assertEquals(testQueueTask.getTaskName(), savedTask.getTaskName());
        assertEquals(testQueueTask.getStatus(), savedTask.getStatus());
        assertEquals(testQueueTask.getTaskParametersJson(), savedTask.getTaskParametersJson());

        // Проверка вызова метода репозитория
        verify(queueTaskRepository, times(1)).save(any(QueueTask.class));
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления задачи по ID
     */
    @Test
    void deleteById_ShouldDeleteTask() {
        // Выполнение теста
        queueTaskService.deleteById(1);

        // Проверка вызова метода репозитория
        verify(queueTaskRepository, times(1)).deleteById(1);
    }
}
