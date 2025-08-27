package org.alloy.controllers;

import org.alloy.models.entities.Employee;
import org.alloy.models.dto.EmployeeDTO;
import org.alloy.models.dto.mapper.EmployeeMapper;
import org.alloy.services.EmployeeService;
import org.alloy.models.GeneralStatus;
import org.alloy.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employees")
@Tag(name = "Сотрудники", description = "API для управления сотрудниками")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Operation(
        summary = "Получить всех сотрудников",
        description = "Возвращает список всех сотрудников, созданных через форму"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список сотрудников успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmployeeDTO.class, type = "array"))
        )
    })
    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<Employee> employees = employeeService.getAllEmployees();
        List<EmployeeDTO> employeeDTOs = employees.stream()
            .map(EmployeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDTOs);
    }

    @Operation(
        summary = "Получить сотрудника по ID",
        description = "Возвращает сотрудника по его уникальному идентификатору"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Сотрудник успешно найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmployeeDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сотрудник не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(
        @Parameter(description = "ID сотрудника", required = true, example = "1")
        @PathVariable Long id
    ) {
        Optional<Employee> employee = employeeService.getEmployeeById(id);
        return employee.map(emp -> ResponseEntity.ok(EmployeeMapper.toDTO(emp)))
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Создать нового сотрудника",
        description = "Создает нового сотрудника с указанными данными"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Сотрудник успешно создан",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmployeeDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(
        @Parameter(description = "Данные сотрудника", required = true)
        @RequestBody EmployeeDTO employeeDTO
    ) {
        try {
            Employee createdEmployee = employeeService.createEmployee(employeeDTO);
            return new ResponseEntity<>(EmployeeMapper.toDTO(createdEmployee), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setStatus(400);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setStatus(500);
            errorResponse.setMessage("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(
        summary = "Обновить сотрудника",
        description = "Обновляет данные существующего сотрудника"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Сотрудник успешно обновлен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmployeeDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сотрудник не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(
        @Parameter(description = "ID сотрудника", required = true, example = "1")
        @PathVariable Long id,
        @Parameter(description = "Данные сотрудника", required = true)
        @RequestBody EmployeeDTO employeeDTO
    ) {
        try {
            Employee updatedEmployee = employeeService.updateEmployee(id, employeeDTO);
            return ResponseEntity.ok(EmployeeMapper.toDTO(updatedEmployee));
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setStatus(400);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setStatus(500);
            errorResponse.setMessage("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(
        summary = "Удалить сотрудника",
        description = "Удаляет сотрудника по его ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Сотрудник успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Сотрудник не найден",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(
        @Parameter(description = "ID сотрудника", required = true, example = "1")
        @PathVariable Long id
    ) {
        System.out.println("Контроллер: Получен запрос на удаление сотрудника с ID: " + id);
        boolean deleted = employeeService.deleteEmployee(id);
        System.out.println("Контроллер: Результат удаления: " + deleted);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
        summary = "Получить сотрудников по статусу",
        description = "Возвращает список сотрудников с указанным статусом"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список сотрудников успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmployeeDTO.class, type = "array"))
        )
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByStatus(
        @Parameter(description = "Статус сотрудников", required = true)
        @PathVariable GeneralStatus status
    ) {
        List<Employee> employees = employeeService.getEmployeesByStatus(status);
        List<EmployeeDTO> employeeDTOs = employees.stream()
            .map(EmployeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDTOs);
    }

    @Operation(
        summary = "Получить сотрудников по подразделению",
        description = "Возвращает список сотрудников указанного подразделения"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список сотрудников успешно получен",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmployeeDTO.class, type = "array"))
        )
    })
    @GetMapping("/organization-unit/{organizationUnitId}")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByOrganizationUnit(
        @Parameter(description = "ID подразделения", required = true, example = "1")
        @PathVariable Long organizationUnitId
    ) {
        List<Employee> employees = employeeService.getEmployeesByOrganizationUnit(organizationUnitId);
        List<EmployeeDTO> employeeDTOs = employees.stream()
            .map(EmployeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDTOs);
    }

    @Operation(
        summary = "Поиск сотрудников",
        description = "Поиск сотрудников по различным критериям"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Результаты поиска успешно получены",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EmployeeDTO.class, type = "array"))
        )
    })
    @GetMapping("/search")
    public ResponseEntity<List<EmployeeDTO>> searchEmployees(
        @Parameter(description = "ФИО для поиска")
        @RequestParam(required = false) String fullName,
        @Parameter(description = "Email для поиска")
        @RequestParam(required = false) String email,
        @Parameter(description = "Должность для поиска")
        @RequestParam(required = false) String position,
        @Parameter(description = "ID подразделения")
        @RequestParam(required = false) Long organizationUnitId,
        @Parameter(description = "ID роли пользователя")
        @RequestParam(required = false) Long userRoleId,
        @Parameter(description = "Статус")
        @RequestParam(required = false) GeneralStatus status
    ) {
        List<Employee> employees = employeeService.searchEmployees(
            fullName, email, position, organizationUnitId, userRoleId, status
        );
        List<EmployeeDTO> employeeDTOs = employees.stream()
            .map(EmployeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDTOs);
    }
}
