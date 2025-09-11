package org.alloy.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "DTO для автоматизированного отчета")
public class AutomatedReportDTO {

    @Schema(description = "ID автоматизированного отчета", example = "1")
    private Long id;

    @Schema(description = "Название автоматизированного отчета", example = "Еженедельный отчет по оборудованию")
    private String name;

    @Schema(description = "Описание автоматизированного отчета", example = "Автоматическая генерация отчета по работе оборудования каждую неделю")
    private String description;

    @Schema(description = "ID шаблона отчета", example = "1")
    private Integer templateId;

    @Schema(description = "Название шаблона отчета", example = "Отчет по работе оборудования")
    private String templateName;

    @Schema(description = "Тип шаблона отчета", example = "equipment")
    private String templateType;

    @Schema(description = "Активен ли автоматизированный отчет", example = "true")
    private Boolean isActive;

    @Schema(description = "ID пользователя, создавшего отчет", example = "1")
    private Integer createdBy;

    @Schema(description = "Имя пользователя, создавшего отчет", example = "Администратор")
    private String createdByName;

    @Schema(description = "Дата создания", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Дата последнего обновления", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "Дата последнего запуска", example = "2024-01-15T09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastRun;

    @Schema(description = "Дата следующего запуска", example = "2024-01-22T09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextRun;

    @Schema(description = "Количество запусков", example = "5")
    private Integer runCount;

    @Schema(description = "Количество успешных запусков", example = "4")
    private Integer successCount;

    @Schema(description = "Количество ошибок", example = "1")
    private Integer errorCount;

    @Schema(description = "Последнее сообщение об ошибке", example = "Ошибка подключения к базе данных")
    private String lastErrorMessage;

    @Schema(description = "Конфигурация отчета в JSON формате")
    private String configuration;

    @Schema(description = "Список триггеров")
    private List<TriggerDTO> triggers;

    @Schema(description = "Включены ли email уведомления", example = "true")
    private Boolean emailNotifications;

    @Schema(description = "Список получателей email уведомлений", example = "admin@company.com,manager@company.com")
    private String emailRecipients;

    @Schema(description = "Количество повторных попыток", example = "2")
    private Integer retryCount;

    @Schema(description = "Максимальное количество повторных попыток", example = "3")
    private Integer maxRetries;

    @Schema(description = "Задержка между повторными попытками в минутах", example = "30")
    private Integer retryDelayMinutes;

    @Schema(description = "Часовой пояс", example = "UTC")
    private String timezone;

    @Schema(description = "Приоритет выполнения (1-10)", example = "5")
    private Integer priority;

    @Schema(description = "Теги для группировки", example = "weekly,equipment,automated")
    private String tags;

    @Schema(description = "Статус отчета", example = "ACTIVE")
    private String status;

    @Schema(description = "Процент успешности", example = "80.0")
    private Double successRate;

    // Конструкторы
    public AutomatedReportDTO() {}

    public AutomatedReportDTO(String name, Integer templateId, String templateName, Integer createdBy) {
        this.name = name;
        this.templateId = templateId;
        this.templateName = templateName;
        this.createdBy = createdBy;
        this.isActive = true;
        this.runCount = 0;
        this.successCount = 0;
        this.errorCount = 0;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.retryDelayMinutes = 30;
        this.timezone = "UTC";
        this.priority = 5;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastRun() {
        return lastRun;
    }

    public void setLastRun(LocalDateTime lastRun) {
        this.lastRun = lastRun;
    }

    public LocalDateTime getNextRun() {
        return nextRun;
    }

    public void setNextRun(LocalDateTime nextRun) {
        this.nextRun = nextRun;
    }

    public Integer getRunCount() {
        return runCount;
    }

    public void setRunCount(Integer runCount) {
        this.runCount = runCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public List<TriggerDTO> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<TriggerDTO> triggers) {
        this.triggers = triggers;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public String getEmailRecipients() {
        return emailRecipients;
    }

    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = emailRecipients;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getRetryDelayMinutes() {
        return retryDelayMinutes;
    }

    public void setRetryDelayMinutes(Integer retryDelayMinutes) {
        this.retryDelayMinutes = retryDelayMinutes;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    // Вспомогательные методы
    public void calculateSuccessRate() {
        if (runCount != null && runCount > 0 && successCount != null) {
            this.successRate = (double) successCount / runCount * 100;
        } else {
            this.successRate = 0.0;
        }
    }

    public boolean isOverdue() {
        if (nextRun == null || !isActive) {
            return false;
        }
        return nextRun.isBefore(LocalDateTime.now());
    }

    public boolean canRetry() {
        return retryCount != null && maxRetries != null && retryCount < maxRetries;
    }

    @Override
    public String toString() {
        return "AutomatedReportDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", templateId=" + templateId +
                ", templateName='" + templateName + '\'' +
                ", isActive=" + isActive +
                ", createdBy=" + createdBy +
                ", lastRun=" + lastRun +
                ", nextRun=" + nextRun +
                ", runCount=" + runCount +
                '}';
    }
}
