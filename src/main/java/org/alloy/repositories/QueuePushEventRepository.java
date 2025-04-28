package org.alloy.repositories;

import org.alloy.models.entities.QueuePushEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueuePushEventRepository extends JpaRepository<QueuePushEvent, Integer> {
} 