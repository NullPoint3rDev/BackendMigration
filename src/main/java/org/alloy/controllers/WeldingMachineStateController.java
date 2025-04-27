package org.alloy.controllers;

import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.services.WeldingMachineStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/welding-machine-states")
public class WeldingMachineStateController {

    private final WeldingMachineStateService weldingMachineStateService;

    @Autowired
    public WeldingMachineStateController(WeldingMachineStateService weldingMachineStateService) {
        this.weldingMachineStateService = weldingMachineStateService;
    }

    @GetMapping
    public ResponseEntity<List<WeldingMachineState>> getAllWeldingMachineStates() {
        List<WeldingMachineState> states = weldingMachineStateService.getAllWeldingMachineStates();
        return ResponseEntity.ok(states);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachineState> getWeldingMachineStateById(@PathVariable Long id) {
        return weldingMachineStateService.getWeldingMachineStateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/machine/{machineId}")
    public ResponseEntity<List<WeldingMachineState>> getWeldingMachineStatesByMachineId(@PathVariable Long machineId) {
        List<WeldingMachineState> states = weldingMachineStateService.getWeldingMachineStatesByMachineId(machineId);
        return ResponseEntity.ok(states);
    }

    @GetMapping("/machine/{machineId}/latest")
    public ResponseEntity<WeldingMachineState> getLatestWeldingMachineState(@PathVariable Long machineId) {
        return weldingMachineStateService.getLatestWeldingMachineState(machineId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/machine/{machineId}/status/{status}")
    public ResponseEntity<List<WeldingMachineState>> getWeldingMachineStatesByStatus(
            @PathVariable Long machineId,
            @PathVariable WeldingMachineStatus status) {
        List<WeldingMachineState> states = weldingMachineStateService.getWeldingMachineStatesByStatus(machineId, status);
        return ResponseEntity.ok(states);
    }

    @PostMapping
    public ResponseEntity<WeldingMachineState> createWeldingMachineState(@RequestBody WeldingMachineState state) {
        try {
            WeldingMachineState createdState = weldingMachineStateService.createWeldingMachineState(state);
            return new ResponseEntity<>(createdState, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeldingMachineState> updateWeldingMachineState(@PathVariable Long id, @RequestBody WeldingMachineState state) {
        try {
            state.setId(id);
            WeldingMachineState updatedState = weldingMachineStateService.updateWeldingMachineState(state);
            return ResponseEntity.ok(updatedState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeldingMachineState(@PathVariable Long id) {
        try {
            weldingMachineStateService.deleteWeldingMachineState(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/machine/{machineId}")
    public ResponseEntity<Void> deleteAllWeldingMachineStates(@PathVariable Long machineId) {
        try {
            weldingMachineStateService.deleteAllWeldingMachineStates(machineId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
