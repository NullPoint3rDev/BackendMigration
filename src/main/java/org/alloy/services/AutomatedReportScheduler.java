package org.alloy.services;

import org.alloy.models.entities.AutomatedReport;
import org.alloy.models.ReportHistory;
import org.alloy.models.entities.Notification;
import org.alloy.models.dto.ReportRequestDTO;
import org.alloy.models.dto.WireConsumptionReportDTO;
import org.alloy.models.dto.WelderReportDTO;
import org.alloy.models.dto.WorkReportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class AutomatedReportScheduler {

    @Autowired
    private AutomatedReportService automatedReportService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDataService reportDataService;

    @Autowired
    private ReportHistoryService reportHistoryService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Проверяет и выполняет автоматические отчеты каждую минуту
     */
    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void checkAndExecuteAutomatedReports() {
        System.out.println("DEBUG AutomatedReportScheduler: Checking for automated reports to execute at " + LocalDateTime.now());
        
        try {
            // Получаем все активные автоматические отчеты
            List<AutomatedReport> activeReports = automatedReportService.getActiveAutomatedReports();
            
            System.out.println("DEBUG AutomatedReportScheduler: Found " + activeReports.size() + " active automated reports");
            
            for (AutomatedReport automatedReport : activeReports) {
                // Проверяем, нужно ли выполнить отчет
                if (shouldExecuteReport(automatedReport)) {
                    System.out.println("DEBUG AutomatedReportScheduler: Executing automated report: " + automatedReport.getName());
                    executeAutomatedReport(automatedReport);
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Error checking automated reports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Проверяет, нужно ли выполнить отчет
     */
    private boolean shouldExecuteReport(AutomatedReport automatedReport) {
        if (automatedReport.getNextRun() == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime nextRun = automatedReport.getNextRun();
        
        // Выполняем отчет, если время пришло (с точностью до минуты)
        boolean shouldExecute = nextRun.isBefore(now) || nextRun.isEqual(now);
        
        System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + 
                          " - now: " + now + ", nextRun: " + nextRun + ", shouldExecute: " + shouldExecute);
        
        return shouldExecute;
    }

    /**
     * Выполняет автоматический отчет
     */
    private void executeAutomatedReport(AutomatedReport automatedReport) {
        try {
            // Создаем запрос для генерации отчета
            ReportRequestDTO reportRequest = createReportRequest(automatedReport);
            
            // Генерируем отчет в зависимости от типа шаблона
            byte[] reportBytes = generateReportByType(reportRequest, automatedReport);
            
            // Создаем имя файла
            String fileName = createFileName(automatedReport);
            
            // Сохраняем отчет в историю
            ReportHistory reportHistory = new ReportHistory(
                automatedReport.getTemplateType(),
                automatedReport.getTemplateName(),
                "PDF", // По умолчанию PDF
                "AUTO", // Автоматический период
                fileName,
                (long) reportBytes.length,
                "Система",
                automatedReport.getId()
            );
            
            reportHistoryService.addReportToHistory(reportHistory);
            
            // Создаем уведомление об успешной генерации
            createSuccessNotification(automatedReport, reportHistory);
            
            // Обновляем время следующего запуска
            updateNextRunTime(automatedReport);
            
            System.out.println("DEBUG AutomatedReportScheduler: Successfully executed automated report: " + automatedReport.getName());
            
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to execute automated report " + automatedReport.getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            // Создаем уведомление об ошибке
            createErrorNotification(automatedReport, e.getMessage());
        }
    }

    /**
     * Создает запрос для генерации отчета
     */
    private ReportRequestDTO createReportRequest(AutomatedReport automatedReport) {
        ReportRequestDTO request = new ReportRequestDTO();
        request.setReportType(automatedReport.getTemplateType());
        request.setFormat("PDF");
        request.setPeriod("DAY"); // По умолчанию за день
        request.setDateFrom(LocalDateTime.now().minusDays(1).toLocalDate());
        request.setDateTo(LocalDateTime.now().toLocalDate());
        
        return request;
    }

    /**
     * Генерирует отчет в зависимости от типа
     */
    private byte[] generateReportByType(ReportRequestDTO request, AutomatedReport automatedReport) throws Exception {
        switch (automatedReport.getTemplateType().toLowerCase()) {
            case "equipment":
                List<WorkReportDTO> workData = reportDataService.getWorkReportData(request);
                return reportService.generateWorkReport(workData, request.getFormat());
                
            case "welders":
                List<WelderReportDTO> welderData = reportDataService.getWelderReportData(request);
                return reportService.generateWelderReport(welderData, request.getFormat());
                
            case "materials":
            case "wire-consumption":
                List<WireConsumptionReportDTO> wireData = reportDataService.getWireConsumptionData(request);
                return reportService.generateWireConsumptionReport(wireData, request.getFormat());
                
            default:
                throw new IllegalArgumentException("Unknown report type: " + automatedReport.getTemplateType());
        }
    }

    /**
     * Создает имя файла для отчета
     */
    private String createFileName(AutomatedReport automatedReport) {
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s_%s.pdf", 
            automatedReport.getTemplateType(), 
            automatedReport.getName().replaceAll("[^a-zA-Z0-9]", "_"),
            timestamp);
    }

    /**
     * Создает уведомление об успешной генерации
     */
    private void createSuccessNotification(AutomatedReport automatedReport, ReportHistory reportHistory) {
        Notification notification = new Notification();
        notification.setUserAccountId(automatedReport.getCreatedBy());
        notification.setTitle("Отчет сгенерирован автоматически");
        notification.setMessage(String.format("Автоматический отчет '%s' успешно сгенерирован и сохранен.", automatedReport.getName()));
        notification.setType("AUTOMATED_REPORT");
        notification.setIsRead(false);
        notification.setLink("/reports/history"); // Ссылка на страницу с отчетами
        
        notificationService.createNotification(notification);
        
        System.out.println("DEBUG AutomatedReportScheduler: Created success notification for user " + automatedReport.getCreatedBy());
    }

    /**
     * Создает уведомление об ошибке
     */
    private void createErrorNotification(AutomatedReport automatedReport, String errorMessage) {
        Notification notification = new Notification();
        notification.setUserAccountId(automatedReport.getCreatedBy());
        notification.setTitle("Ошибка генерации автоматического отчета");
        notification.setMessage(String.format("Не удалось сгенерировать автоматический отчет '%s': %s", automatedReport.getName(), errorMessage));
        notification.setType("AUTOMATED_REPORT_ERROR");
        notification.setIsRead(false);
        notification.setLink("/automated-reports"); // Ссылка на страницу автоматических отчетов
        
        notificationService.createNotification(notification);
        
        System.out.println("DEBUG AutomatedReportScheduler: Created error notification for user " + automatedReport.getCreatedBy());
    }

    /**
     * Обновляет время следующего запуска
     */
    private void updateNextRunTime(AutomatedReport automatedReport) {
        // Пересчитываем следующее время выполнения
        automatedReportService.calculateNextRunTime(automatedReport);
        automatedReportService.updateAutomatedReport(automatedReport);
        
        System.out.println("DEBUG AutomatedReportScheduler: Updated next run time for report " + automatedReport.getName() + " to " + automatedReport.getNextRun());
    }
}
