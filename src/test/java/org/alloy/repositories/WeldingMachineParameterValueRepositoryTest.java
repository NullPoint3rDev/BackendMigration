package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineParameterValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для репозитория WeldingMachineParameterValueRepository.
 * Тестирует все методы репозитория для работы с параметрами сварочной машины.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
public class WeldingMachineParameterValueRepositoryTest {

    @Autowired
    private WeldingMachineParameterValueRepository repository;

    @Autowired
    private EntityManager entityManager;

    private WeldingMachineParameterValue testParameterValue1;
    private WeldingMachineParameterValue testParameterValue2;
    private WeldingMachineParameterValue testParameterValue3;

    /**
     * Подготовка тестовых данных перед каждым тестом.
     * Создаем три тестовых параметра с разными значениями для тестирования различных сценариев.
     */
    @BeforeEach
    void setUp() {
        // Параметры ссылаются на welding_machine_state(id=1,2), которые тут не создаются (тест проверяет только
        // запросы по сырой колонке WeldingMachineStateID). Отключаем проверку FK в H2-сессии теста, чтобы
        // не упираться в referential integrity (схему/сущности не трогаем — только тестовая БД).
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        // Создаем первый тестовый параметр
        testParameterValue1 = new WeldingMachineParameterValue();
        testParameterValue1.setWeldingMachineStateId(1L);
        testParameterValue1.setPropertyCode("TEMPERATURE");
        testParameterValue1.setValue("100");
        testParameterValue1.setPropertyType("FLOAT");
        testParameterValue1.setRawValue("100.0");
        testParameterValue1.setLimitsExceeded(false);
        testParameterValue1.setLimitMin(0.0f);
        testParameterValue1.setLimitMax(200.0f);

        // Создаем второй тестовый параметр
        testParameterValue2 = new WeldingMachineParameterValue();
        testParameterValue2.setWeldingMachineStateId(1L);
        testParameterValue2.setPropertyCode("PRESSURE");
        testParameterValue2.setValue("50");
        testParameterValue2.setPropertyType("FLOAT");
        testParameterValue2.setRawValue("50.0");
        testParameterValue2.setLimitsExceeded(true);
        testParameterValue2.setLimitMin(0.0f);
        testParameterValue2.setLimitMax(100.0f);

        // Создаем третий тестовый параметр
        testParameterValue3 = new WeldingMachineParameterValue();
        testParameterValue3.setWeldingMachineStateId(2L);
        testParameterValue3.setPropertyCode("TEMPERATURE");
        testParameterValue3.setValue("150");
        testParameterValue3.setPropertyType("FLOAT");
        testParameterValue3.setRawValue("150.0");
        testParameterValue3.setLimitsExceeded(false);
        testParameterValue3.setLimitMin(0.0f);
        testParameterValue3.setLimitMax(200.0f);

        // Сохраняем тестовые данные в базу
        entityManager.persist(testParameterValue1);
        entityManager.persist(testParameterValue2);
        entityManager.persist(testParameterValue3);
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Тест поиска параметров по ID состояния сварочной машины.
     * Проверяет, что метод возвращает все параметры, связанные с указанным состоянием.
     */
    @Test
    void findByWeldingMachineStateId_ShouldReturnAllParametersForState() {
        // Вызываем тестируемый метод
        List<WeldingMachineParameterValue> result = repository.findByWeldingMachineStateId(1L);

        // Проверяем результаты
        assertNotNull(result, "Результат не должен быть null");
        assertEquals(2, result.size(), "Должно быть найдено 2 параметра для состояния 1");
        
        // Проверяем, что найденные параметры соответствуют ожидаемым
        assertTrue(result.stream().anyMatch(p -> p.getPropertyCode().equals("TEMPERATURE")), 
            "Должен быть найден параметр TEMPERATURE");
        assertTrue(result.stream().anyMatch(p -> p.getPropertyCode().equals("PRESSURE")), 
            "Должен быть найден параметр PRESSURE");
    }

    /**
     * Тест поиска параметров по коду свойства.
     * Проверяет, что метод возвращает все параметры с указанным кодом свойства.
     */
    @Test
    void findByPropertyCode_ShouldReturnAllParametersWithCode() {
        // Вызываем тестируемый метод
        List<WeldingMachineParameterValue> result = repository.findByPropertyCode("TEMPERATURE");

        // Проверяем результаты
        assertNotNull(result, "Результат не должен быть null");
        assertEquals(2, result.size(), "Должно быть найдено 2 параметра с кодом TEMPERATURE");
        
        // Проверяем, что все найденные параметры имеют правильный код
        assertTrue(result.stream().allMatch(p -> p.getPropertyCode().equals("TEMPERATURE")), 
            "Все найденные параметры должны иметь код TEMPERATURE");
    }

    /**
     * Тест поиска параметра по ID состояния и коду свойства.
     * Проверяет, что метод возвращает конкретный параметр для указанного состояния и кода.
     */
    @Test
    void findByWeldingMachineStateIdAndPropertyCode_ShouldReturnSpecificParameter() {
        // Вызываем тестируемый метод
        Optional<WeldingMachineParameterValue> result = repository.findByWeldingMachineStateIdAndPropertyCode(1L, "TEMPERATURE");

        // Проверяем результаты
        assertTrue(result.isPresent(), "Должен быть найден параметр");
        assertEquals("TEMPERATURE", result.get().getPropertyCode(), "Код свойства должен совпадать");
        assertEquals(1L, result.get().getWeldingMachineStateId(), "ID состояния должен совпадать");
        assertEquals("100", result.get().getValue(), "Значение должно совпадать");
    }

    /**
     * Тест поиска параметров с превышенными лимитами для состояния.
     * Проверяет, что метод возвращает только параметры с превышенными лимитами.
     */
    @Test
    void findByWeldingMachineStateIdAndLimitsExceededTrue_ShouldReturnExceededParameters() {
        // Вызываем тестируемый метод
        List<WeldingMachineParameterValue> result = repository.findByWeldingMachineStateIdAndLimitsExceededTrue(1L);

        // Проверяем результаты
        assertNotNull(result, "Результат не должен быть null");
        assertEquals(1, result.size(), "Должен быть найден 1 параметр с превышенными лимитами");
        assertTrue(result.get(0).getLimitsExceeded(), "Найденный параметр должен иметь превышенные лимиты");
        assertEquals("PRESSURE", result.get(0).getPropertyCode(), "Код свойства должен быть PRESSURE");
    }

    /**
     * Тест поиска параметров по списку ID состояний и коду свойства.
     * Проверяет, что метод возвращает параметры для всех указанных состояний с заданным кодом.
     */
    @Test
    void findByStateIdsAndPropertyCode_ShouldReturnParametersForAllStates() {
        // Вызываем тестируемый метод
        List<WeldingMachineParameterValue> result = repository.findByStateIdsAndPropertyCode(
            Arrays.asList(1L, 2L), "TEMPERATURE");

        // Проверяем результаты
        assertNotNull(result, "Результат не должен быть null");
        assertEquals(2, result.size(), "Должно быть найдено 2 параметра");
        
        // Проверяем, что найдены параметры для обоих состояний
        assertTrue(result.stream().anyMatch(p -> p.getWeldingMachineStateId().equals(1L)), 
            "Должен быть найден параметр для состояния 1");
        assertTrue(result.stream().anyMatch(p -> p.getWeldingMachineStateId().equals(2L)), 
            "Должен быть найден параметр для состояния 2");
        
        // Проверяем, что все найденные параметры имеют правильный код
        assertTrue(result.stream().allMatch(p -> p.getPropertyCode().equals("TEMPERATURE")), 
            "Все найденные параметры должны иметь код TEMPERATURE");
    }
}
