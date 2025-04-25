package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "WeldingMachineParameterValue1")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WeldingMachineStateID", insertable = false, updatable = false)
    private WeldingMachineState weldingMachineState;
}
