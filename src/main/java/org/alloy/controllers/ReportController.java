package org.alloy.controllers;

import org.alloy.models.dto.*;
import org.alloy.models.ReportHistory;
import org.alloy.services.ReportService;
import org.alloy.services.ReportDataService;
import org.alloy.services.ReportHistoryService;
import org.alloy.services.WireConsumptionReportTemplateService;
import org.alloy.services.WelderWorkReportTemplateService;
import org.alloy.services.EquipmentWorkReportTemplateService;
import org.alloy.services.ReportTemplateService;
import org.alloy.services.UserAccountService;
import org.alloy.repositories.WelderRepository;
import org.alloy.repositories.EmployeeRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.OrganizationUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDataService reportDataService;

    @Autowired
    private ReportHistoryService reportHistoryService;

    @Autowired
    private WireConsumptionReportTemplateService templateService;

    @Autowired
    private WelderWorkReportTemplateService welderWorkTemplateService;

    @Autowired
    private EquipmentWorkReportTemplateService equipmentWorkTemplateService;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private WelderRepository welderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Диагностика: последние 20 записей состояний с ошибками из БД (id, welding_machine_id, date_created, state_duration_ms, error_code).
     * Позволяет проверить, что реально пишется в error_code и state_duration_ms.
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/diagnostic/equipment-error-samples")
    public ResponseEntity<Map<String, Object>> getEquipmentErrorSamples() {
        List<Object[]> rows = weldingMachineStateRepository.findErrorStatesNativeRecent(PageRequest.of(0, 20));
        List<Map<String, Object>> samples = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row.length > 0 ? row[0] : null);
            map.put("weldingMachineId", row.length > 1 ? row[1] : null);
            map.put("dateCreated", row.length > 2 ? row[2] : null);
            Object dur = row.length > 3 ? row[3] : null;
            map.put("stateDurationMs", dur != null && dur instanceof Number ? ((Number) dur).longValue() : (dur != null && dur instanceof BigDecimal ? ((BigDecimal) dur).longValue() : null));
            map.put("errorCode", row.length > 4 ? row[4] : null);
            samples.add(map);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("count", samples.size());
        body.put("samples", samples);
        body.put("hint", "Если errorCode и stateDurationMs null/0 — при сохранении состояния не передавались; для Core код ошибки теперь берётся из битов errors1/errors2.");
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/wire-consumption")
    public ResponseEntity<byte[]> generateWireConsumptionReport(@RequestBody ReportRequestDTO request) {
        try {
            List<WireConsumptionReportDTO> data = reportDataService.getWireConsumptionData(request);
            byte[] reportBytes = reportService.generateWireConsumptionReport(data, request.getFormat());

            String filename = "wire_consumption_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Сохранение/обновление шаблона отчета по расходу проволоки
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/wire-consumption/template")
    public ResponseEntity<WireConsumptionReportTemplateDTO> saveWireConsumptionTemplate(
            @RequestBody WireConsumptionReportTemplateDTO template) {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            WireConsumptionReportTemplateDTO savedTemplate = templateService.saveTemplate(template, userId);
            return ResponseEntity.ok(savedTemplate);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            System.err.println("Ошибка сохранения шаблона: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получение шаблона отчета по расходу проволоки
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/wire-consumption/template/{templateId}")
    public ResponseEntity<WireConsumptionReportTemplateDTO> getWireConsumptionTemplate(
            @PathVariable Long templateId) {
        try {
            Optional<WireConsumptionReportTemplateDTO> templateOpt = templateService.getTemplateById(templateId);
            if (templateOpt.isPresent()) {
                return ResponseEntity.ok(templateOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шаблона: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получение всех активных шаблонов
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/wire-consumption/templates")
    public ResponseEntity<List<WireConsumptionReportTemplateDTO>> getAllWireConsumptionTemplates() {
        try {
            List<WireConsumptionReportTemplateDTO> templates = templateService.getAllActiveTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шаблонов: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получение шаблонов текущего пользователя
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/wire-consumption/templates/my")
    public ResponseEntity<List<WireConsumptionReportTemplateDTO>> getMyWireConsumptionTemplates() {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<WireConsumptionReportTemplateDTO> templates = templateService.getTemplatesByUser(userId);
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шаблонов: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Удаление шаблона
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @DeleteMapping("/wire-consumption/template/{templateId}")
    public ResponseEntity<Void> deleteWireConsumptionTemplate(@PathVariable Long templateId) {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            templateService.deleteTemplate(templateId, userId);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Ошибка удаления шаблона: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получает ID текущего пользователя
     */
    private Integer getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            String username = authentication.getName();
            var userOpt = userAccountService.getUserAccountByUserName(username);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Ошибка получения текущего пользователя: " + e.getMessage());
            return null;
        }
    }

    /**
     * Генерация отчета по расходу проволоки (новый формат)
     * Отчет автоматически скачивается
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/wire-consumption/generate")
    public ResponseEntity<byte[]> generateWireConsumptionReportNew(
            @RequestBody WireConsumptionReportGenerationDTO generationRequest) {
        try {
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
            List<WireConsumptionReportDTO> data = reportDataService.getWireConsumptionDataNew(
                    template, periodStartDate, periodEndDate, periodStartTime, periodEndTime
            );

            // Генерируем Excel отчёт (в заголовке будут дата и время)
            byte[] reportBytes = reportService.generateWireConsumptionReportNew(
                    data, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            String filename = "wire_consumption_report_" + System.currentTimeMillis() + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Ошибка генерации отчета: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Сохранение/обновление шаблона отчета "По работе сварщика"
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/welder-work/template")
    public ResponseEntity<WelderWorkReportTemplateDTO> saveWelderWorkTemplate(
            @RequestBody WelderWorkReportTemplateDTO template) {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            WelderWorkReportTemplateDTO saved = welderWorkTemplateService.saveTemplate(template, userId);
            return ResponseEntity.ok(saved);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            System.err.println("Ошибка сохранения шаблона welder-work: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получение шаблона отчета "По работе сварщика"
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/welder-work/template/{templateId}")
    public ResponseEntity<WelderWorkReportTemplateDTO> getWelderWorkTemplate(@PathVariable Long templateId) {
        try {
            Optional<WelderWorkReportTemplateDTO> templateOpt = welderWorkTemplateService.getTemplateById(templateId);
            return templateOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шаблона welder-work: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получение шаблонов текущего пользователя (welder-work)
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/welder-work/templates/my")
    public ResponseEntity<List<WelderWorkReportTemplateDTO>> getMyWelderWorkTemplates() {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.ok(welderWorkTemplateService.getTemplatesByUser(userId));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шаблонов welder-work: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Удаление шаблона (welder-work)
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @DeleteMapping("/welder-work/template/{templateId}")
    public ResponseEntity<Void> deleteWelderWorkTemplate(@PathVariable Long templateId) {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            welderWorkTemplateService.deleteTemplate(templateId, userId);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Ошибка удаления шаблона welder-work: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Генерация отчета "По работе сварщика" (xlsx)
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/welder-work/generate")
    public ResponseEntity<byte[]> generateWelderWorkReport(
            @RequestBody WelderWorkReportGenerationDTO generationRequest) {
        try {
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
                return ResponseEntity.badRequest().build();
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

            byte[] reportBytes = reportService.generateWelderWorkReportMultiSection(
                    sections,
                    template,
                    periodStartDate,
                    periodEndDate,
                    periodStartTime,
                    periodEndTime
            );

            String filename = "welder_work_report_" + System.currentTimeMillis() + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Ошибка генерации отчета welder-work: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Генерация отчета "По работе оборудования" (xlsx)
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/equipment-work/generate")
    public ResponseEntity<byte[]> generateEquipmentWorkReport(
            @RequestBody EquipmentWorkReportGenerationDTO generationRequest) {
        try {
            EquipmentWorkReportTemplateDTO template = new EquipmentWorkReportTemplateDTO();
            ReportTemplateDTO templateWithPeriodSettings = null;
            if (generationRequest.getTemplateId() != null) {
                Optional<EquipmentWorkReportTemplateDTO> templateOpt =
                        equipmentWorkTemplateService.getTemplateById(generationRequest.getTemplateId());
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
                return ResponseEntity.badRequest().build();
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

            byte[] reportBytes = reportService.generateEquipmentWorkReportMultiSection(
                    sections, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            String filename = "equipment_work_report_" + System.currentTimeMillis() + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Ошибка генерации отчета equipment-work: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Генерация отчёта "По неисправностям оборудования" (xlsx).
     * Выбор оборудования — как в отчёте по работе оборудования.
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/equipment-malfunction/generate")
    public ResponseEntity<byte[]> generateEquipmentMalfunctionReport(
            @RequestBody EquipmentMalfunctionReportGenerationDTO generationRequest) {
        try {
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

            List<EquipmentMalfunctionReportSectionDTO> sections = reportDataService.getEquipmentMalfunctionData(
                    template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            byte[] reportBytes = reportService.generateEquipmentMalfunctionReportMultiSection(
                    sections, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            String filename = "equipment_malfunction_report_" + System.currentTimeMillis() + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Ошибка генерации отчёта по неисправностям оборудования: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Генерация отчета на основе нового шаблона ReportTemplate
     * Отчет автоматически скачивается в формате xlsx
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/generate-from-template")
    public ResponseEntity<byte[]> generateReportFromTemplate(
            @RequestBody ReportTemplateGenerationDTO generationRequest) {
        try {
            // Получаем новый шаблон
            ReportTemplateDTO reportTemplate = null;
            if (generationRequest.getTemplateId() != null) {
                Optional<ReportTemplateDTO> templateOpt =
                        reportTemplateService.getTemplateById(generationRequest.getTemplateId());
                if (templateOpt.isPresent()) {
                    reportTemplate = templateOpt.get();
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Преобразуем ReportTemplateDTO в WireConsumptionReportTemplateDTO
            WireConsumptionReportTemplateDTO wireTemplate = convertToWireTemplate(reportTemplate);

            // Получаем даты периода из настроек шаблона или из запроса
            java.time.LocalDate periodStartDate = generationRequest.getPeriodStartDate();
            java.time.LocalDate periodEndDate = generationRequest.getPeriodEndDate();
            java.time.LocalTime periodStartTime = generationRequest.getPeriodStartTime();
            java.time.LocalTime periodEndTime = generationRequest.getPeriodEndTime();

            // Если даты не указаны в запросе, используем настройки из шаблона
            if (periodStartDate == null && reportTemplate.getPeriodSettings() != null) {
                Map<String, Object> periodSettings = reportTemplate.getPeriodSettings();
                if (periodSettings.get("startDate") != null) {
                    periodStartDate = java.time.LocalDate.parse(periodSettings.get("startDate").toString());
                }
                if (periodSettings.get("endDate") != null) {
                    periodEndDate = java.time.LocalDate.parse(periodSettings.get("endDate").toString());
                }
            }

            // Если даты все еще не указаны, используем текущую дату
            if (periodStartDate == null) {
                periodStartDate = java.time.LocalDate.now();
            }
            if (periodEndDate == null) {
                periodEndDate = java.time.LocalDate.now();
            }
            if (periodStartTime == null) {
                periodStartTime = java.time.LocalTime.MIN;
            }
            if (periodEndTime == null) {
                periodEndTime = java.time.LocalTime.MAX;
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
            byte[] reportBytes = reportService.generateWireConsumptionReportNew(
                    data, wireTemplate, periodStartDate, periodEndDate, periodStartTime, periodEndTime);

            String filename = (reportTemplate.getName() != null ? reportTemplate.getName() : "report")
                    + "_" + System.currentTimeMillis() + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Ошибка генерации отчета из шаблона: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/welder")
    public ResponseEntity<byte[]> generateWelderReport(@RequestBody ReportRequestDTO request) {
        try {
            List<WelderReportDTO> data = reportDataService.getWelderReportData(request);
            byte[] reportBytes = reportService.generateWelderReport(data, request.getFormat());

            String filename = "welder_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/work")
    public ResponseEntity<byte[]> generateWorkReport(@RequestBody ReportRequestDTO request) {
        try {
            List<WorkReportDTO> data = reportDataService.getWorkReportData(request);
            byte[] reportBytes = reportService.generateWorkReport(data, request.getFormat());

            String filename = "work_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Новые endpoints для получения данных отчетов для просмотра онлайн
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/data/wire-consumption")
    public ResponseEntity<List<WireConsumptionReportDTO>> getWireConsumptionData(@RequestBody ReportRequestDTO request) {
        try {
            List<WireConsumptionReportDTO> data = reportDataService.getWireConsumptionData(request);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            System.err.println("Ошибка получения данных отчета по расходу проволоки: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/data/welder")
    public ResponseEntity<List<WelderReportDTO>> getWelderData(@RequestBody ReportRequestDTO request) {
        try {
            List<WelderReportDTO> data = reportDataService.getWelderReportData(request);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            System.err.println("Ошибка получения данных отчета по сварщику: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/data/work")
    public ResponseEntity<List<WorkReportDTO>> getWorkData(@RequestBody ReportRequestDTO request) {
        try {
            List<WorkReportDTO> data = reportDataService.getWorkReportData(request);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            System.err.println("Ошибка получения данных отчета по работе оборудования: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/data/welds")
    public ResponseEntity<List<WeldSegmentDTO>> getWeldsData(@RequestBody ReportRequestDTO request) {
        try {
            List<WeldSegmentDTO> data = reportDataService.getWeldsReportData(request);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            System.err.println("Ошибка получения данных отчета по сварочным швам: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/data/equipment")
    public ResponseEntity<List<WorkReportDTO>> getEquipmentData(@RequestBody ReportRequestDTO request) {
        try {
            List<WorkReportDTO> data = reportDataService.getWorkReportData(request);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            System.err.println("Ошибка получения данных отчета по оборудованию: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/types")
    public ResponseEntity<List<String>> getReportTypes() {
        List<String> reportTypes = List.of("WIRE_CONSUMPTION", "WELDER_REPORT", "WORK_REPORT");
        return ResponseEntity.ok(reportTypes);
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/formats")
    public ResponseEntity<List<String>> getReportFormats() {
        List<String> formats = List.of("EXCEL", "PDF", "CSV");
        return ResponseEntity.ok(formats);
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/periods")
    public ResponseEntity<List<String>> getReportPeriods() {
        List<String> periods = List.of("DAY", "WEEK", "MONTH", "QUARTER", "YEAR", "CUSTOM");
        return ResponseEntity.ok(periods);
    }

    // Новые эндпоинты для отчетов согласно требованиям

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/equipment")
    public ResponseEntity<byte[]> generateEquipmentReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по работе оборудования
            byte[] reportBytes = reportService.generateEquipmentReport(request);

            String filename = "equipment_work_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/welders")
    public ResponseEntity<byte[]> generateWeldersReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по работе сварщиков
            byte[] reportBytes = reportService.generateWeldersReport(request);

            String filename = "welders_work_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/materials")
    public ResponseEntity<byte[]> generateMaterialsReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по расходу материалов
            byte[] reportBytes = reportService.generateMaterialsReport(request);

            String filename = "materials_consumption_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/welds")
    public ResponseEntity<byte[]> generateWeldsReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по сварочным швам
            byte[] reportBytes = reportService.generateWeldsReport(request);

            String filename = "welds_quality_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/errors")
    public ResponseEntity<byte[]> generateErrorsReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по ошибкам
            byte[] reportBytes = reportService.generateErrorsReport(request);

            String filename = "equipment_errors_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/violations")
    public ResponseEntity<byte[]> generateViolationsReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по нарушениям
            byte[] reportBytes = reportService.generateViolationsReport(request);

            String filename = "welds_violations_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping("/tasks")
    public ResponseEntity<byte[]> generateTasksReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по заданиям
            byte[] reportBytes = reportService.generateTasksReport(request);

            String filename = "welding_tasks_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Методы для работы с историей отчетов

    /**
     * Получить последние отчеты для определенного типа
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/history/{reportType}")
    public ResponseEntity<List<ReportHistory>> getRecentReports(@PathVariable String reportType) {
        System.out.println("ReportController: Запрос истории для типа '" + reportType + "'");
        System.out.println("ReportController: URL: /reports/history/" + reportType);

        try {
            List<ReportHistory> reports = reportHistoryService.getRecentReports(reportType);
            System.out.println("ReportController: Возвращаем " + reports.size() + " отчетов");

            // Добавляем детальное логирование
            if (reports.isEmpty()) {
                System.out.println("ReportController: История пуста для типа '" + reportType + "'");
            } else {
                System.out.println("ReportController: Детали отчетов:");
                for (ReportHistory report : reports) {
                    System.out.println("  - " + report.getReportName() + " (" + report.getFormat() + ") - " + report.getGeneratedAt());
                }
            }

            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            System.err.println("ReportController: Ошибка при получении истории: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить все отчеты из истории
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS') or hasRole('ADMIN_ALLOY')")
    @GetMapping("/history")
    public ResponseEntity<List<ReportHistory>> getAllReports() {
        System.out.println("ReportController: Запрос всех отчетов из истории");

        try {
            List<ReportHistory> reports = reportHistoryService.getAllReports();
            System.out.println("ReportController: Возвращаем " + reports.size() + " отчетов из БД");

            // Добавляем детальное логирование
            if (reports.isEmpty()) {
                System.out.println("ReportController: История пуста");
            } else {
                System.out.println("ReportController: Детали отчетов:");
                for (ReportHistory report : reports) {
                    System.out.println("  - " + report.getReportName() + " (" + report.getFormat() + ") - " + report.getGeneratedAt() + " - Auto: " + report.getIsAutoGenerated());
                }
            }

            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            System.err.println("ReportController: Ошибка при получении всех отчетов: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Очистить все отчеты из истории
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS') or hasRole('ADMIN_ALLOY')")
    @DeleteMapping("/history/clear")
    public ResponseEntity<String> clearAllReports() {
        try {
            reportHistoryService.clearAllReports();
            System.out.println("ReportController: All reports cleared from history");
            return ResponseEntity.ok("All reports cleared successfully");
        } catch (Exception e) {
            System.err.println("ReportController: Error clearing reports: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error clearing reports: " + e.getMessage());
        }
    }

    /**
     * Получить информацию о том, откуда берутся отчеты
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS') or hasRole('ADMIN_ALLOY')")
    @GetMapping("/history/debug")
    public ResponseEntity<String> debugReports() {
        try {
            List<ReportHistory> reports = reportHistoryService.getAllReports();
            StringBuilder debug = new StringBuilder();
            debug.append("Total reports: ").append(reports.size()).append("\n");

            if (!reports.isEmpty()) {
                debug.append("Recent reports:\n");
                reports.stream().limit(5).forEach(report -> {
                    debug.append("- ").append(report.getReportName())
                            .append(" (").append(report.getFormat()).append(")")
                            .append(" - ").append(report.getGeneratedAt())
                            .append(" - Auto: ").append(report.getIsAutoGenerated())
                            .append("\n");
                });
            }

            return ResponseEntity.ok(debug.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * Получить все типы отчетов, для которых есть история
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS') or hasRole('ADMIN_ALLOY')")
    @GetMapping("/history/types")
    public ResponseEntity<java.util.Set<String>> getHistoryReportTypes() {
        java.util.Set<String> types = reportHistoryService.getReportTypes();
        return ResponseEntity.ok(types);
    }

    /**
     * Получить общее количество отчетов в истории
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS') or hasRole('ADMIN_ALLOY')")
    @GetMapping("/history/count")
    public ResponseEntity<Integer> getTotalReportsCount() {
        int count = reportHistoryService.getTotalReportsCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Очистить историю для определенного типа отчета
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS') or hasRole('ADMIN_ALLOY')")
    @DeleteMapping("/history/{reportType}")
    public ResponseEntity<Void> clearHistory(@PathVariable String reportType) {
        reportHistoryService.clearHistory(reportType);
        return ResponseEntity.ok().build();
    }

    /**
     * Очистить всю историю
     */
    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS') or hasRole('ADMIN_ALLOY')")
    @DeleteMapping("/history/all")
    public ResponseEntity<Void> clearAllHistory() {
        reportHistoryService.clearAllHistory();
        return ResponseEntity.ok().build();
    }

} 