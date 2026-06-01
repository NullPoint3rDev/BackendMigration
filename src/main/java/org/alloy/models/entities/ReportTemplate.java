package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
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
    @Lob
    @Column(name = "report_parameters")
    private String reportParameters;

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
     * JSON с выбранными моделями оборудования
     */
    @Lob
    @Column(name = "selected_equipment_models")
    private String selectedEquipmentModels;

    /**
     * JSON с диапазонами токов (workOutsideSetCurrent, workOutsideActualCurrent)
     */
    @Lob
    @Column(name = "current_ranges")
    private String currentRanges;

    /**
     * JSON с настройками периода отчета (selectedPeriod, startDate, endDate, timeRange, selectedDays)
     */
    @Lob
    @Column(name = "period_settings")
    private String periodSettings;

    /**
     * JSON с настройками автоматического отчета (autoReportTime, autoReportWeekDays, autoReportMonthDays)
     */
    @Lob
    @Column(name = "auto_report_settings")
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

