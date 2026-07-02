package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.EmailTemplate;
import org.alloy.services.EmailTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/email-templates")
@Tag(name = "Email Templates", description = "API для управления шаблонами электронных писем. " +
    "Позволяет создавать, просматривать, обновлять и удалять шаблоны писем, " +
    "которые используются для автоматической отправки уведомлений пользователям системы.")
@SecurityRequirement(name = "JWT")
public class EmailTemplateController {

    @PostConstruct
    public void init() {
        System.out.println("EmailTemplateController initialized!");
    }

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Operation(
        summary = "Получить все шаблоны писем",
        description = "Возвращает список всех доступных шаблонов электронных писем. " +
                     "Шаблоны могут содержать переменные, которые заменяются на реальные значения " +
                     "при отправке письма. Каждый шаблон имеет уникальный идентификатор и может " +
                     "быть использован для различных типов уведомлений."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список шаблонов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmailTemplate.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к шаблонам",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<EmailTemplate> getAll() {
        return emailTemplateService.findAll();
    }

    @Operation(
        summary = "Получить шаблон по ID",
        description = "Возвращает шаблон электронного письма по его уникальному идентификатору. " +
                     "Шаблон содержит текст письма, тему и метаданные. " +
                     "Если шаблон не найден, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Шаблон успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmailTemplate.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Шаблон не найден",
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
            description = "Недостаточно прав для доступа к шаблону",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplate> getById(
        @Parameter(description = "ID шаблона", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return emailTemplateService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новый шаблон",
        description = "Создает новый шаблон электронного письма. " +
                     "Шаблон должен содержать тему письма, текст шаблона и может включать " +
                     "дополнительные метаданные. Текст шаблона может содержать переменные " +
                     "в формате ${variableName}, которые будут заменены на реальные значения " +
                     "при отправке письма."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Шаблон успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmailTemplate.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные шаблона",
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
            description = "Недостаточно прав для создания шаблона",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public EmailTemplate create(
        @Parameter(description = "Данные шаблона", required = true)
        @RequestBody EmailTemplate emailTemplate
    ) {
        return emailTemplateService.save(emailTemplate);
    }

    @Operation(
        summary = "Обновить существующий шаблон",
        description = "Обновляет данные существующего шаблона по его ID. " +
                     "Можно изменить тему, текст шаблона и метаданные. " +
                     "Если шаблон не найден, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Шаблон успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmailTemplate.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Шаблон не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные шаблона",
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
            description = "Недостаточно прав для обновления шаблона",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplate> update(
        @Parameter(description = "ID шаблона", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные шаблона", required = true)
        @RequestBody EmailTemplate emailTemplate
    ) {
        if (!emailTemplateService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        emailTemplate.setId(id);
        return ResponseEntity.ok(emailTemplateService.save(emailTemplate));
    }

    @Operation(
        summary = "Удалить шаблон",
        description = "Удаляет шаблон электронного письма по его ID. " +
                     "Удаление шаблона не влияет на уже отправленные письма. " +
                     "Если шаблон не найден, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Шаблон успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Шаблон не найден",
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
            description = "Недостаточно прав для удаления шаблона",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID шаблона", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!emailTemplateService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        emailTemplateService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Email template with id 1 not found")
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