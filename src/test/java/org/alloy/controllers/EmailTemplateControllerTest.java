package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.AlloyWebMvcTest;
import org.alloy.models.entities.EmailTemplate;
import org.alloy.services.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для EmailTemplateController.
 * Использует @AlloyWebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(MvcTestConfig.class) импортирует конфигурацию для тестов.
 */
@AlloyWebMvcTest(EmailTemplateController.class)
@WithMockUser
class EmailTemplateControllerTest {

    // MockMvc - основной инструмент для тестирования веб-слоя
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper для сериализации/десериализации JSON
    @Autowired
    private ObjectMapper objectMapper;

    // Мокаем EmailTemplateService, так как нам не нужна реальная работа с базой данных
    @MockBean
    private EmailTemplateService emailTemplateService;

    // Тестовые данные
    private EmailTemplate testEmailTemplate;
    private List<EmailTemplate> testEmailTemplates;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый шаблон
        testEmailTemplate = new EmailTemplate();
        testEmailTemplate.setId(1);

        // Создаем второй тестовый шаблон
        EmailTemplate secondTemplate = new EmailTemplate();
        secondTemplate.setId(2);

        // Создаем список тестовых шаблонов
        testEmailTemplates = Arrays.asList(testEmailTemplate, secondTemplate);
    }

    /**
     * Тест получения всех шаблонов
     */
    @Test
    void getAll_ShouldReturnAllTemplates() throws Exception {
        // Настраиваем мок для возврата списка шаблонов
        when(emailTemplateService.findAll()).thenReturn(testEmailTemplates);

        // Выполняем GET запрос на /api/email-templates
        mockMvc.perform(get("/email-templates"))
                // Проверяем статус ответа и содержимое JSON
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        // Проверяем, что сервис был вызван
        verify(emailTemplateService).findAll();
    }

    /**
     * Тест получения шаблона по существующему ID
     */
    @Test
    void getById_WhenTemplateExists_ShouldReturnTemplate() throws Exception {
        // Настраиваем мок для возврата шаблона
        when(emailTemplateService.findById(1)).thenReturn(Optional.of(testEmailTemplate));

        // Выполняем GET запрос на /api/email-templates/1
        mockMvc.perform(get("/email-templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(emailTemplateService).findById(1);
    }

    /**
     * Тест получения шаблона по несуществующему ID
     */
    @Test
    void getById_WhenTemplateDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Настраиваем мок для возврата пустого Optional
        when(emailTemplateService.findById(999)).thenReturn(Optional.empty());

        // Выполняем GET запрос на /api/email-templates/999
        mockMvc.perform(get("/email-templates/999"))
                .andExpect(status().isNotFound());

        verify(emailTemplateService).findById(999);
    }

    /**
     * Тест создания нового шаблона
     */
    @Test
    void create_ShouldCreateNewTemplate() throws Exception {
        // Настраиваем мок для сохранения шаблона
        when(emailTemplateService.save(any(EmailTemplate.class))).thenReturn(testEmailTemplate);

        // Выполняем POST запрос на /api/email-templates
        mockMvc.perform(post("/email-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmailTemplate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(emailTemplateService).save(any(EmailTemplate.class));
    }

    /**
     * Тест обновления существующего шаблона
     */
    @Test
    void update_WhenTemplateExists_ShouldUpdateTemplate() throws Exception {
        // Настраиваем мок для поиска и сохранения шаблона
        when(emailTemplateService.findById(1)).thenReturn(Optional.of(testEmailTemplate));
        when(emailTemplateService.save(any(EmailTemplate.class))).thenReturn(testEmailTemplate);

        // Выполняем PUT запрос на /api/email-templates/1
        mockMvc.perform(put("/email-templates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmailTemplate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(emailTemplateService).findById(1);
        verify(emailTemplateService).save(any(EmailTemplate.class));
    }

    /**
     * Тест обновления несуществующего шаблона
     */
    @Test
    void update_WhenTemplateDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Настраиваем мок для возврата пустого Optional
        when(emailTemplateService.findById(999)).thenReturn(Optional.empty());

        // Выполняем PUT запрос на /api/email-templates/999
        mockMvc.perform(put("/email-templates/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmailTemplate)))
                .andExpect(status().isNotFound());

        verify(emailTemplateService).findById(999);
        verify(emailTemplateService, never()).save(any(EmailTemplate.class));
    }

    /**
     * Тест удаления существующего шаблона
     */
    @Test
    void delete_WhenTemplateExists_ShouldDeleteTemplate() throws Exception {
        // Настраиваем мок для поиска шаблона
        when(emailTemplateService.findById(1)).thenReturn(Optional.of(testEmailTemplate));
        doNothing().when(emailTemplateService).deleteById(anyInt());

        // Выполняем DELETE запрос на /api/email-templates/1
        mockMvc.perform(delete("/email-templates/1"))
                .andExpect(status().isNoContent());

        verify(emailTemplateService).findById(1);
        verify(emailTemplateService).deleteById(1);
    }

    /**
     * Тест удаления несуществующего шаблона
     */
    @Test
    void delete_WhenTemplateDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Настраиваем мок для возврата пустого Optional
        when(emailTemplateService.findById(999)).thenReturn(Optional.empty());

        // Выполняем DELETE запрос на /api/email-templates/999
        mockMvc.perform(delete("/email-templates/999"))
                .andExpect(status().isNotFound());

        verify(emailTemplateService).findById(999);
        verify(emailTemplateService, never()).deleteById(anyInt());
    }
}
