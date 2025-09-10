package org.alloy.services;

import org.alloy.models.entities.AutomatedReport;
import org.alloy.models.dto.TriggerDTO;
import org.alloy.repositories.AutomatedReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class AutomatedReportService {

    private final AutomatedReportRepository automatedReportRepository;

    @Autowired
    public AutomatedReportService(AutomatedReportRepository automatedReportRepository) {
        this.automatedReportRepository = automatedReportRepository;
    }

    public List<AutomatedReport> getAllAutomatedReports() {
        return automatedReportRepository.findAll();
    }

    public Optional<AutomatedReport> getAutomatedReportById(Integer id) {
        return automatedReportRepository.findById(id);
    }

    public List<AutomatedReport> getUserAutomatedReports(Integer userAccountId) {
        return automatedReportRepository.findByCreatedBy(userAccountId);
    }

    public List<AutomatedReport> getActiveAutomatedReports() {
        return automatedReportRepository.findByIsActiveTrue();
    }

    public AutomatedReport createAutomatedReport(AutomatedReport automatedReport) {
        automatedReport.setCreatedAt(LocalDateTime.now());
        automatedReport.setUpdatedAt(LocalDateTime.now());
        automatedReport.setRunCount(0);
        automatedReport.setSuccessCount(0);
        automatedReport.setErrorCount(0);
        automatedReport.setRetryCount(0);
        
        // Вычисляем следующее время выполнения
        calculateNextRunTime(automatedReport);
        
        return automatedReportRepository.save(automatedReport);
    }

    public AutomatedReport updateAutomatedReport(AutomatedReport automatedReport) {
        automatedReport.setUpdatedAt(LocalDateTime.now());
        
        // Пересчитываем следующее время выполнения если изменились триггеры
        calculateNextRunTime(automatedReport);
        
        return automatedReportRepository.save(automatedReport);
    }

    public void deleteAutomatedReport(Integer id) {
        if (!automatedReportRepository.existsById(id)) {
            throw new IllegalArgumentException("Automated report with id " + id + " not found");
        }
        automatedReportRepository.deleteById(id);
    }

    public AutomatedReport toggleAutomatedReportStatus(Integer id) {
        AutomatedReport report = automatedReportRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Automated report with id " + id + " not found"));
        
        report.setIsActive(!report.getIsActive());
        report.setUpdatedAt(LocalDateTime.now());
        
        if (report.getIsActive()) {
            calculateNextRunTime(report);
        } else {
            report.setNextRun(null);
        }
        
        return automatedReportRepository.save(report);
    }

    public AutomatedReport runAutomatedReport(Integer id) {
        AutomatedReport report = automatedReportRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Automated report with id " + id + " not found"));
        
        try {
            // Здесь должна быть логика выполнения отчета
            // Пока просто обновляем статистику
            report.incrementRunCount();
            report.incrementSuccessCount();
            report.updateLastRun();
            report.clearError();
            
            // Вычисляем следующее время выполнения
            calculateNextRunTime(report);
            
            return automatedReportRepository.save(report);
        } catch (Exception e) {
            report.incrementRunCount();
            report.incrementErrorCount();
            report.setError(e.getMessage());
            report.incrementRetryCount();
            
            return automatedReportRepository.save(report);
        }
    }

    public List<Object> getAutomatedReportHistory(Integer id) {
        // Здесь должна быть логика получения истории выполнения
        // Пока возвращаем пустой список
        return new ArrayList<>();
    }

    public Object getAutomatedReportsStats() {
        List<AutomatedReport> allReports = automatedReportRepository.findAll();
        List<AutomatedReport> activeReports = automatedReportRepository.findByIsActiveTrue();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReports", allReports.size());
        stats.put("activeReports", activeReports.size());
        stats.put("inactiveReports", allReports.size() - activeReports.size());
        
        int totalRuns = allReports.stream().mapToInt(r -> r.getRunCount() != null ? r.getRunCount() : 0).sum();
        int totalSuccesses = allReports.stream().mapToInt(r -> r.getSuccessCount() != null ? r.getSuccessCount() : 0).sum();
        int totalErrors = allReports.stream().mapToInt(r -> r.getErrorCount() != null ? r.getErrorCount() : 0).sum();
        
        stats.put("totalRuns", totalRuns);
        stats.put("totalSuccesses", totalSuccesses);
        stats.put("totalErrors", totalErrors);
        stats.put("successRate", totalRuns > 0 ? (double) totalSuccesses / totalRuns * 100 : 0.0);
        
        return stats;
    }

    public List<AutomatedReport> searchAutomatedReports(String searchTerm) {
        return automatedReportRepository.findByNameContainingIgnoreCaseOrTemplateNameContainingIgnoreCase(
            searchTerm, searchTerm);
    }

    public List<Object> getTriggerTypes() {
        List<Object> triggerTypes = new ArrayList<>();
        
        Map<String, Object> timeTrigger = new HashMap<>();
        timeTrigger.put("type", "TIME");
        timeTrigger.put("name", "По времени");
        timeTrigger.put("description", "Запуск отчета по расписанию");
        triggerTypes.add(timeTrigger);
        
        Map<String, Object> errorTrigger = new HashMap<>();
        errorTrigger.put("type", "EQUIPMENT_ERROR");
        errorTrigger.put("name", "По ошибкам оборудования");
        errorTrigger.put("description", "Запуск при превышении количества ошибок");
        triggerTypes.add(errorTrigger);
        
        Map<String, Object> valueTrigger = new HashMap<>();
        valueTrigger.put("type", "VALUE_THRESHOLD");
        valueTrigger.put("name", "По значениям параметров");
        valueTrigger.put("description", "Запуск при достижении определенных значений");
        triggerTypes.add(valueTrigger);
        
        return triggerTypes;
    }

    public List<Object> getAvailableTemplates() {
        List<Object> templates = new ArrayList<>();
        
        Map<String, Object> equipmentTemplate = new HashMap<>();
        equipmentTemplate.put("id", 1);
        equipmentTemplate.put("name", "Отчет по работе оборудования");
        equipmentTemplate.put("type", "equipment");
        templates.add(equipmentTemplate);
        
        Map<String, Object> weldersTemplate = new HashMap<>();
        weldersTemplate.put("id", 2);
        weldersTemplate.put("name", "Отчет по работе сварщиков");
        weldersTemplate.put("type", "welders");
        templates.add(weldersTemplate);
        
        Map<String, Object> materialsTemplate = new HashMap<>();
        materialsTemplate.put("id", 3);
        materialsTemplate.put("name", "Отчет по расходу материалов");
        materialsTemplate.put("type", "materials");
        templates.add(materialsTemplate);
        
        Map<String, Object> weldsTemplate = new HashMap<>();
        weldsTemplate.put("id", 4);
        weldsTemplate.put("name", "Отчет по сварочным швам");
        weldsTemplate.put("type", "welds");
        templates.add(weldsTemplate);
        
        Map<String, Object> errorsTemplate = new HashMap<>();
        errorsTemplate.put("id", 5);
        errorsTemplate.put("name", "Отчет по ошибкам оборудования");
        errorsTemplate.put("type", "errors");
        templates.add(errorsTemplate);
        
        Map<String, Object> violationsTemplate = new HashMap<>();
        violationsTemplate.put("id", 6);
        violationsTemplate.put("name", "Отчет по нарушениям");
        violationsTemplate.put("type", "violations");
        templates.add(violationsTemplate);
        
        Map<String, Object> tasksTemplate = new HashMap<>();
        tasksTemplate.put("id", 7);
        tasksTemplate.put("name", "Отчет по заданиям");
        tasksTemplate.put("type", "tasks");
        templates.add(tasksTemplate);
        
        return templates;
    }

    public Object validateTriggerConfig(Object triggerConfig) {
        // Здесь должна быть логика валидации конфигурации триггера
        Map<String, Object> result = new HashMap<>();
        result.put("isValid", true);
        result.put("errors", new ArrayList<>());
        return result;
    }

    public Object getNextRunTime(Object triggerConfig) {
        // Здесь должна быть логика вычисления следующего времени выполнения
        Map<String, Object> result = new HashMap<>();
        result.put("nextRunTime", LocalDateTime.now().plusHours(1));
        return result;
    }

    private void calculateNextRunTime(AutomatedReport report) {
        if (report.getTriggersConfig() == null || report.getTriggersConfig().trim().isEmpty()) {
            report.setNextRun(null);
            return;
        }

        try {
            // Парсим триггеры из JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<TriggerDTO> triggers = mapper.readValue(
                report.getTriggersConfig(), 
                new com.fasterxml.jackson.core.type.TypeReference<List<TriggerDTO>>() {}
            );

            if (triggers.isEmpty()) {
                report.setNextRun(null);
                return;
            }

            // Берем первый активный триггер
            TriggerDTO activeTrigger = triggers.stream()
                .filter(TriggerDTO::getIsActive)
                .findFirst()
                .orElse(null);

            if (activeTrigger == null) {
                report.setNextRun(null);
                return;
            }

            // Вычисляем следующее время выполнения
            LocalDateTime nextRun = calculateNextRunForTrigger(activeTrigger);
            report.setNextRun(nextRun);

        } catch (Exception e) {
            report.setNextRun(null);
        }
    }

    private LocalDateTime calculateNextRunForTrigger(TriggerDTO trigger) {
        LocalDateTime now = LocalDateTime.now();

        switch (trigger.getType()) {
            case "TIME":
                return calculateTimeTriggerNextRun(trigger, now);
            case "EQUIPMENT_ERROR":
            case "VALUE_THRESHOLD":
                // Для этих типов триггеров время выполнения не планируется заранее
                return null;
            default:
                return null;
        }
    }

    private LocalDateTime calculateTimeTriggerNextRun(TriggerDTO trigger, LocalDateTime now) {
        String frequency = trigger.getValue();
        String timeStr = trigger.getTime();
        
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return now.plusHours(1);
        }
        
        // Парсим время (формат HH:mm)
        String[] timeParts = timeStr.split(":");
        if (timeParts.length != 2) {
            return now.plusHours(1);
        }
        
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        // Создаем время выполнения на сегодня
        LocalDateTime nextRun = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        
        switch (frequency) {
            case "daily":
                // Если время уже прошло сегодня, планируем на завтра
                if (nextRun.isBefore(now)) {
                    nextRun = nextRun.plusDays(1);
                }
                return nextRun;
                
            case "weekly":
                // Для еженедельного выполнения нужно учесть дни недели
                String daysOfWeek = trigger.getDaysOfWeek();
                if (daysOfWeek != null && !daysOfWeek.trim().isEmpty()) {
                    String[] days = daysOfWeek.split(",");
                    // Находим ближайший день недели
                    for (String day : days) {
                        LocalDateTime dayRun = getNextWeekday(nextRun, day.trim());
                        if (dayRun.isAfter(now)) {
                            return dayRun;
                        }
                    }
                    // Если все дни прошли, берем первый день следующей недели
                    return getNextWeekday(nextRun.plusWeeks(1), days[0].trim());
                } else {
                    // Если дни не указаны, просто добавляем неделю
                    if (nextRun.isBefore(now)) {
                        nextRun = nextRun.plusWeeks(1);
                    }
                    return nextRun;
                }
                
            case "monthly":
                // Для месячного выполнения
                Integer dayOfMonth = trigger.getDayOfMonth();
                if (dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31) {
                    nextRun = nextRun.withDayOfMonth(dayOfMonth);
                    // Если день уже прошел в этом месяце, планируем на следующий месяц
                    if (nextRun.isBefore(now)) {
                        nextRun = nextRun.plusMonths(1);
                        // Проверяем, что день существует в следующем месяце
                        try {
                            nextRun = nextRun.withDayOfMonth(dayOfMonth);
                        } catch (Exception e) {
                            // Если дня нет в месяце (например, 31 февраля), берем последний день месяца
                            nextRun = nextRun.withDayOfMonth(nextRun.toLocalDate().lengthOfMonth());
                        }
                    }
                    return nextRun;
                } else {
                    // Если день месяца не указан, используем 1 число
                    nextRun = nextRun.withDayOfMonth(1);
                    if (nextRun.isBefore(now)) {
                        nextRun = nextRun.plusMonths(1);
                    }
                    return nextRun;
                }
                
            default:
                return now.plusHours(1);
        }
    }
    
    private LocalDateTime getNextWeekday(LocalDateTime base, String dayName) {
        java.time.DayOfWeek targetDay = parseDayOfWeek(dayName);
        LocalDateTime result = base;
        
        // Находим следующий день недели
        while (result.getDayOfWeek() != targetDay) {
            result = result.plusDays(1);
        }
        
        return result;
    }
    
    private java.time.DayOfWeek parseDayOfWeek(String dayName) {
        switch (dayName.toUpperCase()) {
            case "MONDAY": return java.time.DayOfWeek.MONDAY;
            case "TUESDAY": return java.time.DayOfWeek.TUESDAY;
            case "WEDNESDAY": return java.time.DayOfWeek.WEDNESDAY;
            case "THURSDAY": return java.time.DayOfWeek.THURSDAY;
            case "FRIDAY": return java.time.DayOfWeek.FRIDAY;
            case "SATURDAY": return java.time.DayOfWeek.SATURDAY;
            case "SUNDAY": return java.time.DayOfWeek.SUNDAY;
            default: return java.time.DayOfWeek.MONDAY;
        }
    }

    // Методы для работы с отчетами, которые нужно выполнить
    public List<AutomatedReport> getReportsToExecute() {
        LocalDateTime now = LocalDateTime.now();
        return automatedReportRepository.findByIsActiveTrueAndNextRunLessThanEqual(now);
    }

    public void markReportAsExecuted(AutomatedReport report) {
        report.updateLastRun();
        calculateNextRunTime(report);
        automatedReportRepository.save(report);
    }

    public void markReportAsFailed(AutomatedReport report, String errorMessage) {
        report.setError(errorMessage);
        report.incrementErrorCount();
        report.incrementRetryCount();
        automatedReportRepository.save(report);
    }
}
