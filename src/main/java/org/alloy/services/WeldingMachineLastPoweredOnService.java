package org.alloy.services;

import org.alloy.models.MonitorActivityMode;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.alloy.repositories.WeldingMachineParameterValueRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Хранит и восстанавливает момент последнего включения аппарата.
 */
@Service
@Transactional
public class WeldingMachineLastPoweredOnService {

    private static final int BACKFILL_LOOKBACK_DAYS = 14;
    /** Лимит состояний при разборе истории (защита от десятков тысяч строк на аппарат). */
    private static final int BACKFILL_MAX_STATES = 500;
    /** PostgreSQL / JDBC: IN (...) не должен содержать десятки тысяч id. */
    private static final int PROPS_LOAD_BATCH_SIZE = 200;
    private static final String[] WORK_TIME_SINCE_POWER_ON_KEYS = {
            "Core.WorkTimeSincePowerOn",
            "Время работы с включения"
    };
    private static final String[] MACHINE_STATE_TEXT_KEYS = {
            "Состояние аппарата",
            "WeldingMachineState",
            "State.WeldingMachineState"
    };

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private WeldingMachineParameterValueRepository parameterValueRepository;

    public void updateFromTelemetry(
            WeldingMachine machine,
            WeldingMachineState previousState,
            StateSummary stateSummary,
            LocalDateTime eventTime) {
        if (machine == null || machine.getId() == null || stateSummary == null || eventTime == null) {
            return;
        }

        Map<String, String> newProps = propsFromSummary(stateSummary);
        String newStateText = MonitorActivityClassifier.pickMachineStateText(newProps);
        BigDecimal newCurrent = MonitorActivityClassifier.pickCurrentAmps(newProps);
        MonitorActivityMode prevMode = classifyPrevious(previousState);
        MonitorActivityMode newMode = MonitorActivityClassifier.classify(
                buildProbeState(stateSummary.getStatus()),
                newStateText,
                newCurrent);

        if (newMode == MonitorActivityMode.off) {
            return;
        }

        LocalDateTime candidate = powerOnFromWorkTime(stateSummary, eventTime);
        if (candidate == null && prevMode == MonitorActivityMode.off) {
            candidate = eventTime;
        }
        if (candidate == null && machine.getLastPoweredOnAt() == null) {
            candidate = eventTime;
        }
        if (candidate == null) {
            return;
        }

        LocalDateTime existing = machine.getLastPoweredOnAt();
        if (existing == null || candidate.isAfter(existing)) {
            machine.setLastPoweredOnAt(candidate);
            weldingMachineRepository.save(machine);
        }
    }

    /**
     * Для ответа API: если в БД пусто — оценка по последним состояниям (без записи в БД).
     */
    public LocalDateTime resolveForDisplay(Integer weldingMachineId) {
        if (weldingMachineId == null) {
            return null;
        }
        return resolveFromHistory(weldingMachineId);
    }

    private LocalDateTime resolveFromHistory(Integer weldingMachineId) {
        LocalDateTime since = LocalDateTime.now().minusDays(BACKFILL_LOOKBACK_DAYS);
        LocalDateTime until = LocalDateTime.now();
        List<WeldingMachineState> recentDesc = weldingMachineStateRepository.findByWeldingMachineIdAndDateRange(
                weldingMachineId,
                since,
                until,
                PageRequest.of(0, BACKFILL_MAX_STATES));
        if (recentDesc.isEmpty()) {
            return null;
        }
        List<WeldingMachineState> states = new ArrayList<>(recentDesc);
        Collections.reverse(states);

        Map<Long, Map<String, String>> propsByStateId = loadPropsByStateId(states);
        MonitorActivityMode prevMode = MonitorActivityMode.off;
        LocalDateTime lastPowerOn = null;

        for (WeldingMachineState s : states) {
            Map<String, String> props = propsByStateId.getOrDefault(s.getId(), Collections.emptyMap());
            String stateText = MonitorActivityClassifier.pickMachineStateText(props);
            BigDecimal current = MonitorActivityClassifier.pickCurrentAmps(props);
            MonitorActivityMode mode = MonitorActivityClassifier.classify(s, stateText, current);
            LocalDateTime at = s.getDateCreated();
            if (at == null) {
                prevMode = mode;
                continue;
            }

            if (mode != MonitorActivityMode.off) {
                LocalDateTime fromCounter = powerOnFromWorkTimeProps(props, at);
                if (fromCounter != null) {
                    lastPowerOn = fromCounter;
                } else if (prevMode == MonitorActivityMode.off) {
                    lastPowerOn = at;
                }
            }
            prevMode = mode;
        }
        return lastPowerOn;
    }

