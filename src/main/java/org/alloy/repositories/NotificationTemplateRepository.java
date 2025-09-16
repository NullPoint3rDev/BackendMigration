package org.alloy.repositories;

import org.alloy.models.entities.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * Найти все активные шаблоны уведомлений
     */
    List<NotificationTemplate> findByIsActiveTrueOrderByCreatedAtDesc();

    /**
     * Найти шаблоны уведомлений по типу триггера
     */
    List<NotificationTemplate> findByTriggerTypeAndIsActiveTrueOrderByCreatedAtDesc(String triggerType);

    /**
     * Найти шаблоны уведомлений по создателю
     */
    List<NotificationTemplate> findByCreatedByOrderByCreatedAtDesc(Integer createdBy);

    /**
     * Найти шаблоны уведомлений по типу уведомления
     */
    List<NotificationTemplate> findByTypeAndIsActiveTrueOrderByCreatedAtDesc(String type);

    /**
     * Найти шаблоны уведомлений по оборудованию
     */
    List<NotificationTemplate> findByEquipmentIdAndIsActiveTrueOrderByCreatedAtDesc(String equipmentId);

    /**
     * Найти все шаблоны уведомлений, отсортированные по дате создания
     */
    @Query("SELECT nt FROM NotificationTemplate nt ORDER BY nt.createdAt DESC")
    List<NotificationTemplate> findAllByOrderByCreatedAtDesc();
}
