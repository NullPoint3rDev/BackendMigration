package org.alloy.services;

import org.alloy.ServiceTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.Survey;
import org.alloy.models.entities.SurveyQuestion;
import org.alloy.repositories.SurveyQuestionRepository;
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
 * Тестовый класс для SurveyQuestionService
 * Проверяет корректность работы сервиса вопросов опроса
 */
@SpringBootTest(classes = SurveyQuestionService.class)
@ActiveProfiles("test")
@Import(ServiceTestConfig.class)
public class SurveyQuestionServiceTest {

    @MockBean
    private SurveyQuestionRepository surveyQuestionRepository;

    @Autowired
    private SurveyQuestionService surveyQuestionService;

    private SurveyQuestion testSurveyQuestion;
    private Survey testSurvey;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовые объекты SurveyQuestion и Survey с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        // Создание тестового опроса
        testSurvey = new Survey();
        testSurvey.setId(1);

        // Создание тестового вопроса опроса
        testSurveyQuestion = new SurveyQuestion();
        testSurveyQuestion.setId(1);
        testSurveyQuestion.setSurveyId(1);
        testSurveyQuestion.setSurvey(testSurvey);
    }

    /**
     * Тест метода findAll()
     * Проверяет корректность получения всех вопросов опроса
     */
    @Test
    void findAll_ShouldReturnAllSurveyQuestions() {
        // Подготовка данных
        List<SurveyQuestion> expectedQuestions = Arrays.asList(testSurveyQuestion);
        when(surveyQuestionRepository.findAll()).thenReturn(expectedQuestions);

        // Выполнение теста
        List<SurveyQuestion> actualQuestions = surveyQuestionService.findAll();

        // Проверка результатов
        assertNotNull(actualQuestions);
        assertEquals(expectedQuestions.size(), actualQuestions.size());
        assertEquals(expectedQuestions.get(0).getId(), actualQuestions.get(0).getId());
        assertEquals(expectedQuestions.get(0).getSurveyId(), actualQuestions.get(0).getSurveyId());
        assertEquals(expectedQuestions.get(0).getSurvey(), actualQuestions.get(0).getSurvey());

        // Проверка вызова метода репозитория
        verify(surveyQuestionRepository, times(1)).findAll();
    }

    /**
     * Тест метода findById() с существующим ID
     * Проверяет корректность получения вопроса опроса по существующему ID
     */
    @Test
    void findById_WhenExists_ShouldReturnSurveyQuestion() {
        // Подготовка данных
        when(surveyQuestionRepository.findById(1)).thenReturn(Optional.of(testSurveyQuestion));

        // Выполнение теста
        Optional<SurveyQuestion> result = surveyQuestionService.findById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testSurveyQuestion.getId(), result.get().getId());
        assertEquals(testSurveyQuestion.getSurveyId(), result.get().getSurveyId());
        assertEquals(testSurveyQuestion.getSurvey(), result.get().getSurvey());

        // Проверка вызова метода репозитория
        verify(surveyQuestionRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() с несуществующим ID
     * Проверяет корректность получения пустого результата при поиске несуществующего вопроса
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготовка данных
        when(surveyQuestionRepository.findById(999)).thenReturn(Optional.empty());

        // Выполнение теста
        Optional<SurveyQuestion> result = surveyQuestionService.findById(999);

        // Проверка результатов
        assertFalse(result.isPresent());

        // Проверка вызова метода репозитория
        verify(surveyQuestionRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения нового вопроса опроса
     */
    @Test
    void save_ShouldSaveSurveyQuestion() {
        // Подготовка данных
        when(surveyQuestionRepository.save(any(SurveyQuestion.class))).thenReturn(testSurveyQuestion);

        // Выполнение теста
        SurveyQuestion savedQuestion = surveyQuestionService.save(testSurveyQuestion);

        // Проверка результатов
        assertNotNull(savedQuestion);
        assertEquals(testSurveyQuestion.getId(), savedQuestion.getId());
        assertEquals(testSurveyQuestion.getSurveyId(), savedQuestion.getSurveyId());
        assertEquals(testSurveyQuestion.getSurvey(), savedQuestion.getSurvey());

        // Проверка вызова метода репозитория
        verify(surveyQuestionRepository, times(1)).save(any(SurveyQuestion.class));
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления вопроса опроса по ID
     */
    @Test
    void deleteById_ShouldDeleteSurveyQuestion() {
        // Выполнение теста
        surveyQuestionService.deleteById(1);

        // Проверка вызова метода репозитория
        verify(surveyQuestionRepository, times(1)).deleteById(1);
    }
}
