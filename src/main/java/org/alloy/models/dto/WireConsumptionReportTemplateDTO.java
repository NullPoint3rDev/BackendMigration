package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO для шаблона отчета по расходу проволоки
 */
@Data
@NoArgsConstructor
public class WireConsumptionReportTemplateDTO {

    /**
     * ID шаблона (если редактируется существующий)
     */
    private Long templateId;

    /**
     * Название шаблона
     */
    private String templateName;

    /**
     * Выбранные сварщики по подразделениям
     * Ключ - ID подразделения, значение - список ID сварщиков (null или пустой список означает "все сварщики")
     */
    private List<Integer> selectedOrganizationUnitIds; // Если подразделение выбрано, все сварщики из него

    /**
     * Конкретные ID сварщиков (если выбраны индивидуально, а не через подразделение)
     */
    private List<Integer> selectedWelderIds;

    /**
     * Минимальный разрешенный устанавливаемый ток (А)
     */
    private Integer setCurrentMin;

    /**
     * Максимальный разрешенный устанавливаемый ток (А)
     */
    private Integer setCurrentMax;

    /**
     * Минимальный разрешенный фактический ток (А)
     */
    private Integer actualCurrentMin;

    /**
     * Максимальный разрешенный фактический ток (А)
     */
    private Integer actualCurrentMax;

    /**
     * Выбранные колонки для отчета
     * Все колонки кроме: сварщик, проволока, расход (они всегда включены)
     * По умолчанию выделены салатовым цветом
     */
    private List<String> selectedColumns;

    /**
     * Колонка для сортировки
     * Возможные колонки для сортировки выделены голубым цветом
     */
    private String sortByColumn;

    /**
     * Направление сортировки (ASC, DESC)
     */
    private String sortDirection = "ASC";

    /**
     * Выбранные модели оборудования (названия аппаратов)
     */
    private List<String> selectedEquipmentModels;

    /**
     * Выбранные дни недели (для периода "Неделя")
     * Например: ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]
     */
    private List<String> selectedDays;
}

