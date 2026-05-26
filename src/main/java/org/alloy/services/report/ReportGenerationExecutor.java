package org.alloy.services.report;

import org.alloy.models.dto.*;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.repositories.EmployeeRepository;
import org.alloy.repositories.WelderRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReportGenerationExecutor {

    @Autowired private ReportService reportService;
    @Autowired private ReportDataService reportDataService;
    @Autowired private WireConsumptionReportTemplateService templateService;
    @Autowired private WelderWorkReportTemplateService welderWorkTemplateService;
    @Autowired private EquipmentWorkReportTemplateService equipmentWorkTemplateService;
    @Autowired private ReportTemplateService reportTemplateService;
    @Autowired private WeldingMachineRepository weldingMachineRepository;
    @Autowired private WelderRepository welderRepository;
    @Autowired private EmployeeRepository employeeRepository;

    private static void progress(int percent, String message) {
        ReportGenerationProgressContext.update(percent, message);
    }

    private WireConsumptionReportTemplateDTO convertToWireTemplate(ReportTemplateDTO reportTemplate) {
        WireConsumptionReportTemplateDTO wireTemplate = new WireConsumptionReportTemplateDTO();

        wireTemplate.setTemplateId(reportTemplate.getId());
        wireTemplate.setTemplateName(reportTemplate.getName());
        wireTemplate.setSelectedOrganizationUnitIds(reportTemplate.getSelectedOrganizationUnitIds());
        wireTemplate.setSelectedWelderIds(reportTemplate.getSelectedWelderIds());
        wireTemplate.setSelectedEquipmentModels(reportTemplate.getSelectedEquipmentModels());

        System.out.println("[REPORT-CONTROLLER] 📋 Преобразование шаблона: ID=" + reportTemplate.getId() +
                ", название='" + reportTemplate.getName() + "'");
        System.out.println("[REPORT-CONTROLLER] 📋 Выбранные модели оборудования: " +
                (reportTemplate.getSelectedEquipmentModels() != null ? reportTemplate.getSelectedEquipmentModels() : "null"));
        System.out.println("[REPORT-CONTROLLER] 📋 Выбранные ID сварщиков: " +
                (reportTemplate.getSelectedWelderIds() != null ? reportTemplate.getSelectedWelderIds() : "null"));
        System.out.println("[REPORT-CONTROLLER] 📋 Выбранные ID подразделений: " +
                (reportTemplate.getSelectedOrganizationUnitIds() != null ? reportTemplate.getSelectedOrganizationUnitIds() : "null"));

        // Преобразуем диапазоны токов
        if (reportTemplate.getCurrentRanges() != null) {
            Map<String, Object> currentRanges = reportTemplate.getCurrentRanges();
            if (currentRanges.get("workOutsideSetCurrent") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> setCurrent = (Map<String, Object>) currentRanges.get("workOutsideSetCurrent");
                if (setCurrent != null && setCurrent.get("min") != null) {
                    wireTemplate.setSetCurrentMin(((Number) setCurrent.get("min")).intValue());
                }
                if (setCurrent != null && setCurrent.get("max") != null) {
                    wireTemplate.setSetCurrentMax(((Number) setCurrent.get("max")).intValue());
                }
            }
            if (currentRanges.get("workOutsideActualCurrent") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> actualCurrent = (Map<String, Object>) currentRanges.get("workOutsideActualCurrent");
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
            Map<String, Object> params = reportTemplate.getReportParameters();
            List<String> selectedColumns = new java.util.ArrayList<>();

            // Добавляем колонки, которые выбраны (значение true)
            if (Boolean.TRUE.equals(params.get("equipmentModel"))) selectedColumns.add("equipmentModel");
            if (Boolean.TRUE.equals(params.get("tableNumber"))) selectedColumns.add("tableNumber");
            if (Boolean.TRUE.equals(params.get("profession"))) selectedColumns.add("profession");
            if (Boolean.TRUE.equals(params.get("department"))) selectedColumns.add("department");
            if (Boolean.TRUE.equals(params.get("equipmentName"))) selectedColumns.add("equipmentName");
            if (Boolean.TRUE.equals(params.get("timeOnline"))) selectedColumns.add("timeOnline");
            if (Boolean.TRUE.equals(params.get("arcBurningTime"))) selectedColumns.add("arcBurningTime");
            if (Boolean.TRUE.equals(params.get("efficiency"))) selectedColumns.add("efficiency");
            if (Boolean.TRUE.equals(params.get("energyConsumed"))) selectedColumns.add("energyConsumed");

            // Проверяем, выбраны ли чекбоксы для работы вне диапазона токов
            // Добавляем колонки только если соответствующие чекбоксы выбраны
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
            Map<String, Object> periodSettings = reportTemplate.getPeriodSettings();
            if (periodSettings.get("selectedDays") != null) {
                @SuppressWarnings("unchecked")
                List<String> selectedDays = (List<String>) periodSettings.get("selectedDays");
                wireTemplate.setSelectedDays(selectedDays);
                System.out.println("[REPORT-CONTROLLER] 📅 Выбранные дни недели: " + selectedDays);
            } else {
                System.out.println("[REPORT-CONTROLLER] ⚠️ selectedDays не найдены в periodSettings");
            }
        } else {
            System.out.println("[REPORT-CONTROLLER] ⚠️ periodSettings = null");
        }

        return wireTemplate;
    }

    public ReportFileResult buildWireConsumptionReport(WireConsumptionReportGenerationDTO generationRequest) {
        try {
            progress(5, "Подготовка параметров отчёта…");
// Получаем шаблон
            WireConsumptionReportTemplateDTO template = new WireConsumptionReportTemplateDTO();
            if (generationRequest.getTemplateId() != null) {
                // Сначала пробуем получить из WireConsumptionReportTemplate
                Optional<WireConsumptionReportTemplateDTO> templateOpt =
                        templateService.getTemplateById(generationRequest.getTemplateId());
                if (templateOpt.isPresent()) {
                    template = templateOpt.get();
                } else {
                    // Если не найден в WireConsumptionReportTemplate, пробуем получить из общего ReportTemplate
                    Optional<ReportTemplateDTO> generalTemplateOpt =
                            reportTemplateService.getTemplateById(generationRequest.getTemplateId());
                    if (generalTemplateOpt.isPresent()) {
                        ReportTemplateDTO generalTemplate = generalTemplateOpt.get();
                        // Преобразуем общий шаблон в WireConsumptionReportTemplateDTO
                        template = convertToWireTemplate(generalTemplate);
                    }
                }
            }

            // Период: подставляем время по умолчанию (00:00 и 23:59), если не задано
            LocalDate periodStartDate = generationRequest.getPeriodStartDate();
            LocalDate periodEndDate = generationRequest.getPeriodEndDate();
            LocalTime periodStartTime = generationRequest.getPeriodStartTime() != null
                    ? generationRequest.getPeriodStartTime() : LocalTime.MIN; // 00:00
            LocalTime periodEndTime = generationRequest.getPeriodEndTime() != null
                    ? generationRequest.getPeriodEndTime() : LocalTime.of(23, 59, 59);

            // Если конец периода ещё не наступил — принимаем за конец текущий момент и пишем его в отчёт
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDateTime = periodEndDate != null
                    ? LocalDateTime.of(periodEndDate, periodEndTime) : null;
            if (endDateTime != null && endDateTime.isAfter(now)) {
                periodEndDate = now.toLocalDate();
                periodEndTime = now.toLocalTime();
            }

            // Получаем данные отчета
            progress(20, "Загрузка данных из базы…");
            List<WireConsumptionReportDTO> data = reportDataService.getWireConsumptionDataNew(
                    template, periodStartDate, periodEndDate, periodStartTime, periodEndTime
            );

            // Генерируем Excel отчёт (в заголовке будут дата и время)
            progress(80, "Формирование файла Excel…");
            byte[] reportBytes = reportService.generateWireConsumptionReportNew(
                    data, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            String filename = "wire_consumption_report_" + System.currentTimeMillis() + ".xlsx";

            progress(100, "Готово");
            return new ReportFileResult(filename, reportBytes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ReportFileResult buildWelderWorkReport(WelderWorkReportGenerationDTO generationRequest) {
        try {
            progress(5, "Подготовка параметров отчёта…");
            WelderWorkReportTemplateDTO template = new WelderWorkReportTemplateDTO();
            ReportTemplateDTO templateWithPeriodSettings = null; // для учёта periodSettings при «За 24 часа»
            if (generationRequest.getTemplateId() != null) {
                // Сначала пробуем получить из WelderWorkReportTemplate
                Optional<WelderWorkReportTemplateDTO> templateOpt =
                        welderWorkTemplateService.getTemplateById(generationRequest.getTemplateId());
                if (templateOpt.isPresent()) {
                    template = templateOpt.get();
                    if (generationRequest.getSelectedColumns() != null && !generationRequest.getSelectedColumns().isEmpty()) {
                        template.setSelectedColumns(generationRequest.getSelectedColumns());
                    }
                } else {
                    // Если не найден в WelderWorkReportTemplate, пробуем получить из общего ReportTemplate
                    Optional<ReportTemplateDTO> generalTemplateOpt =
                            reportTemplateService.getTemplateById(generationRequest.getTemplateId());
                    if (generalTemplateOpt.isPresent()) {
                        ReportTemplateDTO generalTemplate = generalTemplateOpt.get();
                        templateWithPeriodSettings = generalTemplate;
                        // Преобразуем общий шаблон в WelderWorkReportTemplateDTO
                        template.setTemplateId(generalTemplate.getId());
                        template.setTemplateName(generalTemplate.getName());
                        template.setSelectedWelderIds(generalTemplate.getSelectedWelderIds());
                        // Параметры из reportParameters (фронт отправляет: workOutsideActualCurrent, minSeamInterval, minSeamDuration, minSeamIntervalEnabled, minSeamDurationEnabled)
                        if (generalTemplate.getReportParameters() != null) {
                            Map<String, Object> params = generalTemplate.getReportParameters();
                            if (params.containsKey("workOutsideActualCurrent")) {
                                template.setIncludeActualCurrentRange(Boolean.TRUE.equals(params.get("workOutsideActualCurrent")));
                            } else if (params.containsKey("includeActualCurrentRange")) {
                                template.setIncludeActualCurrentRange(Boolean.TRUE.equals(params.get("includeActualCurrentRange")));
                            }
                            if (params.containsKey("minSeamInterval") && !Boolean.FALSE.equals(params.get("minSeamIntervalEnabled"))) {
                                template.setMinIntervalBetweenWeldsSec(((Number) params.get("minSeamInterval")).intValue());
                            } else if (params.containsKey("minIntervalBetweenWeldsSec")) {
                                template.setMinIntervalBetweenWeldsSec(((Number) params.get("minIntervalBetweenWeldsSec")).intValue());
                            }
                            if (params.containsKey("minSeamDuration") && !Boolean.FALSE.equals(params.get("minSeamDurationEnabled"))) {
                                template.setMinWeldDurationSec(((Number) params.get("minSeamDuration")).intValue());
                            } else if (params.containsKey("minWeldDurationSec")) {
                                template.setMinWeldDurationSec(((Number) params.get("minWeldDurationSec")).intValue());
                            }
                        }
                        // Диапазон фактического тока из currentRanges (фронт: currentRanges.workOutsideActualCurrent.min/max)
                        if (generalTemplate.getCurrentRanges() != null && generalTemplate.getCurrentRanges().get("workOutsideActualCurrent") != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> actualRange = (Map<String, Object>) generalTemplate.getCurrentRanges().get("workOutsideActualCurrent");
                            if (actualRange != null) {
                                if (actualRange.get("min") != null) {
                                    template.setActualCurrentMin(((Number) actualRange.get("min")).intValue());
                                }
                                if (actualRange.get("max") != null) {
                                    template.setActualCurrentMax(((Number) actualRange.get("max")).intValue());
                                }
                            }
                        }
                        if (generalTemplate.getReportParameters() != null) {
                            Map<String, Object> params = generalTemplate.getReportParameters();
                            if (template.getActualCurrentMin() == null && params.containsKey("actualCurrentMin")) {
                                template.setActualCurrentMin(((Number) params.get("actualCurrentMin")).intValue());
                            }
                            if (template.getActualCurrentMax() == null && params.containsKey("actualCurrentMax")) {
                                template.setActualCurrentMax(((Number) params.get("actualCurrentMax")).intValue());
                            }
                            // Выбранные колонки для отчёта по работе сварщика (опциональные)
                            List<String> selectedCols = new java.util.ArrayList<>();
                            if (Boolean.TRUE.equals(params.get("equipmentModel"))) selectedCols.add("equipmentModel");
                            if (Boolean.TRUE.equals(params.get("equipmentName"))) selectedCols.add("equipmentName");
                            if (Boolean.TRUE.equals(params.get("wireFeedSpeed"))) selectedCols.add("wireFeedSpeed");
                            if (Boolean.TRUE.equals(params.get("consumption"))) selectedCols.add("consumption");
                            if (Boolean.TRUE.equals(params.get("energyConsumed"))) selectedCols.add("energyConsumed");
                            if (Boolean.TRUE.equals(params.get("gasConsumption"))) selectedCols.add("gasConsumption");
                            template.setSelectedColumns(selectedCols);
                        }
                    }
                    // При генерации приоритет у выбранных колонок из запроса (текущие галочки на форме)
                    if (generationRequest.getSelectedColumns() != null && !generationRequest.getSelectedColumns().isEmpty()) {
                        template.setSelectedColumns(generationRequest.getSelectedColumns());
                    }
                }
            }
            // Если шаблон не загружали по templateId — выбранные колонки только из запроса
            if (generationRequest.getSelectedColumns() != null && !generationRequest.getSelectedColumns().isEmpty()) {
                template.setSelectedColumns(generationRequest.getSelectedColumns());
            }
            if (Boolean.FALSE.equals(generationRequest.getMinSeamIntervalEnabled())) template.setMinIntervalBetweenWeldsSec(null);
            else if (generationRequest.getMinSeamInterval() != null) template.setMinIntervalBetweenWeldsSec(generationRequest.getMinSeamInterval());
            if (Boolean.FALSE.equals(generationRequest.getMinSeamDurationEnabled())) template.setMinWeldDurationSec(null);
            else if (generationRequest.getMinSeamDuration() != null) template.setMinWeldDurationSec(generationRequest.getMinSeamDuration());

            // Список сварщиков для отчёта: несколько выбранных или один по умолчанию
            List<Long> welderIdsForReport = template.getSelectedWelderIds() != null && !template.getSelectedWelderIds().isEmpty()
                    ? template.getSelectedWelderIds().stream()
                    .map(id -> Long.valueOf(id.intValue()))
                    .collect(java.util.stream.Collectors.toList())
                    : (template.getWelderId() != null ? java.util.Collections.singletonList(template.getWelderId()) : java.util.Collections.emptyList());

            if (welderIdsForReport.isEmpty()) {
                throw new IllegalArgumentException("Некорректные параметры отчёта");
            }

            // Заполняем данные по каждому сварщику для шапок (ФИО, таб. №, подразделение)
            java.util.Map<Long, org.alloy.models.dto.WelderWorkReportSectionDTO> welderInfoMap = new java.util.LinkedHashMap<>();
            for (Long wid : welderIdsForReport) {
                String fullName = "";
                String tabNumber = "";
                String department = "";
                try {
                    Optional<org.alloy.models.entities.Welder> welderOpt = welderRepository.findById(wid);
                    if (welderOpt.isPresent()) {
                        var w = welderOpt.get();
                        fullName = w.getName() != null ? w.getName() : "";
                        tabNumber = w.getEmployeeId() != null ? w.getEmployeeId() : "";
                        department = w.getDepartment() != null ? w.getDepartment() : "";
                    } else {
                        Optional<org.alloy.models.entities.Employee> empOpt = employeeRepository.findById(wid);
                        if (empOpt.isPresent()) {
                            var emp = empOpt.get();
                            fullName = emp.getFullName() != null ? emp.getFullName() : "";
                            tabNumber = "";
                            department = emp.getOrganizationUnit() != null && emp.getOrganizationUnit().getName() != null
                                    ? emp.getOrganizationUnit().getName() : "";
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Не удалось загрузить данные сварщика " + wid + ": " + e.getMessage());
                }
                welderInfoMap.put(wid, new org.alloy.models.dto.WelderWorkReportSectionDTO(wid, fullName, tabNumber, department, null));
            }

            // Период: из запроса; если в шаблоне (ReportTemplate) periodType «За 24 часа» — считаем на сервере
            java.time.LocalDate periodStartDate = generationRequest.getPeriodStartDate();
            java.time.LocalDate periodEndDate = generationRequest.getPeriodEndDate();
            java.time.LocalTime periodStartTime = generationRequest.getPeriodStartTime();
            java.time.LocalTime periodEndTime = generationRequest.getPeriodEndTime();
            if (templateWithPeriodSettings != null && templateWithPeriodSettings.getPeriodSettings() != null) {
                Object pt = templateWithPeriodSettings.getPeriodSettings().get("periodType");
                String periodType = pt != null ? pt.toString().trim() : "";
                if ("За 24 часа".equals(periodType) || "LAST_24_HOURS".equalsIgnoreCase(periodType) || "24h".equalsIgnoreCase(periodType)) {
                    java.time.LocalDateTime end = java.time.LocalDateTime.now();
                    java.time.LocalDateTime start = end.minusHours(24);
                    periodStartDate = start.toLocalDate();
                    periodEndDate = end.toLocalDate();
                    periodStartTime = start.toLocalTime();
                    periodEndTime = end.toLocalTime();
                } else if ("За 7 дней".equals(periodType) || "LAST_7_DAYS".equalsIgnoreCase(periodType) || "7DAYS".equalsIgnoreCase(periodType)) {
                    java.time.LocalDateTime end = java.time.LocalDateTime.now();
                    java.time.LocalDateTime start = end.minusDays(7);
                    periodStartDate = start.toLocalDate();
                    periodEndDate = end.toLocalDate();
                    periodStartTime = start.toLocalTime();
                    periodEndTime = end.toLocalTime();
                }
            }

            progress(25, "Загрузка данных из базы…");
            List<WelderWorkReportDTO> data = reportDataService.getWelderWorkDataNew(
                    template,
                    periodStartDate,
                    periodEndDate,
                    periodStartTime,
                    periodEndTime
            );

            java.util.Map<Long, List<WelderWorkReportDTO>> dataByWelder = data != null ? data.stream()
                    .filter(d -> d.getWelderId() != null)
                    .collect(java.util.stream.Collectors.groupingBy(WelderWorkReportDTO::getWelderId))
                    : new java.util.HashMap<>();

            List<org.alloy.models.dto.WelderWorkReportSectionDTO> sections = new java.util.ArrayList<>();
            for (Long wid : welderIdsForReport) {
                org.alloy.models.dto.WelderWorkReportSectionDTO info = welderInfoMap.get(wid);
                List<WelderWorkReportDTO> rows = dataByWelder.getOrDefault(wid, java.util.Collections.emptyList());
                sections.add(new org.alloy.models.dto.WelderWorkReportSectionDTO(
                        info.getWelderId(),
                        info.getWelderFullName(),
                        info.getWelderTabNumber(),
                        info.getWelderDepartment(),
                        rows.isEmpty() ? null : rows
                ));
            }

            progress(80, "Формирование файла Excel…");
            byte[] reportBytes = reportService.generateWelderWorkReportMultiSection(
                    sections,
                    template,
                    periodStartDate,
                    periodEndDate,
                    periodStartTime,
                    periodEndTime
            );

            String filename = "welder_work_report_" + System.currentTimeMillis() + ".xlsx";

            progress(100, "Готово");
            return new ReportFileResult(filename, reportBytes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ReportFileResult buildEquipmentWorkReport(EquipmentWorkReportGenerationDTO generationRequest) {
        try {
            progress(5, "Подготовка параметров отчёта…");
            EquipmentWorkReportTemplateDTO template = new EquipmentWorkReportTemplateDTO();
            ReportTemplateDTO templateWithPeriodSettings = null;
            if (generationRequest.getTemplateId() != null) {
                Optional<EquipmentWorkReportTemplateDTO> templateOpt =
                        equipmentWorkTemplateService.getTemplateByIdForReport(generationRequest.getTemplateId());
                if (templateOpt.isPresent()) {
                    template = templateOpt.get();
                    if (generationRequest.getSelectedColumns() != null && !generationRequest.getSelectedColumns().isEmpty()) {
                        template.setSelectedColumns(generationRequest.getSelectedColumns());
                    }
                } else {
                    Optional<ReportTemplateDTO> generalOpt = reportTemplateService.getTemplateById(generationRequest.getTemplateId());
                    if (generalOpt.isPresent()) {
                        ReportTemplateDTO general = generalOpt.get();
                        templateWithPeriodSettings = general;
                        template.setTemplateId(general.getId());
                        template.setTemplateName(general.getName());
                        @SuppressWarnings("unchecked")
                        List<Number> ids = general.getReportParameters() != null && general.getReportParameters().containsKey("selectedEquipmentIds")
                                ? (List<Number>) general.getReportParameters().get("selectedEquipmentIds") : null;
                        if (ids != null && !ids.isEmpty()) {
                            template.setSelectedEquipmentIds(ids.stream().map(Number::intValue).collect(java.util.stream.Collectors.toList()));
                        }
                        if (generationRequest.getSelectedEquipmentIds() != null && !generationRequest.getSelectedEquipmentIds().isEmpty()) {
                            template.setSelectedEquipmentIds(generationRequest.getSelectedEquipmentIds());
                        }
                        if (general.getReportParameters() != null) {
                            Map<String, Object> params = general.getReportParameters();
                            if (Boolean.TRUE.equals(params.get("workOutsideActualCurrent"))) template.setIncludeActualCurrentRange(true);
                            // Как в generateWelderWorkReport: не подставлять пороги, если в шаблоне они выключены (иначе min=10 с «съедает» все короткие швы).
                            if (params.containsKey("minSeamInterval") && !Boolean.FALSE.equals(params.get("minSeamIntervalEnabled"))) {
                                template.setMinIntervalBetweenWeldsSec(((Number) params.get("minSeamInterval")).intValue());
                            } else if (params.containsKey("minIntervalBetweenWeldsSec")) {
                                template.setMinIntervalBetweenWeldsSec(((Number) params.get("minIntervalBetweenWeldsSec")).intValue());
                            }
                            if (params.containsKey("minSeamDuration") && !Boolean.FALSE.equals(params.get("minSeamDurationEnabled"))) {
                                template.setMinWeldDurationSec(((Number) params.get("minSeamDuration")).intValue());
                            } else if (params.containsKey("minWeldDurationSec")) {
                                template.setMinWeldDurationSec(((Number) params.get("minWeldDurationSec")).intValue());
                            }
                            if (params.get("actualCurrentMin") != null) template.setActualCurrentMin(((Number) params.get("actualCurrentMin")).intValue());
                            if (params.get("actualCurrentMax") != null) template.setActualCurrentMax(((Number) params.get("actualCurrentMax")).intValue());
                            List<String> cols = new java.util.ArrayList<>();
                            if (Boolean.TRUE.equals(params.get("welderFullName"))) cols.add("welderFullName");
                            if (Boolean.TRUE.equals(params.get("welderTabNumber"))) cols.add("welderTabNumber");
                            if (Boolean.TRUE.equals(params.get("profession"))) cols.add("profession");
                            if (Boolean.TRUE.equals(params.get("wireFeedSpeed"))) cols.add("wireFeedSpeed");
                            if (Boolean.TRUE.equals(params.get("consumption"))) cols.add("consumption");
                            if (Boolean.TRUE.equals(params.get("energyConsumed"))) cols.add("energyConsumed");
                            if (Boolean.TRUE.equals(params.get("gasConsumption"))) cols.add("gasConsumption");
                            if (!cols.isEmpty()) template.setSelectedColumns(cols);
                        }
                        if (general.getCurrentRanges() != null && general.getCurrentRanges().get("workOutsideActualCurrent") != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> actual = (Map<String, Object>) general.getCurrentRanges().get("workOutsideActualCurrent");
                            if (actual != null) {
                                if (actual.get("min") != null) template.setActualCurrentMin(((Number) actual.get("min")).intValue());
                                if (actual.get("max") != null) template.setActualCurrentMax(((Number) actual.get("max")).intValue());
                            }
                        }
                    }
                }
                if (generationRequest.getSelectedColumns() != null && !generationRequest.getSelectedColumns().isEmpty()) {
                    template.setSelectedColumns(generationRequest.getSelectedColumns());
                }
            }
            if (generationRequest.getSelectedEquipmentIds() != null && !generationRequest.getSelectedEquipmentIds().isEmpty()) {
                template.setSelectedEquipmentIds(generationRequest.getSelectedEquipmentIds());
            }
            if (Boolean.FALSE.equals(generationRequest.getMinSeamIntervalEnabled())) template.setMinIntervalBetweenWeldsSec(null);
            else if (generationRequest.getMinSeamInterval() != null) template.setMinIntervalBetweenWeldsSec(generationRequest.getMinSeamInterval());
            if (Boolean.FALSE.equals(generationRequest.getMinSeamDurationEnabled())) template.setMinWeldDurationSec(null);
            else if (generationRequest.getMinSeamDuration() != null) template.setMinWeldDurationSec(generationRequest.getMinSeamDuration());

            List<Integer> equipmentIdsForReport = template.getSelectedEquipmentIds() != null && !template.getSelectedEquipmentIds().isEmpty()
                    ? new java.util.ArrayList<>(template.getSelectedEquipmentIds())
                    : new java.util.ArrayList<>();

            if (equipmentIdsForReport.isEmpty()) {
                throw new IllegalArgumentException("Некорректные параметры отчёта");
            }

            java.util.Map<Integer, EquipmentWorkReportSectionDTO> sectionInfoMap = new java.util.LinkedHashMap<>();
            for (Integer mid : equipmentIdsForReport) {
                String model = "";
                String name = "";
                String department = "";
                try {
                    Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(mid);
                    if (machineOpt.isPresent()) {
                        WeldingMachine m = machineOpt.get();
                        name = m.getName() != null ? m.getName() : "";
                        model = m.getDeviceModel() != null ? m.getDeviceModel().name() : "";
                        OrganizationUnit ou = m.getOrganizationUnit();
                        department = ou != null && ou.getName() != null ? ou.getName() : "";
                    }
                } catch (Exception e) {
                    System.err.println("Не удалось загрузить данные аппарата " + mid + ": " + e.getMessage());
                }
                sectionInfoMap.put(mid, new EquipmentWorkReportSectionDTO(mid, model, name, department, null));
            }

            java.time.LocalDate periodStartDate = generationRequest.getPeriodStartDate();
            java.time.LocalDate periodEndDate = generationRequest.getPeriodEndDate();
            java.time.LocalTime periodStartTime = generationRequest.getPeriodStartTime();
            java.time.LocalTime periodEndTime = generationRequest.getPeriodEndTime();
            if (templateWithPeriodSettings != null && templateWithPeriodSettings.getPeriodSettings() != null) {
                Object pt = templateWithPeriodSettings.getPeriodSettings().get("periodType");
                String periodType = pt != null ? pt.toString().trim() : "";
                if ("За 24 часа".equals(periodType) || "LAST_24_HOURS".equalsIgnoreCase(periodType)) {
                    java.time.LocalDateTime end = java.time.LocalDateTime.now();
                    java.time.LocalDateTime start = end.minusHours(24);
                    periodStartDate = start.toLocalDate();
                    periodEndDate = end.toLocalDate();
                    periodStartTime = start.toLocalTime();
                    periodEndTime = end.toLocalTime();
                } else if ("За 7 дней".equals(periodType) || "LAST_7_DAYS".equalsIgnoreCase(periodType)) {
                    java.time.LocalDateTime end = java.time.LocalDateTime.now();
                    java.time.LocalDateTime start = end.minusDays(7);
                    periodStartDate = start.toLocalDate();
                    periodEndDate = end.toLocalDate();
                    periodStartTime = start.toLocalTime();
                    periodEndTime = end.toLocalTime();
                }
            }

            progress(25, "Загрузка данных из базы…");
            List<EquipmentWorkReportDTO> data = reportDataService.getEquipmentWorkDataNew(
                    template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            java.util.Map<Integer, List<EquipmentWorkReportDTO>> dataByMachine = data != null ? data.stream()
                    .filter(d -> d.getWeldingMachineId() != null)
                    .collect(java.util.stream.Collectors.groupingBy(EquipmentWorkReportDTO::getWeldingMachineId))
                    : new java.util.HashMap<>();

            List<EquipmentWorkReportSectionDTO> sections = new java.util.ArrayList<>();
            for (Integer mid : equipmentIdsForReport) {
                EquipmentWorkReportSectionDTO info = sectionInfoMap.get(mid);
                List<EquipmentWorkReportDTO> rows = dataByMachine.getOrDefault(mid, java.util.Collections.emptyList());
                sections.add(new EquipmentWorkReportSectionDTO(
                        info.getWeldingMachineId(),
                        info.getEquipmentModel(),
                        info.getEquipmentName(),
                        info.getEquipmentDepartment(),
                        rows.isEmpty() ? null : rows
                ));
            }

            progress(80, "Формирование файла Excel…");
            byte[] reportBytes = reportService.generateEquipmentWorkReportMultiSection(
                    sections, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            String filename = "equipment_work_report_" + System.currentTimeMillis() + ".xlsx";
            progress(100, "Готово");
            return new ReportFileResult(filename, reportBytes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ReportFileResult buildEquipmentMalfunctionReport(EquipmentMalfunctionReportGenerationDTO generationRequest) {
        try {
            progress(5, "Подготовка параметров отчёта…");
            EquipmentMalfunctionReportTemplateDTO template = new EquipmentMalfunctionReportTemplateDTO();
            ReportTemplateDTO templateWithPeriodSettings = null;
            if (generationRequest.getTemplateId() != null) {
                Optional<ReportTemplateDTO> generalOpt = reportTemplateService.getTemplateById(generationRequest.getTemplateId());
                if (generalOpt.isPresent()) {
                    ReportTemplateDTO general = generalOpt.get();
                    templateWithPeriodSettings = general;
                    template.setTemplateId(general.getId());
                    template.setTemplateName(general.getName());
                    if (general.getReportParameters() != null && general.getReportParameters().containsKey("selectedEquipmentIds")) {
                        @SuppressWarnings("unchecked")
                        List<Number> ids = (List<Number>) general.getReportParameters().get("selectedEquipmentIds");
                        if (ids != null && !ids.isEmpty()) {
                            template.setSelectedEquipmentIds(ids.stream().map(Number::intValue).collect(java.util.stream.Collectors.toList()));
                        }
                    }
                }
            }
            if (generationRequest.getSelectedEquipmentIds() != null && !generationRequest.getSelectedEquipmentIds().isEmpty()) {
                template.setSelectedEquipmentIds(generationRequest.getSelectedEquipmentIds());
            }

            java.time.LocalDate periodStartDate = generationRequest.getPeriodStartDate();
            java.time.LocalDate periodEndDate = generationRequest.getPeriodEndDate();
            java.time.LocalTime periodStartTime = generationRequest.getPeriodStartTime();
            java.time.LocalTime periodEndTime = generationRequest.getPeriodEndTime();
            if (periodStartDate == null) periodStartDate = java.time.LocalDate.now();
            if (periodEndDate == null) periodEndDate = java.time.LocalDate.now();
            if (periodStartTime == null) periodStartTime = java.time.LocalTime.MIN;
            if (periodEndTime == null) periodEndTime = java.time.LocalTime.of(23, 59, 59);

            // «За 24 часа» / «За 7 дней» — считаем период на сервере: конец = сейчас (из шаблона или из запроса periodType)
            String periodTypeFromRequest = generationRequest.getPeriodType() != null ? generationRequest.getPeriodType().trim() : "";
            if (templateWithPeriodSettings != null && templateWithPeriodSettings.getPeriodSettings() != null) {
                Object pt = templateWithPeriodSettings.getPeriodSettings().get("periodType");
                if (pt != null) periodTypeFromRequest = pt.toString().trim();
            }
            if ("За 24 часа".equals(periodTypeFromRequest) || "LAST_24_HOURS".equalsIgnoreCase(periodTypeFromRequest) || "24h".equalsIgnoreCase(periodTypeFromRequest)) {
                LocalDateTime end = LocalDateTime.now();
                LocalDateTime start = end.minusHours(24);
                periodStartDate = start.toLocalDate();
                periodEndDate = end.toLocalDate();
                periodStartTime = start.toLocalTime();
                periodEndTime = end.toLocalTime();
            } else if ("За 7 дней".equals(periodTypeFromRequest) || "LAST_7_DAYS".equalsIgnoreCase(periodTypeFromRequest) || "7DAYS".equalsIgnoreCase(periodTypeFromRequest)) {
                LocalDateTime end = LocalDateTime.now();
                LocalDateTime start = end.minusDays(7);
                periodStartDate = start.toLocalDate();
                periodEndDate = end.toLocalDate();
                periodStartTime = start.toLocalTime();
                periodEndTime = end.toLocalTime();
            }

            // Если конец периода всё ещё в будущем — ограничиваем текущим моментом
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDateTime = LocalDateTime.of(periodEndDate, periodEndTime);
            if (endDateTime.isAfter(now)) {
                periodEndDate = now.toLocalDate();
                periodEndTime = now.toLocalTime();
            }

            progress(25, "Загрузка данных из базы…");
            List<EquipmentMalfunctionReportSectionDTO> sections = reportDataService.getEquipmentMalfunctionData(
                    template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            progress(80, "Формирование файла Excel…");
            byte[] reportBytes = reportService.generateEquipmentMalfunctionReportMultiSection(
                    sections, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            String filename = "equipment_malfunction_report_" + System.currentTimeMillis() + ".xlsx";
            progress(100, "Готово");
            return new ReportFileResult(filename, reportBytes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
