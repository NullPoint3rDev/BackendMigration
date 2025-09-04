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
@Table(name = "plant_map_element")
@Data
@NoArgsConstructor
@Schema(description = "Элемент на карте предприятия")
public class PlantMapElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Column(name = "PlantMapID", nullable = false)
    @Schema(description = "ID карты предприятия", example = "1")
    private Integer plantMapId;

    @Column(name = "ElementType", nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Тип элемента", example = "WELDING_MACHINE")
    private ElementType elementType;

    @Column(name = "ElementID", nullable = false)
    @Schema(description = "ID элемента (сварочной машины, цеха и т.д.)", example = "1")
    private Integer elementId;

    @Column(name = "PositionX", nullable = false)
    @Schema(description = "Позиция X на карте", example = "100.5")
    private Double positionX;

    @Column(name = "PositionY", nullable = false)
    @Schema(description = "Позиция Y на карте", example = "200.5")
    private Double positionY;

    @Column(name = "Width")
    @Schema(description = "Ширина элемента", example = "50.0")
    private Double width;

    @Column(name = "Height")
    @Schema(description = "Высота элемента", example = "30.0")
    private Double height;

    @Column(name = "Rotation")
    @Schema(description = "Поворот элемента в градусах", example = "0.0")
    private Double rotation = 0.0;

    @Column(name = "ZIndex")
    @Schema(description = "Порядок отображения элемента", example = "1")
    private Integer zIndex = 1;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    @Schema(description = "Статус элемента", example = "Active")
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    @Schema(description = "Дата создания", example = "2024-03-15T10:30:00")
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "DateUpdated", nullable = false)
    @Schema(description = "Дата обновления", example = "2024-03-15T10:30:00")
    private LocalDateTime dateUpdated;

    public enum ElementType {
        WELDING_MACHINE,
        WORKSHOP,
        EQUIPMENT,
        ANNOTATION
    }
}
