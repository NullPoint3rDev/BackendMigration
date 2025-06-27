package org.alloy.models.dto;

public class UserAccountDTO {
    private Integer id;
    private String username;
    private String email;
    private String fullName;
    private OrganizationUnitShortDTO organizationUnit;
    // ... другие нужные поля

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public OrganizationUnitShortDTO getOrganizationUnit() { return organizationUnit; }
    public void setOrganizationUnit(OrganizationUnitShortDTO organizationUnit) { this.organizationUnit = organizationUnit; }
} 