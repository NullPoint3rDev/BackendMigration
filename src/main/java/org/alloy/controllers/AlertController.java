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
import org.alloy.models.dto.AlertDTO;
import org.alloy.models.dto.mapper.AlertMapper;
import org.alloy.services.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alerts")
@Tag(name = "Alerts", description = "API для управления оповещениями системы")
@SecurityRequirement(name = "JWT")
public class AlertController {
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
                schema = @Schema(implementation = AlertDTO.class))
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
    public List<AlertDTO> getAll(
        @Parameter(description = "Тип оповещения для фильтрации", example = "MAINTENANCE")
        @RequestParam(required = false) String type,
        
        @Parameter(description = "Статус прочтения для фильтрации", example = "false")
        @RequestParam(required = false) Boolean isRead
    ) {
        return alertService.findAll().stream().map(AlertMapper::toDTO).collect(Collectors.toList());
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
                schema = @Schema(implementation = AlertDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Оповещение не найдено"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<AlertDTO> getById(
        @Parameter(description = "ID оповещения", example = "1", required = true)
        @PathVariable Integer id
    ) {
        return alertService.findById(id)
            .map(AlertMapper::toDTO)
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
                schema = @Schema(implementation = AlertDTO.class))
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
    public ResponseEntity<AlertDTO> createAlert(
        @Parameter(description = "Данные оповещения", required = true)
        @RequestBody AlertDTO alertDTO
    ) {
        Alert entity = AlertMapper.toEntity(alertDTO);
        return new ResponseEntity<>(AlertMapper.toDTO(alertService.createAlert(entity)), HttpStatus.CREATED);
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
                schema = @Schema(implementation = AlertDTO.class))
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
    public ResponseEntity<AlertDTO> updateAlert(
        @Parameter(description = "ID оповещения", required = true, example = "1")
        @PathVariable Integer id,
        @Parameter(description = "Обновленные данные оповещения", required = true)
        @RequestBody AlertDTO alertDTO
    ) {
        Alert entity = AlertMapper.toEntity(alertDTO);
        entity.setId(id);
        return ResponseEntity.ok(AlertMapper.toDTO(alertService.updateAlert(entity)));
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