package org.alloy.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/support")
public class SupportController {
    public static class SupportRequest {
        public String message;
        public String fio;
        public String phone;
        public String username;
    }

    @PostMapping
    public ResponseEntity<Void> sendSupport(@RequestBody SupportRequest req) {
        System.out.println("[SUPPORT] Обращение: " + req.message + " | ФИО: " + req.fio + " | Телефон: " + req.phone + " | Username: " + req.username);
        // TODO: отправка в Telegram-бота
        return ResponseEntity.ok().build();
    }
} 