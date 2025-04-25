package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineParameterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeldingMachineParameterValueRepository extends JpaRepository<WeldingMachineParameterValue, Long> {

    List<WeldingMachineParameterValue> findByWeldingMachineStateId(Long weldingMachineStateId);

    List<WeldingMachineParameterValue> findByPropertyCode(String propertyCode);

    @Query("SELECT wmpv FROM WeldingMachineParameterValue wmpv WHERE wmpv.weldingMachineStateId = :stateId AND wmpv.propertyCode = :propertyCode")
    WeldingMachineParameterValue findByStateIdAndPropertyCode(
            @Param("stateId") Long stateId,
            @Param("propertyCode") String propertyCode);

    @Query("SELECT wmpv FROM WeldingMachineParameterValue wmpv WHERE wmpv.weldingMachineStateId IN :stateIds AND wmpv.propertyCode = :propertyCode")
    List<WeldingMachineParameterValue> findByStateIdsAndPropertyCode(
            @Param("stateIds") List<Long> stateIds,
            @Param("propertyCode") String propertyCode);

    @Query("SELECT wmpv FROM WeldingMachineParameterValue wmpv WHERE wmpv.weldingMachineStateId = :stateId AND wmpv.limitsExceeded = true")
    List<WeldingMachineParameterValue> findParametersWithExceededLimits(@Param("stateId") Long stateId);
}
