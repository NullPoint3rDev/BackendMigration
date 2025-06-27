package org.alloy.services;

import org.alloy.models.entities.QueueTask;
import org.alloy.repositories.QueueTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QueueTaskService {
    @Autowired
    private QueueTaskRepository queueTaskRepository;

    public List<QueueTask> findAll() {
        return queueTaskRepository.findAll();
    }

    public Optional<QueueTask> findById(Integer id) {
        return queueTaskRepository.findById(id);
    }

    public QueueTask save(QueueTask queueTask) {
        return queueTaskRepository.save(queueTask);
    }

    public void deleteById(Integer id) {
        queueTaskRepository.deleteById(id);
    }

    public QueueTask createQueueTask(QueueTask queueTask) {
        return queueTaskRepository.save(queueTask);
    }

    public QueueTask updateQueueTask(QueueTask queueTask) {
        return queueTaskRepository.save(queueTask);
    }
} 