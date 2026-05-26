package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineParameterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeldingMachineParameterValueRepository extends JpaRepository<WeldingMachineParameterValue, Long> {

    List<WeldingMachineParameterValue> findByWeldingMachineStateId(Long weldingMachineStateId);

    List<WeldingMachineParameterValue> findByPropertyCode(String propertyCode);

    Optional<WeldingMachineParameterValue> findByWeldingMachineStateIdAndPropertyCode(Long stateId, String propertyCode);

    List<WeldingMachineParameterValue> findByWeldingMachineStateIdAndLimitsExceededTrue(Long stateId);

    @Query("SELECT wmpv FROM WeldingMachineParameterValue wmpv WHERE wmpv.weldingMachineStateId IN :stateIds AND wmpv.propertyCode = :propertyCode")
    List<WeldingMachineParameterValue> findByStateIdsAndPropertyCode(
            @Param("stateIds") List<Long> stateIds,
            @Param("propertyCode") String propertyCode);

    @Query("SELECT wmpv FROM WeldingMachineParameterValue wmpv WHERE wmpv.weldingMachineStateId IN :stateIds AND wmpv.propertyCode IN :propertyCodes")
    List<WeldingMachineParameterValue> findByStateIdsAndPropertyCodes(
            @Param("stateIds") List<Long> stateIds,
            @Param("propertyCodes") List<String> propertyCodes);

    /**
     * Нативный запрос для совместимости со схемой БД (snake_case: welding_machine_stateid, property_code).
     * Возвращает [state_id, value] для построения карты по stateId без зависимости от маппинга сущности.
     */
    @Query(
            value = "SELECT welding_machine_stateid, value FROM welding_machine_parameter_value " +
                    "WHERE welding_machine_stateid IN (:stateIds) AND property_code = :propertyCode",
            nativeQuery = true)
    List<Object[]> findStateIdAndValueNative(
            @Param("stateIds") List<Long> stateIds,
            @Param("propertyCode") String propertyCode);

    /**
     * Как {@link #findStateIdAndValueNative}, но для схемы со snake_case-колонкой {@code welding_machine_state_id}.
     */
    @Query(
            value = "SELECT welding_machine_state_id, value FROM welding_machine_parameter_value " +
                    "WHERE welding_machine_state_id IN (:stateIds) AND property_code = :propertyCode",
            nativeQuery = true)
    List<Object[]> findStateIdAndValueNativeUnderscore(
            @Param("stateIds") List<Long> stateIds,
            @Param("propertyCode") String propertyCode);

    /**
     * То же, но второе поле — COALESCE(value, raw_value), чтобы не терять данные при пустом value.
     */
    @Query(
            value = "SELECT welding_machine_stateid, COALESCE(value, raw_value) FROM welding_machine_parameter_value " +
                    "WHERE welding_machine_stateid IN (:stateIds) AND property_code = :propertyCode",
            nativeQuery = true)
    List<Object[]> findStateIdAndValueNativeCoalesce(
            @Param("stateIds") List<Long> stateIds,
            @Param("propertyCode") String propertyCode);

    // Найти все уникальные ID состояний, которые содержат RFID код в properties (за всю историю — тяжёлый запрос)
    @Query("SELECT DISTINCT wmpv.weldingMachineStateId FROM WeldingMachineParameterValue wmpv " +
            "WHERE wmpv.propertyCode IN ('RFID.Hex', 'RFID', 'Rfid', 'rfid') AND wmpv.value = :rfidCode")
    List<Long> findStateIdsByRfidInProperties(@Param("rfidCode") String rfidCode);

    /**
     * То же по смыслу, что {@link #findStateIdsByRfidInProperties}, но только для состояний в интервале {@code date_created}.
     * Нужен для отчётов: без фильтра по дате список id может содержать миллионы записей за всё время.
     */
    @Query("SELECT DISTINCT wmpv.weldingMachineStateId FROM WeldingMachineParameterValue wmpv, WeldingMachineState wms " +
            "WHERE wmpv.weldingMachineStateId = wms.id AND " +
            "wmpv.propertyCode IN ('RFID.Hex', 'RFID', 'Rfid', 'rfid') AND wmpv.value = :rfidCode AND " +
            "wms.dateCreated >= :start AND wms.dateCreated <= :end")
    List<Long> findStateIdsByRfidInPropertiesAndDateRange(
            @Param("rfidCode") String rfidCode,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Параметры за период по аппарату (один JOIN вместо тысяч батчей IN по stateId) — отчёты за 7+ дней.
     */
    @Query(
            value = "SELECT s.id, p.value FROM welding_machine_state s " +
                    "INNER JOIN welding_machine_parameter_value p ON p.welding_machine_stateid = s.id " +
                    "WHERE s.welding_machineid = :machineId " +
                    "AND s.date_created >= :start AND s.date_created <= :end " +
                    "AND p.property_code = :propertyCode",
            nativeQuery = true)
    List<Object[]> findStateIdAndValueByMachineDateRange(
            @Param("machineId") Integer machineId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("propertyCode") String propertyCode);

    @Query(
            value = "SELECT s.id, COALESCE(p.value, p.raw_value) FROM welding_machine_state s " +
                    "INNER JOIN welding_machine_parameter_value p ON p.welding_machine_stateid = s.id " +
                    "WHERE s.welding_machineid = :machineId " +
                    "AND s.date_created >= :start AND s.date_created <= :end " +
                    "AND p.property_code = :propertyCode",
            nativeQuery = true)
    List<Object[]> findStateIdAndValueByMachineDateRangeCoalesce(
            @Param("machineId") Integer machineId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("propertyCode") String propertyCode);

    @Query(
            value = "SELECT s.id, p.value FROM welding_machine_state s " +
                    "INNER JOIN welding_machine_parameter_value p ON p.welding_machine_stateid = s.id " +
                    "WHERE s.welding_machineid = :machineId " +
                    "AND s.date_created >= :start AND s.date_created <= :end " +
                    "AND p.property_code IN (:propertyCodes)",
            nativeQuery = true)
    List<Object[]> findStateIdAndValueByMachineDateRangeAndCodes(
            @Param("machineId") Integer machineId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("propertyCodes") List<String> propertyCodes);

    /** [state_id, property_code, value] — один проход по БД для тока/напряжения за период. */
    @Query(
            value = "SELECT s.id, p.property_code, COALESCE(p.value, p.raw_value) FROM welding_machine_state s " +
                    "INNER JOIN welding_machine_parameter_value p ON p.welding_machine_stateid = s.id " +
                    "WHERE s.welding_machineid = :machineId " +
                    "AND s.date_created >= :start AND s.date_created <= :end " +
                    "AND p.property_code IN (:propertyCodes)",
            nativeQuery = true)
    List<Object[]> findStateIdCodeAndValueByMachineDateRange(
            @Param("machineId") Integer machineId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("propertyCodes") List<String> propertyCodes);

    /** [state_id, property_code, value] — один батч IN вместо отдельного запроса на каждый property_code. */
    @Query(
            value = "SELECT welding_machine_stateid, property_code, COALESCE(value, raw_value) FROM welding_machine_parameter_value " +
                    "WHERE welding_machine_stateid IN (:stateIds) AND property_code IN (:propertyCodes)",
            nativeQuery = true)
    List<Object[]> findStateIdCodeAndValueNativeByStateIds(
            @Param("stateIds") List<Long> stateIds,
            @Param("propertyCodes") List<String> propertyCodes);
}
