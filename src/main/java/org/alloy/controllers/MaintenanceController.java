package org.alloy.controllers;

import org.alloy.models.entities.Maintenance;
import org.alloy.services.MaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @Autowired
    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public ResponseEntity<List<Maintenance>> getAllMaintenanceRecords() {
        List<Maintenance> records = maintenanceService.getAllMaintenanceRecords();
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Maintenance> getMaintenanceRecordById(@PathVariable Integer id) {
        return maintenanceService.getMaintenanceRecordById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/machine/{machineId}")
    public ResponseEntity<List<Maintenance>> getMaintenanceRecordsByMachineId(@PathVariable Integer machineId) {
        List<Maintenance> records = maintenanceService.getMaintenanceRecordsByMachineId(machineId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/machine/{machineId}/latest")
    public ResponseEntity<Maintenance> getLatestMaintenanceRecord(@PathVariable Integer machineId) {
        return maintenanceService.getLatestMaintenanceRecord(machineId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/machine/{machineId}/status/{status}")
    public ResponseEntity<List<Maintenance>> getMaintenanceRecordsByStatus(
            @PathVariable Integer machineId,
            @PathVariable String status) {
        List<Maintenance> records = maintenanceService.getMaintenanceRecordsByStatus(machineId, status);
        return ResponseEntity.ok(records);
    }

    @PostMapping
    public ResponseEntity<Maintenance> createMaintenanceRecord(@RequestBody Maintenance maintenance) {
        try {
            Maintenance createdRecord = maintenanceService.createMaintenanceRecord(maintenance);
            return new ResponseEntity<>(createdRecord, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Maintenance> updateMaintenanceRecord(@PathVariable Integer id, @RequestBody Maintenance maintenance) {
        try {
            maintenance.setId(id);
            Maintenance updatedRecord = maintenanceService.updateMaintenanceRecord(maintenance);
            return ResponseEntity.ok(updatedRecord);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenanceRecord(@PathVariable Integer id) {
        try {
            maintenanceService.deleteMaintenanceRecord(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/machine/{machineId}")
    public ResponseEntity<Void> deleteAllMaintenanceRecords(@PathVariable Integer machineId) {
        try {
            maintenanceService.deleteAllMaintenanceRecords(machineId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
