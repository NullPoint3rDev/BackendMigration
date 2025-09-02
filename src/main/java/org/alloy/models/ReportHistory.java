package org.alloy.models;

import java.time.LocalDateTime;

/**
 * Модель для хранения истории сгенерированных отчетов
 */
public class ReportHistory {
    private Long id;
    private String reportType;        // Тип отчета (equipment, welders, materials, etc.)
    private String reportName;        // Название отчета на русском
    private String format;            // Формат отчета (PDF, EXCEL, CSV)
    private String period;            // Период отчета
    private LocalDateTime generatedAt; // Время генерации
    private String fileName;          // Имя файла для скачивания
    private Long fileSize;            // Размер файла в байтах
    private String generatedBy;       // Кто сгенерировал (пока можно оставить "Система")

    public ReportHistory() {}

    public ReportHistory(String reportType, String reportName, String format, String period, 
                        String fileName, Long fileSize, String generatedBy) {
        this.reportType = reportType;
        this.reportName = reportName;
        this.format = format;
        this.period = period;
        this.generatedAt = LocalDateTime.now();
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.generatedBy = generatedBy;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    /**
     * Получить размер файла в читаемом формате
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " Б";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f КБ", fileSize / 1024.0);
        } else {
            return String.format("%.1f МБ", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * Получить расширение файла
     */
    public String getFileExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
        }
        return format;
    }
}
