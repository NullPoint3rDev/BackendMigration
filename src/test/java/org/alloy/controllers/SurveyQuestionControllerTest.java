package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.MvcTestConfig;
import org.alloy.models.entities.Survey;
import org.alloy.models.entities.SurveyQuestion;
import org.alloy.services.SurveyQuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
 * Тесты для SurveyQuestionController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(MvcTestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(SurveyQuestionController.class)
@Import(MvcTestConfig.class)
@WithMockUser
public class SurveyQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private SurveyQuestionService surveyQuestionService;

    private SurveyQuestion testSurveyQuestion;
    private List<SurveyQuestion> testSurveyQuestions;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый вопрос опроса
        testSurveyQuestion = new SurveyQuestion();
        testSurveyQuestion.setId(1);
        testSurveyQuestion.setSurveyId(1);
        
        // Создаем связанную сущность опроса
        Survey survey = new Survey();
        survey.setId(1);
        testSurveyQuestion.setSurvey(survey);

        // Создаем второй тестовый вопрос опроса
        SurveyQuestion secondQuestion = new SurveyQuestion();
        secondQuestion.setId(2);
        secondQuestion.setSurveyId(1);
        secondQuestion.setSurvey(survey);

        // Создаем список тестовых вопросов опроса
        testSurveyQuestions = Arrays.asList(testSurveyQuestion, secondQuestion);
    }

    /**
     * Тест получения всех вопросов опроса
     */
    @Test
    void getAll_ShouldReturnListOfAllSurveyQuestions() throws Exception {
        when(surveyQuestionService.findAll()).thenReturn(testSurveyQuestions);

        mockMvc.perform(get("/survey-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].surveyId").value(1));

        verify(surveyQuestionService).findAll();
    }

    /**
     * Тест получения вопроса опроса по ID
     */
    @Test
    void getById_WhenSurveyQuestionExists_ShouldReturnSurveyQuestion() throws Exception {
        when(surveyQuestionService.findById(1)).thenReturn(Optional.of(testSurveyQuestion));

        mockMvc.perform(get("/survey-questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.id").value(1));

        verify(surveyQuestionService).findById(1);
    }

    /**
     * Тест получения несуществующего вопроса опроса по ID
     */
    @Test
    void getById_WhenSurveyQuestionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(surveyQuestionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/survey-questions/999"))
                .andExpect(status().isNotFound());

        verify(surveyQuestionService).findById(999);
    }

    /**
     * Тест создания нового вопроса опроса
     */
    @Test
    void create_ShouldCreateSurveyQuestion() throws Exception {
        when(surveyQuestionService.save(any(SurveyQuestion.class))).thenReturn(testSurveyQuestion);

        mockMvc.perform(post("/survey-questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testSurveyQuestion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.id").value(1));

        verify(surveyQuestionService).save(any(SurveyQuestion.class));
    }

    /**
     * Тест обновления существующего вопроса опроса
     */
    @Test
    void update_WhenSurveyQuestionExists_ShouldUpdateSurveyQuestion() throws Exception {
        when(surveyQuestionService.findById(1)).thenReturn(Optional.of(testSurveyQuestion));
        when(surveyQuestionService.save(any(SurveyQuestion.class))).thenReturn(testSurveyQuestion);

        mockMvc.perform(put("/survey-questions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testSurveyQuestion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.id").value(1));

        verify(surveyQuestionService).findById(1);
        verify(surveyQuestionService).save(any(SurveyQuestion.class));
    }

    /**
     * Тест обновления несуществующего вопроса опроса
     */
    @Test
    void update_WhenSurveyQuestionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(surveyQuestionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/survey-questions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testSurveyQuestion)))
                .andExpect(status().isNotFound());

        verify(surveyQuestionService).findById(999);
        verify(surveyQuestionService, never()).save(any(SurveyQuestion.class));
    }

    /**
     * Тест удаления существующего вопроса опроса
     */
    @Test
    void delete_WhenSurveyQuestionExists_ShouldDeleteSurveyQuestion() throws Exception {
        when(surveyQuestionService.findById(1)).thenReturn(Optional.of(testSurveyQuestion));
        doNothing().when(surveyQuestionService).deleteById(1);

        mockMvc.perform(delete("/survey-questions/1"))
                .andExpect(status().isNoContent());

        verify(surveyQuestionService).findById(1);
        verify(surveyQuestionService).deleteById(1);
    }

    /**
     * Тест удаления несуществующего вопроса опроса
     */
    @Test
    void delete_WhenSurveyQuestionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(surveyQuestionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/survey-questions/999"))
                .andExpect(status().isNotFound());

        verify(surveyQuestionService).findById(999);
        verify(surveyQuestionService, never()).deleteById(any(Integer.class));
    }
}
