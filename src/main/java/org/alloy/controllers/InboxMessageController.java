package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.InboxMessage;
import org.alloy.services.InboxMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inbox-messages")
@Tag(name = "Inbox Messages", description = "API для управления входящими сообщениями пользователей. " +
    "Позволяет создавать, просматривать, обновлять и удалять сообщения в личном кабинете пользователя. " +
    "Поддерживает работу с непрочитанными сообщениями и фильтрацию по типам.")
@SecurityRequirement(name = "JWT")
public class InboxMessageController {

    private final InboxMessageService inboxMessageService;

    @Autowired
    public InboxMessageController(InboxMessageService inboxMessageService) {
        this.inboxMessageService = inboxMessageService;
    }

    @Operation(
        summary = "Получить все сообщения",
        description = "Возвращает список всех сообщений в системе. " +
                     "Сообщения могут быть разных типов и принадлежать разным пользователям."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список сообщений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InboxMessage.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к сообщениям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<InboxMessage>> getAllInboxMessages() {
        return ResponseEntity.ok(inboxMessageService.getAllInboxMessages());
    }

    @Operation(
        summary = "Получить сообщение по ID",
        description = "Возвращает сообщение по его уникальному идентификатору. " +
                     "Если сообщение не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Сообщение успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InboxMessage.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сообщение не найдено",
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
            description = "Недостаточно прав для доступа к сообщению",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<InboxMessage> getInboxMessageById(
        @Parameter(description = "ID сообщения", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return inboxMessageService.getInboxMessageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить сообщения пользователя",
        description = "Возвращает список всех сообщений для указанного пользователя. " +
                     "Сообщения возвращаются в хронологическом порядке."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список сообщений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InboxMessage.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к сообщениям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userAccountId}")
    public ResponseEntity<List<InboxMessage>> getMessagesByUserAccountId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        return ResponseEntity.ok(inboxMessageService.getMessagesByUserAccountId(userAccountId));
    }

    @Operation(
        summary = "Получить непрочитанные сообщения пользователя",
        description = "Возвращает список всех непрочитанных сообщений для указанного пользователя. " +
                     "Сообщения возвращаются в хронологическом порядке."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список непрочитанных сообщений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InboxMessage.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к сообщениям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userAccountId}/unread")
    public ResponseEntity<List<InboxMessage>> getUnreadMessagesByUserAccountId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        return ResponseEntity.ok(inboxMessageService.getUnreadMessagesByUserAccountId(userAccountId));
    }

    @Operation(
        summary = "Получить сообщения пользователя по типу",
        description = "Возвращает список сообщений указанного типа для пользователя. " +
                     "Сообщения возвращаются в хронологическом порядке."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список сообщений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InboxMessage.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к сообщениям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userAccountId}/type/{type}")
    public ResponseEntity<List<InboxMessage>> getMessagesByUserAccountIdAndType(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId,
        
        @Parameter(description = "Тип сообщения", required = true, example = "NOTIFICATION")
        @PathVariable String type
    ) {
        return ResponseEntity.ok(inboxMessageService.getMessagesByUserAccountIdAndType(userAccountId, type));
    }

    @Operation(
        summary = "Получить количество непрочитанных сообщений",
        description = "Возвращает количество непрочитанных сообщений для указанного пользователя."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Количество непрочитанных сообщений успешно получено",
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
            description = "Недостаточно прав для доступа к сообщениям пользователя",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/user/{userAccountId}/unread/count")
    public ResponseEntity<Long> countUnreadMessagesByUserAccountId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        return ResponseEntity.ok(inboxMessageService.countUnreadMessagesByUserAccountId(userAccountId));
    }

    @Operation(
        summary = "Создать новое сообщение",
        description = "Создает новое сообщение в системе. " +
                     "Сообщение должно содержать ID получателя, тип и содержимое."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Сообщение успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InboxMessage.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные сообщения",
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
            description = "Недостаточно прав для создания сообщения",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<InboxMessage> createInboxMessage(
        @Parameter(description = "Данные сообщения", required = true)
        @RequestBody InboxMessage message
    ) {
        try {
            return ResponseEntity.ok(inboxMessageService.createInboxMessage(message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Обновить сообщение",
        description = "Обновляет существующее сообщение по его ID. " +
                     "Если сообщение не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Сообщение успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InboxMessage.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сообщение не найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные сообщения",
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
            description = "Недостаточно прав для обновления сообщения",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<InboxMessage> updateInboxMessage(
        @Parameter(description = "ID сообщения", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные сообщения", required = true)
        @RequestBody InboxMessage message
    ) {
        try {
            message.setId(id);
            return ResponseEntity.ok(inboxMessageService.updateInboxMessage(message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Отметить сообщение как прочитанное",
        description = "Отмечает сообщение как прочитанное по его ID. " +
                     "Если сообщение не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Сообщение успешно отмечено как прочитанное",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InboxMessage.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сообщение не найдено",
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
            description = "Недостаточно прав для обновления сообщения",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<InboxMessage> markInboxMessageAsRead(
        @Parameter(description = "ID сообщения", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            return ResponseEntity.ok(inboxMessageService.markInboxMessageAsRead(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Отметить все сообщения пользователя как прочитанные",
        description = "Отмечает все сообщения указанного пользователя как прочитанные."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все сообщения успешно отмечены как прочитанные"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для обновления сообщений",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/user/{userAccountId}/read-all")
    public ResponseEntity<Void> markAllInboxMessagesAsRead(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        inboxMessageService.markAllInboxMessagesAsRead(userAccountId);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Удалить сообщение",
        description = "Удаляет сообщение по его ID. " +
                     "Если сообщение не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Сообщение успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сообщение не найдено",
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
            description = "Недостаточно прав для удаления сообщения",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInboxMessage(
        @Parameter(description = "ID сообщения", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            inboxMessageService.deleteInboxMessage(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить все сообщения пользователя",
        description = "Удаляет все сообщения указанного пользователя."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все сообщения пользователя успешно удалены"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления сообщений",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/user/{userAccountId}")
    public ResponseEntity<Void> deleteMessagesByUserAccountId(
        @Parameter(description = "ID пользователя", required = true, example = "1")
        @PathVariable Integer userAccountId
    ) {
        inboxMessageService.deleteMessagesByUserAccountId(userAccountId);
        return ResponseEntity.ok().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Message with id 1 not found")
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
