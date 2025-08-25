package org.alloy.repositories;

import org.alloy.models.entities.NetworkEquipment;
import org.alloy.models.GeneralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkEquipmentRepository extends JpaRepository<NetworkEquipment, Integer> {
    
    List<NetworkEquipment> findByStatus(GeneralStatus status);
    
    List<NetworkEquipment> findByType(NetworkEquipment.EquipmentType type);
    
    Optional<NetworkEquipment> findByMacAddress(String macAddress);
    
    Optional<NetworkEquipment> findByIpAddress(String ipAddress);
    
    List<NetworkEquipment> findByLocationContainingIgnoreCase(String location);
    
    @Query("SELECT ne FROM NetworkEquipment ne WHERE ne.name LIKE %:searchTerm% OR ne.description LIKE %:searchTerm%")
    List<NetworkEquipment> findBySearchTerm(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT ne FROM NetworkEquipment ne WHERE ne.lastSeen < :threshold")
    List<NetworkEquipment> findInactiveEquipment(@Param("threshold") java.time.LocalDateTime threshold);
    
    boolean existsByMacAddress(String macAddress);
    
    boolean existsByIpAddress(String ipAddress);
}
