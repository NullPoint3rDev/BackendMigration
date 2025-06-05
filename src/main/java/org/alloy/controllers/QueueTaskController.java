package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.QueueTask;
import org.alloy.services.QueueTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/queue-tasks")
@Tag(name = "Queue Tasks", description = "API для управления задачами в очереди. " +
    "Позволяет создавать, просматривать, обновлять и удалять задачи, " +
    "которые будут выполнены в системе. Поддерживает различные типы задач, " +
    "их приоритизацию и статусы выполнения. Задачи могут быть связаны с различными " +
    "операциями системы, такими как обработка данных, отправка уведомлений, " +
    "синхронизация и другие фоновые процессы.")
@SecurityRequirement(name = "JWT")
public class QueueTaskController {
    @Autowired
    private QueueTaskService queueTaskService;

    @Operation(
        summary = "Получить все задачи в очереди",
        description = "Возвращает список всех задач в очереди. " +
                     "Задачи возвращаются с полной информацией о типе, статусе, " +
                     "приоритете, времени создания и выполнения, а также связанных данных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список задач успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QueueTask.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку задач",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<QueueTask> getAll() {
        return queueTaskService.findAll();
    }

    @Operation(
        summary = "Получить задачу по ID",
        description = "Возвращает задачу из очереди по ее уникальному идентификатору. " +
                     "Если задача не найдена, возвращается 404 ошибка. " +
                     "Возвращаемая информация включает все детали задачи, включая " +
                     "текущий статус выполнения, ошибки (если есть) и результаты."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Задача успешно найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QueueTask.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Задача не найдена",
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
            description = "Недостаточно прав для доступа к задаче",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<QueueTask> getById(
        @Parameter(description = "ID задачи", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return queueTaskService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новую задачу",
        description = "Создает новую задачу в очереди. " +
                     "Задача должна содержать обязательные поля: тип задачи, " +
                     "приоритет, данные для выполнения и другую необходимую информацию. " +
                     "После создания задача будет помещена в очередь на выполнение " +
                     "согласно установленному приоритету."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Задача успешно создана",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QueueTask.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные задачи",
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
            description = "Недостаточно прав для создания задачи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public QueueTask create(
        @Parameter(description = "Данные задачи", required = true)
        @RequestBody QueueTask queueTask
    ) {
        return queueTaskService.save(queueTask);
    }

    @Operation(
        summary = "Обновить задачу",
        description = "Обновляет существующую задачу по ее ID. " +
                     "Можно изменить любые поля задачи, кроме ID. " +
                     "Если задача не найдена, возвращается 404 ошибка. " +
                     "Обновление может включать изменение приоритета, " +
                     "статуса выполнения или связанных данных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Задача успешно обновлена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QueueTask.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Задача не найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные задачи",
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
            description = "Недостаточно прав для обновления задачи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<QueueTask> update(
        @Parameter(description = "ID задачи", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные задачи", required = true)
        @RequestBody QueueTask queueTask
    ) {
        if (!queueTaskService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        queueTask.setId(id);
        return ResponseEntity.ok(queueTaskService.save(queueTask));
    }

    @Operation(
        summary = "Удалить задачу",
        description = "Удаляет задачу из очереди по ее ID. " +
                     "Если задача не найдена, возвращается 404 ошибка. " +
                     "Удаление задачи также отменяет ее выполнение, " +
                     "если оно еще не было завершено."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Задача успешно удалена"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Задача не найдена",
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
            description = "Недостаточно прав для удаления задачи",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID задачи", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!queueTaskService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        queueTaskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Queue task with id 1 not found")
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