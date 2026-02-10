package org.alloy.repositories;

import org.alloy.models.entities.EquipmentWorkReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentWorkReportTemplateRepository extends JpaRepository<EquipmentWorkReportTemplate, Long> {

    List<EquipmentWorkReportTemplate> findByIsActiveTrue();
    List<EquipmentWorkReportTemplate> findByCreatedByAndIsActiveTrue(Integer createdBy);
}
