package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeldingMachineRepository extends JpaRepository<WeldingMachine, Integer> {

    List<WeldingMachine> findByOrganizationUnitId(Integer organizationUnitId);

    List<WeldingMachine> findByOrganizationUnitIdIn(List<Integer> organizationUnitIds);

    List<WeldingMachine> findByWeldingMachineTypeId(Integer weldingMachineTypeId);

    Optional<WeldingMachine> findByMac(String mac);

    Optional<WeldingMachine> findBySerialNumber(String serialNumber);

    @Query("SELECT wm FROM WeldingMachine wm WHERE wm.organizationUnitId = :organizationUnitId AND (wm.name LIKE %:searchTerm% OR wm.serialNumber LIKE %:searchTerm% OR wm.mac LIKE %:searchTerm%)")
    List<WeldingMachine> searchWeldingMachines(@Param("organizationUnitId") Integer organizationUnitId, @Param("searchTerm") String searchTerm);

    @Query("SELECT wm FROM WeldingMachine wm WHERE wm.id IN :ids")
    List<WeldingMachine> findByIds(@Param("ids") List<Integer> ids);

    @Query("SELECT wm FROM WeldingMachine wm WHERE wm.lastServiceOn < :date AND wm.maintenanceInterval IS NOT NULL")
    List<WeldingMachine> findMachinesNeedingService(@Param("date") LocalDateTime date);

    Optional<WeldingMachine> findByName(String name);

    @Query("SELECT wm FROM WeldingMachine wm WHERE wm.name IN :names")
    List<WeldingMachine> findByNames(@Param("names") List<String> names);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM welder_welding_machine WHERE welding_machine_id = :machineId", nativeQuery = true)
    void deleteWelderMachineLinks(@Param("machineId") Integer machineId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM welding_machine_daily_stats WHERE welding_machine_id = :machineId", nativeQuery = true)
    void deleteDailyStatsByMachineId(@Param("machineId") Integer machineId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM maintenance WHERE welding_machineid = :machineId", nativeQuery = true)
    void deleteMaintenancesByMachineId(@Param("machineId") Integer machineId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM welding_limit_program WHERE welding_machineid = :machineId", nativeQuery = true)
    void deleteLimitProgramsByMachineId(@Param("machineId") Integer machineId);

    /** Узкое обновление last_online_on — без полного save(entity). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WeldingMachine wm SET wm.lastOnlineOn = :now WHERE wm.id = :id "
            + "AND (wm.lastOnlineOn IS NULL OR wm.lastOnlineOn < :threshold)")
    int touchLastOnlineOnIfStale(
            @Param("id") Integer id,
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold);
}
