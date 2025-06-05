package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.UserPermission;
import org.alloy.services.UserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-permissions")
@Tag(name = "User Permissions", description = "API для управления правами пользователей. " +
    "Позволяет создавать, просматривать, обновлять и удалять права пользователей в системе. " +
    "Права пользователей определяют уровень доступа к различным функциям и ресурсам системы. " +
    "Каждое право имеет уникальный идентификатор и описание.")
@SecurityRequirement(name = "JWT")
public class UserPermissionController {
    @Autowired
    private UserPermissionService userPermissionService;

    @Operation(
        summary = "Получить все права пользователей",
        description = "Возвращает список всех прав пользователей в системе. " +
                     "Права возвращаются с полной информацией, включая идентификатор, " +
                     "название, описание и другие связанные данные."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список прав успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserPermission.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку прав пользователей",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<UserPermission> getAll() {
        return userPermissionService.findAll();
    }

    @Operation(
        summary = "Получить право по ID",
        description = "Возвращает право пользователя по его уникальному идентификатору. " +
                     "Если право не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Право успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserPermission.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Право не найдено",
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
            description = "Недостаточно прав для доступа к праву пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserPermission> getById(
        @Parameter(description = "ID права пользователя", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return userPermissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новое право пользователя",
        description = "Создает новое право пользователя в системе. " +
                     "Право должно содержать обязательные поля: название, " +
                     "описание и другие необходимые данные."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Право успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserPermission.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные права",
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
            description = "Недостаточно прав для создания права пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public UserPermission create(
        @Parameter(description = "Данные права пользователя", required = true)
        @RequestBody UserPermission userPermission
    ) {
        return userPermissionService.save(userPermission);
    }

    @Operation(
        summary = "Обновить право пользователя",
        description = "Обновляет существующее право пользователя по его ID. " +
                     "Можно изменить любые поля права, кроме ID. " +
                     "Если право не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Право успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserPermission.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Право не найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные права",
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
            description = "Недостаточно прав для обновления права пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserPermission> update(
        @Parameter(description = "ID права пользователя", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные права", required = true)
        @RequestBody UserPermission userPermission
    ) {
        if (!userPermissionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userPermission.setId(id);
        return ResponseEntity.ok(userPermissionService.save(userPermission));
    }

    @Operation(
        summary = "Удалить право пользователя",
        description = "Удаляет право пользователя по его ID. " +
                     "Если право не найдено, возвращается 404 ошибка. " +
                     "При удалении права необходимо убедиться, что оно не используется " +
                     "в других частях системы."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Право успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Право не найдено",
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
            description = "Недостаточно прав для удаления права пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID права пользователя", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!userPermissionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userPermissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "User permission with id 1 not found")
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