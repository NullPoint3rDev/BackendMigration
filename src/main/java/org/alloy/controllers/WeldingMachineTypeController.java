package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.GeneralStatus;
import org.alloy.services.WeldingMachineTypeService;
import org.alloy.models.dto.WeldingMachineTypeDTO;
import org.alloy.models.dto.mapper.WeldingMachineTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/welding-machine-types")
@Tag(name = "Welding Machine Types", description = "API для управления типами сварочных машин. " +
    "Позволяет создавать, просматривать, обновлять и удалять типы сварочных машин. " +
    "Каждый тип содержит информацию о настройках, лимитах параметров, входящих и исходящих данных, " +
    "определениях режимов и оповещений. API поддерживает поиск типов и фильтрацию по статусу.")
@SecurityRequirement(name = "JWT")
public class WeldingMachineTypeController {

    @PostConstruct
    public void init() {
        System.out.println("WeldingMachineTypeController initialized!");
    }

    private final WeldingMachineTypeService weldingMachineTypeService;

    @Autowired
    public WeldingMachineTypeController(WeldingMachineTypeService weldingMachineTypeService) {
        this.weldingMachineTypeService = weldingMachineTypeService;
    }

    @Operation(
        summary = "Получить все типы сварочных машин",
        description = "Возвращает список всех типов сварочных машин в системе. " +
                     "Каждый тип содержит информацию о настройках, лимитах параметров, " +
                     "входящих и исходящих данных, определениях режимов и оповещений. " +
                     "Список может быть использован для общего обзора доступных типов машин."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список типов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineTypeDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к типам",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<WeldingMachineTypeDTO>> getAllWeldingMachineTypes() {
        List<WeldingMachineTypeDTO> types = weldingMachineTypeService.getAllWeldingMachineTypes().stream()
            .map(WeldingMachineTypeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @Operation(
        summary = "Получить тип по ID",
        description = "Возвращает тип сварочной машины по его уникальному идентификатору. " +
                     "Если тип не найден, возвращается 404 ошибка. " +
                     "Тип содержит полную информацию о настройках, лимитах параметров, " +
                     "входящих и исходящих данных, определениях режимов и оповещений."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Тип успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineTypeDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Тип не найден",
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
            description = "Недостаточно прав для доступа к типу",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachineTypeDTO> getWeldingMachineTypeById(
        @Parameter(description = "ID типа", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return weldingMachineTypeService.getWeldingMachineTypeById(id)
            .map(WeldingMachineTypeMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить тип по имени",
        description = "Возвращает тип сварочной машины по его имени. " +
                     "Если тип не найден, возвращается 404 ошибка. " +
                     "Поиск по имени чувствителен к регистру и должен точно совпадать."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Тип успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineTypeDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Тип не найден",
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
            description = "Недостаточно прав для доступа к типу",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/name/{name}")
    public ResponseEntity<WeldingMachineTypeDTO> getWeldingMachineTypeByName(
        @Parameter(description = "Имя типа", required = true, example = "Standard Type")
        @PathVariable String name
    ) {
        return weldingMachineTypeService.getWeldingMachineTypeByName(name)
            .map(WeldingMachineTypeMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить типы по статусу",
        description = "Возвращает список типов сварочных машин с указанным статусом. " +
                     "Этот метод используется для фильтрации типов по их активности " +
                     "или другим статусам. Поддерживаются статусы: Active, Inactive, Deleted."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список типов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineTypeDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверный статус",
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
            description = "Недостаточно прав для доступа к типам",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<WeldingMachineTypeDTO>> getWeldingMachineTypesByStatus(
        @Parameter(description = "Статус типа", required = true, example = "Active",
                  schema = @Schema(implementation = GeneralStatus.class))
        @PathVariable GeneralStatus status
    ) {
        List<WeldingMachineTypeDTO> types = weldingMachineTypeService.getWeldingMachineTypesByStatus(status).stream()
            .map(WeldingMachineTypeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @Operation(
        summary = "Поиск типов",
        description = "Выполняет поиск типов сварочных машин по поисковому запросу. " +
                     "Поиск осуществляется по имени и описанию типа. " +
                     "Результаты возвращаются в виде списка соответствующих типов."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Поиск успешно выполнен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineTypeDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для поиска типов",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/search")
    public ResponseEntity<List<WeldingMachineTypeDTO>> searchWeldingMachineTypes(
        @Parameter(description = "Поисковый запрос", required = true, example = "Standard")
        @RequestParam String searchTerm
    ) {
        List<WeldingMachineTypeDTO> types = weldingMachineTypeService.searchWeldingMachineTypes(searchTerm).stream()
            .map(WeldingMachineTypeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @Operation(
        summary = "Создать новый тип",
        description = "Создает новый тип сварочной машины. " +
                     "Тип должен содержать информацию о настройках, лимитах параметров, " +
                     "входящих и исходящих данных, определениях режимов и оповещений. " +
                     "При создании проверяется уникальность имени и валидность данных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Тип успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineTypeDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные типа",
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
            description = "Недостаточно прав для создания типа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<WeldingMachineTypeDTO> createWeldingMachineType(
        @Parameter(description = "Данные типа", required = true)
        @RequestBody WeldingMachineTypeDTO typeDTO
    ) {
        WeldingMachineType entity = WeldingMachineTypeMapper.toEntity(typeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(WeldingMachineTypeMapper.toDTO(weldingMachineTypeService.createWeldingMachineType(entity)));
    }

    @Operation(
        summary = "Обновить тип",
        description = "Обновляет существующий тип сварочной машины. " +
                     "Можно изменить настройки, лимиты параметров, входящие и исходящие данные, " +
                     "определения режимов и оповещений. Если тип не найден, возвращается 404 ошибка. " +
                     "При обновлении проверяется валидность данных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Тип успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineTypeDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Тип не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные типа",
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
            description = "Недостаточно прав для обновления типа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<WeldingMachineTypeDTO> updateWeldingMachineType(
        @Parameter(description = "ID типа", required = true, example = "1")
        @PathVariable Integer id,
        @Parameter(description = "Обновленные данные типа", required = true)
        @RequestBody WeldingMachineTypeDTO typeDTO
    ) {
        if (!weldingMachineTypeService.getWeldingMachineTypeById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        WeldingMachineType entity = WeldingMachineTypeMapper.toEntity(typeDTO);
        entity.setId(id);
        return ResponseEntity.ok(WeldingMachineTypeMapper.toDTO(weldingMachineTypeService.updateWeldingMachineType(entity)));
    }

    @Operation(
        summary = "Удалить тип",
        description = "Выполняет мягкое удаление типа сварочной машины. " +
                     "Тип помечается как удаленный, но остается в базе данных. " +
                     "Если тип не найден, возвращается 404 ошибка. " +
                     "Этот метод следует использовать для обычного удаления типов."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Тип успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Тип не найден",
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
            description = "Недостаточно прав для удаления типа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeldingMachineType(
        @Parameter(description = "ID типа", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            weldingMachineTypeService.deleteWeldingMachineType(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Жесткое удаление типа",
        description = "Выполняет полное удаление типа сварочной машины из базы данных. " +
                     "Этот метод следует использовать с осторожностью, так как " +
                     "удаление необратимо и может повлиять на связанные данные. " +
                     "Если тип не найден, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Тип успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Тип не найден",
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
            description = "Недостаточно прав для жесткого удаления типа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteWeldingMachineType(
        @Parameter(description = "ID типа", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            weldingMachineTypeService.hardDeleteWeldingMachineType(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Welding machine type with id 1 not found")
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
