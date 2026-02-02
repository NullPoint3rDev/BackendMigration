package org.alloy.repositories;

import org.alloy.models.entities.WireConsumptionReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WireConsumptionReportTemplateRepository extends JpaRepository<WireConsumptionReportTemplate, Long> {

    /**
     * Найти все активные шаблоны
     */
    List<WireConsumptionReportTemplate> findByIsActiveTrue();

    /**
     * Найти шаблоны по создателю
     */
    List<WireConsumptionReportTemplate> findByCreatedBy(Integer createdBy);

    /**
     * Найти активные шаблоны по создателю
     */
    List<WireConsumptionReportTemplate> findByCreatedByAndIsActiveTrue(Integer createdBy);

    /**
     * Найти шаблон по имени (для проверки уникальности)
     */
    Optional<WireConsumptionReportTemplate> findByName(String name);

    /**
     * Найти шаблон по имени и создателю
     */
    Optional<WireConsumptionReportTemplate> findByNameAndCreatedBy(String name, Integer createdBy);
}

