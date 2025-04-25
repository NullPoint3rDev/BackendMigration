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

    public List<InboxMessage> getInboxMessagesByUserId(Integer userId) {
        return inboxMessageRepository.findByUserId(userId);
    }

    public List<InboxMessage> getUnreadInboxMessagesByUserId(Integer userId) {
        return inboxMessageRepository.findByUserIdAndIsReadFalse(userId);
    }

    public List<InboxMessage> getInboxMessagesByUserIdAndType(Integer userId, String type) {
        return inboxMessageRepository.findByUserIdAndType(userId, type);
    }

    public InboxMessage createInboxMessage(InboxMessage message) {
        // Validate required fields
        if (message.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (message.getSubject() == null || message.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
        if (message.getType() == null || message.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Type is required");
        }

        // Set creation date and read status
        message.setDateCreated(LocalDateTime.now());
        message.setIsRead(false);

        return inboxMessageRepository.save(message);
    }

    public InboxMessage updateInboxMessage(InboxMessage message) {
        // Validate ID
        if (message.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        // Check if message exists
        InboxMessage existingMessage = inboxMessageRepository.findById(message.getId())
                .orElseThrow(() -> new IllegalArgumentException("Inbox message not found"));

        // Preserve creation date
        message.setDateCreated(existingMessage.getDateCreated());

        return inboxMessageRepository.save(message);
    }

    public InboxMessage markInboxMessageAsRead(Integer id) {
        InboxMessage message = inboxMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inbox message not found"));

        message.setIsRead(true);
        message.setDateRead(LocalDateTime.now());

        return inboxMessageRepository.save(message);
    }

    public void markAllInboxMessagesAsRead(Integer userId) {
        List<InboxMessage> messages = inboxMessageRepository.findByUserIdAndIsReadFalse(userId);

        LocalDateTime now = LocalDateTime.now();
        for (InboxMessage message : messages) {
            message.setIsRead(true);
            message.setDateRead(now);
        }

        inboxMessageRepository.saveAll(messages);
    }

    public void deleteInboxMessage(Integer id) {
        if (!inboxMessageRepository.existsById(id)) {
            throw new IllegalArgumentException("Inbox message not found");
        }
        inboxMessageRepository.deleteById(id);
    }

    public void deleteAllInboxMessages(Integer userId) {
        List<InboxMessage> messages = inboxMessageRepository.findByUserId(userId);
        inboxMessageRepository.deleteAll(messages);
    }
}
