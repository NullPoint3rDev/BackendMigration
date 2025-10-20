package org.alloy.controllers;

import org.alloy.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Контроллер для управления archive-style TCP сервером
 */
@RestController
@RequestMapping("/archive-devices")
public class ArchiveDeviceController {
    
    @Autowired
    private ArchiveStyleTcpListener tcpListener;
    
    @Autowired
    private ArchiveStyleOutboundService outboundService;
    
    @Autowired
    private ArchiveIncomingPacketsWorker packetsWorker;
    
    /**
     * Получить статистику подключений
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getConnectionStatistics() {
        Map<String, Object> stats = tcpListener.getConnectionStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Получить статистику воркера пакетов
     */
    @GetMapping("/worker-statistics")
    public ResponseEntity<Map<String, Object>> getWorkerStatistics() {
        Map<String, Object> stats = packetsWorker.getWorkerStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Получить статистику исходящих пакетов
     */
    @GetMapping("/outbound-statistics")
    public ResponseEntity<Map<String, Object>> getOutboundStatistics() {
        Map<String, Object> stats = outboundService.getOutboundStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Отправить команду устройству
     */
    @PostMapping("/send-command")
    public ResponseEntity<Map<String, Object>> sendCommand(
            @RequestParam String mac,
            @RequestParam String command) {
        
        try {
            outboundService.setOutboundPacket(mac, command);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Команда отправлена",
                "mac", mac,
                "command", command
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Ошибка отправки команды: " + e.getMessage(),
                "mac", mac,
                "command", command
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Отправить команду синхронизации времени
     */
    @PostMapping("/send-timesync")
    public ResponseEntity<Map<String, Object>> sendTimeSync(@RequestParam String mac) {
        try {
            String timeSyncCommand = outboundService.buildTimeSyncMessage(mac, true);
            outboundService.setOutboundPacket(mac, timeSyncCommand);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Команда синхронизации времени отправлена",
                "mac", mac,
                "command", timeSyncCommand
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Ошибка отправки синхронизации времени: " + e.getMessage(),
                "mac", mac
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Отправить команду запроса статуса
     */
    @PostMapping("/request-status")
    public ResponseEntity<Map<String, Object>> requestStatus(@RequestParam String mac) {
        try {
            String statusCommand = outboundService.buildStatusRequest(mac);
            outboundService.setOutboundPacket(mac, statusCommand);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Запрос статуса отправлен",
                "mac", mac,
                "command", statusCommand
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Ошибка отправки запроса статуса: " + e.getMessage(),
                "mac", mac
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Отправить команду сброса
     */
    @PostMapping("/reset-device")
    public ResponseEntity<Map<String, Object>> resetDevice(@RequestParam String mac) {
        try {
            String resetCommand = outboundService.buildResetCommand(mac);
            outboundService.setOutboundPacket(mac, resetCommand);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Команда сброса отправлена",
                "mac", mac,
                "command", resetCommand
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Ошибка отправки команды сброса: " + e.getMessage(),
                "mac", mac
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Отправить команду управления
     */
    @PostMapping("/send-control")
    public ResponseEntity<Map<String, Object>> sendControl(
            @RequestParam String mac,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer voltage,
            @RequestParam(required = false) Integer gasFlow) {
        
        try {
            Map<String, Object> parameters = Map.of(
                "current", current != null ? current : 0,
                "voltage", voltage != null ? voltage : 0,
                "gasFlow", gasFlow != null ? gasFlow : 0
            );
            
            String controlCommand = outboundService.buildControlCommand(mac, parameters);
            outboundService.setOutboundPacket(mac, controlCommand);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Команда управления отправлена",
                "mac", mac,
                "command", controlCommand,
                "parameters", parameters
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Ошибка отправки команды управления: " + e.getMessage(),
                "mac", mac
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Очистить очередь исходящих пакетов
     */
    @PostMapping("/clear-outbound-queue")
    public ResponseEntity<Map<String, Object>> clearOutboundQueue() {
        try {
            ArchiveOutboundPacketsRepository.clear();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Очередь исходящих пакетов очищена"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Ошибка очистки очереди: " + e.getMessage()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Получить размер очереди входящих пакетов
     */
    @GetMapping("/incoming-queue-size")
    public ResponseEntity<Map<String, Object>> getIncomingQueueSize() {
        try {
            int queueSize = ArchiveIncomingPacketsQueue.size();
            boolean isEmpty = ArchiveIncomingPacketsQueue.isEmpty();
            
            Map<String, Object> response = Map.of(
                "queueSize", queueSize,
                "isEmpty", isEmpty
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "error", "Ошибка получения размера очереди: " + e.getMessage()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Получить размер очереди исходящих пакетов
     */
    @GetMapping("/outbound-queue-size")
    public ResponseEntity<Map<String, Object>> getOutboundQueueSize() {
        try {
            int queueSize = ArchiveOutboundPacketsRepository.size();
            boolean isEmpty = ArchiveOutboundPacketsRepository.isEmpty();
            
            Map<String, Object> response = Map.of(
                "queueSize", queueSize,
                "isEmpty", isEmpty
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "error", "Ошибка получения размера очереди: " + e.getMessage()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
