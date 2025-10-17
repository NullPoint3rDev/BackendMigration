package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.WeldingProcedureSpecification;
import org.alloy.repositories.WeldingProcedureSpecificationRepository;
import org.alloy.models.GeneralStatus;
import org.alloy.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/wps")
@Tag(name = "Welding Procedure Specifications", description = "API для управления технологическими картами сварки (WPS). " +
    "Позволяет создавать, просматривать, обновлять и удалять технологические карты сварки, " +
    "а также выполнять поиск по различным параметрам.")
@SecurityRequirement(name = "JWT")
public class WeldingProcedureSpecificationController {

    @PostConstruct
    public void init() {
        System.out.println("WeldingProcedureSpecificationController initialized!");
    }

    private final WeldingProcedureSpecificationRepository wpsRepository;

    @Autowired
    public WeldingProcedureSpecificationController(WeldingProcedureSpecificationRepository wpsRepository) {
        this.wpsRepository = wpsRepository;
    }

    @Operation(
        summary = "Получить все технологические карты сварки",
        description = "Возвращает список всех технологических карт сварки в системе."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список WPS успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingProcedureSpecification.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация"
        )
    })
    @GetMapping
    public ResponseEntity<List<WeldingProcedureSpecification>> getAllWPS() {
        // Ограничиваем количество записей для производительности
        List<WeldingProcedureSpecification> wpsList = wpsRepository.findAll().stream()
            .limit(100) // Максимум 100 записей
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(wpsList);
    }

    @Operation(
        summary = "Получить WPS по ID",
        description = "Возвращает технологическую карту сварки по ее уникальному идентификатору."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "WPS успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingProcedureSpecification.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "WPS не найден"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<WeldingProcedureSpecification> getWPSById(
            @Parameter(description = "ID WPS") @PathVariable Integer id) {
        WeldingProcedureSpecification wps = wpsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WPS не найден с ID: " + id));
        return ResponseEntity.ok(wps);
    }

    @Operation(
        summary = "Создать новую технологическую карту сварки",
        description = "Создает новую технологическую карту сварки в системе."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "WPS успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingProcedureSpecification.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные"
        )
    })
    @PostMapping
    public ResponseEntity<WeldingProcedureSpecification> createWPS(
            @RequestBody WeldingProcedureSpecification wps) {
        wps.setDateCreated(LocalDateTime.now());
        wps.setDateUpdated(LocalDateTime.now());
        if (wps.getStatus() == null) {
            wps.setStatus(GeneralStatus.Active);
        }
        WeldingProcedureSpecification savedWPS = wpsRepository.save(wps);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWPS);
    }

    @Operation(
        summary = "Обновить технологическую карту сварки",
        description = "Обновляет существующую технологическую карту сварки."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "WPS успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingProcedureSpecification.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "WPS не найден"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<WeldingProcedureSpecification> updateWPS(
            @Parameter(description = "ID WPS") @PathVariable Integer id,
            @RequestBody WeldingProcedureSpecification wpsDetails) {
        WeldingProcedureSpecification wps = wpsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WPS не найден с ID: " + id));
        
        wps.setName(wpsDetails.getName());
        wps.setDescription(wpsDetails.getDescription());
        wps.setWeldingMethod(wpsDetails.getWeldingMethod());
        wps.setMaterialType(wpsDetails.getMaterialType());
        wps.setThickness(wpsDetails.getThickness());
        wps.setCurrentMin(wpsDetails.getCurrentMin());
        wps.setCurrentMax(wpsDetails.getCurrentMax());
        wps.setVoltageMin(wpsDetails.getVoltageMin());
        wps.setVoltageMax(wpsDetails.getVoltageMax());
        wps.setFeedRate(wpsDetails.getFeedRate());
        wps.setGasConsumption(wpsDetails.getGasConsumption());
        wps.setGostStandard(wpsDetails.getGostStandard());
        wps.setStatus(wpsDetails.getStatus());
        wps.setDateUpdated(LocalDateTime.now());
        
        WeldingProcedureSpecification updatedWPS = wpsRepository.save(wps);
        return ResponseEntity.ok(updatedWPS);
    }

    @Operation(
        summary = "Удалить технологическую карту сварки",
        description = "Удаляет технологическую карту сварки из системы."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "WPS успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "WPS не найден"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWPS(
            @Parameter(description = "ID WPS") @PathVariable Integer id) {
        if (!wpsRepository.existsById(id)) {
            throw new ResourceNotFoundException("WPS не найден с ID: " + id);
        }
        wpsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Получить WPS по статусу",
        description = "Возвращает список WPS с указанным статусом."
    )
    @GetMapping("/status/{status}")
    public ResponseEntity<List<WeldingProcedureSpecification>> getWPSByStatus(
            @Parameter(description = "Статус WPS") @PathVariable GeneralStatus status) {
        List<WeldingProcedureSpecification> wpsList = wpsRepository.findByStatus(status);
        return ResponseEntity.ok(wpsList);
    }

    @Operation(
        summary = "Получить WPS по методу сварки",
        description = "Возвращает список WPS с указанным методом сварки."
    )
    @GetMapping("/method/{method}")
    public ResponseEntity<List<WeldingProcedureSpecification>> getWPSByWeldingMethod(
            @Parameter(description = "Метод сварки") @PathVariable WeldingProcedureSpecification.WeldingMethod method) {
        List<WeldingProcedureSpecification> wpsList = wpsRepository.findByWeldingMethod(method);
        return ResponseEntity.ok(wpsList);
    }

    @Operation(
        summary = "Получить WPS по типу материала",
        description = "Возвращает список WPS для указанного типа материала."
    )
    @GetMapping("/material/{materialType}")
    public ResponseEntity<List<WeldingProcedureSpecification>> getWPSByMaterialType(
            @Parameter(description = "Тип материала") @PathVariable String materialType) {
        List<WeldingProcedureSpecification> wpsList = wpsRepository.findByMaterialTypeContainingIgnoreCase(materialType);
        return ResponseEntity.ok(wpsList);
    }

    @Operation(
        summary = "Поиск WPS",
        description = "Поиск WPS по названию или описанию."
    )
    @GetMapping("/search")
    public ResponseEntity<List<WeldingProcedureSpecification>> searchWPS(
            @Parameter(description = "Поисковый запрос") @RequestParam String query) {
        List<WeldingProcedureSpecification> wpsList = wpsRepository.findBySearchTerm(query);
        return ResponseEntity.ok(wpsList);
    }

    @Operation(
        summary = "Получить WPS по диапазону тока",
        description = "Возвращает список WPS, подходящих для указанного значения тока."
    )
    @GetMapping("/current/{current}")
    public ResponseEntity<List<WeldingProcedureSpecification>> getWPSByCurrentRange(
            @Parameter(description = "Значение тока (А)") @PathVariable Integer current) {
        List<WeldingProcedureSpecification> wpsList = wpsRepository.findByCurrentRange(current);
        return ResponseEntity.ok(wpsList);
    }

    @Operation(
        summary = "Получить WPS по диапазону напряжения",
        description = "Возвращает список WPS, подходящих для указанного значения напряжения."
    )
    @GetMapping("/voltage/{voltage}")
    public ResponseEntity<List<WeldingProcedureSpecification>> getWPSByVoltageRange(
            @Parameter(description = "Значение напряжения (В)") @PathVariable Integer voltage) {
        List<WeldingProcedureSpecification> wpsList = wpsRepository.findByVoltageRange(voltage);
        return ResponseEntity.ok(wpsList);
    }
}
