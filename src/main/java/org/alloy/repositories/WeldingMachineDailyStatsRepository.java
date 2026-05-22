package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeldingMachineDailyStatsRepository extends JpaRepository<WeldingMachineDailyStats, Long> {

    Optional<WeldingMachineDailyStats> findByWeldingMachineIdAndStatDate(Integer weldingMachineId, LocalDate statDate);

    @Query("SELECT DISTINCT wms.weldingMachineId FROM WeldingMachineState wms WHERE wms.dateCreated >= :since")
    List<Integer> findMachineIdsWithStatesSince(@Param("since") LocalDateTime since);
}
