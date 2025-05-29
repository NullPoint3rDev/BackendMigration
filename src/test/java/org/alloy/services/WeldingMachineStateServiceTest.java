package org.alloy.services;

import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.repositories.WeldingMachineStateRepository;
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
public class WeldingMachineStateServiceTest {

    @MockBean
    private WeldingMachineStateRepository weldingMachineStateRepository;

    private WeldingMachineStateService weldingMachineStateService;
    private WeldingMachineState testState;

    @BeforeEach
    void setUp() {
        weldingMachineStateService = new WeldingMachineStateService(weldingMachineStateRepository);

        // Создаем тестовое состояние сварочной машины
        testState = new WeldingMachineState();
        testState.setId(1L);
        testState.setWeldingMachineId(1);
        testState.setWeldingMachineStatus(WeldingMachineStatus.Online);
        testState.setDateCreated(LocalDateTime.now());
        testState.setStateDurationMs(1000L);
    }

    /**
     * Тест получения всех состояний сварочных машин
     * Проверяет корректность получения списка всех состояний
     */
    @Test
    void getAllWeldingMachineStates_ShouldReturnAllStates() {
        // Подготавливаем тестовые данные
        List<WeldingMachineState> expectedStates = Arrays.asList(testState);
        when(weldingMachineStateRepository.findAll()).thenReturn(expectedStates);

        // Вызываем тестируемый метод
        List<WeldingMachineState> result = weldingMachineStateService.getAllWeldingMachineStates();

        // Проверяем результаты
        assertNotNull(result, "Список состояний не должен быть null");
        assertEquals(expectedStates.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testState.getId(), result.get(0).getId(), "ID должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineStateRepository, times(1)).findAll();
    }

    /**
     * Тест получения состояния сварочной машины по ID
     * Проверяет корректность получения состояния по существующему ID
     */
    @Test
    void getWeldingMachineStateById_WhenExists_ShouldReturnState() {
        // Подготавливаем тестовые данные
        when(weldingMachineStateRepository.findById(1L)).thenReturn(Optional.of(testState));

        // Вызываем тестируемый метод
        Optional<WeldingMachineState> result = weldingMachineStateService.getWeldingMachineStateById(1L);

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать состояние");
        assertEquals(testState.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testState.getWeldingMachineStatus(), result.get().getWeldingMachineStatus(), "Статус должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineStateRepository, times(1)).findById(1L);
    }

    /**
     * Тест получения состояний сварочной машины по ID машины
     * Проверяет корректность получения всех состояний для конкретной сварочной машины
     */
    @Test
    void getWeldingMachineStatesByMachineId_ShouldReturnStates() {
        // Подготавливаем тестовые данные
        List<WeldingMachineState> expectedStates = Arrays.asList(testState);
        when(weldingMachineStateRepository.findByWeldingMachineId(1L)).thenReturn(expectedStates);

        // Вызываем тестируемый метод
        List<WeldingMachineState> result = weldingMachineStateService.getWeldingMachineStatesByMachineId(1L);

        // Проверяем результаты
        assertNotNull(result, "Список состояний не должен быть null");
        assertEquals(expectedStates.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testState.getWeldingMachineId(), result.get(0).getWeldingMachineId(), "ID сварочной машины должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineStateRepository, times(1)).findByWeldingMachineId(1L);
    }

    /**
     * Тест получения последнего состояния сварочной машины
     * Проверяет корректность получения последнего состояния для конкретной сварочной машины
     */
    @Test
    void getLatestWeldingMachineState_ShouldReturnLatestState() {
        // Подготавливаем тестовые данные
        when(weldingMachineStateRepository.findTopByWeldingMachineIdOrderByDateCreatedDesc(1L))
                .thenReturn(Optional.of(testState));

        // Вызываем тестируемый метод
        Optional<WeldingMachineState> result = weldingMachineStateService.getLatestWeldingMachineState(1L);

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать состояние");
        assertEquals(testState.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testState.getWeldingMachineId(), result.get().getWeldingMachineId(), "ID сварочной машины должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineStateRepository, times(1))
                .findTopByWeldingMachineIdOrderByDateCreatedDesc(1L);
    }

    /**
     * Тест получения состояний сварочной машины по статусу
     * Проверяет корректность получения всех состояний с определенным статусом для конкретной сварочной машины
     */
    @Test
    void getWeldingMachineStatesByStatus_ShouldReturnStates() {
        // Подготавливаем тестовые данные
        List<WeldingMachineState> expectedStates = Arrays.asList(testState);
        when(weldingMachineStateRepository.findByWeldingMachineIdAndWeldingMachineStatus(1L, WeldingMachineStatus.Online))
                .thenReturn(expectedStates);

        // Вызываем тестируемый метод
        List<WeldingMachineState> result = weldingMachineStateService
                .getWeldingMachineStatesByStatus(1L, WeldingMachineStatus.Online);

        // Проверяем результаты
        assertNotNull(result, "Список состояний не должен быть null");
        assertEquals(expectedStates.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testState.getWeldingMachineStatus(), result.get(0).getWeldingMachineStatus(), "Статус должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(weldingMachineStateRepository, times(1))
                .findByWeldingMachineIdAndWeldingMachineStatus(1L, WeldingMachineStatus.Online);
    }

    /**
     * Тест создания нового состояния сварочной машины
     * Проверяет корректность создания нового состояния с валидными данными
     */
    @Test
    void createWeldingMachineState_WithValidData_ShouldCreateState() {
        // Подготавливаем тестовые данные
        when(weldingMachineStateRepository.save(any(WeldingMachineState.class))).thenReturn(testState);

        // Вызываем тестируемый метод
        WeldingMachineState result = weldingMachineStateService.createWeldingMachineState(testState);

        // Проверяем результаты
        assertNotNull(result, "Созданное состояние не должно быть null");
        assertEquals(testState.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testState.getWeldingMachineStatus(), result.getWeldingMachineStatus(), "Статус должен совпадать");
        assertNotNull(result.getDateCreated(), "Дата создания не должна быть null");
        
        // Проверяем, что метод репозитория был вызван
        verify(weldingMachineStateRepository, times(1)).save(testState);
    }

    /**
     * Тест создания состояния сварочной машины без ID машины
     * Проверяет, что создание состояния без ID машины вызывает исключение
     */
    @Test
    void createWeldingMachineState_WithoutMachineId_ShouldThrowException() {
        // Подготавливаем тестовые данные
        testState.setWeldingMachineId(null);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.createWeldingMachineState(testState);
        }, "Должно быть выброшено исключение при попытке создать состояние без ID машины");
    }

    /**
     * Тест создания состояния сварочной машины без статуса
     * Проверяет, что создание состояния без статуса вызывает исключение
     */
    @Test
    void createWeldingMachineState_WithoutStatus_ShouldThrowException() {
        // Подготавливаем тестовые данные
        testState.setWeldingMachineStatus(null);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.createWeldingMachineState(testState);
        }, "Должно быть выброшено исключение при попытке создать состояние без статуса");
    }

    /**
     * Тест обновления состояния сварочной машины
     * Проверяет корректность обновления существующего состояния
     */
    @Test
    void updateWeldingMachineState_WithValidData_ShouldUpdateState() {
        // Подготавливаем тестовые данные
        when(weldingMachineStateRepository.findById(1L)).thenReturn(Optional.of(testState));
        when(weldingMachineStateRepository.save(any(WeldingMachineState.class))).thenReturn(testState);

        // Вызываем тестируемый метод
        WeldingMachineState result = weldingMachineStateService.updateWeldingMachineState(testState);

        // Проверяем результаты
        assertNotNull(result, "Обновленное состояние не должно быть null");
        assertEquals(testState.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testState.getWeldingMachineStatus(), result.getWeldingMachineStatus(), "Статус должен совпадать");
        
        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineStateRepository, times(1)).findById(1L);
        verify(weldingMachineStateRepository, times(1)).save(testState);
    }

