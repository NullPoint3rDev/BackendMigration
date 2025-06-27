package org.alloy.models.dto;

public class MaintenanceDTO {
    private Integer id;
    private String description;
    private WeldingMachineShortDTO weldingMachine;
    private UserAccountShortDTO userAccount;
    // ... другие нужные поля
    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public WeldingMachineShortDTO getWeldingMachine() { return weldingMachine; }
    public void setWeldingMachine(WeldingMachineShortDTO weldingMachine) { this.weldingMachine = weldingMachine; }
    public UserAccountShortDTO getUserAccount() { return userAccount; }
    public void setUserAccount(UserAccountShortDTO userAccount) { this.userAccount = userAccount; }
} 