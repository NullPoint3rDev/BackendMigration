package org.alloy.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.models.dto.WireConsumptionReportTemplateDTO;
import org.alloy.models.entities.WireConsumptionReportTemplate;
import org.alloy.repositories.WireConsumptionReportTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WireConsumptionReportTemplateService {

    @Autowired
    private WireConsumptionReportTemplateRepository templateRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Сохраняет или обновляет шаблон
     */
    @Transactional
    public WireConsumptionReportTemplateDTO saveTemplate(WireConsumptionReportTemplateDTO dto, Integer createdBy) {
        WireConsumptionReportTemplate template;

        if (dto.getTemplateId() != null) {
            // Обновление существующего шаблона
            Optional<WireConsumptionReportTemplate> existingOpt = templateRepository.findById(dto.getTemplateId());
            if (existingOpt.isPresent()) {
                template = existingOpt.get();
                // Проверяем права доступа (только создатель может редактировать)
                if (!template.getCreatedBy().equals(createdBy)) {
                    throw new SecurityException("Нет прав для редактирования этого шаблона");
                }
            } else {
                throw new IllegalArgumentException("Шаблон с ID " + dto.getTemplateId() + " не найден");
            }
        } else {
            // Создание нового шаблона
            template = new WireConsumptionReportTemplate();
            template.setCreatedBy(createdBy);
        }

        // Обновляем поля
        if (dto.getTemplateName() != null) {
            template.setName(dto.getTemplateName());
        }

        // Сохраняем списки как JSON
        if (dto.getSelectedOrganizationUnitIds() != null) {
            try {
                template.setSelectedOrganizationUnitIds(
                        objectMapper.writeValueAsString(dto.getSelectedOrganizationUnitIds()));
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации selectedOrganizationUnitIds", e);
            }
        }

        if (dto.getSelectedWelderIds() != null) {
            try {
                template.setSelectedWelderIds(
                        objectMapper.writeValueAsString(dto.getSelectedWelderIds()));
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации selectedWelderIds", e);
            }
        }

        if (dto.getSelectedColumns() != null) {
            try {
                template.setSelectedColumns(
                        objectMapper.writeValueAsString(dto.getSelectedColumns()));
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации selectedColumns", e);
            }
        }

        template.setSetCurrentMin(dto.getSetCurrentMin());
        template.setSetCurrentMax(dto.getSetCurrentMax());
        template.setActualCurrentMin(dto.getActualCurrentMin());
        template.setActualCurrentMax(dto.getActualCurrentMax());
        template.setSortByColumn(dto.getSortByColumn());
        if (dto.getSortDirection() != null) {
            template.setSortDirection(dto.getSortDirection());
        }

        template = templateRepository.save(template);
        return convertToDTO(template);
    }

    /**
     * Получает шаблон по ID
     */
    public Optional<WireConsumptionReportTemplateDTO> getTemplateById(Long templateId) {
        return templateRepository.findById(templateId)
                .map(this::convertToDTO);
    }

    /**
     * Получает все активные шаблоны
     */
    public List<WireConsumptionReportTemplateDTO> getAllActiveTemplates() {
        List<WireConsumptionReportTemplate> templates = templateRepository.findByIsActiveTrue();
        List<WireConsumptionReportTemplateDTO> result = new ArrayList<>();
        for (WireConsumptionReportTemplate template : templates) {
            result.add(convertToDTO(template));
        }
        return result;
    }

    /**
     * Получает шаблоны пользователя
     */
    public List<WireConsumptionReportTemplateDTO> getTemplatesByUser(Integer userId) {
        List<WireConsumptionReportTemplate> templates = templateRepository.findByCreatedByAndIsActiveTrue(userId);
        List<WireConsumptionReportTemplateDTO> result = new ArrayList<>();
        for (WireConsumptionReportTemplate template : templates) {
            result.add(convertToDTO(template));
        }
        return result;
    }

    /**
     * Удаляет шаблон (мягкое удаление - устанавливает isActive = false)
     */
    @Transactional
    public void deleteTemplate(Long templateId, Integer userId) {
        Optional<WireConsumptionReportTemplate> templateOpt = templateRepository.findById(templateId);
        if (templateOpt.isPresent()) {
            WireConsumptionReportTemplate template = templateOpt.get();
            // Проверяем права доступа
            if (!template.getCreatedBy().equals(userId)) {
                throw new SecurityException("Нет прав для удаления этого шаблона");
            }
            template.setIsActive(false);
            templateRepository.save(template);
        } else {
            throw new IllegalArgumentException("Шаблон с ID " + templateId + " не найден");
        }
    }

    /**
     * Конвертирует Entity в DTO
     */
    private WireConsumptionReportTemplateDTO convertToDTO(WireConsumptionReportTemplate template) {
        WireConsumptionReportTemplateDTO dto = new WireConsumptionReportTemplateDTO();
        dto.setTemplateId(template.getId());
        dto.setTemplateName(template.getName());

        // Десериализуем списки из JSON
        if (template.getSelectedOrganizationUnitIds() != null) {
            try {
                List<Integer> orgUnitIds = objectMapper.readValue(
                        template.getSelectedOrganizationUnitIds(),
                        new TypeReference<List<Integer>>() {});
                dto.setSelectedOrganizationUnitIds(orgUnitIds);
            } catch (Exception e) {
                System.err.println("Ошибка десериализации selectedOrganizationUnitIds: " + e.getMessage());
                dto.setSelectedOrganizationUnitIds(new ArrayList<>());
            }
        }

        if (template.getSelectedWelderIds() != null) {
            try {
                List<Integer> welderIds = objectMapper.readValue(
                        template.getSelectedWelderIds(),
                        new TypeReference<List<Integer>>() {});
                dto.setSelectedWelderIds(welderIds);
            } catch (Exception e) {
                System.err.println("Ошибка десериализации selectedWelderIds: " + e.getMessage());
                dto.setSelectedWelderIds(new ArrayList<>());
            }
        }

        if (template.getSelectedColumns() != null) {
            try {
                List<String> columns = objectMapper.readValue(
                        template.getSelectedColumns(),
                        new TypeReference<List<String>>() {});
                dto.setSelectedColumns(columns);
            } catch (Exception e) {
                System.err.println("Ошибка десериализации selectedColumns: " + e.getMessage());
                dto.setSelectedColumns(new ArrayList<>());
            }
        }

        dto.setSetCurrentMin(template.getSetCurrentMin());
        dto.setSetCurrentMax(template.getSetCurrentMax());
        dto.setActualCurrentMin(template.getActualCurrentMin());
        dto.setActualCurrentMax(template.getActualCurrentMax());
        dto.setSortByColumn(template.getSortByColumn());
        dto.setSortDirection(template.getSortDirection());

        return dto;
    }
}

