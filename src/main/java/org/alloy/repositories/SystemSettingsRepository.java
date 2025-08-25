package org.alloy.repositories;

import org.alloy.models.entities.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Integer> {
    
    List<SystemSettings> findByCategory(String category);
    
    List<SystemSettings> findByIsActive(Boolean isActive);
    
    Optional<SystemSettings> findBySettingKey(String settingKey);
    
    @Query("SELECT ss FROM SystemSettings ss WHERE ss.category = :category AND ss.isActive = true")
    List<SystemSettings> findActiveByCategory(@Param("category") String category);
    
    @Query("SELECT ss FROM SystemSettings ss WHERE ss.settingKey = :settingKey AND ss.isActive = true")
    Optional<SystemSettings> findActiveByKey(@Param("settingKey") String settingKey);
    
    boolean existsBySettingKey(String settingKey);
    
    @Query("SELECT ss FROM SystemSettings ss WHERE ss.settingKey LIKE %:searchTerm% OR ss.description LIKE %:searchTerm%")
    List<SystemSettings> findBySearchTerm(@Param("searchTerm") String searchTerm);
}
