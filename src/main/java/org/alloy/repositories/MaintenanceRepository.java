package org.alloy.repositories;

import org.alloy.models.entities.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Integer> {
    List<Maintenance> findByWeldingMachineId(Integer weldingMachineId);
    Optional<Maintenance> findTopByWeldingMachineIdOrderByDateCreatedDesc(Integer weldingMachineId);
    List<Maintenance> findByWeldingMachineIdAndStatus(Integer weldingMachineId, String status);
} 