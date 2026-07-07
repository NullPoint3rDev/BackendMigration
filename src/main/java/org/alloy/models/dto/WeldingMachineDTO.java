package org.alloy.models.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.alloy.models.DeviceModel;
import org.alloy.models.dto.serialization.UtcLocalDateTimeAsMoscowOffsetSerializer;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeldingMachineDTO {
    private Integer id;
    private String name;
    private String model;
    private String mac;
    private DeviceModel deviceModel;
    private String serialNumber;
    private String inventoryNumber;
    private String imageUrl;
    private String department;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime commissionDate;
    private Integer manufactureYear;
    private LocalDateTime lastService;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastPoweredOnAt;
    @JsonSerialize(using = UtcLocalDateTimeAsMoscowOffsetSerializer.class)
    private LocalDateTime lastWeldAt;
    private OrganizationUnitShortDTO organizationUnit;
    private WeldingMachineTypeShortDTO weldingMachineType;
    private String modules;
    private Double maintenanceInterval;
    private String maintenanceIntervalUnit;
    private Integer maintenanceRegulation;
    private Integer userServiceNotifiedBeforeHours;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private java.time.LocalDate manufactureDate;
    private Boolean rfidEnabled;

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
    public DeviceModel getDeviceModel() { return deviceModel; }
    public void setDeviceModel(DeviceModel deviceModel) { this.deviceModel = deviceModel; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getInventoryNumber() { return inventoryNumber; }
    public void setInventoryNumber(String inventoryNumber) { this.inventoryNumber = inventoryNumber; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCommissionDate() { return commissionDate; }
    public void setCommissionDate(LocalDateTime commissionDate) { this.commissionDate = commissionDate; }
    public Integer getManufactureYear() { return manufactureYear; }
    public void setManufactureYear(Integer manufactureYear) { this.manufactureYear = manufactureYear; }
    public LocalDateTime getLastService() { return lastService; }
    public void setLastService(LocalDateTime lastService) { this.lastService = lastService; }
    public LocalDateTime getLastPoweredOnAt() { return lastPoweredOnAt; }
    public void setLastPoweredOnAt(LocalDateTime lastPoweredOnAt) { this.lastPoweredOnAt = lastPoweredOnAt; }
    public LocalDateTime getLastWeldAt() { return lastWeldAt; }
    public void setLastWeldAt(LocalDateTime lastWeldAt) { this.lastWeldAt = lastWeldAt; }
    public OrganizationUnitShortDTO getOrganizationUnit() { return organizationUnit; }
    public void setOrganizationUnit(OrganizationUnitShortDTO organizationUnit) { this.organizationUnit = organizationUnit; }
    public WeldingMachineTypeShortDTO getWeldingMachineType() { return weldingMachineType; }
    public void setWeldingMachineType(WeldingMachineTypeShortDTO weldingMachineType) { this.weldingMachineType = weldingMachineType; }
    public String getModules() { return modules; }
    public void setModules(String modules) { this.modules = modules; }
    public Double getMaintenanceInterval() { return maintenanceInterval; }
    public void setMaintenanceInterval(Double maintenanceInterval) { this.maintenanceInterval = maintenanceInterval; }
    public String getMaintenanceIntervalUnit() { return maintenanceIntervalUnit; }
    public void setMaintenanceIntervalUnit(String maintenanceIntervalUnit) { this.maintenanceIntervalUnit = maintenanceIntervalUnit; }
    public java.time.LocalDate getManufactureDate() { return manufactureDate; }
    public void setManufactureDate(java.time.LocalDate manufactureDate) { this.manufactureDate = manufactureDate; }
    public Boolean getRfidEnabled() { return rfidEnabled; }
    public void setRfidEnabled(Boolean rfidEnabled) { this.rfidEnabled = rfidEnabled; }
    public Integer getMaintenanceRegulation() { return maintenanceRegulation; }
    public void setMaintenanceRegulation(Integer maintenanceRegulation) { this.maintenanceRegulation = maintenanceRegulation; }
    public Integer getUserServiceNotifiedBeforeHours() { return userServiceNotifiedBeforeHours; }
    public void setUserServiceNotifiedBeforeHours(Integer userServiceNotifiedBeforeHours) { this.userServiceNotifiedBeforeHours = userServiceNotifiedBeforeHours; }
} 