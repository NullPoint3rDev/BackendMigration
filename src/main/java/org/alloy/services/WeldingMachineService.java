package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WeldingMachineService {

    private final WeldingMachineRepository weldingMachineRepository;

    @Autowired
    public WeldingMachineService(WeldingMachineRepository weldingMachineRepository) {
        this.weldingMachineRepository = weldingMachineRepository;
    }

    public List<WeldingMachine> getAllWeldingMachines() {
        return weldingMachineRepository.findAll();
    }

    public Optional<WeldingMachine> getWeldingMachineById(Integer id) {
        return weldingMachineRepository.findById(id);
    }

    public Optional<WeldingMachine> getWeldingMachineBySerialNumber(String serialNumber) {
        return weldingMachineRepository.findBySerialNumber(serialNumber);
    }

    public List<WeldingMachine> getWeldingMachinesByOrganizationId(Integer organizationId) {
        return weldingMachineRepository.findByOrganizationId(organizationId);
    }

    public List<WeldingMachine> getWeldingMachinesByTypeId(Integer typeId) {
        return weldingMachineRepository.findByTypeId(typeId);
    }

    public List<WeldingMachine> searchWeldingMachines(Integer organizationId, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getWeldingMachinesByOrganizationId(organizationId);
        }
        return weldingMachineRepository.searchWeldingMachines(organizationId, searchTerm.trim());
    }

    public WeldingMachine createWeldingMachine(WeldingMachine weldingMachine) {
        // Validate required fields
        if (weldingMachine.getSerialNumber() == null || weldingMachine.getSerialNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Serial number is required");
        }
        if (weldingMachine.getOrganizationId() == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        if (weldingMachine.getTypeId() == null) {
            throw new IllegalArgumentException("Type ID is required");
        }

        // Check if serial number already exists
        if (weldingMachineRepository.findBySerialNumber(weldingMachine.getSerialNumber()).isPresent()) {
            throw new IllegalArgumentException("A welding machine with this serial number already exists");
        }

        // Set creation date and status
        weldingMachine.setDateCreated(LocalDateTime.now());
        weldingMachine.setStatus("Active");

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

        // Preserve creation date
        weldingMachine.setDateCreated(existingMachine.getDateCreated());

        return weldingMachineRepository.save(weldingMachine);
    }

    public void deleteWeldingMachine(Integer id) {
        WeldingMachine weldingMachine = weldingMachineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Welding machine not found"));

        weldingMachine.setStatus("Deleted");
        weldingMachineRepository.save(weldingMachine);
    }

    public void hardDeleteWeldingMachine(Integer id) {
        if (!weldingMachineRepository.existsById(id)) {
            throw new IllegalArgumentException("Welding machine not found");
        }
        weldingMachineRepository.deleteById(id);
    }
}
