package org.alloy.controllers;

import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.services.WeldingDeviceManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/api/device-test")
public class DeviceTestController {

    @Autowired
    private WeldingDeviceManagerService deviceManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // История сообщений (последние 100)
    private final Queue<DeviceMessage> messageHistory = new ConcurrentLinkedQueue<>();
    private final int MAX_HISTORY_SIZE = 100;

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
    public ResponseEntity<List<DeviceMessage>> getDeviceHistory(@PathVariable String mac) {
        List<DeviceMessage> deviceHistory = messageHistory.stream()
            .filter(msg -> msg.getMac().equalsIgnoreCase(mac))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(100)
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(deviceHistory);
    }

    /**
     * Получить всю историю сообщений
     */
    @GetMapping("/history")
    public ResponseEntity<List<DeviceMessage>> getAllHistory() {
        List<DeviceMessage> allHistory = messageHistory.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(100)
            .collect(java.util.stream.Collectors.toList());
        
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
            DeviceMessage sentMessage = new DeviceMessage();
            sentMessage.setMac(mac);
            sentMessage.setType("sent");
            sentMessage.setData(command);
            sentMessage.setTimestamp(LocalDateTime.now());
            addToHistory(sentMessage);

            // Отправляем через WebSocket для отображения на фронтенде
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("mac", mac);
            wsMessage.put("type", "command_sent");
            wsMessage.put("command", command);
            wsMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            messagingTemplate.convertAndSend("/topic/device-test", wsMessage);

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
        Map<String, Long> messageStats = messageHistory.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                DeviceMessage::getMac,
                java.util.stream.Collectors.counting()
            ));
        
        stats.put("messageHistory", messageStats);
        stats.put("totalMessages", messageHistory.size());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Очистить историю сообщений
     */
    @PostMapping("/history/clear")
    public ResponseEntity<Map<String, String>> clearHistory() {
        messageHistory.clear();
        return ResponseEntity.ok(Map.of(
            "message", "История сообщений очищена",
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));
    }

    /**
     * Добавить сообщение в историю (вызывается из других сервисов)
     */
    public void addDeviceMessage(String mac, String data, String type) {
        DeviceMessage message = new DeviceMessage();
        message.setMac(mac);
        message.setData(data);
        message.setType(type);
        message.setTimestamp(LocalDateTime.now());
        addToHistory(message);
    }

    private void addToHistory(DeviceMessage message) {
        messageHistory.offer(message);
        
        // Ограничиваем размер истории
        while (messageHistory.size() > MAX_HISTORY_SIZE) {
            messageHistory.poll();
        }
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

    public static class DeviceMessage {
        private String mac;
        private String data;
        private String type; // "received" или "sent"
        private LocalDateTime timestamp;

        // Getters and setters
        public String getMac() { return mac; }
        public void setMac(String mac) { this.mac = mac; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
