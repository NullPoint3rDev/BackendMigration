package org.alloy.repositories;

import org.alloy.models.entities.EmailTemplate;
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
 * Тестовый класс для проверки функциональности EmailTemplateRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
public class EmailTemplateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    private EmailTemplate testEmailTemplate;

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовый объект EmailTemplate для использования в тестах
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый объект EmailTemplate
        testEmailTemplate = new EmailTemplate();
        
        // Сохраняем объект в тестовой базе данных
        entityManager.persist(testEmailTemplate);
        entityManager.flush();
    }

    /**
     * Тест сохранения нового шаблона письма
     * Проверяет, что шаблон корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveEmailTemplate() {
        // Создаем новый шаблон
        EmailTemplate newEmailTemplate = new EmailTemplate();
        
        // Сохраняем шаблон через репозиторий
        EmailTemplate savedEmailTemplate = emailTemplateRepository.save(newEmailTemplate);
        
        // Проверяем, что шаблон был сохранен
        assertNotNull(savedEmailTemplate, "Сохраненный шаблон не должен быть null");
        assertNotNull(savedEmailTemplate.getId(), "ID сохраненного шаблона не должен быть null");
        
        // Проверяем, что шаблон можно найти в базе данных
        EmailTemplate foundEmailTemplate = entityManager.find(EmailTemplate.class, savedEmailTemplate.getId());
        assertNotNull(foundEmailTemplate, "Шаблон должен быть найден в базе данных");
    }

    /**
     * Тест поиска шаблона по ID
     * Проверяет, что шаблон корректно находится по его ID
     */
    @Test
    void findById_ShouldReturnEmailTemplate() {
        // Ищем шаблон по ID
        Optional<EmailTemplate> foundEmailTemplate = emailTemplateRepository.findById(testEmailTemplate.getId());
        
        // Проверяем результаты
        assertTrue(foundEmailTemplate.isPresent(), "Шаблон должен быть найден");
        assertEquals(testEmailTemplate.getId(), foundEmailTemplate.get().getId(), 
            "ID найденного шаблона должен совпадать");
    }

    /**
     * Тест поиска несуществующего шаблона
     * Проверяет, что при поиске несуществующего шаблона возвращается пустой Optional
     */
    @Test
    void findById_WhenEmailTemplateDoesNotExist_ShouldReturnEmpty() {
        // Ищем шаблон с несуществующим ID
        Optional<EmailTemplate> foundEmailTemplate = emailTemplateRepository.findById(999);
        
        // Проверяем, что результат пустой
        assertFalse(foundEmailTemplate.isPresent(), "Результат должен быть пустым");
    }

    /**
     * Тест получения всех шаблонов
     * Проверяет, что метод findAll возвращает все сохраненные шаблоны
     */
    @Test
    void findAll_ShouldReturnAllEmailTemplates() {
        // Создаем и сохраняем второй шаблон
        EmailTemplate secondEmailTemplate = new EmailTemplate();
        entityManager.persist(secondEmailTemplate);
        entityManager.flush();
        
        // Получаем все шаблоны
        List<EmailTemplate> emailTemplates = emailTemplateRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(emailTemplates, "Список шаблонов не должен быть null");
        assertTrue(emailTemplates.size() >= 2, "Должно быть найдено как минимум 2 шаблона");
        assertTrue(emailTemplates.stream().anyMatch(t -> t.getId().equals(testEmailTemplate.getId())), 
            "Список должен содержать первый тестовый шаблон");
        assertTrue(emailTemplates.stream().anyMatch(t -> t.getId().equals(secondEmailTemplate.getId())), 
            "Список должен содержать второй тестовый шаблон");
    }

    /**
     * Тест удаления шаблона
     * Проверяет, что шаблон корректно удаляется из базы данных
     */
    @Test
    void delete_ShouldRemoveEmailTemplate() {
        // Удаляем шаблон
        emailTemplateRepository.delete(testEmailTemplate);
        
        // Проверяем, что шаблон больше не существует в базе данных
        EmailTemplate deletedEmailTemplate = entityManager.find(EmailTemplate.class, testEmailTemplate.getId());
        assertNull(deletedEmailTemplate, "Шаблон должен быть удален из базы данных");
    }

    /**
     * Тест проверки существования шаблона
     * Проверяет, что метод existsById корректно определяет наличие шаблона
     */
    @Test
    void existsById_ShouldReturnCorrectResult() {
        // Проверяем существование сохраненного шаблона
        boolean exists = emailTemplateRepository.existsById(testEmailTemplate.getId());
        assertTrue(exists, "Метод должен вернуть true для существующего шаблона");
        
        // Проверяем несуществующий шаблон
        boolean notExists = emailTemplateRepository.existsById(999);
        assertFalse(notExists, "Метод должен вернуть false для несуществующего шаблона");
    }

    /**
     * Тест обновления шаблона
     * Проверяет, что шаблон корректно обновляется в базе данных
     */
    @Test
    void save_ShouldUpdateExistingEmailTemplate() {
        // Сохраняем обновленный шаблон
        EmailTemplate updatedEmailTemplate = emailTemplateRepository.save(testEmailTemplate);
        
        // Проверяем, что ID остался тем же
        assertEquals(testEmailTemplate.getId(), updatedEmailTemplate.getId(), 
            "ID шаблона не должен измениться при обновлении");
        
        // Проверяем, что обновленный шаблон можно найти в базе данных
        EmailTemplate foundEmailTemplate = entityManager.find(EmailTemplate.class, testEmailTemplate.getId());
        assertNotNull(foundEmailTemplate, "Обновленный шаблон должен быть найден в базе данных");
    }
}
