package org.alloy.controllers;

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

    @Autowired
    public DeviceController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendDeviceData(String data) {
        // Добавляем временную метку к данным
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String enrichedData = timestamp + "|" + data;
        
        System.out.println("[DEVICE-CONTROLLER] Отправка данных: " + enrichedData);
        messagingTemplate.convertAndSend("/topic/device", enrichedData);
    }
    @MessageMapping("/device")
    @SendTo("/topic/device")
    public String handleDeviceMessage(String message) {
        System.out.println("Получено сообщение от устройства: " + message);
        return message;
    }
}