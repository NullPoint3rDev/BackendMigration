package org.alloy.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.alloy.models.GeneralStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(description = "DTO цеха на карте предприятия")
public class PlantMapWorkshopDTO {
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Schema(description = "ID карты предприятия", example = "1")
    private Integer plantMapId;

    @Schema(description = "Название цеха", example = "Цех №1")
    private String name;

    @Schema(description = "Описание цеха", example = "Основной производственный цех")
    private String description;

    @Schema(description = "Позиция X на карте", example = "100.5")
    private Double positionX;

    @Schema(description = "Позиция Y на карте", example = "200.5")
    private Double positionY;

    @Schema(description = "Ширина цеха", example = "200.0")
    private Double width;

    @Schema(description = "Высота цеха", example = "150.0")
    private Double height;

    @Schema(description = "Цвет цеха в HEX формате", example = "#4A90E2")
    private String color;

    @Schema(description = "Цвет границы цеха в HEX формате", example = "#2E5C8A")
    private String borderColor;

    @Schema(description = "Прозрачность цеха (0.0 - 1.0)", example = "0.3")
    private Double opacity;

    @Schema(description = "Статус цеха", example = "Active")
    private GeneralStatus status;

    @Schema(description = "Дата создания", example = "2024-03-15T10:30:00")
    private LocalDateTime dateCreated;

    @Schema(description = "Дата обновления", example = "2024-03-15T10:30:00")
    private LocalDateTime dateUpdated;
}
