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
import org.alloy.models.dto.EquipmentWorkReportTemplateDTO;
import org.alloy.models.dto.EquipmentWorkReportSectionDTO;
import org.alloy.models.dto.EquipmentWorkReportDTO;
import org.alloy.models.dto.WelderWorkReportTemplateDTO;
import org.alloy.models.dto.WelderWorkReportSectionDTO;
import org.alloy.models.dto.WelderWorkReportDTO;
import org.alloy.models.dto.EquipmentMalfunctionReportTemplateDTO;
import org.alloy.models.dto.EquipmentMalfunctionReportSectionDTO;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.Welder;
import org.alloy.models.entities.Employee;
import org.alloy.metrics.ReportMetrics;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WelderRepository;
import org.alloy.repositories.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private ReportMetrics reportMetrics;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WelderRepository welderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

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

        // Защита от повторного запуска в течение одной и той же минуты (cron раз в минуту):
        // пропускаем только если этот же автоотчёт уже выполнялся в последние 3 минуты.
        // Так ручной запуск «Выполнить сейчас» или ранний запуск не блокирует запланированный.
        if (shouldExecute) {
            String templateType = automatedReport.getTemplateType();
            if (templateType != null && !templateType.trim().isEmpty()) {
                // generatedAt в истории хранится как LocalDateTime.now() при сохранении (серверная зона)
                LocalDateTime cutoff = LocalDateTime.now().minusMinutes(3);
                List<ReportHistory> recentSameReport = reportHistoryService.getRecentReports(templateType)
                        .stream()
                        .filter(r -> r.getGeneratedAt() != null && r.getGeneratedAt().isAfter(cutoff))
                        .filter(r -> r.getAutomatedReportId() != null && r.getAutomatedReportId().equals(automatedReport.getId()))
                        .collect(java.util.stream.Collectors.toList());

                if (!recentSameReport.isEmpty()) {
                    System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() +
                            " - Already executed in last 3 minutes (" + recentSameReport.size() + " times), skipping");
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
        long start = System.nanoTime();
        boolean success = false;
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

            // Определяем формат файла: EXCEL для отчётов по оборудованию, сварщику, неисправностям и расходу проволоки
            String ttResolved = resolveTemplateTypeForFileFormat(automatedReport);
            String fileFormat = ("wire-consumption".equals(ttResolved) || "equipment".equals(ttResolved)
                    || "welder".equals(ttResolved) || "equipment-malfunction".equals(ttResolved))
                    ? "EXCEL" : "PDF";

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

            success = true;
            System.out.println("DEBUG AutomatedReportScheduler: Successfully executed automated report: " + automatedReport.getName());

        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to execute automated report " + automatedReport.getName() + ": " + e.getMessage());
            e.printStackTrace();

            // Уведомления об ошибках отключены, чтобы не спамить пользователей
            // Особенно когда шаблоны были удалены, но AutomatedReport еще существует
            // Ошибки логируются в консоль для отладки
        } finally {
            String type;
            try {
                type = resolveTemplateTypeForFileFormat(automatedReport);
            } catch (Exception ignore) {
                type = automatedReport.getTemplateType();
            }
            reportMetrics.record(type, "auto", success, System.nanoTime() - start);
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
     * Нормализует тип отчёта: учитывает reportType в шаблоне и строку «по неисправностям оборудования».
     */
    private String normalizeTemplateTypeFromTemplate(ReportTemplateDTO reportTemplate, String automatedTemplateTypeLower) {
        String templateType = automatedTemplateTypeLower != null ? automatedTemplateTypeLower : "";
        if (reportTemplate.getReportParameters() != null && reportTemplate.getReportParameters().get("reportType") != null) {
            String reportType = reportTemplate.getReportParameters().get("reportType").toString().trim();
            if ("По работе оборудования (швы)".equals(reportType)) return "equipment";
            if ("По работе сварщика (швы)".equals(reportType)) return "welder";
            if ("По расходу проволоки".equals(reportType)) return "wire-consumption";
            if ("По неисправностям оборудования".equals(reportType)) return "equipment-malfunction";
        }
        if (templateType.contains("неисправност")) return "equipment-malfunction";
        return templateType;
    }

    /**
     * Тип отчёта для формата файла (xlsx vs pdf): при наличии шаблона — по reportType в шаблоне.
     */
    private String resolveTemplateTypeForFileFormat(AutomatedReport automatedReport) {
        String tt = automatedReport.getTemplateType() != null ? automatedReport.getTemplateType().trim().toLowerCase() : "";
        if (automatedReport.getTemplateId() != null) {
            Optional<ReportTemplateDTO> tOpt = reportTemplateService.getTemplateById(automatedReport.getTemplateId());
            if (tOpt.isPresent()) {
                return normalizeTemplateTypeFromTemplate(tOpt.get(), tt);
            }
        }
        return tt;
    }

    /**
     * Генерирует отчёт «По неисправностям оборудования» из общего шаблона (как {@link org.alloy.controllers.ReportController#generateEquipmentMalfunctionReport}).
     */
    private byte[] generateEquipmentMalfunctionReportFromTemplate(ReportTemplateDTO reportTemplate) throws Exception {
        EquipmentMalfunctionReportTemplateDTO template = convertToMalfunctionTemplate(reportTemplate);
        if (template.getSelectedEquipmentIds() == null || template.getSelectedEquipmentIds().isEmpty()) {
            throw new IllegalArgumentException("Equipment malfunction report template has no selected equipment (selectedEquipmentIds)");
        }

        Object[] period = resolveEquipmentMalfunctionPeriod(reportTemplate);
        java.time.LocalDate periodStartDate = (java.time.LocalDate) period[0];
        java.time.LocalDate periodEndDate = (java.time.LocalDate) period[1];
        java.time.LocalTime periodStartTime = (java.time.LocalTime) period[2];
        java.time.LocalTime periodEndTime = (java.time.LocalTime) period[3];

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime endDateTime = java.time.LocalDateTime.of(periodEndDate, periodEndTime);
        if (endDateTime.isAfter(now)) {
            periodEndDate = now.toLocalDate();
            periodEndTime = now.toLocalTime();
        }

        List<EquipmentMalfunctionReportSectionDTO> sections = reportDataService.getEquipmentMalfunctionData(
                template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

        return reportService.generateEquipmentMalfunctionReportMultiSection(
                sections, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);
    }

    /**
     * Период для отчёта по неисправностям: «За 24 часа» / «За 7 дней» — скользящее окно до «сейчас»; иначе — как в {@link #parsePeriodFromTemplate}.
     */
    private Object[] resolveEquipmentMalfunctionPeriod(ReportTemplateDTO reportTemplate) {
        java.time.LocalDate periodStartDate = java.time.LocalDate.now();
        java.time.LocalDate periodEndDate = java.time.LocalDate.now();
        java.time.LocalTime periodStartTime = java.time.LocalTime.MIN;
        java.time.LocalTime periodEndTime = java.time.LocalTime.of(23, 59, 59);

        String periodTypeFromRequest = "";
        if (reportTemplate.getPeriodSettings() != null) {
            Object pt = reportTemplate.getPeriodSettings().get("periodType");
            if (pt != null) periodTypeFromRequest = pt.toString().trim();
        }
        if ("За 24 часа".equals(periodTypeFromRequest) || "LAST_24_HOURS".equalsIgnoreCase(periodTypeFromRequest)
                || "24h".equalsIgnoreCase(periodTypeFromRequest)) {
            java.time.LocalDateTime end = java.time.LocalDateTime.now();
            java.time.LocalDateTime start = end.minusHours(24);
            periodStartDate = start.toLocalDate();
            periodEndDate = end.toLocalDate();
            periodStartTime = start.toLocalTime();
            periodEndTime = end.toLocalTime();
        } else if ("За 7 дней".equals(periodTypeFromRequest) || "LAST_7_DAYS".equalsIgnoreCase(periodTypeFromRequest)
                || "7DAYS".equalsIgnoreCase(periodTypeFromRequest)) {
            java.time.LocalDateTime end = java.time.LocalDateTime.now();
            java.time.LocalDateTime start = end.minusDays(7);
            periodStartDate = start.toLocalDate();
            periodEndDate = end.toLocalDate();
            periodStartTime = start.toLocalTime();
            periodEndTime = end.toLocalTime();
        } else {
            Object[] fallback = parsePeriodFromTemplate(reportTemplate);
            periodStartDate = (java.time.LocalDate) fallback[0];
            periodEndDate = (java.time.LocalDate) fallback[1];
            periodStartTime = (java.time.LocalTime) fallback[2];
            periodEndTime = (java.time.LocalTime) fallback[3];
        }
        return new Object[]{ periodStartDate, periodEndDate, periodStartTime, periodEndTime };
    }

    /**
     * Преобразует ReportTemplateDTO в EquipmentMalfunctionReportTemplateDTO.
     */
    private EquipmentMalfunctionReportTemplateDTO convertToMalfunctionTemplate(ReportTemplateDTO reportTemplate) {
        EquipmentMalfunctionReportTemplateDTO template = new EquipmentMalfunctionReportTemplateDTO();
        template.setTemplateId(reportTemplate.getId());
        template.setTemplateName(reportTemplate.getName());
        if (reportTemplate.getReportParameters() != null) {
            Map<String, Object> params = reportTemplate.getReportParameters();
            @SuppressWarnings("unchecked")
            List<Number> ids = params.containsKey("selectedEquipmentIds") ? (List<Number>) params.get("selectedEquipmentIds") : null;
            if (ids != null && !ids.isEmpty()) {
                List<Integer> equipmentIds = new ArrayList<>();
                for (Number n : ids) equipmentIds.add(n.intValue());
                template.setSelectedEquipmentIds(equipmentIds);
            }
            List<String> cols = new ArrayList<>();
            if (Boolean.TRUE.equals(params.get("equipmentModel"))) cols.add("equipmentModel");
            if (Boolean.TRUE.equals(params.get("equipmentName"))) cols.add("equipmentName");
            if (Boolean.TRUE.equals(params.get("equipmentDepartment"))) cols.add("equipmentDepartment");
            if (Boolean.TRUE.equals(params.get("serialNumber"))) cols.add("serialNumber");
            if (Boolean.TRUE.equals(params.get("inventoryNumber"))) cols.add("inventoryNumber");
            if (Boolean.TRUE.equals(params.get("malfunctions"))) cols.add("malfunctions");
            if (!cols.isEmpty()) {
                template.setSelectedColumns(cols);
            }
        }
        return template;
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

            // Тип отчёта: из AutomatedReport или из шаблона (для старых записей, где templateType мог быть ошибочно wire-consumption)
            String templateType = normalizeTemplateTypeFromTemplate(reportTemplate,
                    automatedReport.getTemplateType() != null ? automatedReport.getTemplateType().trim().toLowerCase() : "");
            if ("equipment".equals(templateType)) {
                return generateEquipmentReportFromTemplate(reportTemplate, automatedReport);
            }
            if ("welder".equals(templateType)) {
                return generateWelderReportFromTemplate(reportTemplate, automatedReport);
            }
            if ("equipment-malfunction".equals(templateType)) {
                return generateEquipmentMalfunctionReportFromTemplate(reportTemplate);
            }

            // По умолчанию: отчёт по расходу проволоки
            // Преобразуем ReportTemplateDTO в WireConsumptionReportTemplateDTO
            WireConsumptionReportTemplateDTO wireTemplate = convertToWireTemplate(reportTemplate);

            // Определяем период отчета на основе настроек шаблона (selectedPeriod: 'week' или 'day')
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate periodStartDate = today.minusDays(1);
            java.time.LocalDate periodEndDate = today.minusDays(1);
            java.time.LocalTime periodStartTime = java.time.LocalTime.MIN;
            java.time.LocalTime periodEndTime = java.time.LocalTime.MAX;

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
                                java.time.LocalDate start = parseTemplateDate(startDateObj.toString());
                                java.time.LocalDate end = parseTemplateDate(endDateObj.toString());
                                if (start != null) periodStartDate = start;
                                if (end != null) periodEndDate = end;
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
                // Фронт может передавать время в timeRange: { start: "08:00", end: "17:00" }
                Object timeRangeObj = periodSettings.get("timeRange");
                if (timeRangeObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> timeRange = (Map<String, Object>) timeRangeObj;
                    Object start = timeRange.get("start");
                    Object end = timeRange.get("end");
                    if (start != null && !start.toString().trim().isEmpty()) {
                        try {
                            periodStartTime = java.time.LocalTime.parse(start.toString());
                        } catch (Exception e) { /* игнорируем */ }
                    }
                    if (end != null && !end.toString().trim().isEmpty()) {
                        try {
                            periodEndTime = java.time.LocalTime.parse(end.toString());
                        } catch (Exception e) { /* игнорируем */ }
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
     * Генерирует отчёт "По работе оборудования (швы)" из общего шаблона.
     * Период и время берутся из periodSettings шаблона (в т.ч. «Произвольный период» и timeRange).
     */
    private byte[] generateEquipmentReportFromTemplate(ReportTemplateDTO reportTemplate, AutomatedReport automatedReport) throws Exception {
        EquipmentWorkReportTemplateDTO template = convertToEquipmentTemplate(reportTemplate);
        if (template.getSelectedEquipmentIds() == null || template.getSelectedEquipmentIds().isEmpty()) {
            throw new IllegalArgumentException("Equipment report template has no selected equipment (selectedEquipmentIds)");
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate periodStartDate = today.minusDays(1);
        java.time.LocalDate periodEndDate = today.minusDays(1);
        java.time.LocalTime periodStartTime = java.time.LocalTime.MIN;
        java.time.LocalTime periodEndTime = java.time.LocalTime.MAX;

        if (reportTemplate.getPeriodSettings() != null) {
            Map<String, Object> periodSettings = reportTemplate.getPeriodSettings();
            Object periodTypeObj = periodSettings.get("periodType");
            String periodType = periodTypeObj != null ? periodTypeObj.toString() : null;
            if ("Произвольный период".equals(periodType)) {
                Object startDateObj = periodSettings.get("startDate");
                Object endDateObj = periodSettings.get("endDate");
                if (startDateObj != null && endDateObj != null) {
                    java.time.LocalDate start = parseTemplateDate(startDateObj.toString());
                    java.time.LocalDate end = parseTemplateDate(endDateObj.toString());
                    if (start != null) periodStartDate = start;
                    if (end != null) periodEndDate = end;
                }
            } else if ("За 7 дней".equals(periodType)) {
                periodStartDate = today.minusDays(7);
                periodEndDate = today.minusDays(1);
            } else if ("За 24 часа".equals(periodType)) {
                periodStartDate = today.minusDays(1);
                periodEndDate = today.minusDays(1);
            }
            Object timeRangeObj = periodSettings.get("timeRange");
            if (timeRangeObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tr = (Map<String, Object>) timeRangeObj;
                Object start = tr.get("start");
                Object end = tr.get("end");
                if (start != null && !start.toString().trim().isEmpty()) periodStartTime = java.time.LocalTime.parse(start.toString());
                if (end != null && !end.toString().trim().isEmpty()) periodEndTime = java.time.LocalTime.parse(end.toString());
            } else {
                Object startTimeObj = periodSettings.get("startTime");
                Object endTimeObj = periodSettings.get("endTime");
                if (startTimeObj != null && !startTimeObj.toString().trim().isEmpty()) periodStartTime = java.time.LocalTime.parse(startTimeObj.toString());
                if (endTimeObj != null && !endTimeObj.toString().trim().isEmpty()) periodEndTime = java.time.LocalTime.parse(endTimeObj.toString());
            }
        }

        List<Integer> equipmentIdsForReport = new ArrayList<>(template.getSelectedEquipmentIds());
        Map<Integer, EquipmentWorkReportSectionDTO> sectionInfoMap = new LinkedHashMap<>();
        for (Integer mid : equipmentIdsForReport) {
            String model = "";
            String name = "";
            String department = "";
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(mid);
            if (machineOpt.isPresent()) {
                WeldingMachine m = machineOpt.get();
                name = m.getName() != null ? m.getName() : "";
                model = m.getDeviceModel() != null ? m.getDeviceModel().name() : "";
                OrganizationUnit ou = m.getOrganizationUnit();
                department = ou != null && ou.getName() != null ? ou.getName() : "";
            }
            sectionInfoMap.put(mid, new EquipmentWorkReportSectionDTO(mid, model, name, department, null));
        }

        List<EquipmentWorkReportDTO> data = reportDataService.getEquipmentWorkDataNew(
                template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);
        Map<Integer, List<EquipmentWorkReportDTO>> dataByMachine = data != null ? new LinkedHashMap<>() : new LinkedHashMap<>();
        if (data != null) {
            for (EquipmentWorkReportDTO d : data) {
                if (d.getWeldingMachineId() != null) {
                    dataByMachine.computeIfAbsent(d.getWeldingMachineId(), k -> new ArrayList<>()).add(d);
                }
            }
        }

        List<EquipmentWorkReportSectionDTO> sections = new ArrayList<>();
        for (Integer mid : equipmentIdsForReport) {
            EquipmentWorkReportSectionDTO info = sectionInfoMap.get(mid);
            List<EquipmentWorkReportDTO> rows = dataByMachine.getOrDefault(mid, Collections.emptyList());
            sections.add(new EquipmentWorkReportSectionDTO(
                    info.getWeldingMachineId(),
                    info.getEquipmentModel(),
                    info.getEquipmentName(),
                    info.getEquipmentDepartment(),
                    rows.isEmpty() ? null : rows
            ));
        }

        return reportService.generateEquipmentWorkReportMultiSection(
                sections, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);
    }

    /**
     * Генерирует отчёт "По работе сварщика (швы)" из общего шаблона.
     * Период и время берутся из periodSettings шаблона.
     */
    private byte[] generateWelderReportFromTemplate(ReportTemplateDTO reportTemplate, AutomatedReport automatedReport) throws Exception {
        WelderWorkReportTemplateDTO template = convertToWelderTemplate(reportTemplate);
        List<Long> welderIdsForReport = template.getSelectedWelderIds() != null && !template.getSelectedWelderIds().isEmpty()
                ? template.getSelectedWelderIds().stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList())
                : new ArrayList<>();
        if (welderIdsForReport.isEmpty()) {
            throw new IllegalArgumentException("Welder report template has no selected welders (selectedWelderIds)");
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate periodStartDate = today.minusDays(1);
        java.time.LocalDate periodEndDate = today.minusDays(1);
        java.time.LocalTime periodStartTime = java.time.LocalTime.MIN;
        java.time.LocalTime periodEndTime = java.time.LocalTime.MAX;

        if (reportTemplate.getPeriodSettings() != null) {
            Map<String, Object> periodSettings = reportTemplate.getPeriodSettings();
            Object periodTypeObj = periodSettings.get("periodType");
            String periodType = periodTypeObj != null ? periodTypeObj.toString() : null;
            if ("Произвольный период".equals(periodType)) {
                Object startDateObj = periodSettings.get("startDate");
                Object endDateObj = periodSettings.get("endDate");
                if (startDateObj != null && endDateObj != null) {
                    java.time.LocalDate start = parseTemplateDate(startDateObj.toString());
                    java.time.LocalDate end = parseTemplateDate(endDateObj.toString());
                    if (start != null) periodStartDate = start;
                    if (end != null) periodEndDate = end;
                }
            } else if ("За 7 дней".equals(periodType)) {
                periodStartDate = today.minusDays(7);
                periodEndDate = today.minusDays(1);
            } else if ("За 24 часа".equals(periodType)) {
                periodStartDate = today.minusDays(1);
                periodEndDate = today.minusDays(1);
            }
            Object timeRangeObj = periodSettings.get("timeRange");
            if (timeRangeObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tr = (Map<String, Object>) timeRangeObj;
                Object start = tr.get("start");
                Object end = tr.get("end");
                if (start != null && !start.toString().trim().isEmpty()) periodStartTime = java.time.LocalTime.parse(start.toString());
                if (end != null && !end.toString().trim().isEmpty()) periodEndTime = java.time.LocalTime.parse(end.toString());
            } else {
                Object startTimeObj = periodSettings.get("startTime");
                Object endTimeObj = periodSettings.get("endTime");
                if (startTimeObj != null && !startTimeObj.toString().trim().isEmpty()) periodStartTime = java.time.LocalTime.parse(startTimeObj.toString());
                if (endTimeObj != null && !endTimeObj.toString().trim().isEmpty()) periodEndTime = java.time.LocalTime.parse(endTimeObj.toString());
            }
        }

        Map<Long, WelderWorkReportSectionDTO> welderInfoMap = new LinkedHashMap<>();
        for (Long wid : welderIdsForReport) {
            String fullName = "";
            String tabNumber = "";
            String department = "";
            try {
                Optional<Welder> welderOpt = welderRepository.findById(wid);
                if (welderOpt.isPresent()) {
                    Welder w = welderOpt.get();
                    fullName = w.getName() != null ? w.getName() : "";
                    tabNumber = w.getEmployeeId() != null ? w.getEmployeeId() : "";
                    department = w.getDepartment() != null ? w.getDepartment() : "";
                } else {
                    Optional<Employee> empOpt = employeeRepository.findById(wid);
                    if (empOpt.isPresent()) {
                        Employee emp = empOpt.get();
                        fullName = emp.getFullName() != null ? emp.getFullName() : "";
                        tabNumber = "";
                        department = emp.getOrganizationUnit() != null && emp.getOrganizationUnit().getName() != null
                                ? emp.getOrganizationUnit().getName() : "";
                    }
                }
            } catch (Exception e) {
                System.err.println("AutomatedReportScheduler: Failed to load welder " + wid + ": " + e.getMessage());
            }
            welderInfoMap.put(wid, new WelderWorkReportSectionDTO(wid, fullName, tabNumber, department, null));
        }

        List<WelderWorkReportDTO> data = reportDataService.getWelderWorkDataNew(
                template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);
        Map<Long, List<WelderWorkReportDTO>> dataByWelder = new LinkedHashMap<>();
        if (data != null) {
            for (WelderWorkReportDTO d : data) {
                if (d.getWelderId() != null) {
                    dataByWelder.computeIfAbsent(d.getWelderId(), k -> new ArrayList<>()).add(d);
                }
            }
        }

        List<WelderWorkReportSectionDTO> sections = new ArrayList<>();
        for (Long wid : welderIdsForReport) {
            WelderWorkReportSectionDTO info = welderInfoMap.get(wid);
            List<WelderWorkReportDTO> rows = dataByWelder.getOrDefault(wid, Collections.emptyList());
            sections.add(new WelderWorkReportSectionDTO(
                    info.getWelderId(),
                    info.getWelderFullName(),
                    info.getWelderTabNumber(),
                    info.getWelderDepartment(),
                    rows.isEmpty() ? null : rows
            ));
        }

        return reportService.generateWelderWorkReportMultiSection(
                sections, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);
    }

    /**
     * Преобразует ReportTemplateDTO в WelderWorkReportTemplateDTO для отчёта по сварщику.
     */
    private WelderWorkReportTemplateDTO convertToWelderTemplate(ReportTemplateDTO reportTemplate) {
        WelderWorkReportTemplateDTO template = new WelderWorkReportTemplateDTO();
        template.setTemplateId(reportTemplate.getId());
        template.setTemplateName(reportTemplate.getName());
        if (reportTemplate.getSelectedWelderIds() != null && !reportTemplate.getSelectedWelderIds().isEmpty()) {
            template.setSelectedWelderIds(new ArrayList<>(reportTemplate.getSelectedWelderIds()));
        }
        if (reportTemplate.getReportParameters() != null) {
            Map<String, Object> params = reportTemplate.getReportParameters();
            template.setIncludeActualCurrentRange(Boolean.TRUE.equals(params.get("workOutsideActualCurrent")));
            if (params.get("actualCurrentMin") != null) template.setActualCurrentMin(((Number) params.get("actualCurrentMin")).intValue());
            if (params.get("actualCurrentMax") != null) template.setActualCurrentMax(((Number) params.get("actualCurrentMax")).intValue());
            if (Boolean.FALSE.equals(params.get("minSeamIntervalEnabled"))) template.setMinIntervalBetweenWeldsSec(null);
            else if (params.get("minSeamInterval") != null) template.setMinIntervalBetweenWeldsSec(((Number) params.get("minSeamInterval")).intValue());
            if (Boolean.FALSE.equals(params.get("minSeamDurationEnabled"))) template.setMinWeldDurationSec(null);
            else if (params.get("minSeamDuration") != null) template.setMinWeldDurationSec(((Number) params.get("minSeamDuration")).intValue());
            List<String> cols = new ArrayList<>();
            if (Boolean.TRUE.equals(params.get("equipmentModel"))) cols.add("equipmentModel");
            if (Boolean.TRUE.equals(params.get("equipmentName"))) cols.add("equipmentName");
            if (Boolean.TRUE.equals(params.get("wireFeedSpeed"))) cols.add("wireFeedSpeed");
            if (Boolean.TRUE.equals(params.get("consumption"))) cols.add("consumption");
            if (Boolean.TRUE.equals(params.get("energyConsumed"))) cols.add("energyConsumed");
            if (Boolean.TRUE.equals(params.get("gasConsumption"))) cols.add("gasConsumption");
            template.setSelectedColumns(cols);
        }
        if (reportTemplate.getCurrentRanges() != null && reportTemplate.getCurrentRanges().get("workOutsideActualCurrent") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> actual = (Map<String, Object>) reportTemplate.getCurrentRanges().get("workOutsideActualCurrent");
            if (actual != null) {
                if (actual.get("min") != null) template.setActualCurrentMin(((Number) actual.get("min")).intValue());
                if (actual.get("max") != null) template.setActualCurrentMax(((Number) actual.get("max")).intValue());
            }
        }
        return template;
    }

    /**
     * Преобразует ReportTemplateDTO в EquipmentWorkReportTemplateDTO для отчёта по оборудованию.
     */
    private EquipmentWorkReportTemplateDTO convertToEquipmentTemplate(ReportTemplateDTO reportTemplate) {
        EquipmentWorkReportTemplateDTO template = new EquipmentWorkReportTemplateDTO();
        template.setTemplateId(reportTemplate.getId());
        template.setTemplateName(reportTemplate.getName());
        if (reportTemplate.getReportParameters() != null) {
            Map<String, Object> params = reportTemplate.getReportParameters();
            @SuppressWarnings("unchecked")
            List<Number> ids = params.containsKey("selectedEquipmentIds") ? (List<Number>) params.get("selectedEquipmentIds") : null;
            if (ids != null && !ids.isEmpty()) {
                List<Integer> equipmentIds = new ArrayList<>();
                for (Number n : ids) equipmentIds.add(n.intValue());
                template.setSelectedEquipmentIds(equipmentIds);
            }
            template.setIncludeActualCurrentRange(Boolean.TRUE.equals(params.get("workOutsideActualCurrent")));
            if (params.get("actualCurrentMin") != null) template.setActualCurrentMin(((Number) params.get("actualCurrentMin")).intValue());
            if (params.get("actualCurrentMax") != null) template.setActualCurrentMax(((Number) params.get("actualCurrentMax")).intValue());
            if (Boolean.FALSE.equals(params.get("minSeamIntervalEnabled"))) template.setMinIntervalBetweenWeldsSec(null);
            else if (params.get("minSeamInterval") != null) template.setMinIntervalBetweenWeldsSec(((Number) params.get("minSeamInterval")).intValue());
            if (Boolean.FALSE.equals(params.get("minSeamDurationEnabled"))) template.setMinWeldDurationSec(null);
            else if (params.get("minSeamDuration") != null) template.setMinWeldDurationSec(((Number) params.get("minSeamDuration")).intValue());
            List<String> cols = new ArrayList<>();
            if (Boolean.TRUE.equals(params.get("welderFullName"))) cols.add("welderFullName");
            if (Boolean.TRUE.equals(params.get("welderTabNumber"))) cols.add("welderTabNumber");
            if (Boolean.TRUE.equals(params.get("profession"))) cols.add("profession");
            if (Boolean.TRUE.equals(params.get("wireFeedSpeed"))) cols.add("wireFeedSpeed");
            if (Boolean.TRUE.equals(params.get("consumption"))) cols.add("consumption");
            if (Boolean.TRUE.equals(params.get("energyConsumed"))) cols.add("energyConsumed");
            if (Boolean.TRUE.equals(params.get("gasConsumption"))) cols.add("gasConsumption");
            template.setSelectedColumns(cols);
        }
        if (reportTemplate.getCurrentRanges() != null && reportTemplate.getCurrentRanges().get("workOutsideActualCurrent") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> actual = (Map<String, Object>) reportTemplate.getCurrentRanges().get("workOutsideActualCurrent");
            if (actual != null) {
                if (actual.get("min") != null) template.setActualCurrentMin(((Number) actual.get("min")).intValue());
                if (actual.get("max") != null) template.setActualCurrentMax(((Number) actual.get("max")).intValue());
            }
        }
        return template;
    }

    /**
     * Парсит дату из строки, пришедшей с фронта.
     * Если строка в формате ISO с временем (например 2025-11-02T21:00:00.000Z — полночь по Москве 3 ноября),
     * интерпретирует её в зоне Europe/Moscow, чтобы календарная дата совпадала с выбором пользователя.
     */
    private static java.time.LocalDate parseTemplateDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            if (dateStr.contains("T")) {
                java.time.Instant instant = java.time.Instant.parse(dateStr);
                return instant.atZone(java.time.ZoneId.of("Europe/Moscow")).toLocalDate();
            }
            return java.time.LocalDate.parse(dateStr);
        } catch (Exception e) {
            try {
                return java.time.LocalDate.parse(dateStr.substring(0, dateStr.indexOf("T")));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Извлекает период (даты и время) из настроек шаблона.
     * Возвращает массив: [periodStartDate, periodEndDate, periodStartTime, periodEndTime].
     */
    private Object[] parsePeriodFromTemplate(ReportTemplateDTO reportTemplate) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate periodStartDate = today.minusDays(1);
        java.time.LocalDate periodEndDate = today.minusDays(1);
        java.time.LocalTime periodStartTime = java.time.LocalTime.MIN;
        java.time.LocalTime periodEndTime = java.time.LocalTime.MAX;
        if (reportTemplate.getPeriodSettings() != null) {
            Map<String, Object> ps = reportTemplate.getPeriodSettings();
            Object periodTypeObj = ps.get("periodType");
            String periodType = periodTypeObj != null ? periodTypeObj.toString() : null;
            if ("Произвольный период".equals(periodType)) {
                Object startDateObj = ps.get("startDate");
                Object endDateObj = ps.get("endDate");
                if (startDateObj != null && endDateObj != null) {
                    java.time.LocalDate start = parseTemplateDate(startDateObj.toString());
                    java.time.LocalDate end = parseTemplateDate(endDateObj.toString());
                    if (start != null) periodStartDate = start;
                    if (end != null) periodEndDate = end;
                }
            } else if ("За 7 дней".equals(periodType)) {
                periodStartDate = today.minusDays(7);
                periodEndDate = today.minusDays(1);
            } else if ("За 24 часа".equals(periodType)) {
                periodStartDate = today.minusDays(1);
                periodEndDate = today.minusDays(1);
            }
            Object timeRangeObj = ps.get("timeRange");
            if (timeRangeObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tr = (Map<String, Object>) timeRangeObj;
                Object start = tr.get("start");
                Object end = tr.get("end");
                if (start != null && !start.toString().trim().isEmpty()) periodStartTime = java.time.LocalTime.parse(start.toString());
                if (end != null && !end.toString().trim().isEmpty()) periodEndTime = java.time.LocalTime.parse(end.toString());
            } else {
                Object startTimeObj = ps.get("startTime");
                Object endTimeObj = ps.get("endTime");
                if (startTimeObj != null && !startTimeObj.toString().trim().isEmpty()) periodStartTime = java.time.LocalTime.parse(startTimeObj.toString());
                if (endTimeObj != null && !endTimeObj.toString().trim().isEmpty()) periodEndTime = java.time.LocalTime.parse(endTimeObj.toString());
            }
        }
        return new Object[]{ periodStartDate, periodEndDate, periodStartTime, periodEndTime };
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

        // Если есть templateId, используем шаблон и период из шаблона
        if (automatedReport.getTemplateId() != null) {
            try {
                Optional<ReportTemplateDTO> templateOpt = reportTemplateService.getTemplateById(automatedReport.getTemplateId());
                if (templateOpt.isPresent()) {
                    ReportTemplateDTO reportTemplate = templateOpt.get();
                    String tt = normalizeTemplateTypeFromTemplate(reportTemplate, templateType.trim().toLowerCase());
                    if ("equipment".equals(tt)) {
                        EquipmentWorkReportTemplateDTO equipmentTemplate = convertToEquipmentTemplate(reportTemplate);
                        if (equipmentTemplate.getSelectedEquipmentIds() != null && !equipmentTemplate.getSelectedEquipmentIds().isEmpty()) {
                            Object[] period = parsePeriodFromTemplate(reportTemplate);
                            return reportDataService.getEquipmentWorkDataNew(equipmentTemplate,
                                    (java.time.LocalDate) period[0], (java.time.LocalDate) period[1],
                                    (java.time.LocalTime) period[2], (java.time.LocalTime) period[3]);
                        }
                    }
                    if ("welder".equals(tt)) {
                        WelderWorkReportTemplateDTO welderTemplate = convertToWelderTemplate(reportTemplate);
                        if (welderTemplate.getSelectedWelderIds() != null && !welderTemplate.getSelectedWelderIds().isEmpty()) {
                            Object[] period = parsePeriodFromTemplate(reportTemplate);
                            return reportDataService.getWelderWorkDataNew(welderTemplate,
                                    (java.time.LocalDate) period[0], (java.time.LocalDate) period[1],
                                    (java.time.LocalTime) period[2], (java.time.LocalTime) period[3]);
                        }
                    }
                    if ("equipment-malfunction".equals(tt)) {
                        EquipmentMalfunctionReportTemplateDTO malfunctionTemplate = convertToMalfunctionTemplate(reportTemplate);
                        if (malfunctionTemplate.getSelectedEquipmentIds() != null && !malfunctionTemplate.getSelectedEquipmentIds().isEmpty()) {
                            Object[] period = resolveEquipmentMalfunctionPeriod(reportTemplate);
                            java.time.LocalDate ps = (java.time.LocalDate) period[0];
                            java.time.LocalDate pe = (java.time.LocalDate) period[1];
                            java.time.LocalTime pt1 = (java.time.LocalTime) period[2];
                            java.time.LocalTime pt2 = (java.time.LocalTime) period[3];
                            java.time.LocalDateTime now = java.time.LocalDateTime.now();
                            java.time.LocalDateTime endDt = java.time.LocalDateTime.of(pe, pt2);
                            if (endDt.isAfter(now)) {
                                pe = now.toLocalDate();
                                pt2 = now.toLocalTime();
                            }
                            return reportDataService.getEquipmentMalfunctionData(malfunctionTemplate, ps, pe, pt1, pt2);
                        }
                    }
                    // wire-consumption
                    WireConsumptionReportTemplateDTO wireTemplate = convertToWireTemplate(reportTemplate);
                    Object[] period = parsePeriodFromTemplate(reportTemplate);
                    return reportDataService.getWireConsumptionDataNew(wireTemplate,
                            (java.time.LocalDate) period[0], (java.time.LocalDate) period[1],
                            (java.time.LocalTime) period[2], (java.time.LocalTime) period[3]);
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
        String ttResolved = resolveTemplateTypeForFileFormat(automatedReport);
        String extension = ("wire-consumption".equals(ttResolved) || "equipment".equals(ttResolved)
                || "welder".equals(ttResolved) || "equipment-malfunction".equals(ttResolved))
                ? "xlsx" : "pdf";
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
