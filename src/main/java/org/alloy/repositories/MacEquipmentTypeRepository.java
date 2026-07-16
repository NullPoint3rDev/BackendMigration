package org.alloy.repositories;

import org.alloy.models.entities.MacEquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MacEquipmentTypeRepository extends JpaRepository<MacEquipmentType, Integer> {

    Optional<MacEquipmentType> findByNameIgnoreCase(String name);
}
