package org.alloy.controllers;

import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.services.WeldingDeviceManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/welding-devices")
@CrossOrigin(origins = "*")
public class WeldingDeviceController {

    @Autowired
    private WeldingDeviceManagerService deviceManager;

    /**
     * Получить состояние конкретного аппарата
     */
    @GetMapping("/{mac}/state")
    public ResponseEntity<StateSummary> getDeviceState(@PathVariable String mac) {
        StateSummary state = deviceManager.getDeviceState(mac);
        if (state != null) {
            return ResponseEntity.ok(state);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Получить статус подключения аппарата
     */
    @GetMapping("/{mac}/connection")
    public ResponseEntity<Map<String, Object>> getDeviceConnection(@PathVariable String mac) {
        boolean isConnected = deviceManager.isDeviceConnected(mac);
        Map<String, Object> response = Map.of(
            "mac", mac,
            "connected", isConnected,
            "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Получить состояния всех аппаратов
     */
    @GetMapping("/states")
    public ResponseEntity<Map<String, StateSummary>> getAllDeviceStates() {
        Map<String, StateSummary> states = deviceManager.getAllDeviceStates();
        return ResponseEntity.ok(states);
    }

    /**
     * Получить статусы подключения всех аппаратов
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Boolean>> getAllConnectionStatuses() {
        Map<String, Boolean> connections = deviceManager.getAllConnectionStatuses();
        return ResponseEntity.ok(connections);
    }

    /**
     * Получить статистику по аппаратам
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDeviceStatistics() {
        Map<String, Object> statistics = deviceManager.getDeviceStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Отметить аппарат как отключенный
     */
    @PostMapping("/{mac}/disconnect")
    public ResponseEntity<Map<String, String>> markDeviceDisconnected(@PathVariable String mac) {
        deviceManager.markDeviceDisconnected(mac);
        Map<String, String> response = Map.of(
            "message", "Аппарат " + mac + " отмечен как отключенный",
            "mac", mac
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Получить информацию о системе
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = Map.of(
            "service", "WeldTelecom Device Manager",
            "version", "1.0.0",
            "status", "running",
            "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(info);
    }
} 