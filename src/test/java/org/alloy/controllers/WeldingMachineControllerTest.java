package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.services.DeviceModelService;
import org.alloy.services.WeldingMachineService;
import org.alloy.services.Wt2AccessService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для WeldingMachineController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(WeldingMachineController.class)
@Import(TestConfig.class)
@WithMockUser
public class WeldingMachineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeldingMachineService weldingMachineService;

    @MockBean
    private DeviceModelService deviceModelService;

    @MockBean
    private Wt2AccessService wt2AccessService;

    private WeldingMachine testWeldingMachine;
    private List<WeldingMachine> testWeldingMachines;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый тип сварочной машины
        WeldingMachineType weldingMachineType = new WeldingMachineType();
        weldingMachineType.setId(1);
        weldingMachineType.setName("Test Type");

        // Создаем тестовое подразделение организации
        OrganizationUnit organizationUnit = new OrganizationUnit();
        organizationUnit.setId(1);
        organizationUnit.setName("Test Unit");

        // Создаем тестовую сварочную машину
        testWeldingMachine = new WeldingMachine();
        testWeldingMachine.setId(1);
        testWeldingMachine.setWeldingMachineType(weldingMachineType);
        testWeldingMachine.setName("Test Machine");
        testWeldingMachine.setStatus(GeneralStatus.Active);
        testWeldingMachine.setDateCreated(LocalDateTime.now());
        testWeldingMachine.setLabel("Test Label");
        testWeldingMachine.setOrganizationUnit(organizationUnit);
        testWeldingMachine.setDateStartedUsing(LocalDateTime.now());
        testWeldingMachine.setDescription("Test Description");
        testWeldingMachine.setInventoryNumber("INV123");
        testWeldingMachine.setLastOnlineOn(LocalDateTime.now());
        testWeldingMachine.setLastServiceOn(LocalDateTime.now());
        testWeldingMachine.setMac("00:11:22:33:44:55");
        testWeldingMachine.setMaintenanceInterval(30.0);
        testWeldingMachine.setMaintenanceRegulation(1);
        testWeldingMachine.setMeasuringGasMachineId(1);
        testWeldingMachine.setModules("Test Module");
        testWeldingMachine.setPlanPositionX(10.0);
        testWeldingMachine.setPlanPositionY(20.0);
        testWeldingMachine.setSerialNumber("SN123456");
        testWeldingMachine.setTimeAfterLastServiceSecs(3600L);
        testWeldingMachine.setTimeTillNextServiceSecs(7200L);

        // Создаем вторую тестовую сварочную машину
        WeldingMachine secondMachine = new WeldingMachine();
        secondMachine.setId(2);
        secondMachine.setWeldingMachineType(weldingMachineType);
        secondMachine.setName("Second Machine");
        secondMachine.setStatus(GeneralStatus.Inactive);
        secondMachine.setSerialNumber("SN789012");

        // Создаем список тестовых сварочных машин
        testWeldingMachines = Arrays.asList(testWeldingMachine, secondMachine);

        when(wt2AccessService.filterWeldingMachines(any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Тест получения всех сварочных машин
     */
    @Test
    void getAllWeldingMachines_ShouldReturnListOfAllMachines() throws Exception {
        when(weldingMachineService.getAllWeldingMachines()).thenReturn(testWeldingMachines);

        mockMvc.perform(get("/welding-machines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Machine"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Second Machine"));

        verify(weldingMachineService).getAllWeldingMachines();
    }

    /**
     * Тест получения сварочной машины по ID
     */
    @Test
    void getWeldingMachineById_WhenMachineExists_ShouldReturnMachine() throws Exception {
        when(weldingMachineService.getWeldingMachineById(1)).thenReturn(Optional.of(testWeldingMachine));

        mockMvc.perform(get("/welding-machines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Machine"))
                .andExpect(jsonPath("$.serialNumber").value("SN123456"));

        verify(weldingMachineService).getWeldingMachineById(1);
    }

    /**
     * Тест получения несуществующей сварочной машины по ID
     */
    @Test
    void getWeldingMachineById_WhenMachineDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(weldingMachineService.getWeldingMachineById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/welding-machines/999"))
                .andExpect(status().isNotFound());

        verify(weldingMachineService).getWeldingMachineById(999);
    }

    /**
     * Тест получения сварочной машины по серийному номеру
     */
    @Test
    void getWeldingMachineBySerialNumber_WhenMachineExists_ShouldReturnMachine() throws Exception {
        when(weldingMachineService.getWeldingMachineBySerialNumber("SN123456"))
                .thenReturn(Optional.of(testWeldingMachine));

        mockMvc.perform(get("/welding-machines/serial-number/SN123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.serialNumber").value("SN123456"));

        verify(weldingMachineService).getWeldingMachineBySerialNumber("SN123456");
    }

    /**
     * Тест получения несуществующей сварочной машины по серийному номеру
     */
    @Test
    void getWeldingMachineBySerialNumber_WhenMachineDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(weldingMachineService.getWeldingMachineBySerialNumber("NONEXISTENT"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/welding-machines/serial-number/NONEXISTENT"))
                .andExpect(status().isNotFound());

        verify(weldingMachineService).getWeldingMachineBySerialNumber("NONEXISTENT");
    }

    /**
     * Тест получения сварочных машин по ID подразделения
     */
    @Test
    void getWeldingMachinesByOrganizationId_ShouldReturnListOfMachines() throws Exception {
        when(weldingMachineService.getWeldingMachinesByOrganizationId(1))
                .thenReturn(testWeldingMachines);

        mockMvc.perform(get("/welding-machines/organization/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationUnit.id").value(1))
                .andExpect(jsonPath("$[1].organizationUnit.id").value(1));

        verify(weldingMachineService).getWeldingMachinesByOrganizationId(1);
    }

    /**
     * Тест получения сварочных машин по ID типа
     */
    @Test
    void getWeldingMachinesByTypeId_ShouldReturnListOfMachines() throws Exception {
        when(weldingMachineService.getWeldingMachinesByTypeId(1))
                .thenReturn(testWeldingMachines);

        mockMvc.perform(get("/welding-machines/type/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weldingMachineType.id").value(1))
                .andExpect(jsonPath("$[1].weldingMachineType.id").value(1));

        verify(weldingMachineService).getWeldingMachinesByTypeId(1);
    }

    /**
     * Тест поиска сварочных машин
     */
    @Test
    void searchWeldingMachines_ShouldReturnListOfMachines() throws Exception {
        when(weldingMachineService.searchWeldingMachines(anyInt(), anyString()))
                .thenReturn(testWeldingMachines);

        mockMvc.perform(get("/welding-machines/search")
                .param("organizationUnitId", "1")
                .param("searchTerm", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Machine"))
                .andExpect(jsonPath("$[1].name").value("Second Machine"));

        verify(weldingMachineService).searchWeldingMachines(1, "Test");
    }

    /**
     * Тест создания новой сварочной машины
     */
    @Test
    void createWeldingMachine_ShouldCreateMachine() throws Exception {
        when(weldingMachineService.createWeldingMachine(any(WeldingMachine.class)))
                .thenReturn(testWeldingMachine);

        mockMvc.perform(post("/welding-machines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testWeldingMachine)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Machine"));

        verify(weldingMachineService).createWeldingMachine(any(WeldingMachine.class));
    }

    /**
     * Тест создания сварочной машины с невалидными данными
     */
    @Test
    void createWeldingMachine_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        when(weldingMachineService.createWeldingMachine(any(WeldingMachine.class)))
                .thenThrow(new IllegalArgumentException("Invalid machine data"));

        mockMvc.perform(post("/welding-machines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testWeldingMachine)))
                .andExpect(status().isBadRequest());

        verify(weldingMachineService).createWeldingMachine(any(WeldingMachine.class));
    }

    /**
     * Тест обновления существующей сварочной машины
     */
    @Test
    void updateWeldingMachine_WhenMachineExists_ShouldUpdateMachine() throws Exception {
        when(weldingMachineService.updateWeldingMachine(any(WeldingMachine.class)))
                .thenReturn(testWeldingMachine);

        mockMvc.perform(put("/welding-machines/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testWeldingMachine)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Machine"));

        verify(weldingMachineService).updateWeldingMachine(any(WeldingMachine.class));
    }

    /**
     * Тест обновления несуществующей сварочной машины
     */
    @Test
    void updateWeldingMachine_WhenMachineDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(weldingMachineService.updateWeldingMachine(any(WeldingMachine.class)))
                .thenThrow(new IllegalArgumentException("Machine not found"));

        mockMvc.perform(put("/welding-machines/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testWeldingMachine)))
                .andExpect(status().isNotFound());

        verify(weldingMachineService).updateWeldingMachine(any(WeldingMachine.class));
    }

    /**
     * Тест удаления существующей сварочной машины
     */
    @Test
    void deleteWeldingMachine_WhenMachineExists_ShouldDeleteMachine() throws Exception {
        doNothing().when(weldingMachineService).deleteWeldingMachine(1);

        mockMvc.perform(delete("/welding-machines/1"))
                .andExpect(status().isNoContent());

        verify(weldingMachineService).deleteWeldingMachine(1);
    }

    /**
     * Тест удаления несуществующей сварочной машины
     */
    @Test
    void deleteWeldingMachine_WhenMachineDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Machine not found"))
                .when(weldingMachineService).deleteWeldingMachine(999);

        mockMvc.perform(delete("/welding-machines/999"))
                .andExpect(status().isNotFound());

        verify(weldingMachineService).deleteWeldingMachine(999);
    }

    /**
     * Тест жесткого удаления существующей сварочной машины
     */
    @Test
    void hardDeleteWeldingMachine_WhenMachineExists_ShouldDeleteMachine() throws Exception {
        doNothing().when(weldingMachineService).hardDeleteWeldingMachine(1);

        mockMvc.perform(delete("/welding-machines/1/hard"))
                .andExpect(status().isNoContent());

        verify(weldingMachineService).hardDeleteWeldingMachine(1);
    }

    /**
     * Тест жесткого удаления несуществующей сварочной машины
     */
    @Test
    void hardDeleteWeldingMachine_WhenMachineDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Machine not found"))
                .when(weldingMachineService).hardDeleteWeldingMachine(999);

        mockMvc.perform(delete("/welding-machines/999/hard"))
                .andExpect(status().isNotFound());

        verify(weldingMachineService).hardDeleteWeldingMachine(999);
    }
}
