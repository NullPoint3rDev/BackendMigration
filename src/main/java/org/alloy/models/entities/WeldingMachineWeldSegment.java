package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Материализованный сегмент шва для отчётов (вариант B — фоновый пересчёт, без изменений ingestion).
 */
@Entity
@Table(
        name = "welding_machine_weld_segment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"welding_machine_id", "start_state_id"})
)
@Data
@NoArgsConstructor
public class WeldingMachineWeldSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "welding_machine_id", nullable = false)
    private Integer weldingMachineId;

    /** Календарный день начала шва (timezone мониторинга) — для догоняющего backfill по суткам. */
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds", nullable = false, precision = 12, scale = 1)
    private BigDecimal durationSeconds;

    @Column(name = "avg_current", precision = 10, scale = 1)
    private BigDecimal avgCurrent;

    @Column(name = "avg_voltage", precision = 10, scale = 1)
    private BigDecimal avgVoltage;

    @Column(name = "start_state_id", nullable = false)
    private Long startStateId;

    @Column(name = "end_state_id")
    private Long endStateId;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
