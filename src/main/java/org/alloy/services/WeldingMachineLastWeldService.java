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
import java.util.Optional;
import java.util.stream.Collectors;

/** Момент последней сварки на аппарате (для списка «Сварочное оборудование»). */
@Service
@Transactional
public class WeldingMachineLastWeldService {

    private static final int BACKFILL_LOOKBACK_DAYS = 30;
    private static final int BACKFILL_MAX_STATES = 500;
    private static final int PROPS_LOAD_BATCH_SIZE = 200;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private WeldingMachineParameterValueRepository parameterValueRepository;

    public void updateFromTelemetry(WeldingMachine machine, StateSummary stateSummary, LocalDateTime eventTime) {
        if (machine == null || machine.getId() == null || stateSummary == null || eventTime == null) {
            return;
        }
        Map<String, String> props = propsFromSummary(stateSummary);
        String stateText = MonitorActivityClassifier.pickMachineStateText(props);
        BigDecimal current = MonitorActivityClassifier.pickCurrentAmps(props);
        MonitorActivityMode mode = MonitorActivityClassifier.classify(
                buildProbeState(stateSummary.getStatus()),
                stateText,
                current);
        if (mode != MonitorActivityMode.welding) {
            return;
        }
        LocalDateTime existing = machine.getLastWeldAt();
        if (existing == null || eventTime.isAfter(existing)) {
            machine.setLastWeldAt(eventTime);
            weldingMachineRepository.save(machine);
        }
    }

    /** Для ответа API, если в БД ещё не записано. */
    public LocalDateTime resolveForDisplay(Integer weldingMachineId) {
        if (weldingMachineId == null) {
            return null;
        }
        Optional<WeldingMachineState> lastWelding = weldingMachineStateRepository
                .findTopByWeldingMachineIdAndWeldingMachineStatusOrderByDateCreatedDesc(
                        weldingMachineId, WeldingMachineStatus.Welding);
        if (lastWelding.isPresent() && lastWelding.get().getDateCreated() != null) {
            return lastWelding.get().getDateCreated();
        }
        return resolveFromHistory(weldingMachineId);
    }

    private LocalDateTime resolveFromHistory(Integer weldingMachineId) {
        LocalDateTime since = LocalDateTime.now().minusDays(BACKFILL_LOOKBACK_DAYS);
        List<WeldingMachineState> recentDesc = weldingMachineStateRepository.findByWeldingMachineIdAndDateRange(
                weldingMachineId,
                since,
                LocalDateTime.now(),
                PageRequest.of(0, BACKFILL_MAX_STATES));
        if (recentDesc.isEmpty()) {
            return null;
        }
        List<WeldingMachineState> states = new ArrayList<>(recentDesc);
        Collections.reverse(states);
        Map<Long, Map<String, String>> propsByStateId = loadPropsByStateId(states);
        LocalDateTime lastWeld = null;
        for (WeldingMachineState s : states) {
            Map<String, String> props = propsByStateId.getOrDefault(s.getId(), Collections.emptyMap());
            String stateText = MonitorActivityClassifier.pickMachineStateText(props);
            BigDecimal current = MonitorActivityClassifier.pickCurrentAmps(props);
            MonitorActivityMode mode = MonitorActivityClassifier.classify(s, stateText, current);
            if (mode == MonitorActivityMode.welding && s.getDateCreated() != null) {
                lastWeld = s.getDateCreated();
            }
        }
        return lastWeld;
    }

    private WeldingMachineState buildProbeState(WeldingMachineStatus status) {
        WeldingMachineState probe = new WeldingMachineState();
        probe.setWeldingMachineStatus(status);
        return probe;
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
                "current"
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
