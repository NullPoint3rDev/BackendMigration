package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.dto.WeldingMachineDTO;
import org.alloy.models.dto.mapper.WeldingMachineMapper;
import org.alloy.services.WeldingMachineService;
import org.alloy.services.DeviceModelService;
import org.alloy.services.Wt2AccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/welding-machines")
@Tag(name = "Welding Machines", description = "API для управления сварочными машинами. " +
        "Позволяет создавать, просматривать, обновлять и удалять сварочные машины в системе. " +
        "Каждая сварочная машина имеет уникальный идентификатор, серийный номер, тип и привязку к подразделению. " +
        "API поддерживает поиск машин по различным параметрам и управление их состоянием.")
@SecurityRequirement(name = "JWT")
public class WeldingMachineController {

    private static final Logger log = LoggerFactory.getLogger(WeldingMachineController.class);

    @PostConstruct
    public void init() {
        System.out.println("WeldingMachineController initialized!");
    }

    private final WeldingMachineService weldingMachineService;
    private final DeviceModelService deviceModelService;
    private final Wt2AccessService wt2AccessService;

    @Autowired
    private org.alloy.services.DeviceLivenessRegistry deviceLivenessRegistry;

    @Autowired
    public WeldingMachineController(WeldingMachineService weldingMachineService, DeviceModelService deviceModelService, Wt2AccessService wt2AccessService) {
        this.weldingMachineService = weldingMachineService;
        this.deviceModelService = deviceModelService;
        this.wt2AccessService = wt2AccessService;
    }

