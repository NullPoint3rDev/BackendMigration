package org.alloy.services;

import org.alloy.models.entities.Notification;
import org.alloy.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public Optional<Notification> getNotificationById(Integer id) {
        return notificationRepository.findById(id);
    }

    public List<Notification> getNotificationsByUserId(Integer userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotificationsByUserId(Integer userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    public List<Notification> getNotificationsByUserIdAndType(Integer userId, String type) {
        return notificationRepository.findByUserIdAndType(userId, type);
    }

    public Notification createNotification(Notification notification) {
        // Validate required fields
        if (notification.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (notification.getTitle() == null || notification.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (notification.getMessage() == null || notification.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }
        if (notification.getType() == null || notification.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Type is required");
        }

        // Set creation date and read status
        notification.setDateCreated(LocalDateTime.now());
        notification.setIsRead(false);

        return notificationRepository.save(notification);
    }

    public Notification updateNotification(Notification notification) {
        // Validate ID
        if (notification.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        // Check if notification exists
        Notification existingNotification = notificationRepository.findById(notification.getId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        // Preserve creation date
        notification.setDateCreated(existingNotification.getDateCreated());

        return notificationRepository.save(notification);
    }

    public Notification markNotificationAsRead(Integer id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.setIsRead(true);
        notification.setDateRead(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    public void markAllNotificationsAsRead(Integer userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);

        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notification.setDateRead(now);
        }

        notificationRepository.saveAll(notifications);
    }

    public void deleteNotification(Integer id) {
        if (!notificationRepository.existsById(id)) {
            throw new IllegalArgumentException("Notification not found");
        }
        notificationRepository.deleteById(id);
    }

    public void deleteAllNotifications(Integer userId) {
        List<Notification> notifications = notificationRepository.findByUserId(userId);
        notificationRepository.deleteAll(notifications);
    }
}
