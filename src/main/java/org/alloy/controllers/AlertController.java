package org.alloy.controllers;

import org.alloy.models.entities.Alert;
import org.alloy.services.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    @Autowired
    private AlertService alertService;

    @GetMapping
    public List<Alert> getAll() {
        return alertService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getById(@PathVariable Integer id) {
        return alertService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Alert create(@RequestBody Alert alert) {
        return alertService.save(alert);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Alert> update(@PathVariable Integer id, @RequestBody Alert alert) {
        if (!alertService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        alert.setId(id);
        return ResponseEntity.ok(alertService.save(alert));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!alertService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        alertService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 