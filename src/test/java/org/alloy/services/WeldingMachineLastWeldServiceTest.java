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
    void updatesLastWeldAtWhenWeldingEnds() {
        WeldingMachine machine = new WeldingMachine();
        machine.setId(7);
        machine.setLastWeldAt(LocalDateTime.of(2026, 6, 25, 10, 0));

        WeldingMachineState previous = new WeldingMachineState();
        previous.setWeldingMachineStatus(WeldingMachineStatus.Welding);

        WeldingMachineState current = new WeldingMachineState();
        current.setWeldingMachineStatus(WeldingMachineStatus.Idle);

        service.updateFromTelemetry(machine, previous, current, LocalDateTime.of(2026, 6, 25, 10, 47));

        assertEquals(LocalDateTime.of(2026, 6, 25, 10, 47), machine.getLastWeldAt());
        verify(weldingMachineRepository).save(machine);
    }

    @Test
    void ignoresWhenWeldingDidNotEnd() {
        WeldingMachine machine = new WeldingMachine();
        machine.setId(7);

        WeldingMachineState previous = new WeldingMachineState();
        previous.setWeldingMachineStatus(WeldingMachineStatus.Idle);

        WeldingMachineState current = new WeldingMachineState();
        current.setWeldingMachineStatus(WeldingMachineStatus.Welding);

        service.updateFromTelemetry(machine, previous, current, LocalDateTime.of(2026, 6, 25, 10, 47));

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
