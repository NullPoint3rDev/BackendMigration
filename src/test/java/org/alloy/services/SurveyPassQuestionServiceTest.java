package org.alloy.services;

import org.alloy.AlloyServiceTest;
import org.alloy.models.entities.SurveyPass;
import org.alloy.models.entities.SurveyPassQuestion;
import org.alloy.models.entities.SurveyQuestion;
import org.alloy.repositories.SurveyPassQuestionRepository;
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
 * Тестовый класс для SurveyPassQuestionService
 * Проверяет корректность работы сервиса вопросов прохождения опроса
 */
@AlloyServiceTest(SurveyPassQuestionService.class)
public class SurveyPassQuestionServiceTest {

    @MockBean
    private SurveyPassQuestionRepository surveyPassQuestionRepository;

    @Autowired
    private SurveyPassQuestionService surveyPassQuestionService;

    private SurveyPassQuestion testSurveyPassQuestion;
    private SurveyPass testSurveyPass;
    private SurveyQuestion testSurveyQuestion;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовые объекты SurveyPassQuestion, SurveyPass и SurveyQuestion с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        // Создание тестового прохождения опроса
        testSurveyPass = new SurveyPass();
        testSurveyPass.setId(1);

        // Создание тестового вопроса опроса
        testSurveyQuestion = new SurveyQuestion();
        testSurveyQuestion.setId(1);

        // Создание тестового вопроса прохождения опроса
        testSurveyPassQuestion = new SurveyPassQuestion();
        testSurveyPassQuestion.setId(1);
        testSurveyPassQuestion.setSurveyPassId(1);
        testSurveyPassQuestion.setSurveyQuestionId(1);
        testSurveyPassQuestion.setSurveyPass(testSurveyPass);
        testSurveyPassQuestion.setSurveyQuestion(testSurveyQuestion);
    }

    /**
     * Тест метода findAll()
     * Проверяет корректность получения всех вопросов прохождения опроса
     */
    @Test
    void findAll_ShouldReturnAllSurveyPassQuestions() {
        // Подготовка данных
        List<SurveyPassQuestion> expectedQuestions = Arrays.asList(testSurveyPassQuestion);
        when(surveyPassQuestionRepository.findAll()).thenReturn(expectedQuestions);

        // Выполнение теста
        List<SurveyPassQuestion> actualQuestions = surveyPassQuestionService.findAll();

        // Проверка результатов
        assertNotNull(actualQuestions);
        assertEquals(expectedQuestions.size(), actualQuestions.size());
        assertEquals(expectedQuestions.get(0).getId(), actualQuestions.get(0).getId());
        assertEquals(expectedQuestions.get(0).getSurveyPassId(), actualQuestions.get(0).getSurveyPassId());
        assertEquals(expectedQuestions.get(0).getSurveyQuestionId(), actualQuestions.get(0).getSurveyQuestionId());

        // Проверка вызова метода репозитория
        verify(surveyPassQuestionRepository, times(1)).findAll();
    }

    /**
     * Тест метода findById() с существующим ID
     * Проверяет корректность получения вопроса прохождения опроса по существующему ID
     */
    @Test
    void findById_WhenExists_ShouldReturnSurveyPassQuestion() {
        // Подготовка данных
        when(surveyPassQuestionRepository.findById(1)).thenReturn(Optional.of(testSurveyPassQuestion));

        // Выполнение теста
        Optional<SurveyPassQuestion> result = surveyPassQuestionService.findById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testSurveyPassQuestion.getId(), result.get().getId());
        assertEquals(testSurveyPassQuestion.getSurveyPassId(), result.get().getSurveyPassId());
        assertEquals(testSurveyPassQuestion.getSurveyQuestionId(), result.get().getSurveyQuestionId());
        assertEquals(testSurveyPassQuestion.getSurveyPass(), result.get().getSurveyPass());
        assertEquals(testSurveyPassQuestion.getSurveyQuestion(), result.get().getSurveyQuestion());

        // Проверка вызова метода репозитория
        verify(surveyPassQuestionRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() с несуществующим ID
     * Проверяет корректность получения пустого результата при поиске несуществующего вопроса
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготовка данных
        when(surveyPassQuestionRepository.findById(999)).thenReturn(Optional.empty());

        // Выполнение теста
        Optional<SurveyPassQuestion> result = surveyPassQuestionService.findById(999);

        // Проверка результатов
        assertFalse(result.isPresent());

        // Проверка вызова метода репозитория
        verify(surveyPassQuestionRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения нового вопроса прохождения опроса
     */
    @Test
    void save_ShouldSaveSurveyPassQuestion() {
        // Подготовка данных
        when(surveyPassQuestionRepository.save(any(SurveyPassQuestion.class))).thenReturn(testSurveyPassQuestion);

        // Выполнение теста
        SurveyPassQuestion savedQuestion = surveyPassQuestionService.save(testSurveyPassQuestion);

        // Проверка результатов
        assertNotNull(savedQuestion);
        assertEquals(testSurveyPassQuestion.getId(), savedQuestion.getId());
        assertEquals(testSurveyPassQuestion.getSurveyPassId(), savedQuestion.getSurveyPassId());
        assertEquals(testSurveyPassQuestion.getSurveyQuestionId(), savedQuestion.getSurveyQuestionId());
        assertEquals(testSurveyPassQuestion.getSurveyPass(), savedQuestion.getSurveyPass());
        assertEquals(testSurveyPassQuestion.getSurveyQuestion(), savedQuestion.getSurveyQuestion());

        // Проверка вызова метода репозитория
        verify(surveyPassQuestionRepository, times(1)).save(any(SurveyPassQuestion.class));
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления вопроса прохождения опроса по ID
     */
    @Test
    void deleteById_ShouldDeleteSurveyPassQuestion() {
        // Выполнение теста
        surveyPassQuestionService.deleteById(1);

        // Проверка вызова метода репозитория
        verify(surveyPassQuestionRepository, times(1)).deleteById(1);
    }
}
