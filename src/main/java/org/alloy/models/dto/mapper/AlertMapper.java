package org.alloy.models.dto.mapper;

import org.alloy.models.entities.Alert;
import org.alloy.models.dto.AlertDTO;

public class AlertMapper {
    public static AlertDTO toDTO(Alert alert) {
        if (alert == null) return null;
        AlertDTO dto = new AlertDTO();
        dto.setId(alert.getId());
        dto.setMessage(alert.getMessage());
        dto.setType(alert.getType());
        // ... другие нужные поля
        return dto;
    }
} 