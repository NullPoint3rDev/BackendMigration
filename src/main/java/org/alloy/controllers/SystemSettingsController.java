package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.SystemSettings;
import org.alloy.repositories.SystemSettingsRepository;
import org.alloy.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/system-settings")
@Tag(name = "System Settings", description = "API для управления системными настройками. " +
        "Позволяет создавать, просматривать, обновлять и удалять системные настройки, " +
        "а также получать настройки по категориям.")
@SecurityRequirement(name = "JWT")
public class SystemSettingsController {

    @PostConstruct
    public void init() {
        System.out.println("SystemSettingsController initialized!");
    }

    private final SystemSettingsRepository systemSettingsRepository;

    @Autowired
    public SystemSettingsController(SystemSettingsRepository systemSettingsRepository) {
        this.systemSettingsRepository = systemSettingsRepository;
    }

    @Operation(
            summary = "Получить все системные настройки",
            description = "Возвращает список всех системных настроек в системе."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список настроек успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemSettings.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация"
            )
    })
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @GetMapping
    public ResponseEntity<List<SystemSettings>> getAllSettings() {
        List<SystemSettings> settings = systemSettingsRepository.findAll();
        return ResponseEntity.ok(settings);
    }

    @Operation(
            summary = "Получить настройку по ID",
            description = "Возвращает системную настройку по ее уникальному идентификатору."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Настройка успешно найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemSettings.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Настройка не найдена"
            )
    })
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @GetMapping("/{id}")
    public ResponseEntity<SystemSettings> getSettingById(
            @Parameter(description = "ID настройки") @PathVariable Integer id) {
        SystemSettings setting = systemSettingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Настройка не найдена с ID: " + id));
        return ResponseEntity.ok(setting);
    }

    @Operation(
            summary = "Получить настройку по ключу",
            description = "Возвращает активную системную настройку по ключу."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Настройка успешно найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemSettings.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Настройка не найдена"
            )
    })
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @GetMapping("/key/{key}")
    public ResponseEntity<SystemSettings> getSettingByKey(
            @Parameter(description = "Ключ настройки") @PathVariable String key) {
        SystemSettings setting = systemSettingsRepository.findActiveByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Настройка не найдена с ключом: " + key));
        return ResponseEntity.ok(setting);
    }

    @Operation(
            summary = "Получить настройки по категории",
            description = "Возвращает список активных настроек указанной категории."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Настройки успешно получены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemSettings.class, type = "array"))
            )
    })
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SystemSettings>> getSettingsByCategory(
            @Parameter(description = "Категория настроек") @PathVariable String category) {
        List<SystemSettings> settings = systemSettingsRepository.findActiveByCategory(category);
        return ResponseEntity.ok(settings);
    }

    @Operation(
            summary = "Получить все настройки в виде карты",
            description = "Возвращает все активные настройки, сгруппированные по категориям."
    )
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @GetMapping("/map")
    public ResponseEntity<Map<String, Map<String, String>>> getSettingsMap() {
        List<SystemSettings> allSettings = systemSettingsRepository.findByIsActive(true);
        Map<String, Map<String, String>> settingsMap = new HashMap<>();

        for (SystemSettings setting : allSettings) {
            settingsMap.computeIfAbsent(setting.getCategory(), k -> new HashMap<>())
                    .put(setting.getSettingKey(), setting.getSettingValue());
        }

        return ResponseEntity.ok(settingsMap);
    }

    @Operation(
            summary = "Создать новую системную настройку",
            description = "Создает новую системную настройку в системе."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Настройка успешно создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemSettings.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные"
            )
    })
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @PostMapping
    public ResponseEntity<SystemSettings> createSetting(
            @RequestBody SystemSettings setting) {
        setting.setDateCreated(LocalDateTime.now());
        setting.setDateUpdated(LocalDateTime.now());
        if (setting.getIsActive() == null) {
            setting.setIsActive(true);
        }
        SystemSettings savedSetting = systemSettingsRepository.save(setting);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSetting);
    }

    @Operation(
            summary = "Обновить системную настройку",
            description = "Обновляет существующую системную настройку."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Настройка успешно обновлена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemSettings.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Настройка не найдена"
            )
    })
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @PutMapping("/{id}")
    public ResponseEntity<SystemSettings> updateSetting(
            @Parameter(description = "ID настройки") @PathVariable Integer id,
            @RequestBody SystemSettings settingDetails) {
        SystemSettings setting = systemSettingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Настройка не найдена с ID: " + id));

        setting.setCategory(settingDetails.getCategory());
        setting.setSettingKey(settingDetails.getSettingKey());
        setting.setSettingValue(settingDetails.getSettingValue());
        setting.setDescription(settingDetails.getDescription());
        setting.setDataType(settingDetails.getDataType());
        setting.setIsActive(settingDetails.getIsActive());
        setting.setDateUpdated(LocalDateTime.now());

        SystemSettings updatedSetting = systemSettingsRepository.save(setting);
        return ResponseEntity.ok(updatedSetting);
    }

    @Operation(
            summary = "Обновить значение настройки по ключу",
            description = "Обновляет значение активной настройки по ключу."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Настройка успешно обновлена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemSettings.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Настройка не найдена"
            )
    })
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @PutMapping("/key/{key}")
    public ResponseEntity<SystemSettings> updateSettingByKey(
            @Parameter(description = "Ключ настройки") @PathVariable String key,
            @RequestBody String value) {
        SystemSettings setting = systemSettingsRepository.findActiveByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Настройка не найдена с ключом: " + key));

        setting.setSettingValue(value);
        setting.setDateUpdated(LocalDateTime.now());

        SystemSettings updatedSetting = systemSettingsRepository.save(setting);
        return ResponseEntity.ok(updatedSetting);
    }

    @Operation(
            summary = "Удалить системную настройку",
            description = "Удаляет системную настройку из системы."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Настройка успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Настройка не найдена"
            )
    })
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSetting(
            @Parameter(description = "ID настройки") @PathVariable Integer id) {
        if (!systemSettingsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Настройка не найдена с ID: " + id);
        }
        systemSettingsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Поиск настроек",
            description = "Поиск настроек по ключу или описанию."
    )
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @GetMapping("/search")
    public ResponseEntity<List<SystemSettings>> searchSettings(
            @Parameter(description = "Поисковый запрос") @RequestParam String query) {
        List<SystemSettings> settings = systemSettingsRepository.findBySearchTerm(query);
        return ResponseEntity.ok(settings);
    }

    @Operation(
            summary = "Получить активные настройки",
            description = "Возвращает список всех активных настроек."
    )
    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAuthority('PERMISSION_VISIBILITY_EDIT_ALLOY')")
    @GetMapping("/active")
    public ResponseEntity<List<SystemSettings>> getActiveSettings() {
        List<SystemSettings> settings = systemSettingsRepository.findByIsActive(true);
        return ResponseEntity.ok(settings);
    }
}
