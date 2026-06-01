package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.AlloyWebMvcTest;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.services.WeldingMachineStateService;
import org.alloy.services.Wt2AccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AlloyWebMvcTest(WeldingMachineStateController.class)
@WithMockUser
public class WeldingMachineStateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeldingMachineStateService weldingMachineStateService;

    @MockBean
    private Wt2AccessService wt2AccessService;

    @Autowired
    private ObjectMapper objectMapper;

    private WeldingMachineState testState;

    /**
     * Инициализация тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый объект WeldingMachineState
        testState = new WeldingMachineState();
        testState.setId(1L);
        testState.setWeldingMachineId(1);
        testState.setDateCreated(LocalDateTime.now());
        testState.setDateUpdated(LocalDateTime.now());
        testState.setWeldingMachineStatus(WeldingMachineStatus.Online);
        testState.setRfid("TEST_RFID");
        testState.setControl("TEST_CONTROL");
        testState.setControlStatus(1);
        testState.setControlState("TEST_STATE");
        testState.setStateDurationMs(1000L);
        testState.setErrorCode("TEST_ERROR");
        testState.setWeldingMaterialId(1);
        testState.setLimitsExceeded(false);
        testState.setWeldingLimitProgramId(1);
        testState.setWeldingLimitProgramName("TEST_PROGRAM");
        testState.setGasWeldingMaterialId(1);
        testState.setOrganizationUnitId(1);
        testState.setMd5("TEST_MD5");
        testState.setNormGasFlow(1.0);

        when(wt2AccessService.filterWeldingMachineStates(any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Тест получения всех состояний сварочных машин
     * Проверяет успешное получение списка всех состояний
     */
    @Test
    void getAllWeldingMachineStates_ShouldReturnListOfStates() throws Exception {
        // Подготовка данных
        List<WeldingMachineState> states = Arrays.asList(testState);
        when(weldingMachineStateService.getAllWeldingMachineStates()).thenReturn(states);

        // Выполнение запроса и проверка результата
        mockMvc.perform(get("/welding-machine-states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].weldingMachineId").value(1))
                .andExpect(jsonPath("$[0].weldingMachineStatus").value("Online"));
    }

    /**
     * Тест получения состояния сварочной машины по ID
     * Проверяет успешное получение состояния при существующем ID
     */
    @Test
    void getWeldingMachineStateById_WhenExists_ShouldReturnState() throws Exception {
        when(weldingMachineStateService.getWeldingMachineStateById(1L)).thenReturn(Optional.of(testState));

        mockMvc.perform(get("/welding-machine-states/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weldingMachineId").value(1));
    }

    /**
     * Тест получения состояния сварочной машины по несуществующему ID
     * Проверяет возврат 404 при отсутствии состояния
     */
    @Test
    void getWeldingMachineStateById_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(weldingMachineStateService.getWeldingMachineStateById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/welding-machine-states/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Тест получения состояний сварочной машины по ID машины
     * Проверяет успешное получение списка состояний для конкретной машины
     */
    @Test
    void getWeldingMachineStatesByMachineId_ShouldReturnListOfStates() throws Exception {
        List<WeldingMachineState> states = Arrays.asList(testState);
        when(weldingMachineStateService.getWeldingMachineStatesByMachineId(1)).thenReturn(states);

        mockMvc.perform(get("/welding-machine-states/machine/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].weldingMachineId").value(1));
    }

    /**
     * Тест получения последнего состояния сварочной машины
     * Проверяет успешное получение последнего состояния для конкретной машины
     */
    @Test
    void getLatestWeldingMachineState_WhenExists_ShouldReturnState() throws Exception {
        when(weldingMachineStateService.getLatestWeldingMachineState(1)).thenReturn(Optional.of(testState));

        mockMvc.perform(get("/welding-machine-states/machine/1/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weldingMachineId").value(1));
    }

    /**
     * Тест получения состояний сварочной машины по статусу
     * Проверяет успешное получение списка состояний с определенным статусом
     */
    @Test
    void getWeldingMachineStatesByStatus_ShouldReturnListOfStates() throws Exception {
        List<WeldingMachineState> states = Arrays.asList(testState);
        when(weldingMachineStateService.getWeldingMachineStatesByStatus(1, WeldingMachineStatus.Online))
                .thenReturn(states);

        mockMvc.perform(get("/welding-machine-states/machine/1/status/Online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].weldingMachineStatus").value("Online"));
    }

    /**
     * Тест создания нового состояния сварочной машины
     * Проверяет успешное создание нового состояния
     */
    @Test
    void createWeldingMachineState_ShouldReturnCreatedState() throws Exception {
        when(weldingMachineStateService.createWeldingMachineState(any(WeldingMachineState.class)))
                .thenReturn(testState);

        mockMvc.perform(post("/welding-machine-states")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testState)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weldingMachineId").value(1));
    }

    /**
     * Тест обновления состояния сварочной машины
     * Проверяет успешное обновление существующего состояния
     */
    @Test
    void updateWeldingMachineState_WhenExists_ShouldReturnUpdatedState() throws Exception {
        when(weldingMachineStateService.updateWeldingMachineState(any(WeldingMachineState.class)))
                .thenReturn(testState);

        mockMvc.perform(put("/welding-machine-states/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testState)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weldingMachineId").value(1));
    }

    /**
     * Тест удаления состояния сварочной машины
     * Проверяет успешное удаление состояния
     */
    @Test
    void deleteWeldingMachineState_WhenExists_ShouldReturnNoContent() throws Exception {
        doNothing().when(weldingMachineStateService).deleteWeldingMachineState(1L);

        mockMvc.perform(delete("/welding-machine-states/1"))
                .andExpect(status().isNoContent());

        verify(weldingMachineStateService, times(1)).deleteWeldingMachineState(1L);
    }

    /**
     * Тест удаления всех состояний сварочной машины
     * Проверяет успешное удаление всех состояний для конкретной машины
     */
    @Test
    void deleteAllWeldingMachineStates_ShouldReturnNoContent() throws Exception {
        doNothing().when(weldingMachineStateService).deleteAllWeldingMachineStates(1);

        mockMvc.perform(delete("/welding-machine-states/machine/1"))
                .andExpect(status().isNoContent());

        verify(weldingMachineStateService, times(1)).deleteAllWeldingMachineStates(1);
    }
}
