package org.alloy.controllers;

import org.alloy.models.ReportHistory;
import org.alloy.services.ReportHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для работы с историей отчетов
 */
@RestController
@RequestMapping("/reports/history")
@CrossOrigin(origins = "*")
public class ReportHistoryController {
    
    @Autowired
    private ReportHistoryService reportHistoryService;
    
    /**
     * Получить последние отчеты для определенного типа
     */
    @GetMapping("/{reportType}")
    public ResponseEntity<List<ReportHistory>> getRecentReports(@PathVariable String reportType) {
        System.out.println("ReportHistoryController: Запрос истории для типа '" + reportType + "'");
        List<ReportHistory> reports = reportHistoryService.getRecentReports(reportType);
        System.out.println("ReportHistoryController: Возвращаем " + reports.size() + " отчетов");
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Получить все типы отчетов, для которых есть история
     */
    @GetMapping("/types")
    public ResponseEntity<java.util.Set<String>> getReportTypes() {
        java.util.Set<String> types = reportHistoryService.getReportTypes();
        return ResponseEntity.ok(types);
    }
    
    /**
     * Получить общее количество отчетов в истории
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getTotalReportsCount() {
        int count = reportHistoryService.getTotalReportsCount();
        return ResponseEntity.ok(count);
    }
    
    /**
     * Очистить историю для определенного типа отчета
     */
    @DeleteMapping("/{reportType}")
    public ResponseEntity<Void> clearHistory(@PathVariable String reportType) {
        reportHistoryService.clearHistory(reportType);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Очистить всю историю
     */
    @DeleteMapping("/all")
    public ResponseEntity<Void> clearAllHistory() {
        reportHistoryService.clearAllHistory();
        return ResponseEntity.ok().build();
    }
    
    /**
     * Инициализировать тестовые данные
     */
    @PostMapping("/init-test-data")
    public ResponseEntity<Void> initializeTestData() {
        System.out.println("ReportHistoryController: Инициализация тестовых данных");
        reportHistoryService.initializeTestData();
        System.out.println("ReportHistoryController: Тестовые данные инициализированы");
        return ResponseEntity.ok().build();
    }
}
