package org.alloy.controllers;

import org.alloy.models.entities.Notification;
import org.alloy.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Integer id) {
        return notificationService.getNotificationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userAccountId}")
    public ResponseEntity<List<Notification>> getNotificationsByUserAccountId(@PathVariable Integer userAccountId) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserAccountId(userAccountId));
    }

    @GetMapping("/user/{userAccountId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotificationsByUserAccountId(@PathVariable Integer userAccountId) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsByUserAccountId(userAccountId));
    }

    @GetMapping("/user/{userAccountId}/type/{type}")
    public ResponseEntity<List<Notification>> getNotificationsByUserAccountIdAndType(
            @PathVariable Integer userAccountId,
            @PathVariable String type) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserAccountIdAndType(userAccountId, type));
    }

    @GetMapping("/user/{userAccountId}/unread/count")
    public ResponseEntity<Long> countUnreadNotificationsByUserAccountId(@PathVariable Integer userAccountId) {
        return ResponseEntity.ok(notificationService.countUnreadNotificationsByUserAccountId(userAccountId));
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody Notification notification) {
        try {
            Notification createdNotification = notificationService.createNotification(notification);
            return new ResponseEntity<>(createdNotification, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Notification> updateNotification(@PathVariable Integer id, @RequestBody Notification notification) {
        try {
            notification.setId(id);
            Notification updatedNotification = notificationService.updateNotification(notification);
            return ResponseEntity.ok(updatedNotification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markNotificationAsRead(@PathVariable Integer id) {
        try {
            Notification updatedNotification = notificationService.markNotificationAsRead(id);
            return ResponseEntity.ok(updatedNotification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/user/{userAccountId}/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(@PathVariable Integer userAccountId) {
        try {
            notificationService.markAllNotificationsAsRead(userAccountId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Integer id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user/{userAccountId}")
    public ResponseEntity<Void> deleteNotificationsByUserAccountId(@PathVariable Integer userAccountId) {
        notificationService.deleteNotificationsByUserAccountId(userAccountId);
        return ResponseEntity.ok().build();
    }
}
