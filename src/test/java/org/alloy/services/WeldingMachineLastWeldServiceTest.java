package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.alloy.repositories.WeldingMachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeldingMachineLastWeldServiceTest {

    private WeldingMachineRepository weldingMachineRepository;
    private WeldingMachineLastWeldService service;

    @BeforeEach
    void setUp() {
        weldingMachineRepository = mock(WeldingMachineRepository.class);
        service = new WeldingMachineLastWeldService();
        ReflectionTestUtils.setField(service, "weldingMachineRepository", weldingMachineRepository);
    }

    @Test
    void updatesLastWeldAtWhenSvarkaEnds() {
        WeldingMachine machine = new WeldingMachine();
        machine.setId(7);
        machine.setMac("AA:BB:CC:DD:EE:FF");
        machine.setLastWeldAt(LocalDateTime.of(2026, 6, 25, 10, 0));
        when(weldingMachineRepository.findByMac("AA:BB:CC:DD:EE:FF")).thenReturn(Optional.of(machine));

        StateSummary previous = summaryWithState("Сварка");
        StateSummary current = summaryWithState("Аппарат включен");

        service.updateFromPanelState(
                "AA:BB:CC:DD:EE:FF", previous, current, LocalDateTime.of(2026, 6, 25, 14, 35));

        assertEquals(LocalDateTime.of(2026, 6, 25, 14, 35), machine.getLastWeldAt());
        verify(weldingMachineRepository).save(machine);
    }

    @Test
    void ignoresWhenStillSvarka() {
        WeldingMachine machine = new WeldingMachine();
        machine.setId(7);
        machine.setMac("AA:BB:CC:DD:EE:FF");
        when(weldingMachineRepository.findByMac("AA:BB:CC:DD:EE:FF")).thenReturn(Optional.of(machine));

        service.updateFromPanelState(
                "AA:BB:CC:DD:EE:FF",
                summaryWithState("Сварка"),
                summaryWithState("Сварка"),
                LocalDateTime.of(2026, 6, 25, 14, 35));

        assertNull(machine.getLastWeldAt());
        verify(weldingMachineRepository, never()).save(machine);
    }

    @Test
    void ignoresWhenSvarkaStarts() {
        WeldingMachine machine = new WeldingMachine();
        machine.setId(7);
        machine.setMac("AA:BB:CC:DD:EE:FF");
        when(weldingMachineRepository.findByMac("AA:BB:CC:DD:EE:FF")).thenReturn(Optional.of(machine));

        service.updateFromPanelState(
                "AA:BB:CC:DD:EE:FF",
                summaryWithState("Аппарат включен"),
                summaryWithState("Сварка"),
                LocalDateTime.of(2026, 6, 25, 14, 35));

        assertNull(machine.getLastWeldAt());
        verify(weldingMachineRepository, never()).save(machine);
    }

    @Test
    void ignoresHighCurrentWithoutSvarkaText() {
        assertFalse(WeldingMachineLastWeldService.isExplicitSvarka(summaryWithState("Аппарат включен")));
        StateSummary withCurrent = summaryWithState("Аппарат включен");
        withCurrent.getProperties().put("State.I", prop("296"));
        assertFalse(WeldingMachineLastWeldService.isWelding(withCurrent));
    }

    @Test
    void explicitSvarkaOnlyExactText() {
        assertTrue(WeldingMachineLastWeldService.isExplicitSvarka("Сварка"));
        assertTrue(WeldingMachineLastWeldService.isExplicitSvarka("welding"));
        assertFalse(WeldingMachineLastWeldService.isExplicitSvarka("Режим сварки"));
        assertFalse(WeldingMachineLastWeldService.isExplicitSvarka("Аппарат включен"));
    }

    private static StateSummary summaryWithState(String stateText) {
        StateSummary summary = new StateSummary();
        summary.setProperties(new HashMap<>());
        summary.getProperties().put("Состояние аппарата", prop(stateText));
        return summary;
    }

    private static StateSummaryPropertyValue prop(String value) {
        StateSummaryPropertyValue p = new StateSummaryPropertyValue();
        p.setValue(value);
        return p;
    }
}
