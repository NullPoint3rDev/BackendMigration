package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeldingMachineRepository extends JpaRepository<WeldingMachine, Integer> {

    List<WeldingMachine> findByOrganizationUnitId(Integer organizationUnitId);

    List<WeldingMachine> findByWeldingMachineTypeId(Integer weldingMachineTypeId);

    Optional<WeldingMachine> findByMac(String mac);

    Optional<WeldingMachine> findBySerialNumber(String serialNumber);

    @Query("SELECT wm FROM WeldingMachine wm WHERE wm.organizationUnitId = :organizationUnitId AND (wm.name LIKE %:searchTerm% OR wm.serialNumber LIKE %:searchTerm% OR wm.mac LIKE %:searchTerm%)")
    List<WeldingMachine> searchWeldingMachines(@Param("organizationUnitId") Integer organizationUnitId, @Param("searchTerm") String searchTerm);

    @Query("SELECT wm FROM WeldingMachine wm WHERE wm.id IN :ids")
    List<WeldingMachine> findByIds(@Param("ids") List<Integer> ids);

    @Query("SELECT wm FROM WeldingMachine wm WHERE wm.lastServiceOn < :date AND wm.maintenanceInterval IS NOT NULL")
    List<WeldingMachine> findMachinesNeedingService(@Param("date") LocalDateTime date);
}
