package org.alloy.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class DeviceController {
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public DeviceController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendDeviceData(String data) {
        messagingTemplate.convertAndSend("/topic/device", data);
    }
    @MessageMapping("/device")
    @SendTo("/topic/device")
    public String handleDeviceMessage(String message) {
        System.out.println("Получено сообщение от устройства: " + message);
        return message;
    }
}