    @Operation(
            summary = "Получить все сварочные машины",
            description = "Возвращает список всех сварочных машин в системе. " +
                    "Каждая машина содержит полную информацию о своем состоянии, " +
                    "типе, подразделении и других параметрах. " +
                    "Список может быть использован для общего обзора и управления машинами."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список сварочных машин успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeldingMachineDTO.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для доступа к списку сварочных машин",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<WeldingMachineDTO>> getAllWeldingMachines() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanReadEquipment(principal);
        List<WeldingMachineDTO> weldingMachines = wt2AccessService
                .filterWeldingMachines(weldingMachineService.getAllWeldingMachines(), principal).stream()
                .map(WeldingMachineMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(weldingMachines);
    }

    @Operation(
            summary = "Получить сварочную машину по ID",
            description = "Возвращает сварочную машину по её уникальному идентификатору. " +
                    "Если машина не найдена, возвращается 404 ошибка. " +
                    "Машина содержит полную информацию о своем состоянии и параметрах."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Сварочная машина успешно найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeldingMachineDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Сварочная машина не найдена",
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
                    description = "Недостаточно прав для доступа к сварочной машине",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachineDTO> getWeldingMachineById(
            @Parameter(description = "ID сварочной машины", required = true, example = "1")
            @PathVariable Integer id
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanAccessWeldingMachineForRead(id, principal);
        return weldingMachineService.getWeldingMachineById(id)
                .map(WeldingMachineMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Получить сварочную машину по серийному номеру",
            description = "Возвращает сварочную машину по её уникальному серийному номеру. " +
                    "Если машина не найдена, возвращается 404 ошибка. " +
                    "Этот метод часто используется для идентификации машины по её физическому номеру."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Сварочная машина успешно найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeldingMachineDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Сварочная машина не найдена",
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
                    description = "Недостаточно прав для доступа к сварочной машине",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/serial-number/{serialNumber}")
    public ResponseEntity<WeldingMachineDTO> getWeldingMachineBySerialNumber(
            @Parameter(description = "Серийный номер сварочной машины", required = true, example = "SN123456")
            @PathVariable String serialNumber
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        return weldingMachineService.getWeldingMachineBySerialNumber(serialNumber)
                .map(m -> {
                    wt2AccessService.assertCanAccessWeldingMachine(m.getId(), principal);
                    return WeldingMachineMapper.toDTO(m);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Получить сварочные машины по ID подразделения",
            description = "Возвращает список всех сварочных машин, привязанных к указанному подразделению. " +
                    "Этот метод используется для получения списка машин в конкретном подразделении " +
                    "или для проверки распределения машин по подразделениям."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список сварочных машин успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeldingMachineDTO.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Подразделение не найдено",
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
                    description = "Недостаточно прав для доступа к списку сварочных машин",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/organization/{organizationUnitId}")
    public ResponseEntity<List<WeldingMachineDTO>> getWeldingMachinesByOrganizationId(
            @Parameter(description = "ID подразделения", required = true, example = "1")
            @PathVariable Integer organizationUnitId
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanViewOrganizationUnit(organizationUnitId, principal);
        List<WeldingMachineDTO> weldingMachines = wt2AccessService
                .filterWeldingMachines(weldingMachineService.getWeldingMachinesByOrganizationId(organizationUnitId), principal).stream()
                .map(WeldingMachineMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(weldingMachines);
    }

    @Operation(
            summary = "Получить сварочные машины по ID типа",
            description = "Возвращает список всех сварочных машин определенного типа. " +
                    "Этот метод используется для получения списка машин с одинаковыми " +
                    "характеристиками или для анализа распределения машин по типам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список сварочных машин успешно получен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeldingMachineDTO.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тип сварочной машины не найден",
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
                    description = "Недостаточно прав для доступа к списку сварочных машин",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/type/{typeId}")
    public ResponseEntity<List<WeldingMachineDTO>> getWeldingMachinesByTypeId(
            @Parameter(description = "ID типа сварочной машины", required = true, example = "1")
            @PathVariable Integer typeId
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        List<WeldingMachineDTO> weldingMachines = wt2AccessService
                .filterWeldingMachines(weldingMachineService.getWeldingMachinesByTypeId(typeId), principal).stream()
                .map(WeldingMachineMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(weldingMachines);
    }

    @Operation(
            summary = "Поиск сварочных машин",
            description = "Выполняет поиск сварочных машин по заданным критериям. " +
                    "Поиск может осуществляться по подразделению и дополнительному " +
                    "поисковому запросу. Результаты могут быть отфильтрованы по различным параметрам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Результаты поиска успешно получены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeldingMachineDTO.class, type = "array"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для поиска сварочных машин",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/search")
    public ResponseEntity<List<WeldingMachineDTO>> searchWeldingMachines(
            @Parameter(description = "ID подразделения", required = true, example = "1")
            @RequestParam Integer organizationUnitId,

            @Parameter(description = "Поисковый запрос", required = false, example = "SN123")
            @RequestParam(required = false) String searchTerm
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanViewOrganizationUnit(organizationUnitId, principal);
        List<WeldingMachineDTO> weldingMachines = wt2AccessService
                .filterWeldingMachines(weldingMachineService.searchWeldingMachines(organizationUnitId, searchTerm), principal).stream()
                .map(WeldingMachineMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(weldingMachines);
    }

    @Operation(
            summary = "Проверка соединения устройства по MAC",
            description = "Возвращает время последней посылки устройства с данным MAC на сервер (epoch ms) и текущее серверное время. " +
                    "Клиент фиксирует serverTimeMs при старте и опрашивает эндпоинт, пока lastSeenMs не станет >= baseline (аппарат постучался)."
    )
    @GetMapping("/mac-liveness")
    public ResponseEntity<?> getMacLiveness(
            @Parameter(description = "MAC-адрес устройства", required = true, example = "E09806083396")
            @RequestParam String mac
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanWriteEquipment(principal);
        String normalizedMac = deviceModelService.normalizeMac(mac);
        Long lastSeenMs = deviceLivenessRegistry.getLastSeenMs(normalizedMac);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("mac", normalizedMac);
        body.put("lastSeenMs", lastSeenMs);
        body.put("serverTimeMs", System.currentTimeMillis());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Проверка существования аппарата по MAC",
            description = "Возвращает exists=true, если в системе уже есть неудалённый аппарат с данным MAC. " +
                    "excludeId позволяет исключить текущий аппарат при редактировании."
    )
    @GetMapping("/mac-exists")
    public ResponseEntity<?> checkMacExists(
            @Parameter(description = "MAC-адрес устройства", required = true, example = "E09806083396")
            @RequestParam String mac,
            @Parameter(description = "ID аппарата, который нужно исключить (режим редактирования)")
            @RequestParam(required = false) Integer excludeId
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanWriteEquipment(principal);
        String normalizedMac = deviceModelService.normalizeMac(mac);
        boolean exists = weldingMachineService.isMacInUse(normalizedMac, excludeId);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("mac", normalizedMac);
        body.put("exists", exists);
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Создать новую сварочную машину",
            description = "Создает новую сварочную машину в системе. " +
                    "Машина должна содержать информацию о типе, подразделении, " +
                    "серийном номере и других параметрах. " +
                    "При создании проверяется уникальность серийного номера."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Сварочная машина успешно создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeldingMachineDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные данные сварочной машины",
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
                    description = "Недостаточно прав для создания сварочной машины",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createWeldingMachine(
            @Parameter(description = "Данные сварочного аппарата", required = true)
            @RequestBody WeldingMachineDTO machineDTO
    ) {
        // Валидация MAC-адреса
        if (machineDTO.getMac() == null || machineDTO.getMac().trim().isEmpty()) {
            ErrorResponse error = new ErrorResponse();
            error.setError("VALIDATION_ERROR");
            error.setMessage("MAC-адрес обязателен");
            return ResponseEntity.badRequest().body(error);
        }

        // Валидация модели устройства
        if (machineDTO.getDeviceModel() == null) {
            ErrorResponse error = new ErrorResponse();
            error.setError("VALIDATION_ERROR");
            error.setMessage("Модель устройства обязательна");
            return ResponseEntity.badRequest().body(error);
        }


        // Нормализация и валидация формата MAC
        String normalizedMac = deviceModelService.normalizeMac(machineDTO.getMac());
        if (!deviceModelService.isValidMacFormat(normalizedMac)) {
            ErrorResponse error = new ErrorResponse();
            error.setError("VALIDATION_ERROR");
            error.setMessage("MAC-адрес должен содержать 12 символов (только 0-9, A-F)");
            return ResponseEntity.badRequest().body(error);
        }

        // Проверка уникальности MAC
        if (weldingMachineService.isMacInUse(normalizedMac, null)) {
            ErrorResponse error = new ErrorResponse();
            error.setError("DUPLICATE_ERROR");
            error.setMessage("Устройство с таким MAC-адресом уже существует");
            return ResponseEntity.badRequest().body(error);
        }

        // Устанавливаем нормализованный MAC
        machineDTO.setMac(normalizedMac);

        // Бизнес-правила: уникальность в предприятии и порядок дат.
        Integer createOrgUnitId = machineDTO.getOrganizationUnit() != null ? machineDTO.getOrganizationUnit().getId() : null;
        ResponseEntity<?> createValidation = validateEquipmentBusinessRules(machineDTO, createOrgUnitId, null);
        if (createValidation != null) {
            return createValidation;
        }

        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            wt2AccessService.assertCanWriteEquipment(principal);
            WeldingMachine entity = WeldingMachineMapper.toEntity(machineDTO);
            wt2AccessService.assertEnterpriseCanManageMachineOrgUnit(entity.getOrganizationUnitId(), principal);
            return ResponseEntity.status(HttpStatus.CREATED).body(WeldingMachineMapper.toDTO(weldingMachineService.createWeldingMachine(entity)));
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse();
            error.setError("CREATION_ERROR");
            error.setMessage("Ошибка создания устройства: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
            summary = "Обновить существующую сварочную машину",
            description = "Обновляет информацию о существующей сварочной машине. " +
                    "Можно изменить тип, подразделение, состояние и другие параметры. " +
                    "Если машина не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Сварочная машина успешно обновлена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeldingMachineDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Сварочная машина не найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные данные сварочной машины",
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
                    description = "Недостаточно прав для обновления сварочной машины",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWeldingMachine(
            @Parameter(description = "ID сварочного аппарата", required = true, example = "1")
            @PathVariable Integer id,
            @Parameter(description = "Обновленные данные сварочного аппарата", required = true)
            @RequestBody WeldingMachineDTO machineDTO
    ) {
        if (!weldingMachineService.getWeldingMachineById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanWriteEquipment(principal);
        wt2AccessService.assertCanAccessWeldingMachine(id, principal);

        // Валидация MAC-адреса
        if (machineDTO.getMac() == null || machineDTO.getMac().trim().isEmpty()) {
            ErrorResponse error = new ErrorResponse();
            error.setError("VALIDATION_ERROR");
            error.setMessage("MAC-адрес обязателен");
            return ResponseEntity.badRequest().body(error);
        }

        // Валидация модели устройства
        if (machineDTO.getDeviceModel() == null) {
            ErrorResponse error = new ErrorResponse();
            error.setError("VALIDATION_ERROR");
            error.setMessage("Модель устройства обязательна");
            return ResponseEntity.badRequest().body(error);
        }

        // Нормализация и валидация формата MAC
        String normalizedMac = deviceModelService.normalizeMac(machineDTO.getMac());
        if (!deviceModelService.isValidMacFormat(normalizedMac)) {
            ErrorResponse error = new ErrorResponse();
            error.setError("VALIDATION_ERROR");
            error.setMessage("MAC-адрес должен содержать 12 символов (только 0-9, A-F)");
            return ResponseEntity.badRequest().body(error);
        }

        // Проверка уникальности MAC (исключая текущую машину)
        if (weldingMachineService.isMacInUse(normalizedMac, id)) {
            ErrorResponse error = new ErrorResponse();
            error.setError("DUPLICATE_ERROR");
            error.setMessage("Устройство с таким MAC-адресом уже существует");
            return ResponseEntity.badRequest().body(error);
        }

        // Устанавливаем нормализованный MAC
        machineDTO.setMac(normalizedMac);

        // Бизнес-правила: уникальность в предприятии и порядок дат (исключая текущий аппарат).
        Integer updateOrgUnitId = machineDTO.getOrganizationUnit() != null ? machineDTO.getOrganizationUnit().getId() : null;
        ResponseEntity<?> updateValidation = validateEquipmentBusinessRules(machineDTO, updateOrgUnitId, id);
        if (updateValidation != null) {
            return updateValidation;
        }

        try {
            WeldingMachine entity = WeldingMachineMapper.toEntity(machineDTO);
            entity.setId(id);
            wt2AccessService.assertEnterpriseCanManageMachineOrgUnit(entity.getOrganizationUnitId(), principal);
            return ResponseEntity.ok(WeldingMachineMapper.toDTO(weldingMachineService.updateWeldingMachine(entity)));
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse();
            error.setError("UPDATE_ERROR");
            error.setMessage("Ошибка обновления устройства: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
            summary = "Удалить сварочную машину (мягкое удаление)",
            description = "Выполняет мягкое удаление сварочной машины по её ID. " +
                    "Машина помечается как удаленная, но остается в базе данных. " +
                    "Если машина не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Сварочная машина успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Сварочная машина не найдена",
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
                    description = "Недостаточно прав для удаления сварочной машины",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeldingMachine(
            @Parameter(description = "ID сварочной машины", required = true, example = "1")
            @PathVariable Integer id
    ) {
        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            wt2AccessService.assertCanWriteEquipment(principal);
            wt2AccessService.assertCanAccessWeldingMachine(id, principal);
            weldingMachineService.deleteWeldingMachine(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Удалить сварочную машину (жесткое удаление)",
            description = "Мгновенно скрывает аппарат и освобождает MAC. Полная очистка данных "
                    + "выполняется асинхронно в фоне по ID аппарата."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Сварочная машина успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Сварочная машина не найдена",
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
                    description = "Недостаточно прав для жесткого удаления сварочной машины",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteWeldingMachine(
            @Parameter(description = "ID сварочной машины", required = true, example = "1")
            @PathVariable Integer id
    ) {
        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            wt2AccessService.assertCanWriteEquipment(principal);
            wt2AccessService.assertCanAccessWeldingMachine(id, principal);
            weldingMachineService.hardDeleteWeldingMachine(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("hardDeleteWeldingMachine failed for id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Проверяет уникальность наименования/инв. номера в предприятии и порядок дат. Возвращает ошибку или null. */
    private ResponseEntity<?> validateEquipmentBusinessRules(WeldingMachineDTO dto, Integer organizationUnitId, Integer excludeId) {
        if (organizationUnitId != null
                && weldingMachineService.isNameTakenInOrganization(dto.getName(), organizationUnitId, excludeId)) {
            return businessError("DUPLICATE_ERROR", "Наименование уже используется в этой организации");
        }
        if (organizationUnitId != null
                && dto.getInventoryNumber() != null && !dto.getInventoryNumber().trim().isEmpty()
                && weldingMachineService.isInventoryNumberTakenInOrganization(dto.getInventoryNumber(), organizationUnitId, excludeId)) {
            return businessError("DUPLICATE_ERROR", "Инвентарный номер уже используется в этой организации");
        }
        java.time.LocalDateTime commission = dto.getCommissionDate();
        java.time.LocalDate manufacture = dto.getManufactureDate();
        if (commission != null && manufacture != null && commission.toLocalDate().isBefore(manufacture)) {
            return businessError("VALIDATION_ERROR", "Дата ввода в эксплуатацию не может быть раньше даты изготовления");
        }
        java.time.LocalDateTime lastService = dto.getLastService();
        if (commission != null && lastService != null && lastService.isBefore(commission)) {
            return businessError("VALIDATION_ERROR", "Дата последнего ТО не может быть раньше даты ввода в эксплуатацию");
        }
        return null;
    }

    private ResponseEntity<?> businessError(String code, String message) {
        ErrorResponse error = new ErrorResponse();
        error.setError(code);
        error.setMessage(message);
        return ResponseEntity.badRequest().body(error);
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Welding machine with id 1 not found")
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
