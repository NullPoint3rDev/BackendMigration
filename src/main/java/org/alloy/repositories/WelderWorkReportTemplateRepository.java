package org.alloy.repositories;

import org.alloy.models.entities.WelderWorkReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WelderWorkReportTemplateRepository extends JpaRepository<WelderWorkReportTemplate, Long> {
    List<WelderWorkReportTemplate> findByIsActiveTrue();
    List<WelderWorkReportTemplate> findByCreatedByAndIsActiveTrue(Integer createdBy);
}


