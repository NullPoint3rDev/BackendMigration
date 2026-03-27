package org.alloy.services;

import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WeldingMachineStateServiceTest {

    @Mock
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @InjectMocks
    private WeldingMachineStateService weldingMachineStateService;

    private WeldingMachineState testState;

    @BeforeEach
    void setUp() {
        testState = new WeldingMachineState();
        testState.setId(1L);
        testState.setWeldingMachineId(1);
        testState.setWeldingMachineStatus(WeldingMachineStatus.Online);
        testState.setDateCreated(LocalDateTime.now());
        testState.setStateDurationMs(1000L);
    }

    @Test
    void getAllWeldingMachineStates_ShouldReturnAllStates() {
        List<WeldingMachineState> expectedStates = Arrays.asList(testState);
        when(weldingMachineStateRepository.findAll()).thenReturn(expectedStates);

        List<WeldingMachineState> result = weldingMachineStateService.getAllWeldingMachineStates();

        assertNotNull(result, "Список состояний не должен быть null");
        assertEquals(expectedStates.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testState.getId(), result.get(0).getId(), "ID должен совпадать");

        verify(weldingMachineStateRepository, times(1)).findAll();
    }

    @Test
    void getWeldingMachineStateById_WhenExists_ShouldReturnState() {
        when(weldingMachineStateRepository.findById(1L)).thenReturn(Optional.of(testState));

        Optional<WeldingMachineState> result = weldingMachineStateService.getWeldingMachineStateById(1L);

        assertTrue(result.isPresent(), "Результат должен содержать состояние");
        assertEquals(testState.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testState.getWeldingMachineStatus(), result.get().getWeldingMachineStatus(), "Статус должен совпадать");

        verify(weldingMachineStateRepository, times(1)).findById(1L);
    }

    @Test
    void getWeldingMachineStatesByMachineId_ShouldReturnStates() {
        List<WeldingMachineState> expectedStates = Arrays.asList(testState);
        when(weldingMachineStateRepository.findByWeldingMachineId(1)).thenReturn(expectedStates);

        List<WeldingMachineState> result = weldingMachineStateService.getWeldingMachineStatesByMachineId(1);

        assertNotNull(result, "Список состояний не должен быть null");
        assertEquals(expectedStates.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testState.getWeldingMachineId(), result.get(0).getWeldingMachineId(), "ID сварочной машины должен совпадать");

        verify(weldingMachineStateRepository, times(1)).findByWeldingMachineId(1);
    }

    @Test
    void getLatestWeldingMachineState_ShouldReturnLatestState() {
        when(weldingMachineStateRepository.findTopByWeldingMachineIdOrderByDateCreatedDesc(1))
                .thenReturn(Optional.of(testState));

        Optional<WeldingMachineState> result = weldingMachineStateService.getLatestWeldingMachineState(1);

        assertTrue(result.isPresent(), "Результат должен содержать состояние");
        assertEquals(testState.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testState.getWeldingMachineId(), result.get().getWeldingMachineId(), "ID сварочной машины должен совпадать");

        verify(weldingMachineStateRepository, times(1))
                .findTopByWeldingMachineIdOrderByDateCreatedDesc(1);
    }

    @Test
    void getWeldingMachineStatesByStatus_ShouldReturnStates() {
        List<WeldingMachineState> expectedStates = Arrays.asList(testState);
        when(weldingMachineStateRepository.findByWeldingMachineIdAndWeldingMachineStatus(1, WeldingMachineStatus.Online))
                .thenReturn(expectedStates);

        List<WeldingMachineState> result = weldingMachineStateService
                .getWeldingMachineStatesByStatus(1, WeldingMachineStatus.Online);

        assertNotNull(result, "Список состояний не должен быть null");
        assertEquals(expectedStates.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testState.getWeldingMachineStatus(), result.get(0).getWeldingMachineStatus(), "Статус должен совпадать");

        verify(weldingMachineStateRepository, times(1))
                .findByWeldingMachineIdAndWeldingMachineStatus(1, WeldingMachineStatus.Online);
    }

    @Test
    void createWeldingMachineState_WithValidData_ShouldCreateState() {
        when(weldingMachineStateRepository.save(any(WeldingMachineState.class))).thenReturn(testState);

        WeldingMachineState result = weldingMachineStateService.createWeldingMachineState(testState);

        assertNotNull(result, "Созданное состояние не должно быть null");
        assertEquals(testState.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testState.getWeldingMachineStatus(), result.getWeldingMachineStatus(), "Статус должен совпадать");
        assertNotNull(result.getDateCreated(), "Дата создания не должна быть null");

        verify(weldingMachineStateRepository, times(1)).save(testState);
    }

    @Test
    void createWeldingMachineState_WithoutMachineId_ShouldThrowException() {
        testState.setWeldingMachineId(null);

        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.createWeldingMachineState(testState);
        }, "Должно быть выброшено исключение при попытке создать состояние без ID машины");
    }

    @Test
    void createWeldingMachineState_WithoutStatus_ShouldThrowException() {
        testState.setWeldingMachineStatus(null);

        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.createWeldingMachineState(testState);
        }, "Должно быть выброшено исключение при попытке создать состояние без статуса");
    }

    @Test
    void updateWeldingMachineState_WithValidData_ShouldUpdateState() {
        when(weldingMachineStateRepository.findById(1L)).thenReturn(Optional.of(testState));
        when(weldingMachineStateRepository.save(any(WeldingMachineState.class))).thenReturn(testState);

        WeldingMachineState result = weldingMachineStateService.updateWeldingMachineState(testState);

        assertNotNull(result, "Обновленное состояние не должно быть null");
        assertEquals(testState.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testState.getWeldingMachineStatus(), result.getWeldingMachineStatus(), "Статус должен совпадать");

        verify(weldingMachineStateRepository, times(1)).findById(1L);
        verify(weldingMachineStateRepository, times(1)).save(testState);
    }

    @Test
    void updateWeldingMachineState_WithoutId_ShouldThrowException() {
        testState.setId(null);

        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.updateWeldingMachineState(testState);
        }, "Должно быть выброшено исключение при попытке обновить состояние без ID");
    }

    @Test
    void updateWeldingMachineState_WithNonExistentId_ShouldThrowException() {
        when(weldingMachineStateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.updateWeldingMachineState(testState);
        }, "Должно быть выброшено исключение при попытке обновить несуществующее состояние");
    }

    @Test
    void deleteWeldingMachineState_WhenExists_ShouldDeleteState() {
        when(weldingMachineStateRepository.existsById(1L)).thenReturn(true);

        weldingMachineStateService.deleteWeldingMachineState(1L);

        verify(weldingMachineStateRepository, times(1)).existsById(1L);
        verify(weldingMachineStateRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteWeldingMachineState_WhenNotExists_ShouldThrowException() {
        when(weldingMachineStateRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            weldingMachineStateService.deleteWeldingMachineState(1L);
        }, "Должно быть выброшено исключение при попытке удалить несуществующее состояние");
    }

    @Test
    void deleteAllWeldingMachineStates_ShouldDeleteAllStates() {
        List<WeldingMachineState> states = Arrays.asList(testState);
        when(weldingMachineStateRepository.findByWeldingMachineId(1)).thenReturn(states);

        weldingMachineStateService.deleteAllWeldingMachineStates(1);

        verify(weldingMachineStateRepository, times(1)).findByWeldingMachineId(1);
        verify(weldingMachineStateRepository, times(1)).deleteAll(states);
    }
}
