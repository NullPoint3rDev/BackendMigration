package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.SurveyPass;
import org.alloy.models.entities.SurveyPassQuestion;
import org.alloy.models.entities.SurveyQuestion;
import org.alloy.services.SurveyPassQuestionService;
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
 * Тесты для SurveyPassQuestionController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(SurveyPassQuestionController.class)
@Import(TestConfig.class)
@WithMockUser
public class SurveyPassQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SurveyPassQuestionService surveyPassQuestionService;

    private SurveyPassQuestion testSurveyPassQuestion;
    private List<SurveyPassQuestion> testSurveyPassQuestions;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый вопрос прохождения опроса
        testSurveyPassQuestion = new SurveyPassQuestion();
        testSurveyPassQuestion.setId(1);
        testSurveyPassQuestion.setSurveyPassId(1);
        testSurveyPassQuestion.setSurveyQuestionId(1);
        
        // Создаем связанные сущности
        SurveyPass surveyPass = new SurveyPass();
        surveyPass.setId(1);
        testSurveyPassQuestion.setSurveyPass(surveyPass);
        
        SurveyQuestion surveyQuestion = new SurveyQuestion();
        surveyQuestion.setId(1);
        testSurveyPassQuestion.setSurveyQuestion(surveyQuestion);

        // Создаем второй тестовый вопрос прохождения опроса
        SurveyPassQuestion secondQuestion = new SurveyPassQuestion();
        secondQuestion.setId(2);
        secondQuestion.setSurveyPassId(1);
        secondQuestion.setSurveyQuestionId(2);
        
        SurveyQuestion secondSurveyQuestion = new SurveyQuestion();
        secondSurveyQuestion.setId(2);
        secondQuestion.setSurveyQuestion(secondSurveyQuestion);
        secondQuestion.setSurveyPass(surveyPass);

        // Создаем список тестовых вопросов прохождения опроса
        testSurveyPassQuestions = Arrays.asList(testSurveyPassQuestion, secondQuestion);
    }

    /**
     * Тест получения всех вопросов прохождения опроса
     */
    @Test
    void getAll_ShouldReturnListOfAllSurveyPassQuestions() throws Exception {
        when(surveyPassQuestionService.findAll()).thenReturn(testSurveyPassQuestions);

        mockMvc.perform(get("/api/survey-pass-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].surveyPassId").value(1))
                .andExpect(jsonPath("$[0].surveyQuestionId").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].surveyPassId").value(1))
                .andExpect(jsonPath("$[1].surveyQuestionId").value(2));

        verify(surveyPassQuestionService).findAll();
    }

    /**
     * Тест получения вопроса прохождения опроса по ID
     */
    @Test
    void getById_WhenSurveyPassQuestionExists_ShouldReturnSurveyPassQuestion() throws Exception {
        when(surveyPassQuestionService.findById(1)).thenReturn(Optional.of(testSurveyPassQuestion));

        mockMvc.perform(get("/api/survey-pass-questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.surveyPassId").value(1))
                .andExpect(jsonPath("$.surveyQuestionId").value(1));

        verify(surveyPassQuestionService).findById(1);
    }

    /**
     * Тест получения несуществующего вопроса прохождения опроса по ID
     */
    @Test
    void getById_WhenSurveyPassQuestionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(surveyPassQuestionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/survey-pass-questions/999"))
                .andExpect(status().isNotFound());

        verify(surveyPassQuestionService).findById(999);
    }

    /**
     * Тест создания нового вопроса прохождения опроса
     */
    @Test
    void create_ShouldCreateSurveyPassQuestion() throws Exception {
        when(surveyPassQuestionService.save(any(SurveyPassQuestion.class))).thenReturn(testSurveyPassQuestion);

        mockMvc.perform(post("/api/survey-pass-questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSurveyPassQuestion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.surveyPassId").value(1))
                .andExpect(jsonPath("$.surveyQuestionId").value(1));

        verify(surveyPassQuestionService).save(any(SurveyPassQuestion.class));
    }

    /**
     * Тест обновления существующего вопроса прохождения опроса
     */
    @Test
    void update_WhenSurveyPassQuestionExists_ShouldUpdateSurveyPassQuestion() throws Exception {
        when(surveyPassQuestionService.findById(1)).thenReturn(Optional.of(testSurveyPassQuestion));
        when(surveyPassQuestionService.save(any(SurveyPassQuestion.class))).thenReturn(testSurveyPassQuestion);

        mockMvc.perform(put("/api/survey-pass-questions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSurveyPassQuestion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.surveyPassId").value(1))
                .andExpect(jsonPath("$.surveyQuestionId").value(1));

        verify(surveyPassQuestionService).findById(1);
        verify(surveyPassQuestionService).save(any(SurveyPassQuestion.class));
    }

    /**
     * Тест обновления несуществующего вопроса прохождения опроса
     */
    @Test
    void update_WhenSurveyPassQuestionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(surveyPassQuestionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/survey-pass-questions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSurveyPassQuestion)))
                .andExpect(status().isNotFound());

        verify(surveyPassQuestionService).findById(999);
        verify(surveyPassQuestionService, never()).save(any(SurveyPassQuestion.class));
    }

    /**
     * Тест удаления существующего вопроса прохождения опроса
     */
    @Test
    void delete_WhenSurveyPassQuestionExists_ShouldDeleteSurveyPassQuestion() throws Exception {
        when(surveyPassQuestionService.findById(1)).thenReturn(Optional.of(testSurveyPassQuestion));
        doNothing().when(surveyPassQuestionService).deleteById(1);

        mockMvc.perform(delete("/api/survey-pass-questions/1"))
                .andExpect(status().isNoContent());

        verify(surveyPassQuestionService).findById(1);
        verify(surveyPassQuestionService).deleteById(1);
    }

    /**
     * Тест удаления несуществующего вопроса прохождения опроса
     */
    @Test
    void delete_WhenSurveyPassQuestionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(surveyPassQuestionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/survey-pass-questions/999"))
                .andExpect(status().isNotFound());

        verify(surveyPassQuestionService).findById(999);
        verify(surveyPassQuestionService, never()).deleteById(any(Integer.class));
    }
}
