package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.NetworkEquipment;
import org.alloy.repositories.NetworkEquipmentRepository;
import org.alloy.models.GeneralStatus;
import org.alloy.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/network-equipment")
@Tag(name = "Network Equipment", description = "API для управления сетевым оборудованием системы мониторинга. " +
    "Позволяет создавать, просматривать, обновлять и удалять сетевое оборудование, " +
    "а также отслеживать его статус и активность.")
@SecurityRequirement(name = "JWT")
public class NetworkEquipmentController {

    @PostConstruct
    public void init() {
        System.out.println("NetworkEquipmentController initialized!");
    }

    private final NetworkEquipmentRepository networkEquipmentRepository;

    @Autowired
    public NetworkEquipmentController(NetworkEquipmentRepository networkEquipmentRepository) {
        this.networkEquipmentRepository = networkEquipmentRepository;
    }

    @Operation(
        summary = "Получить все сетевое оборудование",
        description = "Возвращает список всего сетевого оборудования в системе."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список оборудования успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = NetworkEquipment.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация"
        )
    })
    @GetMapping
    public ResponseEntity<List<NetworkEquipment>> getAllNetworkEquipment() {
        List<NetworkEquipment> equipment = networkEquipmentRepository.findAll();
        return ResponseEntity.ok(equipment);
    }

    @Operation(
        summary = "Получить оборудование по ID",
        description = "Возвращает сетевое оборудование по его уникальному идентификатору."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Оборудование успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = NetworkEquipment.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Оборудование не найдено"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<NetworkEquipment> getNetworkEquipmentById(
            @Parameter(description = "ID оборудования") @PathVariable Integer id) {
        NetworkEquipment equipment = networkEquipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Сетевое оборудование не найдено с ID: " + id));
        return ResponseEntity.ok(equipment);
    }

    @Operation(
        summary = "Создать новое сетевое оборудование",
        description = "Создает новое сетевое оборудование в системе."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Оборудование успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = NetworkEquipment.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные"
        )
    })
    @PostMapping
    public ResponseEntity<NetworkEquipment> createNetworkEquipment(
            @RequestBody NetworkEquipment equipment) {
        equipment.setDateCreated(LocalDateTime.now());
        equipment.setDateUpdated(LocalDateTime.now());
        if (equipment.getStatus() == null) {
            equipment.setStatus(GeneralStatus.Active);
        }
        NetworkEquipment savedEquipment = networkEquipmentRepository.save(equipment);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEquipment);
    }

    @Operation(
        summary = "Обновить сетевое оборудование",
        description = "Обновляет существующее сетевое оборудование."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Оборудование успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = NetworkEquipment.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Оборудование не найдено"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<NetworkEquipment> updateNetworkEquipment(
            @Parameter(description = "ID оборудования") @PathVariable Integer id,
            @RequestBody NetworkEquipment equipmentDetails) {
        NetworkEquipment equipment = networkEquipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Сетевое оборудование не найдено с ID: " + id));
        
        equipment.setName(equipmentDetails.getName());
        equipment.setType(equipmentDetails.getType());
        equipment.setIpAddress(equipmentDetails.getIpAddress());
        equipment.setMacAddress(equipmentDetails.getMacAddress());
        equipment.setLocation(equipmentDetails.getLocation());
        equipment.setDescription(equipmentDetails.getDescription());
        equipment.setStatus(equipmentDetails.getStatus());
        equipment.setDateUpdated(LocalDateTime.now());
        
        NetworkEquipment updatedEquipment = networkEquipmentRepository.save(equipment);
        return ResponseEntity.ok(updatedEquipment);
    }

    @Operation(
        summary = "Удалить сетевое оборудование",
        description = "Удаляет сетевое оборудование из системы."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Оборудование успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Оборудование не найдено"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNetworkEquipment(
            @Parameter(description = "ID оборудования") @PathVariable Integer id) {
        if (!networkEquipmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Сетевое оборудование не найдено с ID: " + id);
        }
        networkEquipmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Получить оборудование по статусу",
        description = "Возвращает список оборудования с указанным статусом."
    )
    @GetMapping("/status/{status}")
    public ResponseEntity<List<NetworkEquipment>> getEquipmentByStatus(
            @Parameter(description = "Статус оборудования") @PathVariable GeneralStatus status) {
        List<NetworkEquipment> equipment = networkEquipmentRepository.findByStatus(status);
        return ResponseEntity.ok(equipment);
    }

    @Operation(
        summary = "Получить оборудование по типу",
        description = "Возвращает список оборудования указанного типа."
    )
    @GetMapping("/type/{type}")
    public ResponseEntity<List<NetworkEquipment>> getEquipmentByType(
            @Parameter(description = "Тип оборудования") @PathVariable NetworkEquipment.EquipmentType type) {
        List<NetworkEquipment> equipment = networkEquipmentRepository.findByType(type);
        return ResponseEntity.ok(equipment);
    }

    @Operation(
        summary = "Обновить время последней активности",
        description = "Обновляет время последней активности оборудования."
    )
    @PutMapping("/{id}/last-seen")
    public ResponseEntity<NetworkEquipment> updateLastSeen(
            @Parameter(description = "ID оборудования") @PathVariable Integer id) {
        NetworkEquipment equipment = networkEquipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Сетевое оборудование не найдено с ID: " + id));
        
        equipment.setLastSeen(LocalDateTime.now());
        equipment.setDateUpdated(LocalDateTime.now());
        
        NetworkEquipment updatedEquipment = networkEquipmentRepository.save(equipment);
        return ResponseEntity.ok(updatedEquipment);
    }

    @Operation(
        summary = "Поиск оборудования",
        description = "Поиск оборудования по названию или описанию."
    )
    @GetMapping("/search")
    public ResponseEntity<List<NetworkEquipment>> searchEquipment(
            @Parameter(description = "Поисковый запрос") @RequestParam String query) {
        List<NetworkEquipment> equipment = networkEquipmentRepository.findBySearchTerm(query);
        return ResponseEntity.ok(equipment);
    }
}
