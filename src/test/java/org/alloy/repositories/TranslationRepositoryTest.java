package org.alloy.repositories;

import org.alloy.models.entities.Translation;
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
 * Тестовый класс для проверки функциональности TranslationRepository
 * Этот класс тестирует основные операции CRUD для сущности Translation
 * Translation представляет собой перевод для определенного поля в определенной таблице
 * и используется для мультиязычности приложения
 */
@DataJpaTest
@ActiveProfiles("test")
public class TranslationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TranslationRepository translationRepository;

    private Translation testTranslation;

    /**
     * Метод настройки тестового окружения
     * Выполняется перед каждым тестом
     * Создает необходимые тестовые объекты
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый объект перевода
        testTranslation = new Translation();
        testTranslation.setLang("ru");
        testTranslation.setTableName("UserRole");
        testTranslation.setColumnName("Name");
        testTranslation.setIdName("ID");
        testTranslation.setIdValue("1");
        testTranslation.setValue("Администратор");
        testTranslation = entityManager.persist(testTranslation);
        
        entityManager.flush();
    }

    /**
     * Тест проверяет успешное сохранение нового объекта Translation
     * Проверяет, что:
     * 1. Объект успешно сохраняется
     * 2. ID объекта корректно генерируется
     * 3. Объект можно найти в базе данных
     * 4. Все поля объекта сохраняются корректно
     */
    @Test
    void save_ShouldSaveTranslation() {
        // Создаем новый объект для сохранения
        Translation newTranslation = new Translation();
        newTranslation.setLang("en");
        newTranslation.setTableName("UserRole");
        newTranslation.setColumnName("Name");
        newTranslation.setIdName("ID");
        newTranslation.setIdValue("1");
        newTranslation.setValue("Administrator");
        
        // Сохраняем объект через репозиторий
        Translation savedTranslation = translationRepository.save(newTranslation);
        
        // Проверяем, что объект был сохранен
        assertNotNull(savedTranslation, "Сохраненный объект не должен быть null");
        assertNotNull(savedTranslation.getId(), "ID сохраненного объекта не должен быть null");
        
        // Проверяем, что объект можно найти в базе данных
        Optional<Translation> foundTranslation = translationRepository.findById(savedTranslation.getId());
        assertTrue(foundTranslation.isPresent(), "Сохраненный объект должен быть найден в базе данных");
        
        // Проверяем значения всех полей
        Translation found = foundTranslation.get();
        assertEquals("en", found.getLang(), "Язык перевода должен совпадать");
        assertEquals("UserRole", found.getTableName(), "Имя таблицы должно совпадать");
        assertEquals("Name", found.getColumnName(), "Имя колонки должно совпадать");
        assertEquals("ID", found.getIdName(), "Имя идентификатора должно совпадать");
        assertEquals("1", found.getIdValue(), "Значение идентификатора должно совпадать");
        assertEquals("Administrator", found.getValue(), "Значение перевода должно совпадать");
    }

    /**
     * Тест проверяет успешное получение объекта Translation по ID
     * Проверяет, что:
     * 1. Объект успешно находится по ID
     * 2. Найденный объект имеет правильные значения всех полей
     */
    @Test
    void findById_ShouldReturnTranslation() {
        // Ищем объект по ID
        Optional<Translation> foundTranslation = translationRepository.findById(testTranslation.getId());
        
        // Проверяем результаты
        assertTrue(foundTranslation.isPresent(), "Объект должен быть найден");
        Translation found = foundTranslation.get();
        
        // Проверяем значения всех полей
        assertEquals(testTranslation.getId(), found.getId(), "ID должен совпадать");
        assertEquals("ru", found.getLang(), "Язык перевода должен совпадать");
        assertEquals("UserRole", found.getTableName(), "Имя таблицы должно совпадать");
        assertEquals("Name", found.getColumnName(), "Имя колонки должно совпадать");
        assertEquals("ID", found.getIdName(), "Имя идентификатора должно совпадать");
        assertEquals("1", found.getIdValue(), "Значение идентификатора должно совпадать");
        assertEquals("Администратор", found.getValue(), "Значение перевода должно совпадать");
    }

    /**
     * Тест проверяет получение всех объектов Translation
     * Проверяет, что:
     * 1. Список всех объектов не пустой
     * 2. Тестовый объект присутствует в списке
     * 3. Все объекты имеют корректные значения полей
     */
    @Test
    void findAll_ShouldReturnAllTranslations() {
        // Получаем все объекты
        List<Translation> allTranslations = translationRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(allTranslations, "Список объектов не должен быть null");
        assertFalse(allTranslations.isEmpty(), "Список объектов не должен быть пустым");
        
        // Проверяем наличие тестового объекта в списке
        assertTrue(allTranslations.stream()
                .anyMatch(translation -> translation.getId().equals(testTranslation.getId())),
            "Список должен содержать тестовый объект");
        
        // Проверяем значения полей тестового объекта в списке
        Translation found = allTranslations.stream()
            .filter(translation -> translation.getId().equals(testTranslation.getId()))
            .findFirst()
            .orElse(null);
            
        assertNotNull(found, "Тестовый объект должен быть найден в списке");
        assertEquals("ru", found.getLang(), "Язык перевода должен совпадать");
        assertEquals("UserRole", found.getTableName(), "Имя таблицы должно совпадать");
        assertEquals("Name", found.getColumnName(), "Имя колонки должно совпадать");
        assertEquals("ID", found.getIdName(), "Имя идентификатора должно совпадать");
        assertEquals("1", found.getIdValue(), "Значение идентификатора должно совпадать");
        assertEquals("Администратор", found.getValue(), "Значение перевода должно совпадать");
    }

    /**
     * Тест проверяет успешное удаление объекта Translation
     * Проверяет, что:
     * 1. Объект успешно удаляется
     * 2. После удаления объект нельзя найти в базе данных
     */
    @Test
    void delete_ShouldRemoveTranslation() {
        // Удаляем тестовый объект
        translationRepository.delete(testTranslation);
        
        // Проверяем, что объект больше не существует в базе данных
        Optional<Translation> deletedTranslation = translationRepository.findById(testTranslation.getId());
        assertFalse(deletedTranslation.isPresent(), "Удаленный объект не должен быть найден в базе данных");
    }

    /**
     * Тест проверяет сохранение нескольких переводов для одного поля
     * Проверяет, что:
     * 1. Объекты успешно сохраняются
     * 2. Все переводы корректно связываются с одним полем
     * 3. Все переводы сохраняются в базе данных
     */
    @Test
    void save_ShouldHandleMultipleTranslations() {
        // Создаем несколько переводов для одного поля
        String[] languages = {"en", "de", "fr"};
        String[] translations = {"Administrator", "Administrator", "Administrateur"};
        
        for (int i = 0; i < languages.length; i++) {
            Translation translation = new Translation();
            translation.setLang(languages[i]);
            translation.setTableName("UserRole");
            translation.setColumnName("Name");
            translation.setIdName("ID");
            translation.setIdValue("1");
            translation.setValue(translations[i]);
            translationRepository.save(translation);
        }
        
        // Проверяем, что все переводы сохранены
        List<Translation> allTranslations = translationRepository.findAll();
        assertEquals(4, allTranslations.size(), "Должно быть сохранено 4 перевода (включая тестовый)");
        
        // Проверяем наличие всех языков
        for (int i = 0; i < languages.length; i++) {
            final String lang = languages[i];
            final String value = translations[i];
            assertTrue(allTranslations.stream()
                    .anyMatch(t -> t.getLang().equals(lang) && t.getValue().equals(value)),
                "Должен быть найден перевод для языка " + lang);
        }
    }
}
