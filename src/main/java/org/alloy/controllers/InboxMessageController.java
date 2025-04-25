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

    @GetMapping("/user/{userAccountId}")
    public ResponseEntity<List<InboxMessage>> getMessagesByUserAccountId(@PathVariable Integer userAccountId) {
        return ResponseEntity.ok(inboxMessageService.getMessagesByUserAccountId(userAccountId));
    }

    @GetMapping("/user/{userAccountId}/unread")
    public ResponseEntity<List<InboxMessage>> getUnreadMessagesByUserAccountId(@PathVariable Integer userAccountId) {
        return ResponseEntity.ok(inboxMessageService.getUnreadMessagesByUserAccountId(userAccountId));
    }

    @GetMapping("/user/{userAccountId}/type/{type}")
    public ResponseEntity<List<InboxMessage>> getMessagesByUserAccountIdAndType(
            @PathVariable Integer userAccountId,
            @PathVariable String type) {
        return ResponseEntity.ok(inboxMessageService.getMessagesByUserAccountIdAndType(userAccountId, type));
    }

    @GetMapping("/user/{userAccountId}/unread/count")
    public ResponseEntity<Long> countUnreadMessagesByUserAccountId(@PathVariable Integer userAccountId) {
        return ResponseEntity.ok(inboxMessageService.countUnreadMessagesByUserAccountId(userAccountId));
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

    @PutMapping("/user/{userAccountId}/read-all")
    public ResponseEntity<Void> markAllInboxMessagesAsRead(@PathVariable Integer userAccountId) {
        inboxMessageService.markAllInboxMessagesAsRead(userAccountId);
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

    @DeleteMapping("/user/{userAccountId}")
    public ResponseEntity<Void> deleteMessagesByUserAccountId(@PathVariable Integer userAccountId) {
        inboxMessageService.deleteMessagesByUserAccountId(userAccountId);
        return ResponseEntity.ok().build();
    }
}
