package org.alloy.services;

import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.GeneralStatus;
import org.alloy.repositories.WeldingMachineTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
public class WeldingMachineTypeServiceTest {

    @MockBean
    private WeldingMachineTypeRepository weldingMachineTypeRepository;

    @Autowired
    private WeldingMachineTypeService weldingMachineTypeService;

    private WeldingMachineType testWeldingMachineType;

    @BeforeEach
    void setUp() {
        // Инициализируем тестовый объект WeldingMachineType
        testWeldingMachineType = new WeldingMachineType();
        testWeldingMachineType.setId(1);
        testWeldingMachineType.setName("Test Type");
        testWeldingMachineType.setDescription("Test Description");
        testWeldingMachineType.setStatus(GeneralStatus.Active);
        testWeldingMachineType.setDateCreated(LocalDateTime.now());
    }

    /**
     * Тест получения всех типов сварочных машин
     * Проверяет корректность получения списка всех типов
     */
    @Test
    void getAllWeldingMachineTypes_ShouldReturnAllTypes() {
        // Подготавливаем тестовые данные
        List<WeldingMachineType> expectedTypes = Arrays.asList(testWeldingMachineType);
        when(weldingMachineTypeRepository.findAll()).thenReturn(expectedTypes);

        // Вызываем тестируемый метод
        List<WeldingMachineType> result = weldingMachineTypeService.getAllWeldingMachineTypes();

        // Проверяем результаты
        assertNotNull(result, "Список типов не должен быть null");
        assertEquals(expectedTypes.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testWeldingMachineType.getId(), result.get(0).getId(), "ID должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineTypeRepository, times(1)).findAll();
    }

    /**
     * Тест получения типа сварочной машины по ID
     * Проверяет корректность получения типа по существующему ID
     */
    @Test
    void getWeldingMachineTypeById_WhenExists_ShouldReturnType() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.findById(1)).thenReturn(Optional.of(testWeldingMachineType));

        // Вызываем тестируемый метод
        Optional<WeldingMachineType> result = weldingMachineTypeService.getWeldingMachineTypeById(1);

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать тип");
        assertEquals(testWeldingMachineType.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testWeldingMachineType.getName(), result.get().getName(), "Название должно совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineTypeRepository, times(1)).findById(1);
    }

