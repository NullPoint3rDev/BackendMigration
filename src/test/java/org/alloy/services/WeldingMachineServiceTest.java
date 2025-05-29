package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.GeneralStatus;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineTypeRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class WeldingMachineServiceTest {

    @MockBean
    private WeldingMachineRepository weldingMachineRepository;

    @MockBean
    private WeldingMachineTypeRepository weldingMachineTypeRepository;

    @MockBean
    private OrganizationUnitRepository organizationUnitRepository;

    private WeldingMachineService weldingMachineService;
    private WeldingMachine testWeldingMachine;
    private WeldingMachineType testType;
    private OrganizationUnit testOrgUnit;

    @BeforeEach
    void setUp() {
        weldingMachineService = new WeldingMachineService(
                weldingMachineRepository,
                weldingMachineTypeRepository,
                organizationUnitRepository
        );

        // Создаем тестовый тип сварочной машины
        testType = new WeldingMachineType();
        testType.setId(1);
        testType.setName("Test Type");

        // Создаем тестовое подразделение
        testOrgUnit = new OrganizationUnit();
        testOrgUnit.setId(1);
        testOrgUnit.setName("Test Unit");

        // Создаем тестовую сварочную машину
        testWeldingMachine = new WeldingMachine();
        testWeldingMachine.setId(1);
        testWeldingMachine.setName("Test Machine");
        testWeldingMachine.setSerialNumber("TEST123");
        testWeldingMachine.setOrganizationUnitId(1);
        testWeldingMachine.setWeldingMachineTypeId(1);
        testWeldingMachine.setStatus(GeneralStatus.Active);
        testWeldingMachine.setDateCreated(LocalDateTime.now());
        testWeldingMachine.setWeldingMachineType(testType);
        testWeldingMachine.setOrganizationUnit(testOrgUnit);
    }

    /**
     * Тест получения всех сварочных машин
     * Проверяет корректность получения списка всех сварочных машин
     */
    @Test
    void getAllWeldingMachines_ShouldReturnAllMachines() {
        // Подготавливаем тестовые данные
        List<WeldingMachine> expectedMachines = Arrays.asList(testWeldingMachine);
        when(weldingMachineRepository.findAll()).thenReturn(expectedMachines);

        // Вызываем тестируемый метод
        List<WeldingMachine> result = weldingMachineService.getAllWeldingMachines();

        // Проверяем результаты
        assertNotNull(result, "Список сварочных машин не должен быть null");
        assertEquals(expectedMachines.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testWeldingMachine.getId(), result.get(0).getId(), "ID должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineRepository, times(1)).findAll();
    }

    /**
     * Тест получения сварочной машины по ID
     * Проверяет корректность получения сварочной машины по существующему ID
     */
    @Test
    void getWeldingMachineById_WhenExists_ShouldReturnMachine() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.findById(1)).thenReturn(Optional.of(testWeldingMachine));

        // Вызываем тестируемый метод
        Optional<WeldingMachine> result = weldingMachineService.getWeldingMachineById(1);

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать сварочную машину");
        assertEquals(testWeldingMachine.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testWeldingMachine.getSerialNumber(), result.get().getSerialNumber(), "Серийный номер должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineRepository, times(1)).findById(1);
    }

    /**
     * Тест получения сварочной машины по серийному номеру
     * Проверяет корректность получения сварочной машины по существующему серийному номеру
     */
    @Test
    void getWeldingMachineBySerialNumber_WhenExists_ShouldReturnMachine() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.findBySerialNumber("TEST123")).thenReturn(Optional.of(testWeldingMachine));

        // Вызываем тестируемый метод
        Optional<WeldingMachine> result = weldingMachineService.getWeldingMachineBySerialNumber("TEST123");

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать сварочную машину");
        assertEquals(testWeldingMachine.getSerialNumber(), result.get().getSerialNumber(), "Серийный номер должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineRepository, times(1)).findBySerialNumber("TEST123");
    }

    /**
     * Тест получения сварочных машин по ID подразделения
     * Проверяет корректность получения всех сварочных машин для конкретного подразделения
     */
    @Test
    void getWeldingMachinesByOrganizationId_ShouldReturnMachines() {
        // Подготавливаем тестовые данные
        List<WeldingMachine> expectedMachines = Arrays.asList(testWeldingMachine);
        when(weldingMachineRepository.findByOrganizationUnitId(1)).thenReturn(expectedMachines);

        // Вызываем тестируемый метод
        List<WeldingMachine> result = weldingMachineService.getWeldingMachinesByOrganizationId(1);

        // Проверяем результаты
        assertNotNull(result, "Список сварочных машин не должен быть null");
        assertEquals(expectedMachines.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testWeldingMachine.getOrganizationUnitId(), result.get(0).getOrganizationUnitId(), "ID подразделения должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineRepository, times(1)).findByOrganizationUnitId(1);
    }

    /**
     * Тест получения сварочных машин по ID типа
     * Проверяет корректность получения всех сварочных машин определенного типа
     */
    @Test
    void getWeldingMachinesByTypeId_ShouldReturnMachines() {
        // Подготавливаем тестовые данные
        List<WeldingMachine> expectedMachines = Arrays.asList(testWeldingMachine);
        when(weldingMachineRepository.findByWeldingMachineTypeId(1)).thenReturn(expectedMachines);

        // Вызываем тестируемый метод
        List<WeldingMachine> result = weldingMachineService.getWeldingMachinesByTypeId(1);

        // Проверяем результаты
        assertNotNull(result, "Список сварочных машин не должен быть null");
        assertEquals(expectedMachines.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testWeldingMachine.getWeldingMachineTypeId(), result.get(0).getWeldingMachineTypeId(), "ID типа должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineRepository, times(1)).findByWeldingMachineTypeId(1);
    }

    /**
     * Тест поиска сварочных машин
     * Проверяет корректность поиска сварочных машин по подразделению и поисковому запросу
     */
    @Test
    void searchWeldingMachines_WithSearchTerm_ShouldReturnMatchingMachines() {
        // Подготавливаем тестовые данные
        List<WeldingMachine> expectedMachines = Arrays.asList(testWeldingMachine);
        when(weldingMachineRepository.searchWeldingMachines(1, "Test")).thenReturn(expectedMachines);

        // Вызываем тестируемый метод
        List<WeldingMachine> result = weldingMachineService.searchWeldingMachines(1, "Test");

        // Проверяем результаты
        assertNotNull(result, "Список сварочных машин не должен быть null");
        assertEquals(expectedMachines.size(), result.size(), "Размер списка должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineRepository, times(1)).searchWeldingMachines(1, "Test");
    }

    /**
     * Тест поиска сварочных машин без поискового запроса
     * Проверяет корректность получения всех сварочных машин подразделения при пустом поисковом запросе
     */
    @Test
    void searchWeldingMachines_WithoutSearchTerm_ShouldReturnAllMachines() {
        // Подготавливаем тестовые данные
        List<WeldingMachine> expectedMachines = Arrays.asList(testWeldingMachine);
        when(weldingMachineRepository.findByOrganizationUnitId(1)).thenReturn(expectedMachines);

        // Вызываем тестируемый метод
        List<WeldingMachine> result = weldingMachineService.searchWeldingMachines(1, "");

        // Проверяем результаты
        assertNotNull(result, "Список сварочных машин не должен быть null");
        assertEquals(expectedMachines.size(), result.size(), "Размер списка должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineRepository, times(1)).findByOrganizationUnitId(1);
    }

    /**
     * Тест создания новой сварочной машины
     * Проверяет корректность создания новой сварочной машины с валидными данными
     */
    @Test
    void createWeldingMachine_WithValidData_ShouldCreateMachine() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.findBySerialNumber("TEST123")).thenReturn(Optional.empty());
        when(weldingMachineTypeRepository.findById(1)).thenReturn(Optional.of(testType));
        when(organizationUnitRepository.findById(1)).thenReturn(Optional.of(testOrgUnit));
        when(weldingMachineRepository.save(any(WeldingMachine.class))).thenReturn(testWeldingMachine);

        // Вызываем тестируемый метод
        WeldingMachine result = weldingMachineService.createWeldingMachine(testWeldingMachine);

        // Проверяем результаты
        assertNotNull(result, "Созданная сварочная машина не должна быть null");
        assertEquals(testWeldingMachine.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testWeldingMachine.getSerialNumber(), result.getSerialNumber(), "Серийный номер должен совпадать");
        assertEquals(GeneralStatus.Active, result.getStatus(), "Статус должен быть Active");
        assertNotNull(result.getDateCreated(), "Дата создания не должна быть null");
        
        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineRepository, times(1)).findBySerialNumber("TEST123");
        verify(weldingMachineTypeRepository, times(1)).findById(1);
        verify(organizationUnitRepository, times(1)).findById(1);
        verify(weldingMachineRepository, times(1)).save(testWeldingMachine);
    }

    /**
     * Тест создания сварочной машины с существующим серийным номером
     * Проверяет, что создание сварочной машины с существующим серийным номером вызывает исключение
     */
    @Test
    void createWeldingMachine_WithExistingSerialNumber_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.findBySerialNumber("TEST123")).thenReturn(Optional.of(testWeldingMachine));

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineService.createWeldingMachine(testWeldingMachine);
        }, "Должно быть выброшено исключение при попытке создать сварочную машину с существующим серийным номером");
    }

    /**
     * Тест создания сварочной машины без обязательных полей
     * Проверяет, что создание сварочной машины без обязательных полей вызывает исключение
     */
    @Test
    void createWeldingMachine_WithoutRequiredFields_ShouldThrowException() {
        // Подготавливаем тестовые данные
        testWeldingMachine.setSerialNumber(null);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineService.createWeldingMachine(testWeldingMachine);
        }, "Должно быть выброшено исключение при попытке создать сварочную машину без серийного номера");
    }

    /**
     * Тест обновления сварочной машины
     * Проверяет корректность обновления существующей сварочной машины
     */
    @Test
    void updateWeldingMachine_WithValidData_ShouldUpdateMachine() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.findById(1)).thenReturn(Optional.of(testWeldingMachine));
        when(weldingMachineRepository.findBySerialNumber("TEST123")).thenReturn(Optional.of(testWeldingMachine));
        when(weldingMachineTypeRepository.findById(1)).thenReturn(Optional.of(testType));
        when(organizationUnitRepository.findById(1)).thenReturn(Optional.of(testOrgUnit));
        when(weldingMachineRepository.save(any(WeldingMachine.class))).thenReturn(testWeldingMachine);

        // Вызываем тестируемый метод
        WeldingMachine result = weldingMachineService.updateWeldingMachine(testWeldingMachine);

        // Проверяем результаты
        assertNotNull(result, "Обновленная сварочная машина не должна быть null");
        assertEquals(testWeldingMachine.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testWeldingMachine.getSerialNumber(), result.getSerialNumber(), "Серийный номер должен совпадать");
        
        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineRepository, times(1)).findById(1);
        verify(weldingMachineRepository, times(1)).findBySerialNumber("TEST123");
        verify(weldingMachineRepository, times(1)).save(testWeldingMachine);
    }

    /**
     * Тест обновления несуществующей сварочной машины
     * Проверяет, что обновление несуществующей сварочной машины вызывает исключение
     */
    @Test
    void updateWeldingMachine_WithNonExistentId_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.findById(1)).thenReturn(Optional.empty());

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineService.updateWeldingMachine(testWeldingMachine);
        }, "Должно быть выброшено исключение при попытке обновить несуществующую сварочную машину");
    }

    /**
     * Тест удаления сварочной машины
     * Проверяет корректность удаления существующей сварочной машины
     */
    @Test
    void deleteWeldingMachine_WhenExists_ShouldMarkAsDeleted() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.findById(1)).thenReturn(Optional.of(testWeldingMachine));
        when(weldingMachineRepository.save(any(WeldingMachine.class))).thenReturn(testWeldingMachine);

        // Вызываем тестируемый метод
        weldingMachineService.deleteWeldingMachine(1);

        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineRepository, times(1)).findById(1);
        verify(weldingMachineRepository, times(1)).save(argThat(machine -> 
            machine.getStatus() == GeneralStatus.Deleted
        ));
    }

    /**
     * Тест жесткого удаления сварочной машины
     * Проверяет корректность полного удаления существующей сварочной машины
     */
    @Test
    void hardDeleteWeldingMachine_WhenExists_ShouldDeleteMachine() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.existsById(1)).thenReturn(true);

        // Вызываем тестируемый метод
        weldingMachineService.hardDeleteWeldingMachine(1);

        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineRepository, times(1)).existsById(1);
        verify(weldingMachineRepository, times(1)).deleteById(1);
    }

    /**
     * Тест жесткого удаления несуществующей сварочной машины
     * Проверяет, что жесткое удаление несуществующей сварочной машины вызывает исключение
     */
    @Test
    void hardDeleteWeldingMachine_WhenNotExists_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineRepository.existsById(1)).thenReturn(false);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineService.hardDeleteWeldingMachine(1);
        }, "Должно быть выброшено исключение при попытке удалить несуществующую сварочную машину");
    }
}
