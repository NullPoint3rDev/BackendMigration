package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.UserRolePermission;
import org.alloy.models.dto.UserRolePermissionDTO;
import org.alloy.models.dto.mapper.UserRolePermissionMapper;
import org.alloy.services.UserRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user-role-permissions")
@Tag(name = "User Role Permissions", description = "API для управления связями между ролями и правами пользователей. " +
    "Позволяет создавать, просматривать, обновлять и удалять связи между ролями и правами в системе. " +
    "Каждая связь определяет, какие права имеет определенная роль пользователя. " +
    "Связь содержит информацию о правах на чтение и запись для конкретного права в рамках роли.")
@SecurityRequirement(name = "JWT")
public class UserRolePermissionController {

    @PostConstruct
    public void init() {
        System.out.println("UserRolePermissionController initialized!");
    }

    @Autowired
    private UserRolePermissionService userRolePermissionService;

    @Operation(
        summary = "Получить все связи ролей и прав",
        description = "Возвращает список всех связей между ролями и правами в системе. " +
                     "Каждая связь содержит информацию о роли, праве и уровне доступа (чтение/запись). " +
                     "Список может быть использован для управления правами доступа в системе."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список связей успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserRolePermissionDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку связей ролей и прав",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<UserRolePermissionDTO> getAll() {
        return userRolePermissionService.findAll().stream().map(UserRolePermissionMapper::toDTO).collect(Collectors.toList());
    }

    @Operation(
        summary = "Получить связь роли и права по ID",
        description = "Возвращает связь между ролью и правом по её уникальному идентификатору. " +
                     "Если связь не найдена, возвращается 404 ошибка. " +
                     "Связь содержит полную информацию о роли, праве и уровне доступа."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Связь успешно найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserRolePermissionDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Связь не найдена",
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
            description = "Недостаточно прав для доступа к связи роли и права",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserRolePermissionDTO> getById(
        @Parameter(description = "ID связи роли и права", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return userRolePermissionService.findById(id)
                .map(UserRolePermissionMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новую связь роли и права",
        description = "Создает новую связь между ролью и правом в системе. " +
                     "Связь должна содержать информацию о роли, праве и уровне доступа (чтение/запись). " +
                     "При создании связи проверяется существование указанных роли и права."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Связь успешно создана",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserRolePermissionDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные связи",
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
            description = "Недостаточно прав для создания связи роли и права",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<UserRolePermissionDTO> createUserRolePermission(
        @Parameter(description = "Данные разрешения роли пользователя", required = true)
        @RequestBody UserRolePermissionDTO dto
    ) {
        UserRolePermission entity = UserRolePermissionMapper.toEntity(dto);
        return new ResponseEntity<>(UserRolePermissionMapper.toDTO(userRolePermissionService.save(entity)), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Обновить связь роли и права",
        description = "Обновляет существующую связь между ролью и правом по её ID. " +
                     "Можно изменить уровень доступа (чтение/запись) для существующей связи. " +
                     "Если связь не найдена, возвращается 404 ошибка. " +
                     "При обновлении проверяется существование указанных роли и права."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Связь успешно обновлена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserRolePermissionDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Связь не найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные связи",
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
            description = "Недостаточно прав для обновления связи роли и права",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserRolePermissionDTO> updateUserRolePermission(
        @Parameter(description = "ID разрешения роли пользователя", required = true, example = "1")
        @PathVariable Integer id,
        @Parameter(description = "Обновленные данные разрешения роли пользователя", required = true)
        @RequestBody UserRolePermissionDTO dto
    ) {
        UserRolePermission entity = UserRolePermissionMapper.toEntity(dto);
        entity.setId(id);
        return ResponseEntity.ok(UserRolePermissionMapper.toDTO(userRolePermissionService.save(entity)));
    }

    @Operation(
        summary = "Удалить связь роли и права",
        description = "Удаляет связь между ролью и правом по её ID. " +
                     "Если связь не найдена, возвращается 404 ошибка. " +
                     "При удалении связи необходимо убедиться, что это не нарушит " +
                     "функционирование системы и не оставит роль без необходимых прав."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Связь успешно удалена"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Связь не найдена",
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
            description = "Недостаточно прав для удаления связи роли и права",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID связи роли и права", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!userRolePermissionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userRolePermissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "User role permission with id 1 not found")
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