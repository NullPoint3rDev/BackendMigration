package org.alloy.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.dto.OrganizationUnitDTO;
import org.alloy.models.dto.mapper.OrganizationUnitMapper;
import org.alloy.services.OrganizationUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/organization-units")
@Tag(name = "Organization Units", description = "API для управления подразделениями организаций. " +
    "Позволяет создавать, просматривать, обновлять и удалять подразделения, " +
    "а также выполнять поиск по различным параметрам. Поддерживает иерархическую структуру " +
    "подразделений и их привязку к организациям. Поддерживает как мягкое, " +
    "так и жесткое удаление подразделений.")
@SecurityRequirement(name = "JWT")
public class OrganizationUnitController {

    @PostConstruct
    public void init() {
        System.out.println("OrganizationUnitController initialized!");
    }

    private final OrganizationUnitService organizationUnitService;

    @Autowired
    public OrganizationUnitController(OrganizationUnitService organizationUnitService) {
        this.organizationUnitService = organizationUnitService;
    }

    @Operation(
        summary = "Получить все подразделения",
        description = "Возвращает список всех подразделений в системе. " +
                     "Подразделения возвращаются с полной информацией о структуре, " +
                     "иерархии и привязке к организациям."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список подразделений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrganizationUnitDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Требуется аутентификация",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для доступа к списку подразделений",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<OrganizationUnitDTO>> getAllOrganizationUnits() {
        List<OrganizationUnitDTO> organizationUnits = organizationUnitService.getAllOrganizationUnits().stream()
            .map(OrganizationUnitMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(organizationUnits);
    }

    @Operation(
        summary = "Получить подразделение по ID",
        description = "Возвращает подразделение по его уникальному идентификатору. " +
                     "Если подразделение не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Подразделение успешно найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrganizationUnitDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Подразделение не найдено",
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
            description = "Недостаточно прав для доступа к подразделению",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationUnitDTO> getOrganizationUnitById(
        @Parameter(description = "ID подразделения", required = true, example = "1")
        @PathVariable Integer id
    ) {
        return organizationUnitService.getOrganizationUnitById(id)
            .map(OrganizationUnitMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Получить подразделения организации",
        description = "Возвращает список всех подразделений, принадлежащих указанной организации. " +
                     "Подразделения возвращаются с полной информацией о структуре и иерархии."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список подразделений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrganizationUnitDTO.class, type = "array"))
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
            description = "Недостаточно прав для доступа к подразделениям организации",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<OrganizationUnitDTO>> getOrganizationUnitsByOrganizationId(
        @Parameter(description = "ID организации", required = true, example = "1")
        @PathVariable Integer organizationId
    ) {
        List<OrganizationUnitDTO> organizationUnits = organizationUnitService.getOrganizationUnitsByOrganizationId(organizationId).stream()
            .map(OrganizationUnitMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(organizationUnits);
    }

    @Operation(
        summary = "Получить дочерние подразделения",
        description = "Возвращает список всех подразделений, являющихся дочерними для указанного родительского подразделения. " +
                     "Позволяет построить иерархическую структуру подразделений."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список дочерних подразделений успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrganizationUnitDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Родительское подразделение не найдено",
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
            description = "Недостаточно прав для доступа к дочерним подразделениям",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<OrganizationUnitDTO>> getOrganizationUnitsByParentId(
        @Parameter(description = "ID родительского подразделения", required = true, example = "1")
        @PathVariable Integer parentId
    ) {
        List<OrganizationUnitDTO> organizationUnits = organizationUnitService.getOrganizationUnitsByParentId(parentId).stream()
            .map(OrganizationUnitMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(organizationUnits);
    }

    @Operation(
        summary = "Поиск подразделений",
        description = "Выполняет поиск подразделений в рамках указанной организации по заданному поисковому запросу. " +
                     "Поиск осуществляется по названию, описанию и другим релевантным полям. " +
                     "Результаты возвращаются в виде списка подразделений, отсортированных по релевантности."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Поиск успешно выполнен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrganizationUnitDTO.class, type = "array"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные параметры поиска",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
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
            description = "Недостаточно прав для поиска подразделений",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/search")
    public ResponseEntity<List<OrganizationUnitDTO>> searchOrganizationUnits(
        @Parameter(description = "ID организации", required = true, example = "1")
        @RequestParam Integer organizationId,
        
        @Parameter(description = "Поисковый запрос", required = true, example = "Отдел разработки")
        @RequestParam String searchTerm
    ) {
        List<OrganizationUnitDTO> organizationUnits = organizationUnitService.searchOrganizationUnits(organizationId, searchTerm).stream()
            .map(OrganizationUnitMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(organizationUnits);
    }

    @Operation(
        summary = "Создать новое подразделение",
        description = "Создает новое подразделение в системе. " +
                     "Подразделение должно содержать обязательные поля: название, " +
                     "привязку к организации и другую необходимую информацию. " +
                     "Можно указать родительское подразделение для создания иерархии."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Подразделение успешно создано",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrganizationUnitDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные подразделения",
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
            description = "Недостаточно прав для создания подразделения",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<OrganizationUnitDTO> createOrganizationUnit(
        @Parameter(description = "Данные подразделения", required = true)
        @RequestBody OrganizationUnitDTO organizationUnitDTO
    ) {
        OrganizationUnit entity = OrganizationUnitMapper.toEntity(organizationUnitDTO);
        return new ResponseEntity<>(OrganizationUnitMapper.toDTO(organizationUnitService.createOrganizationUnit(entity)), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Обновить подразделение",
        description = "Обновляет существующее подразделение по его ID. " +
                     "Можно изменить любые поля подразделения, кроме ID. " +
                     "Если подразделение не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Подразделение успешно обновлено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrganizationUnitDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Подразделение не найдено",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные подразделения",
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
            description = "Недостаточно прав для обновления подразделения",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationUnitDTO> updateOrganizationUnit(
        @Parameter(description = "ID подразделения", required = true, example = "1")
        @PathVariable Integer id,
        @Parameter(description = "Обновленные данные подразделения", required = true)
        @RequestBody OrganizationUnitDTO organizationUnitDTO
    ) {
        OrganizationUnit entity = OrganizationUnitMapper.toEntity(organizationUnitDTO);
        entity.setId(id);
        return ResponseEntity.ok(OrganizationUnitMapper.toDTO(organizationUnitService.updateOrganizationUnit(entity)));
    }

    @Operation(
        summary = "Удалить подразделение (мягкое удаление)",
        description = "Выполняет мягкое удаление подразделения по его ID. " +
                     "Подразделение помечается как удаленное, но данные сохраняются в базе. " +
                     "Если подразделение не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Подразделение успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Подразделение не найдено",
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
            description = "Недостаточно прав для удаления подразделения",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganizationUnit(
        @Parameter(description = "ID подразделения", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            organizationUnitService.deleteOrganizationUnit(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Удалить подразделение (жесткое удаление)",
        description = "Выполняет полное удаление подразделения и всех связанных данных из базы. " +
                     "Операция необратима. Если подразделение не найдено, возвращается 404 ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Подразделение успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Подразделение не найдено",
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
            description = "Недостаточно прав для жесткого удаления подразделения",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteOrganizationUnit(
        @Parameter(description = "ID подразделения", required = true, example = "1")
        @PathVariable Integer id
    ) {
        try {
            organizationUnitService.hardDeleteOrganizationUnit(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Schema(description = "Модель ответа с ошибкой")
    public static class ErrorResponse {
        @Schema(description = "Тип ошибки", example = "Not Found")
        private String error;

        @Schema(description = "Сообщение об ошибке", example = "Organization unit with id 1 not found")
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
