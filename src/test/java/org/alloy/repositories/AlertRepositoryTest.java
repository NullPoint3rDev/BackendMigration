package org.alloy.repositories;

import org.alloy.models.entities.Alert;
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
 * Тестовый класс для проверки функциональности AlertRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
public class AlertRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AlertRepository alertRepository;

    private Alert testAlert;

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовый объект Alert для использования в тестах
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый объект Alert
        testAlert = new Alert();
        
        // Сохраняем объект в тестовой базе данных
        entityManager.persist(testAlert);
        entityManager.flush();
    }

    /**
     * Тест сохранения нового оповещения
     * Проверяет, что оповещение корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveAlert() {
        // Создаем новое оповещение
        Alert newAlert = new Alert();
        
        // Сохраняем оповещение через репозиторий
        Alert savedAlert = alertRepository.save(newAlert);
        
        // Проверяем, что оповещение было сохранено
        assertNotNull(savedAlert, "Сохраненное оповещение не должно быть null");
        assertNotNull(savedAlert.getId(), "ID сохраненного оповещения не должен быть null");
        
        // Проверяем, что оповещение можно найти в базе данных
        Alert foundAlert = entityManager.find(Alert.class, savedAlert.getId());
        assertNotNull(foundAlert, "Оповещение должно быть найдено в базе данных");
    }

    /**
     * Тест поиска оповещения по ID
     * Проверяет, что оповещение корректно находится по его ID
     */
    @Test
    void findById_ShouldReturnAlert() {
        // Ищем оповещение по ID
        Optional<Alert> foundAlert = alertRepository.findById(testAlert.getId());
        
        // Проверяем результаты
        assertTrue(foundAlert.isPresent(), "Оповещение должно быть найдено");
        assertEquals(testAlert.getId(), foundAlert.get().getId(), "ID найденного оповещения должен совпадать");
    }

    /**
     * Тест поиска несуществующего оповещения
     * Проверяет, что при поиске несуществующего оповещения возвращается пустой Optional
     */
    @Test
    void findById_WhenAlertDoesNotExist_ShouldReturnEmpty() {
        // Ищем оповещение с несуществующим ID
        Optional<Alert> foundAlert = alertRepository.findById(999);
        
        // Проверяем, что результат пустой
        assertFalse(foundAlert.isPresent(), "Результат должен быть пустым");
    }

    /**
     * Тест получения всех оповещений
     * Проверяет, что метод findAll возвращает все сохраненные оповещения
     */
    @Test
    void findAll_ShouldReturnAllAlerts() {
        // Создаем и сохраняем второе оповещение
        Alert secondAlert = new Alert();
        entityManager.persist(secondAlert);
        entityManager.flush();
        
        // Получаем все оповещения
        List<Alert> alerts = alertRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(alerts, "Список оповещений не должен быть null");
        assertTrue(alerts.size() >= 2, "Должно быть найдено как минимум 2 оповещения");
        assertTrue(alerts.stream().anyMatch(a -> a.getId().equals(testAlert.getId())), 
            "Список должен содержать первое тестовое оповещение");
        assertTrue(alerts.stream().anyMatch(a -> a.getId().equals(secondAlert.getId())), 
            "Список должен содержать второе тестовое оповещение");
    }

    /**
     * Тест удаления оповещения
     * Проверяет, что оповещение корректно удаляется из базы данных
     */
    @Test
    void delete_ShouldRemoveAlert() {
        // Удаляем оповещение
        alertRepository.delete(testAlert);
        
        // Проверяем, что оповещение больше не существует в базе данных
        Alert deletedAlert = entityManager.find(Alert.class, testAlert.getId());
        assertNull(deletedAlert, "Оповещение должно быть удалено из базы данных");
    }

    /**
     * Тест проверки существования оповещения
     * Проверяет, что метод existsById корректно определяет наличие оповещения
     */
    @Test
    void existsById_ShouldReturnCorrectResult() {
        // Проверяем существование сохраненного оповещения
        boolean exists = alertRepository.existsById(testAlert.getId());
        assertTrue(exists, "Метод должен вернуть true для существующего оповещения");
        
        // Проверяем несуществующее оповещение
        boolean notExists = alertRepository.existsById(999);
        assertFalse(notExists, "Метод должен вернуть false для несуществующего оповещения");
    }
}
