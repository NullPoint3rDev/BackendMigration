package org.alloy.services;

import org.alloy.AlloyServiceTest;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.Organization;
import org.alloy.repositories.OrganizationRepository;
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
 * Тестовый класс для OrganizationService
 * Проверяет корректность работы сервиса организаций
 */
@AlloyServiceTest(OrganizationService.class)
public class OrganizationServiceTest {

    @MockBean
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationService organizationService;

    private Organization testOrganization;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект Organization с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1);
        testOrganization.setName("Test Organization");
        testOrganization.setDescription("Test Description");
        testOrganization.setStatus(GeneralStatus.Active);
        testOrganization.setAddress("Test Address");
        testOrganization.setPhone("1234567890");
        testOrganization.setEmail("test@organization.com");
    }

    /**
     * Тест метода getAllOrganizations()
     * Проверяет корректность получения всех организаций
     */
    @Test
    void getAllOrganizations_ShouldReturnAllOrganizations() {
        // Подготовка данных
        List<Organization> expectedOrganizations = Arrays.asList(testOrganization);
        when(organizationRepository.findByStatusNot(org.alloy.models.GeneralStatus.Deleted)).thenReturn(expectedOrganizations);

        // Выполнение теста
        List<Organization> actualOrganizations = organizationService.getAllOrganizations();

        // Проверка результатов
        assertNotNull(actualOrganizations);
        assertEquals(expectedOrganizations.size(), actualOrganizations.size());
        assertEquals(expectedOrganizations.get(0).getId(), actualOrganizations.get(0).getId());

        // Проверка вызова метода репозитория
        verify(organizationRepository, times(1)).findByStatusNot(org.alloy.models.GeneralStatus.Deleted);
    }

    /**
     * Тест метода getOrganizationById() с существующим ID
     * Проверяет корректность получения организации по существующему ID
     */
    @Test
    void getOrganizationById_WhenExists_ShouldReturnOrganization() {
        // Подготовка данных
        when(organizationRepository.findById(1)).thenReturn(Optional.of(testOrganization));

        // Выполнение теста
        Optional<Organization> result = organizationService.getOrganizationById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testOrganization.getId(), result.get().getId());
        assertEquals(testOrganization.getName(), result.get().getName());

        // Проверка вызова метода репозитория
        verify(organizationRepository, times(1)).findById(1);
    }

    /**
     * Тест метода getOrganizationByName() с существующим именем
     * Проверяет корректность получения организации по существующему имени
     */
    @Test
    void getOrganizationByName_WhenExists_ShouldReturnOrganization() {
        // Подготовка данных
        when(organizationRepository.findByName("Test Organization")).thenReturn(Optional.of(testOrganization));

        // Выполнение теста
        Optional<Organization> result = organizationService.getOrganizationByName("Test Organization");

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testOrganization.getName(), result.get().getName());
        assertEquals(testOrganization.getDescription(), result.get().getDescription());

        // Проверка вызова метода репозитория
        verify(organizationRepository, times(1)).findByName("Test Organization");
    }

    /**
     * Тест метода getOrganizationsByStatus()
     * Проверяет корректность получения организаций по статусу
     */
    @Test
    void getOrganizationsByStatus_ShouldReturnFilteredOrganizations() {
        // Подготовка данных
        List<Organization> expectedOrganizations = Arrays.asList(testOrganization);
        when(organizationRepository.findByStatus(GeneralStatus.Active)).thenReturn(expectedOrganizations);

        // Выполнение теста
        List<Organization> actualOrganizations = organizationService.getOrganizationsByStatus(GeneralStatus.Active);

        // Проверка результатов
        assertNotNull(actualOrganizations);
        assertEquals(expectedOrganizations.size(), actualOrganizations.size());
        assertEquals(GeneralStatus.Active, actualOrganizations.get(0).getStatus());

        // Проверка вызова метода репозитория
        verify(organizationRepository, times(1)).findByStatus(GeneralStatus.Active);
    }

    /**
     * Тест метода searchOrganizations()
     * Проверяет корректность поиска организаций по поисковому запросу
     */
    @Test
    void searchOrganizations_ShouldReturnMatchingOrganizations() {
        // Подготовка данных
        List<Organization> expectedOrganizations = Arrays.asList(testOrganization);
        when(organizationRepository.searchOrganizationsNotDeleted("Test", GeneralStatus.Deleted)).thenReturn(expectedOrganizations);

        // Выполнение теста
        List<Organization> actualOrganizations = organizationService.searchOrganizations("Test");

        // Проверка результатов
        assertNotNull(actualOrganizations);
        assertEquals(expectedOrganizations.size(), actualOrganizations.size());
        assertTrue(actualOrganizations.get(0).getName().contains("Test"));

        // Проверка вызова метода репозитория
        verify(organizationRepository, times(1)).searchOrganizationsNotDeleted("Test", GeneralStatus.Deleted);
    }

    /**
     * Тест метода createOrganization() с валидными данными
     * Проверяет корректность создания новой организации
     */
    @Test
    void createOrganization_WithValidData_ShouldCreateOrganization() {
        // Подготовка данных
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

        // Выполнение теста
        Organization createdOrganization = organizationService.createOrganization(testOrganization);

        // Проверка результатов
        assertNotNull(createdOrganization);
        assertEquals(testOrganization.getName(), createdOrganization.getName());
        assertEquals(testOrganization.getDescription(), createdOrganization.getDescription());
        assertEquals(GeneralStatus.Active, createdOrganization.getStatus());

        // Проверка вызова метода репозитория
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    /**
     * Тест метода createOrganization() с установкой статуса по умолчанию
     * Проверяет корректность установки статуса по умолчанию при создании организации
     */
    @Test
    void createOrganization_WithNullStatus_ShouldSetDefaultStatus() {
        // Подготовка данных
        testOrganization.setStatus(null);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Выполнение теста
        Organization createdOrganization = organizationService.createOrganization(testOrganization);

        // Проверка результатов
        assertNotNull(createdOrganization);
        assertEquals(GeneralStatus.Active, createdOrganization.getStatus());

        // Проверка вызова метода репозитория
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    /**
     * Тест метода updateOrganization()
     * Проверяет корректность обновления существующей организации
     */
    @Test
    void updateOrganization_ShouldUpdateOrganization() {
        // Подготовка данных
        when(organizationRepository.existsById(1)).thenReturn(true);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

        // Выполнение теста
        Organization updatedOrganization = organizationService.updateOrganization(testOrganization);

        // Проверка результатов
        assertNotNull(updatedOrganization);
        assertEquals(testOrganization.getName(), updatedOrganization.getName());

        // Проверка вызовов методов репозитория
        verify(organizationRepository, times(1)).existsById(1);
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    /**
     * Тест метода updateOrganization() с несуществующим ID
     * Проверяет корректность обработки ошибки при обновлении несуществующей организации
     */
    @Test
    void updateOrganization_WithNonExistentId_ShouldThrowException() {
        // Подготовка данных
        when(organizationRepository.existsById(999)).thenReturn(false);

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            organizationService.updateOrganization(testOrganization)
        );
    }

    /**
     * Тест метода deleteOrganization()
     * Проверяет корректность мягкого удаления организации
     */
    @Test
    void deleteOrganization_ShouldSoftDeleteOrganization() {
        // Подготовка данных
        when(organizationRepository.findById(1)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

        // Выполнение теста
        organizationService.deleteOrganization(1);

        // Проверка результатов
        assertEquals(GeneralStatus.Deleted, testOrganization.getStatus());

        // Проверка вызовов методов репозитория
        verify(organizationRepository, times(1)).findById(1);
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    /**
     * Тест метода deleteOrganization() с несуществующим ID
     * Проверяет корректность обработки ошибки при удалении несуществующей организации
     */
    @Test
    void deleteOrganization_WithNonExistentId_ShouldThrowException() {
        // Подготовка данных
        when(organizationRepository.findById(999)).thenReturn(Optional.empty());

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            organizationService.deleteOrganization(999)
        );
    }

    /**
     * Тест метода hardDeleteOrganization()
     * Проверяет корректность полного удаления организации
     */
    @Test
    void hardDeleteOrganization_ShouldDeleteOrganization() {
        // Выполнение теста
        organizationService.hardDeleteOrganization(1);

        // Проверка вызова метода репозитория
        verify(organizationRepository, times(1)).deleteById(1);
    }
}
