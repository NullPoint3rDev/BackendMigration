package org.alloy.repositories;

import org.alloy.models.entities.Survey;
import org.alloy.models.entities.SurveyPass;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRole;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности SurveyRepository
 * Этот класс тестирует основные операции CRUD для сущности Survey
 * Survey представляет собой опрос, который может содержать множество прохождений (SurveyPass)
 */
@DataJpaTest
@ActiveProfiles("test")
public class SurveyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SurveyRepository surveyRepository;

    private Survey testSurvey;
    private SurveyPass testSurveyPass;
    private UserAccount testUserAccount;
    private UserRole testUserRole;

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
        testUserAccount.setUserName("testuser");
        testUserAccount.setName("Test User");
        testUserAccount.setStatus(GeneralStatus.Active);
        testUserAccount.setUserRoleId(testUserRole.getId()); // Используем ID созданной роли
        testUserAccount = entityManager.persist(testUserAccount);

        // Создаем тестовый опрос
        testSurvey = new Survey();
        testSurvey.setSurveyPasses(new ArrayList<>());
        testSurvey = entityManager.persist(testSurvey);

        // Создаем тестовое прохождение опроса
        testSurveyPass = new SurveyPass();
        testSurveyPass.setSurvey(testSurvey);
        testSurveyPass.setUserAccountId(testUserAccount.getId()); // Используем ID созданного пользователя
        testSurveyPass.setTitle("Test Survey Pass");
        testSurveyPass.setStatus("NEW");
        testSurveyPass = entityManager.persist(testSurveyPass);

        // Добавляем прохождение в список прохождений опроса
        testSurvey.getSurveyPasses().add(testSurveyPass);
        testSurvey = entityManager.persist(testSurvey);
        
        entityManager.flush();
    }

    /**
     * Тест проверяет успешное сохранение нового объекта Survey
     * Проверяет, что:
     * 1. Объект успешно сохраняется
     * 2. ID объекта корректно генерируется
     * 3. Объект можно найти в базе данных
     * 4. Связи с другими объектами сохраняются корректно
     */
    @Test
    void save_ShouldSaveSurvey() {
        // Создаем новый объект для сохранения
        Survey newSurvey = new Survey();
        newSurvey.setSurveyPasses(new ArrayList<>());
        
        // Создаем и связываем прохождение опроса
        SurveyPass newSurveyPass = new SurveyPass();
        newSurveyPass.setSurvey(newSurvey);
        newSurveyPass.setUserAccountId(testUserAccount.getId()); // Используем ID созданного пользователя
        newSurveyPass.setTitle("New Survey Pass");
        newSurveyPass.setStatus("NEW");
        newSurvey.getSurveyPasses().add(newSurveyPass);
        
        // Сохраняем объект через репозиторий
        Survey savedSurvey = surveyRepository.save(newSurvey);
        
        // Проверяем, что объект был сохранен
        assertNotNull(savedSurvey, "Сохраненный объект не должен быть null");
        assertNotNull(savedSurvey.getId(), "ID сохраненного объекта не должен быть null");
        
        // Проверяем, что объект можно найти в базе данных
        Optional<Survey> foundSurvey = surveyRepository.findById(savedSurvey.getId());
        assertTrue(foundSurvey.isPresent(), "Сохраненный объект должен быть найден в базе данных");
        assertEquals(savedSurvey.getId(), foundSurvey.get().getId(), 
            "ID найденного объекта должен совпадать с ID сохраненного объекта");
        
        // Проверяем сохранение связей
        assertNotNull(foundSurvey.get().getSurveyPasses(), "Список прохождений не должен быть null");
        assertEquals(1, foundSurvey.get().getSurveyPasses().size(), 
            "Должно быть сохранено одно прохождение опроса");
    }

    /**
     * Тест проверяет успешное получение объекта Survey по ID
     * Проверяет, что:
     * 1. Объект успешно находится по ID
     * 2. Найденный объект имеет правильные значения всех полей
     * 3. Связи с другими объектами загружаются корректно
     */
    @Test
    void findById_ShouldReturnSurvey() {
        // Ищем объект по ID
        Optional<Survey> foundSurvey = surveyRepository.findById(testSurvey.getId());
        
        // Проверяем результаты
        assertTrue(foundSurvey.isPresent(), "Объект должен быть найден");
        assertEquals(testSurvey.getId(), foundSurvey.get().getId(), 
            "ID найденного объекта должен совпадать с ID тестового объекта");
        
        // Проверяем загрузку связей
        assertNotNull(foundSurvey.get().getSurveyPasses(), "Список прохождений не должен быть null");
        assertEquals(1, foundSurvey.get().getSurveyPasses().size(), 
            "Должно быть загружено одно прохождение опроса");
        assertEquals(testSurveyPass.getId(), foundSurvey.get().getSurveyPasses().get(0).getId(),
            "ID загруженного прохождения должен совпадать с ID тестового прохождения");
    }

    /**
     * Тест проверяет получение всех объектов Survey
     * Проверяет, что:
     * 1. Список всех объектов не пустой
     * 2. Тестовый объект присутствует в списке
     * 3. Все объекты имеют корректные значения полей
     * 4. Связи с другими объектами загружаются корректно
     */
    @Test
    void findAll_ShouldReturnAllSurveys() {
        // Получаем все объекты
        List<Survey> allSurveys = surveyRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(allSurveys, "Список объектов не должен быть null");
        assertFalse(allSurveys.isEmpty(), "Список объектов не должен быть пустым");
        assertTrue(allSurveys.stream()
                .anyMatch(survey -> survey.getId().equals(testSurvey.getId())),
            "Список должен содержать тестовый объект");
        
        // Проверяем значения полей тестового объекта в списке
        Survey foundSurvey = allSurveys.stream()
            .filter(survey -> survey.getId().equals(testSurvey.getId()))
            .findFirst()
            .orElse(null);
            
        assertNotNull(foundSurvey, "Тестовый объект должен быть найден в списке");
        assertNotNull(foundSurvey.getSurveyPasses(), "Список прохождений не должен быть null");
        assertEquals(1, foundSurvey.getSurveyPasses().size(), 
            "Должно быть загружено одно прохождение опроса");
    }

    /**
     * Тест проверяет успешное удаление объекта Survey
     * Проверяет, что:
     * 1. Объект успешно удаляется
     * 2. После удаления объект нельзя найти в базе данных
     * 3. Связанные объекты также удаляются (cascade delete)
     */
    @Test
    void delete_ShouldRemoveSurvey() {
        // Удаляем тестовый объект
        surveyRepository.delete(testSurvey);
        
        // Проверяем, что объект больше не существует в базе данных
        Optional<Survey> deletedSurvey = surveyRepository.findById(testSurvey.getId());
        assertFalse(deletedSurvey.isPresent(), "Удаленный объект не должен быть найден в базе данных");
        
        // Проверяем, что связанные объекты также удалены
        // Для этого нужно получить доступ к EntityManager
        SurveyPass deletedSurveyPass = entityManager.find(SurveyPass.class, testSurveyPass.getId());
        assertNull(deletedSurveyPass, "Связанный объект SurveyPass также должен быть удален");
    }

    /**
     * Тест проверяет сохранение Survey с несколькими прохождениями
     * Проверяет, что:
     * 1. Объект успешно сохраняется с несколькими прохождениями
     * 2. Все прохождения корректно связываются с опросом
     * 3. Все связи сохраняются в базе данных
     */
    @Test
    void save_ShouldHandleMultipleSurveyPasses() {
        // Создаем новый опрос
        Survey newSurvey = new Survey();
        newSurvey.setSurveyPasses(new ArrayList<>());
        
        // Создаем несколько прохождений
        for (int i = 0; i < 3; i++) {
            SurveyPass surveyPass = new SurveyPass();
            surveyPass.setSurvey(newSurvey);
            surveyPass.setUserAccountId(testUserAccount.getId()); // Используем ID созданного пользователя
            surveyPass.setTitle("Survey Pass " + (i + 1));
            surveyPass.setStatus("NEW");
            newSurvey.getSurveyPasses().add(surveyPass);
        }
        
        // Сохраняем опрос
        Survey savedSurvey = surveyRepository.save(newSurvey);
        
        // Проверяем результаты
        assertNotNull(savedSurvey, "Сохраненный объект не должен быть null");
        assertNotNull(savedSurvey.getSurveyPasses(), "Список прохождений не должен быть null");
        assertEquals(3, savedSurvey.getSurveyPasses().size(), 
            "Должно быть сохранено три прохождения опроса");
        
        // Проверяем сохранение в базе данных
        Optional<Survey> foundSurvey = surveyRepository.findById(savedSurvey.getId());
        assertTrue(foundSurvey.isPresent(), "Сохраненный объект должен быть найден в базе данных");
        assertEquals(3, foundSurvey.get().getSurveyPasses().size(),
            "В базе данных должно быть сохранено три прохождения опроса");
    }
}
