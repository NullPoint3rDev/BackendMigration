package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.services.WeldingMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/welding-machines")
@Tag(name = "Welding Machine", description = "API для управления сварочными машинами")
public class WeldingMachineController {

    private final WeldingMachineService weldingMachineService;

    @Autowired
    public WeldingMachineController(WeldingMachineService weldingMachineService) {
        this.weldingMachineService = weldingMachineService;
    }

    @Operation(summary = "Получить все сварочные машины")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список сварочных машин успешно получен",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WeldingMachine.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping
    public ResponseEntity<List<WeldingMachine>> getAllWeldingMachines() {
        List<WeldingMachine> weldingMachines = weldingMachineService.getAllWeldingMachines();
        return ResponseEntity.ok(weldingMachines);
    }

    @Operation(summary = "Получить сварочную машину по ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Сварочная машина найдена",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WeldingMachine.class))),
        @ApiResponse(responseCode = "404", description = "Сварочная машина не найдена")
    })
    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachine> getWeldingMachineById(
            @Parameter(description = "ID сварочной машины") @PathVariable Integer id) {
        return weldingMachineService.getWeldingMachineById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить сварочную машину по серийному номеру")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Сварочная машина найдена",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WeldingMachine.class))),
        @ApiResponse(responseCode = "404", description = "Сварочная машина не найдена")
    })
    @GetMapping("/serial-number/{serialNumber}")
    public ResponseEntity<WeldingMachine> getWeldingMachineBySerialNumber(
            @Parameter(description = "Серийный номер сварочной машины") @PathVariable String serialNumber) {
        return weldingMachineService.getWeldingMachineBySerialNumber(serialNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить сварочные машины по ID подразделения")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список сварочных машин успешно получен",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WeldingMachine.class))),
        @ApiResponse(responseCode = "404", description = "Подразделение не найдено")
    })
    @GetMapping("/organization/{organizationUnitId}")
    public ResponseEntity<List<WeldingMachine>> getWeldingMachinesByOrganizationId(
            @Parameter(description = "ID подразделения") @PathVariable Integer organizationUnitId) {
        List<WeldingMachine> weldingMachines = weldingMachineService.getWeldingMachinesByOrganizationId(organizationUnitId);
        return ResponseEntity.ok(weldingMachines);
    }

    @Operation(summary = "Получить сварочные машины по ID типа")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список сварочных машин успешно получен",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WeldingMachine.class))),
        @ApiResponse(responseCode = "404", description = "Тип сварочной машины не найден")
    })
    @GetMapping("/type/{typeId}")
    public ResponseEntity<List<WeldingMachine>> getWeldingMachinesByTypeId(
            @Parameter(description = "ID типа сварочной машины") @PathVariable Integer typeId) {
        List<WeldingMachine> weldingMachines = weldingMachineService.getWeldingMachinesByTypeId(typeId);
        return ResponseEntity.ok(weldingMachines);
    }

    @Operation(summary = "Поиск сварочных машин")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Результаты поиска успешно получены",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WeldingMachine.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<List<WeldingMachine>> searchWeldingMachines(
            @Parameter(description = "ID подразделения") @RequestParam Integer organizationUnitId,
            @Parameter(description = "Поисковый запрос") @RequestParam(required = false) String searchTerm) {
        List<WeldingMachine> weldingMachines = weldingMachineService.searchWeldingMachines(organizationUnitId, searchTerm);
        return ResponseEntity.ok(weldingMachines);
    }

    @Operation(summary = "Создать новую сварочную машину")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Сварочная машина успешно создана",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WeldingMachine.class))),
        @ApiResponse(responseCode = "400", description = "Неверные данные сварочной машины")
    })
    @PostMapping
    public ResponseEntity<WeldingMachine> createWeldingMachine(
            @Parameter(description = "Данные сварочной машины") @RequestBody WeldingMachine weldingMachine) {
        try {
            WeldingMachine createdWeldingMachine = weldingMachineService.createWeldingMachine(weldingMachine);
            return new ResponseEntity<>(createdWeldingMachine, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Обновить существующую сварочную машину")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Сварочная машина успешно обновлена",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = WeldingMachine.class))),
        @ApiResponse(responseCode = "404", description = "Сварочная машина не найдена"),
        @ApiResponse(responseCode = "400", description = "Неверные данные сварочной машины")
    })
    @PutMapping("/{id}")
    public ResponseEntity<WeldingMachine> updateWeldingMachine(
            @Parameter(description = "ID сварочной машины") @PathVariable Integer id,
            @Parameter(description = "Данные сварочной машины") @RequestBody WeldingMachine weldingMachine) {
        try {
            weldingMachine.setId(id);
            WeldingMachine updatedWeldingMachine = weldingMachineService.updateWeldingMachine(weldingMachine);
            return ResponseEntity.ok(updatedWeldingMachine);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeldingMachine(@PathVariable Integer id) {
        try {
            weldingMachineService.deleteWeldingMachine(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteWeldingMachine(@PathVariable Integer id) {
        try {
            weldingMachineService.hardDeleteWeldingMachine(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
