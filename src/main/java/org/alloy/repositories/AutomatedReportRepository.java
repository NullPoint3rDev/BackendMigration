package org.alloy.repositories;

import org.alloy.models.entities.AutomatedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AutomatedReportRepository extends JpaRepository<AutomatedReport, Long> {

    /**
     * Найти все автоматизированные отчеты по пользователю
     */
    List<AutomatedReport> findByCreatedBy(Integer createdBy);

    /**
     * Найти все активные автоматизированные отчеты
     */
    List<AutomatedReport> findByIsActiveTrue();

    /**
     * Найти все неактивные автоматизированные отчеты
     */
    List<AutomatedReport> findByIsActiveFalse();

    /**
     * Найти автоматизированные отчеты по шаблону
     */
    List<AutomatedReport> findByTemplateId(Long templateId);

    /**
     * Найти автоматизированные отчеты по типу шаблона
     */
    List<AutomatedReport> findByTemplateType(String templateType);

    /**
     * Найти автоматизированные отчеты, которые нужно выполнить
     */
    List<AutomatedReport> findByIsActiveTrueAndNextRunLessThanEqual(LocalDateTime now);

    /**
     * Найти автоматизированные отчеты, которые нужно выполнить в определенном диапазоне времени
     */
    List<AutomatedReport> findByIsActiveTrueAndNextRunBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Найти автоматизированные отчеты по названию (поиск без учета регистра)
     */
    List<AutomatedReport> findByNameContainingIgnoreCase(String name);

    /**
     * Найти автоматизированные отчеты по названию шаблона (поиск без учета регистра)
     */
    List<AutomatedReport> findByTemplateNameContainingIgnoreCase(String templateName);

    /**
     * Найти автоматизированные отчеты по названию или названию шаблона (поиск без учета регистра)
     */
    List<AutomatedReport> findByNameContainingIgnoreCaseOrTemplateNameContainingIgnoreCase(String name, String templateName);

    /**
     * Найти автоматизированные отчеты по тегам
     */
    @Query("SELECT ar FROM AutomatedReport ar WHERE ar.tags LIKE %:tag%")
    List<AutomatedReport> findByTag(@Param("tag") String tag);

    /**
     * Найти автоматизированные отчеты с ошибками
     */
    List<AutomatedReport> findByErrorCountGreaterThan(Integer errorCount);

    /**
     * Найти автоматизированные отчеты без ошибок
     */
    List<AutomatedReport> findByErrorCount(Integer errorCount);

    /**
     * Найти автоматизированные отчеты с высокой частотой ошибок
     */
    @Query("SELECT ar FROM AutomatedReport ar WHERE ar.errorCount > 0 AND ar.runCount > 0 AND (ar.errorCount * 100.0 / ar.runCount) > :errorRate")
    List<AutomatedReport> findByHighErrorRate(@Param("errorRate") Double errorRate);

    /**
     * Найти автоматизированные отчеты, которые не выполнялись долгое время
     */
    @Query("SELECT ar FROM AutomatedReport ar WHERE ar.isActive = true AND (ar.lastRun IS NULL OR ar.lastRun < :threshold)")
    List<AutomatedReport> findStaleReports(@Param("threshold") LocalDateTime threshold);

    /**
     * Найти автоматизированные отчеты по приоритету
     */
    List<AutomatedReport> findByPriority(Integer priority);

    /**
     * Найти автоматизированные отчеты с приоритетом выше указанного
     */
    List<AutomatedReport> findByPriorityLessThanEqual(Integer priority);

    /**
     * Найти автоматизированные отчеты с email уведомлениями
     */
    List<AutomatedReport> findByEmailNotificationsTrue();

    /**
     * Найти автоматизированные отчеты по часовому поясу
     */
    List<AutomatedReport> findByTimezone(String timezone);

    /**
     * Подсчитать количество автоматизированных отчетов по пользователю
     */
    long countByCreatedBy(Integer createdBy);

    /**
     * Подсчитать количество активных автоматизированных отчетов
     */
    long countByIsActiveTrue();

    /**
     * Подсчитать количество автоматизированных отчетов по шаблону
     */
    long countByTemplateId(Long templateId);

    /**
     * Получить статистику по автоматизированным отчетам
     */
    @Query("SELECT " +
           "COUNT(ar) as totalReports, " +
           "SUM(CASE WHEN ar.isActive = true THEN 1 ELSE 0 END) as activeReports, " +
           "SUM(ar.runCount) as totalRuns, " +
           "SUM(ar.successCount) as totalSuccesses, " +
           "SUM(ar.errorCount) as totalErrors " +
           "FROM AutomatedReport ar")
    Object getStatistics();

    /**
     * Получить статистику по автоматизированным отчетам пользователя
     */
    @Query("SELECT " +
           "COUNT(ar) as totalReports, " +
           "SUM(CASE WHEN ar.isActive = true THEN 1 ELSE 0 END) as activeReports, " +
           "SUM(ar.runCount) as totalRuns, " +
           "SUM(ar.successCount) as totalSuccesses, " +
           "SUM(ar.errorCount) as totalErrors " +
           "FROM AutomatedReport ar WHERE ar.createdBy = :userId")
    Object getStatisticsByUser(@Param("userId") Integer userId);

    /**
     * Получить автоматизированные отчеты, отсортированные по приоритету и времени следующего выполнения
     */
    @Query("SELECT ar FROM AutomatedReport ar WHERE ar.isActive = true ORDER BY ar.priority ASC, ar.nextRun ASC")
    List<AutomatedReport> findActiveReportsOrderedByPriorityAndNextRun();

    /**
     * Получить автоматизированные отчеты, которые нужно выполнить в ближайшее время
     */
    @Query("SELECT ar FROM AutomatedReport ar WHERE ar.isActive = true AND ar.nextRun IS NOT NULL AND ar.nextRun <= :time ORDER BY ar.priority ASC, ar.nextRun ASC")
    List<AutomatedReport> findReportsToExecuteSoon(@Param("time") LocalDateTime time);

    /**
     * Найти автоматизированные отчеты с определенным типом триггера
     */
    @Query("SELECT ar FROM AutomatedReport ar WHERE ar.triggersConfig LIKE %:triggerType%")
    List<AutomatedReport> findByTriggerType(@Param("triggerType") String triggerType);

    /**
     * Получить автоматизированные отчеты, созданные в определенном диапазоне дат
     */
    List<AutomatedReport> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Получить автоматизированные отчеты, обновленные в определенном диапазоне дат
     */
    List<AutomatedReport> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Получить автоматизированные отчеты, которые выполнялись в определенном диапазоне дат
     */
    List<AutomatedReport> findByLastRunBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Удалить старые автоматизированные отчеты
     */
    void deleteByCreatedAtBefore(LocalDateTime date);

    /**
     * Найти автоматизированные отчеты с определенным количеством повторных попыток
     */
    List<AutomatedReport> findByRetryCount(Integer retryCount);

    /**
     * Найти автоматизированные отчеты, которые достигли максимального количества повторных попыток
     */
    @Query("SELECT ar FROM AutomatedReport ar WHERE ar.retryCount >= ar.maxRetries")
    List<AutomatedReport> findReportsWithMaxRetriesReached();

    /**
     * Сбросить счетчик повторных попыток для всех отчетов
     */
    @Query("UPDATE AutomatedReport ar SET ar.retryCount = 0 WHERE ar.retryCount > 0")
    void resetRetryCounts();
}
