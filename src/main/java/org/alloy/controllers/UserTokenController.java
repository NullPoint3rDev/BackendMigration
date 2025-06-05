package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.UserToken;
import org.alloy.services.UserTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tokens")
@Tag(name = "User Tokens", description = "API для управления токенами пользователей. " +
    "Позволяет создавать, просматривать, обновлять и удалять токены пользователей в системе. " +
    "Токены используются для аутентификации и авторизации пользователей. " +
    "Каждый токен имеет уникальный идентификатор, связан с пользователем и имеет срок действия.")
@SecurityRequirement(name = "JWT")
public class UserTokenController {

    private final UserTokenService userTokenService;

    @Autowired
    public UserTokenController(UserTokenService userTokenService) {
        this.userTokenService = userTokenService;
    }

    @Operation(
        summary = "Получить все токены пользователей",
        description = "Возвращает список всех токенов пользователей в системе. " +
                     "Каждый токен содержит информацию о пользователе, сроке действия " +
                     "и других связанных данных. Используйте с осторожностью, так как " +
                     "это может быть конфиденциальная информация."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список токенов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserToken.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку токенов",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<UserToken>> getAllTokens() {
        return ResponseEntity.ok(userTokenService.getAllUserTokens());
    }

    @Operation(
        summary = "Получить токен по ID",
        description = "Возвращает токен пользователя по его уникальному идентификатору. " +
                     "Если токен не найден, возвращается 404 ошибка. " +
                     "Токен содержит полную информацию о пользователе и сроке действия."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Токен успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserToken.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Токен не найден",
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
            description = "Недостаточно прав для доступа к токену",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserToken> getTokenById(
        @Parameter(description = "ID токена", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return userTokenService.getUserTokenById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить токены пользователя по ID пользователя",
        description = "Возвращает список всех токенов, связанных с указанным пользователем. " +
                     "Это может быть полезно для управления сессиями пользователя или " +
                     "принудительного выхода из всех устройств."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список токенов пользователя успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserToken.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к токенам пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserToken>> getTokensByUserId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(userTokenService.getUserTokensByUserId(userId));
    }

    @Operation(
        summary = "Получить токен по строке токена",
        description = "Возвращает токен пользователя по его строковому значению (UUID). " +
                     "Если токен не найден, возвращается 404 ошибка. " +
                     "Этот метод часто используется для валидации токена при аутентификации."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Токен успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserToken.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Токен не найден",
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
            description = "Недостаточно прав для доступа к токену",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/token/{token}")
    public ResponseEntity<UserToken> getTokenByTokenString(
        @Parameter(description = "Строковое значение токена (UUID)", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID token
    ) {
        return userTokenService.getUserTokenByToken(token)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новый токен",
        description = "Создает новый токен для пользователя. " +
                     "Токен должен содержать информацию о пользователе и сроке действия. " +
                     "При создании токена проверяется валидность данных пользователя."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Токен успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserToken.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные токена",
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
            description = "Недостаточно прав для создания токена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<UserToken> createToken(
        @Parameter(description = "Данные токена", required = true)
        @RequestBody UserToken userToken
    ) {
        try {
            return ResponseEntity.ok(userTokenService.createUserToken(userToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Обновить токен",
        description = "Обновляет существующий токен по его ID. " +
                     "Можно изменить срок действия и другие параметры токена. " +
                     "Если токен не найден, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Токен успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserToken.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные токена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Токен не найден",
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
            description = "Недостаточно прав для обновления токена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserToken> updateToken(
        @Parameter(description = "ID токена", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные токена", required = true)
        @RequestBody UserToken userToken
    ) {
        try {
            userToken.setId(id);
            return ResponseEntity.ok(userTokenService.updateUserToken(userToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Удалить токен",
        description = "Удаляет токен по его ID. " +
                     "Если токен не найден, возвращается 404 ошибка. " +
                     "Этот метод часто используется для выхода пользователя из системы."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Токен успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Токен не найден",
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
            description = "Недостаточно прав для удаления токена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToken(
        @Parameter(description = "ID токена", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            userTokenService.deleteUserToken(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить все токены пользователя",
        description = "Удаляет все токены, связанные с указанным пользователем. " +
                     "Этот метод часто используется для принудительного выхода " +
                     "пользователя из всех устройств или при сбросе пароля."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все токены пользователя успешно удалены"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления токенов пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllUserTokens(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userId
    ) {
        userTokenService.deleteAllUserTokens(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Очистить истекшие токены",
        description = "Удаляет все токены, срок действия которых истек. " +
                     "Этот метод часто используется в качестве задачи по расписанию " +
                     "для поддержания чистоты базы данных и безопасности системы."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Истекшие токены успешно удалены"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для очистки токенов",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/cleanup/expired")
    public ResponseEntity<Void> cleanupExpiredTokens() {
        userTokenService.deleteExpiredTokens();
        return ResponseEntity.ok().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "User token with id 1 not found")
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
