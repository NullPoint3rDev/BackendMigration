package org.alloy.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "welding_machine_parameter_value")
@Data
@NoArgsConstructor
public class WeldingMachineParameterValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "WeldingMachineStateID", nullable = false)
    private Long weldingMachineStateId;

    @Column(name = "PropertyCode", nullable = false)
    private String propertyCode;

    @Column(name = "Value")
    private String value;

    @Column(name = "PropertyType")
    private String propertyType;

    @Column(name = "RawValue")
    private String rawValue;

    @Column(name = "LimitsExceeded")
    private Boolean limitsExceeded;

    @Column(name = "LimitMin")
    private Float limitMin;

    @Column(name = "LimitMax")
    private Float limitMax;

    @JsonBackReference("parameterValuesRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WeldingMachineStateID", insertable = false, updatable = false)
    private WeldingMachineState weldingMachineState;
}
