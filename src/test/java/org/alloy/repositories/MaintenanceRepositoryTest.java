package org.alloy.repositories;

import org.alloy.models.entities.Maintenance;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRole;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.Organization;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности MaintenanceRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
public class MaintenanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    private Maintenance testMaintenance;
    private WeldingMachine testMachine;
    private UserAccount testUser;
    private UserRole testRole;
    private OrganizationUnit testOrgUnit;
    private Organization testOrg;
    private WeldingMachineType testMachineType;
    private static final String TEST_TYPE = "PLANNED";
    private static final String TEST_DESCRIPTION = "Test Maintenance";
    private static final String TEST_NOTES = "Test Notes";
    private static final String TEST_RESULT = "Test Result";

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовые объекты Organization, OrganizationUnit, UserRole, UserAccount, 
     * WeldingMachineType, WeldingMachine и Maintenance
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую организацию
        testOrg = new Organization();
        testOrg.setName("Test Organization");
        testOrg.setStatus(GeneralStatus.Active);
        testOrg = entityManager.persist(testOrg);
        entityManager.flush();

        // Создаем тестовую организационную единицу
        testOrgUnit = new OrganizationUnit();
        testOrgUnit.setName("Test Organization Unit");
        testOrgUnit.setStatus(GeneralStatus.Active);
        testOrgUnit.setOrganizationId(testOrg.getId());
        testOrgUnit = entityManager.persist(testOrgUnit);
        entityManager.flush();

        // Создаем тестовый тип сварочной машины
        testMachineType = new WeldingMachineType();
        testMachineType.setName("Test Machine Type");
        testMachineType.setDescription("Test Machine Type Description");
        testMachineType.setStatus(GeneralStatus.Active);
        testMachineType = entityManager.persist(testMachineType);
        entityManager.flush();

        // Создаем тестовую роль
        testRole = new UserRole();
        testRole.setName("Test Role");
        testRole.setDescription("Test Role Description");
        testRole.setStatus(GeneralStatus.Active);
        testRole = entityManager.persist(testRole);
        entityManager.flush();

        // Создаем тестового пользователя
        testUser = new UserAccount();
        testUser.setUserName("testUser");
        testUser.setEmail("test@example.com");
        testUser.setStatus(GeneralStatus.Active);
        testUser.setUserRoleId(testRole.getId());
        testUser = entityManager.persist(testUser);
        entityManager.flush();

        // Создаем тестовую сварочную машину
        testMachine = new WeldingMachine();
        testMachine.setName("Test Machine");
        testMachine.setStatus(GeneralStatus.Active);
        testMachine.setOrganizationUnitId(testOrgUnit.getId());
        testMachine.setWeldingMachineTypeId(testMachineType.getId()); // Устанавливаем ID типа машины
        testMachine = entityManager.persist(testMachine);
        entityManager.flush();
        
        // Создаем тестовое обслуживание
        testMaintenance = new Maintenance();
        testMaintenance.setWeldingMachineId(testMachine.getId());
        testMaintenance.setUserAccountId(testUser.getId());
        testMaintenance.setStatus(GeneralStatus.Active);
        testMaintenance.setDatePlanned(LocalDateTime.now().plusDays(1));
        testMaintenance.setDescription(TEST_DESCRIPTION);
        testMaintenance.setNotes(TEST_NOTES);
        testMaintenance.setType(TEST_TYPE);
        testMaintenance.setResult(TEST_RESULT);
        
        // Сохраняем объект в тестовой базе данных
        testMaintenance = entityManager.persist(testMaintenance);
        entityManager.flush();
    }

    /**
     * Тест сохранения нового обслуживания
     * Проверяет, что обслуживание корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveMaintenance() {
        // Создаем новое обслуживание
        Maintenance newMaintenance = new Maintenance();
        newMaintenance.setWeldingMachineId(testMachine.getId());
        newMaintenance.setUserAccountId(testUser.getId());
        newMaintenance.setStatus(GeneralStatus.Active);
        newMaintenance.setDatePlanned(LocalDateTime.now().plusDays(2));
        newMaintenance.setDescription("New Maintenance");
        newMaintenance.setNotes("New Notes");
        newMaintenance.setType(TEST_TYPE);
        
        // Сохраняем обслуживание через репозиторий
        Maintenance savedMaintenance = maintenanceRepository.save(newMaintenance);
        
        // Проверяем, что обслуживание было сохранено
        assertNotNull(savedMaintenance, "Сохраненное обслуживание не должно быть null");
        assertNotNull(savedMaintenance.getId(), "ID сохраненного обслуживания не должен быть null");
        assertNotNull(savedMaintenance.getDateCreated(), "Дата создания не должна быть null");
        assertEquals(newMaintenance.getWeldingMachineId(), savedMaintenance.getWeldingMachineId(), 
            "ID сварочной машины должен совпадать");
        assertEquals(newMaintenance.getUserAccountId(), savedMaintenance.getUserAccountId(), 
            "ID пользователя должен совпадать");
        assertEquals(newMaintenance.getStatus(), savedMaintenance.getStatus(), 
            "Статус должен совпадать");
        assertEquals(newMaintenance.getDescription(), savedMaintenance.getDescription(), 
            "Описание должно совпадать");
        assertEquals(newMaintenance.getNotes(), savedMaintenance.getNotes(), 
            "Заметки должны совпадать");
        assertEquals(newMaintenance.getType(), savedMaintenance.getType(), 
            "Тип должен совпадать");
        
        // Проверяем, что обслуживание можно найти в базе данных
        Maintenance foundMaintenance = entityManager.find(Maintenance.class, savedMaintenance.getId());
        assertNotNull(foundMaintenance, "Обслуживание должно быть найдено в базе данных");
    }

    /**
     * Тест поиска обслуживаний по ID сварочной машины
     * Проверяет, что метод findByWeldingMachineId возвращает все обслуживания для машины
     */
    @Test
    void findByWeldingMachineId_ShouldReturnMachineMaintenances() {
        // Создаем второе обслуживание для той же машины
        Maintenance secondMaintenance = new Maintenance();
        secondMaintenance.setWeldingMachineId(testMachine.getId());
        secondMaintenance.setUserAccountId(testUser.getId());
        secondMaintenance.setStatus(GeneralStatus.Active);
        secondMaintenance.setDatePlanned(LocalDateTime.now().plusDays(3));
        secondMaintenance.setDescription("Second Maintenance");
        secondMaintenance.setNotes("Second Notes");
        secondMaintenance.setType(TEST_TYPE);
        entityManager.persist(secondMaintenance);
        entityManager.flush();
        
        // Получаем все обслуживания машины
        List<Maintenance> maintenances = maintenanceRepository.findByWeldingMachineId(testMachine.getId());
        
        // Проверяем результаты
        assertNotNull(maintenances, "Список обслуживаний не должен быть null");
        assertTrue(maintenances.size() >= 2, "Должно быть найдено как минимум 2 обслуживания");
        assertTrue(maintenances.stream().anyMatch(m -> m.getId().equals(testMaintenance.getId())), 
            "Список должен содержать первое тестовое обслуживание");
        assertTrue(maintenances.stream().anyMatch(m -> m.getId().equals(secondMaintenance.getId())), 
            "Список должен содержать второе тестовое обслуживание");
    }

    /**
     * Тест поиска последнего обслуживания машины
     * Проверяет, что метод findTopByWeldingMachineIdOrderByDateCreatedDesc
     * возвращает самое последнее обслуживание
     */
    @Test
    void findTopByWeldingMachineIdOrderByDateCreatedDesc_ShouldReturnLatestMaintenance() {
        // Создаем второе обслуживание для той же машины
        Maintenance secondMaintenance = new Maintenance();
        secondMaintenance.setWeldingMachineId(testMachine.getId());
        secondMaintenance.setUserAccountId(testUser.getId());
        secondMaintenance.setStatus(GeneralStatus.Active);
        secondMaintenance.setDatePlanned(LocalDateTime.now().plusDays(3));
        secondMaintenance.setDescription("Second Maintenance");
        secondMaintenance.setNotes("Second Notes");
        secondMaintenance.setType(TEST_TYPE);
        entityManager.persist(secondMaintenance);
        entityManager.flush();
        
        // Получаем последнее обслуживание
        Optional<Maintenance> latestMaintenance = maintenanceRepository
            .findTopByWeldingMachineIdOrderByDateCreatedDesc(testMachine.getId());
        
        // Проверяем результаты
        assertTrue(latestMaintenance.isPresent(), "Последнее обслуживание должно быть найдено");
        assertEquals(secondMaintenance.getId(), latestMaintenance.get().getId(), 
            "Должно быть возвращено второе обслуживание как самое последнее");
    }

    /**
     * Тест поиска обслуживаний по ID машины и статусу
     * Проверяет, что метод findByWeldingMachineIdAndStatus возвращает
     * обслуживания с указанным статусом
     */
    @Test
    void findByWeldingMachineIdAndStatus_ShouldReturnMaintenancesByStatus() {
        // Создаем второе обслуживание с другим статусом
        Maintenance secondMaintenance = new Maintenance();
        secondMaintenance.setWeldingMachineId(testMachine.getId());
        secondMaintenance.setUserAccountId(testUser.getId());
        secondMaintenance.setStatus(GeneralStatus.Inactive);
        secondMaintenance.setDatePlanned(LocalDateTime.now().plusDays(3));
        secondMaintenance.setDescription("Second Maintenance");
        secondMaintenance.setNotes("Second Notes");
        secondMaintenance.setType(TEST_TYPE);
        entityManager.persist(secondMaintenance);
        entityManager.flush();
        
        // Получаем активные обслуживания
        List<Maintenance> activeMaintenances = maintenanceRepository
            .findByWeldingMachineIdAndStatus(testMachine.getId(), GeneralStatus.Active.name());
        
        // Проверяем результаты
        assertNotNull(activeMaintenances, "Список обслуживаний не должен быть null");
        assertTrue(activeMaintenances.stream().allMatch(m -> m.getStatus() == GeneralStatus.Active), 
            "Все обслуживания должны быть активными");
        assertTrue(activeMaintenances.stream().anyMatch(m -> m.getId().equals(testMaintenance.getId())), 
            "Список должен содержать тестовое обслуживание");
        assertFalse(activeMaintenances.stream().anyMatch(m -> m.getId().equals(secondMaintenance.getId())), 
            "Список не должен содержать неактивное обслуживание");
    }

    /**
     * Тест удаления обслуживания
     * Проверяет, что метод delete корректно удаляет обслуживание из базы данных
     */
    @Test
    void delete_ShouldRemoveMaintenance() {
        // Удаляем обслуживание
        maintenanceRepository.delete(testMaintenance);
        
        // Проверяем, что обслуживание было удалено
        Maintenance deletedMaintenance = entityManager.find(Maintenance.class, testMaintenance.getId());
        assertNull(deletedMaintenance, "Обслуживание должно быть удалено из базы данных");
    }

    /**
     * Тест проверки существования обслуживания
     * Проверяет, что метод existsById корректно определяет наличие обслуживания
     */
    @Test
    void existsById_ShouldReturnCorrectResult() {
        // Проверяем существование тестового обслуживания
        boolean exists = maintenanceRepository.existsById(testMaintenance.getId());
        assertTrue(exists, "Тестовое обслуживание должно существовать");
        
        // Проверяем существование несуществующего обслуживания
        boolean notExists = maintenanceRepository.existsById(999);
        assertFalse(notExists, "Несуществующее обслуживание не должно быть найдено");
    }

    /**
     * Тест подсчета обслуживаний
     * Проверяет, что метод count возвращает корректное количество обслуживаний
     */
    @Test
    void count_ShouldReturnCorrectCount() {
        // Создаем второе обслуживание
        Maintenance secondMaintenance = new Maintenance();
        secondMaintenance.setWeldingMachineId(testMachine.getId());
        secondMaintenance.setUserAccountId(testUser.getId());
        secondMaintenance.setStatus(GeneralStatus.Active);
        secondMaintenance.setDatePlanned(LocalDateTime.now().plusDays(3));
        secondMaintenance.setDescription("Second Maintenance");
        secondMaintenance.setNotes("Second Notes");
        secondMaintenance.setType(TEST_TYPE);
        entityManager.persist(secondMaintenance);
        entityManager.flush();
        
        // Получаем количество обслуживаний
        long count = maintenanceRepository.count();
        
        // Проверяем результат
        assertTrue(count >= 2, "Должно быть как минимум 2 обслуживания");
    }
}
