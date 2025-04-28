package org.alloy.controllers;

import org.alloy.models.entities.UserPermission;
import org.alloy.services.UserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-permissions")
public class UserPermissionController {
    @Autowired
    private UserPermissionService userPermissionService;

    @GetMapping
    public List<UserPermission> getAll() {
        return userPermissionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserPermission> getById(@PathVariable Integer id) {
        return userPermissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public UserPermission create(@RequestBody UserPermission userPermission) {
        return userPermissionService.save(userPermission);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserPermission> update(@PathVariable Integer id, @RequestBody UserPermission userPermission) {
        if (!userPermissionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userPermission.setId(id);
        return ResponseEntity.ok(userPermissionService.save(userPermission));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!userPermissionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userPermissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 