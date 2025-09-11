package org.alloy.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO для триггера автоматизированного отчета")
public class TriggerDTO {

    @Schema(description = "Тип триггера", example = "TIME", allowableValues = {"TIME", "EQUIPMENT_ERROR", "VALUE_THRESHOLD"})
    private String type;

    @Schema(description = "Значение триггера", example = "weekly")
    private String value;

    @Schema(description = "Описание триггера", example = "Каждую неделю в понедельник в 09:00")
    private String description;

    @Schema(description = "Дополнительные параметры триггера")
    private String parameters;

    @Schema(description = "Активен ли триггер", example = "true")
    private Boolean isActive = true;

    @Schema(description = "Приоритет триггера (1-10)", example = "5")
    private Integer priority = 5;

    @Schema(description = "Часовой пояс для временных триггеров", example = "UTC")
    private String timezone = "UTC";

    @Schema(description = "Время выполнения для временных триггеров", example = "09:00")
    private String time;

    @Schema(description = "Дни недели для временных триггеров", example = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
    private String daysOfWeek;

    @Schema(description = "День месяца для месячных триггеров", example = "1")
    private Integer dayOfMonth;

    @Schema(description = "Пороговое значение для триггеров по значениям", example = "100")
    private Double thresholdValue;

    @Schema(description = "Оператор сравнения для триггеров по значениям", example = "GREATER_THAN", allowableValues = {"GREATER_THAN", "LESS_THAN", "EQUALS", "NOT_EQUALS"})
    private String operator;

    @Schema(description = "ID оборудования для триггеров по ошибкам/значениям", example = "1,2,3")
    private String equipmentIds;

    @Schema(description = "Параметр для триггеров по значениям", example = "temperature", allowableValues = {"temperature", "current", "voltage", "power"})
    private String parameter;

    @Schema(description = "Количество ошибок для триггеров по ошибкам", example = "5")
    private Integer errorCount;

    @Schema(description = "Период времени для подсчета ошибок в минутах", example = "60")
    private Integer timeWindowMinutes;

    // Конструкторы
    public TriggerDTO() {}

    public TriggerDTO(String type, String value, String description) {
        this.type = type;
        this.value = value;
        this.description = description;
        this.isActive = true;
        this.priority = 5;
    }

    // Геттеры и сеттеры
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public Double getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Double thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(String equipmentIds) {
        this.equipmentIds = equipmentIds;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Integer getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(Integer timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    // Вспомогательные методы
    public boolean isTimeTrigger() {
        return "TIME".equals(type);
    }

    @JsonIgnore
    public boolean isEquipmentErrorTrigger() {
        return "EQUIPMENT_ERROR".equals(type);
    }

    @JsonIgnore
    public boolean isValueThresholdTrigger() {
        return "VALUE_THRESHOLD".equals(type);
    }

    @JsonIgnore
    public String[] getEquipmentIdArray() {
        if (equipmentIds == null || equipmentIds.trim().isEmpty()) {
            return new String[0];
        }
        return equipmentIds.split(",");
    }

    public void setEquipmentIdArray(String[] ids) {
        if (ids == null || ids.length == 0) {
            this.equipmentIds = null;
        } else {
            this.equipmentIds = String.join(",", ids);
        }
    }

    @JsonIgnore
    public String[] getDaysOfWeekArray() {
        if (daysOfWeek == null || daysOfWeek.trim().isEmpty()) {
            return new String[0];
        }
        return daysOfWeek.split(",");
    }

    public void setDaysOfWeekArray(String[] days) {
        if (days == null || days.length == 0) {
            this.daysOfWeek = null;
        } else {
            this.daysOfWeek = String.join(",", days);
        }
    }

    @JsonIgnore
    public boolean isValid() {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }

        switch (type) {
            case "TIME":
                return value != null && !value.trim().isEmpty() && description != null;
            case "EQUIPMENT_ERROR":
                return errorCount != null && errorCount > 0 && description != null;
            case "VALUE_THRESHOLD":
                return parameter != null && operator != null && thresholdValue != null && description != null;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "TriggerDTO{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                ", priority=" + priority +
                '}';
    }
}
