package org.alloy.models.dto.mapper;

import org.alloy.models.entities.SurveyQuestion;
import org.alloy.models.dto.SurveyQuestionDTO;

public class SurveyQuestionMapper {
    public static SurveyQuestionDTO toDTO(SurveyQuestion entity) {
        if (entity == null) return null;
        SurveyQuestionDTO dto = new SurveyQuestionDTO();
        dto.setId(entity.getId());
        // ... добавить другие поля по необходимости
        return dto;
    }

    public static SurveyQuestion toEntity(SurveyQuestionDTO dto) {
        if (dto == null) return null;
        SurveyQuestion entity = new SurveyQuestion();
        entity.setId(dto.getId());
        // ... добавить другие поля по необходимости
        return entity;
    }
} 