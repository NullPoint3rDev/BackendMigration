package org.alloy.models.entities;

import org.alloy.models.WeldingMachineStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "welding_machine_state")
@Data
@NoArgsConstructor
public class WeldingMachineState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "welding_machineid", nullable = false)
    private Integer weldingMachineId;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;

    @Column(name = "welding_machine_status")
    @Enumerated(EnumType.ORDINAL)
    private WeldingMachineStatus weldingMachineStatus;

    @Column(name = "rfid")
    private String rfid;

    @Column(name = "control")
    private String control;

    @Column(name = "control_status")
    private Integer controlStatus;

    @Column(name = "control_state")
    private String controlState;

    @Column(name = "state_duration_ms", nullable = false)
    private Long stateDurationMs;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "welding_materialid")
    private Integer weldingMaterialId;

    @Column(name = "limits_exceeded")
    private Boolean limitsExceeded;

    @Column(name = "welding_limit_programid")
    private Integer weldingLimitProgramId;

    @Column(name = "welding_limit_program_name")
    private String weldingLimitProgramName;

    @Column(name = "gas_welding_materialid")
    private Integer gasWeldingMaterialId;

    @Column(name = "organization_unitid")
    private Integer organizationUnitId;

    @Column(name = "md5")
    private String md5;

    @Column(name = "norm_gas_flow")
    private Double normGasFlow;

    @JsonBackReference("machineStatesRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "welding_machineid", insertable = false, updatable = false)
    private WeldingMachine weldingMachine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "welding_limit_programid", insertable = false, updatable = false)
    private WeldingLimitProgram weldingLimitProgram;

    @JsonManagedReference("parameterValuesRef")
    @OneToMany(mappedBy = "weldingMachineState", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingMachineParameterValue> parameterValues = new ArrayList<>();
}
