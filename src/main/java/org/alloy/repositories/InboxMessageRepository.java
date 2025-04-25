package org.alloy.repositories;

import org.alloy.models.entities.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboxMessageRepository extends JpaRepository<InboxMessage, Integer> {

    List<InboxMessage> findByUserAccountId(Integer userAccountId);

    List<InboxMessage> findByUserAccountIdAndIsReadFalse(Integer userAccountId);

    List<InboxMessage> findByUserAccountIdAndType(Integer userAccountId, String type);

    @Query("SELECT m FROM InboxMessage m WHERE m.userAccountId = :userAccountId AND m.isRead = false ORDER BY m.dateCreated DESC")
    List<InboxMessage> findUnreadMessagesByUserAccountIdOrderByDateCreatedDesc(@Param("userAccountId") Integer userAccountId);

    @Query("SELECT m FROM InboxMessage m WHERE m.userAccountId = :userAccountId AND m.type = :type ORDER BY m.dateCreated DESC")
    List<InboxMessage> findMessagesByUserAccountIdAndTypeOrderByDateCreatedDesc(
            @Param("userAccountId") Integer userAccountId,
            @Param("type") String type);

    @Query("SELECT COUNT(m) FROM InboxMessage m WHERE m.userAccountId = :userAccountId AND m.isRead = false")
    long countUnreadMessagesByUserAccountId(@Param("userAccountId") Integer userAccountId);

    void deleteByUserAccountId(Integer userAccountId);
}
