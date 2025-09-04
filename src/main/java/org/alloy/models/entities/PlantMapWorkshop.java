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
@Table(name = "plant_map_workshop")
@Data
@NoArgsConstructor
@Schema(description = "Цех на карте предприятия")
public class PlantMapWorkshop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Column(name = "PlantMapID", nullable = false)
    @Schema(description = "ID карты предприятия", example = "1")
    private Integer plantMapId;

    @Column(name = "Name", nullable = false)
    @Schema(description = "Название цеха", example = "Цех №1")
    private String name;

    @Column(name = "Description")
    @Schema(description = "Описание цеха", example = "Основной производственный цех")
    private String description;

    @Column(name = "PositionX", nullable = false)
    @Schema(description = "Позиция X на карте", example = "100.5")
    private Double positionX;

    @Column(name = "PositionY", nullable = false)
    @Schema(description = "Позиция Y на карте", example = "200.5")
    private Double positionY;

    @Column(name = "Width", nullable = false)
    @Schema(description = "Ширина цеха", example = "200.0")
    private Double width;

    @Column(name = "Height", nullable = false)
    @Schema(description = "Высота цеха", example = "150.0")
    private Double height;

    @Column(name = "Color")
    @Schema(description = "Цвет цеха в HEX формате", example = "#4A90E2")
    private String color = "#4A90E2";

    @Column(name = "BorderColor")
    @Schema(description = "Цвет границы цеха в HEX формате", example = "#2E5C8A")
    private String borderColor = "#2E5C8A";

    @Column(name = "Opacity")
    @Schema(description = "Прозрачность цеха (0.0 - 1.0)", example = "0.3")
    private Double opacity = 0.3;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    @Schema(description = "Статус цеха", example = "Active")
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    @Schema(description = "Дата создания", example = "2024-03-15T10:30:00")
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "DateUpdated", nullable = false)
    @Schema(description = "Дата обновления", example = "2024-03-15T10:30:00")
    private LocalDateTime dateUpdated;
}
