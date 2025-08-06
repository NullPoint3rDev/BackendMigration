package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.WeldingMachineStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;

@Controller
public class DeviceController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public DeviceController(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendDeviceData(String data) {
        try {
            // Добавляем временную метку к данным
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String enrichedData = timestamp + "|" + data;
            
            System.out.println("[DEVICE-CONTROLLER] Отправка данных: " + enrichedData);
            messagingTemplate.convertAndSend("/topic/device", enrichedData);
        } catch (Exception e) {
            System.err.println("[DEVICE-CONTROLLER] Ошибка отправки данных: " + e.getMessage());
        }
    }

    public void sendDeviceState(StateSummary state) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            DeviceStateMessage message = new DeviceStateMessage();
            message.setTimestamp(timestamp);
            message.setState(state);
            String jsonData = objectMapper.writeValueAsString(message);
            
            System.out.println("[DEVICE-CONTROLLER] 📤 Отправка состояния: " + jsonData);
            messagingTemplate.convertAndSend("/topic/device-state", jsonData);
            System.out.println("[DEVICE-CONTROLLER] ✅ Состояние отправлено на /topic/device-state");
            
        } catch (Exception e) {
            System.err.println("[DEVICE-CONTROLLER] ❌ Ошибка отправки состояния: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/device-command")
    @SendTo("/topic/device-response")
    public String handleDeviceCommand(String command) {
        System.out.println("[DEVICE-CONTROLLER] Получена команда: " + command);
        return "Команда получена: " + command;
    }
    
    // Тестовый endpoint для проверки WebSocket
    @GetMapping("/test-websocket")
    public ResponseEntity<String> testWebSocket() {
        try {
            StateSummary testState = new StateSummary();
            testState.setStatus(WeldingMachineStatus.Idle);
            testState.setDateCreated(LocalDateTime.now());
            testState.setLastDatetimeUpdate(LocalDateTime.now());
            
            sendDeviceState(testState);
            return ResponseEntity.ok("Тестовое сообщение отправлено через WebSocket");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }

    // Внутренний класс для сообщений
    public static class DeviceStateMessage {
        private String timestamp;
        private StateSummary state;

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public StateSummary getState() {
            return state;
        }

        public void setState(StateSummary state) {
            this.state = state;
        }
    }
}