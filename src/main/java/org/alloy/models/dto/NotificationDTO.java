package org.alloy.models.dto;

public class NotificationDTO {
    private Integer id;
    private String message;
    private String type;
    private UserAccountShortDTO userAccount;
    // ... другие нужные поля
    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public UserAccountShortDTO getUserAccount() { return userAccount; }
    public void setUserAccount(UserAccountShortDTO userAccount) { this.userAccount = userAccount; }
} 