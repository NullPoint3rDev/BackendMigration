package org.alloy.controllers;

import org.alloy.models.entities.AutomatedReport;
import org.alloy.services.AutomatedReportService;
import org.alloy.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/automated-reports-test")
@CrossOrigin(origins = "*")
public class AutomatedReportTestController {

    @Autowired
    private AutomatedReportService automatedReportService;

    @Autowired
    private EmailService emailService;

    /**
     * Тестирует отправку email отчета
     */
    @PostMapping("/test-email-report")
    public Map<String, Object> testEmailReport(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String toEmail = request.get("toEmail");
            String reportName = request.getOrDefault("reportName", "Тестовый автоматический отчет");
            String reportType = request.getOrDefault("reportType", "equipment");
            
            if (toEmail == null || toEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Email адрес обязателен");
                return response;
            }
            
            // Создаем тестовые данные для вложения (простой PDF)
            String testContent = "Тестовый автоматический отчет\n" +
                               "Название: " + reportName + "\n" +
                               "Тип: " + reportType + "\n" +
                               "Дата: " + java.time.LocalDateTime.now() + "\n" +
                               "Система: WeldTelecom\n" +
                               "Статус: Тестовый режим";
            byte[] testData = testContent.getBytes("UTF-8");
            
            emailService.sendAutomatedReportNotification(
                toEmail,
                "Тестовый получатель",
                reportName,
                reportType,
                "test_automated_report.pdf",
                testData
            );
            
            response.put("success", true);
            response.put("message", "Тестовый автоматический отчет отправлен на " + toEmail);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Ошибка отправки: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Тестирует отправку email уведомления об ошибке
     */
    @PostMapping("/test-email-error")
    public Map<String, Object> testEmailError(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String toEmail = request.get("toEmail");
            String reportName = request.getOrDefault("reportName", "Тестовый автоматический отчет");
            String errorMessage = request.getOrDefault("errorMessage", "Тестовая ошибка генерации отчета");
            
            if (toEmail == null || toEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Email адрес обязателен");
                return response;
            }
            
            emailService.sendReportErrorNotification(
                toEmail,
                "Тестовый получатель",
                reportName,
                errorMessage
            );
            
            response.put("success", true);
            response.put("message", "Тестовое уведомление об ошибке отправлено на " + toEmail);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Ошибка отправки: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Получает информацию о автоматических отчетах с email настройками
     */
    @GetMapping("/email-reports")
    public Map<String, Object> getEmailReports() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Получаем все автоматические отчеты
            var allReports = automatedReportService.getAllAutomatedReports();
            
            // Фильтруем отчеты с включенными email уведомлениями
            var emailReports = allReports.stream()
                .filter(report -> report.getEmailNotifications() != null && report.getEmailNotifications())
                .map(report -> {
                    Map<String, Object> reportInfo = new HashMap<>();
                    reportInfo.put("id", report.getId());
                    reportInfo.put("name", report.getName());
                    reportInfo.put("emailRecipients", report.getEmailRecipients());
                    reportInfo.put("isActive", report.getIsActive());
                    reportInfo.put("nextRun", report.getNextRun());
                    return reportInfo;
                })
                .toList();
            
            response.put("success", true);
            response.put("totalReports", allReports.size());
            response.put("emailReports", emailReports);
            response.put("emailReportsCount", emailReports.size());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Ошибка получения отчетов: " + e.getMessage());
        }
        
        return response;
    }
}
