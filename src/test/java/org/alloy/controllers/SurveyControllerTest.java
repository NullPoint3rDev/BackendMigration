package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.AlloyWebMvcTest;
import org.alloy.models.entities.Survey;
import org.alloy.services.SurveyService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для SurveyController.
 * Использует @AlloyWebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(MvcTestConfig.class) импортирует конфигурацию для тестов.
 */
@AlloyWebMvcTest(SurveyController.class)
@WithMockUser
public class SurveyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private SurveyService surveyService;

    private Survey testSurvey;
    private List<Survey> testSurveys;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый опрос
        testSurvey = new Survey();
        testSurvey.setId(1);

        // Создаем второй тестовый опрос
        Survey secondSurvey = new Survey();
        secondSurvey.setId(2);

        // Создаем список тестовых опросов
        testSurveys = Arrays.asList(testSurvey, secondSurvey);
    }

    /**
     * Тест получения всех опросов
     */
    @Test
    void getAll_ShouldReturnListOfAllSurveys() throws Exception {
        when(surveyService.findAll()).thenReturn(testSurveys);

        mockMvc.perform(get("/surveys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(surveyService).findAll();
    }

    /**
     * Тест получения опроса по ID
     */
    @Test
    void getById_WhenSurveyExists_ShouldReturnSurvey() throws Exception {
        when(surveyService.findById(1)).thenReturn(Optional.of(testSurvey));

        mockMvc.perform(get("/surveys/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(surveyService).findById(1);
    }

    /**
     * Тест получения несуществующего опроса по ID
     */
    @Test
    void getById_WhenSurveyDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(surveyService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/surveys/999"))
                .andExpect(status().isNotFound());

        verify(surveyService).findById(999);
    }

    /**
     * Тест создания нового опроса
     */
    @Test
    void create_ShouldCreateSurvey() throws Exception {
        when(surveyService.save(any(Survey.class))).thenReturn(testSurvey);

        mockMvc.perform(post("/surveys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testSurvey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(surveyService).save(any(Survey.class));
    }

    /**
     * Тест обновления существующего опроса
     */
    @Test
    void update_WhenSurveyExists_ShouldUpdateSurvey() throws Exception {
        when(surveyService.save(any(Survey.class))).thenReturn(testSurvey);

        mockMvc.perform(put("/surveys/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testSurvey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(surveyService).save(any(Survey.class));
    }

    /**
     * Тест обновления несуществующего опроса
     */
    @Test
    void update_WhenSurveyDoesNotExist_ShouldStillSave() throws Exception {
        when(surveyService.save(any(Survey.class))).thenReturn(testSurvey);

        mockMvc.perform(put("/surveys/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testSurvey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(surveyService).save(any(Survey.class));
    }

    /**
     * Тест удаления существующего опроса
     */
    @Test
    void delete_WhenSurveyExists_ShouldDeleteSurvey() throws Exception {
        when(surveyService.findById(1)).thenReturn(Optional.of(testSurvey));
        doNothing().when(surveyService).deleteById(1);

        mockMvc.perform(delete("/surveys/1"))
                .andExpect(status().isNoContent());

        verify(surveyService).findById(1);
        verify(surveyService).deleteById(1);
    }

    /**
     * Тест удаления несуществующего опроса
     */
    @Test
    void delete_WhenSurveyDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(surveyService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/surveys/999"))
                .andExpect(status().isNotFound());

        verify(surveyService).findById(999);
        verify(surveyService, never()).deleteById(any(Integer.class));
    }
}
