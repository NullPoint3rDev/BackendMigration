package org.alloy.models.entities;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "UserRolePermission")
public class UserRolePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userRoleId;

    @Column(nullable = false)
    private Integer userPermissionId;

    private Boolean read;
    private Boolean write;

    /** Если не null (1–6): право настраивается пользователем с ролью данного уровня; фактическое наличие — из user_permission_grant. */
    @Column(name = "ConfigurableByRoleLevel")
    private Integer configurableByRoleLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userRoleId", insertable = false, updatable = false)
    private UserRole userRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userPermissionId", insertable = false, updatable = false)
    private UserPermission userPermission;
} 