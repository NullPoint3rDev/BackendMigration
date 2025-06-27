package org.alloy.models.dto.mapper;

import org.alloy.models.entities.Translation;
import org.alloy.models.dto.TranslationDTO;

public class TranslationMapper {
    public static TranslationDTO toDTO(Translation entity) {
        if (entity == null) return null;
        TranslationDTO dto = new TranslationDTO();
        dto.setId(entity.getId());
        dto.setKey(entity.getTableName() + "." + entity.getColumnName() + "." + entity.getIdValue() + "." + entity.getLang());
        dto.setValue(entity.getValue());
        // Можно добавить дополнительные поля, если они есть в DTO
        return dto;
    }

    public static Translation toEntity(TranslationDTO dto) {
        if (dto == null) return null;
        Translation entity = new Translation();
        entity.setId(dto.getId());
        // Пример парсинга key: tableName.columnName.idValue.lang
        if (dto.getKey() != null) {
            String[] parts = dto.getKey().split("\\.");
            if (parts.length == 4) {
                entity.setTableName(parts[0]);
                entity.setColumnName(parts[1]);
                entity.setIdValue(parts[2]);
                entity.setLang(parts[3]);
            }
        }
        entity.setValue(dto.getValue());
        return entity;
    }
} 