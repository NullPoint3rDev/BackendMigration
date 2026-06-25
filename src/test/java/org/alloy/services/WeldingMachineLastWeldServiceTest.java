package org.alloy.services;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeldingMachineLastWeldServiceTest {

    private WeldingMachineRepository weldingMachineRepository;
    private WeldingMachineStateRepository weldingMachineStateRepository;
    private WeldingMachineLastWeldService service;

    @BeforeEach
    void setUp() {
        weldingMachineRepository = mock(WeldingMachineRepository.class);
        weldingMachineStateRepository = mock(WeldingMachineStateRepository.class);
        service = new WeldingMachineLastWeldService();
        ReflectionTestUtils.setField(service, "weldingMachineRepository", weldingMachineRepository);
        ReflectionTestUtils.setField(service, "weldingMachineStateRepository", weldingMachineStateRepository);
    }

    @Test
    void updatesLastWeldAtOnlyForWeldingStates() {
        WeldingMachine machine = new WeldingMachine();
        machine.setId(7);
        machine.setLastWeldAt(LocalDateTime.of(2026, 6, 25, 10, 0));

        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Welding);
        state.setDateCreated(LocalDateTime.of(2026, 6, 25, 10, 47));

        service.updateFromTelemetry(machine, state);

        assertEquals(LocalDateTime.of(2026, 6, 25, 10, 47), machine.getLastWeldAt());
        verify(weldingMachineRepository).save(machine);
    }

    @Test
    void ignoresNonWeldingStates() {
        WeldingMachine machine = new WeldingMachine();
        machine.setId(7);

        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(WeldingMachineStatus.Online);
        state.setDateCreated(LocalDateTime.of(2026, 6, 25, 10, 47));

        service.updateFromTelemetry(machine, state);

        assertNull(machine.getLastWeldAt());
        verify(weldingMachineRepository, never()).save(machine);
    }

    @Test
    void resolvesLastWeldFromLatestWeldingState() {
        WeldingMachineState lastWelding = new WeldingMachineState();
        lastWelding.setDateCreated(LocalDateTime.of(2026, 6, 25, 10, 47));
        when(weldingMachineStateRepository.findTopByWeldingMachineIdAndWeldingMachineStatusOrderByDateCreatedDesc(
                7, WeldingMachineStatus.Welding)).thenReturn(Optional.of(lastWelding));

        LocalDateTime resolved = service.resolveForDisplay(7);

        assertEquals(LocalDateTime.of(2026, 6, 25, 10, 47), resolved);
    }
}
