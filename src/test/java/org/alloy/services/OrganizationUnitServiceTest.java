package org.alloy.services;

import org.alloy.AlloyServiceTest;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.repositories.OrganizationUnitRepository;
import org.alloy.repositories.WelderRepository;
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
 * Тестовый класс для OrganizationUnitService
 * Проверяет корректность работы сервиса подразделений организаций
 */
@AlloyServiceTest(OrganizationUnitService.class)
public class OrganizationUnitServiceTest {

    @MockBean
    private OrganizationUnitRepository organizationUnitRepository;

    @MockBean
    private WelderRepository welderRepository;

    @Autowired
    private OrganizationUnitService organizationUnitService;

    private OrganizationUnit testOrganizationUnit;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект OrganizationUnit с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testOrganizationUnit = new OrganizationUnit();
        testOrganizationUnit.setId(1);
        testOrganizationUnit.setName("Test Unit");
        testOrganizationUnit.setDescription("Test Description");
        testOrganizationUnit.setStatus(GeneralStatus.Active);
        testOrganizationUnit.setOrganizationId(1);
        testOrganizationUnit.setParentId(null);
    }

    /**
     * Тест метода getAllOrganizationUnits()
     * Проверяет корректность получения всех подразделений
     */
    @Test
    void getAllOrganizationUnits_ShouldReturnAllUnits() {
        // Подготовка данных
        List<OrganizationUnit> expectedUnits = Arrays.asList(testOrganizationUnit);
        when(organizationUnitRepository.findByStatus(GeneralStatus.Active)).thenReturn(expectedUnits);

        // Выполнение теста
        List<OrganizationUnit> actualUnits = organizationUnitService.getAllOrganizationUnits();

        // Проверка результатов
        assertNotNull(actualUnits);
        assertEquals(expectedUnits.size(), actualUnits.size());
        assertEquals(expectedUnits.get(0).getId(), actualUnits.get(0).getId());

        // Проверка вызова метода репозитория
        verify(organizationUnitRepository, times(1)).findByStatus(GeneralStatus.Active);
    }

    /**
     * Тест метода getOrganizationUnitById() с существующим ID
     * Проверяет корректность получения подразделения по существующему ID
     */
    @Test
    void getOrganizationUnitById_WhenExists_ShouldReturnUnit() {
        // Подготовка данных
        when(organizationUnitRepository.findById(1)).thenReturn(Optional.of(testOrganizationUnit));

        // Выполнение теста
        Optional<OrganizationUnit> result = organizationUnitService.getOrganizationUnitById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testOrganizationUnit.getId(), result.get().getId());
        assertEquals(testOrganizationUnit.getName(), result.get().getName());

        // Проверка вызова метода репозитория
        verify(organizationUnitRepository, times(1)).findById(1);
    }

    /**
     * Тест метода getOrganizationUnitsByOrganizationId()
     * Проверяет корректность получения подразделений по ID организации
     */
    @Test
    void getOrganizationUnitsByOrganizationId_ShouldReturnOrganizationUnits() {
        // Подготовка данных
        List<OrganizationUnit> expectedUnits = Arrays.asList(testOrganizationUnit);
        when(organizationUnitRepository.findByOrganizationIdAndStatus(1, GeneralStatus.Active)).thenReturn(expectedUnits);

        // Выполнение теста
        List<OrganizationUnit> actualUnits = organizationUnitService.getOrganizationUnitsByOrganizationId(1);

        // Проверка результатов
        assertNotNull(actualUnits);
        assertEquals(expectedUnits.size(), actualUnits.size());
        assertEquals(expectedUnits.get(0).getOrganizationId(), actualUnits.get(0).getOrganizationId());

        // Проверка вызова метода репозитория
        verify(organizationUnitRepository, times(1)).findByOrganizationIdAndStatus(1, GeneralStatus.Active);
    }

    /**
     * Тест метода getOrganizationUnitsByParentId()
     * Проверяет корректность получения подразделений по ID родительского подразделения
     */
    @Test
    void getOrganizationUnitsByParentId_ShouldReturnChildUnits() {
        // Подготовка данных
        List<OrganizationUnit> expectedUnits = Arrays.asList(testOrganizationUnit);
        when(organizationUnitRepository.findByParentId(1)).thenReturn(expectedUnits);

        // Выполнение теста
        List<OrganizationUnit> actualUnits = organizationUnitService.getOrganizationUnitsByParentId(1);

        // Проверка результатов
        assertNotNull(actualUnits);
        assertEquals(expectedUnits.size(), actualUnits.size());
        assertEquals(expectedUnits.get(0).getParentId(), actualUnits.get(0).getParentId());

        // Проверка вызова метода репозитория
        verify(organizationUnitRepository, times(1)).findByParentId(1);
    }

    /**
     * Тест метода getOrganizationUnitByNameAndOrganizationId()
     * Проверяет корректность получения подразделения по имени и ID организации
     */
    @Test
    void getOrganizationUnitByNameAndOrganizationId_ShouldReturnUnit() {
        // Подготовка данных
        when(organizationUnitRepository.findByNameAndOrganizationId("Test Unit", 1))
                .thenReturn(Optional.of(testOrganizationUnit));

        // Выполнение теста
        Optional<OrganizationUnit> result = organizationUnitService
                .getOrganizationUnitByNameAndOrganizationId("Test Unit", 1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testOrganizationUnit.getName(), result.get().getName());
        assertEquals(testOrganizationUnit.getOrganizationId(), result.get().getOrganizationId());

        // Проверка вызова метода репозитория
        verify(organizationUnitRepository, times(1)).findByNameAndOrganizationId("Test Unit", 1);
    }

    /**
     * Тест метода searchOrganizationUnits()
     * Проверяет корректность поиска подразделений по ID организации и поисковому запросу
     */
    @Test
    void searchOrganizationUnits_ShouldReturnMatchingUnits() {
        // Подготовка данных
        List<OrganizationUnit> expectedUnits = Arrays.asList(testOrganizationUnit);
        when(organizationUnitRepository.searchOrganizationUnits(1, "Test")).thenReturn(expectedUnits);

        // Выполнение теста
        List<OrganizationUnit> actualUnits = organizationUnitService.searchOrganizationUnits(1, "Test");

        // Проверка результатов
        assertNotNull(actualUnits);
        assertEquals(expectedUnits.size(), actualUnits.size());
        assertTrue(actualUnits.get(0).getName().contains("Test"));

        // Проверка вызова метода репозитория
        verify(organizationUnitRepository, times(1)).searchOrganizationUnits(1, "Test");
    }

    /**
     * Тест метода createOrganizationUnit() с валидными данными
     * Проверяет корректность создания нового подразделения
     */
    @Test
    void createOrganizationUnit_WithValidData_ShouldCreateUnit() {
        // Подготовка данных
        when(organizationUnitRepository.findByNameAndOrganizationIdAndStatus("Test Unit", 1, GeneralStatus.Active))
                .thenReturn(Optional.empty());
        when(organizationUnitRepository.save(any(OrganizationUnit.class))).thenReturn(testOrganizationUnit);

        // Выполнение теста
        OrganizationUnit createdUnit = organizationUnitService.createOrganizationUnit(testOrganizationUnit);

        // Проверка результатов
        assertNotNull(createdUnit);
        assertEquals(testOrganizationUnit.getName(), createdUnit.getName());
        assertEquals(testOrganizationUnit.getOrganizationId(), createdUnit.getOrganizationId());
        assertEquals(GeneralStatus.Active, createdUnit.getStatus());

        // Проверка вызовов методов репозитория
        verify(organizationUnitRepository, times(1))
                .findByNameAndOrganizationIdAndStatus("Test Unit", 1, GeneralStatus.Active);
        verify(organizationUnitRepository, times(1)).save(any(OrganizationUnit.class));
    }

    /**
     * Тест метода createOrganizationUnit() с существующим именем
     * Проверяет корректность обработки ошибки при создании подразделения с существующим именем
     */
    @Test
    void createOrganizationUnit_WithExistingName_ShouldThrowException() {
        // Подготовка данных
        when(organizationUnitRepository.findByNameAndOrganizationIdAndStatus("Test Unit", 1, GeneralStatus.Active))
                .thenReturn(Optional.of(testOrganizationUnit));

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            organizationUnitService.createOrganizationUnit(testOrganizationUnit)
        );
    }

    /**
     * Тест метода updateOrganizationUnit()
     * Проверяет корректность обновления существующего подразделения
     */
    @Test
    void updateOrganizationUnit_ShouldUpdateUnit() {
        // Подготовка данных
        when(organizationUnitRepository.existsById(1)).thenReturn(true);
        when(organizationUnitRepository.findByNameAndOrganizationIdAndStatus("Test Unit", 1, GeneralStatus.Active))
                .thenReturn(Optional.empty());
        when(organizationUnitRepository.save(any(OrganizationUnit.class))).thenReturn(testOrganizationUnit);

        // Выполнение теста
        OrganizationUnit updatedUnit = organizationUnitService.updateOrganizationUnit(testOrganizationUnit);

        // Проверка результатов
        assertNotNull(updatedUnit);
        assertEquals(testOrganizationUnit.getName(), updatedUnit.getName());

        // Проверка вызовов методов репозитория
        verify(organizationUnitRepository, times(1)).existsById(1);
        verify(organizationUnitRepository, times(1))
                .findByNameAndOrganizationIdAndStatus("Test Unit", 1, GeneralStatus.Active);
        verify(organizationUnitRepository, times(1)).save(any(OrganizationUnit.class));
    }

    /**
     * Тест метода updateOrganizationUnit() с несуществующим ID
     * Проверяет корректность обработки ошибки при обновлении несуществующего подразделения
     */
    @Test
    void updateOrganizationUnit_WithNonExistentId_ShouldThrowException() {
        // Подготовка данных
        when(organizationUnitRepository.existsById(999)).thenReturn(false);

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            organizationUnitService.updateOrganizationUnit(testOrganizationUnit)
        );
    }

    /**
     * Тест метода deleteOrganizationUnit()
     * Проверяет корректность мягкого удаления подразделения
     */
    @Test
    void deleteOrganizationUnit_ShouldSoftDeleteUnit() {
        // Подготовка данных
        when(organizationUnitRepository.findById(1)).thenReturn(Optional.of(testOrganizationUnit));
        when(organizationUnitRepository.save(any(OrganizationUnit.class))).thenReturn(testOrganizationUnit);

        // Выполнение теста
        organizationUnitService.deleteOrganizationUnit(1);

        // Проверка результатов
        assertEquals(GeneralStatus.Deleted, testOrganizationUnit.getStatus());

        // Проверка вызовов методов репозитория
        verify(organizationUnitRepository, times(1)).findById(1);
        verify(organizationUnitRepository, times(1)).save(any(OrganizationUnit.class));
    }

    /**
     * Тест метода deleteOrganizationUnit() с несуществующим ID
     * Проверяет корректность обработки ошибки при удалении несуществующего подразделения
     */
    @Test
    void deleteOrganizationUnit_WithNonExistentId_ShouldThrowException() {
        // Подготовка данных
        when(organizationUnitRepository.findById(999)).thenReturn(Optional.empty());

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            organizationUnitService.deleteOrganizationUnit(999)
        );
    }

    /**
     * Тест метода hardDeleteOrganizationUnit()
     * Проверяет корректность полного удаления подразделения
     */
    @Test
    void hardDeleteOrganizationUnit_ShouldDeleteUnit() {
        // Выполнение теста
        organizationUnitService.hardDeleteOrganizationUnit(1);

        // Проверка вызова метода репозитория
        verify(organizationUnitRepository, times(1)).deleteById(1);
    }
}
