package org.alloy.repositories;

import org.alloy.models.entities.InboxNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxNotificationRepository extends JpaRepository<InboxNotification, Integer> {
} 