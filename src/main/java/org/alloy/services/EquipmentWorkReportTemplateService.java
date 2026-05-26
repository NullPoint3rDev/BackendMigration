package org.alloy.services;

import org.alloy.models.dto.EquipmentWorkReportTemplateDTO;
import org.alloy.models.entities.EquipmentWorkReportTemplate;
import org.alloy.repositories.EquipmentWorkReportTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EquipmentWorkReportTemplateService {

    @Autowired
    private EquipmentWorkReportTemplateRepository templateRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Transactional(readOnly = true)
    public Optional<EquipmentWorkReportTemplateDTO> getTemplateById(Long templateId) {
        return templateRepository.findById(templateId).map(this::convertToDTO);
    }

    /**
     * Шаблон для генерации отчёта без Hibernate (не открывает OSIV-сессию на весь долгий запрос).
     */
    public Optional<EquipmentWorkReportTemplateDTO> getTemplateByIdForReport(Long templateId) {
        if (templateId == null) return Optional.empty();
        List<EquipmentWorkReportTemplateDTO> rows = jdbcTemplate.query(
                "SELECT id, name, include_actual_current_range, actual_current_min, actual_current_max, "
                        + "min_interval_between_welds_sec, min_weld_duration_sec, selected_equipment_ids, selected_columns "
                        + "FROM equipment_work_report_templates WHERE id = ? AND is_active = TRUE",
                (rs, rowNum) -> {
                    EquipmentWorkReportTemplateDTO dto = new EquipmentWorkReportTemplateDTO();
                    dto.setTemplateId(rs.getLong("id"));
                    dto.setTemplateName(rs.getString("name"));
                    dto.setIncludeActualCurrentRange(rs.getBoolean("include_actual_current_range"));
                    if (rs.wasNull()) dto.setIncludeActualCurrentRange(false);
                    int min = rs.getInt("actual_current_min");
                    if (!rs.wasNull()) dto.setActualCurrentMin(min);
                    int max = rs.getInt("actual_current_max");
                    if (!rs.wasNull()) dto.setActualCurrentMax(max);
                    int interval = rs.getInt("min_interval_between_welds_sec");
                    if (!rs.wasNull()) dto.setMinIntervalBetweenWeldsSec(interval);
                    int dur = rs.getInt("min_weld_duration_sec");
                    if (!rs.wasNull()) dto.setMinWeldDurationSec(dur);
                    String eqIds = rs.getString("selected_equipment_ids");
                    if (eqIds != null && !eqIds.trim().isEmpty()) {
                        dto.setSelectedEquipmentIds(Arrays.stream(eqIds.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(s -> {
                                    try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
                                })
                                .filter(id -> id != null)
                                .collect(Collectors.toList()));
                    }
                    String cols = rs.getString("selected_columns");
                    if (cols != null && !cols.trim().isEmpty()) {
                        dto.setSelectedColumns(Arrays.stream(cols.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList()));
                    }
                    return dto;
                },
                templateId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
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
