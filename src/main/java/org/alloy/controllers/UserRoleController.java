package org.alloy.controllers;

import org.alloy.models.entities.UserRole;
import org.alloy.services.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@Tag(name = "Роли пользователей", description = "API для управления ролями пользователей")
public class UserRoleController {

    @Autowired
    private UserRoleService userRoleService;

    @Operation(
        summary = "Получить все роли",
        description = "Возвращает список всех ролей пользователей в системе"
    )
    @GetMapping
    public ResponseEntity<List<UserRole>> getAllRoles() {
        List<UserRole> roles = userRoleService.getAllUserRoles();
        return ResponseEntity.ok(roles);
    }
}
