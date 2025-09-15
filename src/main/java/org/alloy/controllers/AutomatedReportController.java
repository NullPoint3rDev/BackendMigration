package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.AutomatedReport;
import org.alloy.models.dto.AutomatedReportDTO;
import org.alloy.models.dto.mapper.AutomatedReportMapper;
import org.alloy.services.AutomatedReportService;
import org.alloy.services.AutomatedReportDataFixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/automated-reports")
@Tag(name = "Automated Reports", description = "API для управления автоматизированными отчетами. " +
    "Позволяет создавать, настраивать и управлять автоматической генерацией отчетов " +
    "на основе различных триггеров (время, ошибки оборудования, значения параметров).")
@SecurityRequirement(name = "JWT")
public class AutomatedReportController {

    @PostConstruct
    public void init() {
        System.out.println("AutomatedReportController initialized!");
    }

    private final AutomatedReportService automatedReportService;
    private final AutomatedReportDataFixService dataFixService;

    @Autowired
    public AutomatedReportController(AutomatedReportService automatedReportService, AutomatedReportDataFixService dataFixService) {
        this.automatedReportService = automatedReportService;
        this.dataFixService = dataFixService;
    }

    @Operation(
        summary = "Получить все автоматизированные отчеты",
        description = "Возвращает список всех автоматизированных отчетов в системе."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список автоматизированных отчетов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к автоматизированным отчетам",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<AutomatedReportDTO>> getAllAutomatedReports() {
        List<AutomatedReportDTO> reports = automatedReportService.getAllAutomatedReports().stream()
            .map(AutomatedReportMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(reports);
    }

    @Operation(
        summary = "Исправить данные автоматических отчетов",
        description = "Исправляет некорректные данные во всех автоматических отчетах (null template_type, template_name, created_by)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Данные успешно исправлены",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Ошибка при исправлении данных",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/fix-data")
    public ResponseEntity<String> fixAutomatedReportsData() {
        try {
            dataFixService.fixAllAutomatedReports();
            return ResponseEntity.ok("Данные автоматических отчетов успешно исправлены");
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportController: Failed to fix automated reports data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка при исправлении данных: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Принудительно выполнить автоматический отчет",
        description = "Принудительно выполняет указанный автоматический отчет независимо от времени."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Отчет успешно выполнен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автоматический отчет не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Ошибка при выполнении отчета",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/{id}/execute")
    public ResponseEntity<String> executeAutomatedReport(
        @Parameter(description = "ID автоматического отчета", required = true, example = "1")
        @PathVariable Long id
    ) {
        try {
            AutomatedReport automatedReport = automatedReportService.getAutomatedReportById(id)
                .orElse(null);
            if (automatedReport == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Автоматический отчет с ID " + id + " не найден");
            }
            
            // Принудительно выполняем отчет
            automatedReportService.executeAutomatedReport(automatedReport);
            
            return ResponseEntity.ok("Автоматический отчет '" + automatedReport.getName() + "' успешно выполнен");
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportController: Failed to execute automated report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка при выполнении отчета: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Проверить время выполнения отчета",
        description = "Проверяет, должно ли выполняться указанное время отчета."
    )
    @GetMapping("/{id}/check-time")
    public ResponseEntity<String> checkReportTime(
        @Parameter(description = "ID автоматического отчета", required = true, example = "1")
        @PathVariable Long id
    ) {
        try {
            AutomatedReport automatedReport = automatedReportService.getAutomatedReportById(id)
                .orElse(null);
            if (automatedReport == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Автоматический отчет с ID " + id + " не найден");
            }
            
            LocalDateTime nowUTC = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime nextRun = automatedReport.getNextRun();
            LocalDateTime nextRunUTC = nextRun.minusHours(3); // Конвертируем московское время в UTC
            long minutesDiff = java.time.Duration.between(nowUTC, nextRunUTC).toMinutes();
            
            String result = String.format(
                "Отчет: %s\nТекущее время (UTC): %s\nВремя выполнения (Москва): %s\nВремя выполнения (UTC): %s\nРазница: %d минут\nДолжен выполняться: %s",
                automatedReport.getName(),
                nowUTC,
                nextRun,
                nextRunUTC,
                minutesDiff,
                Math.abs(minutesDiff) <= 2 ? "ДА" : "НЕТ"
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportController: Failed to check report time: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка при проверке времени: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Получить автоматизированный отчет по ID",
        description = "Возвращает автоматизированный отчет по его уникальному идентификатору."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Автоматизированный отчет успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автоматизированный отчет не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<AutomatedReportDTO> getAutomatedReportById(
        @Parameter(description = "ID автоматизированного отчета", required = true, example = "1")
        @PathVariable Long id
    ) {
        return automatedReportService.getAutomatedReportById(id)
            .map(AutomatedReportMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить автоматизированные отчеты пользователя",
        description = "Возвращает список всех автоматизированных отчетов для указанного пользователя."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список автоматизированных отчетов пользователя успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class, type = "array"))
        )
    })
    @GetMapping("/user/{userAccountId}")
    public ResponseEntity<List<AutomatedReportDTO>> getUserAutomatedReports(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        System.out.println("AutomatedReportController: Getting reports for user: " + userAccountId);
        List<AutomatedReport> rawReports = automatedReportService.getUserAutomatedReports(userAccountId);
        System.out.println("AutomatedReportController: Found " + rawReports.size() + " raw reports");
        for (AutomatedReport r : rawReports) {
            System.out.println("AutomatedReportController: Report - ID: " + r.getId() + ", Name: " + r.getName() + ", CreatedBy: " + r.getCreatedBy());
        }
        List<AutomatedReportDTO> reports = rawReports.stream()
            .map(AutomatedReportMapper::toDTO)
            .collect(Collectors.toList());
        System.out.println("AutomatedReportController: Returning " + reports.size() + " DTO reports");
        return ResponseEntity.ok(reports);
    }

    @Operation(
        summary = "Получить активные автоматизированные отчеты",
        description = "Возвращает список всех активных автоматизированных отчетов."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список активных автоматизированных отчетов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class, type = "array"))
        )
    })
    @GetMapping("/active")
    public ResponseEntity<List<AutomatedReportDTO>> getActiveAutomatedReports() {
        List<AutomatedReportDTO> reports = automatedReportService.getActiveAutomatedReports().stream()
            .map(AutomatedReportMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(reports);
    }

    @Operation(
        summary = "Создать новый автоматизированный отчет",
        description = "Создает новый автоматизированный отчет с указанными параметрами и триггерами."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Автоматизированный отчет успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные автоматизированного отчета",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<AutomatedReportDTO> createAutomatedReport(
        @Parameter(description = "Данные автоматизированного отчета", required = true)
        @RequestBody AutomatedReportDTO automatedReportDTO
    ) {
        AutomatedReport entity = AutomatedReportMapper.toEntity(automatedReportDTO);
        return new ResponseEntity<>(AutomatedReportMapper.toDTO(automatedReportService.createAutomatedReport(entity)), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Обновить автоматизированный отчет",
        description = "Обновляет существующий автоматизированный отчет по его ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Автоматизированный отчет успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автоматизированный отчет не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<AutomatedReportDTO> updateAutomatedReport(
        @Parameter(description = "ID автоматизированного отчета", required = true, example = "1")
        @PathVariable Long id,
        @Parameter(description = "Обновленные данные автоматизированного отчета", required = true)
        @RequestBody AutomatedReportDTO automatedReportDTO
    ) {
        AutomatedReport entity = AutomatedReportMapper.toEntity(automatedReportDTO);
        entity.setId(id);
        return ResponseEntity.ok(AutomatedReportMapper.toDTO(automatedReportService.updateAutomatedReport(entity)));
    }

    @Operation(
        summary = "Удалить автоматизированный отчет",
        description = "Удаляет автоматизированный отчет по его ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Автоматизированный отчет успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автоматизированный отчет не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAutomatedReport(
        @Parameter(description = "ID автоматизированного отчета", required = true, example = "1")
        @PathVariable Long id
    ) {
        try {
            automatedReportService.deleteAutomatedReport(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Переключить статус автоматизированного отчета",
        description = "Активирует или деактивирует автоматизированный отчет."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статус автоматизированного отчета успешно изменен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автоматизированный отчет не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleAutomatedReportStatus(
        @Parameter(description = "ID автоматизированного отчета", required = true, example = "1")
        @PathVariable Long id
    ) {
        try {
            System.out.println("Controller: Received toggle request for ID: " + id);
            AutomatedReport updated = automatedReportService.toggleAutomatedReportStatus(id);
            return ResponseEntity.ok(AutomatedReportMapper.toDTO(updated));
        } catch (IllegalArgumentException e) {
            System.err.println("Controller: Report not found for ID: " + id);
            ErrorResponse error = new ErrorResponse();
            error.setError("Not Found");
            error.setMessage("Automated report with id " + id + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            System.err.println("Controller: Unexpected error for ID: " + id + ", error: " + e.getMessage());
            e.printStackTrace();
            ErrorResponse error = new ErrorResponse();
            error.setError("Internal Server Error");
            error.setMessage("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(
        summary = "Запустить автоматизированный отчет вручную",
        description = "Принудительно запускает выполнение автоматизированного отчета."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Автоматизированный отчет успешно запущен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автоматизированный отчет не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/{id}/run")
    public ResponseEntity<AutomatedReportDTO> runAutomatedReport(
        @Parameter(description = "ID автоматизированного отчета", required = true, example = "1")
        @PathVariable Long id
    ) {
        AutomatedReport executed = automatedReportService.runAutomatedReport(id);
        return ResponseEntity.ok(AutomatedReportMapper.toDTO(executed));
    }

    @Operation(
        summary = "Получить историю выполнения автоматизированного отчета",
        description = "Возвращает историю выполнения указанного автоматизированного отчета."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "История выполнения успешно получена",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/{id}/history")
    public ResponseEntity<List<Object>> getAutomatedReportHistory(
        @Parameter(description = "ID автоматизированного отчета", required = true, example = "1")
        @PathVariable Long id
    ) {
        List<Object> history = automatedReportService.getAutomatedReportHistory(id);
        return ResponseEntity.ok(history);
    }

    @Operation(
        summary = "Получить статистику автоматизированных отчетов",
        description = "Возвращает общую статистику по автоматизированным отчетам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статистика успешно получена",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/stats")
    public ResponseEntity<Object> getAutomatedReportsStats() {
        Object stats = automatedReportService.getAutomatedReportsStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(
        summary = "Поиск автоматизированных отчетов",
        description = "Выполняет поиск автоматизированных отчетов по заданному критерию."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Результаты поиска успешно получены",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AutomatedReportDTO.class, type = "array"))
        )
    })
    @GetMapping("/search")
    public ResponseEntity<List<AutomatedReportDTO>> searchAutomatedReports(
        @Parameter(description = "Поисковый запрос", required = true, example = "еженедельный")
        @RequestParam String q
    ) {
        List<AutomatedReportDTO> results = automatedReportService.searchAutomatedReports(q).stream()
            .map(AutomatedReportMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @Operation(
        summary = "Получить доступные типы триггеров",
        description = "Возвращает список всех доступных типов триггеров для автоматизированных отчетов."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список типов триггеров успешно получен",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/trigger-types")
    public ResponseEntity<List<Object>> getTriggerTypes() {
        List<Object> triggerTypes = automatedReportService.getTriggerTypes();
        return ResponseEntity.ok(triggerTypes);
    }

    @Operation(
        summary = "Получить доступные шаблоны отчетов",
        description = "Возвращает список шаблонов отчетов, доступных для автоматизации."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список доступных шаблонов успешно получен",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/available-templates")
    public ResponseEntity<List<Object>> getAvailableTemplates() {
        List<Object> templates = automatedReportService.getAvailableTemplates();
        return ResponseEntity.ok(templates);
    }

    @Operation(
        summary = "Валидация конфигурации триггера",
        description = "Проверяет корректность конфигурации триггера."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Конфигурация триггера валидна",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Конфигурация триггера некорректна",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/validate-trigger")
    public ResponseEntity<Object> validateTriggerConfig(
        @Parameter(description = "Конфигурация триггера", required = true)
        @RequestBody Object triggerConfig
    ) {
        Object validationResult = automatedReportService.validateTriggerConfig(triggerConfig);
        return ResponseEntity.ok(validationResult);
    }

    @Operation(
        summary = "Получить следующее время выполнения",
        description = "Вычисляет следующее время выполнения для указанной конфигурации триггера."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Следующее время выполнения успешно вычислено",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/next-run-time")
    public ResponseEntity<Object> getNextRunTime(
        @Parameter(description = "Конфигурация триггера", required = true)
        @RequestBody Object triggerConfig
    ) {
        Object nextRunTime = automatedReportService.getNextRunTime(triggerConfig);
        return ResponseEntity.ok(nextRunTime);
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Automated report with id 1 not found")
        private String message;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
