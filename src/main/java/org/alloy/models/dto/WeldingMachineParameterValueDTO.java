package org.alloy.models.dto;

public class WeldingMachineParameterValueDTO {
    private Integer id;
    private String parameterName;
    private String value;
    // ... другие нужные поля

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    // ... геттеры и сеттеры для других полей
} 