    /**
     * Тест обновления состояния сварочной машины без ID
     * Проверяет, что обновление состояния без ID вызывает исключение
     */
    @Test
    void updateWeldingMachineState_WithoutId_ShouldThrowException() {
        // Подготавливаем тестовые данные
        testState.setId(null);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.updateWeldingMachineState(testState);
        }, "Должно быть выброшено исключение при попытке обновить состояние без ID");
    }

    /**
     * Тест обновления несуществующего состояния сварочной машины
     * Проверяет, что обновление несуществующего состояния вызывает исключение
     */
    @Test
    void updateWeldingMachineState_WithNonExistentId_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineStateRepository.findById(1L)).thenReturn(Optional.empty());

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.updateWeldingMachineState(testState);
        }, "Должно быть выброшено исключение при попытке обновить несуществующее состояние");
    }

    /**
     * Тест удаления состояния сварочной машины
     * Проверяет корректность удаления существующего состояния
     */
    @Test
    void deleteWeldingMachineState_WhenExists_ShouldDeleteState() {
        // Подготавливаем тестовые данные
        when(weldingMachineStateRepository.existsById(1L)).thenReturn(true);

        // Вызываем тестируемый метод
        weldingMachineStateService.deleteWeldingMachineState(1L);

        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineStateRepository, times(1)).existsById(1L);
        verify(weldingMachineStateRepository, times(1)).deleteById(1L);
    }

    /**
     * Тест удаления несуществующего состояния сварочной машины
     * Проверяет, что удаление несуществующего состояния вызывает исключение
     */
    @Test
    void deleteWeldingMachineState_WhenNotExists_ShouldThrowException() {
        // Подготавливаем тестовые данные
        when(weldingMachineStateRepository.existsById(1L)).thenReturn(false);

        // Проверяем, что вызов метода вызывает исключение
        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.deleteWeldingMachineState(1L);
        }, "Должно быть выброшено исключение при попытке удалить несуществующее состояние");
    }

    /**
     * Тест удаления всех состояний сварочной машины
     * Проверяет корректность удаления всех состояний для конкретной сварочной машины
     */
    @Test
    void deleteAllWeldingMachineStates_ShouldDeleteAllStates() {
        // Подготавливаем тестовые данные
        List<WeldingMachineState> states = Arrays.asList(testState);
        when(weldingMachineStateRepository.findByWeldingMachineId(1L)).thenReturn(states);

        // Вызываем тестируемый метод
        weldingMachineStateService.deleteAllWeldingMachineStates(1L);

        // Проверяем, что методы репозитория были вызваны
        verify(weldingMachineStateRepository, times(1)).findByWeldingMachineId(1L);
        verify(weldingMachineStateRepository, times(1)).deleteAll(states);
    }
}
