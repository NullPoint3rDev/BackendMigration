package org.alloy.services;

import org.alloy.models.ReportHistory;
import org.alloy.repositories.ReportHistoryRepository;
import org.alloy.models.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        // Проверяем, что reportType не null
        if (reportType == null || reportType.trim().isEmpty()) {
            System.out.println("ReportHistoryService: reportType is null or empty, returning empty list");
            return new ArrayList<>();
        }

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
            List<ReportHistory> reports = reportHistoryRepository.findAllByOrderByGeneratedAtDesc();
            System.out.println("ReportHistoryService: Загружено из БД отчетов: " + reports.size());

            // Для автоматически сгенерированных отчетов добавляем тестовые данные
            for (ReportHistory report : reports) {
                if (report.getIsAutoGenerated() != null && report.getIsAutoGenerated()) {
                    report.setReportData(generateTestDataForReport(report));
                }
            }

            return reports;
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

    /**
     * Генерирует тестовые данные для автоматически сгенерированного отчета
     */
    private Object generateTestDataForReport(ReportHistory report) {
        String reportType = report.getReportType();
        if (reportType == null) {
            return null;
        }

        switch (reportType.toLowerCase()) {
            case "equipment":
                return generateWorkReportTestData();

            case "welders":
                return generateWelderReportTestData();

            case "materials":
            case "wire-consumption":
                return generateWireConsumptionTestData();

            default:
                return null;
        }
    }

    /**
     * Генерирует тестовые данные для отчета по работе оборудования
     */
    private List<WorkReportDTO> generateWorkReportTestData() {
        List<WorkReportDTO> data = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.now();

        for (int i = 0; i < 8; i++) {
            WorkReportDTO item = new WorkReportDTO();
            item.setWeldingMachineId(1 + i % 3);
            item.setWeldingMachineName("Аппарат " + (1 + i % 3));
            item.setWeldingMachineSerialNumber("SN" + (1000 + i));
            item.setWelderId(1 + i % 2);
            item.setWelderName("Сварщик " + (1 + i % 2));
            item.setStartTime(baseDate.minusHours(i * 2));
            item.setEndTime(baseDate.minusHours(i * 2).plusMinutes(45 + i * 5));
            item.setWeldingTime(BigDecimal.valueOf(45 + i * 5));
            item.setCurrent(BigDecimal.valueOf(180 + i * 8));
            item.setVoltage(BigDecimal.valueOf(22 + i * 0.4));
            item.setWeldingMode("Ручной");
            item.setWeldingType("MIG/MAG");
            item.setWireConsumption(BigDecimal.valueOf(3.2 + i * 0.4));
            item.setWireFeedRate(BigDecimal.valueOf(5.1 + i * 0.2));
            item.setOrganizationUnitName("Цех " + (1 + i % 2));
            item.setNotes("Сессия " + (i + 1));
            data.add(item);
        }

        return data;
    }

    /**
     * Генерирует тестовые данные для отчета по сварщикам
     */
    private List<WelderReportDTO> generateWelderReportTestData() {
        List<WelderReportDTO> data = new ArrayList<>();
        LocalDate baseDate = LocalDate.now();

        for (int i = 0; i < 5; i++) {
            WelderReportDTO item = new WelderReportDTO();
            item.setWelderId(1 + i);
            item.setWelderName("Сварщик " + (1 + i));
            item.setWelderEmail("welder" + (1 + i) + "@company.com");
            item.setDate(baseDate.minusDays(i * 7));
            item.setTotalWireConsumption(BigDecimal.valueOf(15.5 + i * 2.3));
            item.setTotalWeldingTime(BigDecimal.valueOf(180 + i * 30));
            item.setTotalWeldingSessions(8 + i * 2);
            item.setAverageCurrent(BigDecimal.valueOf(185 + i * 5));
            item.setAverageVoltage(BigDecimal.valueOf(23.5 + i * 0.3));
            item.setAverageWireFeedRate(BigDecimal.valueOf(5.2 + i * 0.1));
            item.setOrganizationUnitName("Цех " + (1 + i % 2));
            item.setWeldingMachineName("Аппарат " + (1 + i % 3));
            data.add(item);
        }

        return data;
    }

    /**
     * Генерирует тестовые данные для отчета по расходу материалов
     */
    private List<WireConsumptionReportDTO> generateWireConsumptionTestData() {
        List<WireConsumptionReportDTO> data = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.now();

        for (int i = 0; i < 10; i++) {
            WireConsumptionReportDTO item = new WireConsumptionReportDTO();
            item.setWeldingMachineId(1 + i % 3);
            item.setWeldingMachineName("Аппарат " + (1 + i % 3));
            // item.setWeldingMachineSerialNumber("SN" + (1000 + i));
            item.setWelderId(1 + i % 2);
            item.setWelderName("Сварщик " + (1 + i % 2));
            //  item.setDate(baseDate.minusDays(i));
            item.setWireConsumption(BigDecimal.valueOf(2.5 + i * 0.3));
            // item.setWireFeedRate(BigDecimal.valueOf(5.0 + i * 0.2));
            //  item.setWeldingTime(BigDecimal.valueOf(30 + i * 5));
            //  item.setCurrent(BigDecimal.valueOf(180 + i * 10));
            //  item.setVoltage(BigDecimal.valueOf(22 + i * 0.5));
            //  item.setWeldingMode("Ручной");
            //   item.setWeldingType("MIG/MAG");
            item.setOrganizationUnitName("Цех " + (1 + i % 2));
            data.add(item);
        }

        return data;
    }
}
