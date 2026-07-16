package org.alloy.repositories;

import org.alloy.models.MacRegistryStatus;
import org.alloy.models.entities.MacAddressRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MacAddressRegistryRepository
        extends JpaRepository<MacAddressRegistry, Integer>, JpaSpecificationExecutor<MacAddressRegistry> {

    Optional<MacAddressRegistry> findByMac(String mac);

    List<MacAddressRegistry> findByStatus(MacRegistryStatus status);

    List<MacAddressRegistry> findByIdIn(Collection<Integer> ids);

    Optional<MacAddressRegistry> findByWeldingMachineId(Integer weldingMachineId);

    boolean existsByMac(String mac);
}
