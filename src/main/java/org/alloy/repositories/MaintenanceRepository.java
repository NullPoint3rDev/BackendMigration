package org.alloy.repositories;

import org.alloy.models.entities.Maintenance;
import org.alloy.models.GeneralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Integer> {
    List<Maintenance> findByWeldingMachineId(Integer weldingMachineId);
    Optional<Maintenance> findTopByWeldingMachineIdOrderByDateCreatedDesc(Integer weldingMachineId);
    // status — enum GeneralStatus в сущности; параметр должен быть того же типа, иначе derived-query
    // падает с "Parameter value [Active] did not match expected type [GeneralStatus]".
    List<Maintenance> findByWeldingMachineIdAndStatus(Integer weldingMachineId, GeneralStatus status);
} 