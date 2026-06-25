package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * «Последний шов» = серверное время перехода текста «Сварка» в любое другое состояние.
 * Пока идёт сварка, поле не меняется (остаётся предыдущий завершённый шов).
 */
@Service
@Transactional
public class WeldingMachineLastWeldService {

    private static final String[] MACHINE_STATE_TEXT_KEYS = {
            "Состояние аппарата",
            "WeldingMachineState",
            "State.WeldingMachineState"
    };

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    /**
     * На каждом пакете телеметрии (до троттлинга БД): запись только при окончании сварки.
     */
    public void updateFromPanelState(
            String mac,
            StateSummary previous,
            StateSummary current,
            LocalDateTime eventTime) {
        if (mac == null || mac.isBlank() || current == null || eventTime == null) {
            return;
        }
        if (!isExplicitSvarka(previous) || isExplicitSvarka(current)) {
            return;
        }
        Optional<WeldingMachine> machineOpt = resolveMachine(mac);
        if (!machineOpt.isPresent()) {
            return;
        }
        WeldingMachine machine = machineOpt.get();
        if (machine.getId() == null) {
            return;
        }
        LocalDateTime existing = machine.getLastWeldAt();
        if (existing == null || eventTime.isAfter(existing)) {
            machine.setLastWeldAt(eventTime);
            weldingMachineRepository.save(machine);
        }
    }

    /** Для ответа API при пустом поле: без пересчёта истории. */
    public LocalDateTime resolveForDisplay(Integer weldingMachineId) {
        return null;
    }

    static boolean isExplicitSvarka(StateSummary summary) {
        return isExplicitSvarka(pickMachineStateText(summary));
    }

    static boolean isExplicitSvarka(String machineStateText) {
        if (machineStateText == null || machineStateText.isBlank()) {
            return false;
        }
        String lower = machineStateText.toLowerCase(Locale.ROOT).trim();
        return "сварка".equals(lower) || "welding".equals(lower);
    }

    static String pickMachineStateText(StateSummary summary) {
        if (summary == null || summary.getProperties() == null) {
            return null;
        }
        for (String key : MACHINE_STATE_TEXT_KEYS) {
            StateSummaryPropertyValue v = summary.getProperties().get(key);
            if (v != null && v.getValue() != null && !v.getValue().isBlank()) {
                return v.getValue().trim();
            }
        }
        return null;
    }

    private Optional<WeldingMachine> resolveMachine(String mac) {
        String normalized = mac.trim();
        Optional<WeldingMachine> opt = weldingMachineRepository.findByMac(normalized);
        if (!opt.isPresent()) {
            opt = weldingMachineRepository.findByMac(normalized.toUpperCase());
        }
        if (!opt.isPresent()) {
            opt = weldingMachineRepository.findByMac(normalized.toLowerCase());
        }
        return opt;
    }
}
