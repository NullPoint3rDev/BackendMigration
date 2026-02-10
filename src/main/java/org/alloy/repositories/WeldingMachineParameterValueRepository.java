package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineParameterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    /**
     * Нативный запрос: [state_id, value] по property_code (колонка welding_machine_stateid в БД).
     */
    @Query(
            value = "SELECT welding_machine_stateid, value FROM welding_machine_parameter_value " +
                    "WHERE welding_machine_stateid IN (:stateIds) AND property_code = :propertyCode",
            nativeQuery = true)
    List<Object[]> findStateIdAndValueNative(
            @Param("stateIds") List<Long> stateIds,
            @Param("propertyCode") String propertyCode);

    /**
     * То же, но колонка welding_machine_state_id (с подчёркиванием) — для БД с таким именем.
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

    // Найти все уникальные ID состояний, которые содержат RFID код в properties
    @Query("SELECT DISTINCT wmpv.weldingMachineStateId FROM WeldingMachineParameterValue wmpv " +
            "WHERE wmpv.propertyCode IN ('RFID.Hex', 'RFID', 'Rfid', 'rfid') AND wmpv.value = :rfidCode")
    List<Long> findStateIdsByRfidInProperties(@Param("rfidCode") String rfidCode);
}
