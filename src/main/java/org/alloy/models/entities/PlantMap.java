package org.alloy.models.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plant_map")
@Data
@NoArgsConstructor
@Schema(description = "Карта предприятия")
public class PlantMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Column(name = "OrganizationID", nullable = false)
    @Schema(description = "ID организации", example = "1")
    private Integer organizationId;

    @Column(name = "Name", nullable = false)
    @Schema(description = "Название карты", example = "Основная карта предприятия")
    private String name;

    @Column(name = "Description")
    @Schema(description = "Описание карты", example = "Схематичная карта основного производственного комплекса")
    private String description;

    @Column(name = "Width", nullable = false)
    @Schema(description = "Ширина карты в пикселях", example = "1200")
    private Integer width;

    @Column(name = "Height", nullable = false)
    @Schema(description = "Высота карты в пикселях", example = "800")
    private Integer height;

    @Column(name = "BackgroundImage")
    @Schema(description = "Путь к фоновому изображению карты")
    private String backgroundImage;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    @Schema(description = "Статус карты", example = "Active")
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    @Schema(description = "Дата создания", example = "2024-03-15T10:30:00")
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "DateUpdated", nullable = false)
    @Schema(description = "Дата обновления", example = "2024-03-15T10:30:00")
    private LocalDateTime dateUpdated;

    @Column(name = "IsDefault")
    @Schema(description = "Является ли карта картой по умолчанию", example = "true")
    private Boolean isDefault = false;
}
