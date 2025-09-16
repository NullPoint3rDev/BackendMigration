package org.alloy.controllers;

import org.alloy.models.dto.NotificationTemplateDTO;
import org.alloy.services.NotificationTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notification-templates")
@CrossOrigin(origins = "*")
public class NotificationTemplateController {

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    /**
     * Создать новый шаблон уведомления
     */
    @PostMapping
    public ResponseEntity<NotificationTemplateDTO> createNotificationTemplate(@RequestBody NotificationTemplateDTO templateDTO) {
        try {
            NotificationTemplateDTO createdTemplate = notificationTemplateService.createNotificationTemplate(templateDTO);
            return ResponseEntity.ok(createdTemplate);
        } catch (Exception e) {
            System.err.println("Ошибка при создании шаблона уведомления: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получить все шаблоны уведомлений
     */
    @GetMapping
    public ResponseEntity<List<NotificationTemplateDTO>> getAllNotificationTemplates() {
        try {
            List<NotificationTemplateDTO> templates = notificationTemplateService.getAllNotificationTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            System.err.println("Ошибка при получении шаблонов уведомлений: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получить активные шаблоны уведомлений
     */
    @GetMapping("/active")
    public ResponseEntity<List<NotificationTemplateDTO>> getActiveNotificationTemplates() {
        try {
            List<NotificationTemplateDTO> templates = notificationTemplateService.getActiveNotificationTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            System.err.println("Ошибка при получении активных шаблонов уведомлений: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получить шаблоны уведомлений по типу триггера
     */
    @GetMapping("/trigger-type/{triggerType}")
    public ResponseEntity<List<NotificationTemplateDTO>> getNotificationTemplatesByTriggerType(@PathVariable String triggerType) {
        try {
            List<NotificationTemplateDTO> templates = notificationTemplateService.getNotificationTemplatesByTriggerType(triggerType);
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            System.err.println("Ошибка при получении шаблонов уведомлений по типу триггера: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получить шаблон уведомления по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplateDTO> getNotificationTemplateById(@PathVariable Long id) {
        try {
            Optional<NotificationTemplateDTO> template = notificationTemplateService.getNotificationTemplateById(id);
            return template.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            System.err.println("Ошибка при получении шаблона уведомления по ID: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Обновить шаблон уведомления
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplateDTO> updateNotificationTemplate(@PathVariable Long id, @RequestBody NotificationTemplateDTO templateDTO) {
        try {
            NotificationTemplateDTO updatedTemplate = notificationTemplateService.updateNotificationTemplate(id, templateDTO);
            if (updatedTemplate != null) {
                return ResponseEntity.ok(updatedTemplate);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении шаблона уведомления: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Удалить шаблон уведомления
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotificationTemplate(@PathVariable Long id) {
        try {
            boolean deleted = notificationTemplateService.deleteNotificationTemplate(id);
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при удалении шаблона уведомления: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Переключить статус активности шаблона
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<NotificationTemplateDTO> toggleNotificationTemplateStatus(@PathVariable Long id) {
        try {
            NotificationTemplateDTO updatedTemplate = notificationTemplateService.toggleNotificationTemplateStatus(id);
            if (updatedTemplate != null) {
                return ResponseEntity.ok(updatedTemplate);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при переключении статуса шаблона уведомления: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
