package org.alloy.repositories;

import org.alloy.models.entities.Organization;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности OrganizationRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
public class OrganizationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization testOrganization;
    private static final String TEST_NAME = "Test Organization";
    private static final String TEST_DESCRIPTION = "Test Organization Description";
    private static final String TEST_ADDRESS = "Test Address";
    private static final String TEST_PHONE = "+7 (999) 123-45-67";
    private static final String TEST_EMAIL = "test@organization.com";
    private static final String TEST_WEBSITE = "https://test-organization.com";
    private static final String TEST_LOGO = "test-logo.png";
    private static final String TEST_SETTINGS = "{\"setting1\": \"value1\"}";

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовую организацию
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую организацию
        testOrganization = new Organization();
        testOrganization.setName(TEST_NAME);
        testOrganization.setDescription(TEST_DESCRIPTION);
        testOrganization.setAddress(TEST_ADDRESS);
        testOrganization.setPhone(TEST_PHONE);
        testOrganization.setEmail(TEST_EMAIL);
        testOrganization.setWebsite(TEST_WEBSITE);
        testOrganization.setLogo(TEST_LOGO);
        testOrganization.setSettings(TEST_SETTINGS);
        testOrganization.setStatus(GeneralStatus.Active);
        
        // Сохраняем объект в тестовой базе данных
        testOrganization = entityManager.persist(testOrganization);
        entityManager.flush();
    }

    /**
     * Тест сохранения новой организации
     * Проверяет, что организация корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveOrganization() {
        // Создаем новую организацию
        Organization newOrganization = new Organization();
        newOrganization.setName("New Organization");
        newOrganization.setDescription("New Organization Description");
        newOrganization.setStatus(GeneralStatus.Active);
        
        // Сохраняем организацию через репозиторий
        Organization savedOrganization = organizationRepository.save(newOrganization);
        
        // Проверяем, что организация была сохранена
        assertNotNull(savedOrganization, "Сохраненная организация не должна быть null");
        assertNotNull(savedOrganization.getId(), "ID сохраненной организации не должен быть null");
        assertNotNull(savedOrganization.getDateCreated(), "Дата создания не должна быть null");
        assertEquals(newOrganization.getName(), savedOrganization.getName(), 
            "Название должно совпадать");
        assertEquals(newOrganization.getDescription(), savedOrganization.getDescription(), 
            "Описание должно совпадать");
        assertEquals(newOrganization.getStatus(), savedOrganization.getStatus(), 
            "Статус должен совпадать");
        
        // Проверяем, что организацию можно найти в базе данных
        Organization foundOrganization = entityManager.find(Organization.class, savedOrganization.getId());
        assertNotNull(foundOrganization, "Организация должна быть найдена в базе данных");
    }

    /**
     * Тест поиска организации по названию
     * Проверяет, что метод findByName возвращает организацию с указанным названием
     */
    @Test
    void findByName_ShouldReturnOrganization() {
        // Ищем организацию по названию
        Optional<Organization> foundOrganization = organizationRepository.findByName(TEST_NAME);
        
        // Проверяем результаты
        assertTrue(foundOrganization.isPresent(), "Организация должна быть найдена");
        assertEquals(TEST_NAME, foundOrganization.get().getName(), 
            "Название найденной организации должно совпадать");
        assertEquals(TEST_DESCRIPTION, foundOrganization.get().getDescription(), 
            "Описание найденной организации должно совпадать");
    }

    /**
     * Тест поиска организаций по статусу
     * Проверяет, что метод findByStatus возвращает все организации с указанным статусом
     */
    @Test
    void findByStatus_ShouldReturnOrganizationsByStatus() {
        // Создаем вторую активную организацию
        Organization secondOrganization = new Organization();
        secondOrganization.setName("Second Organization");
        secondOrganization.setDescription("Second Organization Description");
        secondOrganization.setStatus(GeneralStatus.Active);
        entityManager.persist(secondOrganization);
        
        // Создаем неактивную организацию
        Organization inactiveOrganization = new Organization();
        inactiveOrganization.setName("Inactive Organization");
        inactiveOrganization.setDescription("Inactive Organization Description");
        inactiveOrganization.setStatus(GeneralStatus.Inactive);
        entityManager.persist(inactiveOrganization);
        entityManager.flush();
        
        // Получаем активные организации
        List<Organization> activeOrganizations = organizationRepository.findByStatus(GeneralStatus.Active);
        
        // Проверяем результаты
        assertNotNull(activeOrganizations, "Список организаций не должен быть null");
        assertTrue(activeOrganizations.size() >= 2, "Должно быть найдено как минимум 2 активные организации");
        assertTrue(activeOrganizations.stream().allMatch(o -> o.getStatus() == GeneralStatus.Active), 
            "Все найденные организации должны быть активными");
    }

    /**
     * Тест поиска организаций по поисковому запросу
     * Проверяет, что метод searchOrganizations возвращает организации, соответствующие поисковому запросу
     */
    @Test
    void searchOrganizations_ShouldReturnMatchingOrganizations() {
        // Создаем организации с разными названиями и описаниями
        Organization alphaOrg = new Organization();
        alphaOrg.setName("Alpha Organization");
        alphaOrg.setDescription("Alpha Description");
        alphaOrg.setStatus(GeneralStatus.Active);
        entityManager.persist(alphaOrg);
        
        Organization betaOrg = new Organization();
        betaOrg.setName("Beta Company");
        betaOrg.setDescription("Beta Organization Description");
        betaOrg.setStatus(GeneralStatus.Active);
        entityManager.persist(betaOrg);
        entityManager.flush();
        
        // Ищем организации по частичному совпадению
        List<Organization> searchResults = organizationRepository.searchOrganizations("Organization");
        
        // Проверяем результаты
        assertNotNull(searchResults, "Список результатов поиска не должен быть null");
        assertTrue(searchResults.size() >= 2, "Должно быть найдено как минимум 2 организации");
        assertTrue(searchResults.stream().anyMatch(o -> o.getName().equals("Alpha Organization")), 
            "Результаты должны содержать Alpha Organization");
        assertTrue(searchResults.stream().anyMatch(o -> o.getName().equals("Beta Company")), 
            "Результаты должны содержать Beta Company");
    }

    /**
     * Тест поиска организаций по списку ID
     * Проверяет, что метод findByIds возвращает все организации с указанными ID
     */
    @Test
    void findByIds_ShouldReturnOrganizationsByIds() {
        // Создаем вторую организацию
        final Organization secondOrganization = new Organization();
        secondOrganization.setName("Second Organization");
        secondOrganization.setDescription("Second Organization Description");
        secondOrganization.setStatus(GeneralStatus.Active);
        entityManager.persist(secondOrganization);
        entityManager.flush();
        
        // Получаем организации по списку ID
        List<Organization> foundOrganizations = organizationRepository.findByIds(
            Arrays.asList(testOrganization.getId(), secondOrganization.getId())
        );
        
        // Проверяем результаты
        assertNotNull(foundOrganizations, "Список организаций не должен быть null");
        assertEquals(2, foundOrganizations.size(), "Должно быть найдено 2 организации");
        assertTrue(foundOrganizations.stream().anyMatch(o -> o.getId().equals(testOrganization.getId())), 
            "Результаты должны содержать первую тестовую организацию");
        assertTrue(foundOrganizations.stream().anyMatch(o -> o.getId().equals(secondOrganization.getId())), 
            "Результаты должны содержать вторую тестовую организацию");
    }

    /**
     * Тест обновления организации
     * Проверяет, что организация корректно обновляется в базе данных
     */
    @Test
    void update_ShouldUpdateOrganization() {
        // Обновляем данные организации
        String updatedName = "Updated Organization Name";
        String updatedDescription = "Updated Organization Description";
        testOrganization.setName(updatedName);
        testOrganization.setDescription(updatedDescription);
        
        // Сохраняем изменения
        Organization updatedOrganization = organizationRepository.save(testOrganization);
        
        // Проверяем результаты
        assertNotNull(updatedOrganization, "Обновленная организация не должна быть null");
        assertEquals(updatedName, updatedOrganization.getName(), 
            "Название должно быть обновлено");
        assertEquals(updatedDescription, updatedOrganization.getDescription(), 
            "Описание должно быть обновлено");
        
        // Проверяем, что изменения сохранились в базе данных
        Organization foundOrganization = entityManager.find(Organization.class, testOrganization.getId());
        assertEquals(updatedName, foundOrganization.getName(), 
            "Название должно быть обновлено в базе данных");
        assertEquals(updatedDescription, foundOrganization.getDescription(), 
            "Описание должно быть обновлено в базе данных");
    }

    /**
     * Тест удаления организации
     * Проверяет, что организация корректно удаляется из базы данных
     */
    @Test
    void delete_ShouldDeleteOrganization() {
        // Удаляем организацию
        organizationRepository.delete(testOrganization);
        
        // Проверяем, что организация была удалена
        Organization deletedOrganization = entityManager.find(Organization.class, testOrganization.getId());
        assertNull(deletedOrganization, "Организация должна быть удалена из базы данных");
    }
}
