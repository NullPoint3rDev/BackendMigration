package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
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
