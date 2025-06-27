package org.alloy.models.dto.mapper;

import org.alloy.models.entities.Notification;
import org.alloy.models.dto.NotificationDTO;
import org.alloy.models.dto.UserAccountShortDTO;
import org.alloy.models.dto.mapper.UserAccountMapper;

public class NotificationMapper {
    public static NotificationDTO toDTO(Notification notification) {
        if (notification == null) return null;
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        if (notification.getUserAccount() != null) {
            UserAccountShortDTO userDto = UserAccountMapper.toShortDTO(notification.getUserAccount());
            dto.setUserAccount(userDto);
        }
        // ... другие нужные поля
        return dto;
    }
} 