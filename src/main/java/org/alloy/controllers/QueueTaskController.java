package org.alloy.controllers;

import org.alloy.models.entities.QueueTask;
import org.alloy.services.QueueTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue-tasks")
public class QueueTaskController {
    @Autowired
    private QueueTaskService queueTaskService;

    @GetMapping
    public List<QueueTask> getAll() {
        return queueTaskService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueueTask> getById(@PathVariable Integer id) {
        return queueTaskService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public QueueTask create(@RequestBody QueueTask queueTask) {
        return queueTaskService.save(queueTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QueueTask> update(@PathVariable Integer id, @RequestBody QueueTask queueTask) {
        if (!queueTaskService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        queueTask.setId(id);
        return ResponseEntity.ok(queueTaskService.save(queueTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!queueTaskService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        queueTaskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 