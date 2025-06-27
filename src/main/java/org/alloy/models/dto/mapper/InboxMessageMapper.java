package org.alloy.models.dto.mapper;

import org.alloy.models.entities.InboxMessage;
import org.alloy.models.dto.InboxMessageDTO;
import org.alloy.models.dto.UserAccountShortDTO;
import org.alloy.models.dto.mapper.UserAccountMapper;

public class InboxMessageMapper {
    public static InboxMessageDTO toDTO(InboxMessage message) {
        if (message == null) return null;
        InboxMessageDTO dto = new InboxMessageDTO();
        dto.setId(message.getId());
        dto.setSubject(message.getSubject());
        dto.setBody(message.getMessage());
        if (message.getUserAccount() != null) {
            UserAccountShortDTO senderDto = UserAccountMapper.toShortDTO(message.getUserAccount());
            dto.setSender(senderDto);
        }
        if (message.getUserAccountTo() != null) {
            UserAccountShortDTO recipientDto = UserAccountMapper.toShortDTO(message.getUserAccountTo());
            dto.setRecipient(recipientDto);
        }
        // ... другие нужные поля
        return dto;
    }

    public static InboxMessage toEntity(InboxMessageDTO dto) {
        if (dto == null) return null;
        InboxMessage entity = new InboxMessage();
        entity.setId(dto.getId());
        entity.setSubject(dto.getSubject());
        entity.setMessage(dto.getBody());
        if (dto.getSender() != null) {
            entity.setUserAccountId(dto.getSender().getId());
        }
        if (dto.getRecipient() != null) {
            entity.setUserAccountToId(dto.getRecipient().getId());
        }
        // ... другие нужные поля
        return entity;
    }
} 