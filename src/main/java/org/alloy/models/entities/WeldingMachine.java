package org.alloy.models.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "WeldingMachine")
@Data
@NoArgsConstructor
@Schema(description = "Сварочная машина")
public class WeldingMachine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Column(name = "OrganizationUnitID", nullable = false)
    @Schema(description = "ID подразделения", example = "1")
    private Integer organizationUnitId;

    @Column(name = "WeldingMachineTypeID", nullable = false)
    @Schema(description = "ID типа сварочной машины", example = "1")
    private Integer weldingMachineTypeId;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    @Schema(description = "Статус сварочной машины", example = "Active")
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    @Schema(description = "Дата создания", example = "2024-03-15T10:30:00")
    private LocalDateTime dateCreated;

    @Column(name = "Name")
    @Schema(description = "Название сварочной машины", example = "Сварочная машина #1")
    private String name;

    @Column(name = "MAC")
    @Schema(description = "MAC-адрес", example = "00:11:22:33:44:55")
    private String mac;

    @Column(name = "SerialNumber")
    @Schema(description = "Серийный номер", example = "SN123456")
    private String serialNumber;

    @Column(name = "YearManufactured")
    @Schema(description = "Год производства", example = "2023")
    private String yearManufactured;

    @Column(name = "DateStartedUsing")
    @Schema(description = "Дата начала использования", example = "2024-01-01T00:00:00")
    private LocalDateTime dateStartedUsing;

    @Column(name = "InventoryNumber")
    @Schema(description = "Инвентарный номер", example = "INV-001")
    private String inventoryNumber;

    @Column(name = "MaintenanceRegulation")
    @Schema(description = "Регламент обслуживания", example = "30")
    private Integer maintenanceRegulation;

    @Column(name = "MaintenanceInterval")
    @Schema(description = "Интервал обслуживания (в днях)", example = "30.0")
    private Double maintenanceInterval;

    @Column(name = "Modules")
    @Schema(description = "Модули", example = "[\"module1\", \"module2\"]")
    private String modules;

    @Column(name = "Label")
    @Schema(description = "Метка", example = "Сварочная машина в цеху №1")
    private String label;

    @Column(name = "Description")
    @Schema(description = "Описание", example = "Сварочная машина для сварки труб")
    private String description;

    @Column(name = "PlanPositionX")
    @Schema(description = "Позиция X на плане", example = "100.5")
    private Double planPositionX;

    @Column(name = "PlanPositionY")
    @Schema(description = "Позиция Y на плане", example = "200.5")
    private Double planPositionY;

    @Column(name = "LastServiceOn")
    @Schema(description = "Дата последнего обслуживания", example = "2024-02-15T10:30:00")
    private LocalDateTime lastServiceOn;

    @Column(name = "TimeTotalSecs")
    @Schema(description = "Общее время работы в секундах", example = "3600")
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
