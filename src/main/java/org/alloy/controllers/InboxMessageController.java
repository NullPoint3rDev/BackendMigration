package org.alloy.controllers;

import org.alloy.models.entities.InboxMessage;
import org.alloy.services.InboxMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inbox-messages")
public class InboxMessageController {

    private final InboxMessageService inboxMessageService;

    @Autowired
    public InboxMessageController(InboxMessageService inboxMessageService) {
        this.inboxMessageService = inboxMessageService;
    }

    @GetMapping
    public ResponseEntity<List<InboxMessage>> getAllInboxMessages() {
        List<InboxMessage> messages = inboxMessageService.getAllInboxMessages();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InboxMessage> getInboxMessageById(@PathVariable Integer id) {
        return inboxMessageService.getInboxMessageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InboxMessage>> getInboxMessagesByUserId(@PathVariable Integer userId) {
        List<InboxMessage> messages = inboxMessageService.getInboxMessagesByUserId(userId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<InboxMessage>> getUnreadInboxMessagesByUserId(@PathVariable Integer userId) {
        List<InboxMessage> messages = inboxMessageService.getUnreadInboxMessagesByUserId(userId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<InboxMessage>> getInboxMessagesByUserIdAndType(
            @PathVariable Integer userId,
            @PathVariable String type) {
        List<InboxMessage> messages = inboxMessageService.getInboxMessagesByUserIdAndType(userId, type);
        return ResponseEntity.ok(messages);
    }

    @PostMapping
    public ResponseEntity<InboxMessage> createInboxMessage(@RequestBody InboxMessage message) {
        try {
            InboxMessage createdMessage = inboxMessageService.createInboxMessage(message);
            return new ResponseEntity<>(createdMessage, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<InboxMessage> updateInboxMessage(@PathVariable Integer id, @RequestBody InboxMessage message) {
        try {
            message.setId(id);
            InboxMessage updatedMessage = inboxMessageService.updateInboxMessage(message);
            return ResponseEntity.ok(updatedMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<InboxMessage> markInboxMessageAsRead(@PathVariable Integer id) {
        try {
            InboxMessage updatedMessage = inboxMessageService.markInboxMessageAsRead(id);
            return ResponseEntity.ok(updatedMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllInboxMessagesAsRead(@PathVariable Integer userId) {
        try {
            inboxMessageService.markAllInboxMessagesAsRead(userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInboxMessage(@PathVariable Integer id) {
        try {
            inboxMessageService.deleteInboxMessage(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllInboxMessages(@PathVariable Integer userId) {
        try {
            inboxMessageService.deleteAllInboxMessages(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
