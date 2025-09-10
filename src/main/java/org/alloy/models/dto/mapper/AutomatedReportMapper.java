package org.alloy.models.dto.mapper;

import org.alloy.models.entities.AutomatedReport;
import org.alloy.models.dto.AutomatedReportDTO;
import org.alloy.models.dto.TriggerDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.ArrayList;

public class AutomatedReportMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AutomatedReportDTO toDTO(AutomatedReport entity) {
        if (entity == null) {
            return null;
        }

        AutomatedReportDTO dto = new AutomatedReportDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setTemplateId(entity.getTemplateId());
        dto.setTemplateName(entity.getTemplateName());
        dto.setTemplateType(entity.getTemplateType());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setLastRun(entity.getLastRun());
        dto.setNextRun(entity.getNextRun());
        dto.setRunCount(entity.getRunCount());
        dto.setSuccessCount(entity.getSuccessCount());
        dto.setErrorCount(entity.getErrorCount());
        dto.setLastErrorMessage(entity.getLastErrorMessage());
        dto.setConfiguration(entity.getConfiguration());
        dto.setEmailNotifications(entity.getEmailNotifications());
        dto.setEmailRecipients(entity.getEmailRecipients());
        dto.setRetryCount(entity.getRetryCount());
        dto.setMaxRetries(entity.getMaxRetries());
        dto.setRetryDelayMinutes(entity.getRetryDelayMinutes());
        dto.setTimezone(entity.getTimezone());
        dto.setPriority(entity.getPriority());
        dto.setTags(entity.getTags());

        // Парсинг триггеров из JSON
        if (entity.getTriggersConfig() != null && !entity.getTriggersConfig().trim().isEmpty()) {
            try {
                List<TriggerDTO> triggers = objectMapper.readValue(
                    entity.getTriggersConfig(), 
                    new TypeReference<List<TriggerDTO>>() {}
                );
                dto.setTriggers(triggers);
            } catch (Exception e) {
                // Если не удалось распарсить, оставляем пустой список
                dto.setTriggers(new ArrayList<>());
            }
        } else {
            dto.setTriggers(new ArrayList<>());
        }

        // Вычисление статуса
        dto.setStatus(calculateStatus(entity));
        
        // Вычисление процента успешности
        dto.calculateSuccessRate();

