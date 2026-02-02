package org.alloy.services;

import org.alloy.models.dto.WelderWorkReportTemplateDTO;
import org.alloy.models.entities.WelderWorkReportTemplate;
import org.alloy.repositories.WelderWorkReportTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WelderWorkReportTemplateService {

    @Autowired
    private WelderWorkReportTemplateRepository templateRepository;

    @Transactional
    public WelderWorkReportTemplateDTO saveTemplate(WelderWorkReportTemplateDTO dto, Integer createdBy) {
        WelderWorkReportTemplate template;

        if (dto.getTemplateId() != null) {
            Optional<WelderWorkReportTemplate> existingOpt = templateRepository.findById(dto.getTemplateId());
            if (existingOpt.isPresent()) {
                template = existingOpt.get();
                if (!template.getCreatedBy().equals(createdBy)) {
                    throw new SecurityException("Нет прав для редактирования этого шаблона");
                }
            } else {
                throw new IllegalArgumentException("Шаблон с ID " + dto.getTemplateId() + " не найден");
            }
        } else {
            template = new WelderWorkReportTemplate();
            template.setCreatedBy(createdBy);
        }

        if (dto.getTemplateName() != null) {
            template.setName(dto.getTemplateName());
        }

        template.setWelderId(dto.getWelderId());
        template.setIncludeActualCurrentRange(dto.getIncludeActualCurrentRange() != null && dto.getIncludeActualCurrentRange());
        template.setActualCurrentMin(dto.getActualCurrentMin());
        template.setActualCurrentMax(dto.getActualCurrentMax());
        template.setMinIntervalBetweenWeldsSec(dto.getMinIntervalBetweenWeldsSec());
        template.setMinWeldDurationSec(dto.getMinWeldDurationSec());

        template = templateRepository.save(template);
        return convertToDTO(template);
    }

    public Optional<WelderWorkReportTemplateDTO> getTemplateById(Long templateId) {
        return templateRepository.findById(templateId).map(this::convertToDTO);
    }

    public List<WelderWorkReportTemplateDTO> getAllActiveTemplates() {
        List<WelderWorkReportTemplate> templates = templateRepository.findByIsActiveTrue();
        List<WelderWorkReportTemplateDTO> result = new ArrayList<>();
        for (WelderWorkReportTemplate t : templates) {
            result.add(convertToDTO(t));
        }
        return result;
    }

    public List<WelderWorkReportTemplateDTO> getTemplatesByUser(Integer userId) {
        List<WelderWorkReportTemplate> templates = templateRepository.findByCreatedByAndIsActiveTrue(userId);
        List<WelderWorkReportTemplateDTO> result = new ArrayList<>();
        for (WelderWorkReportTemplate t : templates) {
            result.add(convertToDTO(t));
        }
        return result;
    }

    @Transactional
    public void deleteTemplate(Long templateId, Integer userId) {
        Optional<WelderWorkReportTemplate> templateOpt = templateRepository.findById(templateId);
        if (templateOpt.isPresent()) {
            WelderWorkReportTemplate template = templateOpt.get();
            if (!template.getCreatedBy().equals(userId)) {
                throw new SecurityException("Нет прав для удаления этого шаблона");
            }
            template.setIsActive(false);
            templateRepository.save(template);
        } else {
            throw new IllegalArgumentException("Шаблон с ID " + templateId + " не найден");
        }
    }

    private WelderWorkReportTemplateDTO convertToDTO(WelderWorkReportTemplate template) {
        WelderWorkReportTemplateDTO dto = new WelderWorkReportTemplateDTO();
        dto.setTemplateId(template.getId());
        dto.setTemplateName(template.getName());
        dto.setWelderId(template.getWelderId());
        dto.setIncludeActualCurrentRange(template.getIncludeActualCurrentRange());
        dto.setActualCurrentMin(template.getActualCurrentMin());
        dto.setActualCurrentMax(template.getActualCurrentMax());
        dto.setMinIntervalBetweenWeldsSec(template.getMinIntervalBetweenWeldsSec());
        dto.setMinWeldDurationSec(template.getMinWeldDurationSec());
        return dto;
    }
}


