package org.alloy.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.PlantMapElement;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(description = "DTO элемента на карте предприятия")
public class PlantMapElementDTO {
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Schema(description = "ID карты предприятия", example = "1")
    private Integer plantMapId;

    @Schema(description = "Тип элемента", example = "WELDING_MACHINE")
    private PlantMapElement.ElementType elementType;

    @Schema(description = "ID элемента (сварочной машины, цеха и т.д.)", example = "1")
    private Integer elementId;

    @Schema(description = "Позиция X на карте", example = "100.5")
    private Double positionX;

    @Schema(description = "Позиция Y на карте", example = "200.5")
    private Double positionY;

    @Schema(description = "Ширина элемента", example = "50.0")
    private Double width;

    @Schema(description = "Высота элемента", example = "30.0")
    private Double height;

    @Schema(description = "Поворот элемента в градусах", example = "0.0")
    private Double rotation;

    @Schema(description = "Порядок отображения элемента", example = "1")
    private Integer zIndex;

    @Schema(description = "Статус элемента", example = "Active")
    private GeneralStatus status;

    @Schema(description = "Дата создания", example = "2024-03-15T10:30:00")
    private LocalDateTime dateCreated;

    @Schema(description = "Дата обновления", example = "2024-03-15T10:30:00")
    private LocalDateTime dateUpdated;

    // Дополнительные поля для отображения
    @Schema(description = "Название элемента")
    private String elementName;

    @Schema(description = "Описание элемента")
    private String elementDescription;

    @Schema(description = "Иконка элемента")
    private String icon;
}
