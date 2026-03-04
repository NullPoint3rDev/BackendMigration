package org.alloy.controllers;

import org.alloy.models.User;
import org.alloy.models.entities.UserPermissionGrant;
import org.alloy.repositories.UserPermissionGrantRepository;
import org.alloy.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * API для выдачи и отзыва настраиваемых прав («наст N»).
 * Выдавать право может пользователь с соответствующей ролью (например, Админ Эллой для «наст 1»).
 */
@RestController
@RequestMapping("/user-permission-grants")
public class UserPermissionGrantController {

    private final UserPermissionGrantRepository grantRepository;
    private final UserRepository userRepository;

    public UserPermissionGrantController(UserPermissionGrantRepository grantRepository, UserRepository userRepository) {
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAnyAuthority('PERMISSION_CREATE_EDIT_USER_ALLOY','PERMISSION_CREATE_EDIT_ADMIN_DEALER','PERMISSION_CREATE_EDIT_USER_DEALER','PERMISSION_CREATE_EDIT_ADMIN_ENTERPRISE','PERMISSION_CREATE_EDIT_USER_ENTERPRISE')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GrantDTO>> getGrantsForUser(@PathVariable Long userId) {
        List<UserPermissionGrant> grants = grantRepository.findByUserId(userId);
        List<GrantDTO> dtos = grants.stream()
                .map(g -> new GrantDTO(g.getId(), g.getUserId(), g.getUserPermissionId(), g.getGrantedByUserId(), g.getGrantedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAnyAuthority('PERMISSION_CREATE_EDIT_USER_ALLOY','PERMISSION_CREATE_EDIT_ADMIN_DEALER','PERMISSION_CREATE_EDIT_USER_DEALER','PERMISSION_CREATE_EDIT_ADMIN_ENTERPRISE','PERMISSION_CREATE_EDIT_USER_ENTERPRISE')")
    @PostMapping
    public ResponseEntity<GrantDTO> grant(
            @RequestBody GrantRequest request
    ) {
        Long grantedByUserId = currentUserId();
        if (grantedByUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserPermissionGrant grant = new UserPermissionGrant();
        grant.setUserId(request.getUserId());
        grant.setUserPermissionId(request.getUserPermissionId());
        grant.setGrantedByUserId(grantedByUserId);
        grant = grantRepository.save(grant);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new GrantDTO(grant.getId(), grant.getUserId(), grant.getUserPermissionId(), grant.getGrantedByUserId(), grant.getGrantedAt()));
    }

    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAnyAuthority('PERMISSION_CREATE_EDIT_USER_ALLOY','PERMISSION_CREATE_EDIT_ADMIN_DEALER','PERMISSION_CREATE_EDIT_USER_DEALER','PERMISSION_CREATE_EDIT_ADMIN_ENTERPRISE','PERMISSION_CREATE_EDIT_USER_ENTERPRISE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Integer id) {
        if (grantRepository.existsById(id)) {
            grantRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('ADMIN_ALLOY') or hasAnyAuthority('PERMISSION_CREATE_EDIT_USER_ALLOY','PERMISSION_CREATE_EDIT_ADMIN_DEALER','PERMISSION_CREATE_EDIT_USER_DEALER','PERMISSION_CREATE_EDIT_ADMIN_ENTERPRISE','PERMISSION_CREATE_EDIT_USER_ENTERPRISE')")
    @DeleteMapping("/user/{userId}/permission/{permissionId}")
    public ResponseEntity<Void> revokeByUserAndPermission(@PathVariable Long userId, @PathVariable Integer permissionId) {
        List<UserPermissionGrant> grants = grantRepository.findByUserIdAndUserPermissionId(userId, permissionId);
        grants.forEach(grantRepository::delete);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Optional<User> user = userRepository.findByUsername(auth.getName());
        return user.map(User::getId).orElse(null);
    }

    public static class GrantDTO {
        private Integer id;
        private Long userId;
        private Integer userPermissionId;
        private Long grantedByUserId;
        private java.time.LocalDateTime grantedAt;

        public GrantDTO() {}
        public GrantDTO(Integer id, Long userId, Integer userPermissionId, Long grantedByUserId, java.time.LocalDateTime grantedAt) {
            this.id = id;
            this.userId = userId;
            this.userPermissionId = userPermissionId;
            this.grantedByUserId = grantedByUserId;
            this.grantedAt = grantedAt;
        }
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Integer getUserPermissionId() { return userPermissionId; }
        public void setUserPermissionId(Integer userPermissionId) { this.userPermissionId = userPermissionId; }
        public Long getGrantedByUserId() { return grantedByUserId; }
        public void setGrantedByUserId(Long grantedByUserId) { this.grantedByUserId = grantedByUserId; }
        public java.time.LocalDateTime getGrantedAt() { return grantedAt; }
        public void setGrantedAt(java.time.LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
    }

    public static class GrantRequest {
        private Long userId;
        private Integer userPermissionId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Integer getUserPermissionId() { return userPermissionId; }
        public void setUserPermissionId(Integer userPermissionId) { this.userPermissionId = userPermissionId; }
    }
}
