package org.alloy.models.dto;

public class QueueTaskDTO {
    private Integer id;
    private String name;
    private String status;
    // ... другие нужные поля
    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
} 