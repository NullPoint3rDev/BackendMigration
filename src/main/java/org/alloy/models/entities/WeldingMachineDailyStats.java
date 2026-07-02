package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "welding_machine_daily_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"welding_machine_id", "stat_date"})
)
@Data
@NoArgsConstructor
public class WeldingMachineDailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "welding_machine_id", nullable = false)
    private Integer weldingMachineId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "wire_consumption_kg", nullable = false, precision = 16, scale = 5)
    private BigDecimal wireConsumptionKg = BigDecimal.ZERO;

    /** DEFAULT 0 в columnDefinition — иначе ddl-auto не добавит NOT NULL на таблицу с данными. */
    @Column(name = "gas_consumption_l", nullable = false,
            columnDefinition = "NUMERIC(16,3) NOT NULL DEFAULT 0")
    private BigDecimal gasConsumptionL = BigDecimal.ZERO;

    /** Первое значение счётчика «расход с включения» за сутки (л) — для live-обновления на UI. */
    @Column(name = "gas_baseline_at_day_start_l", columnDefinition = "NUMERIC(16,3)")
    private BigDecimal gasBaselineAtDayStartL;

    @Column(name = "off_ms", nullable = false)
    private Long offMs = 0L;

    @Column(name = "standby_ms", nullable = false)
    private Long standbyMs = 0L;

    @Column(name = "on_ms", nullable = false)
    private Long onMs = 0L;

    @Column(name = "welding_ms", nullable = false)
    private Long weldingMs = 0L;

    @Column(name = "last_state_id")
    private Long lastStateId;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
