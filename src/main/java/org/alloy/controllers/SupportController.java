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
        // Отправка в Telegram-бота
        try {
            String token = "<YOUR_TELEGRAM_BOT_TOKEN>"; // <-- Замените на ваш токен
            String chatId = "<YOUR_CHAT_ID>"; // <-- Замените на ваш chat_id
            String text = "\uD83D\uDEA8 Новое обращение в поддержку!%0A" +
                    "Сообщение: " + req.message + "%0A" +
                    "ФИО: " + req.fio + "%0A" +
                    "Телефон: " + req.phone + "%0A" +
                    (req.username != null ? ("Username: @" + req.username + "%0A") : "");
            String urlString = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&text=" + text + "&parse_mode=HTML";
            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            System.out.println("Telegram response code: " + responseCode);
        } catch (Exception e) {
            System.err.println("Ошибка отправки в Telegram: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
} 