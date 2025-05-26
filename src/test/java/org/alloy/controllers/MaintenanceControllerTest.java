package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.EmailTemplate;
import org.alloy.models.entities.Maintenance;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.services.EmailTemplateService;
import org.alloy.services.MaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для MaintenanceController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(MaintenanceController.class)
@Import(TestConfig.class)
@WithMockUser
public class MaintenanceControllerTest {

    // MockMvc - основной инструмент для тестирования веб-слоя
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper для сериализации/десериализации JSON
    @Autowired
    private ObjectMapper objectMapper;

    // Мокаем MaintenanceService, так как нам не нужна реальная работа с базой данных
    @MockBean
    private MaintenanceService maintenanceService;

    // Тестовые данные
    private Maintenance testMaintenance;
    private List<Maintenance> testMaintenances;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовое обслуживание
        testMaintenance = new Maintenance();
        testMaintenance.setId(1);
        testMaintenance.setStatus(GeneralStatus.Active);
        testMaintenance.setDateCreated(LocalDateTime.now());
        testMaintenance.setNotes("Работаем, брат!");
        testMaintenance.setResult("Поработали, брат!");
        testMaintenance.setType("MX-Pulse");
        testMaintenance.setDescription("Варит хорошо!");
        testMaintenance.setDateCompleted(LocalDateTime.now());
        testMaintenance.setDatePlanned(LocalDateTime.now());
        testMaintenance.setUserAccount(new UserAccount());
        testMaintenance.setUserAccountId(2);
        testMaintenance.setWeldingMachine(new WeldingMachine());
        testMaintenance.setWeldingMachineId(3);

        // Создаем второе тестовое обслуживание
        Maintenance secondMaintenance = new Maintenance();
        secondMaintenance.setId(2);
        secondMaintenance.setStatus(GeneralStatus.Inactive);
        secondMaintenance.setDateCreated(LocalDateTime.now());
        secondMaintenance.setNotes("Второе обслуживание");
        secondMaintenance.setResult("Успешно завершено");
        secondMaintenance.setType("MX-Pulse");
        secondMaintenance.setDescription("Плановое обслуживание");
        secondMaintenance.setDateCompleted(LocalDateTime.now());
        secondMaintenance.setDatePlanned(LocalDateTime.now());
        secondMaintenance.setUserAccount(new UserAccount());
        secondMaintenance.setUserAccountId(2);
        secondMaintenance.setWeldingMachine(new WeldingMachine());
        secondMaintenance.setWeldingMachineId(3);

