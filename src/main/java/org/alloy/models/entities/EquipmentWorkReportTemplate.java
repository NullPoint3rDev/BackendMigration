package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity для шаблона отчета "По работе оборудования"
 */
@Entity
@Table(name = "equipment_work_report_templates")
@Data
@NoArgsConstructor
public class EquipmentWorkReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

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

    /** Выбранные ID аппаратов (JSON-массив или через запятую: 1,2,3) */
    @Column(name = "selected_equipment_ids", length = 500)
    private String selectedEquipmentIds;

    @Column(name = "selected_columns", length = 500)
    private String selectedColumns;

    public List<Integer> getSelectedEquipmentIdsList() {
        if (selectedEquipmentIds == null || selectedEquipmentIds.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(selectedEquipmentIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }

    public void setSelectedEquipmentIdsList(List<Integer> list) {
        this.selectedEquipmentIds = list != null && !list.isEmpty()
                ? list.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null)
                : null;
    }

    public List<String> getSelectedColumnsList() {
        if (selectedColumns == null || selectedColumns.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(selectedColumns.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    public void setSelectedColumnsList(List<String> list) {
        this.selectedColumns = list != null && !list.isEmpty() ? String.join(",", list) : null;
    }
}
