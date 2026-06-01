package org.alloy.services;

import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.Alert;
import org.alloy.repositories.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для AlertService
 * Проверяет корректность работы сервиса оповещений
 */
@SpringBootTest(classes = AlertService.class)
@ActiveProfiles("test")
public class AlertServiceTest {

    @MockBean
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    private Alert testAlert;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект Alert для использования в тестах
     */
    @BeforeEach
    void setUp() {
        testAlert = new Alert();
        testAlert.setId(1);
    }

    /**
     * Тест метода findAll()
     * Проверяет корректность получения всех оповещений
     */
    @Test
    void findAll_ShouldReturnAllAlerts() {
        // Подготовка данных
        List<Alert> expectedAlerts = Arrays.asList(testAlert);
        when(alertRepository.findAll()).thenReturn(expectedAlerts);

        // Выполнение теста
        List<Alert> actualAlerts = alertService.findAll();

        // Проверка результатов
        assertNotNull(actualAlerts);
        assertEquals(expectedAlerts.size(), actualAlerts.size());
        assertEquals(expectedAlerts.get(0).getId(), actualAlerts.get(0).getId());

        // Проверка вызова метода репозитория
        verify(alertRepository, times(1)).findAll();
    }

    /**
     * Тест метода findById() с существующим ID
     * Проверяет корректность получения оповещения по существующему ID
     */
    @Test
    void findById_WhenExists_ShouldReturnAlert() {
        // Подготовка данных
        when(alertRepository.findById(1)).thenReturn(Optional.of(testAlert));

        // Выполнение теста
        Optional<Alert> result = alertService.findById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testAlert.getId(), result.get().getId());

        // Проверка вызова метода репозитория
        verify(alertRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() с несуществующим ID
     * Проверяет корректность обработки случая, когда оповещение не найдено
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготовка данных
        when(alertRepository.findById(999)).thenReturn(Optional.empty());

        // Выполнение теста
        Optional<Alert> result = alertService.findById(999);

        // Проверка результатов
        assertFalse(result.isPresent());

        // Проверка вызова метода репозитория
        verify(alertRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения оповещения
     */
    @Test
    void save_ShouldReturnSavedAlert() {
        // Подготовка данных
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Выполнение теста
        Alert savedAlert = alertService.save(testAlert);

        // Проверка результатов
        assertNotNull(savedAlert);
        assertEquals(testAlert.getId(), savedAlert.getId());

        // Проверка вызова метода репозитория
        verify(alertRepository, times(1)).save(testAlert);
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления оповещения
     */
    @Test
    void deleteById_ShouldDeleteAlert() {
        // Выполнение теста
        alertService.deleteById(1);

        // Проверка вызова метода репозитория
        verify(alertRepository, times(1)).deleteById(1);
    }
} 