package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.UserAct;
import org.alloy.services.UserActService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/user-acts")
@Tag(name = "User Actions", description = "API для управления действиями пользователей. " +
    "Позволяет отслеживать, создавать и управлять действиями пользователей в системе. " +
    "Поддерживает фильтрацию действий по пользователю, типу действия и временному диапазону. " +
    "Включает функционал для подсчета действий и очистки устаревших записей.")
@SecurityRequirement(name = "JWT")
public class UserActController {

    @PostConstruct
    public void init() {
        System.out.println("UserActController initialized!");
    }

    private final UserActService userActService;

    @Autowired
    public UserActController(UserActService userActService) {
        this.userActService = userActService;
    }

    @Operation(
        summary = "Получить все действия пользователей",
        description = "Возвращает список всех действий пользователей в системе. " +
                     "Действия возвращаются с полной информацией, включая пользователя, " +
                     "тип действия, дату и время выполнения и другие связанные данные."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список действий успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserAct.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку действий",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<UserAct>> getAllUserActs() {
        return ResponseEntity.ok(userActService.getAllUserActs());
    }

    @Operation(
        summary = "Получить действие по ID",
        description = "Возвращает действие пользователя по его уникальному идентификатору. " +
                     "Если действие не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Действие успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserAct.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Действие не найдено",
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
            description = "Недостаточно прав для доступа к действию",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserAct> getUserActById(
        @Parameter(description = "ID действия", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return userActService.getUserActById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить действия пользователя",
        description = "Возвращает список всех действий указанного пользователя. " +
                     "Действия сортируются по дате и времени выполнения в порядке убывания."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список действий успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserAct.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к действиям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAct>> getUserActsByUserId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(userActService.getUserActsByUserId(userId));
    }

    @Operation(
        summary = "Получить действия пользователя по типу",
        description = "Возвращает список действий указанного пользователя определенного типа. " +
                     "Действия сортируются по дате и времени выполнения в порядке убывания."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список действий успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserAct.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к действиям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<UserAct>> getUserActsByUserIdAndType(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userId,
        
        @Parameter(description = "Тип действия", required = true, example = "LOGIN")
        @PathVariable String type
    ) {
        return ResponseEntity.ok(userActService.getUserActsByUserIdAndType(userId, type));
    }

    @Operation(
        summary = "Получить действия пользователя за период",
        description = "Возвращает список действий указанного пользователя за указанный период времени. " +
                     "Действия сортируются по дате и времени выполнения в порядке убывания."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список действий успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserAct.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный формат даты или некорректный временной диапазон",
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
            description = "Недостаточно прав для доступа к действиям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<UserAct>> getUserActsByUserIdAndDateRange(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userId,
        
        @Parameter(description = "Начальная дата (ISO 8601)", required = true, example = "2024-01-01T00:00:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        
        @Parameter(description = "Конечная дата (ISO 8601)", required = true, example = "2024-12-31T23:59:59")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return ResponseEntity.ok(userActService.getUserActsByUserIdAndDateRange(userId, startDate, endDate));
    }

    @Operation(
        summary = "Получить действия пользователя по типу за период",
        description = "Возвращает список действий указанного пользователя определенного типа " +
                     "за указанный период времени. Действия сортируются по дате и времени " +
                     "выполнения в порядке убывания."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список действий успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserAct.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный формат даты или некорректный временной диапазон",
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
            description = "Недостаточно прав для доступа к действиям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userId}/type/{type}/date-range")
    public ResponseEntity<List<UserAct>> getUserActsByUserIdAndTypeAndDateRange(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userId,
        
        @Parameter(description = "Тип действия", required = true, example = "LOGIN")
        @PathVariable String type,
        
        @Parameter(description = "Начальная дата (ISO 8601)", required = true, example = "2024-01-01T00:00:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        
        @Parameter(description = "Конечная дата (ISO 8601)", required = true, example = "2024-12-31T23:59:59")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return ResponseEntity.ok(userActService.getUserActsByUserIdAndTypeAndDateRange(userId, type, startDate, endDate));
    }

    @Operation(
        summary = "Подсчитать количество действий пользователя",
        description = "Возвращает количество действий указанного пользователя определенного типа " +
                     "за указанный период времени. Используется для статистики и аналитики."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Количество действий успешно подсчитано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "integer", format = "int64"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный формат даты или некорректный временной диапазон",
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
            description = "Недостаточно прав для подсчета действий пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userId}/type/{type}/count")
    public ResponseEntity<Long> countUserActsByUserIdAndTypeAndDateRange(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userId,
        
        @Parameter(description = "Тип действия", required = true, example = "LOGIN")
        @PathVariable String type,
        
        @Parameter(description = "Начальная дата (ISO 8601)", required = true, example = "2024-01-01T00:00:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        
        @Parameter(description = "Конечная дата (ISO 8601)", required = true, example = "2024-12-31T23:59:59")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return ResponseEntity.ok(userActService.countUserActsByUserIdAndTypeAndDateRange(userId, type, startDate, endDate));
    }

    @Operation(
        summary = "Создать новое действие пользователя",
        description = "Создает новую запись о действии пользователя. " +
                     "Действие должно содержать обязательные поля: пользователь, " +
                     "тип действия, дату и время выполнения и другие необходимые данные."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Действие успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserAct.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные действия",
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
            description = "Недостаточно прав для создания действия",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<UserAct> createUserAct(
        @Parameter(description = "Данные действия пользователя", required = true)
        @RequestBody UserAct userAct
    ) {
        try {
            return ResponseEntity.ok(userActService.createUserAct(userAct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Обновить действие пользователя",
        description = "Обновляет существующую запись о действии пользователя по его ID. " +
                     "Можно изменить любые поля действия, кроме ID. " +
                     "Если действие не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Действие успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserAct.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Действие не найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные действия",
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
            description = "Недостаточно прав для обновления действия",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserAct> updateUserAct(
        @Parameter(description = "ID действия", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные действия", required = true)
        @RequestBody UserAct userAct
    ) {
        try {
            userAct.setId(id);
            return ResponseEntity.ok(userActService.updateUserAct(userAct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить действие пользователя",
        description = "Удаляет запись о действии пользователя по его ID. " +
                     "Если действие не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Действие успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Действие не найдено",
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
            description = "Недостаточно прав для удаления действия",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserAct(
        @Parameter(description = "ID действия", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            userActService.deleteUserAct(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить все действия пользователя",
        description = "Удаляет все записи о действиях указанного пользователя. " +
                     "Используется для очистки истории действий пользователя."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все действия пользователя успешно удалены"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления действий пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllUserActs(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userId
    ) {
        userActService.deleteAllUserActs(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Очистить устаревшие действия",
        description = "Удаляет все записи о действиях пользователей, которые были выполнены " +
                     "до указанной даты. Используется для периодической очистки старых записей."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Устаревшие действия успешно удалены"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный формат даты",
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
            description = "Недостаточно прав для очистки устаревших действий",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanupOldUserActs(
        @Parameter(description = "Дата, до которой нужно удалить действия (ISO 8601)", 
                  required = true, 
                  example = "2023-12-31T23:59:59")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date
    ) {
        userActService.cleanupOldUserActs(date);
        return ResponseEntity.ok().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "User act with id 1 not found")
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