        return dto;
    }

    public static AutomatedReport toEntity(AutomatedReportDTO dto) {
        if (dto == null) {
            return null;
        }

        AutomatedReport entity = new AutomatedReport();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setTemplateId(dto.getTemplateId());
        entity.setTemplateName(dto.getTemplateName());
        entity.setTemplateType(dto.getTemplateType());
        entity.setIsActive(dto.getIsActive());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setLastRun(dto.getLastRun());
        entity.setNextRun(dto.getNextRun());
        entity.setRunCount(dto.getRunCount());
        entity.setSuccessCount(dto.getSuccessCount());
        entity.setErrorCount(dto.getErrorCount());
        entity.setLastErrorMessage(dto.getLastErrorMessage());
        entity.setConfiguration(dto.getConfiguration());
        entity.setEmailNotifications(dto.getEmailNotifications());
        entity.setEmailRecipients(dto.getEmailRecipients());
        entity.setRetryCount(dto.getRetryCount());
        entity.setMaxRetries(dto.getMaxRetries());
        entity.setRetryDelayMinutes(dto.getRetryDelayMinutes());
        entity.setTimezone(dto.getTimezone());
        entity.setPriority(dto.getPriority());
        entity.setTags(dto.getTags());

        // Сериализация триггеров в JSON
        if (dto.getTriggers() != null && !dto.getTriggers().isEmpty()) {
            try {
                String triggersJson = objectMapper.writeValueAsString(dto.getTriggers());
                entity.setTriggersConfig(triggersJson);
            } catch (Exception e) {
                // Если не удалось сериализовать, оставляем null
                entity.setTriggersConfig(null);
            }
        } else {
            entity.setTriggersConfig(null);
        }

        return entity;
    }

    public static List<AutomatedReportDTO> toDTOList(List<AutomatedReport> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }

        List<AutomatedReportDTO> dtos = new ArrayList<>();
        for (AutomatedReport entity : entities) {
            AutomatedReportDTO dto = toDTO(entity);
            if (dto != null) {
                dtos.add(dto);
            }
        }
        return dtos;
    }

    public static List<AutomatedReport> toEntityList(List<AutomatedReportDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }

        List<AutomatedReport> entities = new ArrayList<>();
        for (AutomatedReportDTO dto : dtos) {
            AutomatedReport entity = toEntity(dto);
            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }

    private static String calculateStatus(AutomatedReport entity) {
        if (entity == null || !entity.getIsActive()) {
            return "INACTIVE";
        }

        if (entity.getNextRun() == null) {
            return "NO_SCHEDULE";
        }

        if (entity.getNextRun().isBefore(java.time.LocalDateTime.now())) {
            return "OVERDUE";
        }

        return "ACTIVE";
    }

    // Методы для обновления отдельных полей
    public static void updateEntityFromDTO(AutomatedReport entity, AutomatedReportDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setTemplateId(dto.getTemplateId());
        entity.setTemplateName(dto.getTemplateName());
        entity.setTemplateType(dto.getTemplateType());
        entity.setIsActive(dto.getIsActive());
        entity.setConfiguration(dto.getConfiguration());
        entity.setEmailNotifications(dto.getEmailNotifications());
        entity.setEmailRecipients(dto.getEmailRecipients());
        entity.setMaxRetries(dto.getMaxRetries());
        entity.setRetryDelayMinutes(dto.getRetryDelayMinutes());
        entity.setTimezone(dto.getTimezone());
        entity.setPriority(dto.getPriority());
        entity.setTags(dto.getTags());

        // Обновление триггеров
        if (dto.getTriggers() != null && !dto.getTriggers().isEmpty()) {
            try {
                String triggersJson = objectMapper.writeValueAsString(dto.getTriggers());
                entity.setTriggersConfig(triggersJson);
            } catch (Exception e) {
                // Если не удалось сериализовать, оставляем как есть
            }
        } else {
            entity.setTriggersConfig(null);
        }

        entity.setUpdatedAt(java.time.LocalDateTime.now());
    }

    // Метод для создания копии DTO
    public static AutomatedReportDTO copyDTO(AutomatedReportDTO original) {
        if (original == null) {
            return null;
        }

        AutomatedReportDTO copy = new AutomatedReportDTO();
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setDescription(original.getDescription());
        copy.setTemplateId(original.getTemplateId());
        copy.setTemplateName(original.getTemplateName());
        copy.setTemplateType(original.getTemplateType());
        copy.setIsActive(original.getIsActive());
        copy.setCreatedBy(original.getCreatedBy());
        copy.setCreatedByName(original.getCreatedByName());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setUpdatedAt(original.getUpdatedAt());
        copy.setLastRun(original.getLastRun());
        copy.setNextRun(original.getNextRun());
        copy.setRunCount(original.getRunCount());
        copy.setSuccessCount(original.getSuccessCount());
        copy.setErrorCount(original.getErrorCount());
        copy.setLastErrorMessage(original.getLastErrorMessage());
        copy.setConfiguration(original.getConfiguration());
        copy.setEmailNotifications(original.getEmailNotifications());
        copy.setEmailRecipients(original.getEmailRecipients());
        copy.setRetryCount(original.getRetryCount());
        copy.setMaxRetries(original.getMaxRetries());
        copy.setRetryDelayMinutes(original.getRetryDelayMinutes());
        copy.setTimezone(original.getTimezone());
        copy.setPriority(original.getPriority());
        copy.setTags(original.getTags());
        copy.setStatus(original.getStatus());
        copy.setSuccessRate(original.getSuccessRate());

        // Копирование триггеров
        if (original.getTriggers() != null) {
            List<TriggerDTO> triggersCopy = new ArrayList<>();
            for (TriggerDTO trigger : original.getTriggers()) {
                TriggerDTO triggerCopy = new TriggerDTO();
                triggerCopy.setType(trigger.getType());
                triggerCopy.setValue(trigger.getValue());
                triggerCopy.setDescription(trigger.getDescription());
                triggerCopy.setParameters(trigger.getParameters());
                triggerCopy.setIsActive(trigger.getIsActive());
                triggerCopy.setPriority(trigger.getPriority());
                triggerCopy.setTimezone(trigger.getTimezone());
                triggerCopy.setTime(trigger.getTime());
                triggerCopy.setDaysOfWeek(trigger.getDaysOfWeek());
                triggerCopy.setDayOfMonth(trigger.getDayOfMonth());
                triggerCopy.setThresholdValue(trigger.getThresholdValue());
                triggerCopy.setOperator(trigger.getOperator());
                triggerCopy.setEquipmentIds(trigger.getEquipmentIds());
                triggerCopy.setParameter(trigger.getParameter());
                triggerCopy.setErrorCount(trigger.getErrorCount());
                triggerCopy.setTimeWindowMinutes(trigger.getTimeWindowMinutes());
                triggersCopy.add(triggerCopy);
            }
            copy.setTriggers(triggersCopy);
        }

        return copy;
    }
}
