package org.alloy.services;

import org.alloy.models.dto.EquipmentWorkReportTemplateDTO;
import org.alloy.models.entities.EquipmentWorkReportTemplate;
import org.alloy.repositories.EquipmentWorkReportTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EquipmentWorkReportTemplateService {

    @Autowired
    private EquipmentWorkReportTemplateRepository templateRepository;

    @Transactional
    public EquipmentWorkReportTemplateDTO saveTemplate(EquipmentWorkReportTemplateDTO dto, Integer createdBy) {
        EquipmentWorkReportTemplate template;

        if (dto.getTemplateId() != null) {
            Optional<EquipmentWorkReportTemplate> existingOpt = templateRepository.findById(dto.getTemplateId());
            if (existingOpt.isPresent()) {
                template = existingOpt.get();
                if (!template.getCreatedBy().equals(createdBy)) {
                    throw new SecurityException("Нет прав для редактирования этого шаблона");
                }
            } else {
                throw new IllegalArgumentException("Шаблон с ID " + dto.getTemplateId() + " не найден");
            }
        } else {
            template = new EquipmentWorkReportTemplate();
            template.setCreatedBy(createdBy);
        }

        if (dto.getTemplateName() != null) {
            template.setName(dto.getTemplateName());
        }

        template.setIncludeActualCurrentRange(dto.getIncludeActualCurrentRange() != null && dto.getIncludeActualCurrentRange());
        template.setActualCurrentMin(dto.getActualCurrentMin());
        template.setActualCurrentMax(dto.getActualCurrentMax());
        template.setMinIntervalBetweenWeldsSec(dto.getMinIntervalBetweenWeldsSec());
        template.setMinWeldDurationSec(dto.getMinWeldDurationSec());
        if (dto.getSelectedEquipmentIds() != null) {
            template.setSelectedEquipmentIdsList(dto.getSelectedEquipmentIds());
        }
        if (dto.getSelectedColumns() != null) {
            template.setSelectedColumnsList(dto.getSelectedColumns());
        }

        template = templateRepository.save(template);
        return convertToDTO(template);
    }

    public Optional<EquipmentWorkReportTemplateDTO> getTemplateById(Long templateId) {
        return templateRepository.findById(templateId).map(this::convertToDTO);
    }

    public List<EquipmentWorkReportTemplateDTO> getTemplatesByUser(Integer userId) {
        List<EquipmentWorkReportTemplate> templates = templateRepository.findByCreatedByAndIsActiveTrue(userId);
        List<EquipmentWorkReportTemplateDTO> result = new ArrayList<>();
        for (EquipmentWorkReportTemplate t : templates) {
            result.add(convertToDTO(t));
        }
        return result;
    }

    @Transactional
    public void deleteTemplate(Long templateId, Integer userId) {
        Optional<EquipmentWorkReportTemplate> templateOpt = templateRepository.findById(templateId);
        if (templateOpt.isPresent()) {
            EquipmentWorkReportTemplate template = templateOpt.get();
            if (!template.getCreatedBy().equals(userId)) {
                throw new SecurityException("Нет прав для удаления этого шаблона");
            }
            template.setIsActive(false);
            templateRepository.save(template);
        } else {
            throw new IllegalArgumentException("Шаблон с ID " + templateId + " не найден");
        }
    }

    private EquipmentWorkReportTemplateDTO convertToDTO(EquipmentWorkReportTemplate template) {
        EquipmentWorkReportTemplateDTO dto = new EquipmentWorkReportTemplateDTO();
        dto.setTemplateId(template.getId());
        dto.setTemplateName(template.getName());
        dto.setSelectedEquipmentIds(template.getSelectedEquipmentIdsList());
        dto.setIncludeActualCurrentRange(template.getIncludeActualCurrentRange());
        dto.setActualCurrentMin(template.getActualCurrentMin());
        dto.setActualCurrentMax(template.getActualCurrentMax());
        dto.setMinIntervalBetweenWeldsSec(template.getMinIntervalBetweenWeldsSec());
        dto.setMinWeldDurationSec(template.getMinWeldDurationSec());
        dto.setSelectedColumns(template.getSelectedColumnsList());
        return dto;
    }
}
