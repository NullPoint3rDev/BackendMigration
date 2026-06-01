package org.alloy.services;

import org.alloy.models.entities.Translation;
import org.alloy.repositories.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для TranslationService
 * Проверяет корректность работы сервиса переводов
 */
@SpringBootTest(classes = TranslationService.class)
@ActiveProfiles("test")
public class TranslationServiceTest {

    @MockBean
    private TranslationRepository translationRepository;

    @Autowired
    private TranslationService translationService;

    private Translation testTranslation;
    private Pageable pageable;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект Translation с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        // Создание тестового перевода
        testTranslation = new Translation();
        testTranslation.setId(1);
        testTranslation.setLang("ru");
        testTranslation.setTableName("User");
        testTranslation.setColumnName("name");
        testTranslation.setIdName("id");
        testTranslation.setIdValue("1");
        testTranslation.setValue("Пользователь");

        // Создание объекта пагинации
        pageable = PageRequest.of(0, 10);
    }

    /**
     * Тест метода findAll() с пагинацией
     * Проверяет корректность получения всех переводов с учетом пагинации
     */
    @Test
    void findAll_ShouldReturnAllTranslations() {
        // Подготовка данных
        List<Translation> translations = Arrays.asList(testTranslation);
        Page<Translation> expectedPage = new PageImpl<>(translations, pageable, translations.size());
        when(translationRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // Выполнение теста
        Page<Translation> actualPage = translationService.findAll(pageable);

        // Проверка результатов
        assertNotNull(actualPage);
        assertEquals(expectedPage.getTotalElements(), actualPage.getTotalElements());
        assertEquals(expectedPage.getContent().size(), actualPage.getContent().size());
        
        Translation actualTranslation = actualPage.getContent().get(0);
        assertEquals(testTranslation.getId(), actualTranslation.getId());
        assertEquals(testTranslation.getLang(), actualTranslation.getLang());
        assertEquals(testTranslation.getTableName(), actualTranslation.getTableName());
        assertEquals(testTranslation.getColumnName(), actualTranslation.getColumnName());
        assertEquals(testTranslation.getIdName(), actualTranslation.getIdName());
        assertEquals(testTranslation.getIdValue(), actualTranslation.getIdValue());
        assertEquals(testTranslation.getValue(), actualTranslation.getValue());

        // Проверка вызова метода репозитория
        verify(translationRepository, times(1)).findAll(any(Pageable.class));
    }

    /**
     * Тест метода findById() с существующим ID
     * Проверяет корректность получения перевода по существующему ID
     */
    @Test
    void findById_WhenExists_ShouldReturnTranslation() {
        // Подготовка данных
        when(translationRepository.findById(1)).thenReturn(Optional.of(testTranslation));

        // Выполнение теста
        Optional<Translation> result = translationService.findById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testTranslation.getId(), result.get().getId());
        assertEquals(testTranslation.getLang(), result.get().getLang());
        assertEquals(testTranslation.getTableName(), result.get().getTableName());
        assertEquals(testTranslation.getColumnName(), result.get().getColumnName());
        assertEquals(testTranslation.getIdName(), result.get().getIdName());
        assertEquals(testTranslation.getIdValue(), result.get().getIdValue());
        assertEquals(testTranslation.getValue(), result.get().getValue());

        // Проверка вызова метода репозитория
        verify(translationRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() с несуществующим ID
     * Проверяет корректность получения пустого результата при поиске несуществующего перевода
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготовка данных
        when(translationRepository.findById(999)).thenReturn(Optional.empty());

        // Выполнение теста
        Optional<Translation> result = translationService.findById(999);

        // Проверка результатов
        assertFalse(result.isPresent());

        // Проверка вызова метода репозитория
        verify(translationRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения нового перевода
     */
    @Test
    void save_ShouldSaveTranslation() {
        // Подготовка данных
        when(translationRepository.save(any(Translation.class))).thenReturn(testTranslation);

        // Выполнение теста
        Translation savedTranslation = translationService.save(testTranslation);

        // Проверка результатов
        assertNotNull(savedTranslation);
        assertEquals(testTranslation.getId(), savedTranslation.getId());
        assertEquals(testTranslation.getLang(), savedTranslation.getLang());
        assertEquals(testTranslation.getTableName(), savedTranslation.getTableName());
        assertEquals(testTranslation.getColumnName(), savedTranslation.getColumnName());
        assertEquals(testTranslation.getIdName(), savedTranslation.getIdName());
        assertEquals(testTranslation.getIdValue(), savedTranslation.getIdValue());
        assertEquals(testTranslation.getValue(), savedTranslation.getValue());

        // Проверка вызова метода репозитория
        verify(translationRepository, times(1)).save(any(Translation.class));
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления перевода по ID
     */
    @Test
    void deleteById_ShouldDeleteTranslation() {
        // Выполнение теста
        translationService.deleteById(1);

        // Проверка вызова метода репозитория
        verify(translationRepository, times(1)).deleteById(1);
    }
}
