package org.alloy.models.dto.mapper;

import org.alloy.models.dto.MessageDTO;
import org.alloy.models.dto.AttachmentDTO;
import org.alloy.models.dto.UserAccountShortDTO;
import org.alloy.models.entities.Message;
import org.alloy.models.entities.Attachment;
import org.alloy.models.entities.UserAccount;
import java.util.List;
import java.util.stream.Collectors;

public class MessageMapper {
    public static MessageDTO toDTO(Message entity) {
        if (entity == null) return null;
        MessageDTO dto = new MessageDTO();
        dto.setId(entity.getId());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        if (entity.getSender() != null) {
            UserAccountShortDTO sender = UserAccountMapper.toShortDTO(entity.getSender());
            dto.setSender(sender);
        }
        if (entity.getRecipient() != null) {
            UserAccountShortDTO recipient = UserAccountMapper.toShortDTO(entity.getRecipient());
            dto.setRecipient(recipient);
        }
        dto.setDateSent(entity.getDateSent());
        dto.setIsRead(entity.getIsRead());
        if (entity.getAttachments() != null) {
            List<AttachmentDTO> attachments = entity.getAttachments().stream()
                .map(AttachmentMapper::toDTO)
                .collect(Collectors.toList());
            dto.setAttachments(attachments);
        }
        return dto;
    }

    public static Message toEntity(MessageDTO dto, UserAccount sender, UserAccount recipient, List<Attachment> attachments) {
        if (dto == null) return null;
        Message entity = new Message();
        entity.setId(dto.getId());
        entity.setSubject(dto.getSubject());
        entity.setBody(dto.getBody());
        entity.setSender(sender);
        entity.setRecipient(recipient);
        entity.setDateSent(dto.getDateSent());
        entity.setIsRead(dto.getIsRead());
        entity.setAttachments(attachments);
        return entity;
    }
} 