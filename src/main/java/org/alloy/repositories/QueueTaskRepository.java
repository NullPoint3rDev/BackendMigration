package org.alloy.repositories;

import org.alloy.models.entities.QueueTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueueTaskRepository extends JpaRepository<QueueTask, Integer> {
} 