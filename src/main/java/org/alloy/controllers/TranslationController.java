package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.Translation;
import org.alloy.services.TranslationService;
import org.alloy.models.dto.TranslationDTO;
import org.alloy.mappers.TranslationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/translations")
@Tag(name = "Translations", description = "API для управления переводами в системе. " +
    "Позволяет создавать, просматривать, обновлять и удалять переводы текстов " +
    "для различных языков. Поддерживает пагинацию для эффективной работы с большими " +
    "объемами переводов. Каждый перевод содержит ключ, язык, текст перевода " +
    "и другие метаданные. Система обеспечивает централизованное управление " +
    "многоязычными текстами для всего приложения.")
@SecurityRequirement(name = "JWT")
public class TranslationController {

    @PostConstruct
    public void init() {
        System.out.println("TranslationController initialized!");
    }

    @Autowired
    private TranslationService translationService;

    @Operation(
        summary = "Получить все переводы",
        description = "Возвращает страницу переводов с поддержкой пагинации. " +
                     "Переводы возвращаются с полной информацией о ключе, языке, " +
                     "тексте перевода и других метаданных. " +
                     "Поддерживает сортировку и фильтрацию результатов."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Страница переводов успешно получена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Translation.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к переводам",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public Page<Translation> getAll(
        @Parameter(description = "Параметры пагинации и сортировки", required = true)
        Pageable pageable
    ) {
        return translationService.findAll(pageable);
    }

    @Operation(
        summary = "Получить перевод по ID",
        description = "Возвращает перевод по его уникальному идентификатору. " +
                     "Если перевод не найден, возвращается 404 ошибка. " +
                     "Возвращаемая информация включает все детали перевода, " +
                     "включая ключ, язык, текст перевода и метаданные."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Перевод успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Translation.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Перевод не найден",
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
            description = "Недостаточно прав для доступа к переводу",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Translation> getById(
        @Parameter(description = "ID перевода", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return translationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать новый перевод",
        description = "Создает новый перевод в системе. " +
                     "Перевод должен содержать обязательные поля: ключ, язык, " +
                     "текст перевода и другие метаданные. " +
                     "После создания перевод будет доступен для использования " +
                     "в приложении согласно установленным настройкам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Перевод успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Translation.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные перевода",
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
            description = "Недостаточно прав для создания перевода",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public TranslationDTO create(
        @Parameter(description = "Данные перевода", required = true)
        @RequestBody TranslationDTO translationDTO
    ) {
        Translation entity = TranslationMapper.toEntity(translationDTO);
        return TranslationMapper.toDTO(translationService.save(entity));
    }

    @Operation(
        summary = "Обновить перевод",
        description = "Обновляет существующий перевод по его ID. " +
                     "Можно изменить любые поля перевода, кроме ID. " +
                     "Если перевод не найден, возвращается 404 ошибка. " +
                     "Обновление может включать изменение ключа, языка, " +
                     "текста перевода и других метаданных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Перевод успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Translation.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Перевод не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные перевода",
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
            description = "Недостаточно прав для обновления перевода",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<TranslationDTO> update(
        @Parameter(description = "ID перевода", required = true, example = "1")
        @PathVariable Integer id,
        @Parameter(description = "Обновленные данные перевода", required = true)
        @RequestBody TranslationDTO translationDTO
    ) {
        if (!translationService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Translation entity = TranslationMapper.toEntity(translationDTO);
        entity.setId(id);
        return ResponseEntity.ok(TranslationMapper.toDTO(translationService.save(entity)));
    }

    @Operation(
        summary = "Удалить перевод",
        description = "Удаляет перевод по его ID. " +
                     "Если перевод не найден, возвращается 404 ошибка. " +
                     "Удаление перевода может повлиять на отображение текстов " +
                     "в приложении для соответствующего языка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Перевод успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Перевод не найден",
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
            description = "Недостаточно прав для удаления перевода",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID перевода", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return translationService.findById(id)
                .map(translation -> {
                    translationService.deleteById(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Translation with id 1 not found")
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