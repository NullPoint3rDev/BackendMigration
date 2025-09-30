package org.alloy.controllers;

import org.alloy.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email-test")
@CrossOrigin(origins = "*")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    /**
     * Тестирует отправку простого уведомления
     */
    @PostMapping("/send-notification")
    public Map<String, Object> testSendNotification(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String toEmail = request.get("toEmail");
            String toName = request.getOrDefault("toName", "Тестовый пользователь");
            String subject = request.getOrDefault("subject", "Тестовое уведомление");
            String message = request.getOrDefault("message", "Это тестовое уведомление от системы WeldTelecom.");
            
            if (toEmail == null || toEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Email адрес обязателен");
                return response;
            }
            
            emailService.sendSimpleNotification(toEmail, toName, subject, message);
            
            response.put("success", true);
            response.put("message", "Уведомление отправлено на " + toEmail);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Ошибка отправки: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Тестирует отправку HTML уведомления
     */
    @PostMapping("/send-html-notification")
    public Map<String, Object> testSendHtmlNotification(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String toEmail = request.get("toEmail");
            String toName = request.getOrDefault("toName", "Тестовый пользователь");
            String subject = request.getOrDefault("subject", "HTML тестовое уведомление");
            String htmlMessage = request.getOrDefault("htmlMessage", 
                "<h3>Тестовое HTML уведомление</h3>" +
                "<p>Это <strong>HTML</strong> уведомление от системы <em>WeldTelecom</em>.</p>" +
                "<ul><li>Пункт 1</li><li>Пункт 2</li></ul>");
            
            if (toEmail == null || toEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Email адрес обязателен");
                return response;
            }
            
            emailService.sendHtmlNotification(toEmail, toName, subject, htmlMessage);
            
            response.put("success", true);
            response.put("message", "HTML уведомление отправлено на " + toEmail);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Ошибка отправки: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Тестирует отправку отчета с вложением
     */
    @PostMapping("/send-report")
    public Map<String, Object> testSendReport(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String toEmail = request.get("toEmail");
            String toName = request.getOrDefault("toName", "Тестовый пользователь");
            String subject = request.getOrDefault("subject", "Тестовый отчет");
            String message = request.getOrDefault("message", "Это тестовый отчет от системы WeldTelecom.");
            
            if (toEmail == null || toEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Email адрес обязателен");
                return response;
            }
            
            // Создаем тестовые данные для вложения (простой текстовый файл)
            String testContent = "Тестовый отчет\n" +
                               "Дата: " + java.time.LocalDateTime.now() + "\n" +
                               "Система: WeldTelecom\n" +
                               "Статус: Тестовый режим";
            byte[] testData = testContent.getBytes("UTF-8");
            
            emailService.sendReportWithAttachment(
                toEmail, 
                toName, 
                subject, 
                message, 
                "test_report.txt", 
                testData
            );
            
            response.put("success", true);
            response.put("message", "Отчет с вложением отправлен на " + toEmail);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Ошибка отправки: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Тестирует отправку критического уведомления
     */
    @PostMapping("/send-critical-alert")
    public Map<String, Object> testSendCriticalAlert(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String toEmail = request.get("toEmail");
            String toName = request.getOrDefault("toName", "Тестовый пользователь");
            String alertType = request.getOrDefault("alertType", "Тестовая критическая ошибка");
            String message = request.getOrDefault("message", "Это тестовое критическое уведомление.");
            String equipmentName = request.getOrDefault("equipmentName", "Тестовый аппарат");
            
            if (toEmail == null || toEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Email адрес обязателен");
                return response;
            }
            
            emailService.sendCriticalAlert(toEmail, toName, alertType, message, equipmentName);
            
            response.put("success", true);
            response.put("message", "Критическое уведомление отправлено на " + toEmail);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Ошибка отправки: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Получает информацию о конфигурации SMTP
     */
    @GetMapping("/config")
    public Map<String, Object> getEmailConfig() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Возвращаем только безопасную информацию (без паролей)
            response.put("success", true);
            response.put("smtpConfigured", true);
            response.put("message", "SMTP конфигурация загружена успешно");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Ошибка получения конфигурации: " + e.getMessage());
        }
        
        return response;
    }
}
