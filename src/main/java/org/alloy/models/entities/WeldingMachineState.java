package org.alloy.models.entities;

import org.alloy.models.WeldingMachineStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "WeldingMachineState")
@Data
@NoArgsConstructor
public class WeldingMachineState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "WeldingMachineID", nullable = false)
    private Integer weldingMachineId;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "DateUpdated", nullable = false)
    private LocalDateTime dateUpdated;

    @Column(name = "WeldingMachineStatus")
    @Enumerated(EnumType.ORDINAL)
    private WeldingMachineStatus weldingMachineStatus;

    @Column(name = "RFID")
    private String rfid;

    @Column(name = "Control")
    private String control;

    @Column(name = "ControlStatus")
    private Integer controlStatus;

    @Column(name = "ControlState")
    private String controlState;

    @Column(name = "StateDurationMs", nullable = false)
    private Long stateDurationMs;

    @Column(name = "ErrorCode")
    private String errorCode;

    @Column(name = "WeldingMaterialID")
    private Integer weldingMaterialId;

    @Column(name = "LimitsExceeded")
    private Boolean limitsExceeded;

    @Column(name = "WeldingLimitProgramID")
    private Integer weldingLimitProgramId;

    @Column(name = "WeldingLimitProgramName")
    private String weldingLimitProgramName;

    @Column(name = "GasWeldingMaterialID")
    private Integer gasWeldingMaterialId;

    @Column(name = "OrganizationUnitID")
    private Integer organizationUnitId;

    @Column(name = "MD5")
    private String md5;

    @Column(name = "NormGasFlow")
    private Double normGasFlow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WeldingMachineID", insertable = false, updatable = false)
    private WeldingMachine weldingMachine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WeldingLimitProgramID", insertable = false, updatable = false)
    private WeldingLimitProgram weldingLimitProgram;

    @OneToMany(mappedBy = "weldingMachineState", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingMachineParameterValue> parameterValues = new ArrayList<>();
}
