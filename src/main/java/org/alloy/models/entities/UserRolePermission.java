package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userRoleId", insertable = false, updatable = false)
    private UserRole userRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userPermissionId", insertable = false, updatable = false)
    private UserPermission userPermission;
} 