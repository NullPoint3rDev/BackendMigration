package org.alloy.models.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Alert")
@Schema(description = "Модель оповещения системы")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор оповещения", example = "1")
    private Integer id;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    @Schema(description = "Дата и время создания оповещения", example = "2024-03-20T10:30:00")
    private LocalDateTime dateCreated;

    @Column(name = "Type", nullable = false)
    @Schema(description = "Тип оповещения", example = "MAINTENANCE", allowableValues = {"MAINTENANCE", "ERROR", "WARNING", "INFO"})
    private String type;

    @Column(name = "Message", nullable = false)
    @Schema(description = "Текст сообщения оповещения", example = "Требуется техническое обслуживание сварочного аппарата")
    private String message;

    @Column(name = "Severity", nullable = false)
    @Schema(description = "Уровень важности оповещения", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    private String severity;

    @Column(name = "IsRead")
    @Schema(description = "Флаг прочтения оповещения", example = "false")
    private Boolean isRead;

    @Column(name = "DateRead")
    @Schema(description = "Дата и время прочтения оповещения", example = "2024-03-20T11:30:00")
    private LocalDateTime dateRead;

    @Column(name = "EntityType")
    @Schema(description = "Тип сущности, к которой относится оповещение", example = "WELDING_MACHINE")
    private String entityType;

    @Column(name = "EntityID")
    @Schema(description = "Идентификатор сущности, к которой относится оповещение", example = "123")
    private Integer entityId;
} 