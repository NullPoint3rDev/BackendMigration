package org.alloy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.properties.mail.smtp.from:WeldTelecom}")
    private String fromName;

    /**
     * Отправляет простое текстовое уведомление
     */
    public void sendSimpleNotification(String toEmail, String toName, String subject, String message) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(buildNotificationBody(toName, message));
            
            mailSender.send(mailMessage);
            System.out.println("DEBUG EmailService: Successfully sent notification to " + toEmail);
        } catch (Exception e) {
            System.err.println("ERROR EmailService: Failed to send notification to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Отправляет HTML уведомление
     */
    public void sendHtmlNotification(String toEmail, String toName, String subject, String htmlMessage) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildHtmlNotificationBody(toName, htmlMessage), true);
            
            mailSender.send(mimeMessage);
            System.out.println("DEBUG EmailService: Successfully sent HTML notification to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("ERROR EmailService: Failed to send HTML notification to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Отправляет отчет с вложением
     */
    public void sendReportWithAttachment(String toEmail, String toName, String subject, 
                                       String message, String attachmentName, byte[] attachmentData) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildReportBody(toName, message), true);
            
            // Добавляем вложение
            if (attachmentData != null && attachmentData.length > 0) {
                ByteArrayResource attachment = new ByteArrayResource(attachmentData);
                helper.addAttachment(attachmentName, attachment);
            }
            
            mailSender.send(mimeMessage);
            System.out.println("DEBUG EmailService: Successfully sent report with attachment to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("ERROR EmailService: Failed to send report to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Отправляет уведомление об автоматическом отчете
     */
    public void sendAutomatedReportNotification(String toEmail, String toName, String reportName, 
                                              String reportType, String fileName, byte[] reportData) {
        String subject = "Автоматический отчет: " + reportName;
        String message = String.format(
            "Автоматический отчет '%s' (%s) успешно сгенерирован и прикреплен к данному письму.\n\n" +
            "Файл отчета: %s\n" +
            "Тип отчета: %s\n" +
            "Дата генерации: %s",
            reportName, reportType, fileName, reportType, java.time.LocalDateTime.now()
        );
        
        sendReportWithAttachment(toEmail, toName, subject, message, fileName, reportData);
    }

    /**
     * Отправляет уведомление об ошибке генерации отчета
     */
    public void sendReportErrorNotification(String toEmail, String toName, String reportName, String errorMessage) {
        String subject = "Ошибка генерации автоматического отчета: " + reportName;
        String message = String.format(
            "Не удалось сгенерировать автоматический отчет '%s'.\n\n" +
            "Ошибка: %s\n" +
            "Время ошибки: %s\n\n" +
            "Пожалуйста, проверьте настройки отчета или обратитесь к администратору.",
            reportName, errorMessage, java.time.LocalDateTime.now()
        );
        
        sendSimpleNotification(toEmail, toName, subject, message);
    }

    /**
     * Отправляет уведомление о критическом событии
     */
    public void sendCriticalAlert(String toEmail, String toName, String alertType, String message, String equipmentName) {
        String subject = "КРИТИЧЕСКОЕ УВЕДОМЛЕНИЕ: " + alertType;
        String alertMessage = String.format(
            "КРИТИЧЕСКОЕ СОБЫТИЕ В СИСТЕМЕ WELDTELECOM\n\n" +
            "Тип события: %s\n" +
            "Оборудование: %s\n" +
            "Сообщение: %s\n" +
            "Время события: %s\n\n" +
            "ТРЕБУЕТСЯ НЕМЕДЛЕННОЕ ВНИМАНИЕ!",
            alertType, equipmentName, message, java.time.LocalDateTime.now()
        );
        
        sendSimpleNotification(toEmail, toName, subject, alertMessage);
    }

    /**
     * Отправляет уведомление нескольким получателям
     */
    public void sendBulkNotification(List<String> toEmails, String subject, String message) {
        for (String email : toEmails) {
            if (isValidEmail(email)) {
                sendSimpleNotification(email, "", subject, message);
            }
        }
    }

    /**
     * Проверяет валидность email адреса
     */
    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        
        // Простая проверка формата email
        return email.contains("@") && email.contains(".") && !email.contains("@test.com");
    }

    /**
     * Строит тело уведомления
     */
    private String buildNotificationBody(String toName, String message) {
        StringBuilder body = new StringBuilder();
        body.append("Здравствуйте");
        if (StringUtils.hasText(toName)) {
            body.append(", ").append(toName);
        }
        body.append(".\n\n");
        body.append("У Вас новое уведомление от системы WeldTelecom:\n\n");
        body.append(message);
        body.append("\n\n");
        body.append("С уважением,\n");
        body.append("Система WeldTelecom");
        return body.toString();
    }

    /**
     * Строит HTML тело уведомления
     */
    private String buildHtmlNotificationBody(String toName, String htmlMessage) {
        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
        body.append("<p>Здравствуйте");
        if (StringUtils.hasText(toName)) {
            body.append(", ").append(toName);
        }
        body.append(".</p>");
        body.append("<p>У Вас новое уведомление от системы <strong>WeldTelecom</strong>:</p>");
        body.append("<div style='background-color: #f5f5f5; padding: 15px; border-left: 4px solid #007bff; margin: 15px 0;'>");
        body.append(htmlMessage);
        body.append("</div>");
        body.append("<p>С уважением,<br>Система WeldTelecom</p>");
        body.append("</body></html>");
        return body.toString();
    }

    /**
     * Строит тело отчета
     */
    private String buildReportBody(String toName, String message) {
        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
        body.append("<p>Здравствуйте");
        if (StringUtils.hasText(toName)) {
            body.append(", ").append(toName);
        }
        body.append(".</p>");
        body.append("<p>Отчет от системы <strong>WeldTelecom</strong>:</p>");
        body.append("<div style='background-color: #f8f9fa; padding: 15px; border: 1px solid #dee2e6; margin: 15px 0;'>");
        body.append(message.replace("\n", "<br>"));
        body.append("</div>");
        body.append("<p>Отчет прикреплен к данному письму.</p>");
        body.append("<p>С уважением,<br>Система WeldTelecom</p>");
        body.append("</body></html>");
        return body.toString();
    }
}
