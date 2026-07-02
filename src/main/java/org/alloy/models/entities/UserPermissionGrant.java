package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Выданное право пользователю (для прав типа «наст N»).
 * Кто-то с ролью уровня N выдал данному пользователю право на указанное действие.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "user_permission_grant")
public class UserPermissionGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ID пользователя (users.id), которому выдано право */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** ID права (UserPermission) */
    @Column(name = "user_permission_id", nullable = false)
    private Integer userPermissionId;

    /** ID пользователя (users.id), который выдал право (админ уровня N) */
    @Column(name = "granted_by_user_id", nullable = false)
    private Long grantedByUserId;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_permission_id", insertable = false, updatable = false)
    private UserPermission userPermission;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }
}
