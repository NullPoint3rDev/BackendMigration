package org.alloy.controllers;

import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.services.WeldingDeviceManagerService;
import org.alloy.services.DeviceMessageHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/device-test")
public class DeviceTestController {

    @Autowired
    private WeldingDeviceManagerService deviceManager;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DeviceMessageHistoryService messageHistoryService;

    // Список доступных плат
    private final List<DeviceInfo> availableDevices = Arrays.asList(
        new DeviceInfo("8CAAB50C4254", "Блок мониторинга ОГК", "95.172.58.219", 3000),
        new DeviceInfo("E09806083396", "Core", "192.168.10.137", 3000),
        new DeviceInfo("DC4F22763D5C", "Core2", "192.168.10.137", 3000)
    );

    /**
     * Получить список доступных плат
     */
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceInfo>> getAvailableDevices() {
        return ResponseEntity.ok(availableDevices);
    }

    /**
     * Получить состояние конкретной платы
     */
    @GetMapping("/devices/{mac}/state")
    public ResponseEntity<Map<String, Object>> getDeviceState(@PathVariable String mac) {
        StateSummary state = deviceManager.getDeviceState(mac);
        boolean isConnected = deviceManager.isDeviceConnected(mac);
        
        Map<String, Object> response = new HashMap<>();
        response.put("mac", mac);
        response.put("connected", isConnected);
        response.put("state", state);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Получить историю сообщений для конкретной платы
     */
    @GetMapping("/devices/{mac}/history")
    public ResponseEntity<List<DeviceMessageHistoryService.DeviceMessage>> getDeviceHistory(@PathVariable String mac) {
        List<DeviceMessageHistoryService.DeviceMessage> deviceHistory = messageHistoryService.getDeviceHistory(mac);
        return ResponseEntity.ok(deviceHistory);
    }

    /**
     * Получить всю историю сообщений
     */
    @GetMapping("/history")
    public ResponseEntity<List<DeviceMessageHistoryService.DeviceMessage>> getAllHistory() {
        List<DeviceMessageHistoryService.DeviceMessage> allHistory = messageHistoryService.getAllHistory();
        return ResponseEntity.ok(allHistory);
    }

    /**
     * Отправить команду плате
     */
    @PostMapping("/devices/{mac}/send")
    public ResponseEntity<Map<String, Object>> sendCommandToDevice(
            @PathVariable String mac,
            @RequestBody Map<String, String> request) {
        
        String command = request.get("command");
        if (command == null || command.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Команда не может быть пустой",
                "mac", mac
            ));
        }

        try {
            // Добавляем сообщение в историю
            messageHistoryService.addMessage(mac, command, "sent");

            // Отправляем через WebSocket для отображения на фронтенде
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("mac", mac);
            wsMessage.put("type", "command_sent");
            wsMessage.put("command", command);
            wsMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/device-test", wsMessage);
            }

            // Здесь можно добавить реальную отправку команды плате
            // Пока просто логируем
            System.out.println("[DEVICE-TEST] 📤 Отправка команды плате " + mac + ": " + command);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Команда отправлена плате " + mac,
                "command", command,
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));

        } catch (Exception e) {
            System.err.println("[DEVICE-TEST] ❌ Ошибка отправки команды: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Ошибка отправки команды: " + e.getMessage(),
                "mac", mac
            ));
        }
    }

    /**
     * Получить статистику по платам
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = deviceManager.getDeviceStatistics();
        
        // Добавляем статистику по сообщениям
        Map<String, Long> messageStats = messageHistoryService.getMessageStatistics();
        
        stats.put("messageHistory", messageStats);
        stats.put("totalMessages", messageHistoryService.getTotalMessageCount());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Очистить историю сообщений
     */
    @PostMapping("/history/clear")
    public ResponseEntity<Map<String, String>> clearHistory() {
        messageHistoryService.clearHistory();
        return ResponseEntity.ok(Map.of(
            "message", "История сообщений очищена",
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));
    }

    // Внутренние классы
    public static class DeviceInfo {
        private String mac;
        private String name;
        private String ip;
        private int port;

        public DeviceInfo(String mac, String name, String ip, int port) {
            this.mac = mac;
            this.name = name;
            this.ip = ip;
            this.port = port;
        }

        // Getters and setters
        public String getMac() { return mac; }
        public void setMac(String mac) { this.mac = mac; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

}
