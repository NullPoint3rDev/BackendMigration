package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.GeneralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeldingMachineTypeRepository extends JpaRepository<WeldingMachineType, Integer> {
    
    Optional<WeldingMachineType> findByName(String name);
    
    List<WeldingMachineType> findByStatus(GeneralStatus status);
    
    @Query("SELECT w FROM WeldingMachineType w WHERE " +
           "LOWER(w.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(w.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<WeldingMachineType> searchWeldingMachineTypes(@Param("searchTerm") String searchTerm);
} 