package org.alloy.services;

import org.alloy.AlloyServiceTest;
import org.alloy.models.entities.Maintenance;
import org.alloy.models.GeneralStatus;
import org.alloy.repositories.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для MaintenanceService
 * Проверяет корректность работы сервиса обслуживания сварочных машин
 */
@AlloyServiceTest(MaintenanceService.class)
public class MaintenanceServiceTest {

    @MockBean
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private MaintenanceService maintenanceService;

    private Maintenance testMaintenance;
    private LocalDateTime testDateTime;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект Maintenance с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.now();
        testMaintenance = new Maintenance();
        testMaintenance.setId(1);
        testMaintenance.setWeldingMachineId(1);
        testMaintenance.setUserAccountId(1);
        testMaintenance.setStatus(GeneralStatus.Active);
        testMaintenance.setDateCreated(testDateTime);
        testMaintenance.setDatePlanned(testDateTime.plusDays(1));
        testMaintenance.setDescription("Test maintenance");
        testMaintenance.setNotes("Test notes");
        testMaintenance.setType("Regular");
        testMaintenance.setResult("Pending");
    }

    /**
     * Тест метода getAllMaintenanceRecords()
     * Проверяет корректность получения всех записей обслуживания
     */
    @Test
    void getAllMaintenanceRecords_ShouldReturnAllRecords() {
        // Подготовка данных
        List<Maintenance> expectedRecords = Arrays.asList(testMaintenance);
        when(maintenanceRepository.findAll()).thenReturn(expectedRecords);

        // Выполнение теста
        List<Maintenance> actualRecords = maintenanceService.getAllMaintenanceRecords();

        // Проверка результатов
        assertNotNull(actualRecords);
        assertEquals(expectedRecords.size(), actualRecords.size());
        assertEquals(expectedRecords.get(0).getId(), actualRecords.get(0).getId());

        // Проверка вызова метода репозитория
        verify(maintenanceRepository, times(1)).findAll();
    }

    /**
     * Тест метода getMaintenanceRecordById() с существующим ID
     * Проверяет корректность получения записи обслуживания по существующему ID
     */
    @Test
    void getMaintenanceRecordById_WhenExists_ShouldReturnRecord() {
        // Подготовка данных
        when(maintenanceRepository.findById(1)).thenReturn(Optional.of(testMaintenance));

        // Выполнение теста
        Optional<Maintenance> result = maintenanceService.getMaintenanceRecordById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testMaintenance.getId(), result.get().getId());
        assertEquals(testMaintenance.getDescription(), result.get().getDescription());

        // Проверка вызова метода репозитория
        verify(maintenanceRepository, times(1)).findById(1);
    }

    /**
     * Тест метода getMaintenanceRecordsByMachineId()
     * Проверяет корректность получения записей обслуживания по ID машины
     */
    @Test
    void getMaintenanceRecordsByMachineId_ShouldReturnMachineRecords() {
        // Подготовка данных
        List<Maintenance> expectedRecords = Arrays.asList(testMaintenance);
        when(maintenanceRepository.findByWeldingMachineId(1)).thenReturn(expectedRecords);

        // Выполнение теста
        List<Maintenance> actualRecords = maintenanceService.getMaintenanceRecordsByMachineId(1);

        // Проверка результатов
        assertNotNull(actualRecords);
        assertEquals(expectedRecords.size(), actualRecords.size());
        assertEquals(expectedRecords.get(0).getWeldingMachineId(), actualRecords.get(0).getWeldingMachineId());

        // Проверка вызова метода репозитория
        verify(maintenanceRepository, times(1)).findByWeldingMachineId(1);
    }

    /**
     * Тест метода getLatestMaintenanceRecord()
     * Проверяет корректность получения последней записи обслуживания
     */
    @Test
    void getLatestMaintenanceRecord_ShouldReturnLatestRecord() {
        // Подготовка данных
        when(maintenanceRepository.findTopByWeldingMachineIdOrderByDateCreatedDesc(1))
                .thenReturn(Optional.of(testMaintenance));

        // Выполнение теста
        Optional<Maintenance> result = maintenanceService.getLatestMaintenanceRecord(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testMaintenance.getId(), result.get().getId());
        assertEquals(testMaintenance.getWeldingMachineId(), result.get().getWeldingMachineId());

        // Проверка вызова метода репозитория
        verify(maintenanceRepository, times(1))
                .findTopByWeldingMachineIdOrderByDateCreatedDesc(1);
    }

    /**
     * Тест метода getMaintenanceRecordsByStatus()
     * Проверяет корректность получения записей обслуживания по статусу
     */
    @Test
    void getMaintenanceRecordsByStatus_ShouldReturnFilteredRecords() {
        // Подготовка данных
        List<Maintenance> expectedRecords = Arrays.asList(testMaintenance);
        when(maintenanceRepository.findByWeldingMachineIdAndStatus(1, GeneralStatus.Active))
                .thenReturn(expectedRecords);

        // Выполнение теста
        List<Maintenance> actualRecords = maintenanceService.getMaintenanceRecordsByStatus(1, GeneralStatus.Active.toString());

        // Проверка результатов
        assertNotNull(actualRecords);
        assertEquals(expectedRecords.size(), actualRecords.size());
        assertEquals(expectedRecords.get(0).getStatus(), actualRecords.get(0).getStatus());

        // Проверка вызова метода репозитория
        verify(maintenanceRepository, times(1))
                .findByWeldingMachineIdAndStatus(1, GeneralStatus.Active);
    }

    /**
     * Тест метода createMaintenanceRecord() с валидными данными
     * Проверяет корректность создания новой записи обслуживания
     */
    @Test
    void createMaintenanceRecord_WithValidData_ShouldCreateRecord() {
        // Подготовка данных
        when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(testMaintenance);

        // Выполнение теста
        Maintenance createdRecord = maintenanceService.createMaintenanceRecord(testMaintenance);

        // Проверка результатов
        assertNotNull(createdRecord);
        assertEquals(testMaintenance.getDescription(), createdRecord.getDescription());
        assertEquals(testMaintenance.getType(), createdRecord.getType());
        assertNotNull(createdRecord.getDateCreated());
        assertEquals(GeneralStatus.Active, createdRecord.getStatus());

        // Проверка вызова метода репозитория
        verify(maintenanceRepository, times(1)).save(any(Maintenance.class));
    }

    /**
     * Тест метода createMaintenanceRecord() с невалидными данными
     * Проверяет корректность обработки ошибок при создании записи
     */
    @Test
    void createMaintenanceRecord_WithInvalidData_ShouldThrowException() {
        // Подготовка данных
        Maintenance invalidRecord = new Maintenance();
        invalidRecord.setWeldingMachineId(null);
        invalidRecord.setDescription(null);
        invalidRecord.setType(null);

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            maintenanceService.createMaintenanceRecord(invalidRecord)
        );
    }

    /**
     * Тест метода updateMaintenanceRecord()
     * Проверяет корректность обновления записи обслуживания
     */
    @Test
    void updateMaintenanceRecord_ShouldUpdateRecord() {
        // Подготовка данных
        when(maintenanceRepository.findById(1)).thenReturn(Optional.of(testMaintenance));
        when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(testMaintenance);

        // Выполнение теста
        Maintenance updatedRecord = maintenanceService.updateMaintenanceRecord(testMaintenance);

        // Проверка результатов
        assertNotNull(updatedRecord);
        assertEquals(testMaintenance.getDateCreated(), updatedRecord.getDateCreated());

        // Проверка вызовов методов репозитория
        verify(maintenanceRepository, times(1)).findById(1);
        verify(maintenanceRepository, times(1)).save(any(Maintenance.class));
    }

    /**
     * Тест метода updateMaintenanceRecord() с несуществующим ID
     * Проверяет корректность обработки ошибки при обновлении несуществующей записи
     */
    @Test
    void updateMaintenanceRecord_WithNonExistentId_ShouldThrowException() {
        // Подготовка данных
        when(maintenanceRepository.findById(999)).thenReturn(Optional.empty());

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            maintenanceService.updateMaintenanceRecord(testMaintenance)
        );
    }

    /**
     * Тест метода deleteMaintenanceRecord()
     * Проверяет корректность удаления записи обслуживания
     */
    @Test
    void deleteMaintenanceRecord_ShouldDeleteRecord() {
        // Подготовка данных
        when(maintenanceRepository.existsById(1)).thenReturn(true);

        // Выполнение теста
        maintenanceService.deleteMaintenanceRecord(1);

        // Проверка вызова метода репозитория
        verify(maintenanceRepository, times(1)).deleteById(1);
    }

    /**
     * Тест метода deleteMaintenanceRecord() с несуществующим ID
     * Проверяет корректность обработки ошибки при удалении несуществующей записи
     */
    @Test
    void deleteMaintenanceRecord_WithNonExistentId_ShouldThrowException() {
        // Подготовка данных
        when(maintenanceRepository.existsById(999)).thenReturn(false);

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            maintenanceService.deleteMaintenanceRecord(999)
        );
    }

    /**
     * Тест метода deleteAllMaintenanceRecords()
     * Проверяет корректность удаления всех записей обслуживания для машины
     */
    @Test
    void deleteAllMaintenanceRecords_ShouldDeleteAllMachineRecords() {
        // Подготовка данных
        List<Maintenance> records = Arrays.asList(testMaintenance);
        when(maintenanceRepository.findByWeldingMachineId(1)).thenReturn(records);

        // Выполнение теста
        maintenanceService.deleteAllMaintenanceRecords(1);

        // Проверка вызовов методов репозитория
        verify(maintenanceRepository, times(1)).findByWeldingMachineId(1);
        verify(maintenanceRepository, times(1)).deleteAll(records);
    }
}
