package org.alloy.services;

import org.alloy.models.entities.InboxMessage;
import org.alloy.repositories.InboxMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InboxMessageService {

    private final InboxMessageRepository inboxMessageRepository;

    @Autowired
    public InboxMessageService(InboxMessageRepository inboxMessageRepository) {
        this.inboxMessageRepository = inboxMessageRepository;
    }

    public List<InboxMessage> getAllInboxMessages() {
        return inboxMessageRepository.findAll();
    }

    public Optional<InboxMessage> getInboxMessageById(Integer id) {
        return inboxMessageRepository.findById(id);
    }

    public List<InboxMessage> getMessagesByUserAccountId(Integer userAccountId) {
        return inboxMessageRepository.findByUserAccountId(userAccountId);
    }

    public List<InboxMessage> getUnreadMessagesByUserAccountId(Integer userAccountId) {
        return inboxMessageRepository.findUnreadMessagesByUserAccountIdOrderByDateCreatedDesc(userAccountId);
    }

    public List<InboxMessage> getMessagesByUserAccountIdAndType(Integer userAccountId, String type) {
        return inboxMessageRepository.findMessagesByUserAccountIdAndTypeOrderByDateCreatedDesc(userAccountId, type);
    }

    public long countUnreadMessagesByUserAccountId(Integer userAccountId) {
        return inboxMessageRepository.countUnreadMessagesByUserAccountId(userAccountId);
    }

    public InboxMessage createInboxMessage(InboxMessage message) {
        if (message.getUserAccountId() == null) {
            throw new IllegalArgumentException("User Account ID is required");
        }
        if (message.getSubject() == null || message.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (message.getMessage() == null || message.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content is required");
        }

        message.setDateCreated(LocalDateTime.now());
        message.setIsRead(false);
        return inboxMessageRepository.save(message);
    }

    public InboxMessage updateInboxMessage(InboxMessage message) {
        if (message.getId() == null) {
            throw new IllegalArgumentException("Message ID is required");
        }

        InboxMessage existingMessage = inboxMessageRepository.findById(message.getId())
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        message.setDateCreated(existingMessage.getDateCreated());
        return inboxMessageRepository.save(message);
    }

    public InboxMessage markInboxMessageAsRead(Integer id) {
        InboxMessage message = inboxMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        message.setIsRead(true);
        message.setDateRead(LocalDateTime.now());
        return inboxMessageRepository.save(message);
    }

    public void markAllInboxMessagesAsRead(Integer userAccountId) {
        List<InboxMessage> messages = inboxMessageRepository.findByUserAccountIdAndIsReadFalse(userAccountId);
        LocalDateTime now = LocalDateTime.now();

        for (InboxMessage message : messages) {
            message.setIsRead(true);
            message.setDateRead(now);
        }

        inboxMessageRepository.saveAll(messages);
    }

    public void deleteInboxMessage(Integer id) {
        if (!inboxMessageRepository.existsById(id)) {
            throw new IllegalArgumentException("Message not found");
        }
        inboxMessageRepository.deleteById(id);
    }

    public void deleteMessagesByUserAccountId(Integer userAccountId) {
        inboxMessageRepository.deleteByUserAccountId(userAccountId);
    }
}
