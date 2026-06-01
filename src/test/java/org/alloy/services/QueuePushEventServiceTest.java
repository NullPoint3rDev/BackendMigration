package org.alloy.services;

import org.alloy.AlloyServiceTest;
import org.alloy.models.entities.QueuePushEvent;
import org.alloy.repositories.QueuePushEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для QueuePushEventService
 * Проверяет корректность работы сервиса событий очереди push-уведомлений
 */
@AlloyServiceTest(QueuePushEventService.class)
public class QueuePushEventServiceTest {

    @MockBean
    private QueuePushEventRepository queuePushEventRepository;

    @Autowired
    private QueuePushEventService queuePushEventService;

    private QueuePushEvent testQueuePushEvent;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект QueuePushEvent с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testQueuePushEvent = new QueuePushEvent();
        testQueuePushEvent.setId(1);
    }

    /**
     * Тест метода findAll()
     * Проверяет корректность получения всех событий очереди
     */
    @Test
    void findAll_ShouldReturnAllEvents() {
        // Подготовка данных
        List<QueuePushEvent> expectedEvents = Arrays.asList(testQueuePushEvent);
        when(queuePushEventRepository.findAll()).thenReturn(expectedEvents);

        // Выполнение теста
        List<QueuePushEvent> actualEvents = queuePushEventService.findAll();

        // Проверка результатов
        assertNotNull(actualEvents);
        assertEquals(expectedEvents.size(), actualEvents.size());
        assertEquals(expectedEvents.get(0).getId(), actualEvents.get(0).getId());

        // Проверка вызова метода репозитория
        verify(queuePushEventRepository, times(1)).findAll();
    }

    /**
     * Тест метода findById() с существующим ID
     * Проверяет корректность получения события по существующему ID
     */
    @Test
    void findById_WhenExists_ShouldReturnEvent() {
        // Подготовка данных
        when(queuePushEventRepository.findById(1)).thenReturn(Optional.of(testQueuePushEvent));

        // Выполнение теста
        Optional<QueuePushEvent> result = queuePushEventService.findById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testQueuePushEvent.getId(), result.get().getId());

        // Проверка вызова метода репозитория
        verify(queuePushEventRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() с несуществующим ID
     * Проверяет корректность получения пустого результата при поиске несуществующего события
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготовка данных
        when(queuePushEventRepository.findById(999)).thenReturn(Optional.empty());

        // Выполнение теста
        Optional<QueuePushEvent> result = queuePushEventService.findById(999);

        // Проверка результатов
        assertFalse(result.isPresent());

        // Проверка вызова метода репозитория
        verify(queuePushEventRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения нового события
     */
    @Test
    void save_ShouldSaveEvent() {
        // Подготовка данных
        when(queuePushEventRepository.save(any(QueuePushEvent.class))).thenReturn(testQueuePushEvent);

        // Выполнение теста
        QueuePushEvent savedEvent = queuePushEventService.save(testQueuePushEvent);

        // Проверка результатов
        assertNotNull(savedEvent);
        assertEquals(testQueuePushEvent.getId(), savedEvent.getId());

        // Проверка вызова метода репозитория
        verify(queuePushEventRepository, times(1)).save(any(QueuePushEvent.class));
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления события по ID
     */
    @Test
    void deleteById_ShouldDeleteEvent() {
        // Выполнение теста
        queuePushEventService.deleteById(1);

        // Проверка вызова метода репозитория
        verify(queuePushEventRepository, times(1)).deleteById(1);
    }
}
