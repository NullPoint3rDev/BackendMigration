package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.GeneralStatus;
import org.alloy.services.WeldingMachineTypeService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeldingMachineTypeController.class)
@Import(TestConfig.class)
@WithMockUser
public class WeldingMachineTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeldingMachineTypeService weldingMachineTypeService;

    @Autowired
    private ObjectMapper objectMapper;

    private WeldingMachineType testType;

    /**
     * Инициализация тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый объект WeldingMachineType
        testType = new WeldingMachineType();
        testType.setId(1);
        testType.setStatus(GeneralStatus.Active);
        testType.setDateCreated(LocalDateTime.now());
        testType.setName("Test Type");
        testType.setDescription("Test Description");
        testType.setSettings("Test Settings");
        testType.setPropertyLimits("Test Limits");
        testType.setInbound("Test Inbound");
        testType.setOutbound("Test Outbound");
        testType.setPresentation("Test Presentation");
        testType.setModeDefinitions("Test Modes");
        testType.setAlertDefinitions("Test Alerts");
    }

    /**
     * Тест получения всех типов сварочных машин
     * Проверяет успешное получение списка всех типов
     */
    @Test
    void getAllWeldingMachineTypes_ShouldReturnListOfTypes() throws Exception {
        // Подготовка данных
        List<WeldingMachineType> types = Arrays.asList(testType);
        when(weldingMachineTypeService.getAllWeldingMachineTypes()).thenReturn(types);

        // Выполнение запроса и проверка результата
        mockMvc.perform(get("/api/welding-machine-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Type"))
                .andExpect(jsonPath("$[0].status").value("Active"));
    }

    /**
     * Тест получения типа сварочной машины по ID
     * Проверяет успешное получение типа при существующем ID
     */
    @Test
    void getWeldingMachineTypeById_WhenExists_ShouldReturnType() throws Exception {
        when(weldingMachineTypeService.getWeldingMachineTypeById(1)).thenReturn(Optional.of(testType));

        mockMvc.perform(get("/api/welding-machine-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Type"));
    }

    /**
     * Тест получения типа сварочной машины по несуществующему ID
     * Проверяет возврат 404 при отсутствии типа
     */
    @Test
    void getWeldingMachineTypeById_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(weldingMachineTypeService.getWeldingMachineTypeById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/welding-machine-types/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Тест получения типа сварочной машины по имени
     * Проверяет успешное получение типа при существующем имени
     */
    @Test
    void getWeldingMachineTypeByName_WhenExists_ShouldReturnType() throws Exception {
        when(weldingMachineTypeService.getWeldingMachineTypeByName("Test Type")).thenReturn(Optional.of(testType));

        mockMvc.perform(get("/api/welding-machine-types/name/Test Type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Type"));
    }

    /**
     * Тест получения типов сварочных машин по статусу
     * Проверяет успешное получение списка типов с определенным статусом
     */
    @Test
    void getWeldingMachineTypesByStatus_ShouldReturnListOfTypes() throws Exception {
        List<WeldingMachineType> types = Arrays.asList(testType);
        when(weldingMachineTypeService.getWeldingMachineTypesByStatus(GeneralStatus.Active)).thenReturn(types);

        mockMvc.perform(get("/api/welding-machine-types/status/Active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("Active"));
    }

    /**
     * Тест получения типов сварочных машин по статусу с неверным статусом
     * Проверяет возврат 400 при неверном статусе
     */
    @Test
    void getWeldingMachineTypesByStatus_WithInvalidStatus_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/welding-machine-types/status/InvalidStatus"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Тест поиска типов сварочных машин
     * Проверяет успешный поиск типов по поисковому запросу
     */
    @Test
    void searchWeldingMachineTypes_ShouldReturnListOfTypes() throws Exception {
        List<WeldingMachineType> types = Arrays.asList(testType);
        when(weldingMachineTypeService.searchWeldingMachineTypes("Test")).thenReturn(types);

        mockMvc.perform(get("/api/welding-machine-types/search")
                .param("searchTerm", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Type"));
    }

    /**
     * Тест создания нового типа сварочной машины
     * Проверяет успешное создание нового типа
     */
    @Test
    void createWeldingMachineType_ShouldReturnCreatedType() throws Exception {
        when(weldingMachineTypeService.createWeldingMachineType(any(WeldingMachineType.class)))
                .thenReturn(testType);

        mockMvc.perform(post("/api/welding-machine-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testType)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Type"));
    }

    /**
     * Тест обновления типа сварочной машины
     * Проверяет успешное обновление существующего типа
     */
    @Test
    void updateWeldingMachineType_WhenExists_ShouldReturnUpdatedType() throws Exception {
        when(weldingMachineTypeService.updateWeldingMachineType(any(WeldingMachineType.class)))
                .thenReturn(testType);

        mockMvc.perform(put("/api/welding-machine-types/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Type"));
    }

    /**
     * Тест удаления типа сварочной машины
     * Проверяет успешное удаление типа
     */
    @Test
    void deleteWeldingMachineType_WhenExists_ShouldReturnNoContent() throws Exception {
        doNothing().when(weldingMachineTypeService).deleteWeldingMachineType(1);

        mockMvc.perform(delete("/api/welding-machine-types/1"))
                .andExpect(status().isNoContent());

        verify(weldingMachineTypeService, times(1)).deleteWeldingMachineType(1);
    }

    /**
     * Тест жесткого удаления типа сварочной машины
     * Проверяет успешное жесткое удаление типа
     */
    @Test
    void hardDeleteWeldingMachineType_WhenExists_ShouldReturnNoContent() throws Exception {
        doNothing().when(weldingMachineTypeService).hardDeleteWeldingMachineType(1);

        mockMvc.perform(delete("/api/welding-machine-types/1/hard"))
                .andExpect(status().isNoContent());

        verify(weldingMachineTypeService, times(1)).hardDeleteWeldingMachineType(1);
    }
}
