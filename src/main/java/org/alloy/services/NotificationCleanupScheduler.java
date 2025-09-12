package org.alloy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Планировщик для очистки старых уведомлений
 */
@Service
public class NotificationCleanupScheduler {

    @Autowired
    private NotificationService notificationService;

    /**
     * Очищает уведомления старше 2 дней каждый день в 02:00
     */
    @Scheduled(cron = "0 0 2 * * ?") // Каждый день в 02:00
    public void cleanupOldNotifications() {
        System.out.println("DEBUG NotificationCleanupScheduler: Starting cleanup of old notifications at " + java.time.LocalDateTime.now());
        
        try {
            // Удаляем уведомления старше 2 дней
            notificationService.deleteOldNotifications(2);
            System.out.println("DEBUG NotificationCleanupScheduler: Cleanup completed successfully");
        } catch (Exception e) {
            System.err.println("ERROR NotificationCleanupScheduler: Failed to cleanup old notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
