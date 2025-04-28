package org.alloy.controllers;

import org.alloy.models.entities.QueuePushEvent;
import org.alloy.services.QueuePushEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue-push-events")
public class QueuePushEventController {
    @Autowired
    private QueuePushEventService queuePushEventService;

    @GetMapping
    public List<QueuePushEvent> getAll() {
        return queuePushEventService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueuePushEvent> getById(@PathVariable Integer id) {
        return queuePushEventService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public QueuePushEvent create(@RequestBody QueuePushEvent queuePushEvent) {
        return queuePushEventService.save(queuePushEvent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QueuePushEvent> update(@PathVariable Integer id, @RequestBody QueuePushEvent queuePushEvent) {
        if (!queuePushEventService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        queuePushEvent.setId(id);
        return ResponseEntity.ok(queuePushEventService.save(queuePushEvent));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!queuePushEventService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        queuePushEventService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 