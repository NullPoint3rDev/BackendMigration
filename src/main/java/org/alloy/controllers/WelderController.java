package org.alloy.controllers;

import org.alloy.models.entities.Welder;
import org.alloy.services.WelderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/welders")
public class WelderController {
    
    @Autowired
    private WelderService welderService;
    
    @GetMapping
    public ResponseEntity<List<Welder>> getAllWelders() {
        List<Welder> welders = welderService.getAllWelders();
        return ResponseEntity.ok(welders);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Welder> getWelderById(@PathVariable Long id) {
        Optional<Welder> welder = welderService.getWelderById(id);
        return welder.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Welder> createWelder(@RequestBody Welder welder) {
        Welder createdWelder = welderService.createWelder(welder);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWelder);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Welder> updateWelder(@PathVariable Long id, @RequestBody Welder welderDetails) {
        Welder updatedWelder = welderService.updateWelder(id, welderDetails);
        if (updatedWelder != null) {
            return ResponseEntity.ok(updatedWelder);
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWelder(@PathVariable Long id) {
        boolean deleted = welderService.deleteWelder(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Welder>> getWeldersByStatus(@PathVariable Welder.WelderStatus status) {
        List<Welder> welders = welderService.getWeldersByStatus(status);
        return ResponseEntity.ok(welders);
    }
    
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Welder>> getWeldersByDepartment(@PathVariable String department) {
        List<Welder> welders = welderService.getWeldersByDepartment(department);
        return ResponseEntity.ok(welders);
    }
    
    @GetMapping("/name/{name}")
    public ResponseEntity<List<Welder>> getWeldersByName(@PathVariable String name) {
        List<Welder> welders = welderService.getWeldersByName(name);
        return ResponseEntity.ok(welders);
    }
    
    @GetMapping("/grade/{grade}")
    public ResponseEntity<List<Welder>> getWeldersByGrade(@PathVariable String grade) {
        List<Welder> welders = welderService.getWeldersByGrade(grade);
        return ResponseEntity.ok(welders);
    }
    
    @GetMapping("/rfid/{rfidCode}")
    public ResponseEntity<Welder> getWelderByRfidCode(@PathVariable String rfidCode) {
        Welder welder = welderService.getWelderByRfidCode(rfidCode);
        if (welder != null) {
            return ResponseEntity.ok(welder);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Welder> getWelderByEmployeeId(@PathVariable String employeeId) {
        Welder welder = welderService.getWelderByEmployeeId(employeeId);
        if (welder != null) {
            return ResponseEntity.ok(welder);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Welder>> searchWelders(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Welder.WelderStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String grade) {
        List<Welder> welders = welderService.getWeldersByFilters(name, status, department, grade);
        return ResponseEntity.ok(welders);
    }
}
