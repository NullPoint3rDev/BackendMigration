package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.Survey;
import org.alloy.services.SurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/surveys")
@Tag(name = "Surveys", description = "API для управления опросами в системе. " +
    "Позволяет создавать, просматривать, обновлять и удалять опросы, " +
    "которые используются для сбора информации от пользователей. " +
    "Поддерживает различные типы опросов, включая анкеты, тесты, " +
    "опросы удовлетворенности и другие формы сбора данных. " +
    "Каждый опрос может содержать множество вопросов разных типов " +
    "и иметь свои настройки доступности и сроков проведения.")
@SecurityRequirement(name = "JWT")
public class SurveyController {
    @Autowired
    private SurveyService surveyService;

    @Operation(
        summary = "Получить все опросы",
        description = "Возвращает список всех опросов в системе. " +
                     "Опросы возвращаются с полной информацией о названии, " +
                     "описании, статусе, сроках проведения, типах вопросов " +
                     "и других связанных данных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список опросов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Survey.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку опросов",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<Survey> getAll() {
        return surveyService.findAll();
    }

    @Operation(
        summary = "Получить опрос по ID",
        description = "Возвращает опрос по его уникальному идентификатору. " +
                     "Если опрос не найден, возвращается 404 ошибка. " +
                     "Возвращаемая информация включает все детали опроса, " +
                     "включая вопросы, настройки, статус и статистику ответов."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Опрос успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Survey.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Опрос не найден",
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
            description = "Недостаточно прав для доступа к опросу",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Survey> getById(
        @Parameter(description = "ID опроса", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return surveyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новый опрос",
        description = "Создает новый опрос в системе. " +
                     "Опрос должен содержать обязательные поля: название, " +
                     "описание, тип опроса, настройки доступности и сроков. " +
                     "После создания опрос будет доступен для заполнения " +
                     "согласно установленным настройкам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Опрос успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Survey.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные опроса",
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
            description = "Недостаточно прав для создания опроса",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public Survey create(
        @Parameter(description = "Данные опроса", required = true)
        @RequestBody Survey survey
    ) {
        return surveyService.save(survey);
    }

    @Operation(
        summary = "Обновить опрос",
        description = "Обновляет существующий опрос по его ID. " +
                     "Можно изменить любые поля опроса, кроме ID. " +
                     "Если опрос не найден, возвращается 404 ошибка. " +
                     "Обновление может включать изменение названия, описания, " +
                     "настроек доступности, сроков проведения и других параметров."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Опрос успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Survey.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Опрос не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные опроса",
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
            description = "Недостаточно прав для обновления опроса",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<Survey> update(
        @Parameter(description = "ID опроса", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные опроса", required = true)
        @RequestBody Survey survey
    ) {
        if (!surveyService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        survey.setId(id);
        return ResponseEntity.ok(surveyService.save(survey));
    }

    @Operation(
        summary = "Удалить опрос",
        description = "Удаляет опрос по его ID. " +
                     "Если опрос не найден, возвращается 404 ошибка. " +
                     "Удаление опроса также удаляет все связанные с ним вопросы " +
                     "и ответы пользователей."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Опрос успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Опрос не найден",
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
            description = "Недостаточно прав для удаления опроса",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID опроса", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!surveyService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Survey with id 1 not found")
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