package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.services.WeldingMachineParameterValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/welding-machine-parameters")
@Tag(name = "Welding Machine Parameters", description = "API для управления параметрами сварочных машин. " +
    "Позволяет создавать, просматривать, обновлять и удалять значения параметров сварочных машин. " +
    "Каждый параметр имеет уникальный идентификатор, связан с состоянием машины и содержит информацию " +
    "о значении, типе, лимитах и превышении лимитов. API поддерживает мониторинг параметров в реальном времени " +
    "и отслеживание превышения допустимых значений.")
@SecurityRequirement(name = "JWT")
public class WeldingMachineParameterValueController {

    private final WeldingMachineParameterValueService parameterValueService;

    @Autowired
    public WeldingMachineParameterValueController(WeldingMachineParameterValueService parameterValueService) {
        this.parameterValueService = parameterValueService;
    }

    @Operation(
        summary = "Получить все значения параметров",
        description = "Возвращает список всех значений параметров сварочных машин в системе. " +
                     "Каждое значение содержит информацию о параметре, его текущем значении, " +
                     "типе данных, лимитах и статусе превышения лимитов. " +
                     "Список может быть использован для общего мониторинга параметров."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список значений параметров успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineParameterValue.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к значениям параметров",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<WeldingMachineParameterValue>> getAllParameterValues() {
        return ResponseEntity.ok(parameterValueService.getAllParameterValues());
    }

    @Operation(
        summary = "Получить значение параметра по ID",
        description = "Возвращает значение параметра по его уникальному идентификатору. " +
                     "Если значение не найдено, возвращается 404 ошибка. " +
                     "Значение содержит полную информацию о параметре, включая текущее значение, " +
                     "тип данных, лимиты и статус превышения лимитов."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Значение параметра успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineParameterValue.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Значение параметра не найдено",
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
            description = "Недостаточно прав для доступа к значению параметра",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachineParameterValue> getParameterValueById(
        @Parameter(description = "ID значения параметра", required = true, example = "1")
        @PathVariable Long id
    ) {
        return parameterValueService.getParameterValueById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить значения параметров по ID состояния",
        description = "Возвращает список всех значений параметров для конкретного состояния сварочной машины. " +
                     "Этот метод используется для получения полной картины параметров машины " +
                     "в определенный момент времени или для анализа изменений параметров."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список значений параметров успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineParameterValue.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Состояние сварочной машины не найдено",
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
            description = "Недостаточно прав для доступа к значениям параметров",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/state/{stateId}")
    public ResponseEntity<List<WeldingMachineParameterValue>> getParameterValuesByStateId(
        @Parameter(description = "ID состояния сварочной машины", required = true, example = "1")
        @PathVariable Long stateId
    ) {
        return ResponseEntity.ok(parameterValueService.getParameterValuesByStateId(stateId));
    }

    @Operation(
        summary = "Получить значение параметра по ID состояния и коду свойства",
        description = "Возвращает значение конкретного параметра для определенного состояния сварочной машины. " +
                     "Этот метод используется для получения значения отдельного параметра " +
                     "в конкретный момент времени или для мониторинга конкретного параметра."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Значение параметра успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineParameterValue.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Значение параметра не найдено",
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
            description = "Недостаточно прав для доступа к значению параметра",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/state/{stateId}/property/{propertyCode}")
    public ResponseEntity<WeldingMachineParameterValue> getParameterValueByStateIdAndPropertyCode(
        @Parameter(description = "ID состояния сварочной машины", required = true, example = "1")
        @PathVariable Long stateId,
        
        @Parameter(description = "Код свойства параметра", required = true, example = "TEMPERATURE")
        @PathVariable String propertyCode
    ) {
        return parameterValueService.getParameterValueByStateIdAndPropertyCode(stateId, propertyCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить превышенные значения параметров",
        description = "Возвращает список параметров, значения которых превышают допустимые лимиты " +
                     "для конкретного состояния сварочной машины. " +
                     "Этот метод используется для мониторинга критических ситуаций " +
                     "и выявления потенциальных проблем."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список превышенных значений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineParameterValue.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Состояние сварочной машины не найдено",
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
            description = "Недостаточно прав для доступа к значениям параметров",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/state/{stateId}/exceeded")
    public ResponseEntity<List<WeldingMachineParameterValue>> getExceededParameterValues(
        @Parameter(description = "ID состояния сварочной машины", required = true, example = "1")
        @PathVariable Long stateId
    ) {
        return ResponseEntity.ok(parameterValueService.getExceededParameterValues(stateId));
    }

    @Operation(
        summary = "Создать новое значение параметра",
        description = "Создает новое значение параметра для сварочной машины. " +
                     "Значение должно содержать информацию о состоянии машины, " +
                     "коде свойства, значении, типе данных и лимитах. " +
                     "При создании проверяется валидность данных и соответствие лимитам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Значение параметра успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineParameterValue.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные значения параметра",
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
            description = "Недостаточно прав для создания значения параметра",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<WeldingMachineParameterValue> createParameterValue(
        @Parameter(description = "Данные значения параметра", required = true)
        @RequestBody WeldingMachineParameterValue parameterValue
    ) {
        try {
            WeldingMachineParameterValue createdParameterValue = parameterValueService.createParameterValue(parameterValue);
            return new ResponseEntity<>(createdParameterValue, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Обновить значение параметра",
        description = "Обновляет существующее значение параметра. " +
                     "Можно изменить значение, лимиты и другие параметры. " +
                     "Если значение не найдено, возвращается 404 ошибка. " +
                     "При обновлении проверяется соответствие новым лимитам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Значение параметра успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineParameterValue.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Значение параметра не найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные значения параметра",
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
            description = "Недостаточно прав для обновления значения параметра",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<WeldingMachineParameterValue> updateParameterValue(
        @Parameter(description = "ID значения параметра", required = true, example = "1")
        @PathVariable Long id,
        
        @Parameter(description = "Обновленные данные значения параметра", required = true)
        @RequestBody WeldingMachineParameterValue parameterValue
    ) {
        try {
            parameterValue.setId(id);
            return ResponseEntity.ok(parameterValueService.updateParameterValue(parameterValue));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить значение параметра",
        description = "Удаляет значение параметра по его ID. " +
                     "Если значение не найдено, возвращается 404 ошибка. " +
                     "Этот метод следует использовать с осторожностью, так как " +
                     "удаление значения параметра может повлиять на историю данных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Значение параметра успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Значение параметра не найдено",
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
            description = "Недостаточно прав для удаления значения параметра",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParameterValue(
        @Parameter(description = "ID значения параметра", required = true, example = "1")
        @PathVariable Long id
    ) {
        try {
            parameterValueService.deleteParameterValue(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить все значения параметров для состояния",
        description = "Удаляет все значения параметров для конкретного состояния сварочной машины. " +
                     "Этот метод следует использовать с осторожностью, так как " +
                     "удаление всех значений может привести к потере исторических данных " +
                     "о параметрах машины в определенный момент времени."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Все значения параметров успешно удалены"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Состояние сварочной машины не найдено",
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
            description = "Недостаточно прав для удаления значений параметров",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/state/{stateId}")
    public ResponseEntity<Void> deleteAllParameterValues(
        @Parameter(description = "ID состояния сварочной машины", required = true, example = "1")
        @PathVariable Long stateId
    ) {
        try {
            parameterValueService.deleteAllParameterValues(stateId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Parameter value with id 1 not found")
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
