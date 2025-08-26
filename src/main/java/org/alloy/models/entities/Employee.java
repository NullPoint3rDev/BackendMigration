package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.alloy.models.GeneralStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Employees")
@Data
@NoArgsConstructor
@Schema(description = "Сотрудник")
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор")
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 255)
    @Schema(description = "Логин для входа в систему")
    private String username;
    
    @Column(name = "password", nullable = false, length = 255)
    @Schema(description = "Пароль для входа в систему")
    private String password;
    
    @Column(name = "full_name", nullable = false, length = 255)
    @Schema(description = "ФИО сотрудника")
    private String fullName;
    
    @Column(name = "email", length = 255)
    @Schema(description = "Email сотрудника")
    private String email;
    
    @Column(name = "position", length = 255)
    @Schema(description = "Должность")
    private String position;
    
    @Column(name = "phone", length = 50)
    @Schema(description = "Телефон")
    private String phone;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_unit_id")
    @Schema(description = "Подразделение")
    private OrganizationUnit organizationUnit;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_role_id")
    @Schema(description = "Роль пользователя")
    private UserRole userRole;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Статус сотрудника")
    private GeneralStatus status;
    
    @Column(name = "photo", length = 255)
    @Schema(description = "Путь к аватарке")
    private String photo;
    
    @Column(name = "date_created", nullable = false)
    @Schema(description = "Дата создания")
    private LocalDateTime dateCreated;
    
    @Column(name = "date_updated")
    @Schema(description = "Дата обновления")
    private LocalDateTime dateUpdated;
    
    @PrePersist
    protected void onCreate() {
        dateCreated = LocalDateTime.now();
        if (status == null) {
            status = GeneralStatus.Active;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        dateUpdated = LocalDateTime.now();
    }
}
