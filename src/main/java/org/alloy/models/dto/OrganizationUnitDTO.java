package org.alloy.models.dto;

public class OrganizationUnitDTO {
    private Integer id;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String email;
    private Integer level;
    private String parentDepartment;
    private OrganizationShortDTO organization;
    // ... другие нужные поля

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public OrganizationShortDTO getOrganization() { return organization; }
    public void setOrganization(OrganizationShortDTO organization) { this.organization = organization; }
    
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    
    public String getParentDepartment() { return parentDepartment; }
    public void setParentDepartment(String parentDepartment) { this.parentDepartment = parentDepartment; }
} 