    private MonitorActivityMode classifyPrevious(WeldingMachineState previousState) {
        if (previousState == null) {
            return MonitorActivityMode.off;
        }
        Map<String, String> props = Collections.emptyMap();
        if (previousState.getId() != null) {
            props = loadPropsByStateId(Collections.singletonList(previousState))
                    .getOrDefault(previousState.getId(), Collections.emptyMap());
        }
        String stateText = MonitorActivityClassifier.pickMachineStateText(props);
        BigDecimal current = MonitorActivityClassifier.pickCurrentAmps(props);
        return MonitorActivityClassifier.classify(previousState, stateText, current);
    }

    private WeldingMachineState buildProbeState(WeldingMachineStatus status) {
        WeldingMachineState probe = new WeldingMachineState();
        probe.setWeldingMachineStatus(status);
        return probe;
    }

    private LocalDateTime powerOnFromWorkTime(StateSummary summary, LocalDateTime eventTime) {
        if (summary == null || summary.getProperties() == null || eventTime == null) {
            return null;
        }
        return powerOnFromWorkTimeProps(propsFromSummary(summary), eventTime);
    }

    private LocalDateTime powerOnFromWorkTimeProps(Map<String, String> props, LocalDateTime eventTime) {
        Long workSec = readWorkTimeSeconds(props);
        if (workSec == null || workSec < 0) {
            return null;
        }
        return eventTime.minusSeconds(workSec);
    }

    private Long readWorkTimeSeconds(Map<String, String> props) {
        if (props == null || props.isEmpty()) {
            return null;
        }
        for (String key : WORK_TIME_SINCE_POWER_ON_KEYS) {
            String raw = props.get(key);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                String normalized = raw.trim().replace(',', '.').replaceAll("[^\\d.+-]", "");
                if (normalized.isEmpty()) {
                    continue;
                }
                double val = Double.parseDouble(normalized);
                if (!Double.isFinite(val) || val < 0) {
                    continue;
                }
                return (long) val;
            } catch (NumberFormatException ignored) {
                // next key
            }
        }
        return null;
    }

    private Map<String, String> propsFromSummary(StateSummary summary) {
        Map<String, String> out = new HashMap<>();
        if (summary.getProperties() == null) {
            return out;
        }
        for (Map.Entry<String, StateSummaryPropertyValue> e : summary.getProperties().entrySet()) {
            if (e.getValue() != null && e.getValue().getValue() != null) {
                out.put(e.getKey(), e.getValue().getValue());
            }
        }
        for (String key : MACHINE_STATE_TEXT_KEYS) {
            if (!out.containsKey(key) && summary.getProperties().containsKey(key)) {
                StateSummaryPropertyValue v = summary.getProperties().get(key);
                if (v != null && v.getValue() != null) {
                    out.put(key, v.getValue());
                }
            }
        }
        return out;
    }

    private Map<Long, Map<String, String>> loadPropsByStateId(List<WeldingMachineState> states) {
        List<Long> ids = states.stream()
                .map(WeldingMachineState::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> codes = List.of(
                "Состояние аппарата",
                "WeldingMachineState",
                "State.WeldingMachineState",
                "State.I",
                "Ток",
                "Current",
                "current",
                "Core.WorkTimeSincePowerOn",
                "Время работы с включения"
        );
        Map<Long, Map<String, String>> out = new HashMap<>();
        for (int offset = 0; offset < ids.size(); offset += PROPS_LOAD_BATCH_SIZE) {
            int end = Math.min(offset + PROPS_LOAD_BATCH_SIZE, ids.size());
            List<Long> batch = ids.subList(offset, end);
            List<WeldingMachineParameterValue> values =
                    parameterValueRepository.findByStateIdsAndPropertyCodes(batch, codes);
            for (WeldingMachineParameterValue pv : values) {
                if (pv.getWeldingMachineStateId() == null || pv.getPropertyCode() == null) {
                    continue;
                }
                String val = pv.getValue();
                if (val == null || val.isBlank()) {
                    val = pv.getRawValue();
                }
                if (val == null || val.isBlank()) {
                    continue;
                }
                out.computeIfAbsent(pv.getWeldingMachineStateId(), k -> new HashMap<>())
                        .put(pv.getPropertyCode(), val.trim());
            }
        }
        return out;
    }
}
