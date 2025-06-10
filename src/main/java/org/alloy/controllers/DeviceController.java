package org.alloy.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class DeviceController {

    @MessageMapping("/device")
    @SendTo("/topic/device")
    public String handleDeviceMessage(String message) {
        return message;
    }
} 