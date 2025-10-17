package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.GeneralStatus;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineTypeRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WeldingMachineService {

    private final WeldingMachineRepository weldingMachineRepository;
    private final WeldingMachineTypeRepository weldingMachineTypeRepository;
    private final OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    public WeldingMachineService(WeldingMachineRepository weldingMachineRepository,
                                 WeldingMachineTypeRepository weldingMachineTypeRepository,
                                 OrganizationUnitRepository organizationUnitRepository) {
        this.weldingMachineRepository = weldingMachineRepository;
        this.weldingMachineTypeRepository = weldingMachineTypeRepository;
        this.organizationUnitRepository = organizationUnitRepository;
    }

    public List<WeldingMachine> getAllWeldingMachines() {
        return weldingMachineRepository.findAll().stream()
                .filter(machine -> machine.getStatus() != GeneralStatus.Deleted)
                .collect(Collectors.toList());
    }

    public Optional<WeldingMachine> getWeldingMachineById(Integer id) {
        return weldingMachineRepository.findById(id);
    }

    public Optional<WeldingMachine> getWeldingMachineBySerialNumber(String serialNumber) {
        return weldingMachineRepository.findBySerialNumber(serialNumber);
    }

    public Optional<WeldingMachine> getWeldingMachineByMac(String mac) {
        return weldingMachineRepository.findByMac(mac);
    }

    public List<WeldingMachine> getWeldingMachinesByOrganizationId(Integer organizationUnitId) {
        return weldingMachineRepository.findByOrganizationUnitId(organizationUnitId).stream()
                .filter(machine -> machine.getStatus() != GeneralStatus.Deleted)
                .collect(Collectors.toList());
    }

    public List<WeldingMachine> getWeldingMachinesByTypeId(Integer typeId) {
        return weldingMachineRepository.findByWeldingMachineTypeId(typeId).stream()
                .filter(machine -> machine.getStatus() != GeneralStatus.Deleted)
                .collect(Collectors.toList());
    }

    public List<WeldingMachine> searchWeldingMachines(Integer organizationUnitId, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getWeldingMachinesByOrganizationId(organizationUnitId);
        }
        return weldingMachineRepository.searchWeldingMachines(organizationUnitId, searchTerm.trim()).stream()
                .filter(machine -> machine.getStatus() != GeneralStatus.Deleted)
                .collect(Collectors.toList());
    }

    public WeldingMachine createWeldingMachine(WeldingMachine weldingMachine) {
        // Validate required fields
        if (weldingMachine.getMac() == null || weldingMachine.getMac().trim().isEmpty()) {
            throw new IllegalArgumentException("MAC address is required");
        }
        if (weldingMachine.getName() == null || weldingMachine.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (weldingMachine.getOrganizationUnitId() == null) {
            throw new IllegalArgumentException("Organization Unit ID is required");
        }
        if (weldingMachine.getWeldingMachineTypeId() == null) {
            throw new IllegalArgumentException("Type ID is required");
        }

        // Verify that the referenced entities exist
        WeldingMachineType type = weldingMachineTypeRepository.findById(weldingMachine.getWeldingMachineTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid welding machine type ID"));
        OrganizationUnit orgUnit = organizationUnitRepository.findById(weldingMachine.getOrganizationUnitId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid organization unit ID"));

        // Set creation date and status
        weldingMachine.setDateCreated(LocalDateTime.now());
        weldingMachine.setStatus(GeneralStatus.Active);
        weldingMachine.setWeldingMachineType(type);
        weldingMachine.setOrganizationUnit(orgUnit);

        return weldingMachineRepository.save(weldingMachine);
    }

    public WeldingMachine updateWeldingMachine(WeldingMachine weldingMachine) {
        // Validate ID
        if (weldingMachine.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        // Check if welding machine exists
        WeldingMachine existingMachine = weldingMachineRepository.findById(weldingMachine.getId())
                .orElseThrow(() -> new IllegalArgumentException("Welding machine not found"));

        // Check if new serial number conflicts with existing one
        if (!existingMachine.getSerialNumber().equals(weldingMachine.getSerialNumber())) {
            if (weldingMachineRepository.findBySerialNumber(weldingMachine.getSerialNumber()).isPresent()) {
                throw new IllegalArgumentException("A welding machine with this serial number already exists");
            }
        }

        // Verify that the referenced entities exist
        if (weldingMachine.getWeldingMachineTypeId() != null) {
            WeldingMachineType type = weldingMachineTypeRepository.findById(weldingMachine.getWeldingMachineTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid welding machine type ID"));
            weldingMachine.setWeldingMachineType(type);
        }
        if (weldingMachine.getOrganizationUnitId() != null) {
            OrganizationUnit orgUnit = organizationUnitRepository.findById(weldingMachine.getOrganizationUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid organization unit ID"));
            weldingMachine.setOrganizationUnit(orgUnit);
        }

        // Preserve creation date
        weldingMachine.setDateCreated(existingMachine.getDateCreated());

        return weldingMachineRepository.save(weldingMachine);
    }

    public void deleteWeldingMachine(Integer id) {
        WeldingMachine weldingMachine = weldingMachineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Welding machine not found"));

        weldingMachine.setStatus(GeneralStatus.Deleted);
        weldingMachineRepository.save(weldingMachine);
    }

    public void hardDeleteWeldingMachine(Integer id) {
        if (!weldingMachineRepository.existsById(id)) {
            throw new IllegalArgumentException("Welding machine not found");
        }
        weldingMachineRepository.deleteById(id);
    }
}
