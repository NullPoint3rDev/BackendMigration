package org.alloy.repositories;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.SurveyPass;
import org.alloy.models.entities.SurveyPassQuestion;
import org.alloy.models.entities.SurveyQuestion;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRole;
import org.alloy.models.entities.Survey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности SurveyPassQuestionRepository
 * Этот класс тестирует основные операции CRUD для сущности SurveyPassQuestion
 * SurveyPassQuestion представляет собой связь между прохождением опроса (SurveyPass)
 * и вопросом опроса (SurveyQuestion)
 */
@DataJpaTest
@ActiveProfiles("test")
public class SurveyPassQuestionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SurveyPassQuestionRepository surveyPassQuestionRepository;

    private SurveyPassQuestion testSurveyPassQuestion;
    private SurveyPass testSurveyPass;
    private SurveyQuestion testSurveyQuestion;
    private UserAccount testUserAccount;
    private UserRole testUserRole;
    private Survey testSurvey;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает необходимые тестовые объекты и связи между ними
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую роль пользователя
        testUserRole = new UserRole();
        testUserRole.setName("Test Role");
        testUserRole.setDescription("Test Role Description");
        testUserRole.setStatus(GeneralStatus.Active);
        testUserRole = entityManager.persist(testUserRole);

        // Создаем тестового пользователя
        testUserAccount = new UserAccount();
        testUserAccount.setEmail("test@example.com");
        testUserAccount.setUserName("testuser");
        testUserAccount.setPasswordHash("password".getBytes());
        testUserAccount.setPasswordSalt("salt");
        testUserAccount.setStatus(GeneralStatus.Active);
        testUserAccount.setUserRoleId(testUserRole.getId());
        testUserAccount = entityManager.persist(testUserAccount);

        // Создаем тестовый опрос
        testSurvey = new Survey();
        testSurvey = entityManager.persist(testSurvey);

        // Создаем тестовый проход опроса
        testSurveyPass = new SurveyPass();
        testSurveyPass.setUserAccountId(testUserAccount.getId());
        testSurveyPass.setTitle("Test Survey Pass");
        testSurveyPass.setDescription("Test Description");
        testSurveyPass.setStatus("In Progress");
        testSurveyPass.setSurvey(testSurvey); // Устанавливаем связь с опросом
        testSurveyPass = entityManager.persist(testSurveyPass);

        // Создаем тестовый вопрос
        testSurveyQuestion = new SurveyQuestion();
        testSurveyQuestion.setSurveyId(testSurvey.getId()); // Используем ID созданного опроса
        testSurveyQuestion = entityManager.persist(testSurveyQuestion);

        // Создаем тестовую связь между проходом опроса и вопросом
        testSurveyPassQuestion = new SurveyPassQuestion();
        testSurveyPassQuestion.setSurveyPassId(testSurveyPass.getId());
        testSurveyPassQuestion.setSurveyQuestionId(testSurveyQuestion.getId());
        
        // Сохраняем объект в тестовой базе данных
        testSurveyPassQuestion = entityManager.persist(testSurveyPassQuestion);
        entityManager.flush();
    }

    /**
     * Тест проверяет успешное сохранение нового объекта SurveyPassQuestion
     * Проверяет, что:
     * 1. Объект успешно сохраняется
     * 2. ID объекта корректно генерируется
     * 3. Объект можно найти в базе данных
     */
    @Test
    void save_ShouldSaveSurveyPassQuestion() {
        // Создаем новый объект для сохранения
        SurveyPassQuestion newSurveyPassQuestion = new SurveyPassQuestion();
        newSurveyPassQuestion.setSurveyPassId(testSurveyPass.getId());
        newSurveyPassQuestion.setSurveyQuestionId(testSurveyQuestion.getId());
        
        // Сохраняем объект через репозиторий
        SurveyPassQuestion savedSurveyPassQuestion = surveyPassQuestionRepository.save(newSurveyPassQuestion);
        
        // Проверяем, что объект был сохранен
        assertNotNull(savedSurveyPassQuestion, "Сохраненный объект не должен быть null");
        assertNotNull(savedSurveyPassQuestion.getId(), "ID сохраненного объекта не должен быть null");
        
        // Проверяем, что объект можно найти в базе данных
        Optional<SurveyPassQuestion> foundSurveyPassQuestion = surveyPassQuestionRepository.findById(savedSurveyPassQuestion.getId());
        assertTrue(foundSurveyPassQuestion.isPresent(), "Сохраненный объект должен быть найден в базе данных");
        assertEquals(savedSurveyPassQuestion.getId(), foundSurveyPassQuestion.get().getId(), 
            "ID найденного объекта должен совпадать с ID сохраненного объекта");
    }

    /**
     * Тест проверяет успешное получение объекта SurveyPassQuestion по ID
     * Проверяет, что:
     * 1. Объект успешно находится по ID
     * 2. Найденный объект имеет правильные значения всех полей
     */
    @Test
    void findById_ShouldReturnSurveyPassQuestion() {
        // Ищем объект по ID
        Optional<SurveyPassQuestion> foundSurveyPassQuestion = surveyPassQuestionRepository.findById(testSurveyPassQuestion.getId());
        
        // Проверяем результаты
        assertTrue(foundSurveyPassQuestion.isPresent(), "Объект должен быть найден");
        assertEquals(testSurveyPassQuestion.getId(), foundSurveyPassQuestion.get().getId(), 
            "ID найденного объекта должен совпадать с ID тестового объекта");
        assertEquals(testSurveyPassQuestion.getSurveyPassId(), foundSurveyPassQuestion.get().getSurveyPassId(),
            "ID прохода опроса должен совпадать");
        assertEquals(testSurveyPassQuestion.getSurveyQuestionId(), foundSurveyPassQuestion.get().getSurveyQuestionId(),
            "ID вопроса опроса должен совпадать");
    }

    /**
     * Тест проверяет получение всех объектов SurveyPassQuestion
     * Проверяет, что:
     * 1. Список всех объектов не пустой
     * 2. Тестовый объект присутствует в списке
     * 3. Все объекты имеют корректные значения полей
     */
    @Test
    void findAll_ShouldReturnAllSurveyPassQuestions() {
        // Получаем все объекты
        List<SurveyPassQuestion> allSurveyPassQuestions = surveyPassQuestionRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(allSurveyPassQuestions, "Список объектов не должен быть null");
        assertFalse(allSurveyPassQuestions.isEmpty(), "Список объектов не должен быть пустым");
        assertTrue(allSurveyPassQuestions.stream()
                .anyMatch(question -> question.getId().equals(testSurveyPassQuestion.getId())),
            "Список должен содержать тестовый объект");
        
        // Проверяем значения полей тестового объекта в списке
        SurveyPassQuestion foundQuestion = allSurveyPassQuestions.stream()
            .filter(question -> question.getId().equals(testSurveyPassQuestion.getId()))
            .findFirst()
            .orElse(null);
            
        assertNotNull(foundQuestion, "Тестовый объект должен быть найден в списке");
        assertEquals(testSurveyPassQuestion.getSurveyPassId(), foundQuestion.getSurveyPassId(),
            "ID прохода опроса должен совпадать");
        assertEquals(testSurveyPassQuestion.getSurveyQuestionId(), foundQuestion.getSurveyQuestionId(),
            "ID вопроса опроса должен совпадать");
    }

    /**
     * Тест проверяет успешное удаление объекта SurveyPassQuestion
     * Проверяет, что:
     * 1. Объект успешно удаляется
     * 2. После удаления объект нельзя найти в базе данных
     */
    @Test
    void delete_ShouldRemoveSurveyPassQuestion() {
        // Удаляем тестовый объект
        surveyPassQuestionRepository.delete(testSurveyPassQuestion);
        
        // Проверяем, что объект больше не существует в базе данных
        Optional<SurveyPassQuestion> deletedSurveyPassQuestion = surveyPassQuestionRepository.findById(testSurveyPassQuestion.getId());
        assertFalse(deletedSurveyPassQuestion.isPresent(), "Удаленный объект не должен быть найден в базе данных");
    }

    /**
     * Тест проверяет получение всех вопросов для конкретного прохода опроса
     * Проверяет, что:
     * 1. Находятся все вопросы, связанные с указанным проходом опроса
     * 2. Найденные вопросы имеют правильные значения полей
     */
    @Test
    void findBySurveyPassId_ShouldReturnAllQuestionsForSurveyPass() {
        // Создаем дополнительный вопрос для того же прохода опроса
        SurveyQuestion secondQuestion = new SurveyQuestion();
        secondQuestion.setSurveyId(testSurvey.getId());
        secondQuestion = entityManager.persist(secondQuestion);

        SurveyPassQuestion secondPassQuestion = new SurveyPassQuestion();
        secondPassQuestion.setSurveyPassId(testSurveyPass.getId());
        secondPassQuestion.setSurveyQuestionId(secondQuestion.getId());
        entityManager.persist(secondPassQuestion);
        entityManager.flush();

        // Получаем все вопросы для прохода опроса
        List<SurveyPassQuestion> questions = surveyPassQuestionRepository.findAll().stream()
            .filter(q -> q.getSurveyPassId().equals(testSurveyPass.getId()))
            .collect(Collectors.toList());
        
        // Проверяем результаты
        assertNotNull(questions, "Список вопросов не должен быть null");
        assertEquals(2, questions.size(), "Должно быть найдено 2 вопроса");
        assertTrue(questions.stream().allMatch(q -> q.getSurveyPassId().equals(testSurveyPass.getId())),
            "Все найденные вопросы должны быть связаны с указанным проходом опроса");
    }
}
