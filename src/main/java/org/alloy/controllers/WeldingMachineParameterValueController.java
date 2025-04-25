package org.alloy.controllers;

import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.services.WeldingMachineParameterValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/welding-machine-parameters")
public class WeldingMachineParameterValueController {

    private final WeldingMachineParameterValueService parameterValueService;

    @Autowired
    public WeldingMachineParameterValueController(WeldingMachineParameterValueService parameterValueService) {
        this.parameterValueService = parameterValueService;
    }

    @GetMapping
    public ResponseEntity<List<WeldingMachineParameterValue>> getAllParameterValues() {
        List<WeldingMachineParameterValue> values = parameterValueService.getAllParameterValues();
        return ResponseEntity.ok(values);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachineParameterValue> getParameterValueById(@PathVariable Integer id) {
        return parameterValueService.getParameterValueById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/state/{stateId}")
    public ResponseEntity<List<WeldingMachineParameterValue>> getParameterValuesByStateId(@PathVariable Integer stateId) {
        List<WeldingMachineParameterValue> values = parameterValueService.getParameterValuesByStateId(stateId);
        return ResponseEntity.ok(values);
    }

    @GetMapping("/state/{stateId}/property/{propertyCode}")
    public ResponseEntity<WeldingMachineParameterValue> getParameterValueByStateIdAndPropertyCode(
            @PathVariable Integer stateId,
            @PathVariable String propertyCode) {
        return parameterValueService.getParameterValueByStateIdAndPropertyCode(stateId, propertyCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/state/{stateId}/exceeded")
    public ResponseEntity<List<WeldingMachineParameterValue>> getExceededParameterValues(@PathVariable Integer stateId) {
        List<WeldingMachineParameterValue> values = parameterValueService.getExceededParameterValues(stateId);
        return ResponseEntity.ok(values);
    }

    @PostMapping
    public ResponseEntity<WeldingMachineParameterValue> createParameterValue(@RequestBody WeldingMachineParameterValue value) {
        try {
            WeldingMachineParameterValue createdValue = parameterValueService.createParameterValue(value);
            return new ResponseEntity<>(createdValue, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeldingMachineParameterValue> updateParameterValue(@PathVariable Integer id, @RequestBody WeldingMachineParameterValue value) {
        try {
            value.setId(id);
            WeldingMachineParameterValue updatedValue = parameterValueService.updateParameterValue(value);
            return ResponseEntity.ok(updatedValue);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParameterValue(@PathVariable Integer id) {
        try {
            parameterValueService.deleteParameterValue(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/state/{stateId}")
    public ResponseEntity<Void> deleteAllParameterValues(@PathVariable Integer stateId) {
        try {
            parameterValueService.deleteAllParameterValues(stateId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
