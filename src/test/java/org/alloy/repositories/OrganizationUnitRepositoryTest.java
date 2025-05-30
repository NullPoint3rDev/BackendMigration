package org.alloy.repositories;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.Organization;
import org.alloy.models.entities.OrganizationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности OrganizationUnitRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
public class OrganizationUnitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization testOrganization;
    private OrganizationUnit testUnit;
    private static final String TEST_NAME = "Test Unit";
    private static final String TEST_DESCRIPTION = "Test Unit Description";
    private static final String TEST_ADDRESS = "Test Address";
    private static final String TEST_PHONE = "+7 (999) 123-45-67";
    private static final String TEST_EMAIL = "test@unit.com";

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовую организацию и подразделение
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую организацию
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setDescription("Test Organization Description");
        testOrganization.setStatus(GeneralStatus.Active);
        testOrganization = entityManager.persist(testOrganization);
        entityManager.flush();

        // Создаем тестовое подразделение
        testUnit = new OrganizationUnit();
        testUnit.setName(TEST_NAME);
        testUnit.setDescription(TEST_DESCRIPTION);
        testUnit.setAddress(TEST_ADDRESS);
        testUnit.setPhone(TEST_PHONE);
        testUnit.setEmail(TEST_EMAIL);
        testUnit.setOrganizationId(testOrganization.getId());
        testUnit.setStatus(GeneralStatus.Active);
        
        // Сохраняем объект в тестовой базе данных
        testUnit = entityManager.persist(testUnit);
        entityManager.flush();
    }

    /**
     * Тест сохранения нового подразделения
     * Проверяет, что подразделение корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveOrganizationUnit() {
        // Создаем новое подразделение
        OrganizationUnit newUnit = new OrganizationUnit();
        newUnit.setName("New Unit");
        newUnit.setDescription("New Unit Description");
        newUnit.setOrganizationId(testOrganization.getId());
        newUnit.setStatus(GeneralStatus.Active);
        
        // Сохраняем подразделение через репозиторий
        OrganizationUnit savedUnit = organizationUnitRepository.save(newUnit);
        
        // Проверяем, что подразделение было сохранено
        assertNotNull(savedUnit, "Сохраненное подразделение не должно быть null");
        assertNotNull(savedUnit.getId(), "ID сохраненного подразделения не должен быть null");
        assertNotNull(savedUnit.getDateCreated(), "Дата создания не должна быть null");
        assertEquals(newUnit.getName(), savedUnit.getName(), 
            "Название должно совпадать");
        assertEquals(newUnit.getDescription(), savedUnit.getDescription(), 
            "Описание должно совпадать");
        assertEquals(newUnit.getStatus(), savedUnit.getStatus(), 
            "Статус должен совпадать");
        
        // Проверяем, что подразделение можно найти в базе данных
        OrganizationUnit foundUnit = entityManager.find(OrganizationUnit.class, savedUnit.getId());
        assertNotNull(foundUnit, "Подразделение должно быть найдено в базе данных");
    }

    /**
     * Тест поиска подразделений по ID организации
     * Проверяет, что метод findByOrganizationId возвращает все подразделения указанной организации
     */
    @Test
    void findByOrganizationId_ShouldReturnOrganizationUnits() {
        // Создаем второе подразделение в той же организации
        OrganizationUnit secondUnit = new OrganizationUnit();
        secondUnit.setName("Second Unit");
        secondUnit.setDescription("Second Unit Description");
        secondUnit.setOrganizationId(testOrganization.getId());
        secondUnit.setStatus(GeneralStatus.Active);
        entityManager.persist(secondUnit);
        
        // Создаем подразделение в другой организации
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Organization");
        otherOrg.setDescription("Other Organization Description");
        otherOrg.setStatus(GeneralStatus.Active);
        otherOrg = entityManager.persist(otherOrg);
        
        OrganizationUnit otherOrgUnit = new OrganizationUnit();
        otherOrgUnit.setName("Other Org Unit");
        otherOrgUnit.setDescription("Other Org Unit Description");
        otherOrgUnit.setOrganizationId(otherOrg.getId());
        otherOrgUnit.setStatus(GeneralStatus.Active);
        entityManager.persist(otherOrgUnit);
        entityManager.flush();
        
        // Получаем подразделения первой организации
        List<OrganizationUnit> units = organizationUnitRepository.findByOrganizationId(testOrganization.getId());
        
        // Проверяем результаты
        assertNotNull(units, "Список подразделений не должен быть null");
        assertEquals(2, units.size(), "Должно быть найдено 2 подразделения");
        assertTrue(units.stream().allMatch(u -> u.getOrganizationId().equals(testOrganization.getId())), 
            "Все найденные подразделения должны принадлежать указанной организации");
    }

    /**
     * Тест поиска подразделений по ID родительского подразделения
     * Проверяет, что метод findByParentId возвращает все дочерние подразделения
     */
    @Test
    void findByParentId_ShouldReturnChildUnits() {
        // Создаем родительское подразделение
        final OrganizationUnit parentUnit = new OrganizationUnit();
        parentUnit.setName("Parent Unit");
        parentUnit.setDescription("Parent Unit Description");
        parentUnit.setOrganizationId(testOrganization.getId());
        parentUnit.setStatus(GeneralStatus.Active);
        entityManager.persist(parentUnit);
        
        // Создаем дочерние подразделения
        OrganizationUnit childUnit1 = new OrganizationUnit();
        childUnit1.setName("Child Unit 1");
        childUnit1.setDescription("Child Unit 1 Description");
        childUnit1.setOrganizationId(testOrganization.getId());
        childUnit1.setStatus(GeneralStatus.Active);
        childUnit1.setParentId(parentUnit.getId());
        entityManager.persist(childUnit1);
        
        OrganizationUnit childUnit2 = new OrganizationUnit();
        childUnit2.setName("Child Unit 2");
        childUnit2.setDescription("Child Unit 2 Description");
        childUnit2.setOrganizationId(testOrganization.getId());
        childUnit2.setStatus(GeneralStatus.Active);
        childUnit2.setParentId(parentUnit.getId());
        entityManager.persist(childUnit2);
        entityManager.flush();
        
        // Получаем дочерние подразделения
        List<OrganizationUnit> childUnits = organizationUnitRepository.findByParentId(parentUnit.getId());
        
        // Проверяем результаты
        assertNotNull(childUnits, "Список дочерних подразделений не должен быть null");
        assertEquals(2, childUnits.size(), "Должно быть найдено 2 дочерних подразделения");
        assertTrue(childUnits.stream().allMatch(u -> u.getParentId().equals(parentUnit.getId())), 
            "Все найденные подразделения должны быть дочерними для указанного родительского подразделения");
    }

    /**
     * Тест поиска подразделения по имени и ID организации
     * Проверяет, что метод findByNameAndOrganizationId возвращает подразделение с указанным именем
     */
    @Test
    void findByNameAndOrganizationId_ShouldReturnOrganizationUnit() {
        // Ищем подразделение по имени и ID организации
        Optional<OrganizationUnit> foundUnit = organizationUnitRepository.findByNameAndOrganizationId(
            TEST_NAME, testOrganization.getId());
        
        // Проверяем результаты
        assertTrue(foundUnit.isPresent(), "Подразделение должно быть найдено");
        assertEquals(TEST_NAME, foundUnit.get().getName(), 
            "Название найденного подразделения должно совпадать");
        assertEquals(testOrganization.getId(), foundUnit.get().getOrganizationId(), 
            "ID организации должен совпадать");
    }

    /**
     * Тест поиска подразделений по поисковому запросу
     * Проверяет, что метод searchOrganizationUnits возвращает подразделения, соответствующие поисковому запросу
     */
    @Test
    void searchOrganizationUnits_ShouldReturnMatchingUnits() {
        // Создаем подразделения с разными названиями и описаниями
        OrganizationUnit alphaUnit = new OrganizationUnit();
        alphaUnit.setName("Alpha Unit");
        alphaUnit.setDescription("Alpha Description");
        alphaUnit.setOrganizationId(testOrganization.getId());
        alphaUnit.setStatus(GeneralStatus.Active);
        entityManager.persist(alphaUnit);
        
        OrganizationUnit betaUnit = new OrganizationUnit();
        betaUnit.setName("Beta Unit");
        betaUnit.setDescription("Beta Description");
        betaUnit.setOrganizationId(testOrganization.getId());
        betaUnit.setStatus(GeneralStatus.Active);
        entityManager.persist(betaUnit);
        entityManager.flush();
        
        // Ищем подразделения по частичному совпадению
        List<OrganizationUnit> searchResults = organizationUnitRepository.searchOrganizationUnits(
            testOrganization.getId(), "Unit");
        
        // Проверяем результаты
        assertNotNull(searchResults, "Список результатов поиска не должен быть null");
        assertTrue(searchResults.size() >= 3, "Должно быть найдено как минимум 3 подразделения");
        assertTrue(searchResults.stream().anyMatch(u -> u.getName().equals("Alpha Unit")), 
            "Результаты должны содержать Alpha Unit");
        assertTrue(searchResults.stream().anyMatch(u -> u.getName().equals("Beta Unit")), 
            "Результаты должны содержать Beta Unit");
    }

    /**
     * Тест поиска подразделений по списку ID
     * Проверяет, что метод findByIds возвращает все подразделения с указанными ID
     */
    @Test
    void findByIds_ShouldReturnOrganizationUnitsByIds() {
        // Создаем второе подразделение
        OrganizationUnit secondUnit = new OrganizationUnit();
        secondUnit.setName("Second Unit");
        secondUnit.setDescription("Second Unit Description");
        secondUnit.setOrganizationId(testOrganization.getId());
        secondUnit.setStatus(GeneralStatus.Active);
        entityManager.persist(secondUnit);
        entityManager.flush();
        
        // Получаем подразделения по списку ID
        List<OrganizationUnit> foundUnits = organizationUnitRepository.findByIds(
            Arrays.asList(testUnit.getId(), secondUnit.getId())
        );
        
        // Проверяем результаты
        assertNotNull(foundUnits, "Список подразделений не должен быть null");
        assertEquals(2, foundUnits.size(), "Должно быть найдено 2 подразделения");
        assertTrue(foundUnits.stream().anyMatch(u -> u.getId().equals(testUnit.getId())), 
            "Результаты должны содержать первое тестовое подразделение");
        assertTrue(foundUnits.stream().anyMatch(u -> u.getId().equals(secondUnit.getId())), 
            "Результаты должны содержать второе тестовое подразделение");
    }

    /**
     * Тест обновления подразделения
     * Проверяет, что подразделение корректно обновляется в базе данных
     */
    @Test
    void update_ShouldUpdateOrganizationUnit() {
        // Обновляем данные подразделения
        String updatedName = "Updated Unit Name";
        String updatedDescription = "Updated Unit Description";
        testUnit.setName(updatedName);
        testUnit.setDescription(updatedDescription);
        
        // Сохраняем изменения
        OrganizationUnit updatedUnit = organizationUnitRepository.save(testUnit);
        
        // Проверяем результаты
        assertNotNull(updatedUnit, "Обновленное подразделение не должно быть null");
        assertEquals(updatedName, updatedUnit.getName(), 
            "Название должно быть обновлено");
        assertEquals(updatedDescription, updatedUnit.getDescription(), 
            "Описание должно быть обновлено");
        
        // Проверяем, что изменения сохранились в базе данных
        OrganizationUnit foundUnit = entityManager.find(OrganizationUnit.class, testUnit.getId());
        assertEquals(updatedName, foundUnit.getName(), 
            "Название должно быть обновлено в базе данных");
        assertEquals(updatedDescription, foundUnit.getDescription(), 
            "Описание должно быть обновлено в базе данных");
    }

    /**
     * Тест удаления подразделения
     * Проверяет, что подразделение корректно удаляется из базы данных
     */
    @Test
    void delete_ShouldDeleteOrganizationUnit() {
        // Удаляем подразделение
        organizationUnitRepository.delete(testUnit);
        
        // Проверяем, что подразделение было удалено
        OrganizationUnit deletedUnit = entityManager.find(OrganizationUnit.class, testUnit.getId());
        assertNull(deletedUnit, "Подразделение должно быть удалено из базы данных");
    }
}
