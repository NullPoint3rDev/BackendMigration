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

    List<Notification> findByUserAccountId(Integer userAccountId);

    List<Notification> findByUserAccountIdAndIsReadFalse(Integer userAccountId);

    List<Notification> findByUserAccountIdAndType(Integer userAccountId, String type);

    @Query("SELECT n FROM Notification n WHERE n.userAccountId = :userAccountId AND n.isRead = false ORDER BY n.dateCreated DESC")
    List<Notification> findUnreadNotificationsByUserAccountIdOrderByDateCreatedDesc(@Param("userAccountId") Integer userAccountId);

    @Query("SELECT n FROM Notification n WHERE n.userAccountId = :userAccountId AND n.type = :type ORDER BY n.dateCreated DESC")
    List<Notification> findNotificationsByUserAccountIdAndTypeOrderByDateCreatedDesc(
            @Param("userAccountId") Integer userAccountId,
            @Param("type") String type);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userAccountId = :userAccountId AND n.isRead = false")
    long countUnreadNotificationsByUserAccountId(@Param("userAccountId") Integer userAccountId);

    @Query("SELECT n FROM Notification n WHERE n.dateCreated < :date AND n.isRead = true")
    List<Notification> findOldReadNotifications(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userAccountId = :userAccountId AND n.isRead = false")
    long countByUserAccountIdAndIsReadFalse(@Param("userAccountId") Integer userAccountId);

    void deleteByUserAccountId(Integer userAccountId);

    void deleteByDateCreatedBefore(LocalDateTime date);
}
