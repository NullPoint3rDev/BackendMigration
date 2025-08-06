package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.models.weldingmachine.StateSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
            // Добавляем временную метку
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // Создаем объект с данными состояния
            DeviceStateMessage message = new DeviceStateMessage();
            message.setTimestamp(timestamp);
            message.setState(state);
            
            String jsonData = objectMapper.writeValueAsString(message);
            
            System.out.println("[DEVICE-CONTROLLER] Отправка состояния: " + jsonData);
            messagingTemplate.convertAndSend("/topic/device-state", jsonData);
        } catch (Exception e) {
            System.err.println("[DEVICE-CONTROLLER] Ошибка отправки состояния: " + e.getMessage());
        }
    }

    @MessageMapping("/device-command")
    @SendTo("/topic/device-response")
    public String handleDeviceCommand(String command) {
        System.out.println("[DEVICE-CONTROLLER] Получена команда: " + command);
        return "Команда обработана: " + command;
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