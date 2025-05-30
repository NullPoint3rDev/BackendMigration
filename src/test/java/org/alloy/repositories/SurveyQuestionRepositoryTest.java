package org.alloy.repositories;

import org.alloy.models.entities.Survey;
import org.alloy.models.entities.SurveyQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности SurveyQuestionRepository
 * Этот класс тестирует основные операции CRUD для сущности SurveyQuestion
 * SurveyQuestion представляет собой вопрос в опросе (Survey)
 */
@DataJpaTest
@ActiveProfiles("test")
public class SurveyQuestionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    private SurveyQuestion testSurveyQuestion;
    private Survey testSurvey;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает необходимые тестовые объекты и связи между ними
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый опрос
        testSurvey = new Survey();
        testSurvey = entityManager.persist(testSurvey);

        // Создаем тестовый вопрос
        testSurveyQuestion = new SurveyQuestion();
        testSurveyQuestion.setSurveyId(testSurvey.getId());
        testSurveyQuestion = entityManager.persist(testSurveyQuestion);
        entityManager.flush();
    }

    /**
     * Тест проверяет успешное сохранение нового объекта SurveyQuestion
     * Проверяет, что:
     * 1. Объект успешно сохраняется
     * 2. ID объекта корректно генерируется
     * 3. Объект можно найти в базе данных
     */
    @Test
    void save_ShouldSaveSurveyQuestion() {
        // Создаем новый объект для сохранения
        SurveyQuestion newSurveyQuestion = new SurveyQuestion();
        newSurveyQuestion.setSurveyId(testSurvey.getId());
        
        // Сохраняем объект через репозиторий
        SurveyQuestion savedSurveyQuestion = surveyQuestionRepository.save(newSurveyQuestion);
        
        // Проверяем, что объект был сохранен
        assertNotNull(savedSurveyQuestion, "Сохраненный объект не должен быть null");
        assertNotNull(savedSurveyQuestion.getId(), "ID сохраненного объекта не должен быть null");
        
        // Проверяем, что объект можно найти в базе данных
        Optional<SurveyQuestion> foundSurveyQuestion = surveyQuestionRepository.findById(savedSurveyQuestion.getId());
        assertTrue(foundSurveyQuestion.isPresent(), "Сохраненный объект должен быть найден в базе данных");
        assertEquals(savedSurveyQuestion.getId(), foundSurveyQuestion.get().getId(), 
            "ID найденного объекта должен совпадать с ID сохраненного объекта");
    }

    /**
     * Тест проверяет успешное получение объекта SurveyQuestion по ID
     * Проверяет, что:
     * 1. Объект успешно находится по ID
     * 2. Найденный объект имеет правильные значения всех полей
     */
    @Test
    void findById_ShouldReturnSurveyQuestion() {
        // Ищем объект по ID
        Optional<SurveyQuestion> foundSurveyQuestion = surveyQuestionRepository.findById(testSurveyQuestion.getId());
        
        // Проверяем результаты
        assertTrue(foundSurveyQuestion.isPresent(), "Объект должен быть найден");
        assertEquals(testSurveyQuestion.getId(), foundSurveyQuestion.get().getId(), 
            "ID найденного объекта должен совпадать с ID тестового объекта");
        assertEquals(testSurveyQuestion.getSurveyId(), foundSurveyQuestion.get().getSurveyId(),
            "ID опроса должен совпадать");
    }

    /**
     * Тест проверяет получение всех объектов SurveyQuestion
     * Проверяет, что:
     * 1. Список всех объектов не пустой
     * 2. Тестовый объект присутствует в списке
     * 3. Все объекты имеют корректные значения полей
     */
    @Test
    void findAll_ShouldReturnAllSurveyQuestions() {
        // Получаем все объекты
        List<SurveyQuestion> allSurveyQuestions = surveyQuestionRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(allSurveyQuestions, "Список объектов не должен быть null");
        assertFalse(allSurveyQuestions.isEmpty(), "Список объектов не должен быть пустым");
        assertTrue(allSurveyQuestions.stream()
                .anyMatch(question -> question.getId().equals(testSurveyQuestion.getId())),
            "Список должен содержать тестовый объект");
        
        // Проверяем значения полей тестового объекта в списке
        SurveyQuestion foundQuestion = allSurveyQuestions.stream()
            .filter(question -> question.getId().equals(testSurveyQuestion.getId()))
            .findFirst()
            .orElse(null);
            
        assertNotNull(foundQuestion, "Тестовый объект должен быть найден в списке");
        assertEquals(testSurveyQuestion.getSurveyId(), foundQuestion.getSurveyId(),
            "ID опроса должен совпадать");
    }

    /**
     * Тест проверяет успешное удаление объекта SurveyQuestion
     * Проверяет, что:
     * 1. Объект успешно удаляется
     * 2. После удаления объект нельзя найти в базе данных
     */
    @Test
    void delete_ShouldRemoveSurveyQuestion() {
        // Удаляем тестовый объект
        surveyQuestionRepository.delete(testSurveyQuestion);
        
        // Проверяем, что объект больше не существует в базе данных
        Optional<SurveyQuestion> deletedSurveyQuestion = surveyQuestionRepository.findById(testSurveyQuestion.getId());
        assertFalse(deletedSurveyQuestion.isPresent(), "Удаленный объект не должен быть найден в базе данных");
    }

    /**
     * Тест проверяет получение всех вопросов для конкретного опроса
     * Проверяет, что:
     * 1. Находятся все вопросы, связанные с указанным опросом
     * 2. Найденные вопросы имеют правильные значения полей
     */
    @Test
    void findBySurveyId_ShouldReturnAllQuestionsForSurvey() {
        // Создаем дополнительный вопрос для того же опроса
        SurveyQuestion secondQuestion = new SurveyQuestion();
        secondQuestion.setSurveyId(testSurvey.getId());
        entityManager.persist(secondQuestion);
        entityManager.flush();

        // Получаем все вопросы для опроса
        List<SurveyQuestion> questions = surveyQuestionRepository.findAll().stream()
            .filter(q -> q.getSurveyId().equals(testSurvey.getId()))
            .collect(java.util.stream.Collectors.toList());
        
        // Проверяем результаты
        assertNotNull(questions, "Список вопросов не должен быть null");
        assertEquals(2, questions.size(), "Должно быть найдено 2 вопроса");
        assertTrue(questions.stream().allMatch(q -> q.getSurveyId().equals(testSurvey.getId())),
            "Все найденные вопросы должны быть связаны с указанным опросом");
    }
}
