package org.alloy.services;

import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.repositories.WeldingMachineParameterValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

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
public class WeldingMachineParameterValueServiceTest {

    @MockBean
    private WeldingMachineParameterValueRepository parameterValueRepository;

    @Autowired
    private WeldingMachineParameterValueService parameterValueService;

    private WeldingMachineParameterValue testParameterValue;

    @BeforeEach
    void setUp() {
        // Создаем тестовый параметр
        testParameterValue = new WeldingMachineParameterValue();
        testParameterValue.setId(1L);
        testParameterValue.setWeldingMachineStateId(100L);
        testParameterValue.setPropertyCode("TEST_PARAM");
        testParameterValue.setValue("42.5");
        testParameterValue.setPropertyType("NUMERIC");
        testParameterValue.setRawValue("42.5");
        testParameterValue.setLimitsExceeded(false);
        testParameterValue.setLimitMin(0.0f);
        testParameterValue.setLimitMax(100.0f);
    }

    /**
     * Тест получения всех значений параметров
     * Проверяет корректность получения списка всех значений параметров
     */
    @Test
    void getAllParameterValues_ShouldReturnAllValues() {
        // Подготавливаем тестовые данные
        List<WeldingMachineParameterValue> expectedValues = Arrays.asList(testParameterValue);
        when(parameterValueRepository.findAll()).thenReturn(expectedValues);

        // Вызываем тестируемый метод
        List<WeldingMachineParameterValue> result = parameterValueService.getAllParameterValues();

        // Проверяем результаты
        assertNotNull(result, "Список значений параметров не должен быть null");
        assertEquals(expectedValues.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testParameterValue.getId(), result.get(0).getId(), "ID должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(parameterValueRepository, times(1)).findAll();
    }

    /**
     * Тест получения значения параметра по ID
     * Проверяет корректность получения значения параметра по существующему ID
     */
    @Test
    void getParameterValueById_WhenExists_ShouldReturnValue() {
        // Подготавливаем тестовые данные
        when(parameterValueRepository.findById(1L)).thenReturn(Optional.of(testParameterValue));

        // Вызываем тестируемый метод
        Optional<WeldingMachineParameterValue> result = parameterValueService.getParameterValueById(1L);

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать значение параметра");
        assertEquals(testParameterValue.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testParameterValue.getValue(), result.get().getValue(), "Значение должно совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(parameterValueRepository, times(1)).findById(1L);
    }

    /**
     * Тест получения значений параметров по ID состояния
     * Проверяет корректность получения всех значений параметров для конкретного состояния
     */
    @Test
    void getParameterValuesByStateId_ShouldReturnValues() {
        // Подготавливаем тестовые данные
        List<WeldingMachineParameterValue> expectedValues = Arrays.asList(testParameterValue);
        when(parameterValueRepository.findByWeldingMachineStateId(100L)).thenReturn(expectedValues);

        // Вызываем тестируемый метод
        List<WeldingMachineParameterValue> result = parameterValueService.getParameterValuesByStateId(100L);

        // Проверяем результаты
        assertNotNull(result, "Список значений параметров не должен быть null");
        assertEquals(expectedValues.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testParameterValue.getWeldingMachineStateId(), result.get(0).getWeldingMachineStateId(), "ID состояния должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(parameterValueRepository, times(1)).findByWeldingMachineStateId(100L);
    }

    /**
     * Тест получения значения параметра по ID состояния и коду свойства
     * Проверяет корректность получения значения параметра по состоянию и коду свойства
     */
    @Test
    void getParameterValueByStateIdAndPropertyCode_WhenExists_ShouldReturnValue() {
        // Подготавливаем тестовые данные
        when(parameterValueRepository.findByWeldingMachineStateIdAndPropertyCode(100L, "TEST_PARAM"))
                .thenReturn(Optional.of(testParameterValue));

        // Вызываем тестируемый метод
        Optional<WeldingMachineParameterValue> result = parameterValueService
                .getParameterValueByStateIdAndPropertyCode(100L, "TEST_PARAM");

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать значение параметра");
        assertEquals(testParameterValue.getPropertyCode(), result.get().getPropertyCode(), "Код свойства должен совпадать");
        assertEquals(testParameterValue.getValue(), result.get().getValue(), "Значение должно совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(parameterValueRepository, times(1)).findByWeldingMachineStateIdAndPropertyCode(100L, "TEST_PARAM");
    }

    /**
     * Тест получения превышенных значений параметров
     * Проверяет корректность получения значений параметров, превысивших лимиты
     */
    @Test
    void getExceededParameterValues_ShouldReturnExceededValues() {
        // Подготавливаем тестовые данные
        testParameterValue.setLimitsExceeded(true);
        List<WeldingMachineParameterValue> expectedValues = Arrays.asList(testParameterValue);
        when(parameterValueRepository.findByWeldingMachineStateIdAndLimitsExceededTrue(100L))
                .thenReturn(expectedValues);

        // Вызываем тестируемый метод
        List<WeldingMachineParameterValue> result = parameterValueService.getExceededParameterValues(100L);

        // Проверяем результаты
        assertNotNull(result, "Список значений параметров не должен быть null");
        assertEquals(expectedValues.size(), result.size(), "Размер списка должен совпадать");
        assertTrue(result.get(0).getLimitsExceeded(), "Флаг превышения лимитов должен быть true");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(parameterValueRepository, times(1)).findByWeldingMachineStateIdAndLimitsExceededTrue(100L);
    }

    /**
     * Тест создания нового значения параметра
     * Проверяет корректность создания нового значения параметра
     */
    @Test
    void createParameterValue_WithValidData_ShouldCreateValue() {
        // Подготавливаем тестовые данные
        when(parameterValueRepository.findByWeldingMachineStateIdAndPropertyCode(100L, "TEST_PARAM"))
                .thenReturn(Optional.empty());
        when(parameterValueRepository.save(any(WeldingMachineParameterValue.class)))
                .thenReturn(testParameterValue);

        // Вызываем тестируемый метод
        WeldingMachineParameterValue result = parameterValueService.createParameterValue(testParameterValue);

        // Проверяем результаты
        assertNotNull(result, "Созданное значение параметра не должно быть null");
        assertEquals(testParameterValue.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testParameterValue.getValue(), result.getValue(), "Значение должно совпадать");
        
        // Проверяем, что методы репозитория были вызваны
        verify(parameterValueRepository, times(1))
                .findByWeldingMachineStateIdAndPropertyCode(100L, "TEST_PARAM");
        verify(parameterValueRepository, times(1)).save(testParameterValue);
    }

    /**
     * Тест создания значения параметра с существующим кодом свойства
     * Проверяет, что создание значения параметра с существующим кодом свойства вызывает исключение
     */
    @Test
    void createParameterValue_WithExistingPropertyCode_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(parameterValueRepository.findByWeldingMachineStateIdAndPropertyCode(100L, "TEST_PARAM"))
                .thenReturn(Optional.of(testParameterValue));

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            parameterValueService.createParameterValue(testParameterValue);
        }, "Должно быть выброшено исключение при попытке создать значение параметра с существующим кодом свойства");
    }

    /**
     * Тест обновления значения параметра
     * Проверяет корректность обновления существующего значения параметра
     */
    @Test
    void updateParameterValue_WithValidData_ShouldUpdateValue() {
        // Подготавливаем тестовые данные
        when(parameterValueRepository.findById(1L)).thenReturn(Optional.of(testParameterValue));
        when(parameterValueRepository.findByWeldingMachineStateIdAndPropertyCode(100L, "TEST_PARAM"))
                .thenReturn(Optional.empty());
        when(parameterValueRepository.save(any(WeldingMachineParameterValue.class)))
                .thenReturn(testParameterValue);

        // Вызываем тестируемый метод
        WeldingMachineParameterValue result = parameterValueService.updateParameterValue(testParameterValue);

        // Проверяем результаты
        assertNotNull(result, "Обновленное значение параметра не должно быть null");
        assertEquals(testParameterValue.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testParameterValue.getValue(), result.getValue(), "Значение должно совпадать");
        
        // Проверяем, что методы репозитория были вызваны
        verify(parameterValueRepository, times(1)).findById(1L);
        verify(parameterValueRepository, times(1))
                .findByWeldingMachineStateIdAndPropertyCode(100L, "TEST_PARAM");
        verify(parameterValueRepository, times(1)).save(testParameterValue);
    }

    /**
     * Тест удаления значения параметра
     * Проверяет корректность удаления существующего значения параметра
     */
    @Test
    void deleteParameterValue_WhenExists_ShouldDeleteValue() {
        // Подготавливаем тестовые данные
        when(parameterValueRepository.existsById(1L)).thenReturn(true);

        // Вызываем тестируемый метод
        parameterValueService.deleteParameterValue(1L);

        // Проверяем, что методы репозитория были вызваны
        verify(parameterValueRepository, times(1)).existsById(1L);
        verify(parameterValueRepository, times(1)).deleteById(1L);
    }

    /**
     * Тест удаления всех значений параметров для состояния
     * Проверяет корректность удаления всех значений параметров для конкретного состояния
     */
    @Test
    void deleteAllParameterValues_ShouldDeleteAllValues() {
        // Подготавливаем тестовые данные
        List<WeldingMachineParameterValue> values = Arrays.asList(testParameterValue);
        when(parameterValueRepository.findByWeldingMachineStateId(100L)).thenReturn(values);

        // Вызываем тестируемый метод
        parameterValueService.deleteAllParameterValues(100L);

        // Проверяем, что методы репозитория были вызваны
        verify(parameterValueRepository, times(1)).findByWeldingMachineStateId(100L);
        verify(parameterValueRepository, times(1)).deleteAll(values);
    }
}
