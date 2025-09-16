package org.alloy.services;

import org.alloy.models.ReportHistory;
import org.alloy.repositories.ReportHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления историей сгенерированных отчетов
 */
@Service
public class ReportHistoryService {
    
    @Autowired
    private ReportHistoryRepository reportHistoryRepository;
    
    // Временное хранилище в памяти (в реальном проекте это должна быть база данных)
    private final Map<String, List<ReportHistory>> reportHistory = new ConcurrentHashMap<>();
    
    // Максимальное количество отчетов для каждого типа
    private static final int MAX_HISTORY_SIZE = 5;
    
    public ReportHistoryService() {
        System.out.println("ReportHistoryService: Сервис инициализирован");
        System.out.println("ReportHistoryService: Готов к работе с историей отчетов");
    }
    
    /**
     * Добавить отчет в историю
     */
    public void addReportToHistory(ReportHistory report) {
        try {
            String reportType = report.getReportType();
            
            System.out.println("ReportHistoryService: Добавляем отчет типа '" + reportType + "' в историю");
            System.out.println("ReportHistoryService: Детали отчета - Название: '" + report.getReportName() + "', Формат: '" + report.getFormat() + "', Размер: " + report.getFileSize() + " байт");
            
            // Сохраняем в базу данных
            ReportHistory savedReport = reportHistoryRepository.save(report);
            System.out.println("ReportHistoryService: Отчет сохранен в БД с ID: " + savedReport.getId());
            
            // Также сохраняем в память для быстрого доступа
            List<ReportHistory> history = reportHistory.computeIfAbsent(reportType, k -> new ArrayList<>());
            history.add(0, savedReport);
            
            // Ограничиваем размер истории в памяти
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(0, MAX_HISTORY_SIZE);
                reportHistory.put(reportType, history);
            }
            
            System.out.println("ReportHistoryService: Отчет успешно добавлен. Всего отчетов типа '" + reportType + "': " + history.size());
            System.out.println("ReportHistoryService: Общее количество отчетов в истории: " + getTotalReportsCount());
            System.out.println("ReportHistoryService: Текущее содержимое истории для типа '" + reportType + "': " + history.stream().map(r -> r.getReportName() + " (" + r.getFormat() + ")").collect(java.util.stream.Collectors.joining(", ")));
            
        } catch (Exception e) {
            System.err.println("ReportHistoryService: Ошибка при добавлении отчета в историю: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Получить последние отчеты для определенного типа
     */
    public List<ReportHistory> getRecentReports(String reportType) {
        // Сначала пытаемся получить из памяти
        List<ReportHistory> memoryReports = reportHistory.get(reportType);
        System.out.println("ReportHistoryService: Запрос истории для типа '" + reportType + "'. Найдено отчетов в памяти: " + (memoryReports != null ? memoryReports.size() : 0));
        
        // Если в памяти нет данных, загружаем из БД
        if (memoryReports == null || memoryReports.isEmpty()) {
            try {
                List<ReportHistory> dbReports = reportHistoryRepository.findByReportTypeOrderByGeneratedAtDesc(reportType);
                if (!dbReports.isEmpty()) {
                    // Ограничиваем количество отчетов
                    if (dbReports.size() > MAX_HISTORY_SIZE) {
                        dbReports = dbReports.subList(0, MAX_HISTORY_SIZE);
                    }
                    reportHistory.put(reportType, dbReports);
                    System.out.println("ReportHistoryService: Загружено из БД отчетов типа '" + reportType + "': " + dbReports.size());
                    return dbReports;
                }
            } catch (Exception e) {
                System.err.println("ReportHistoryService: Ошибка при загрузке отчетов из БД: " + e.getMessage());
            }
        }
        
        if (memoryReports == null) {
            System.out.println("ReportHistoryService: История для типа '" + reportType + "' не найдена, возвращаем пустой список");
            return new ArrayList<>();
        }
        
        System.out.println("ReportHistoryService: Возвращаем историю для типа '" + reportType + "': " + memoryReports.stream().map(r -> r.getReportName() + " (" + r.getFormat() + ")").collect(java.util.stream.Collectors.joining(", ")));
        return new ArrayList<>(memoryReports);
    }
    
    /**
     * Получить все отчеты из БД
     */
    public List<ReportHistory> getAllReports() {
        try {
            return reportHistoryRepository.findAllByOrderByGeneratedAtDesc();
        } catch (Exception e) {
            System.err.println("ReportHistoryService: Ошибка при загрузке всех отчетов из БД: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Получить все типы отчетов, для которых есть история
     */
    public Set<String> getReportTypes() {
        return new HashSet<>(reportHistory.keySet());
    }
    
    /**
     * Получить общее количество отчетов в истории
     */
    public int getTotalReportsCount() {
        return reportHistory.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Очистить историю для определенного типа отчета
     */
    public void clearHistory(String reportType) {
        reportHistory.remove(reportType);
    }
    
    /**
     * Очистить всю историю
     */
    public void clearAllHistory() {
        reportHistory.clear();
    }
    
    /**
     * Получить отчет по ID (для скачивания)
     */
    public ReportHistory getReportById(String reportType, Long reportId) {
        List<ReportHistory> history = reportHistory.get(reportType);
        if (history == null) {
            return null;
        }
        
        return history.stream()
                .filter(report -> Objects.equals(report.getId(), reportId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Инициализация тестовыми данными
     */
    public void initializeTestData() {
        System.out.println("ReportHistoryService: Начинаем инициализацию тестовых данных");
        
        // Тестовые данные для оборудования
        addReportToHistory(new ReportHistory(
            "equipment", "Отчет по работе оборудования", "PDF", "За месяц",
            "equipment_report_2024_01.pdf", 245760L, "Система"
        ));
        addReportToHistory(new ReportHistory(
            "equipment", "Отчет по работе оборудования", "EXCEL", "За неделю",
            "equipment_report_2024_01_week.xlsx", 156800L, "Система"
        ));
        addReportToHistory(new ReportHistory(
            "equipment", "Отчет по работе оборудования", "CSV", "За день",
            "equipment_report_2024_01_15.csv", 45600L, "Система"
        ));
        
        // Тестовые данные для сварщиков
        addReportToHistory(new ReportHistory(
            "welders", "Отчет по работе сварщиков", "PDF", "За месяц",
            "welders_report_2024_01.pdf", 198400L, "Система"
        ));
        addReportToHistory(new ReportHistory(
            "welders", "Отчет по работе сварщиков", "EXCEL", "За неделю",
            "welders_report_2024_01_week.xlsx", 128000L, "Система"
        ));
        
        // Тестовые данные для материалов
        addReportToHistory(new ReportHistory(
            "materials", "Отчет по расходу материалов", "PDF", "За месяц",
            "materials_report_2024_01.pdf", 187200L, "Система"
        ));
        addReportToHistory(new ReportHistory(
            "materials", "Отчет по расходу материалов", "EXCEL", "За неделю",
            "materials_report_2024_01_week.xlsx", 112000L, "Система"
        ));
        
        // Тестовые данные для сварочных швов
        addReportToHistory(new ReportHistory(
            "welds", "Отчет по сварочным швам", "PDF", "За месяц",
            "welds_report_2024_01.pdf", 320000L, "Система"
        ));
        
        // Тестовые данные для ошибок
        addReportToHistory(new ReportHistory(
            "errors", "Отчет по ошибкам оборудования", "PDF", "За месяц",
            "errors_report_2024_01.pdf", 156800L, "Система"
        ));
        
        // Тестовые данные для нарушений
        addReportToHistory(new ReportHistory(
            "violations", "Отчет по нарушениям", "PDF", "За месяц",
            "violations_report_2024_01.pdf", 134400L, "Система"
        ));
        
        // Тестовые данные для заданий
        addReportToHistory(new ReportHistory(
            "tasks", "Отчет по сварочным заданиям", "PDF", "За месяц",
            "tasks_report_2024_01.pdf", 198400L, "Система"
        ));
        
        System.out.println("ReportHistoryService: Инициализация тестовых данных завершена. Общее количество отчетов: " + getTotalReportsCount());
        System.out.println("ReportHistoryService: Доступные типы отчетов: " + getReportTypes());
    }

    /**
     * Очистить все отчеты из истории
     */
    public void clearAllReports() {
        try {
            // Очищаем кэш в памяти
            reportHistory.clear();
            
            // Очищаем базу данных
            reportHistoryRepository.deleteAll();
            
            System.out.println("ReportHistoryService: All reports cleared from history and database");
        } catch (Exception e) {
            System.err.println("ReportHistoryService: Error clearing reports: " + e.getMessage());
            throw new RuntimeException("Failed to clear reports", e);
        }
    }
}
