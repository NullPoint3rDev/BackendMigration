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

    // Найти все уникальные ID состояний, которые содержат RFID код в properties
    @Query("SELECT DISTINCT wmpv.weldingMachineStateId FROM WeldingMachineParameterValue wmpv " +
            "WHERE wmpv.propertyCode IN ('RFID.Hex', 'RFID', 'Rfid', 'rfid') AND wmpv.value = :rfidCode")
    List<Long> findStateIdsByRfidInProperties(@Param("rfidCode") String rfidCode);
}
