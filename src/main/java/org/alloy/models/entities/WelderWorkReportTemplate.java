package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity для шаблона отчета "По работе сварщика"
 */
@Entity
@Table(name = "welder_work_report_templates")
@Data
@NoArgsConstructor
public class WelderWorkReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "welder_id")
    private Long welderId;

    @Column(name = "include_actual_current_range")
    private Boolean includeActualCurrentRange = false;

    @Column(name = "actual_current_min")
    private Integer actualCurrentMin;

    @Column(name = "actual_current_max")
    private Integer actualCurrentMax;

    @Column(name = "min_interval_between_welds_sec")
    private Integer minIntervalBetweenWeldsSec;

    @Column(name = "min_weld_duration_sec")
    private Integer minWeldDurationSec;

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
}


