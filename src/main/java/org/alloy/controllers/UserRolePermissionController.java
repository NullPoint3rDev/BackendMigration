package org.alloy.controllers;

import org.alloy.models.entities.UserRolePermission;
import org.alloy.services.UserRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-role-permissions")
public class UserRolePermissionController {
    @Autowired
    private UserRolePermissionService userRolePermissionService;

    @GetMapping
    public List<UserRolePermission> getAll() {
        return userRolePermissionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRolePermission> getById(@PathVariable Integer id) {
        return userRolePermissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public UserRolePermission create(@RequestBody UserRolePermission userRolePermission) {
        return userRolePermissionService.save(userRolePermission);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserRolePermission> update(@PathVariable Integer id, @RequestBody UserRolePermission userRolePermission) {
        if (!userRolePermissionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userRolePermission.setId(id);
        return ResponseEntity.ok(userRolePermissionService.save(userRolePermission));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!userRolePermissionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userRolePermissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 