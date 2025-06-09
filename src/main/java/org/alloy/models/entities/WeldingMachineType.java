package org.alloy.models.entities;

import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "WeldingMachineType")
@Data
@NoArgsConstructor
public class WeldingMachineType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "Description")
    private String description;

    @Column(name = "Settings")
    private String settings;

    @Column(name = "PropertyLimits")
    private String propertyLimits;

    @Column(name = "Inbound")
    private String inbound;

    @Column(name = "Outbound")
    private String outbound;

    @Column(name = "Presentation")
    private String presentation;

    @Column(name = "ModeDefinitions")
    private String modeDefinitions;

    @Column(name = "AlertDefinitions")
    private String alertDefinitions;

    @OneToMany(mappedBy = "weldingMachineType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingMachine> weldingMachines = new ArrayList<>();
}
