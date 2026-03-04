package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.WeldingMachineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeldingMachineStateRepository extends JpaRepository<WeldingMachineState, Long> {

    List<WeldingMachineState> findByWeldingMachineId(Integer weldingMachineId);

    Optional<WeldingMachineState> findTopByWeldingMachineIdOrderByDateCreatedDesc(Integer weldingMachineId);

    List<WeldingMachineState> findByWeldingMachineIdAndWeldingMachineStatus(Integer weldingMachineId, WeldingMachineStatus status);

    @Query("SELECT wms FROM WeldingMachineState wms WHERE wms.weldingMachineId = :weldingMachineId AND wms.dateCreated BETWEEN :startDate AND :endDate ORDER BY wms.dateCreated DESC")
    List<WeldingMachineState> findByWeldingMachineIdAndDateRange(
            @Param("weldingMachineId") Integer weldingMachineId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT wms FROM WeldingMachineState wms WHERE wms.weldingMachineId = :weldingMachineId AND wms.limitsExceeded = true ORDER BY wms.dateCreated DESC")
    List<WeldingMachineState> findStatesWithExceededLimits(@Param("weldingMachineId") Integer weldingMachineId);

    @Query("SELECT wms FROM WeldingMachineState wms WHERE wms.rfid = :rfid AND wms.dateCreated BETWEEN :startDate AND :endDate ORDER BY wms.dateCreated ASC")
    List<WeldingMachineState> findByRfidAndDateRange(
            @Param("rfid") String rfid,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DISTINCT wms.weldingMachineId FROM WeldingMachineState wms WHERE wms.rfid IN :rfidCodes")
    List<Integer> findDistinctWeldingMachineIdsByRfidCodes(@Param("rfidCodes") List<String> rfidCodes);

    /** Состояния с ошибками (Error) по списку аппаратов за период — для отчёта по неисправностям (JPQL) */
    @Query("SELECT wms FROM WeldingMachineState wms WHERE wms.weldingMachineId IN :machineIds AND wms.weldingMachineStatus = org.alloy.models.WeldingMachineStatus.Error AND wms.dateCreated >= :start AND wms.dateCreated <= :end ORDER BY wms.weldingMachineId, wms.dateCreated ASC")
    List<WeldingMachineState> findByWeldingMachineIdInAndStatusErrorAndDateRange(
            @Param("machineIds") List<Integer> machineIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Нативный SQL — для БД с именами в snake_case (welding_machine_state, welding_machine_status = 4).
     * Возвращает: [0]=id, [1]=welding_machineid, [2]=date_created, [3]=state_duration_ms, [4]=error_code.
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT s.id, s.welding_machineid, s.date_created, s.state_duration_ms, s.error_code FROM welding_machine_state s WHERE s.welding_machine_status = 4 AND s.welding_machineid IN (:machineIds) AND s.date_created >= :start AND s.date_created <= :end ORDER BY s.welding_machineid, s.date_created ASC", nativeQuery = true)
    List<Object[]> findErrorStatesNative(
            @Param("machineIds") List<Integer> machineIds,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);

    /**
     * Все состояния с ошибками за период (без фильтра по аппаратам).
     * Диапазон: date_created >= :start AND date_created < :end (end — начало следующего дня).
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT s.id, s.welding_machineid, s.date_created, s.state_duration_ms, s.error_code FROM welding_machine_state s WHERE s.welding_machine_status = 4 AND s.date_created >= :start AND s.date_created < :end ORDER BY s.welding_machineid, s.date_created ASC", nativeQuery = true)
    List<Object[]> findErrorStatesNativeAll(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Все состояния с ошибками без фильтра по дате (для запасного варианта — фильтр по дате в Java). */
    @org.springframework.data.jpa.repository.Query(value = "SELECT s.id, s.welding_machineid, s.date_created, s.state_duration_ms, s.error_code FROM welding_machine_state s WHERE s.welding_machine_status = 4 ORDER BY s.welding_machineid, s.date_created ASC", nativeQuery = true)
    List<Object[]> findErrorStatesNativeAllUnbounded();

    /** Последние состояния с ошибками (для диагностики: что реально лежит в error_code и state_duration_ms). */
    @org.springframework.data.jpa.repository.Query(value = "SELECT s.id, s.welding_machineid, s.date_created, s.state_duration_ms, s.error_code FROM welding_machine_state s WHERE s.welding_machine_status = 4 ORDER BY s.date_created DESC", nativeQuery = true)
    List<Object[]> findErrorStatesNativeRecent(Pageable pageable);
}
