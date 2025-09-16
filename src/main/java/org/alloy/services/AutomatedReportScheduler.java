package org.alloy.services;

import org.alloy.models.entities.AutomatedReport;
import org.alloy.models.ReportHistory;
import org.alloy.models.entities.Notification;
import org.alloy.models.entities.UserAccount;
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

    @Autowired
    private AutomatedReportDataFixService dataFixService;

    @Autowired
    private UserAccountService userAccountService;

    /**
     * Проверяет и выполняет автоматические отчеты каждую минуту
     */
    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void checkAndExecuteAutomatedReports() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        System.out.println("DEBUG AutomatedReportScheduler: Checking for automated reports to execute at " + now + " (UTC)");
        
        try {
            // Получаем все активные автоматические отчеты
            List<AutomatedReport> activeReports = automatedReportService.getActiveAutomatedReports();
            
            System.out.println("DEBUG AutomatedReportScheduler: Found " + activeReports.size() + " active automated reports");
            
            for (AutomatedReport automatedReport : activeReports) {
                System.out.println("DEBUG AutomatedReportScheduler: Checking report: " + automatedReport.getName() + 
                                  " (ID: " + automatedReport.getId() + ", templateType: " + automatedReport.getTemplateType() + 
                                  ", nextRun: " + automatedReport.getNextRun() + ")");
                
                // Проверяем, нужно ли выполнить отчет
                if (shouldExecuteReport(automatedReport)) {
                    System.out.println("DEBUG AutomatedReportScheduler: Executing automated report: " + automatedReport.getName());
                    executeAutomatedReport(automatedReport);
                } else {
                    System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + " - not ready for execution");
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
            System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + " - nextRun is null, skipping");
            return false;
        }
        
        // Получаем текущее время в UTC
        LocalDateTime nowUTC = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime nextRun = automatedReport.getNextRun();
        
        // Конвертируем время выполнения в UTC (вычитаем 3 часа для московского времени)
        LocalDateTime nextRunUTC = nextRun.minusHours(3);
        
        // Выполняем отчет, если время пришло (с точностью до минуты)
        // Добавляем небольшую задержку (30 секунд) для надежности
        LocalDateTime executionTime = nextRunUTC.plusSeconds(30);
        boolean shouldExecute = executionTime.isBefore(nowUTC) || executionTime.isEqual(nowUTC);
        
        // Дополнительная проверка: если разница меньше 2 минут, тоже выполняем
        long minutesDiff = java.time.Duration.between(nowUTC, nextRunUTC).toMinutes();
        if (Math.abs(minutesDiff) <= 2) {
            shouldExecute = true;
            System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + 
                              " - Time difference is " + minutesDiff + " minutes, executing anyway");
        }
        
        // ДОПОЛНИТЕЛЬНАЯ ЗАЩИТА: если отчет уже выполнен сегодня, не выполняем повторно
        if (shouldExecute) {
            // Проверяем, был ли уже выполнен отчет сегодня
            String templateType = automatedReport.getTemplateType();
            if (templateType != null && !templateType.trim().isEmpty()) {
                LocalDateTime todayStart = nowUTC.toLocalDate().atStartOfDay();
                List<ReportHistory> todayReports = reportHistoryService.getRecentReports(templateType)
                    .stream()
                    .filter(r -> r.getGeneratedAt() != null && r.getGeneratedAt().isAfter(todayStart))
                    .filter(r -> r.getAutomatedReportId() != null && r.getAutomatedReportId().equals(automatedReport.getId()))
                    .collect(java.util.stream.Collectors.toList());
                
                if (!todayReports.isEmpty()) {
                    System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + 
                                      " - Already executed today (" + todayReports.size() + " times), skipping");
                    return false;
                }
            } else {
                System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + 
                                  " - templateType is null or empty, skipping duplicate check");
            }
        }
        
        System.out.println("DEBUG AutomatedReportScheduler: Report " + automatedReport.getName() + 
                          " - nowUTC: " + nowUTC + ", nextRun (Moscow): " + nextRun + ", nextRunUTC: " + nextRunUTC + 
                          ", executionTime: " + executionTime + ", minutesDiff: " + minutesDiff + ", shouldExecute: " + shouldExecute);
        
        return shouldExecute;
    }

    /**
     * Выполняет автоматический отчет
     */
    private void executeAutomatedReport(AutomatedReport automatedReport) {
        try {
            // СНАЧАЛА обновляем время следующего запуска, чтобы предотвратить повторное выполнение
            updateNextRunTime(automatedReport);
            
            // Автоматически исправляем данные если они некорректные
            dataFixService.fixAutomatedReportData(automatedReport);
            
            // Проверяем обязательные поля
            if (automatedReport.getTemplateType() == null || automatedReport.getTemplateType().trim().isEmpty()) {
                throw new IllegalArgumentException("Template type is required for automated report: " + automatedReport.getName());
            }
            
            if (automatedReport.getTemplateName() == null || automatedReport.getTemplateName().trim().isEmpty()) {
                throw new IllegalArgumentException("Template name is required for automated report: " + automatedReport.getName());
            }
            
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
        String templateType = automatedReport.getTemplateType();
        if (templateType == null || templateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Template type is null or empty for automated report: " + automatedReport.getName());
        }
        
        switch (templateType.toLowerCase()) {
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
        try {
            // Получаем правильный user_accountid для уведомления
            Integer userId = getValidUserId(automatedReport.getCreatedBy());
            if (userId == null) {
                System.out.println("WARN AutomatedReportScheduler: Cannot create notification - no valid user found for report: " + automatedReport.getName());
                return;
            }
            
            Notification notification = new Notification();
            notification.setUserAccountId(userId);
            notification.setTitle("Отчет сгенерирован автоматически");
            notification.setMessage(String.format("Автоматический отчет '%s' успешно сгенерирован и сохранен.", automatedReport.getName()));
            notification.setType("AUTOMATED_REPORT");
            notification.setIsRead(false);
            notification.setLink("/my-reports"); // Ссылка на страницу с отчетами
            notification.setDateCreated(LocalDateTime.now()); // Устанавливаем текущую дату и время
            
            notificationService.createNotification(notification);
            
            System.out.println("DEBUG AutomatedReportScheduler: Created success notification for user " + userId);
            System.out.println("DEBUG AutomatedReportScheduler: Notification details - Title: " + notification.getTitle() + ", Type: " + notification.getType() + ", UserId: " + notification.getUserAccountId());
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to create success notification: " + e.getMessage());
            // Не прерываем выполнение из-за ошибки уведомления
        }
    }

    /**
     * Создает уведомление об ошибке
     */
    private void createErrorNotification(AutomatedReport automatedReport, String errorMessage) {
        try {
            // Получаем правильный user_accountid для уведомления
            Integer userId = getValidUserId(automatedReport.getCreatedBy());
            if (userId == null) {
                System.out.println("WARN AutomatedReportScheduler: Cannot create error notification - no valid user found for report: " + automatedReport.getName());
                return;
            }
            
            Notification notification = new Notification();
            notification.setUserAccountId(userId);
            notification.setTitle("Ошибка генерации автоматического отчета");
            notification.setMessage(String.format("Не удалось сгенерировать автоматический отчет '%s': %s", automatedReport.getName(), errorMessage));
            notification.setType("AUTOMATED_REPORT_ERROR");
            notification.setIsRead(false);
            notification.setLink("/automated-reports"); // Ссылка на страницу автоматических отчетов
            notification.setDateCreated(LocalDateTime.now()); // Устанавливаем текущую дату и время
            
            notificationService.createNotification(notification);
            
            System.out.println("DEBUG AutomatedReportScheduler: Created error notification for user " + userId);
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to create error notification: " + e.getMessage());
            // Не прерываем выполнение из-за ошибки уведомления
        }
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
    
    /**
     * Получает валидный user_accountid для уведомления
     * Если переданный ID не существует, возвращает ID первого существующего пользователя
     */
    private Integer getValidUserId(Integer originalUserId) {
        try {
            // Если переданный ID существует, используем его
            if (originalUserId != null && userAccountService.existsById(originalUserId)) {
                return originalUserId;
            }
            
            // Иначе находим первого существующего пользователя
            List<UserAccount> allUsers = userAccountService.getAllUserAccounts();
            if (!allUsers.isEmpty()) {
                Integer firstUserId = allUsers.get(0).getId();
                System.out.println("DEBUG AutomatedReportScheduler: Using first available user ID: " + firstUserId + " instead of invalid ID: " + originalUserId);
                return firstUserId;
            }
            
            System.err.println("ERROR AutomatedReportScheduler: No users found in the system");
            return null;
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportScheduler: Failed to get valid user ID: " + e.getMessage());
            return null;
        }
    }

}
