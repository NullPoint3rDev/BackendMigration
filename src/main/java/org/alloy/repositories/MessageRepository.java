package org.alloy.repositories;

import org.alloy.models.entities.Message;
import org.alloy.models.entities.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipient(UserAccount recipient);
    List<Message> findBySender(UserAccount sender);
} 