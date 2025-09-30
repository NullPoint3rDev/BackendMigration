package org.alloy.services;

import org.alloy.models.entities.Notification;
import org.alloy.models.entities.UserAccount;
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
    private EmailService emailService;
    
    @Autowired
    private UserAccountService userAccountService;

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

    public List<Notification> getNotificationsByUserAccountId(Integer userAccountId) {
        return notificationRepository.findByUserAccountId(userAccountId);
    }

    public List<Notification> getUnreadNotificationsByUserAccountId(Integer userAccountId) {
        return notificationRepository.findByUserAccountIdAndIsReadFalse(userAccountId);
    }

    public List<Notification> getNotificationsByUserAccountIdAndType(Integer userAccountId, String type) {
        return notificationRepository.findByUserAccountIdAndType(userAccountId, type);
    }

    public Notification createNotification(Notification notification) {
        if (notification.getUserAccountId() == null) {
            throw new IllegalArgumentException("User Account ID is required");
        }
        if (notification.getTitle() == null || notification.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (notification.getMessage() == null || notification.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }

        notification.setDateCreated(LocalDateTime.now());
        notification.setIsRead(false);
        
        // Сохраняем уведомление в БД
        Notification savedNotification = notificationRepository.save(notification);
        
        // Отправляем email уведомление (асинхронно, чтобы не блокировать основной поток)
        try {
            sendEmailNotification(savedNotification);
        } catch (Exception e) {
            System.err.println("ERROR NotificationService: Failed to send email notification: " + e.getMessage());
            // Не прерываем выполнение из-за ошибки email
        }
        
        return savedNotification;
    }
    
    /**
     * Отправляет email уведомление пользователю
     */
    private void sendEmailNotification(Notification notification) {
        try {
            // Получаем данные пользователя
            Optional<UserAccount> userOpt = userAccountService.getUserAccountById(notification.getUserAccountId());
            if (userOpt.isPresent()) {
                UserAccount user = userOpt.get();
                
                // Проверяем, разрешены ли email уведомления для пользователя
                if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    // Отправляем email уведомление
                    emailService.sendSimpleNotification(
                        user.getEmail(),
                        user.getName(),
                        notification.getTitle(),
                        notification.getMessage()
                    );
                    
                    System.out.println("DEBUG NotificationService: Email notification sent to " + user.getEmail());
                } else {
                    System.out.println("DEBUG NotificationService: User " + user.getId() + " has no email address");
                }
            } else {
                System.out.println("WARN NotificationService: User " + notification.getUserAccountId() + " not found for email notification");
            }
        } catch (Exception e) {
            System.err.println("ERROR NotificationService: Failed to send email notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Notification updateNotification(Notification notification) {
        if (notification.getId() == null) {
            throw new IllegalArgumentException("Notification ID is required");
        }

        Notification existingNotification = notificationRepository.findById(notification.getId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

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

    public void markAllNotificationsAsRead(Integer userAccountId) {
        List<Notification> notifications = notificationRepository.findByUserAccountIdAndIsReadFalse(userAccountId);
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

    /**
     * Удаляет уведомления старше указанного количества дней
     */
    public void deleteOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<Notification> oldNotifications = notificationRepository.findByDateCreatedBefore(cutoffDate);
        
        if (!oldNotifications.isEmpty()) {
            notificationRepository.deleteAll(oldNotifications);
            System.out.println("DEBUG NotificationService: Deleted " + oldNotifications.size() + " old notifications");
        }
    }

    public void deleteNotificationsByUserAccountId(Integer userAccountId) {
        notificationRepository.deleteByUserAccountId(userAccountId);
    }

    public void cleanupOldNotifications(LocalDateTime date) {
        notificationRepository.deleteByDateCreatedBefore(date);
    }

    public long countUnreadNotificationsByUserAccountId(Integer userAccountId) {
        return notificationRepository.countByUserAccountIdAndIsReadFalse(userAccountId);
    }
}
