package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WeldingMachineStateService {

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Transactional
    public void saveMachineState(String mac, StateSummary stateSummary) {
        try {
            // Находим сварочный аппарат по MAC-адресу
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
            if (!machineOpt.isPresent()) {
                System.err.println("[STATE-SERVICE] Сварочный аппарат с MAC " + mac + " не найден");
                return;
            }

            WeldingMachine machine = machineOpt.get();

            // Создаем новое состояние
            WeldingMachineState state = new WeldingMachineState();
            state.setWeldingMachineId(machine.getId());
            state.setDateCreated(LocalDateTime.now());
            state.setDateUpdated(LocalDateTime.now());
            state.setWeldingMachineStatus(stateSummary.getStatus());
            state.setStateDurationMs(0L); // Можно вычислить на основе предыдущего состояния

            // Устанавливаем ошибку, если есть
            if (stateSummary.getErrorCode() != null) {
                state.setErrorCode(stateSummary.getErrorCode());
            }

            // Сохраняем состояние
            weldingMachineStateRepository.save(state);

            // Обновляем время последнего онлайн
            machine.setLastOnlineOn(LocalDateTime.now());
            weldingMachineRepository.save(machine);

            System.out.println("[STATE-SERVICE] ✅ Состояние сохранено для аппарата " + mac);

        } catch (Exception e) {
            System.err.println("[STATE-SERVICE] Ошибка сохранения состояния: " + e.getMessage());
        }
    }

    public StateSummary getCurrentState(String mac) {
        try {
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
            if (!machineOpt.isPresent()) {
                return null;
            }

            WeldingMachine machine = machineOpt.get();
            Optional<WeldingMachineState> stateOpt = weldingMachineStateRepository
                    .findTopByWeldingMachineIdOrderByDateCreatedDesc(machine.getId());

            if (stateOpt.isPresent()) {
                WeldingMachineState state = stateOpt.get();
                return convertToStateSummary(state);
            }

        } catch (Exception e) {
            System.err.println("[STATE-SERVICE] Ошибка получения состояния: " + e.getMessage());
        }

        return null;
    }

    private StateSummary convertToStateSummary(WeldingMachineState state) {
        StateSummary summary = new StateSummary();
        summary.setWeldingMachineStateId(state.getId());
        summary.setDateCreated(state.getDateCreated());
        summary.setLastDatetimeUpdate(state.getDateUpdated());
        summary.setStatus(state.getWeldingMachineStatus());
        summary.setStateDurationMs(state.getStateDurationMs());
        summary.setErrorCode(state.getErrorCode());
        summary.setWeldingMaterialId(state.getWeldingMaterialId());
        summary.setLimitsExceeded(state.getLimitsExceeded() != null ? state.getLimitsExceeded() : false);
        summary.setWeldingLimitProgramId(state.getWeldingLimitProgramId());
        summary.setWeldingLimitProgramName(state.getWeldingLimitProgramName());

        return summary;
    }
}
