package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.SurveyQuestion;
import org.alloy.models.dto.SurveyQuestionDTO;
import org.alloy.models.dto.mapper.SurveyQuestionMapper;
import org.alloy.services.SurveyQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/survey-questions")
@Tag(name = "Survey Questions", description = "API для управления вопросами опросов. " +
    "Позволяет создавать, просматривать, обновлять и удалять вопросы, " +
    "которые используются в опросах. Поддерживает различные типы вопросов: " +
    "текстовые вопросы, вопросы с выбором одного ответа, вопросы с множественным выбором, " +
    "шкалы оценок и другие. Каждый вопрос может иметь свои настройки валидации, " +
    "обязательности ответа, порядка отображения и другие параметры. " +
    "Вопросы могут быть сгруппированы по категориям и иметь связи с другими вопросами.")
@SecurityRequirement(name = "JWT")
public class SurveyQuestionController {

    @PostConstruct
    public void init() {
        System.out.println("SurveyQuestionController initialized!");
    }

    @Autowired
    private SurveyQuestionService surveyQuestionService;

    @Operation(
        summary = "Получить все вопросы опросов",
        description = "Возвращает список всех вопросов опросов в системе. " +
                     "Вопросы возвращаются с полной информацией о тексте вопроса, " +
                     "типе вопроса, настройках валидации, обязательности ответа, " +
                     "порядке отображения и других параметрах."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список вопросов успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SurveyQuestionDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку вопросов",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public List<SurveyQuestionDTO> getAll() {
        return surveyQuestionService.findAll().stream().map(SurveyQuestionMapper::toDTO).collect(Collectors.toList());
    }

    @Operation(
        summary = "Получить вопрос по ID",
        description = "Возвращает вопрос опроса по его уникальному идентификатору. " +
                     "Если вопрос не найден, возвращается 404 ошибка. " +
                     "Возвращаемая информация включает все детали вопроса, " +
                     "включая текст, тип, настройки валидации, обязательность ответа, " +
                     "порядок отображения и другие параметры."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Вопрос успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SurveyQuestionDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Вопрос не найден",
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
            description = "Недостаточно прав для доступа к вопросу",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<SurveyQuestionDTO> getById(
        @Parameter(description = "ID вопроса", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return surveyQuestionService.findById(id)
                .map(SurveyQuestionMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новый вопрос",
        description = "Создает новый вопрос опроса. " +
                     "Вопрос должен содержать обязательные поля: текст вопроса, " +
                     "тип вопроса, настройки валидации и другие параметры. " +
                     "После создания вопрос будет доступен для использования в опросах " +
                     "согласно установленным настройкам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Вопрос успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SurveyQuestionDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные вопроса",
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
            description = "Недостаточно прав для создания вопроса",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public SurveyQuestionDTO create(
        @Parameter(description = "Данные вопроса", required = true)
        @RequestBody SurveyQuestionDTO surveyQuestionDTO
    ) {
        SurveyQuestion entity = SurveyQuestionMapper.toEntity(surveyQuestionDTO);
        return SurveyQuestionMapper.toDTO(surveyQuestionService.save(entity));
    }

    @Operation(
        summary = "Обновить вопрос",
        description = "Обновляет существующий вопрос по его ID. " +
                     "Можно изменить любые поля вопроса, кроме ID. " +
                     "Если вопрос не найден, возвращается 404 ошибка. " +
                     "Обновление может включать изменение текста вопроса, " +
                     "типа вопроса, настроек валидации и других параметров."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Вопрос успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = SurveyQuestionDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Вопрос не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные вопроса",
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
            description = "Недостаточно прав для обновления вопроса",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<SurveyQuestionDTO> update(
        @Parameter(description = "ID вопроса", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные вопроса", required = true)
        @RequestBody SurveyQuestionDTO surveyQuestionDTO
    ) {
        if (!surveyQuestionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        SurveyQuestion entity = SurveyQuestionMapper.toEntity(surveyQuestionDTO);
        entity.setId(id);
        return ResponseEntity.ok(SurveyQuestionMapper.toDTO(surveyQuestionService.save(entity)));
    }

    @Operation(
        summary = "Удалить вопрос",
        description = "Удаляет вопрос по его ID. " +
                     "Если вопрос не найден, возвращается 404 ошибка. " +
                     "Удаление вопроса может повлиять на опросы, " +
                     "в которых он используется, и на ответы пользователей."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Вопрос успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Вопрос не найден",
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
            description = "Недостаточно прав для удаления вопроса",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID вопроса", required = true, example = "1")
        @PathVariable Integer id
    ) {
        if (!surveyQuestionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        surveyQuestionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Survey question with id 1 not found")
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