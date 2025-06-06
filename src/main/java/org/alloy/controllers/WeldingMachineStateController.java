package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.services.WeldingMachineStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/welding-machine-states")
@Tag(name = "Welding Machine States", description = "API для управления состояниями сварочных машин. " +
    "Позволяет создавать, просматривать, обновлять и удалять состояния сварочных машин. " +
    "Каждое состояние содержит информацию о текущем статусе машины, времени изменения состояния " +
    "и связанных параметрах. API поддерживает отслеживание истории состояний и получение " +
    "текущего состояния машины.")
@SecurityRequirement(name = "JWT")
public class WeldingMachineStateController {

    @PostConstruct
    public void init() {
        System.out.println("WeldingMachineStateController initialized!");
    }

    private final WeldingMachineStateService weldingMachineStateService;

    @Autowired
    public WeldingMachineStateController(WeldingMachineStateService weldingMachineStateService) {
        this.weldingMachineStateService = weldingMachineStateService;
    }

    @Operation(
        summary = "Получить все состояния сварочных машин",
        description = "Возвращает список всех состояний сварочных машин в системе. " +
                     "Каждое состояние содержит информацию о машине, её статусе, " +
                     "времени изменения и связанных параметрах. " +
                     "Список может быть использован для общего мониторинга состояний."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список состояний успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineState.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к состояниям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<WeldingMachineState>> getAllWeldingMachineStates() {
        List<WeldingMachineState> states = weldingMachineStateService.getAllWeldingMachineStates();
        return ResponseEntity.ok(states);
    }

    @Operation(
        summary = "Получить состояние по ID",
        description = "Возвращает состояние сварочной машины по его уникальному идентификатору. " +
                     "Если состояние не найдено, возвращается 404 ошибка. " +
                     "Состояние содержит полную информацию о машине, её статусе, " +
                     "времени изменения и связанных параметрах."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Состояние успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineState.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Состояние не найдено",
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
            description = "Недостаточно прав для доступа к состоянию",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<WeldingMachineState> getWeldingMachineStateById(
        @Parameter(description = "ID состояния", required = true, example = "1")
        @PathVariable Long id
    ) {
        return weldingMachineStateService.getWeldingMachineStateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить состояния по ID машины",
        description = "Возвращает список всех состояний для конкретной сварочной машины. " +
                     "Этот метод используется для получения истории состояний машины " +
                     "или для анализа изменений её статуса во времени."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список состояний успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineState.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сварочная машина не найдена",
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
            description = "Недостаточно прав для доступа к состояниям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/machine/{machineId}")
    public ResponseEntity<List<WeldingMachineState>> getWeldingMachineStatesByMachineId(
        @Parameter(description = "ID сварочной машины", required = true, example = "1")
        @PathVariable Integer machineId
    ) {
        List<WeldingMachineState> states = weldingMachineStateService.getWeldingMachineStatesByMachineId(machineId);
        return ResponseEntity.ok(states);
    }

    @Operation(
        summary = "Получить последнее состояние машины",
        description = "Возвращает последнее известное состояние конкретной сварочной машины. " +
                     "Этот метод используется для получения текущего статуса машины " +
                     "и её актуальных параметров."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Последнее состояние успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineState.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Состояния не найдены",
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
            description = "Недостаточно прав для доступа к состоянию",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/machine/{machineId}/latest")
    public ResponseEntity<WeldingMachineState> getLatestWeldingMachineState(
        @Parameter(description = "ID сварочной машины", required = true, example = "1")
        @PathVariable Integer machineId
    ) {
        return weldingMachineStateService.getLatestWeldingMachineState(machineId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить состояния по статусу",
        description = "Возвращает список состояний конкретной сварочной машины с указанным статусом. " +
                     "Этот метод используется для фильтрации состояний по статусу " +
                     "и анализа времени пребывания машины в определенном состоянии."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список состояний успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineState.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сварочная машина не найдена",
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
            description = "Недостаточно прав для доступа к состояниям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/machine/{machineId}/status/{status}")
    public ResponseEntity<List<WeldingMachineState>> getWeldingMachineStatesByStatus(
        @Parameter(description = "ID сварочной машины", required = true, example = "1")
        @PathVariable Integer machineId,
        
        @Parameter(description = "Статус сварочной машины", required = true, example = "ACTIVE")
        @PathVariable WeldingMachineStatus status
    ) {
        List<WeldingMachineState> states = weldingMachineStateService.getWeldingMachineStatesByStatus(machineId, status);
        return ResponseEntity.ok(states);
    }

    @Operation(
        summary = "Создать новое состояние",
        description = "Создает новое состояние для сварочной машины. " +
                     "Состояние должно содержать информацию о машине, её статусе " +
                     "и времени изменения. При создании проверяется валидность данных " +
                     "и соответствие предыдущему состоянию."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Состояние успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineState.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные состояния",
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
            description = "Недостаточно прав для создания состояния",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<WeldingMachineState> createWeldingMachineState(
        @Parameter(description = "Данные состояния", required = true)
        @RequestBody WeldingMachineState state
    ) {
        try {
            WeldingMachineState createdState = weldingMachineStateService.createWeldingMachineState(state);
            return new ResponseEntity<>(createdState, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Обновить состояние",
        description = "Обновляет существующее состояние сварочной машины. " +
                     "Можно изменить статус, время изменения и другие параметры. " +
                     "Если состояние не найдено, возвращается 404 ошибка. " +
                     "При обновлении проверяется валидность данных."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Состояние успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WeldingMachineState.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Состояние не найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные состояния",
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
            description = "Недостаточно прав для обновления состояния",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<WeldingMachineState> updateWeldingMachineState(
        @Parameter(description = "ID состояния", required = true, example = "1")
        @PathVariable Long id,
        
        @Parameter(description = "Обновленные данные состояния", required = true)
        @RequestBody WeldingMachineState state
    ) {
        try {
            state.setId(id);
            WeldingMachineState updatedState = weldingMachineStateService.updateWeldingMachineState(state);
            return ResponseEntity.ok(updatedState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить состояние",
        description = "Удаляет состояние сварочной машины по его ID. " +
                     "Если состояние не найдено, возвращается 404 ошибка. " +
                     "Этот метод следует использовать с осторожностью, так как " +
                     "удаление состояния может повлиять на историю машины."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Состояние успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Состояние не найдено",
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
            description = "Недостаточно прав для удаления состояния",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeldingMachineState(
        @Parameter(description = "ID состояния", required = true, example = "1")
        @PathVariable Long id
    ) {
        try {
            weldingMachineStateService.deleteWeldingMachineState(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить все состояния машины",
        description = "Удаляет все состояния конкретной сварочной машины. " +
                     "Этот метод следует использовать с осторожностью, так как " +
                     "удаление всех состояний приведет к потере всей истории " +
                     "изменений статуса машины."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Все состояния успешно удалены"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сварочная машина не найдена",
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
            description = "Недостаточно прав для удаления состояний",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/machine/{machineId}")
    public ResponseEntity<Void> deleteAllWeldingMachineStates(
        @Parameter(description = "ID сварочной машины", required = true, example = "1")
        @PathVariable Integer machineId
    ) {
        try {
            weldingMachineStateService.deleteAllWeldingMachineStates(machineId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Welding machine state with id 1 not found")
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
