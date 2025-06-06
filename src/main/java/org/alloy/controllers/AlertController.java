package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.Alert;
import org.alloy.services.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/alerts")
@Tag(name = "Alerts", description = "API для управления оповещениями системы")
@SecurityRequirement(name = "JWT")
public class AlertController {

    @PostConstruct
    public void init() {
        System.out.println("AlertController initialized!");
    }

    @Autowired
    private AlertService alertService;

    @Operation(
        summary = "Получить список всех оповещений",
        description = "Возвращает список всех оповещений в системе. Опционально можно фильтровать по типу и статусу прочтения."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список оповещений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Alert.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав доступа"
        )
    })
    @GetMapping
    public List<Alert> getAll(
        @Parameter(description = "Тип оповещения для фильтрации", example = "MAINTENANCE")
        @RequestParam(required = false) String type,
        
        @Parameter(description = "Статус прочтения для фильтрации", example = "false")
        @RequestParam(required = false) Boolean isRead
    ) {
        return alertService.findAll();
    }

    @Operation(
        summary = "Получить оповещение по ID",
        description = "Возвращает информацию о конкретном оповещении по его идентификатору"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Оповещение найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Alert.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Оповещение не найдено"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Alert> getById(
        @Parameter(description = "ID оповещения", example = "1", required = true)
        @PathVariable Integer id
    ) {
        return alertService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новое оповещение",
        description = "Создает новое оповещение в системе. Требуются права администратора."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Оповещение успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Alert.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные входные данные"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав для создания оповещения"
        )
    })
    @PostMapping
    public Alert create(
        @Parameter(description = "Данные оповещения", required = true)
        @RequestBody Alert alert
    ) {
        return alertService.save(alert);
    }

    @Operation(
        summary = "Обновить оповещение",
        description = "Обновляет информацию о существующем оповещении. Требуются права администратора."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Оповещение успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Alert.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Оповещение не найдено"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав для обновления оповещения"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<Alert> update(
        @Parameter(description = "ID оповещения", example = "1", required = true)
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные оповещения", required = true)
        @RequestBody Alert alert
    ) {
        if (!alertService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        alert.setId(id);
        return ResponseEntity.ok(alertService.save(alert));
    }

    @Operation(
        summary = "Удалить оповещение",
        description = "Удаляет оповещение из системы. Требуются права администратора."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Оповещение успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Оповещение не найдено"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав для удаления оповещения"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID оповещения", example = "1", required = true)
        @PathVariable Integer id
    ) {
        if (!alertService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        alertService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 