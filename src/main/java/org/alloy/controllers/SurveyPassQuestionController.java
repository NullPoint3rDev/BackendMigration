package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.SurveyPassQuestion;
import org.alloy.services.SurveyPassQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/survey-pass-questions")
@Tag(name = "Survey Pass Questions", description = "API для управления ответами на вопросы опросов. " +
    "Позволяет создавать, просматривать, обновлять и удалять ответы пользователей " +
    "на вопросы опросов. Каждый ответ связан с конкретным прохождением опроса " +
    "и конкретным вопросом. Поддерживает различные типы ответов в зависимости " +
    "от типа вопроса (текстовые ответы, выбор из вариантов, множественный выбор и т.д.). " +
    "Позволяет отслеживать статистику ответов и анализировать результаты опросов.")
@SecurityRequirement(name = "JWT")
public class SurveyPassQuestionController {
    @Autowired
    private SurveyPassQuestionService surveyPassQuestionService;

    @Operation(
        summary = "Получить все ответы на вопросы опросов",
        description = "Возвращает список всех ответов на вопросы опросов в системе. " +
                     "Ответы возвращаются с полной информацией о прохождении опроса, " +
                     "вопросе, тексте ответа, времени ответа и других связанных данных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список ответов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SurveyPassQuestion.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку ответов",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<SurveyPassQuestion> getAll() {
        return surveyPassQuestionService.findAll();
    }

    @Operation(
        summary = "Получить ответ на вопрос по ID",
        description = "Возвращает ответ на вопрос опроса по его уникальному идентификатору. " +
                     "Если ответ не найден, возвращается 404 ошибка. " +
                     "Возвращаемая информация включает все детали ответа, " +
                     "включая связь с прохождением опроса, вопросом, " +
                     "текстом ответа и временем ответа."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ответ успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SurveyPassQuestion.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Ответ не найден",
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
            description = "Недостаточно прав для доступа к ответу",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<SurveyPassQuestion> getById(
        @Parameter(description = "ID ответа на вопрос", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return surveyPassQuestionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новый ответ на вопрос",
        description = "Создает новый ответ на вопрос опроса. " +
                     "Ответ должен содержать обязательные поля: связь с прохождением опроса, " +
                     "связь с вопросом, текст ответа и другую необходимую информацию. " +
                     "После создания ответ будет связан с конкретным прохождением опроса " +
                     "и конкретным вопросом."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ответ успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SurveyPassQuestion.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные ответа",
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
            description = "Недостаточно прав для создания ответа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public SurveyPassQuestion create(
        @Parameter(description = "Данные ответа на вопрос", required = true)
        @RequestBody SurveyPassQuestion surveyPassQuestion
    ) {
        return surveyPassQuestionService.save(surveyPassQuestion);
    }

    @Operation(
        summary = "Обновить ответ на вопрос",
        description = "Обновляет существующий ответ на вопрос по его ID. " +
                     "Можно изменить любые поля ответа, кроме ID. " +
                     "Если ответ не найден, возвращается 404 ошибка. " +
                     "Обновление может включать изменение текста ответа, " +
                     "времени ответа и других параметров."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ответ успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SurveyPassQuestion.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Ответ не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные ответа",
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
            description = "Недостаточно прав для обновления ответа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<SurveyPassQuestion> update(
        @Parameter(description = "ID ответа на вопрос", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные ответа", required = true)
        @RequestBody SurveyPassQuestion surveyPassQuestion
    ) {
        if (!surveyPassQuestionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyPassQuestion.setId(id);
        return ResponseEntity.ok(surveyPassQuestionService.save(surveyPassQuestion));
    }

    @Operation(
        summary = "Удалить ответ на вопрос",
        description = "Удаляет ответ на вопрос по его ID. " +
                     "Если ответ не найден, возвращается 404 ошибка. " +
                     "Удаление ответа не влияет на прохождение опроса " +
                     "и сам вопрос, но удаляет связь между ними."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Ответ успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Ответ не найден",
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
            description = "Недостаточно прав для удаления ответа",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID ответа на вопрос", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!surveyPassQuestionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyPassQuestionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Survey pass question with id 1 not found")
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