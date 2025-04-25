package org.alloy.repositories;

import org.alloy.models.entities.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboxMessageRepository extends JpaRepository<InboxMessage, Integer> {

    List<InboxMessage> findByUserId(Integer userId);

    List<InboxMessage> findByUserIdAndIsReadFalse(Integer userId);

    List<InboxMessage> findByUserIdAndType(Integer userId, String type);

    @Query("SELECT m FROM InboxMessage m WHERE m.userId = :userId AND m.isRead = false ORDER BY m.dateCreated DESC")
    List<InboxMessage> findUnreadMessagesByUserIdOrderByDateCreatedDesc(@Param("userId") Integer userId);

    @Query("SELECT m FROM InboxMessage m WHERE m.userId = :userId AND m.type = :type ORDER BY m.dateCreated DESC")
    List<InboxMessage> findMessagesByUserIdAndTypeOrderByDateCreatedDesc(
            @Param("userId") Integer userId,
            @Param("type") String type);

    @Query("SELECT COUNT(m) FROM InboxMessage m WHERE m.userId = :userId AND m.isRead = false")
    long countUnreadMessagesByUserId(@Param("userId") Integer userId);

    void deleteByUserId(Integer userId);
}
