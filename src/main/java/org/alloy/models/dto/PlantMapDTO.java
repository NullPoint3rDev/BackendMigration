package org.alloy.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.alloy.models.GeneralStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO карты предприятия")
public class PlantMapDTO {
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Schema(description = "ID организации", example = "1")
    private Integer organizationId;

    @Schema(description = "Название карты", example = "Основная карта предприятия")
    private String name;

    @Schema(description = "Описание карты", example = "Схематичная карта основного производственного комплекса")
    private String description;

    @Schema(description = "Ширина карты в пикселях", example = "1200")
    private Integer width;

    @Schema(description = "Высота карты в пикселях", example = "800")
    private Integer height;

    @Schema(description = "Путь к фоновому изображению карты")
    private String backgroundImage;

    @Schema(description = "Статус карты", example = "Active")
    private GeneralStatus status;

    @Schema(description = "Дата создания", example = "2024-03-15T10:30:00")
    private LocalDateTime dateCreated;

    @Schema(description = "Дата обновления", example = "2024-03-15T10:30:00")
    private LocalDateTime dateUpdated;

    @Schema(description = "Является ли карта картой по умолчанию", example = "true")
    private Boolean isDefault;

    @Schema(description = "Список элементов на карте")
    private List<PlantMapElementDTO> elements;

    @Schema(description = "Список цехов на карте")
    private List<PlantMapWorkshopDTO> workshops;
}
