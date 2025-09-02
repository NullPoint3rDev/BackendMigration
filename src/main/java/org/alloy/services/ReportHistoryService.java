package org.alloy.services;

import org.alloy.models.ReportHistory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис для управления историей сгенерированных отчетов
 */
@Service
public class ReportHistoryService {
    
    // Временное хранилище в памяти (в реальном проекте это должна быть база данных)
    private final Map<String, List<ReportHistory>> reportHistory = new ConcurrentHashMap<>();
    
    // Максимальное количество отчетов для каждого типа
    private static final int MAX_HISTORY_SIZE = 5;
    
    /**
     * Добавить отчет в историю
     */
    public void addReportToHistory(ReportHistory report) {
        try {
            String reportType = report.getReportType();
            
            System.out.println("ReportHistoryService: Добавляем отчет типа '" + reportType + "' в историю");
            
            // Получаем текущую историю для данного типа отчета
            List<ReportHistory> history = reportHistory.computeIfAbsent(reportType, k -> new ArrayList<>());
            
            // Добавляем новый отчет в начало списка
            history.add(0, report);
            
            // Ограничиваем размер истории
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(0, MAX_HISTORY_SIZE);
                reportHistory.put(reportType, history);
            }
            
            System.out.println("ReportHistoryService: Отчет успешно добавлен. Всего отчетов типа '" + reportType + "': " + history.size());
            System.out.println("ReportHistoryService: Общее количество отчетов в истории: " + getTotalReportsCount());
            
        } catch (Exception e) {
            System.err.println("ReportHistoryService: Ошибка при добавлении отчета в историю: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Получить последние отчеты для определенного типа
     */
    public List<ReportHistory> getRecentReports(String reportType) {
        List<ReportHistory> history = reportHistory.get(reportType);
        System.out.println("ReportHistoryService: Запрос истории для типа '" + reportType + "'. Найдено отчетов: " + (history != null ? history.size() : 0));
        if (history == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history);
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
    }
}
