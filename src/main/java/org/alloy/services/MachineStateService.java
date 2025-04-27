package org.alloy.services;

import org.alloy.models.configuration.PropertyCodes;
import org.alloy.models.machine.MachineState;
import org.alloy.models.machine.MachineStatus;
import org.alloy.repositories.WeldingMachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MachineStateService {
    private final WeldingMachineRepository weldingMachineRepository;
    private final Map<Integer, MachineState> machineStates = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Optional<MachineState> getCurrentMachineState(int machineId, boolean includeRawValues) {
        return Optional.ofNullable(machineStates.get(machineId))
                .map(state -> {
                    if (!includeRawValues) {
                        state.setRawValues(null);
                    }
                    return state;
                });
    }

    @Transactional
    public void updateMachineState(int machineId, Map<String, String> values) {
        MachineState state = machineStates.computeIfAbsent(machineId, k -> new MachineState());
        state.setLastUpdateTime(LocalDateTime.now());
        state.setRawValues(values);

        // Update machine status based on control parameters
        String stateCtrl = values.get(PropertyCodes.StateCtrl);
        if (stateCtrl != null) {
            switch (stateCtrl) {
                case "0":
                    state.setStatus(MachineStatus.READY);
                    break;
                case "1":
                    state.setStatus(MachineStatus.WORKING);
                    break;
                case "2":
                    state.setStatus(MachineStatus.ERROR);
                    break;
                default:
                    state.setStatus(MachineStatus.OFFLINE);
                    break;
            }
        }

        // Update current values
        state.setCurrent(parseDoubleOrDefault(values.get(PropertyCodes.Current), 0.0));
        state.setVoltage(parseDoubleOrDefault(values.get(PropertyCodes.Voltage), 0.0));
        state.setPower(parseDoubleOrDefault(values.get(PropertyCodes.Power), 0.0));
        state.setGasFlow(parseDoubleOrDefault(values.get(PropertyCodes.GasFlow), 0.0));
        state.setTemperature(parseDoubleOrDefault(values.get(PropertyCodes.Temperature), 0.0));

        // Update barcode if present
        String barcode = values.get(PropertyCodes.Barcode);
        if (barcode != null && !barcode.isEmpty()) {
            state.setBarcode(barcode);
        }
    }

    private double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional
    public void markMachineOffline(int machineId) {
        machineStates.computeIfPresent(machineId, (k, state) -> {
            state.setStatus(MachineStatus.OFFLINE);
            state.setLastUpdateTime(LocalDateTime.now());
            return state;
        });
    }

    @Transactional
    public void clearMachineState(int machineId) {
        machineStates.remove(machineId);
    }
}
