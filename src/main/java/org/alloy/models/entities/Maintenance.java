package org.alloy.models.entities;

import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Maintenance")
@Data
@NoArgsConstructor
public class Maintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "WeldingMachineID", nullable = false)
    private Integer weldingMachineId;

    @Column(name = "UserAccountID", nullable = false)
    private Integer userAccountId;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "DatePlanned")
    private LocalDateTime datePlanned;

    @Column(name = "DateCompleted")
    private LocalDateTime dateCompleted;

    @Column(name = "Description")
    private String description;

    @Column(name = "Notes")
    private String notes;

    @Column(name = "Type")
    private String type;

    @Column(name = "Result")
    private String result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WeldingMachineID", insertable = false, updatable = false)
    private WeldingMachine weldingMachine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountID", insertable = false, updatable = false)
    private UserAccount userAccount;
}
