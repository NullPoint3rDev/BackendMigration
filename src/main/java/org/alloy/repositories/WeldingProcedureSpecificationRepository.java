package org.alloy.repositories;

import org.alloy.models.entities.WeldingProcedureSpecification;
import org.alloy.models.GeneralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeldingProcedureSpecificationRepository extends JpaRepository<WeldingProcedureSpecification, Integer> {
    
    List<WeldingProcedureSpecification> findByStatus(GeneralStatus status);
    
    List<WeldingProcedureSpecification> findByWeldingMethod(WeldingProcedureSpecification.WeldingMethod weldingMethod);
    
    List<WeldingProcedureSpecification> findByMaterialTypeContainingIgnoreCase(String materialType);
    
    List<WeldingProcedureSpecification> findByGostStandardContainingIgnoreCase(String gostStandard);
    
    @Query("SELECT wps FROM WeldingProcedureSpecification wps WHERE wps.name LIKE %:searchTerm% OR wps.description LIKE %:searchTerm%")
    List<WeldingProcedureSpecification> findBySearchTerm(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT wps FROM WeldingProcedureSpecification wps WHERE wps.currentMin <= :current AND wps.currentMax >= :current")
    List<WeldingProcedureSpecification> findByCurrentRange(@Param("current") Integer current);
    
    @Query("SELECT wps FROM WeldingProcedureSpecification wps WHERE wps.voltageMin <= :voltage AND wps.voltageMax >= :voltage")
    List<WeldingProcedureSpecification> findByVoltageRange(@Param("voltage") Integer voltage);
    
    boolean existsByName(String name);
}
