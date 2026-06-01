package org.alloy.models.entities;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "automated_reports")
public class AutomatedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "template_name", nullable = false, length = 255)
    private String templateName;

    @Column(name = "template_type", length = 100)
    private String templateType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_run")
    private LocalDateTime lastRun;

    @Column(name = "next_run")
    private LocalDateTime nextRun;

    @Column(name = "run_count")
    private Integer runCount = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "error_count")
    private Integer errorCount = 0;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Lob
    @Column(name = "configuration")
    private String configuration;

    @Lob
    @Column(name = "triggers_config")
    private String triggersConfig;

    @Column(name = "email_notifications")
    private Boolean emailNotifications = false;

    @Column(name = "email_recipients", length = 1000)
    private String emailRecipients;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "retry_delay_minutes")
    private Integer retryDelayMinutes = 30;

    @Column(name = "timezone", length = 50)
    private String timezone = "UTC";

    @Column(name = "priority")
    private Integer priority = 5; // 1-10, где 1 - наивысший приоритет

    @Column(name = "tags", length = 500)
    private String tags;

    // Конструкторы
    public AutomatedReport() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public AutomatedReport(String name, Long templateId, String templateName, Integer createdBy) {
        this();
        this.name = name;
        this.templateId = templateId;
        this.templateName = templateName;
        this.createdBy = createdBy;
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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
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

    public String getTriggersConfig() {
        return triggersConfig;
    }

    public void setTriggersConfig(String triggersConfig) {
        this.triggersConfig = triggersConfig;
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

    // Методы для работы с триггерами
    public void incrementRunCount() {
        this.runCount = (this.runCount == null) ? 1 : this.runCount + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementSuccessCount() {
        this.successCount = (this.successCount == null) ? 1 : this.successCount + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementErrorCount() {
        this.errorCount = (this.errorCount == null) ? 1 : this.errorCount + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLastRun() {
        this.lastRun = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateNextRun(LocalDateTime nextRun) {
        this.nextRun = nextRun;
        this.updatedAt = LocalDateTime.now();
    }

    public void setError(String errorMessage) {
        this.lastErrorMessage = errorMessage;
        this.incrementErrorCount();
    }

    public void clearError() {
        this.lastErrorMessage = null;
        this.retryCount = 0;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
    }

    public void resetRetryCount() {
        this.retryCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "AutomatedReport{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", templateId=" + templateId +
                ", templateName='" + templateName + '\'' +
                ", isActive=" + isActive +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", lastRun=" + lastRun +
                ", nextRun=" + nextRun +
                ", runCount=" + runCount +
                '}';
    }
}
