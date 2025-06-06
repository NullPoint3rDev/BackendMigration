package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.Notification;
import org.alloy.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "API для управления уведомлениями пользователей. " +
    "Позволяет создавать, просматривать, обновлять и удалять уведомления, " +
    "а также управлять статусом их прочтения. Поддерживает различные типы уведомлений " +
    "и фильтрацию по пользователям.")
@SecurityRequirement(name = "JWT")
public class NotificationController {

    @PostConstruct
    public void init() {
        System.out.println("NotificationController initialized!");
    }

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
        summary = "Получить все уведомления",
        description = "Возвращает список всех уведомлений в системе. " +
                     "Уведомления могут быть разных типов и принадлежать разным пользователям."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список уведомлений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Notification.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к уведомлениям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @Operation(
        summary = "Получить уведомление по ID",
        description = "Возвращает уведомление по его уникальному идентификатору. " +
                     "Если уведомление не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Уведомление успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Notification.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Уведомление не найдено",
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
            description = "Недостаточно прав для доступа к уведомлению",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(
        @Parameter(description = "ID уведомления", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return notificationService.getNotificationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить уведомления пользователя",
        description = "Возвращает список всех уведомлений для указанного пользователя. " +
                     "Уведомления возвращаются в хронологическом порядке."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список уведомлений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Notification.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к уведомлениям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userAccountId}")
    public ResponseEntity<List<Notification>> getNotificationsByUserAccountId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserAccountId(userAccountId));
    }

    @Operation(
        summary = "Получить непрочитанные уведомления пользователя",
        description = "Возвращает список всех непрочитанных уведомлений для указанного пользователя. " +
                     "Уведомления возвращаются в хронологическом порядке."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список непрочитанных уведомлений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Notification.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к уведомлениям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userAccountId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotificationsByUserAccountId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsByUserAccountId(userAccountId));
    }

    @Operation(
        summary = "Получить уведомления пользователя по типу",
        description = "Возвращает список уведомлений указанного типа для пользователя. " +
                     "Уведомления возвращаются в хронологическом порядке."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список уведомлений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Notification.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к уведомлениям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userAccountId}/type/{type}")
    public ResponseEntity<List<Notification>> getNotificationsByUserAccountIdAndType(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId,
        
        @Parameter(description = "Тип уведомления", required = true, example = "SYSTEM",
            schema = @Schema(allowableValues = {"SYSTEM", "ALERT", "MAINTENANCE", "WARNING"}))
        @PathVariable String type
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserAccountIdAndType(userAccountId, type));
    }

    @Operation(
        summary = "Получить количество непрочитанных уведомлений",
        description = "Возвращает количество непрочитанных уведомлений для указанного пользователя."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Количество непрочитанных уведомлений успешно получено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "integer", format = "int64"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к уведомлениям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userAccountId}/unread/count")
    public ResponseEntity<Long> countUnreadNotificationsByUserAccountId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        return ResponseEntity.ok(notificationService.countUnreadNotificationsByUserAccountId(userAccountId));
    }

    @Operation(
        summary = "Создать новое уведомление",
        description = "Создает новое уведомление в системе. " +
                     "Уведомление должно содержать ID получателя, тип, заголовок и содержимое."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Уведомление успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Notification.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные уведомления",
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
            description = "Недостаточно прав для создания уведомления",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<Notification> createNotification(
        @Parameter(description = "Данные уведомления", required = true)
        @RequestBody Notification notification
    ) {
        try {
            Notification createdNotification = notificationService.createNotification(notification);
            return new ResponseEntity<>(createdNotification, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Обновить уведомление",
        description = "Обновляет существующее уведомление по его ID. " +
                     "Можно изменить содержимое, тип и другие параметры уведомления. " +
                     "Если уведомление не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Уведомление успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Notification.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Уведомление не найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные уведомления",
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
            description = "Недостаточно прав для обновления уведомления",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<Notification> updateNotification(
        @Parameter(description = "ID уведомления", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные уведомления", required = true)
        @RequestBody Notification notification
    ) {
        try {
            notification.setId(id);
            Notification updatedNotification = notificationService.updateNotification(notification);
            return ResponseEntity.ok(updatedNotification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Отметить уведомление как прочитанное",
        description = "Отмечает уведомление как прочитанное по его ID. " +
                     "Если уведомление не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Уведомление успешно отмечено как прочитанное",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Notification.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Уведомление не найдено",
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
            description = "Недостаточно прав для обновления уведомления",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markNotificationAsRead(
        @Parameter(description = "ID уведомления", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            Notification updatedNotification = notificationService.markNotificationAsRead(id);
            return ResponseEntity.ok(updatedNotification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Отметить все уведомления пользователя как прочитанные",
        description = "Отмечает все уведомления указанного пользователя как прочитанные."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все уведомления успешно отмечены как прочитанные"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для обновления уведомлений",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/user/{userAccountId}/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        try {
            notificationService.markAllNotificationsAsRead(userAccountId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить уведомление",
        description = "Удаляет уведомление по его ID. " +
                     "Если уведомление не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Уведомление успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Уведомление не найдено",
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
            description = "Недостаточно прав для удаления уведомления",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
        @Parameter(description = "ID уведомления", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить все уведомления пользователя",
        description = "Удаляет все уведомления указанного пользователя."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все уведомления пользователя успешно удалены"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления уведомлений",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/user/{userAccountId}")
    public ResponseEntity<Void> deleteNotificationsByUserAccountId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        notificationService.deleteNotificationsByUserAccountId(userAccountId);
        return ResponseEntity.ok().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Notification with id 1 not found")
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
