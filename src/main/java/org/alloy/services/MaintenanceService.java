package org.alloy.services;

import org.alloy.models.entities.Maintenance;
import org.alloy.models.GeneralStatus;
import org.alloy.repositories.MaintenanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;

    @Autowired
    public MaintenanceService(MaintenanceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
    }

    public List<Maintenance> getAllMaintenanceRecords() {
        return maintenanceRepository.findAll();
    }

    public Optional<Maintenance> getMaintenanceRecordById(Integer id) {
        return maintenanceRepository.findById(id);
    }

    public List<Maintenance> getMaintenanceRecordsByMachineId(Integer machineId) {
        return maintenanceRepository.findByWeldingMachineId(machineId);
    }

    public Optional<Maintenance> getLatestMaintenanceRecord(Integer machineId) {
        return maintenanceRepository.findTopByWeldingMachineIdOrderByDateCreatedDesc(machineId);
    }

    public List<Maintenance> getMaintenanceRecordsByStatus(Integer machineId, String status) {
        // status приходит строкой из API — конвертируем в enum (поле сущности типа GeneralStatus).
        return maintenanceRepository.findByWeldingMachineIdAndStatus(machineId, GeneralStatus.valueOf(status));
    }

    public Maintenance createMaintenanceRecord(Maintenance maintenance) {
        // Validate required fields
        if (maintenance.getWeldingMachineId() == null) {
            throw new IllegalArgumentException("Welding machine ID is required");
        }
        if (maintenance.getDescription() == null || maintenance.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (maintenance.getType() == null || maintenance.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Maintenance type is required");
        }

        // Set creation date and status
        maintenance.setDateCreated(LocalDateTime.now());
        if (maintenance.getStatus() == null) {
            maintenance.setStatus(GeneralStatus.Active);
        }

        return maintenanceRepository.save(maintenance);
    }

    public Maintenance updateMaintenanceRecord(Maintenance maintenance) {
        // Validate ID
        if (maintenance.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        // Check if maintenance record exists
        Maintenance existingRecord = maintenanceRepository.findById(maintenance.getId())
                .orElseThrow(() -> new IllegalArgumentException("Maintenance record not found"));

        // Preserve creation date
        maintenance.setDateCreated(existingRecord.getDateCreated());

        return maintenanceRepository.save(maintenance);
    }

    public void deleteMaintenanceRecord(Integer id) {
        if (!maintenanceRepository.existsById(id)) {
            throw new IllegalArgumentException("Maintenance record not found");
        }
        maintenanceRepository.deleteById(id);
    }

    public void deleteAllMaintenanceRecords(Integer machineId) {
        List<Maintenance> records = maintenanceRepository.findByWeldingMachineId(machineId);
        maintenanceRepository.deleteAll(records);
    }
}
