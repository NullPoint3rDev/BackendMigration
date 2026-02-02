package org.alloy.repositories;

import org.alloy.models.entities.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    /**
     * Найти все активные шаблоны
     */
    List<ReportTemplate> findByIsActiveTrue();

    /**
     * Найти все шаблоны пользователя
     */
    List<ReportTemplate> findByCreatedBy(Integer createdBy);

    /**
     * Найти все активные шаблоны пользователя
     */
    List<ReportTemplate> findByCreatedByAndIsActiveTrue(Integer createdBy);

    /**
     * Найти шаблон по ID и создателю (для проверки прав)
     */
    Optional<ReportTemplate> findByIdAndCreatedBy(Long id, Integer createdBy);
}

