package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity для общего шаблона отчета
 */
@Entity
@Table(name = "report_templates")
@Data
@NoArgsConstructor
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    /**
     * JSON с параметрами отчета (selectedColumns, parameters и т.д.)
     */
    @Column(name = "report_parameters", columnDefinition = "TEXT")
    private String reportParameters;

    /**
     * JSON с выбранными ID подразделений
     */
    @Column(name = "selected_organization_unit_ids", columnDefinition = "TEXT")
    private String selectedOrganizationUnitIds;

    /**
     * JSON с выбранными ID сварщиков
     */
    @Column(name = "selected_welder_ids", columnDefinition = "TEXT")
    private String selectedWelderIds;

    /**
     * JSON с выбранными моделями оборудования
     */
    @Column(name = "selected_equipment_models", columnDefinition = "TEXT")
    private String selectedEquipmentModels;

    /**
     * JSON с диапазонами токов (workOutsideSetCurrent, workOutsideActualCurrent)
     */
    @Column(name = "current_ranges", columnDefinition = "TEXT")
    private String currentRanges;

    /**
     * JSON с настройками периода отчета (selectedPeriod, startDate, endDate, timeRange, selectedDays)
     */
    @Column(name = "period_settings", columnDefinition = "TEXT")
    private String periodSettings;

    /**
     * JSON с настройками автоматического отчета (autoReportTime, autoReportWeekDays, autoReportMonthDays)
     */
    @Column(name = "auto_report_settings", columnDefinition = "TEXT")
    private String autoReportSettings;

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

    public ReportTemplate(String name, Integer createdBy) {
        this.name = name;
        this.createdBy = createdBy;
        this.isActive = true;
    }
}

