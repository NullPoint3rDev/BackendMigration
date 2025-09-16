package org.alloy.services;

import org.alloy.models.entities.NotificationTemplate;
import org.alloy.models.dto.NotificationTemplateDTO;
import org.alloy.repositories.NotificationTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationTemplateService {

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    /**
     * Создать новый шаблон уведомления
     */
    public NotificationTemplateDTO createNotificationTemplate(NotificationTemplateDTO templateDTO) {
        NotificationTemplate template = new NotificationTemplate();
        template.setName(templateDTO.getName());
        template.setDescription(templateDTO.getDescription());
        template.setType(templateDTO.getType());
        template.setTriggerType(templateDTO.getTriggerType());
        template.setTriggerValue(templateDTO.getTriggerValue());
        template.setThreshold(templateDTO.getThreshold());
        template.setEquipmentId(templateDTO.getEquipmentId());
        template.setIsActive(templateDTO.getIsActive() != null ? templateDTO.getIsActive() : true);
        template.setCreatedBy(templateDTO.getCreatedBy());
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        NotificationTemplate savedTemplate = notificationTemplateRepository.save(template);
        return convertToDTO(savedTemplate);
    }

    /**
     * Получить все шаблоны уведомлений
     */
    public List<NotificationTemplateDTO> getAllNotificationTemplates() {
        List<NotificationTemplate> templates = notificationTemplateRepository.findAllByOrderByCreatedAtDesc();
        return templates.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Получить активные шаблоны уведомлений
     */
    public List<NotificationTemplateDTO> getActiveNotificationTemplates() {
        List<NotificationTemplate> templates = notificationTemplateRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        return templates.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Получить шаблоны уведомлений по типу триггера
     */
    public List<NotificationTemplateDTO> getNotificationTemplatesByTriggerType(String triggerType) {
        List<NotificationTemplate> templates = notificationTemplateRepository.findByTriggerTypeAndIsActiveTrueOrderByCreatedAtDesc(triggerType);
        return templates.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Получить шаблон уведомления по ID
     */
    public Optional<NotificationTemplateDTO> getNotificationTemplateById(Long id) {
        return notificationTemplateRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Обновить шаблон уведомления
     */
    public NotificationTemplateDTO updateNotificationTemplate(Long id, NotificationTemplateDTO templateDTO) {
        Optional<NotificationTemplate> optionalTemplate = notificationTemplateRepository.findById(id);
        if (optionalTemplate.isPresent()) {
            NotificationTemplate template = optionalTemplate.get();
            template.setName(templateDTO.getName());
            template.setDescription(templateDTO.getDescription());
            template.setType(templateDTO.getType());
            template.setTriggerType(templateDTO.getTriggerType());
            template.setTriggerValue(templateDTO.getTriggerValue());
            template.setThreshold(templateDTO.getThreshold());
            template.setEquipmentId(templateDTO.getEquipmentId());
            template.setIsActive(templateDTO.getIsActive());
            template.setUpdatedAt(LocalDateTime.now());

            NotificationTemplate savedTemplate = notificationTemplateRepository.save(template);
            return convertToDTO(savedTemplate);
        }
        return null;
    }

    /**
     * Удалить шаблон уведомления
     */
    public boolean deleteNotificationTemplate(Long id) {
        if (notificationTemplateRepository.existsById(id)) {
            notificationTemplateRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Переключить статус активности шаблона
     */
    public NotificationTemplateDTO toggleNotificationTemplateStatus(Long id) {
        Optional<NotificationTemplate> optionalTemplate = notificationTemplateRepository.findById(id);
        if (optionalTemplate.isPresent()) {
            NotificationTemplate template = optionalTemplate.get();
            template.setIsActive(!template.getIsActive());
            template.setUpdatedAt(LocalDateTime.now());

            NotificationTemplate savedTemplate = notificationTemplateRepository.save(template);
            return convertToDTO(savedTemplate);
        }
        return null;
    }

    /**
     * Конвертировать Entity в DTO
     */
    private NotificationTemplateDTO convertToDTO(NotificationTemplate template) {
        NotificationTemplateDTO dto = new NotificationTemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setType(template.getType());
        dto.setTriggerType(template.getTriggerType());
        dto.setTriggerValue(template.getTriggerValue());
        dto.setThreshold(template.getThreshold());
        dto.setEquipmentId(template.getEquipmentId());
        dto.setIsActive(template.getIsActive());
        dto.setCreatedBy(template.getCreatedBy());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        return dto;
    }
}
