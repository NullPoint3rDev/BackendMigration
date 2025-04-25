package org.alloy.models.entities;

import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "WeldingMachine")
@Data
@NoArgsConstructor
public class WeldingMachine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "OrganizationUnitID", nullable = false)
    private Integer organizationUnitId;

    @Column(name = "WeldingMachineTypeID", nullable = false)
    private Integer weldingMachineTypeId;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "Name")
    private String name;

    @Column(name = "MAC")
    private String mac;

    @Column(name = "SerialNumber")
    private String serialNumber;

    @Column(name = "YearManufactured")
    private String yearManufactured;

    @Column(name = "DateStartedUsing")
    private LocalDateTime dateStartedUsing;

    @Column(name = "InventoryNumber")
    private String inventoryNumber;

    @Column(name = "MaintenanceRegulation")
    private Integer maintenanceRegulation;

    @Column(name = "MaintenanceInterval")
    private Double maintenanceInterval;

    @Column(name = "Modules")
    private String modules;

    @Column(name = "Label")
    private String label;

    @Column(name = "Description")
    private String description;

    @Column(name = "PlanPositionX")
    private Double planPositionX;

    @Column(name = "PlanPositionY")
    private Double planPositionY;

    @Column(name = "LastServiceOn")
    private LocalDateTime lastServiceOn;

    @Column(name = "TimeTotalSecs")
    private Long timeTotalSecs;

    @Column(name = "TimeAfterLastServiceSecs")
    private Long timeAfterLastServiceSecs;

    @Column(name = "UserServiceNotifiedBeforeHours")
    private Integer userServiceNotifiedBeforeHours;

    @Column(name = "UserServiceNotifiedOn")
    private LocalDateTime userServiceNotifiedOn;

    @Column(name = "TimeTillNextServiceSecs")
    private Long timeTillNextServiceSecs;

    @Column(name = "MeasuringGasMachineID")
    private Integer measuringGasMachineId;

    @Column(name = "LastOnlineOn")
    private LocalDateTime lastOnlineOn;

    @OneToMany(mappedBy = "weldingMachine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Maintenance> maintenances = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WeldingMachineTypeID", insertable = false, updatable = false)
    private WeldingMachineType weldingMachineType;

    @OneToMany(mappedBy = "weldingMachine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingLimitProgram> weldingLimitPrograms = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrganizationUnitID", insertable = false, updatable = false)
    private OrganizationUnit organizationUnit;

    @OneToMany(mappedBy = "weldingMachine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingMachineState> weldingMachineStates = new ArrayList<>();
}
