package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * «Последний шов» = серверное время окончания сварки (переход «Сварка» → другое состояние).
 */
@Service
public class WeldingMachineLastWeldService {

    private static final String[] MACHINE_STATE_TEXT_KEYS = {
            "Состояние аппарата",
            "WeldingMachineState",
            "State.WeldingMachineState"
    };

    private final TransactionTemplate transactionTemplate;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    public WeldingMachineLastWeldService(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * На каждом пакете телеметрии: запись только при окончании сварки.
     * Без @Transactional на методе — иначе каждый пакет держит JDBC-соединение.
     */
    public void updateFromPanelState(
            String mac,
            StateSummary previous,
            StateSummary current,
            LocalDateTime eventTime) {
        if (mac == null || mac.isBlank() || current == null || eventTime == null) {
            return;
        }
        if (!isWelding(previous) || isWelding(current)) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> persistLastWeldAt(mac, eventTime));
    }

    private void persistLastWeldAt(String mac, LocalDateTime eventTime) {
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

    static boolean isWelding(StateSummary summary) {
        if (summary == null) {
            return false;
        }
        WeldingMachineState state = new WeldingMachineState();
        state.setWeldingMachineStatus(summary.getStatus());
        Map<String, String> props = toPropsMap(summary);
        String machineStateText = MonitorActivityClassifier.pickMachineStateText(props);
        BigDecimal current = MonitorActivityClassifier.pickCurrentAmps(props);
        return MonitorActivityClassifier.isWelding(state, machineStateText, current);
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

    private static Map<String, String> toPropsMap(StateSummary summary) {
        Map<String, String> out = new HashMap<>();
        if (summary == null || summary.getProperties() == null) {
            return out;
        }
        summary.getProperties().forEach((key, value) -> {
            if (value != null && value.getValue() != null && !value.getValue().isBlank()) {
                out.put(key, value.getValue().trim());
            }
        });
        return out;
    }

    private Optional<WeldingMachine> resolveMachine(String mac) {
        String normalized = mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return weldingMachineRepository.findActiveByMac(normalized);
    }
}
