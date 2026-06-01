package org.alloy.services;

import org.alloy.ServiceTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.EmailTemplate;
import org.alloy.repositories.EmailTemplateRepository;
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
 * Тестовый класс для EmailTemplateService
 * Проверяет корректность работы сервиса шаблонов электронной почты
 */
@SpringBootTest(classes = EmailTemplateService.class)
@ActiveProfiles("test")
@Import(ServiceTestConfig.class)
public class EmailTemplateServiceTest {

    @MockBean
    private EmailTemplateRepository emailTemplateRepository;

    @Autowired
    private EmailTemplateService emailTemplateService;

    private EmailTemplate testEmailTemplate;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект EmailTemplate с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testEmailTemplate = new EmailTemplate();
        testEmailTemplate.setId(1);
    }

    /**
     * Тест метода findAll()
     * Проверяет корректность получения всех шаблонов электронной почты
     */
    @Test
    void findAll_ShouldReturnAllEmailTemplates() {
        // Подготовка данных
        List<EmailTemplate> expectedTemplates = Arrays.asList(testEmailTemplate);
        when(emailTemplateRepository.findAll()).thenReturn(expectedTemplates);

        // Выполнение теста
        List<EmailTemplate> actualTemplates = emailTemplateService.findAll();

        // Проверка результатов
        assertNotNull(actualTemplates);
        assertEquals(expectedTemplates.size(), actualTemplates.size());
        assertEquals(expectedTemplates.get(0).getId(), actualTemplates.get(0).getId());

        // Проверка вызова метода репозитория
        verify(emailTemplateRepository, times(1)).findAll();
    }

    /**
     * Тест метода findById() с существующим ID
     * Проверяет корректность получения шаблона по существующему ID
     */
    @Test
    void findById_WhenExists_ShouldReturnEmailTemplate() {
        // Подготовка данных
        when(emailTemplateRepository.findById(1)).thenReturn(Optional.of(testEmailTemplate));

        // Выполнение теста
        Optional<EmailTemplate> result = emailTemplateService.findById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testEmailTemplate.getId(), result.get().getId());

        // Проверка вызова метода репозитория
        verify(emailTemplateRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() с несуществующим ID
     * Проверяет корректность обработки случая, когда шаблон не найден
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготовка данных
        when(emailTemplateRepository.findById(999)).thenReturn(Optional.empty());

        // Выполнение теста
        Optional<EmailTemplate> result = emailTemplateService.findById(999);

        // Проверка результатов
        assertFalse(result.isPresent());

        // Проверка вызова метода репозитория
        verify(emailTemplateRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения шаблона
     */
    @Test
    void save_ShouldReturnSavedEmailTemplate() {
        // Подготовка данных
        when(emailTemplateRepository.save(any(EmailTemplate.class))).thenReturn(testEmailTemplate);

        // Выполнение теста
        EmailTemplate savedTemplate = emailTemplateService.save(testEmailTemplate);

        // Проверка результатов
        assertNotNull(savedTemplate);
        assertEquals(testEmailTemplate.getId(), savedTemplate.getId());

        // Проверка вызова метода репозитория
        verify(emailTemplateRepository, times(1)).save(testEmailTemplate);
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления шаблона
     */
    @Test
    void deleteById_ShouldDeleteEmailTemplate() {
        // Выполнение теста
        emailTemplateService.deleteById(1);

        // Проверка вызова метода репозитория
        verify(emailTemplateRepository, times(1)).deleteById(1);
    }
}
