package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.WeldingMachineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeldingMachineStateRepository extends JpaRepository<WeldingMachineState, Long> {

    List<WeldingMachineState> findByWeldingMachineId(Long weldingMachineId);

    Optional<WeldingMachineState> findTopByWeldingMachineIdOrderByDateCreatedDesc(Long weldingMachineId);

    List<WeldingMachineState> findByWeldingMachineIdAndWeldingMachineStatus(Long weldingMachineId, WeldingMachineStatus status);

    @Query("SELECT wms FROM WeldingMachineState wms WHERE wms.weldingMachineId = :weldingMachineId AND wms.dateCreated BETWEEN :startDate AND :endDate ORDER BY wms.dateCreated DESC")
    List<WeldingMachineState> findByWeldingMachineIdAndDateRange(
            @Param("weldingMachineId") Long weldingMachineId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT wms FROM WeldingMachineState wms WHERE wms.weldingMachineId = :weldingMachineId AND wms.limitsExceeded = true ORDER BY wms.dateCreated DESC")
    List<WeldingMachineState> findStatesWithExceededLimits(@Param("weldingMachineId") Long weldingMachineId);
}
