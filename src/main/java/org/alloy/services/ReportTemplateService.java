package org.alloy.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.models.dto.ReportTemplateDTO;
import org.alloy.models.dto.TriggerDTO;
import org.alloy.models.entities.ReportTemplate;
import org.alloy.models.entities.AutomatedReport;
import org.alloy.repositories.ReportTemplateRepository;
import org.alloy.repositories.AutomatedReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class ReportTemplateService {

    @Autowired
    private ReportTemplateRepository templateRepository;

    @Autowired
    private AutomatedReportRepository automatedReportRepository;

    @Autowired
    private AutomatedReportService automatedReportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Сохраняет или обновляет шаблон
     */
    @Transactional
    public ReportTemplateDTO saveTemplate(ReportTemplateDTO dto, Integer createdBy) {
        ReportTemplate template;

        if (dto.getId() != null) {
            // Обновление существующего шаблона
            Optional<ReportTemplate> existingOpt = templateRepository.findById(dto.getId());
            if (existingOpt.isPresent()) {
                template = existingOpt.get();
                // Проверяем права доступа (только создатель может редактировать)
                if (!template.getCreatedBy().equals(createdBy)) {
                    throw new SecurityException("Нет прав для редактирования этого шаблона");
                }
            } else {
                throw new IllegalArgumentException("Шаблон с ID " + dto.getId() + " не найден");
            }
        } else {
            // Создание нового шаблона
            template = new ReportTemplate();
            template.setCreatedBy(createdBy);
        }

        // Обновляем поля
        if (dto.getName() != null) {
            template.setName(dto.getName());
        }

        if (dto.getEmail() != null) {
            template.setEmail(dto.getEmail());
        }

        // Сохраняем сложные объекты как JSON
        if (dto.getReportParameters() != null) {
            try {
                template.setReportParameters(
                        objectMapper.writeValueAsString(dto.getReportParameters()));
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации reportParameters", e);
            }
        }

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

        if (dto.getSelectedEquipmentModels() != null) {
            try {
                template.setSelectedEquipmentModels(
                        objectMapper.writeValueAsString(dto.getSelectedEquipmentModels()));
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации selectedEquipmentModels", e);
            }
        }

        if (dto.getCurrentRanges() != null) {
            try {
                template.setCurrentRanges(
                        objectMapper.writeValueAsString(dto.getCurrentRanges()));
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации currentRanges", e);
            }
        }

        if (dto.getPeriodSettings() != null) {
            try {
                template.setPeriodSettings(
                        objectMapper.writeValueAsString(dto.getPeriodSettings()));
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации periodSettings", e);
            }
        }

        if (dto.getAutoReportSettings() != null && !dto.getAutoReportSettings().isEmpty()) {
            // Проверяем, что есть хотя бы одно значимое поле
            boolean hasValidConfig = dto.getAutoReportSettings().containsKey("autoReportTime") ||
                    (dto.getAutoReportSettings().containsKey("autoReportWeekDays") &&
                            dto.getAutoReportSettings().get("autoReportWeekDays") != null) ||
                    (dto.getAutoReportSettings().containsKey("autoReportMonthDays") &&
                            dto.getAutoReportSettings().get("autoReportMonthDays") != null);

            if (hasValidConfig) {
                try {
                    template.setAutoReportSettings(
                            objectMapper.writeValueAsString(dto.getAutoReportSettings()));
                } catch (Exception e) {
                    throw new RuntimeException("Ошибка сериализации autoReportSettings", e);
                }
            } else {
                // Если объект пустой или не содержит значимых полей, очищаем
                template.setAutoReportSettings(null);
            }
        } else {
            // Явно очищаем autoReportSettings, если они null или пустые
            template.setAutoReportSettings(null);
        }

        if (dto.getIsActive() != null) {
            template.setIsActive(dto.getIsActive());
        }

        template = templateRepository.save(template);

        // Если есть настройки автоматического отчета (не пустые), создаем/обновляем AutomatedReport
        if (dto.getAutoReportSettings() != null && !dto.getAutoReportSettings().isEmpty()) {
            // Проверяем, что есть хотя бы одно из полей: время, дни недели или дни месяца
            boolean hasAutoReportConfig = dto.getAutoReportSettings().containsKey("autoReportTime") ||
                    (dto.getAutoReportSettings().containsKey("autoReportWeekDays") &&
                            dto.getAutoReportSettings().get("autoReportWeekDays") != null) ||
                    (dto.getAutoReportSettings().containsKey("autoReportMonthDays") &&
                            dto.getAutoReportSettings().get("autoReportMonthDays") != null);

            if (hasAutoReportConfig) {
                createOrUpdateAutomatedReport(template, dto);
            } else {
                // Если настройки есть, но они пустые, деактивируем AutomatedReport
                deactivateAutomatedReportsForTemplate(template.getId());
            }
        } else {
            // Если настроек автоматического отчета нет, деактивируем связанные AutomatedReport
            deactivateAutomatedReportsForTemplate(template.getId());
        }

        return convertToDTO(template);
    }

    /**
     * Создает или обновляет AutomatedReport для активного шаблона
     */
    private void createOrUpdateAutomatedReport(ReportTemplate template, ReportTemplateDTO dto) {
        try {
            // Ищем существующий AutomatedReport для этого шаблона
            List<AutomatedReport> existingReports = automatedReportRepository.findByTemplateId(template.getId());
            AutomatedReport automatedReport;

            if (!existingReports.isEmpty()) {
                // Обновляем существующий отчет
                automatedReport = existingReports.get(0);
            } else {
                // Создаем новый отчет
                automatedReport = new AutomatedReport();
                automatedReport.setCreatedBy(template.getCreatedBy());
            }

            // Устанавливаем основные поля
            automatedReport.setName(template.getName());
            automatedReport.setTemplateId(template.getId());
            automatedReport.setTemplateName(template.getName());
            // Тип отчёта берём из шаблона (reportParameters.reportType с фронта)
            String templateType = mapReportTypeToTemplateType(dto.getReportParameters());
            automatedReport.setTemplateType(templateType);
            automatedReport.setIsActive(true);
            automatedReport.setEmailNotifications(true);
            automatedReport.setEmailRecipients(template.getEmail());

            // Преобразуем autoReportSettings в triggersConfig
            String triggersConfig = convertAutoReportSettingsToTriggersConfig(dto.getAutoReportSettings());
            automatedReport.setTriggersConfig(triggersConfig);

            // Сохраняем AutomatedReport
            automatedReportService.createAutomatedReport(automatedReport);

            System.out.println("DEBUG ReportTemplateService: Created/updated AutomatedReport for template ID: " + template.getId());

        } catch (Exception e) {
            System.err.println("ERROR ReportTemplateService: Failed to create/update AutomatedReport: " + e.getMessage());
            e.printStackTrace();
            // Не прерываем сохранение шаблона из-за ошибки создания автоматического отчета
        }
    }

    /**
     * Преобразует reportType из шаблона (русское название с фронта) в templateType для AutomatedReport.
     */
    private String mapReportTypeToTemplateType(Map<String, Object> reportParameters) {
        if (reportParameters == null) return "wire-consumption";
        Object reportTypeObj = reportParameters.get("reportType");
        if (reportTypeObj == null || !(reportTypeObj instanceof String)) return "wire-consumption";
        String reportType = ((String) reportTypeObj).trim();
        if ("По работе оборудования (швы)".equals(reportType)) return "equipment";
        if ("По работе сварщика (швы)".equals(reportType)) return "welder";
        return "wire-consumption";
    }

    /**
     * Преобразует настройки автоматического отчета в конфигурацию триггеров
     */
    private String convertAutoReportSettingsToTriggersConfig(Map<String, Object> autoReportSettings) {
        try {
            List<TriggerDTO> triggers = new ArrayList<>();
            TriggerDTO trigger = new TriggerDTO();
            trigger.setType("TIME");
            trigger.setIsActive(true);

            // Получаем время из autoReportSettings
            String time = (String) autoReportSettings.get("autoReportTime");
            if (time != null && !time.trim().isEmpty()) {
                trigger.setTime(time);
            } else {
                trigger.setTime("08:00"); // По умолчанию 8:00
            }

            // Определяем частоту выполнения на основе выбранных дней
            @SuppressWarnings("unchecked")
            List<String> weekDays = autoReportSettings.get("autoReportWeekDays") instanceof List
                    ? (List<String>) autoReportSettings.get("autoReportWeekDays")
                    : null;
            @SuppressWarnings("unchecked")
            List<Integer> monthDays = autoReportSettings.get("autoReportMonthDays") instanceof List
                    ? (List<Integer>) autoReportSettings.get("autoReportMonthDays")
                    : null;

            if (weekDays != null && !weekDays.isEmpty()) {
                // Еженедельное выполнение по дням недели
                trigger.setValue("weekly");
                // Преобразуем русские названия дней в английские
                String daysOfWeek = weekDays.stream()
                        .map(this::convertDayNameToEnglish)
                        .collect(Collectors.joining(","));
                trigger.setDaysOfWeek(daysOfWeek);
                trigger.setDescription("Еженедельно в " + trigger.getTime() + " по дням: " + String.join(", ", weekDays));
            } else if (monthDays != null && !monthDays.isEmpty()) {
                // Ежемесячное выполнение по числам месяца
                trigger.setValue("monthly");
                // Берем первое число месяца (можно расширить логику для нескольких чисел)
                trigger.setDayOfMonth(monthDays.get(0));
                trigger.setDescription("Ежемесячно " + monthDays.get(0) + " числа в " + trigger.getTime());
            } else {
                // Ежедневное выполнение по умолчанию
                trigger.setValue("daily");
                trigger.setDescription("Ежедневно в " + trigger.getTime());
            }

            triggers.add(trigger);

            // Сериализуем триггеры в JSON
            return objectMapper.writeValueAsString(triggers);

        } catch (Exception e) {
            System.err.println("ERROR ReportTemplateService: Failed to convert autoReportSettings to triggersConfig: " + e.getMessage());
            e.printStackTrace();
            // Возвращаем триггер по умолчанию (ежедневно в 8:00)
            try {
                List<TriggerDTO> defaultTriggers = new ArrayList<>();
                TriggerDTO defaultTrigger = new TriggerDTO();
                defaultTrigger.setType("TIME");
                defaultTrigger.setValue("daily");
                defaultTrigger.setTime("08:00");
                defaultTrigger.setIsActive(true);
                defaultTrigger.setDescription("Ежедневно в 08:00");
                defaultTriggers.add(defaultTrigger);
                return objectMapper.writeValueAsString(defaultTriggers);
            } catch (Exception ex) {
                return "[]";
            }
        }
    }

    /**
     * Преобразует русское название дня недели в английское
     */
    private String convertDayNameToEnglish(String dayName) {
        switch (dayName.trim()) {
            case "Пн": return "MONDAY";
            case "Вт": return "TUESDAY";
            case "Ср": return "WEDNESDAY";
            case "Чт": return "THURSDAY";
            case "Пт": return "FRIDAY";
            case "Сб": return "SATURDAY";
            case "Вс": return "SUNDAY";
            default: return "MONDAY";
        }
    }

    /**
     * Деактивирует все AutomatedReport для указанного шаблона
     */
    private void deactivateAutomatedReportsForTemplate(Long templateId) {
        try {
            List<AutomatedReport> reports = automatedReportRepository.findByTemplateId(templateId);
            for (AutomatedReport report : reports) {
                report.setIsActive(false);
                automatedReportRepository.save(report);
            }
            System.out.println("DEBUG ReportTemplateService: Deactivated " + reports.size() + " AutomatedReport(s) for template ID: " + templateId);
        } catch (Exception e) {
            System.err.println("ERROR ReportTemplateService: Failed to deactivate AutomatedReports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Получает шаблон по ID
     */
    @Transactional(readOnly = true)
    public Optional<ReportTemplateDTO> getTemplateById(Long templateId) {
        return templateRepository.findById(templateId)
                .map(this::convertToDTO);
    }

    /**
     * Получает все активные шаблоны
     */
    @Transactional(readOnly = true)
    public List<ReportTemplateDTO> getAllActiveTemplates() {
        return templateRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получает все шаблоны пользователя
     */
    @Transactional(readOnly = true)
    public List<ReportTemplateDTO> getTemplatesByUser(Integer userId) {
        return templateRepository.findByCreatedBy(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получает все активные шаблоны пользователя
     */
    @Transactional(readOnly = true)
    public List<ReportTemplateDTO> getActiveTemplatesByUser(Integer userId) {
        return templateRepository.findByCreatedByAndIsActiveTrue(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Удаляет шаблон (только создатель может удалить)
     */
    @Transactional
    public void deleteTemplate(Long templateId, Integer userId) {
        Optional<ReportTemplate> templateOpt = templateRepository.findByIdAndCreatedBy(templateId, userId);
        if (templateOpt.isPresent()) {
            templateRepository.delete(templateOpt.get());
        } else {
            throw new IllegalArgumentException("Шаблон не найден или нет прав для удаления");
        }
    }

    /**
     * Конвертирует Entity в DTO
     */
    private ReportTemplateDTO convertToDTO(ReportTemplate template) {
        ReportTemplateDTO dto = new ReportTemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setEmail(template.getEmail());
        dto.setCreatedBy(template.getCreatedBy());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        dto.setIsActive(template.getIsActive());

        // Десериализуем JSON поля
        try {
            if (template.getReportParameters() != null) {
                dto.setReportParameters(
                        objectMapper.readValue(template.getReportParameters(),
                                new TypeReference<Map<String, Object>>() {}));
            }

            if (template.getSelectedOrganizationUnitIds() != null) {
                dto.setSelectedOrganizationUnitIds(
                        objectMapper.readValue(template.getSelectedOrganizationUnitIds(),
                                new TypeReference<List<Integer>>() {}));
            }

            if (template.getSelectedWelderIds() != null) {
                dto.setSelectedWelderIds(
                        objectMapper.readValue(template.getSelectedWelderIds(),
                                new TypeReference<List<Integer>>() {}));
            }

            if (template.getSelectedEquipmentModels() != null) {
                dto.setSelectedEquipmentModels(
                        objectMapper.readValue(template.getSelectedEquipmentModels(),
                                new TypeReference<List<String>>() {}));
            }

            if (template.getCurrentRanges() != null) {
                dto.setCurrentRanges(
                        objectMapper.readValue(template.getCurrentRanges(),
                                new TypeReference<Map<String, Object>>() {}));
            }

            if (template.getPeriodSettings() != null) {
                dto.setPeriodSettings(
                        objectMapper.readValue(template.getPeriodSettings(),
                                new TypeReference<Map<String, Object>>() {}));
            }

            if (template.getAutoReportSettings() != null && !template.getAutoReportSettings().trim().isEmpty()) {
                try {
                    Map<String, Object> autoSettings = objectMapper.readValue(template.getAutoReportSettings(),
                            new TypeReference<Map<String, Object>>() {});
                    // Проверяем, что настройки не пустые и содержат хотя бы одно значимое поле
                    if (autoSettings != null && !autoSettings.isEmpty()) {
                        // Проверяем, что есть хотя бы одно из полей: время, дни недели или дни месяца
                        boolean hasValidConfig = autoSettings.containsKey("autoReportTime") &&
                                autoSettings.get("autoReportTime") != null &&
                                !autoSettings.get("autoReportTime").toString().trim().isEmpty();

                        if (!hasValidConfig && autoSettings.containsKey("autoReportWeekDays")) {
                            Object weekDays = autoSettings.get("autoReportWeekDays");
                            hasValidConfig = weekDays instanceof List && !((List<?>) weekDays).isEmpty();
                        }

                        if (!hasValidConfig && autoSettings.containsKey("autoReportMonthDays")) {
                            Object monthDays = autoSettings.get("autoReportMonthDays");
                            hasValidConfig = monthDays instanceof List && !((List<?>) monthDays).isEmpty();
                        }

                        if (hasValidConfig) {
                            dto.setAutoReportSettings(autoSettings);
                        } else {
                            dto.setAutoReportSettings(null);
                        }
                    } else {
                        dto.setAutoReportSettings(null);
                    }
                } catch (Exception e) {
                    // Если ошибка десериализации, устанавливаем null
                    System.err.println("WARN ReportTemplateService: Failed to deserialize autoReportSettings: " + e.getMessage());
                    dto.setAutoReportSettings(null);
                }
            } else {
                dto.setAutoReportSettings(null);
            }
        } catch (Exception e) {
            System.err.println("Ошибка десериализации шаблона: " + e.getMessage());
            e.printStackTrace();
        }

        return dto;
    }
}

