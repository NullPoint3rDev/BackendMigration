package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineWeldSegmentDayMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeldingMachineWeldSegmentDayMarkRepository extends JpaRepository<WeldingMachineWeldSegmentDayMark, Long> {

    Optional<WeldingMachineWeldSegmentDayMark> findByWeldingMachineIdAndStatDate(
            Integer weldingMachineId, LocalDate statDate);

    @Query("SELECT m FROM WeldingMachineWeldSegmentDayMark m "
            + "WHERE m.weldingMachineId = :machineId AND m.statDate >= :from AND m.statDate <= :to")
    List<WeldingMachineWeldSegmentDayMark> findByMachineAndStatDateBetween(
            @Param("machineId") Integer machineId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT DISTINCT wms.weldingMachineId FROM WeldingMachineState wms "
            + "WHERE wms.dateCreated >= :dayStart AND wms.dateCreated < :dayEnd")
    List<Integer> findMachineIdsWithStatesOnDay(
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd);
}
