package org.alloy.services;

import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.GeneralStatus;
import org.alloy.repositories.WeldingMachineTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WeldingMachineTypeService {

    private final WeldingMachineTypeRepository weldingMachineTypeRepository;

    @Autowired
    public WeldingMachineTypeService(WeldingMachineTypeRepository weldingMachineTypeRepository) {
        this.weldingMachineTypeRepository = weldingMachineTypeRepository;
    }

    public List<WeldingMachineType> getAllWeldingMachineTypes() {
        return weldingMachineTypeRepository.findAll();
    }

    public Optional<WeldingMachineType> getWeldingMachineTypeById(Integer id) {
        return weldingMachineTypeRepository.findById(id);
    }

    public Optional<WeldingMachineType> getWeldingMachineTypeByName(String name) {
        return weldingMachineTypeRepository.findByName(name);
    }

    public List<WeldingMachineType> getWeldingMachineTypesByStatus(GeneralStatus status) {
        return weldingMachineTypeRepository.findByStatus(status);
    }

    public List<WeldingMachineType> searchWeldingMachineTypes(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllWeldingMachineTypes();
        }
        return weldingMachineTypeRepository.searchWeldingMachineTypes(searchTerm.trim());
    }

    public WeldingMachineType createWeldingMachineType(WeldingMachineType type) {
        // Validate required fields
        if (type.getName() == null || type.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        // Check if name already exists
        if (weldingMachineTypeRepository.findByName(type.getName()).isPresent()) {
            throw new IllegalArgumentException("A welding machine type with this name already exists");
        }

        // Set creation date and status
        type.setDateCreated(LocalDateTime.now());
        if (type.getStatus() == null) {
            type.setStatus(GeneralStatus.Active);
        }

        return weldingMachineTypeRepository.save(type);
    }

    public WeldingMachineType updateWeldingMachineType(WeldingMachineType type) {
        // Validate ID
        if (type.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        // Check if type exists
        WeldingMachineType existingType = weldingMachineTypeRepository.findById(type.getId())
                .orElseThrow(() -> new IllegalArgumentException("Welding machine type not found"));

        // Check if new name conflicts with existing one
        if (!existingType.getName().equals(type.getName())) {
            if (weldingMachineTypeRepository.findByName(type.getName()).isPresent()) {
                throw new IllegalArgumentException("A welding machine type with this name already exists");
            }
        }

        // Preserve creation date
        type.setDateCreated(existingType.getDateCreated());

        return weldingMachineTypeRepository.save(type);
    }

    public void deleteWeldingMachineType(Integer id) {
        WeldingMachineType type = weldingMachineTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Welding machine type not found"));

        type.setStatus(GeneralStatus.Deleted);
        weldingMachineTypeRepository.save(type);
    }

    public void hardDeleteWeldingMachineType(Integer id) {
        if (!weldingMachineTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("Welding machine type not found");
        }
        weldingMachineTypeRepository.deleteById(id);
    }
}
