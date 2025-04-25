package org.alloy.controllers;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.services.WeldingMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/welding-machines")
public class WeldingMachineController {

    private final WeldingMachineService weldingMachineService;

    @Autowired
    public WeldingMachineController(WeldingMachineService weldingMachineService) {
        this.weldingMachineService = weldingMachineService;
    }

    @GetMapping
    public ResponseEntity<List<WeldingMachine>> getAllWeldingMachines() {
        List<WeldingMachine> weldingMachines = weldingMachineService.getAllWeldingMachines();
        return ResponseEntity.ok(weldingMachines);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachine> getWeldingMachineById(@PathVariable Integer id) {
        return weldingMachineService.getWeldingMachineById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/serial-number/{serialNumber}")
    public ResponseEntity<WeldingMachine> getWeldingMachineBySerialNumber(@PathVariable String serialNumber) {
        return weldingMachineService.getWeldingMachineBySerialNumber(serialNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<WeldingMachine>> getWeldingMachinesByOrganizationId(@PathVariable Integer organizationId) {
        List<WeldingMachine> weldingMachines = weldingMachineService.getWeldingMachinesByOrganizationId(organizationId);
        return ResponseEntity.ok(weldingMachines);
    }

    @GetMapping("/type/{typeId}")
    public ResponseEntity<List<WeldingMachine>> getWeldingMachinesByTypeId(@PathVariable Integer typeId) {
        List<WeldingMachine> weldingMachines = weldingMachineService.getWeldingMachinesByTypeId(typeId);
        return ResponseEntity.ok(weldingMachines);
    }

    @GetMapping("/search")
    public ResponseEntity<List<WeldingMachine>> searchWeldingMachines(
            @RequestParam Integer organizationId,
            @RequestParam(required = false) String searchTerm) {
        List<WeldingMachine> weldingMachines = weldingMachineService.searchWeldingMachines(organizationId, searchTerm);
        return ResponseEntity.ok(weldingMachines);
    }

    @PostMapping
    public ResponseEntity<WeldingMachine> createWeldingMachine(@RequestBody WeldingMachine weldingMachine) {
        try {
            WeldingMachine createdWeldingMachine = weldingMachineService.createWeldingMachine(weldingMachine);
            return new ResponseEntity<>(createdWeldingMachine, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeldingMachine> updateWeldingMachine(@PathVariable Integer id, @RequestBody WeldingMachine weldingMachine) {
        try {
            weldingMachine.setId(id);
            WeldingMachine updatedWeldingMachine = weldingMachineService.updateWeldingMachine(weldingMachine);
            return ResponseEntity.ok(updatedWeldingMachine);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeldingMachine(@PathVariable Integer id) {
        try {
            weldingMachineService.deleteWeldingMachine(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteWeldingMachine(@PathVariable Integer id) {
        try {
            weldingMachineService.hardDeleteWeldingMachine(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