        testMaintenances = Arrays.asList(testMaintenance, secondMaintenance);
    }

    /**
     * Тест получения всех записей обслуживания
     */
    @Test
    void getAllMaintenanceRecords_ShouldReturnAllRecords() throws Exception {
        when(maintenanceService.getAllMaintenanceRecords()).thenReturn(testMaintenances);

        mockMvc.perform(get("/api/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("MX-Pulse"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].type").value("MX-Pulse"));

        verify(maintenanceService).getAllMaintenanceRecords();
    }

    /**
     * Тест получения записи обслуживания по ID
     */
    @Test
    void getMaintenanceRecordById_WhenRecordExists_ShouldReturnRecord() throws Exception {
        when(maintenanceService.getMaintenanceRecordById(1)).thenReturn(Optional.of(testMaintenance));

        mockMvc.perform(get("/api/maintenance/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("MX-Pulse"));

        verify(maintenanceService).getMaintenanceRecordById(1);
    }

    /**
     * Тест получения записи обслуживания по несуществующему ID
     */
    @Test
    void getMaintenanceRecordById_WhenRecordDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(maintenanceService.getMaintenanceRecordById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/maintenance/999"))
                .andExpect(status().isNotFound());

        verify(maintenanceService).getMaintenanceRecordById(999);
    }

    /**
     * Тест получения записей обслуживания по ID машины
     */
    @Test
    void getMaintenanceRecordsByMachineId_ShouldReturnMachineRecords() throws Exception {
        when(maintenanceService.getMaintenanceRecordsByMachineId(3)).thenReturn(testMaintenances);

        mockMvc.perform(get("/api/maintenance/machine/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weldingMachineId").value(3))
                .andExpect(jsonPath("$[1].weldingMachineId").value(3));

        verify(maintenanceService).getMaintenanceRecordsByMachineId(3);
    }

    /**
     * Тест получения последней записи обслуживания для машины
     */
    @Test
    void getLatestMaintenanceRecord_WhenRecordExists_ShouldReturnLatestRecord() throws Exception {
        when(maintenanceService.getLatestMaintenanceRecord(3)).thenReturn(Optional.of(testMaintenance));

        mockMvc.perform(get("/api/maintenance/machine/3/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weldingMachineId").value(3));

        verify(maintenanceService).getLatestMaintenanceRecord(3);
    }

    /**
     * Тест получения последней записи обслуживания для машины, когда записей нет
     */
    @Test
    void getLatestMaintenanceRecord_WhenNoRecords_ShouldReturnNotFound() throws Exception {
        when(maintenanceService.getLatestMaintenanceRecord(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/maintenance/machine/999/latest"))
                .andExpect(status().isNotFound());

        verify(maintenanceService).getLatestMaintenanceRecord(999);
    }

    /**
     * Тест получения записей обслуживания по статусу для машины
     */
    @Test
    void getMaintenanceRecordsByStatus_ShouldReturnFilteredRecords() throws Exception {
        when(maintenanceService.getMaintenanceRecordsByStatus(3, "Active"))
                .thenReturn(List.of(testMaintenance));

        mockMvc.perform(get("/api/maintenance/machine/3/status/Active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("Active"))
                .andExpect(jsonPath("$[0].weldingMachineId").value(3));

        verify(maintenanceService).getMaintenanceRecordsByStatus(3, "Active");
    }

    /**
     * Тест создания новой записи обслуживания
     */
    @Test
    void createMaintenanceRecord_ShouldCreateRecord() throws Exception {
        when(maintenanceService.createMaintenanceRecord(any(Maintenance.class))).thenReturn(testMaintenance);

        mockMvc.perform(post("/api/maintenance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMaintenance)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("MX-Pulse"));

        verify(maintenanceService).createMaintenanceRecord(any(Maintenance.class));
    }

    /**
     * Тест создания записи обслуживания с невалидными данными
     */
    @Test
    void createMaintenanceRecord_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        testMaintenance.setWeldingMachineId(null); // Невалидные данные

        when(maintenanceService.createMaintenanceRecord(any(Maintenance.class)))
                .thenThrow(new IllegalArgumentException("Welding machine ID is required"));

        mockMvc.perform(post("/api/maintenance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMaintenance)))
                .andExpect(status().isBadRequest());

        verify(maintenanceService).createMaintenanceRecord(any(Maintenance.class));
    }

    /**
     * Тест обновления существующей записи обслуживания
     */
    @Test
    void updateMaintenanceRecord_WhenRecordExists_ShouldUpdateRecord() throws Exception {
        when(maintenanceService.updateMaintenanceRecord(any(Maintenance.class))).thenReturn(testMaintenance);

        mockMvc.perform(put("/api/maintenance/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMaintenance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(maintenanceService).updateMaintenanceRecord(any(Maintenance.class));
    }

    /**
     * Тест обновления несуществующей записи обслуживания
     */
    @Test
    void updateMaintenanceRecord_WhenRecordDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(maintenanceService.updateMaintenanceRecord(any(Maintenance.class)))
                .thenThrow(new IllegalArgumentException("Maintenance record not found"));

        mockMvc.perform(put("/api/maintenance/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMaintenance)))
                .andExpect(status().isNotFound());

        verify(maintenanceService).updateMaintenanceRecord(any(Maintenance.class));
    }

    /**
     * Тест удаления существующей записи обслуживания
     */
    @Test
    void deleteMaintenanceRecord_WhenRecordExists_ShouldDeleteRecord() throws Exception {
        doNothing().when(maintenanceService).deleteMaintenanceRecord(1);

        mockMvc.perform(delete("/api/maintenance/1"))
                .andExpect(status().isNoContent());

        verify(maintenanceService).deleteMaintenanceRecord(1);
    }

    /**
     * Тест удаления несуществующей записи обслуживания
     */
    @Test
    void deleteMaintenanceRecord_WhenRecordDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Maintenance record not found"))
                .when(maintenanceService).deleteMaintenanceRecord(999);

        mockMvc.perform(delete("/api/maintenance/999"))
                .andExpect(status().isNotFound());

        verify(maintenanceService).deleteMaintenanceRecord(999);
    }

    /**
     * Тест удаления всех записей обслуживания для машины
     */
    @Test
    void deleteAllMaintenanceRecords_ShouldDeleteAllMachineRecords() throws Exception {
        doNothing().when(maintenanceService).deleteAllMaintenanceRecords(3);

        mockMvc.perform(delete("/api/maintenance/machine/3"))
                .andExpect(status().isNoContent());

        verify(maintenanceService).deleteAllMaintenanceRecords(3);
    }

    /**
     * Тест удаления всех записей обслуживания для несуществующей машины
     */
    @Test
    void deleteAllMaintenanceRecords_WhenMachineDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Machine not found"))
                .when(maintenanceService).deleteAllMaintenanceRecords(999);

        mockMvc.perform(delete("/api/maintenance/machine/999"))
                .andExpect(status().isNotFound());

        verify(maintenanceService).deleteAllMaintenanceRecords(999);
    }
}
