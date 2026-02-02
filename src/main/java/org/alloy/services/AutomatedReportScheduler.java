package org.alloy.services;

import org.alloy.models.entities.AutomatedReport;
import org.alloy.models.ReportHistory;
import org.alloy.models.entities.Notification;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.dto.ReportRequestDTO;
import org.alloy.models.dto.WireConsumptionReportDTO;
import org.alloy.models.dto.WelderReportDTO;
import org.alloy.models.dto.WorkReportDTO;
import org.alloy.models.dto.ReportTemplateDTO;
import org.alloy.models.dto.WireConsumptionReportTemplateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class AutomatedReportScheduler {

    @Autowired
    private AutomatedReportService automatedReportService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDataService reportDataService;

    @Autowired
    private ReportHistoryService reportHistoryService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AutomatedReportDataFixService dataFixService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ReportTemplateService reportTemplateService;

    /**
     * Публичный метод для немедленного выполнения отчета по ID
     * Использует ту же бизнес-логику, что и планировщик
     */
    public void executeNow(Long automatedReportId) {
        try {
            AutomatedReport report = automatedReportService.getAutomatedReportById(automatedReportId)
                    .orElseThrow(() -> new IllegalArgumentException("Automated report with id " + automatedReportId + " not found"));

            System.out.println("DEBUG AutomatedReportScheduler: Manual executeNow called for report: " + report.getName());
            executeAutomatedReport(report);
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: executeNow failed for id " + automatedReportId + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Проверяет и выполняет автоматические отчеты каждую минуту
     */
    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void checkAndExecuteAutomatedReports() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        System.out.println("DEBUG AutomatedReportScheduler: Checking for automated reports to execute at " + now + " (UTC)");

        try {
            // Получаем все активные автоматические отчеты
            List<AutomatedReport> activeReports = automatedReportService.getActiveAutomatedReports();

            System.out.println("DEBUG AutomatedReportScheduler: Found " + activeReports.size() + " active automated reports");

            for (AutomatedReport automatedReport : activeReports) {
                System.out.println("DEBUG AutomatedReportScheduler: Checking report: " + automatedReport.getName() +
                        " (ID: " + automatedReport.getId() + ", templateType: " + automatedReport.getTemplateType() +
                        ", nextRun: " + automatedReport.getNextRun() + ")");

                // Проверяем, нужно ли выполнить отчет
                if (shouldExecuteReport(automatedReport)) {
                    System.out.println("DEBUG AutomatedReportScheduler: Executing automated report: " + automatedReport.getName());
                    executeAutomatedReport(automatedReport);
                } else {
                    System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + " - not ready for execution");
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Error checking automated reports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Проверяет, нужно ли выполнить отчет
     */
    private boolean shouldExecuteReport(AutomatedReport automatedReport) {
        if (automatedReport.getNextRun() == null) {
            System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + " - nextRun is null, skipping");
            return false;
        }

        // Получаем текущее время в UTC
        LocalDateTime nowUTC = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime nextRun = automatedReport.getNextRun();

        // Конвертируем время выполнения в UTC (вычитаем 3 часа для московского времени)
        LocalDateTime nextRunUTC = nextRun.minusHours(3);

        // Вычисляем разницу во времени
        long minutesDiff = java.time.Duration.between(nowUTC, nextRunUTC).toMinutes();
        long secondsDiff = java.time.Duration.between(nowUTC, nextRunUTC).getSeconds();

        boolean shouldExecute = false;

        // Выполняем отчет только если время уже наступило или прошло
        // Добавляем небольшую задержку (30 секунд) для надежности
        if (secondsDiff <= 30) {
            // Время наступило или прошло (с учетом 30 секунд задержки)
            shouldExecute = true;
            if (minutesDiff < 0) {
                System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() +
                        " - Time has passed (" + Math.abs(minutesDiff) + " minutes ago), executing");
            } else {
                System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() +
                        " - Time has arrived (within 30 seconds), executing");
            }
        } else {
            // Время еще не наступило
            shouldExecute = false;
            System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() +
                    " - Time not yet arrived (" + minutesDiff + " minutes remaining), skipping");
        }

        // ДОПОЛНИТЕЛЬНАЯ ЗАЩИТА: если отчет уже выполнен сегодня, не выполняем повторно
        if (shouldExecute) {
            // Проверяем, был ли уже выполнен отчет сегодня
            String templateType = automatedReport.getTemplateType();
            if (templateType != null && !templateType.trim().isEmpty()) {
                LocalDateTime todayStart = nowUTC.toLocalDate().atStartOfDay();
                List<ReportHistory> todayReports = reportHistoryService.getRecentReports(templateType)
                        .stream()
                        .filter(r -> r.getGeneratedAt() != null && r.getGeneratedAt().isAfter(todayStart))
                        .filter(r -> r.getAutomatedReportId() != null && r.getAutomatedReportId().equals(automatedReport.getId()))
                        .collect(java.util.stream.Collectors.toList());

                if (!todayReports.isEmpty()) {
                    System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() +
                            " - Already executed today (" + todayReports.size() + " times), skipping");
                    return false;
                }
            } else {
                System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() +
                        " - templateType is null or empty, skipping duplicate check");
            }
        }

        System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() +
                " - nowUTC: " + nowUTC + ", nextRun (Moscow): " + nextRun + ", nextRunUTC: " + nextRunUTC +
                ", minutesDiff: " + minutesDiff + ", secondsDiff: " + secondsDiff + ", shouldExecute: " + shouldExecute);

        return shouldExecute;
    }

    /**
     * Выполняет автоматический отчет
     */
    private void executeAutomatedReport(AutomatedReport automatedReport) {
        try {
            // СНАЧАЛА обновляем время следующего запуска, чтобы предотвратить повторное выполнение
            updateNextRunTime(automatedReport);

            // Автоматически исправляем данные если они некорректные
            dataFixService.fixAutomatedReportData(automatedReport);

            // Проверяем обязательные поля
            if (automatedReport.getTemplateType() == null || automatedReport.getTemplateType().trim().isEmpty()) {
                throw new IllegalArgumentException("Template type is required for automated report: " + automatedReport.getName());
            }

            if (automatedReport.getTemplateName() == null || automatedReport.getTemplateName().trim().isEmpty()) {
                throw new IllegalArgumentException("Template name is required for automated report: " + automatedReport.getName());
            }

            // Проверяем, существует ли шаблон отчета
            if (automatedReport.getTemplateId() != null) {
                java.util.Optional<ReportTemplateDTO> templateOpt = reportTemplateService.getTemplateById(automatedReport.getTemplateId());
                if (!templateOpt.isPresent()) {
                    System.out.println("DEBUG AutomatedReportScheduler: Template with ID " + automatedReport.getTemplateId() +
                            " not found for report " + automatedReport.getName() + ". Skipping execution.");
                    // Помечаем отчет как неактивный, если шаблон удален
                    automatedReport.setIsActive(false);
                    automatedReportService.updateAutomatedReport(automatedReport);
                    return;
                }
            }

            // Создаем запрос для генерации отчета
            ReportRequestDTO reportRequest = createReportRequest(automatedReport);

            // Генерируем отчет в зависимости от типа шаблона
            byte[] reportBytes = generateReportByType(reportRequest, automatedReport);

            // Получаем данные отчета для сохранения
            Object reportData = getReportDataByType(reportRequest, automatedReport);

            // Создаем имя файла
            String fileName = createFileName(automatedReport);

            // Определяем формат файла на основе типа шаблона
            // Для wire-consumption используем EXCEL, для остальных - PDF
            String fileFormat = "EXCEL";
            if (!"wire-consumption".equalsIgnoreCase(automatedReport.getTemplateType())) {
                fileFormat = "PDF";
            }

            // Сохраняем отчет в историю с данными
            ReportHistory reportHistory = new ReportHistory(
                    automatedReport.getTemplateType(),
                    automatedReport.getTemplateName(),
                    fileFormat,
                    "AUTO", // Автоматический период
                    fileName,
                    (long) reportBytes.length,
                    "Система",
                    automatedReport.getId()
            );

            // Устанавливаем данные отчета
            reportHistory.setReportData(reportData);

            reportHistoryService.addReportToHistory(reportHistory);

            // Создаем уведомление об успешной генерации
            createSuccessNotification(automatedReport, reportHistory);

            // Отправляем отчет по email
            sendReportByEmail(automatedReport, reportHistory, reportBytes);

            System.out.println("DEBUG AutomatedReportScheduler: Successfully executed automated report: " + automatedReport.getName());

        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to execute automated report " + automatedReport.getName() + ": " + e.getMessage());
            e.printStackTrace();

            // Уведомления об ошибках отключены, чтобы не спамить пользователей
            // Особенно когда шаблоны были удалены, но AutomatedReport еще существует
            // Ошибки логируются в консоль для отладки
        }
    }

    /**
     * Создает запрос для генерации отчета на основе шаблона
     */
    private ReportRequestDTO createReportRequest(AutomatedReport automatedReport) {
        ReportRequestDTO request = new ReportRequestDTO();
        request.setReportType(automatedReport.getTemplateType());
        request.setFormat("EXCEL"); // Используем Excel для отчетов по расходу проволоки
        request.setPeriod("DAY"); // По умолчанию за день
        request.setDateFrom(LocalDateTime.now().minusDays(1));
        request.setDateTo(LocalDateTime.now());

        return request;
    }

    /**
     * Генерирует отчет в зависимости от типа, используя шаблон если доступен
     */
    private byte[] generateReportByType(ReportRequestDTO request, AutomatedReport automatedReport) throws Exception {
        String templateType = automatedReport.getTemplateType();
        if (templateType == null || templateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Template type is null or empty for automated report: " + automatedReport.getName());
        }

        // Если есть templateId, используем шаблон для генерации отчета
        if (automatedReport.getTemplateId() != null) {
            return generateReportFromTemplate(automatedReport);
        }

        // Иначе используем старую логику
        switch (templateType.toLowerCase()) {
            case "equipment":
                List<WorkReportDTO> workData = reportDataService.getWorkReportData(request);
                return reportService.generateWorkReport(workData, request.getFormat());

            case "welders":
                List<WelderReportDTO> welderData = reportDataService.getWelderReportData(request);
                return reportService.generateWelderReport(welderData, request.getFormat());

            case "materials":
            case "wire-consumption":
                List<WireConsumptionReportDTO> wireData = reportDataService.getWireConsumptionData(request);
                return reportService.generateWireConsumptionReport(wireData, request.getFormat());

            default:
                throw new IllegalArgumentException("Unknown report type: " + automatedReport.getTemplateType());
        }
    }

    /**
     * Генерирует отчет из шаблона
     */
    private byte[] generateReportFromTemplate(AutomatedReport automatedReport) throws Exception {
        try {
            // Получаем шаблон
            java.util.Optional<ReportTemplateDTO> templateOpt = reportTemplateService.getTemplateById(automatedReport.getTemplateId());
            if (!templateOpt.isPresent()) {
                throw new IllegalArgumentException("Template with ID " + automatedReport.getTemplateId() + " not found");
            }

            ReportTemplateDTO reportTemplate = templateOpt.get();

            // Преобразуем ReportTemplateDTO в WireConsumptionReportTemplateDTO
            WireConsumptionReportTemplateDTO wireTemplate = convertToWireTemplate(reportTemplate);

            // Определяем период отчета на основе настроек шаблона (selectedPeriod: 'week' или 'day')
            java.time.LocalDate periodStartDate;
            java.time.LocalDate periodEndDate;
            java.time.LocalTime periodStartTime = java.time.LocalTime.MIN;
            java.time.LocalTime periodEndTime = java.time.LocalTime.MAX;

            java.time.LocalDate today = java.time.LocalDate.now();

            // Получаем тип периода из настроек шаблона
            // Сначала проверяем periodType (новое поле: "За 24 часа", "За 7 дней", "Произвольный период")
            // Если periodType не указан, используем selectedPeriod (старое поле: "day", "week", "month")
            String periodType = null;
            String selectedPeriod = "day"; // По умолчанию

            if (reportTemplate.getPeriodSettings() != null) {
                java.util.Map<String, Object> periodSettings = reportTemplate.getPeriodSettings();

                // Проверяем periodType (новое поле)
                Object periodTypeObj = periodSettings.get("periodType");
                if (periodTypeObj != null) {
                    periodType = periodTypeObj.toString();
                    System.out.println("DEBUG AutomatedReportScheduler: periodType from template: " + periodType);
                }

                // Проверяем selectedPeriod (старое поле, для обратной совместимости)
                Object periodObj = periodSettings.get("selectedPeriod");
                if (periodObj != null) {
                    selectedPeriod = periodObj.toString();
                    System.out.println("DEBUG AutomatedReportScheduler: selectedPeriod from template: " + selectedPeriod);
                }
            }

            // Определяем период на основе periodType или selectedPeriod
            if (periodType != null) {
                if ("За 7 дней".equals(periodType)) {
                    // Отчет за последние 7 дней (сегодня минус 7 дней до сегодня минус 1 день)
                    periodStartDate = today.minusDays(7);
                    periodEndDate = today.minusDays(1);
                    System.out.println("DEBUG AutomatedReportScheduler: Using periodType 'За 7 дней': " + periodStartDate + " - " + periodEndDate);
                } else if ("За 24 часа".equals(periodType)) {
                    // Отчет за последние 24 часа (вчерашний день)
                    periodStartDate = today.minusDays(1);
                    periodEndDate = today.minusDays(1);
                    System.out.println("DEBUG AutomatedReportScheduler: Using periodType 'За 24 часа': " + periodStartDate + " - " + periodEndDate);
                } else if ("Произвольный период".equals(periodType)) {
                    // Используем даты из startDate и endDate из periodSettings
                    java.util.Map<String, Object> periodSettings = reportTemplate.getPeriodSettings();
                    if (periodSettings != null) {
                        Object startDateObj = periodSettings.get("startDate");
                        Object endDateObj = periodSettings.get("endDate");

                        if (startDateObj != null && endDateObj != null) {
                            try {
                                // Парсим даты из ISO строки
                                String startDateStr = startDateObj.toString();
                                String endDateStr = endDateObj.toString();

                                // Если это ISO строка с временем, извлекаем только дату
                                if (startDateStr.contains("T")) {
                                    startDateStr = startDateStr.substring(0, startDateStr.indexOf("T"));
                                }
                                if (endDateStr.contains("T")) {
                                    endDateStr = endDateStr.substring(0, endDateStr.indexOf("T"));
                                }

                                periodStartDate = java.time.LocalDate.parse(startDateStr);
                                periodEndDate = java.time.LocalDate.parse(endDateStr);

                                System.out.println("DEBUG AutomatedReportScheduler: Using periodType 'Произвольный период' with dates from template: " + periodStartDate + " - " + periodEndDate);
                            } catch (Exception e) {
                                System.err.println("ERROR AutomatedReportScheduler: Failed to parse startDate/endDate from template: " + e.getMessage());
                                // Fallback на selectedPeriod
                                if ("week".equals(selectedPeriod)) {
                                    periodStartDate = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                                    periodEndDate = periodStartDate.plusDays(6);
                                } else {
                                    periodStartDate = today.minusDays(1);
                                    periodEndDate = today.minusDays(1);
                                }
                                System.out.println("DEBUG AutomatedReportScheduler: Using periodType 'Произвольный период' with selectedPeriod fallback: " + periodStartDate + " - " + periodEndDate);
                            }
                        } else {
                            // Если даты не указаны, используем selectedPeriod как fallback
                            if ("week".equals(selectedPeriod)) {
                                periodStartDate = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                                periodEndDate = periodStartDate.plusDays(6);
                            } else {
                                periodStartDate = today.minusDays(1);
                                periodEndDate = today.minusDays(1);
                            }
                            System.out.println("DEBUG AutomatedReportScheduler: Using periodType 'Произвольный период' with selectedPeriod fallback (no dates): " + periodStartDate + " - " + periodEndDate);
                        }
                    } else {
                        // periodSettings null, используем selectedPeriod
                        if ("week".equals(selectedPeriod)) {
                            periodStartDate = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                            periodEndDate = periodStartDate.plusDays(6);
                        } else {
                            periodStartDate = today.minusDays(1);
                            periodEndDate = today.minusDays(1);
                        }
                        System.out.println("DEBUG AutomatedReportScheduler: Using periodType 'Произвольный период' with selectedPeriod fallback (no periodSettings): " + periodStartDate + " - " + periodEndDate);
                    }
                } else {
                    // Неизвестный periodType, используем selectedPeriod
                    if ("week".equals(selectedPeriod)) {
                        periodStartDate = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                        periodEndDate = periodStartDate.plusDays(6);
                        System.out.println("DEBUG AutomatedReportScheduler: Using selectedPeriod 'week' (fallback): " + periodStartDate + " - " + periodEndDate);
                    } else {
                        periodStartDate = today.minusDays(1);
                        periodEndDate = today.minusDays(1);
                        System.out.println("DEBUG AutomatedReportScheduler: Using selectedPeriod 'day' (fallback): " + periodStartDate + " - " + periodEndDate);
                    }
                }
            } else {
                // periodType не указан, используем selectedPeriod (старая логика)
                if ("week".equals(selectedPeriod)) {
                    // Отчет за прошлую неделю (с понедельника по воскресенье)
                    periodStartDate = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                    periodEndDate = periodStartDate.plusDays(6);
                    System.out.println("DEBUG AutomatedReportScheduler: Using selectedPeriod 'week' (legacy): " + periodStartDate + " - " + periodEndDate);
                } else {
                    // Отчет за вчерашний день (по умолчанию)
                    periodStartDate = today.minusDays(1);
                    periodEndDate = today.minusDays(1);
                    System.out.println("DEBUG AutomatedReportScheduler: Using selectedPeriod 'day' (legacy): " + periodStartDate + " - " + periodEndDate);
                }
            }

            // Также получаем время из настроек периода, если указано
            if (reportTemplate.getPeriodSettings() != null) {
                java.util.Map<String, Object> periodSettings = reportTemplate.getPeriodSettings();
                Object startTimeObj = periodSettings.get("startTime");
                Object endTimeObj = periodSettings.get("endTime");

                if (startTimeObj != null && !startTimeObj.toString().trim().isEmpty()) {
                    try {
                        periodStartTime = java.time.LocalTime.parse(startTimeObj.toString());
                    } catch (Exception e) {
                        // Игнорируем ошибку парсинга
                    }
                }
                if (endTimeObj != null && !endTimeObj.toString().trim().isEmpty()) {
                    try {
                        periodEndTime = java.time.LocalTime.parse(endTimeObj.toString());
                    } catch (Exception e) {
                        // Игнорируем ошибку парсинга
                    }
                }
            }

            // Получаем данные отчета
            List<WireConsumptionReportDTO> data = reportDataService.getWireConsumptionDataNew(
                    wireTemplate,
                    periodStartDate,
                    periodEndDate,
                    periodStartTime,
                    periodEndTime
            );

            // Генерируем Excel отчет
            return reportService.generateWireConsumptionReportNew(
                    data, wireTemplate, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to generate report from template: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Преобразует ReportTemplateDTO в WireConsumptionReportTemplateDTO
     */
    private WireConsumptionReportTemplateDTO convertToWireTemplate(ReportTemplateDTO reportTemplate) {
        WireConsumptionReportTemplateDTO wireTemplate = new WireConsumptionReportTemplateDTO();

        wireTemplate.setTemplateId(reportTemplate.getId());
        wireTemplate.setTemplateName(reportTemplate.getName());
        wireTemplate.setSelectedOrganizationUnitIds(reportTemplate.getSelectedOrganizationUnitIds());
        wireTemplate.setSelectedWelderIds(reportTemplate.getSelectedWelderIds());
        wireTemplate.setSelectedEquipmentModels(reportTemplate.getSelectedEquipmentModels());

        // Преобразуем диапазоны токов
        if (reportTemplate.getCurrentRanges() != null) {
            java.util.Map<String, Object> currentRanges = reportTemplate.getCurrentRanges();
            if (currentRanges.get("workOutsideSetCurrent") != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> setCurrent = (java.util.Map<String, Object>) currentRanges.get("workOutsideSetCurrent");
                if (setCurrent != null && setCurrent.get("min") != null) {
                    wireTemplate.setSetCurrentMin(((Number) setCurrent.get("min")).intValue());
                }
                if (setCurrent != null && setCurrent.get("max") != null) {
                    wireTemplate.setSetCurrentMax(((Number) setCurrent.get("max")).intValue());
                }
            }
            if (currentRanges.get("workOutsideActualCurrent") != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> actualCurrent = (java.util.Map<String, Object>) currentRanges.get("workOutsideActualCurrent");
                if (actualCurrent != null && actualCurrent.get("min") != null) {
                    wireTemplate.setActualCurrentMin(((Number) actualCurrent.get("min")).intValue());
                }
                if (actualCurrent != null && actualCurrent.get("max") != null) {
                    wireTemplate.setActualCurrentMax(((Number) actualCurrent.get("max")).intValue());
                }
            }
        }

        // Преобразуем выбранные колонки из reportParameters
        if (reportTemplate.getReportParameters() != null) {
            java.util.Map<String, Object> params = reportTemplate.getReportParameters();
            java.util.List<String> selectedColumns = new java.util.ArrayList<>();

            if (Boolean.TRUE.equals(params.get("equipmentModel"))) selectedColumns.add("equipmentModel");
            if (Boolean.TRUE.equals(params.get("tableNumber"))) selectedColumns.add("tableNumber");
            if (Boolean.TRUE.equals(params.get("profession"))) selectedColumns.add("profession");
            if (Boolean.TRUE.equals(params.get("department"))) selectedColumns.add("department");
            if (Boolean.TRUE.equals(params.get("equipmentName"))) selectedColumns.add("equipmentName");
            if (Boolean.TRUE.equals(params.get("timeOnline"))) selectedColumns.add("timeOnline");
            if (Boolean.TRUE.equals(params.get("arcBurningTime"))) selectedColumns.add("arcBurningTime");
            if (Boolean.TRUE.equals(params.get("efficiency"))) selectedColumns.add("efficiency");
            if (Boolean.TRUE.equals(params.get("energyConsumed"))) selectedColumns.add("energyConsumed");
            if (Boolean.TRUE.equals(params.get("workOutsideSetCurrent"))) {
                selectedColumns.add("workOutsideSetCurrent");
            }
            if (Boolean.TRUE.equals(params.get("workOutsideActualCurrent"))) {
                selectedColumns.add("workOutsideActualCurrent");
            }

            wireTemplate.setSelectedColumns(selectedColumns);
        }

        // Преобразуем выбранные дни недели из periodSettings
        if (reportTemplate.getPeriodSettings() != null) {
            java.util.Map<String, Object> periodSettings = reportTemplate.getPeriodSettings();
            if (periodSettings.get("selectedDays") != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> selectedDays = (java.util.List<String>) periodSettings.get("selectedDays");
                wireTemplate.setSelectedDays(selectedDays);
            }
        }

        return wireTemplate;
    }

    /**
     * Получает значение триггера из AutomatedReport
     */
    private String getTriggerValue(AutomatedReport automatedReport) {
        try {
            if (automatedReport.getTriggersConfig() == null || automatedReport.getTriggersConfig().trim().isEmpty()) {
                return "daily";
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<org.alloy.models.dto.TriggerDTO> triggers = mapper.readValue(
                    automatedReport.getTriggersConfig(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<org.alloy.models.dto.TriggerDTO>>() {}
            );

            if (!triggers.isEmpty()) {
                org.alloy.models.dto.TriggerDTO activeTrigger = triggers.stream()
                        .filter(org.alloy.models.dto.TriggerDTO::getIsActive)
                        .findFirst()
                        .orElse(null);

                if (activeTrigger != null && activeTrigger.getValue() != null) {
                    return activeTrigger.getValue();
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to parse trigger value: " + e.getMessage());
        }

        return "daily"; // По умолчанию
    }

    /**
     * Получает данные отчета в зависимости от типа для сохранения в истории
     */
    private Object getReportDataByType(ReportRequestDTO request, AutomatedReport automatedReport) {
        String templateType = automatedReport.getTemplateType();
        if (templateType == null || templateType.trim().isEmpty()) {
            return null;
        }

        // Если есть templateId, используем шаблон для получения данных
        if (automatedReport.getTemplateId() != null) {
            try {
                java.util.Optional<ReportTemplateDTO> templateOpt = reportTemplateService.getTemplateById(automatedReport.getTemplateId());
                if (templateOpt.isPresent()) {
                    ReportTemplateDTO reportTemplate = templateOpt.get();
                    WireConsumptionReportTemplateDTO wireTemplate = convertToWireTemplate(reportTemplate);

                    java.time.LocalDate periodStartDate = java.time.LocalDate.now().minusDays(1);
                    java.time.LocalDate periodEndDate = java.time.LocalDate.now().minusDays(1);
                    java.time.LocalTime periodStartTime = java.time.LocalTime.MIN;
                    java.time.LocalTime periodEndTime = java.time.LocalTime.MAX;

                    return reportDataService.getWireConsumptionDataNew(
                            wireTemplate, periodStartDate, periodEndDate, periodStartTime, periodEndTime);
                }
            } catch (Exception e) {
                System.err.println("ERROR AutomatedReportScheduler: Failed to get report data from template: " + e.getMessage());
            }
        }

        // Иначе используем старую логику
        switch (templateType.toLowerCase()) {
            case "equipment":
                return reportDataService.getWorkReportData(request);

            case "welders":
                return reportDataService.getWelderReportData(request);

            case "materials":
            case "wire-consumption":
                return reportDataService.getWireConsumptionData(request);

            default:
                return null;
        }
    }

    /**
     * Создает имя файла для отчета
     */
    private String createFileName(AutomatedReport automatedReport) {
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = "pdf";
        if ("wire-consumption".equalsIgnoreCase(automatedReport.getTemplateType())) {
            extension = "xlsx";
        }
        return String.format("%s_%s_%s.%s",
                automatedReport.getTemplateType(),
                automatedReport.getName().replaceAll("[^a-zA-Z0-9]", "_"),
                timestamp,
                extension);
    }

    /**
     * Создает уведомление об успешной генерации
     */
    private void createSuccessNotification(AutomatedReport automatedReport, ReportHistory reportHistory) {
        try {
            // Получаем правильный user_accountid для уведомления
            Integer userId = getValidUserId(automatedReport.getCreatedBy());
            if (userId == null) {
                System.out.println("WARN AutomatedReportScheduler: Cannot create notification - no valid user found for report: " + automatedReport.getName());
                return;
            }

            Notification notification = new Notification();
            notification.setUserAccountId(userId);
            notification.setTitle("Отчет сгенерирован автоматически");
            notification.setMessage(String.format("Автоматический отчет '%s' успешно сгенерирован и сохранен.", automatedReport.getName()));
            notification.setType("AUTOMATED_REPORT");
            notification.setIsRead(false);
            notification.setLink("/my-reports"); // Ссылка на страницу с отчетами
            notification.setDateCreated(LocalDateTime.now()); // Устанавливаем текущую дату и время

            notificationService.createNotification(notification);

            System.out.println("DEBUG AutomatedReportScheduler: Created success notification for user " + userId);
            System.out.println("DEBUG AutomatedReportScheduler: Notification details - Title: " + notification.getTitle() + ", Type: " + notification.getType() + ", UserId: " + notification.getUserAccountId());
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to create success notification: " + e.getMessage());
            // Не прерываем выполнение из-за ошибки уведомления
        }
    }

    /**
     * Создает уведомление об ошибке
     */
    private void createErrorNotification(AutomatedReport automatedReport, String errorMessage) {
        try {
            // Получаем правильный user_accountid для уведомления
            Integer userId = getValidUserId(automatedReport.getCreatedBy());
            if (userId == null) {
                System.out.println("WARN AutomatedReportScheduler: Cannot create error notification - no valid user found for report: " + automatedReport.getName());
                return;
            }

            Notification notification = new Notification();
            notification.setUserAccountId(userId);
            notification.setTitle("Ошибка генерации автоматического отчета");
            notification.setMessage(String.format("Не удалось сгенерировать автоматический отчет '%s': %s", automatedReport.getName(), errorMessage));
            notification.setType("AUTOMATED_REPORT_ERROR");
            notification.setIsRead(false);
            notification.setLink("/automated-reports"); // Ссылка на страницу автоматических отчетов
            notification.setDateCreated(LocalDateTime.now()); // Устанавливаем текущую дату и время

            notificationService.createNotification(notification);

            System.out.println("DEBUG AutomatedReportScheduler: Created error notification for user " + userId);
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to create error notification: " + e.getMessage());
            // Не прерываем выполнение из-за ошибки уведомления
        }
    }

    /**
     * Обновляет время следующего запуска
     */
    private void updateNextRunTime(AutomatedReport automatedReport) {
        // Пересчитываем следующее время выполнения
        automatedReportService.calculateNextRunTime(automatedReport);
        automatedReportService.updateAutomatedReport(automatedReport);

        System.out.println("DEBUG AutomatedReportScheduler: Updated next run time for report " + automatedReport.getName() + " to " + automatedReport.getNextRun());
    }

    /**
     * Отправляет отчет по email пользователю
     */
    private void sendReportByEmail(AutomatedReport automatedReport, ReportHistory reportHistory, byte[] reportBytes) {
        try {
            // Проверяем, включены ли email уведомления
            if (automatedReport.getEmailNotifications() == null || !automatedReport.getEmailNotifications()) {
                System.out.println("DEBUG AutomatedReportScheduler: Email notifications disabled for report: " + automatedReport.getName());
                return;
            }

            // Получаем список получателей
            String emailRecipients = automatedReport.getEmailRecipients();
            if (emailRecipients == null || emailRecipients.trim().isEmpty()) {
                System.out.println("WARN AutomatedReportScheduler: No email recipients specified for report: " + automatedReport.getName());
                return;
            }

            // Разбиваем список получателей по запятым
            String[] recipients = emailRecipients.split(",");
            int sentCount = 0;

            for (String recipient : recipients) {
                String email = recipient.trim();
                if (isValidEmail(email)) {
                    try {
                        // Отправляем отчет по email
                        emailService.sendAutomatedReportNotification(
                                email,
                                "Получатель", // Имя получателя (можно улучшить)
                                automatedReport.getName(),
                                automatedReport.getTemplateType(),
                                reportHistory.getFileName(),
                                reportBytes
                        );

                        sentCount++;
                        System.out.println("DEBUG AutomatedReportScheduler: Report sent by email to " + email);
                    } catch (Exception e) {
                        System.err.println("ERROR AutomatedReportScheduler: Failed to send report to " + email + ": " + e.getMessage());
                        e.printStackTrace();
                        // Не прерываем выполнение, но логируем ошибку
                        // Можно добавить уведомление пользователю об ошибке отправки email
                    }
                } else {
                    System.out.println("WARN AutomatedReportScheduler: Invalid email address: " + email);
                }
            }

            System.out.println("DEBUG AutomatedReportScheduler: Report sent to " + sentCount + " recipients for report: " + automatedReport.getName());

        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to send report by email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Отправляет email уведомление об ошибке
     */
    private void sendErrorEmailNotification(AutomatedReport automatedReport, String errorMessage) {
        try {
            // Проверяем, включены ли email уведомления
            if (automatedReport.getEmailNotifications() == null || !automatedReport.getEmailNotifications()) {
                System.out.println("DEBUG AutomatedReportScheduler: Email notifications disabled for error notification: " + automatedReport.getName());
                return;
            }

            // Получаем список получателей
            String emailRecipients = automatedReport.getEmailRecipients();
            if (emailRecipients == null || emailRecipients.trim().isEmpty()) {
                System.out.println("WARN AutomatedReportScheduler: No email recipients specified for error notification: " + automatedReport.getName());
                return;
            }

            // Разбиваем список получателей по запятым
            String[] recipients = emailRecipients.split(",");
            int sentCount = 0;

            for (String recipient : recipients) {
                String email = recipient.trim();
                if (isValidEmail(email)) {
                    try {
                        // Отправляем уведомление об ошибке по email
                        emailService.sendReportErrorNotification(
                                email,
                                "Получатель", // Имя получателя
                                automatedReport.getName(),
                                errorMessage
                        );

                        sentCount++;
                        System.out.println("DEBUG AutomatedReportScheduler: Error notification sent by email to " + email);
                    } catch (Exception e) {
                        System.err.println("ERROR AutomatedReportScheduler: Failed to send error notification to " + email + ": " + e.getMessage());
                    }
                } else {
                    System.out.println("WARN AutomatedReportScheduler: Invalid email address for error notification: " + email);
                }
            }

            System.out.println("DEBUG AutomatedReportScheduler: Error notification sent to " + sentCount + " recipients for report: " + automatedReport.getName());

        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to send error email notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Проверяет валидность email адреса
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Простая проверка формата email
        return email.contains("@") && email.contains(".") && !email.contains("@test.com");
    }

    /**
     * Получает валидный user_accountid для уведомления
     * Если переданный ID не существует, возвращает ID первого существующего пользователя
     */
    private Integer getValidUserId(Integer originalUserId) {
        try {
            // Если переданный ID существует, используем его
            if (originalUserId != null && userAccountService.existsById(originalUserId)) {
                return originalUserId;
            }

            // Иначе находим первого существующего пользователя
            List<UserAccount> allUsers = userAccountService.getAllUserAccounts();
            if (!allUsers.isEmpty()) {
                Integer firstUserId = allUsers.get(0).getId();
                System.out.println("DEBUG AutomatedReportScheduler: Using first available user ID: " + firstUserId + " instead of invalid ID: " + originalUserId);
                return firstUserId;
            }

            System.err.println("ERROR AutomatedReportScheduler: No users found in the system");
            return null;
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to get valid user ID: " + e.getMessage());
            return null;
        }
    }

}
