package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.Translation;
import org.alloy.services.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для TranslationController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(TranslationController.class)
@Import(TestConfig.class)
@WithMockUser
public class TranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TranslationService translationService;

    private Translation testTranslation;
    private List<Translation> testTranslations;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый перевод
        testTranslation = new Translation();
        testTranslation.setId(1);
        testTranslation.setLang("ru");
        testTranslation.setTableName("UserAccount");
        testTranslation.setColumnName("name");
        testTranslation.setIdName("id");
        testTranslation.setIdValue("1");
        testTranslation.setValue("Тестовый перевод");

        // Создаем второй тестовый перевод
        Translation secondTranslation = new Translation();
        secondTranslation.setId(2);
        secondTranslation.setLang("en");
        secondTranslation.setTableName("UserAccount");
        secondTranslation.setColumnName("name");
        secondTranslation.setIdName("id");
        secondTranslation.setIdValue("1");
        secondTranslation.setValue("Test translation");

        // Создаем список тестовых переводов
        testTranslations = Arrays.asList(testTranslation, secondTranslation);
    }

    /**
     * Тест получения всех переводов с пагинацией
     */
    @Test
    void getAll_ShouldReturnPageOfTranslations() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Translation> translationPage = new PageImpl<>(testTranslations, pageable, testTranslations.size());
        
        when(translationService.findAll(any(Pageable.class))).thenReturn(translationPage);

        mockMvc.perform(get("/translations")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].lang").value("ru"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].lang").value("en"));

        verify(translationService).findAll(any(Pageable.class));
    }

    /**
     * Тест получения перевода по ID
     */
    @Test
    void getById_WhenTranslationExists_ShouldReturnTranslation() throws Exception {
        when(translationService.findById(1)).thenReturn(Optional.of(testTranslation));

        mockMvc.perform(get("/translations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lang").value("ru"))
                .andExpect(jsonPath("$.value").value("Тестовый перевод"));

        verify(translationService).findById(1);
    }

    /**
     * Тест получения несуществующего перевода по ID
     */
    @Test
    void getById_WhenTranslationDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(translationService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/translations/999"))
                .andExpect(status().isNotFound());

        verify(translationService).findById(999);
    }

    /**
     * Тест создания нового перевода
     */
    @Test
    void create_ShouldCreateTranslation() throws Exception {
        when(translationService.save(any(Translation.class))).thenReturn(testTranslation);

        mockMvc.perform(post("/translations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTranslation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lang").value("ru"))
                .andExpect(jsonPath("$.value").value("Тестовый перевод"));

        verify(translationService).save(any(Translation.class));
    }

    /**
     * Тест обновления существующего перевода
     */
    @Test
    void update_WhenTranslationExists_ShouldUpdateTranslation() throws Exception {
        when(translationService.findById(1)).thenReturn(Optional.of(testTranslation));
        when(translationService.save(any(Translation.class))).thenReturn(testTranslation);

        mockMvc.perform(put("/translations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTranslation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lang").value("ru"))
                .andExpect(jsonPath("$.value").value("Тестовый перевод"));

        verify(translationService).findById(1);
        verify(translationService).save(any(Translation.class));
    }

    /**
     * Тест обновления несуществующего перевода
     */
    @Test
    void update_WhenTranslationDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(translationService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/translations/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTranslation)))
                .andExpect(status().isNotFound());

        verify(translationService).findById(999);
        verify(translationService, never()).save(any(Translation.class));
    }

    /**
     * Тест удаления существующего перевода
     */
    @Test
    void delete_WhenTranslationExists_ShouldDeleteTranslation() throws Exception {
        when(translationService.findById(1)).thenReturn(Optional.of(testTranslation));
        doNothing().when(translationService).deleteById(1);

        mockMvc.perform(delete("/translations/1"))
                .andExpect(status().isOk());

        verify(translationService).findById(1);
        verify(translationService).deleteById(1);
    }

    /**
     * Тест удаления несуществующего перевода
     */
    @Test
    void delete_WhenTranslationDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(translationService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/translations/999"))
                .andExpect(status().isNotFound());

        verify(translationService).findById(999);
        verify(translationService, never()).deleteById(any(Integer.class));
    }
}