    /**
     * Тест получения типа сварочной машины по названию
     * Проверяет корректность получения типа по существующему названию
     */
    @Test
    void getWeldingMachineTypeByName_WhenExists_ShouldReturnType() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.findByName("Test Type"))
            .thenReturn(Optional.of(testWeldingMachineType));

        // Вызываем тестируемый метод
        Optional<WeldingMachineType> result = weldingMachineTypeService.getWeldingMachineTypeByName("Test Type");

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать тип");
        assertEquals(testWeldingMachineType.getName(), result.get().getName(), "Название должно совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineTypeRepository, times(1)).findByName("Test Type");
    }

    /**
     * Тест получения типов сварочных машин по статусу
     * Проверяет корректность получения всех типов с определенным статусом
     */
    @Test
    void getWeldingMachineTypesByStatus_ShouldReturnTypes() {
        // Подготавливаем тестовые данные
        List<WeldingMachineType> expectedTypes = Arrays.asList(testWeldingMachineType);
        when(weldingMachineTypeRepository.findByStatus(GeneralStatus.Active)).thenReturn(expectedTypes);

        // Вызываем тестируемый метод
        List<WeldingMachineType> result = weldingMachineTypeService.getWeldingMachineTypesByStatus(GeneralStatus.Active);

        // Проверяем результаты
        assertNotNull(result, "Список типов не должен быть null");
        assertEquals(expectedTypes.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testWeldingMachineType.getStatus(), result.get(0).getStatus(), "Статус должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineTypeRepository, times(1)).findByStatus(GeneralStatus.Active);
    }

    /**
     * Тест поиска типов сварочных машин
     * Проверяет корректность поиска типов по поисковому запросу
     */
    @Test
    void searchWeldingMachineTypes_WithValidTerm_ShouldReturnTypes() {
        // Подготавливаем тестовые данные
        List<WeldingMachineType> expectedTypes = Arrays.asList(testWeldingMachineType);
        when(weldingMachineTypeRepository.searchWeldingMachineTypes("Test")).thenReturn(expectedTypes);

        // Вызываем тестируемый метод
        List<WeldingMachineType> result = weldingMachineTypeService.searchWeldingMachineTypes("Test");

        // Проверяем результаты
        assertNotNull(result, "Список типов не должен быть null");
        assertEquals(expectedTypes.size(), result.size(), "Размер списка должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineTypeRepository, times(1)).searchWeldingMachineTypes("Test");
    }

    /**
     * Тест поиска типов сварочных машин с пустым запросом
     * Проверяет, что при пустом поисковом запросе возвращаются все типы
     */
    @Test
    void searchWeldingMachineTypes_WithEmptyTerm_ShouldReturnAllTypes() {
        // Подготавливаем тестовые данные
        List<WeldingMachineType> expectedTypes = Arrays.asList(testWeldingMachineType);
        when(weldingMachineTypeRepository.findAll()).thenReturn(expectedTypes);

        // Вызываем тестируемый метод
        List<WeldingMachineType> result = weldingMachineTypeService.searchWeldingMachineTypes("");

        // Проверяем результаты
        assertNotNull(result, "Список типов не должен быть null");
        assertEquals(expectedTypes.size(), result.size(), "Размер списка должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineTypeRepository, times(1)).findAll();
    }

    /**
     * Тест создания нового типа сварочной машины
     * Проверяет корректность создания нового типа с валидными данными
     */
    @Test
    void createWeldingMachineType_WithValidData_ShouldCreateType() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.findByName(testWeldingMachineType.getName())).thenReturn(Optional.empty());
        when(weldingMachineTypeRepository.save(any(WeldingMachineType.class))).thenReturn(testWeldingMachineType);

        // Вызываем тестируемый метод
        WeldingMachineType result = weldingMachineTypeService.createWeldingMachineType(testWeldingMachineType);

        // Проверяем результаты
        assertNotNull(result, "Созданный тип не должен быть null");
        assertEquals(testWeldingMachineType.getName(), result.getName(), "Название должно совпадать");
        assertNotNull(result.getDateCreated(), "Дата создания не должна быть null");
        assertEquals(GeneralStatus.Active, result.getStatus(), "Статус должен быть Active");
        
        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineTypeRepository, times(1)).findByName(testWeldingMachineType.getName());
        verify(weldingMachineTypeRepository, times(1)).save(testWeldingMachineType);
    }

    /**
     * Тест создания типа сварочной машины без названия
     * Проверяет, что создание типа без названия вызывает исключение
     */
    @Test
    void createWeldingMachineType_WithoutName_ShouldThrowException() {
        // Подготавливаем тестовые данные
        testWeldingMachineType.setName(null);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineTypeService.createWeldingMachineType(testWeldingMachineType);
        }, "Должно быть выброшено исключение при попытке создать тип без названия");
    }

    /**
     * Тест создания типа сварочной машины с существующим названием
     * Проверяет, что создание типа с существующим названием вызывает исключение
     */
    @Test
    void createWeldingMachineType_WithExistingName_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.findByName(testWeldingMachineType.getName())).thenReturn(Optional.of(testWeldingMachineType));

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineTypeService.createWeldingMachineType(testWeldingMachineType);
        }, "Должно быть выброшено исключение при попытке создать тип с существующим названием");
    }

    /**
     * Тест обновления типа сварочной машины
     * Проверяет корректность обновления существующего типа
     */
    @Test
    void updateWeldingMachineType_WithValidData_ShouldUpdateType() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.findById(testWeldingMachineType.getId())).thenReturn(Optional.of(testWeldingMachineType));
        when(weldingMachineTypeRepository.findByName(testWeldingMachineType.getName())).thenReturn(Optional.of(testWeldingMachineType));
        when(weldingMachineTypeRepository.save(any(WeldingMachineType.class))).thenReturn(testWeldingMachineType);

        // Вызываем тестируемый метод
        WeldingMachineType result = weldingMachineTypeService.updateWeldingMachineType(testWeldingMachineType);

        // Проверяем результаты
        assertNotNull(result, "Обновленный тип не должен быть null");
        assertEquals(testWeldingMachineType.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testWeldingMachineType.getName(), result.getName(), "Название должно совпадать");
        
        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineTypeRepository, times(1)).findById(testWeldingMachineType.getId());
        verify(weldingMachineTypeRepository, times(1)).findByName(testWeldingMachineType.getName());
        verify(weldingMachineTypeRepository, times(1)).save(testWeldingMachineType);
    }

    /**
     * Тест обновления типа сварочной машины без ID
     * Проверяет, что обновление типа без ID вызывает исключение
     */
    @Test
    void updateWeldingMachineType_WithoutId_ShouldThrowException() {
        // Подготавливаем тестовые данные
        testWeldingMachineType.setId(null);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineTypeService.updateWeldingMachineType(testWeldingMachineType);
        }, "Должно быть выброшено исключение при попытке обновить тип без ID");
    }

    /**
     * Тест обновления несуществующего типа сварочной машины
     * Проверяет, что обновление несуществующего типа вызывает исключение
     */
    @Test
    void updateWeldingMachineType_WithNonExistentId_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.findById(testWeldingMachineType.getId())).thenReturn(Optional.empty());

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineTypeService.updateWeldingMachineType(testWeldingMachineType);
        }, "Должно быть выброшено исключение при попытке обновить несуществующий тип");
    }

    /**
     * Тест удаления типа сварочной машины
     * Проверяет корректность удаления существующего типа
     */
    @Test
    void deleteWeldingMachineType_WhenExists_ShouldMarkAsDeleted() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.findById(testWeldingMachineType.getId())).thenReturn(Optional.of(testWeldingMachineType));
        when(weldingMachineTypeRepository.save(any(WeldingMachineType.class))).thenReturn(testWeldingMachineType);

        // Вызываем тестируемый метод
        weldingMachineTypeService.deleteWeldingMachineType(testWeldingMachineType.getId());

        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineTypeRepository, times(1)).findById(testWeldingMachineType.getId());
        verify(weldingMachineTypeRepository, times(1)).save(any(WeldingMachineType.class));
    }

    /**
     * Тест удаления несуществующего типа сварочной машины
     * Проверяет, что удаление несуществующего типа вызывает исключение
     */
    @Test
    void deleteWeldingMachineType_WhenNotExists_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.findById(testWeldingMachineType.getId())).thenReturn(Optional.empty());

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineTypeService.deleteWeldingMachineType(testWeldingMachineType.getId());
        }, "Должно быть выброшено исключение при попытке удалить несуществующий тип");
    }

    /**
     * Тест жесткого удаления типа сварочной машины
     * Проверяет корректность жесткого удаления существующего типа
     */
    @Test
    void hardDeleteWeldingMachineType_WhenExists_ShouldDeleteType() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.existsById(testWeldingMachineType.getId())).thenReturn(true);

        // Вызываем тестируемый метод
        weldingMachineTypeService.hardDeleteWeldingMachineType(testWeldingMachineType.getId());

        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineTypeRepository, times(1)).existsById(testWeldingMachineType.getId());
        verify(weldingMachineTypeRepository, times(1)).deleteById(testWeldingMachineType.getId());
    }

    /**
     * Тест жесткого удаления несуществующего типа сварочной машины
     * Проверяет, что жесткое удаление несуществующего типа вызывает исключение
     */
    @Test
    void hardDeleteWeldingMachineType_WhenNotExists_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineTypeRepository.existsById(testWeldingMachineType.getId())).thenReturn(false);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineTypeService.hardDeleteWeldingMachineType(testWeldingMachineType.getId());
        }, "Должно быть выброшено исключение при попытке жесткого удаления несуществующего типа");
    }
}
