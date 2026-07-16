package org.alloy.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.alloy.models.MacRegistryStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "MacAddressRegistry")
@Data
@NoArgsConstructor
public class MacAddressRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "MAC", nullable = false, unique = true, length = 12)
    private String mac;

    @Column(name = "MacEquipmentTypeID", nullable = false)
    private Integer macEquipmentTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MacEquipmentTypeID", insertable = false, updatable = false)
    private MacEquipmentType equipmentType;

    @Column(name = "Status", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private MacRegistryStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "EnteredByName", nullable = false)
    private String enteredByName;

    @Column(name = "SessionCount", nullable = false)
    private Long sessionCount = 0L;

    /** ponytail: внутреннее поле для debounce сессий (10 мин), в UI не показываем. */
    @Column(name = "LastPacketAt")
    private LocalDateTime lastPacketAt;

    @Column(name = "WeldingMachineID")
    private Integer weldingMachineId;
}
