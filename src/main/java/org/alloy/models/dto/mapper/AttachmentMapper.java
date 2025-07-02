package org.alloy.models.dto.mapper;

import org.alloy.models.dto.AttachmentDTO;
import org.alloy.models.entities.Attachment;

public class AttachmentMapper {
    public static AttachmentDTO toDTO(Attachment entity) {
        if (entity == null) return null;
        AttachmentDTO dto = new AttachmentDTO();
        dto.setId(entity.getId());
        dto.setFilename(entity.getFilename());
        // downloadUrl будет формироваться в сервисе/контроллере
        return dto;
    }

    public static Attachment toEntity(AttachmentDTO dto) {
        if (dto == null) return null;
        Attachment entity = new Attachment();
        entity.setId(dto.getId());
        entity.setFilename(dto.getFilename());
        // filePath и message задаются отдельно
        return entity;
    }
} 