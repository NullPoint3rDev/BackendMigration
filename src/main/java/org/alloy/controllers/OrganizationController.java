package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.Organization;
import org.alloy.services.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizations", description = "API для управления организациями в системе. " +
    "Позволяет создавать, просматривать, обновлять и удалять организации, " +
    "а также выполнять поиск по различным параметрам. Поддерживает как мягкое, " +
    "так и жесткое удаление организаций.")
@SecurityRequirement(name = "JWT")
public class OrganizationController {

    private final OrganizationService organizationService;

    @Autowired
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Operation(
        summary = "Получить все организации",
        description = "Возвращает список всех организаций в системе. " +
                     "Организации возвращаются с полной информацией о структуре, " +
                     "контактных данных и статусе."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список организаций успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Organization.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку организаций",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        List<Organization> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(organizations);
    }

    @Operation(
        summary = "Получить организацию по ID",
        description = "Возвращает организацию по ее уникальному идентификатору. " +
                     "Если организация не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Организация успешно найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Organization.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Организация не найдена",
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
            description = "Недостаточно прав для доступа к организации",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(
        @Parameter(description = "ID организации", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return organizationService.getOrganizationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Поиск организаций",
        description = "Выполняет поиск организаций по заданному поисковому запросу. " +
                     "Поиск осуществляется по названию, описанию и другим релевантным полям. " +
                     "Результаты возвращаются в виде списка организаций, отсортированных по релевантности."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Поиск успешно выполнен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Organization.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный поисковый запрос",
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
            description = "Недостаточно прав для поиска организаций",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/search")
    public ResponseEntity<List<Organization>> searchOrganizations(
        @Parameter(description = "Поисковый запрос", required = true, example = "ООО ТехноСварка")
        @RequestParam String searchTerm
    ) {
        List<Organization> organizations = organizationService.searchOrganizations(searchTerm);
        return ResponseEntity.ok(organizations);
    }

    @Operation(
        summary = "Создать новую организацию",
        description = "Создает новую организацию в системе. " +
                     "Организация должна содержать обязательные поля: название, " +
                     "контактные данные и другую необходимую информацию."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Организация успешно создана",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Organization.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные организации",
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
            description = "Недостаточно прав для создания организации",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<Organization> createOrganization(
        @Parameter(description = "Данные организации", required = true)
        @RequestBody Organization organization
    ) {
        try {
            Organization createdOrganization = organizationService.createOrganization(organization);
            return new ResponseEntity<>(createdOrganization, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Обновить организацию",
        description = "Обновляет существующую организацию по ее ID. " +
                     "Можно изменить любые поля организации, кроме ID. " +
                     "Если организация не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Организация успешно обновлена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Organization.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Организация не найдена",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные организации",
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
            description = "Недостаточно прав для обновления организации",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrganization(
        @Parameter(description = "ID организации", required = true, example = "1")
        @PathVariable Integer id,
        
        @Parameter(description = "Обновленные данные организации", required = true)
        @RequestBody Organization organization
    ) {
        try {
            organization.setId(id);
            Organization updatedOrganization = organizationService.updateOrganization(organization);
            return ResponseEntity.ok(updatedOrganization);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить организацию (мягкое удаление)",
        description = "Выполняет мягкое удаление организации по ее ID. " +
                     "Организация помечается как удаленная, но данные сохраняются в базе. " +
                     "Если организация не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Организация успешно удалена"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Организация не найдена",
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
            description = "Недостаточно прав для удаления организации",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(
        @Parameter(description = "ID организации", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            organizationService.deleteOrganization(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить организацию (жесткое удаление)",
        description = "Выполняет полное удаление организации и всех связанных данных из базы. " +
                     "Операция необратима. Если организация не найдена, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Организация успешно удалена"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Организация не найдена",
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
            description = "Недостаточно прав для жесткого удаления организации",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteOrganization(
        @Parameter(description = "ID организации", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            organizationService.hardDeleteOrganization(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Organization with id 1 not found")
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
