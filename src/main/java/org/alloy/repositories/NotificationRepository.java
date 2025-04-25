package org.alloy.repositories;

import org.alloy.models.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByUserId(Integer userId);

    List<Notification> findByUserIdAndIsReadFalse(Integer userId);

    List<Notification> findByUserIdAndType(Integer userId, String type);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false ORDER BY n.dateCreated DESC")
    List<Notification> findUnreadNotificationsByUserIdOrderByDateCreatedDesc(@Param("userId") Integer userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.type = :type ORDER BY n.dateCreated DESC")
    List<Notification> findNotificationsByUserIdAndTypeOrderByDateCreatedDesc(
            @Param("userId") Integer userId,
            @Param("type") String type);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    long countUnreadNotificationsByUserId(@Param("userId") Integer userId);

    @Query("SELECT n FROM Notification n WHERE n.dateCreated < :date AND n.isRead = true")
    List<Notification> findOldReadNotifications(@Param("date") LocalDateTime date);

    void deleteByUserId(Integer userId);

    void deleteByDateCreatedBefore(LocalDateTime date);
}
