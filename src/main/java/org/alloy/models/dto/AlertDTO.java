package org.alloy.models.dto;

public class AlertDTO {
    private Integer id;
    private String message;
    private String type;
    // ... другие нужные поля
    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
} 