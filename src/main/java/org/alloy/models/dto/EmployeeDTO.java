package org.alloy.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.alloy.models.GeneralStatus;
import org.alloy.models.EmployeeType;

@Schema(description = "DTO для сотрудника")
public class EmployeeDTO {
    
    @Schema(description = "Уникальный идентификатор")
    private Long id;
    
    @Schema(description = "Логин для входа в систему")
    private String username;
    
    @Schema(description = "Пароль для входа в систему")
    private String password;
    
    @Schema(description = "ФИО сотрудника")
    private String fullName;
    
    @Schema(description = "Email сотрудника")
    private String email;
    
    @Schema(description = "Тип сотрудника")
    private EmployeeType employeeType;
    
    @Schema(description = "Должность")
    private String position;
    
    @Schema(description = "Телефон")
    private String phone;
    
    @Schema(description = "Подразделение")
    private OrganizationUnitShortDTO organizationUnit;
    
    @Schema(description = "Роль пользователя")
    private UserRoleShortDTO userRole;
    
    @Schema(description = "ID роли пользователя (для создания/обновления)")
    private Integer userRoleId;
    
    @Schema(description = "Статус сотрудника")
    private GeneralStatus status;
    
    @Schema(description = "Путь к аватарке")
    private String photo;
    
    @Schema(description = "Дата создания")
    private String dateCreated;
    
    @Schema(description = "Дата обновления")
    private String dateUpdated;
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public EmployeeType getEmployeeType() {
        return employeeType;
    }
    
    public void setEmployeeType(EmployeeType employeeType) {
        this.employeeType = employeeType;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public OrganizationUnitShortDTO getOrganizationUnit() {
        return organizationUnit;
    }
    
    public void setOrganizationUnit(OrganizationUnitShortDTO organizationUnit) {
        this.organizationUnit = organizationUnit;
    }
    
    public UserRoleShortDTO getUserRole() {
        return userRole;
    }
    
    public void setUserRole(UserRoleShortDTO userRole) {
        this.userRole = userRole;
    }
    
    public Integer getUserRoleId() {
        return userRoleId;
    }
    
    public void setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
    }
    
    public GeneralStatus getStatus() {
        return status;
    }
    
    public void setStatus(GeneralStatus status) {
        this.status = status;
    }
    
    public String getPhoto() {
        return photo;
    }
    
    public void setPhoto(String photo) {
        this.photo = photo;
    }
    
    public String getDateCreated() {
        return dateCreated;
    }
    
    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    public String getDateUpdated() {
        return dateUpdated;
    }
    
    public void setDateUpdated(String dateUpdated) {
        this.dateUpdated = dateUpdated;
    }
}
