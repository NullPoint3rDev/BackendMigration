package org.alloy.models.dto.mapper;

import org.alloy.models.entities.Survey;
import org.alloy.models.dto.SurveyDTO;

public class SurveyMapper {
    public static SurveyDTO toDTO(Survey entity) {
        if (entity == null) return null;
        SurveyDTO dto = new SurveyDTO();
        dto.setId(entity.getId());
        // ... добавить другие поля по необходимости
        return dto;
    }

    public static Survey toEntity(SurveyDTO dto) {
        if (dto == null) return null;
        Survey entity = new Survey();
        entity.setId(dto.getId());
        // ... добавить другие поля по необходимости
        return entity;
    }
} 