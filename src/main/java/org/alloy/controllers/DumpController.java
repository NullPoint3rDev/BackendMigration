package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.Dump;
import org.alloy.models.dto.DumpDTO;
import org.alloy.models.dto.mapper.DumpMapper;
import org.alloy.services.DumpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dumps")
@Tag(name = "Dump Management", description = "API для управления дампами системы")
@SecurityRequirement(name = "JWT")
public class DumpController {
    @Autowired
    private DumpService dumpService;

    @Operation(
        summary = "Получить все дампы",
        description = "Возвращает список всех дампов в системе. " +
                     "Дампы могут содержать различные типы данных, такие как логи, состояния системы, " +
                     "или другие диагностические информации."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список дампов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DumpDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<DumpDTO> getAll() {
        return dumpService.findAll().stream().map(DumpMapper::toDTO).collect(Collectors.toList());
    }

    @Operation(
        summary = "Получить дамп по ID",
        description = "Возвращает дамп по его уникальному идентификатору. " +
                     "Если дамп не найден, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Дамп успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DumpDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Дамп не найден",
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
            description = "Недостаточно прав для доступа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<DumpDTO> getById(
        @Parameter(description = "ID дампа", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return dumpService.findById(id)
                .map(dump -> ResponseEntity.ok(DumpMapper.toDTO(dump)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новый дамп",
        description = "Создает новый дамп в системе. " +
                     "Все обязательные поля должны быть заполнены в соответствии с требованиями."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Дамп успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DumpDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные дампа",
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
            description = "Недостаточно прав для создания дампа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public DumpDTO create(
        @Parameter(description = "Данные дампа", required = true)
        @RequestBody DumpDTO dumpDTO
    ) {
        Dump entity = DumpMapper.toEntity(dumpDTO);
        return DumpMapper.toDTO(dumpService.save(entity));
    }

    @Operation(
        summary = "Обновить существующий дамп",
        description = "Обновляет данные существующего дампа по его ID. " +
                     "Если дамп не найден, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Дамп успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DumpDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Дамп не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные дампа",
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
            description = "Недостаточно прав для обновления дампа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<DumpDTO> update(
        @Parameter(description = "ID дампа", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные дампа", required = true)
        @RequestBody DumpDTO dumpDTO
    ) {
        if (!dumpService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Dump entity = DumpMapper.toEntity(dumpDTO);
        entity.setId(id);
        return ResponseEntity.ok(DumpMapper.toDTO(dumpService.save(entity)));
    }

    @Operation(
        summary = "Удалить дамп",
        description = "Удаляет дамп по его ID. " +
                     "Если дамп не найден, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Дамп успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Дамп не найден",
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
            description = "Недостаточно прав для удаления дампа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID дампа", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!dumpService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        dumpService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Dump with id 1 not found")
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