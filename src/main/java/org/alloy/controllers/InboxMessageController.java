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
        return ResponseEntity.ok(inboxMessageService.getAllInboxMessages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InboxMessage> getInboxMessageById(@PathVariable Integer id) {
        return inboxMessageService.getInboxMessageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InboxMessage>> getInboxMessagesByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(inboxMessageService.getInboxMessagesByUserId(userId));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<InboxMessage>> getUnreadInboxMessagesByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(inboxMessageService.getUnreadInboxMessagesByUserId(userId));
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<InboxMessage>> getInboxMessagesByUserIdAndType(
            @PathVariable Integer userId,
            @PathVariable String type) {
        return ResponseEntity.ok(inboxMessageService.getInboxMessagesByUserIdAndType(userId, type));
    }

    @PostMapping
    public ResponseEntity<InboxMessage> createInboxMessage(@RequestBody InboxMessage message) {
        try {
            return ResponseEntity.ok(inboxMessageService.createInboxMessage(message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<InboxMessage> updateInboxMessage(@PathVariable Integer id, @RequestBody InboxMessage message) {
        try {
            message.setId(id);
            return ResponseEntity.ok(inboxMessageService.updateInboxMessage(message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<InboxMessage> markInboxMessageAsRead(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(inboxMessageService.markInboxMessageAsRead(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllInboxMessagesAsRead(@PathVariable Integer userId) {
        inboxMessageService.markAllInboxMessagesAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInboxMessage(@PathVariable Integer id) {
        try {
            inboxMessageService.deleteInboxMessage(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllInboxMessages(@PathVariable Integer userId) {
        inboxMessageService.deleteAllInboxMessages(userId);
        return ResponseEntity.ok().build();
    }
}
