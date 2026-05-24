package org.alloy.models.dto;

import java.util.List;

public class UserAccountDTO {
    private Integer id;
    private String username;
    private String email;
    /** Подтверждён ли email (только чтение с сервера). */
    private Boolean emailVerified;
    private String fullName;
    private Integer organizationId;
    private OrganizationUnitShortDTO organizationUnit;
    private Integer userRoleId;
    private String position;
    private String phone;
    private String status;
    /** Есть ли активная сессия (пользователь залогинен). */
    private Boolean online;
    private String workplace;
    private String category;
    private String personnelNumber;
    private String rfid;
    private String recruitmentDate;
    private String birthDate;
    private String education;
    private String address;
    private String description;
    private String photo;
    private List<String> allowedUserActions;
    /** Пароль (только для создания/обновления, не возвращается в ответах). */
    private String password;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getOrganizationId() { return organizationId; }
    public void setOrganizationId(Integer organizationId) { this.organizationId = organizationId; }
    public OrganizationUnitShortDTO getOrganizationUnit() { return organizationUnit; }
    public void setOrganizationUnit(OrganizationUnitShortDTO organizationUnit) { this.organizationUnit = organizationUnit; }
    public Integer getUserRoleId() { return userRoleId; }
    public void setUserRoleId(Integer userRoleId) { this.userRoleId = userRoleId; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getOnline() { return online; }
    public void setOnline(Boolean online) { this.online = online; }
    public String getWorkplace() { return workplace; }
    public void setWorkplace(String workplace) { this.workplace = workplace; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPersonnelNumber() { return personnelNumber; }
    public void setPersonnelNumber(String personnelNumber) { this.personnelNumber = personnelNumber; }
    public String getRfid() { return rfid; }
    public void setRfid(String rfid) { this.rfid = rfid; }
    public String getRecruitmentDate() { return recruitmentDate; }
    public void setRecruitmentDate(String recruitmentDate) { this.recruitmentDate = recruitmentDate; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
    public List<String> getAllowedUserActions() { return allowedUserActions; }
    public void setAllowedUserActions(List<String> allowedUserActions) { this.allowedUserActions = allowedUserActions; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
} 