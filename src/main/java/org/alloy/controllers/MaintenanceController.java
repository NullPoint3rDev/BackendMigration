package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.Maintenance;
import org.alloy.models.dto.MaintenanceDTO;
import org.alloy.models.dto.mapper.MaintenanceMapper;
import org.alloy.services.MaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/maintenance")
@Tag(name = "Maintenance", description = "API для управления записями технического обслуживания сварочного оборудования. " +
    "Позволяет создавать, просматривать, обновлять и удалять записи о техническом обслуживании, " +
    "а также получать информацию о статусе обслуживания конкретных машин.")
@SecurityRequirement(name = "JWT")
public class MaintenanceController {

    @PostConstruct
    public void init() {
        System.out.println("MaintenanceController initialized!");
    }

    private final MaintenanceService maintenanceService;

    @Autowired
    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Operation(
        summary = "Получить все записи обслуживания",
        description = "Возвращает список всех записей технического обслуживания в системе. " +
                     "Записи содержат информацию о проведенных работах, датах обслуживания, " +
                     "статусе и связанном оборудовании."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список записей успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MaintenanceDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к записям обслуживания",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<MaintenanceDTO>> getAllMaintenanceRecords() {
        List<MaintenanceDTO> records = maintenanceService.getAllMaintenanceRecords().stream()
            .map(MaintenanceMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }

    @Operation(
        summary = "Получить запись обслуживания по ID",
        description = "Возвращает запись технического обслуживания по ее уникальному идентификатору. " +
                     "Если запись не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Запись успешно найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MaintenanceDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Запись не найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к записи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceDTO> getMaintenanceRecordById(
        @Parameter(description = "ID записи обслуживания", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return maintenanceService.getMaintenanceRecordById(id)
            .map(MaintenanceMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить записи обслуживания по ID машины",
        description = "Возвращает список всех записей технического обслуживания для указанной сварочной машины. " +
                     "Записи возвращаются в хронологическом порядке."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список записей успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MaintenanceDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к записям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/machine/{machineId}")
    public ResponseEntity<List<MaintenanceDTO>> getMaintenanceRecordsByMachineId(
        @Parameter(description = "ID сварочной машины", required = true, example = "1")
        @PathVariable Integer machineId
    ) {
        List<MaintenanceDTO> records = maintenanceService.getMaintenanceRecordsByMachineId(machineId).stream()
            .map(MaintenanceMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }

    @Operation(
        summary = "Получить последнюю запись обслуживания",
        description = "Возвращает последнюю запись технического обслуживания для указанной сварочной машины. " +
                     "Если записей нет, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Последняя запись успешно найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MaintenanceDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Записи не найдены",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к записи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/machine/{machineId}/latest")
    public ResponseEntity<MaintenanceDTO> getLatestMaintenanceRecord(
        @Parameter(description = "ID сварочной машины", required = true, example = "1")
        @PathVariable Integer machineId
    ) {
        return maintenanceService.getLatestMaintenanceRecord(machineId)
            .map(MaintenanceMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить записи обслуживания по статусу",
        description = "Возвращает список записей технического обслуживания для указанной машины, " +
                     "отфильтрованных по статусу (например: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список записей успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MaintenanceDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к записям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/machine/{machineId}/status/{status}")
    public ResponseEntity<List<MaintenanceDTO>> getMaintenanceRecordsByStatus(
        @Parameter(description = "ID сварочной машины", required = true, example = "1")
        @PathVariable Integer machineId,
        
        @Parameter(description = "Статус обслуживания", required = true, example = "COMPLETED",
            schema = @Schema(allowableValues = {"PLANNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"}))
        @PathVariable String status
    ) {
        List<MaintenanceDTO> records = maintenanceService.getMaintenanceRecordsByStatus(machineId, status).stream()
            .map(MaintenanceMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }

    @Operation(
        summary = "Создать новую запись обслуживания",
        description = "Создает новую запись технического обслуживания. " +
                     "Запись должна содержать информацию о машине, типе обслуживания, " +
                     "планируемой дате и описании работ."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Запись успешно создана",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MaintenanceDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные записи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для создания записи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<MaintenanceDTO> createMaintenanceRecord(
        @Parameter(description = "Данные записи обслуживания", required = true)
        @RequestBody MaintenanceDTO maintenanceDTO
    ) {
        Maintenance entity = MaintenanceMapper.toEntity(maintenanceDTO);
        return new ResponseEntity<>(MaintenanceMapper.toDTO(maintenanceService.createMaintenanceRecord(entity)), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Обновить запись обслуживания",
        description = "Обновляет существующую запись технического обслуживания по ее ID. " +
                     "Можно изменить статус, описание работ, даты и другую информацию. " +
                     "Если запись не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Запись успешно обновлена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MaintenanceDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Запись не найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные записи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для обновления записи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceDTO> updateMaintenanceRecord(
        @Parameter(description = "ID записи обслуживания", required = true, example = "1")
        @PathVariable Integer id,
        @Parameter(description = "Обновленные данные записи обслуживания", required = true)
        @RequestBody MaintenanceDTO maintenanceDTO
    ) {
        Maintenance entity = MaintenanceMapper.toEntity(maintenanceDTO);
        entity.setId(id);
        return ResponseEntity.ok(MaintenanceMapper.toDTO(maintenanceService.updateMaintenanceRecord(entity)));
    }

    @Operation(
        summary = "Удалить запись обслуживания",
        description = "Удаляет запись технического обслуживания по ее ID. " +
                     "Если запись не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Запись успешно удалена"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Запись не найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления записи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenanceRecord(
        @Parameter(description = "ID записи обслуживания", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            maintenanceService.deleteMaintenanceRecord(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить все записи обслуживания машины",
        description = "Удаляет все записи технического обслуживания для указанной сварочной машины. " +
                     "Если машина не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Все записи успешно удалены"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Машина не найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления записей",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/machine/{machineId}")
    public ResponseEntity<Void> deleteAllMaintenanceRecords(
        @Parameter(description = "ID сварочной машины", required = true, example = "1")
        @PathVariable Integer machineId
    ) {
        try {
            maintenanceService.deleteAllMaintenanceRecords(machineId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Maintenance record with id 1 not found")
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
