package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO для генерации отчета по расходу проволоки
 */
@Data
@NoArgsConstructor
public class WireConsumptionReportGenerationDTO {

    /**
     * ID шаблона отчета
     */
    private Long templateId;

    /**
     * Генерация "прямо сейчас"
     */
    private Boolean generateNow = false;

    /**
     * Период формирования отчета - дата начала
     */
    private LocalDate periodStartDate;

    /**
     * Период формирования отчета - дата окончания
     */
    private LocalDate periodEndDate;

    /**
     * Время начала периода
     */
    private LocalTime periodStartTime;

    /**
     * Время окончания периода
     */
    private LocalTime periodEndTime;

    /**
     * Включить создание отчетов по расписанию
     */
    private Boolean enableScheduledReports = false;

    /**
     * Список расписаний для автоматической генерации
     */
    private List<ReportScheduleDTO> schedules;

    /**
     * DTO для расписания отчета
     */
    @Data
    @NoArgsConstructor
    public static class ReportScheduleDTO {
        /**
         * ID расписания (если редактируется существующее)
         */
        private Long scheduleId;

        /**
         * Выбор дней для формирования отчетов
         * Может быть: дни недели (MONDAY, TUESDAY, etc.) или числа месяца (1-31)
         */
        private List<String> selectedDays; // "MONDAY", "TUESDAY" или "1", "15", "31"

        /**
         * Тип выбора дней: "WEEKDAYS" (дни недели) или "MONTH_DAYS" (числа месяца)
         */
        private String daySelectionType; // "WEEKDAYS" или "MONTH_DAYS"

        /**
         * Час формирования отчета (0-23)
         */
        private Integer hour;

        /**
         * Минуты формирования отчета (0-59)
         */
        private Integer minutes;

        /**
         * Период для отчета (отсчитывается от момента формирования)
         * Возможные значения: "DAY", "WEEK", "MONTH", "QUARTER"
         */
        private String reportPeriod; // "DAY", "WEEK", "MONTH", "QUARTER"

        /**
         * Для недели: указание конкретных дней (если reportPeriod = "WEEK")
         * Например: [1, 2, 3, 4, 5] для пн-пт
         */
        private List<Integer> weekDays; // 1-7 (понедельник-воскресенье)

        /**
         * Временные промежутки в выбранном периоде
         * Формат: "HH:mm-HH:mm" (например, "08:00-17:00")
         */
        private List<String> timeRanges; // ["08:00-17:00", "18:00-22:00"]
    }
}

