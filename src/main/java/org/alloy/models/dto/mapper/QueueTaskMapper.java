package org.alloy.models.dto.mapper;

import org.alloy.models.entities.QueueTask;
import org.alloy.models.dto.QueueTaskDTO;

public class QueueTaskMapper {
    public static QueueTaskDTO toDTO(QueueTask task) {
        if (task == null) return null;
        QueueTaskDTO dto = new QueueTaskDTO();
        dto.setId(task.getId());
        dto.setName(task.getTaskName());
        dto.setStatus(task.getStatus() != null ? String.valueOf(task.getStatus()) : null);
        // ... другие нужные поля
        return dto;
    }
} 