package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Отметка «сутки пересчитаны» — отчёт знает, можно ли читать сегменты из кэша. */
@Entity
@Table(
        name = "welding_machine_weld_segment_day_mark",
        uniqueConstraints = @UniqueConstraint(columnNames = {"welding_machine_id", "stat_date"})
)
@Data
@NoArgsConstructor
public class WeldingMachineWeldSegmentDayMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "welding_machine_id", nullable = false)
    private Integer weldingMachineId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "segment_count", nullable = false)
    private Integer segmentCount = 0;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
