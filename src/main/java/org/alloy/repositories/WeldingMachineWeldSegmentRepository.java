package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineWeldSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface WeldingMachineWeldSegmentRepository extends JpaRepository<WeldingMachineWeldSegment, Long> {

    List<WeldingMachineWeldSegment> findByWeldingMachineIdInAndStartTimeGreaterThanEqualAndStartTimeLessThanEqualOrderByStartTimeAsc(
            Collection<Integer> weldingMachineIds,
            LocalDateTime startInclusive,
            LocalDateTime endInclusive);

    @Modifying
    @Query("DELETE FROM WeldingMachineWeldSegment s WHERE s.weldingMachineId = :machineId "
            + "AND s.startTime >= :start AND s.startTime < :end")
    int deleteByMachineAndStartTimeRange(
            @Param("machineId") Integer machineId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Modifying
    @Query("DELETE FROM WeldingMachineWeldSegment s WHERE s.weldingMachineId = :machineId AND s.statDate = :statDate")
    int deleteByMachineAndStatDate(
            @Param("machineId") Integer machineId,
            @Param("statDate") LocalDate statDate);
}
