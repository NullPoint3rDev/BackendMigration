package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для шаблона отчета
 */
@Data
@NoArgsConstructor
public class ReportTemplateDTO {

    /**
     * ID шаблона (если редактируется существующий)
     */
    private Long id;

    /**
     * Название шаблона
     */
    private String name;

    /**
     * Email для отправки отчета
     */
    private String email;

    /**
     * Параметры отчета (selectedColumns, parameters)
     */
    private Map<String, Object> reportParameters;

    /**
     * Выбранные ID подразделений
     */
    private List<Integer> selectedOrganizationUnitIds;

    /**
     * Выбранные ID сварщиков
     */
    private List<Integer> selectedWelderIds;

    /**
     * Выбранные модели оборудования (ключи)
     */
    private List<String> selectedEquipmentModels;

    /**
     * Диапазоны токов
     */
    private Map<String, Object> currentRanges;

    /**
     * Настройки периода отчета
     */
    private Map<String, Object> periodSettings;

    /**
     * Настройки автоматического отчета
     */
    private Map<String, Object> autoReportSettings;

    /**
     * ID пользователя, создавшего шаблон
     */
    private Integer createdBy;

    /**
     * Дата создания
     */
    private LocalDateTime createdAt;

    /**
     * Дата обновления
     */
    private LocalDateTime updatedAt;

    /**
     * Активен ли шаблон
     */
    private Boolean isActive;
}

