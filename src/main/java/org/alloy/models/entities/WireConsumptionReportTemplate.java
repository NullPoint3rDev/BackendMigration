package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity для шаблона отчета по расходу проволоки
 */
@Entity
@Table(name = "wire_consumption_report_templates")
@Data
@NoArgsConstructor
public class WireConsumptionReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    /**
     * JSON с выбранными ID подразделений
     */
    @Lob
    @Column(name = "selected_organization_unit_ids")
    private String selectedOrganizationUnitIds;

    /**
     * JSON с выбранными ID сварщиков
     */
    @Lob
    @Column(name = "selected_welder_ids")
    private String selectedWelderIds;

    /**
     * Минимальный разрешенный устанавливаемый ток (А)
     */
    @Column(name = "set_current_min")
    private Integer setCurrentMin;

    /**
     * Максимальный разрешенный устанавливаемый ток (А)
     */
    @Column(name = "set_current_max")
    private Integer setCurrentMax;

    /**
     * Минимальный разрешенный фактический ток (А)
     */
    @Column(name = "actual_current_min")
    private Integer actualCurrentMin;

    /**
     * Максимальный разрешенный фактический ток (А)
     */
    @Column(name = "actual_current_max")
    private Integer actualCurrentMax;

    /**
     * JSON с выбранными колонками
     */
    @Lob
    @Column(name = "selected_columns")
    private String selectedColumns;

    /**
     * Колонка для сортировки
     */
    @Column(name = "sort_by_column", length = 100)
    private String sortByColumn;

    /**
     * Направление сортировки (ASC, DESC)
     */
    @Column(name = "sort_direction", length = 10)
    private String sortDirection = "ASC";

    /**
     * ID пользователя, создавшего шаблон
     */
    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public WireConsumptionReportTemplate(String name, Integer createdBy) {
        this.name = name;
        this.createdBy = createdBy;
        this.isActive = true;
        this.sortDirection = "ASC";
    }
}

