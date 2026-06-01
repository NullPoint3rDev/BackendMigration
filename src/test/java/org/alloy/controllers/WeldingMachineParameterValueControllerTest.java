package org.alloy.controllers;

import lombok.With;
import org.alloy.MvcTestConfig;
import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.services.WeldingMachineParameterValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeldingMachineParameterValueController.class)
@Import(MvcTestConfig.class)
@WithMockUser
public class WeldingMachineParameterValueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeldingMachineParameterValueService parameterValueService;

    @Autowired
    private ObjectMapper objectMapper;

    private WeldingMachineParameterValue testParameterValue;

    /**
     * Инициализация тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый объект WeldingMachineParameterValue
        testParameterValue = new WeldingMachineParameterValue();
        testParameterValue.setId(1L);
        testParameterValue.setWeldingMachineStateId(1L);
        testParameterValue.setPropertyCode("TEST_PROPERTY");
        testParameterValue.setValue("100");
        testParameterValue.setPropertyType("NUMBER");
        testParameterValue.setRawValue("100.0");
        testParameterValue.setLimitsExceeded(false);
        testParameterValue.setLimitMin(0.0f);
        testParameterValue.setLimitMax(200.0f);
    }

    /**
     * Тест получения всех значений параметров
     * Проверяет успешное получение списка всех значений параметров
     */
    @Test
    void getAllParameterValues_ShouldReturnListOfParameterValues() throws Exception {
        // Подготовка данных
        List<WeldingMachineParameterValue> parameterValues = Arrays.asList(testParameterValue);
        when(parameterValueService.getAllParameterValues()).thenReturn(parameterValues);

        // Выполнение запроса и проверка результата
        mockMvc.perform(get("/welding-machine-parameters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].parameterName").value("TEST_PROPERTY"));
    }

    /**
     * Тест получения значения параметра по ID
     * Проверяет успешное получение значения параметра при существующем ID
     */
    @Test
    void getParameterValueById_WhenExists_ShouldReturnParameterValue() throws Exception {
        when(parameterValueService.getParameterValueById(1L)).thenReturn(Optional.of(testParameterValue));

        mockMvc.perform(get("/welding-machine-parameters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.parameterName").value("TEST_PROPERTY"));
    }

    /**
     * Тест получения значения параметра по несуществующему ID
     * Проверяет возврат 404 при отсутствии значения параметра
     */
    @Test
    void getParameterValueById_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(parameterValueService.getParameterValueById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/welding-machine-parameters/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Тест получения значений параметров по ID состояния
     * Проверяет успешное получение списка значений параметров для конкретного состояния
     */
    @Test
    void getParameterValuesByStateId_ShouldReturnListOfParameterValues() throws Exception {
        List<WeldingMachineParameterValue> parameterValues = Arrays.asList(testParameterValue);
        when(parameterValueService.getParameterValuesByStateId(1L)).thenReturn(parameterValues);

        mockMvc.perform(get("/welding-machine-parameters/state/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    /**
     * Тест получения значения параметра по ID состояния и коду свойства
     * Проверяет успешное получение значения параметра при существующих данных
     */
    @Test
    void getParameterValueByStateIdAndPropertyCode_WhenExists_ShouldReturnParameterValue() throws Exception {
        when(parameterValueService.getParameterValueByStateIdAndPropertyCode(1L, "TEST_PROPERTY"))
                .thenReturn(Optional.of(testParameterValue));

        mockMvc.perform(get("/welding-machine-parameters/state/1/property/TEST_PROPERTY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.parameterName").value("TEST_PROPERTY"));
    }

    /**
     * Тест получения превышенных значений параметров
     * Проверяет успешное получение списка параметров с превышенными лимитами
     */
    @Test
    void getExceededParameterValues_ShouldReturnListOfExceededValues() throws Exception {
        testParameterValue.setLimitsExceeded(true);
        List<WeldingMachineParameterValue> exceededValues = Arrays.asList(testParameterValue);
        when(parameterValueService.getExceededParameterValues(1L)).thenReturn(exceededValues);

        mockMvc.perform(get("/welding-machine-parameters/state/1/exceeded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(true));
    }

    /**
     * Тест создания нового значения параметра
     * Проверяет успешное создание нового значения параметра
     */
    @Test
    void createParameterValue_ShouldReturnCreatedParameterValue() throws Exception {
        when(parameterValueService.createParameterValue(any(WeldingMachineParameterValue.class)))
                .thenReturn(testParameterValue);

        mockMvc.perform(post("/welding-machine-parameters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testParameterValue)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.parameterName").value("TEST_PROPERTY"));
    }

    /**
     * Тест обновления значения параметра
     * Проверяет успешное обновление существующего значения параметра
     */
    @Test
    void updateParameterValue_WhenExists_ShouldReturnUpdatedParameterValue() throws Exception {
        when(parameterValueService.updateParameterValue(any(WeldingMachineParameterValue.class)))
                .thenReturn(testParameterValue);

        mockMvc.perform(put("/welding-machine-parameters/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testParameterValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.parameterName").value("TEST_PROPERTY"));
    }

    /**
     * Тест удаления значения параметра
     * Проверяет успешное удаление значения параметра
     */
    @Test
    void deleteParameterValue_WhenExists_ShouldReturnNoContent() throws Exception {
        doNothing().when(parameterValueService).deleteParameterValue(1L);

        mockMvc.perform(delete("/welding-machine-parameters/1"))
                .andExpect(status().isNoContent());

        verify(parameterValueService, times(1)).deleteParameterValue(1L);
    }

    /**
     * Тест удаления всех значений параметров для состояния
     * Проверяет успешное удаление всех значений параметров для конкретного состояния
     */
    @Test
    void deleteAllParameterValues_ShouldReturnNoContent() throws Exception {
        doNothing().when(parameterValueService).deleteAllParameterValues(1L);

        mockMvc.perform(delete("/welding-machine-parameters/state/1"))
                .andExpect(status().isNoContent());

        verify(parameterValueService, times(1)).deleteAllParameterValues(1L);
    }
}
