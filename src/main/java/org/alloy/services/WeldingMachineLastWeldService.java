package org.alloy.services;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** Момент последней сварки на аппарате: тот же источник, что и у истории графика. */
@Service
@Transactional
public class WeldingMachineLastWeldService {

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    public void updateFromTelemetry(WeldingMachine machine, WeldingMachineState state) {
        if (machine == null || machine.getId() == null || state == null) {
            return;
        }
        if (state.getWeldingMachineStatus() != WeldingMachineStatus.Welding) {
            return;
        }
        LocalDateTime candidate = state.getDateCreated();
        if (candidate == null) {
            return;
        }
        LocalDateTime existing = machine.getLastWeldAt();
        if (existing == null || candidate.isAfter(existing)) {
            machine.setLastWeldAt(candidate);
            weldingMachineRepository.save(machine);
        }
    }

    /** Для ответа API, если в БД ещё пусто: берём последнюю точку истории со статусом Welding. */
    public LocalDateTime resolveForDisplay(Integer weldingMachineId) {
        if (weldingMachineId == null) {
            return null;
        }
        Optional<WeldingMachineState> lastWelding = weldingMachineStateRepository
                .findTopByWeldingMachineIdAndWeldingMachineStatusOrderByDateCreatedDesc(
                        weldingMachineId, WeldingMachineStatus.Welding);
        return lastWelding.map(WeldingMachineState::getDateCreated).orElse(null);
    }
}
