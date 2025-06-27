package org.alloy.models.dto;

public class UserRolePermissionDTO {
    private Integer id;
    private Integer roleId;
    private Integer permissionId;
    // ... другие нужные поля

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public Integer getPermissionId() { return permissionId; }
    public void setPermissionId(Integer permissionId) { this.permissionId = permissionId; }
    // ... геттеры и сеттеры для других полей
} 