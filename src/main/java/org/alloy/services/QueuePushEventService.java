package org.alloy.services;

import org.alloy.models.entities.QueuePushEvent;
import org.alloy.repositories.QueuePushEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QueuePushEventService {
    @Autowired
    private QueuePushEventRepository queuePushEventRepository;

    public List<QueuePushEvent> findAll() {
        return queuePushEventRepository.findAll();
    }

    public Optional<QueuePushEvent> findById(Integer id) {
        return queuePushEventRepository.findById(id);
    }

    public QueuePushEvent save(QueuePushEvent queuePushEvent) {
        return queuePushEventRepository.save(queuePushEvent);
    }

    public void deleteById(Integer id) {
        queuePushEventRepository.deleteById(id);
    }
} 