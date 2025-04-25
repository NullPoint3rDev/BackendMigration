package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeldingMachineTypeRepository extends JpaRepository<WeldingMachineType, Integer> {
} 