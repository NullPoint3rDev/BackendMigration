package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.QueuePushEvent;
import org.alloy.services.QueuePushEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/queue-push-events")
@Tag(name = "Queue Push Events", description = "API для управления событиями push-уведомлений в очереди. " +
    "Позволяет создавать, просматривать, обновлять и удалять события push-уведомлений, " +
    "которые будут отправлены пользователям системы. Поддерживает различные типы событий " +
    "и их приоритизацию в очереди.")
@SecurityRequirement(name = "JWT")
public class QueuePushEventController {

    @PostConstruct
    public void init() {
        System.out.println("QueuePushEventController initialized!");
    }

    @Autowired
    private QueuePushEventService queuePushEventService;

    @Operation(
        summary = "Получить все события push-уведомлений",
        description = "Возвращает список всех событий push-уведомлений в очереди. " +
                     "События возвращаются с полной информацией о типе, статусе, " +
                     "приоритете и данных для отправки."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список событий успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QueuePushEvent.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку событий",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<QueuePushEvent> getAll() {
        return queuePushEventService.findAll();
    }

    @Operation(
        summary = "Получить событие push-уведомления по ID",
        description = "Возвращает событие push-уведомления по его уникальному идентификатору. " +
                     "Если событие не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Событие успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QueuePushEvent.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Событие не найдено",
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
            description = "Недостаточно прав для доступа к событию",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<QueuePushEvent> getById(
        @Parameter(description = "ID события push-уведомления", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return queuePushEventService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новое событие push-уведомления",
        description = "Создает новое событие push-уведомления в очереди. " +
                     "Событие должно содержать обязательные поля: тип события, " +
                     "данные для отправки, приоритет и другую необходимую информацию."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Событие успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QueuePushEvent.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные события",
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
            description = "Недостаточно прав для создания события",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public QueuePushEvent create(
        @Parameter(description = "Данные события push-уведомления", required = true)
        @RequestBody QueuePushEvent queuePushEvent
    ) {
        return queuePushEventService.save(queuePushEvent);
    }

    @Operation(
        summary = "Обновить событие push-уведомления",
        description = "Обновляет существующее событие push-уведомления по его ID. " +
                     "Можно изменить любые поля события, кроме ID. " +
                     "Если событие не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Событие успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QueuePushEvent.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Событие не найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные события",
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
            description = "Недостаточно прав для обновления события",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<QueuePushEvent> update(
        @Parameter(description = "ID события push-уведомления", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные события", required = true)
        @RequestBody QueuePushEvent queuePushEvent
    ) {
        if (!queuePushEventService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        queuePushEvent.setId(id);
        return ResponseEntity.ok(queuePushEventService.save(queuePushEvent));
    }

    @Operation(
        summary = "Удалить событие push-уведомления",
        description = "Удаляет событие push-уведомления из очереди по его ID. " +
                     "Если событие не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Событие успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Событие не найдено",
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
            description = "Недостаточно прав для удаления события",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID события push-уведомления", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!queuePushEventService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        queuePushEventService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Queue push event with id 1 not found")
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