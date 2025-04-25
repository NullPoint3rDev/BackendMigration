package org.alloy.services;

import org.alloy.models.entities.WeldingMachineState;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WeldingMachineStateService {

    private final WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    public WeldingMachineStateService(WeldingMachineStateRepository weldingMachineStateRepository) {
        this.weldingMachineStateRepository = weldingMachineStateRepository;
    }

    public List<WeldingMachineState> getAllWeldingMachineStates() {
        return weldingMachineStateRepository.findAll();
    }

    public Optional<WeldingMachineState> getWeldingMachineStateById(Integer id) {
        return weldingMachineStateRepository.findById(id);
    }

    public List<WeldingMachineState> getWeldingMachineStatesByMachineId(Integer machineId) {
        return weldingMachineStateRepository.findByWeldingMachineId(machineId);
    }

    public Optional<WeldingMachineState> getLatestWeldingMachineState(Integer machineId) {
        return weldingMachineStateRepository.findTopByWeldingMachineIdOrderByDateCreatedDesc(machineId);
    }

    public List<WeldingMachineState> getWeldingMachineStatesByStatus(Integer machineId, String status) {
        return weldingMachineStateRepository.findByWeldingMachineIdAndStatus(machineId, status);
    }

    public WeldingMachineState createWeldingMachineState(WeldingMachineState state) {
        // Validate required fields
        if (state.getWeldingMachineId() == null) {
            throw new IllegalArgumentException("Welding machine ID is required");
        }
        if (state.getStatus() == null || state.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }

        // Set creation date
        state.setDateCreated(LocalDateTime.now());

        return weldingMachineStateRepository.save(state);
    }

    public WeldingMachineState updateWeldingMachineState(WeldingMachineState state) {
        // Validate ID
        if (state.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        // Check if state exists
        WeldingMachineState existingState = weldingMachineStateRepository.findById(state.getId())
                .orElseThrow(() -> new IllegalArgumentException("Welding machine state not found"));

        // Preserve creation date
        state.setDateCreated(existingState.getDateCreated());

        return weldingMachineStateRepository.save(state);
    }

    public void deleteWeldingMachineState(Integer id) {
        if (!weldingMachineStateRepository.existsById(id)) {
            throw new IllegalArgumentException("Welding machine state not found");
        }
        weldingMachineStateRepository.deleteById(id);
    }

    public void deleteAllWeldingMachineStates(Integer machineId) {
        List<WeldingMachineState> states = weldingMachineStateRepository.findByWeldingMachineId(machineId);
        weldingMachineStateRepository.deleteAll(states);
    }
}
