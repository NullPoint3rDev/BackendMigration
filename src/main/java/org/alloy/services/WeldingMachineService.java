package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.GeneralStatus;
import org.alloy.repositories.WeldingMachineParameterValueRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.alloy.repositories.WeldingMachineTypeRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WeldingMachineService {

    private static final Logger log = LoggerFactory.getLogger(WeldingMachineService.class);

    private final WeldingMachineRepository weldingMachineRepository;
    private final WeldingMachineTypeRepository weldingMachineTypeRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final WeldingMachineStateRepository weldingMachineStateRepository;
    private final WeldingMachineParameterValueRepository weldingMachineParameterValueRepository;
    private final WeldingMachineLastWeldService weldingMachineLastWeldService;

    @Autowired
    public WeldingMachineService(WeldingMachineRepository weldingMachineRepository,
                                 WeldingMachineTypeRepository weldingMachineTypeRepository,
                                 OrganizationUnitRepository organizationUnitRepository,
                                 WeldingMachineStateRepository weldingMachineStateRepository,
                                 WeldingMachineParameterValueRepository weldingMachineParameterValueRepository,
                                 WeldingMachineLastWeldService weldingMachineLastWeldService) {
        this.weldingMachineRepository = weldingMachineRepository;
        this.weldingMachineTypeRepository = weldingMachineTypeRepository;
        this.organizationUnitRepository = organizationUnitRepository;
        this.weldingMachineStateRepository = weldingMachineStateRepository;
        this.weldingMachineParameterValueRepository = weldingMachineParameterValueRepository;
        this.weldingMachineLastWeldService = weldingMachineLastWeldService;
    }

    public List<WeldingMachine> getAllWeldingMachines() {
        List<WeldingMachine> machines = weldingMachineRepository.findAll().stream()
                .filter(machine -> machine.getStatus() != GeneralStatus.Deleted)
                .collect(Collectors.toList());
        for (WeldingMachine machine : machines) {
            if (machine.getId() == null || machine.getLastWeldAt() != null) {
                continue;
            }
            LocalDateTime fallback = weldingMachineLastWeldService.resolveForDisplay(machine.getId());
            if (fallback != null) {
                machine.setLastWeldAt(fallback);
            }
        }
        return machines;
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
        // WeldingMachineTypeId is now optional
        // if (weldingMachine.getWeldingMachineTypeId() == null) {
        //     throw new IllegalArgumentException("Type ID is required");
        // }

        // Verify that the referenced entities exist
        if (weldingMachine.getWeldingMachineTypeId() != null) {
            weldingMachineTypeRepository.findById(weldingMachine.getWeldingMachineTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid welding machine type ID"));
        }
        OrganizationUnit orgUnit = organizationUnitRepository.findById(weldingMachine.getOrganizationUnitId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid organization unit ID"));

        // Set creation date and status
        weldingMachine.setDateCreated(LocalDateTime.now());
        weldingMachine.setStatus(GeneralStatus.Active);
        // weldingMachineType is now optional, so we don't set it here
        weldingMachine.setOrganizationUnit(orgUnit);

        // RFID активирован по умолчанию, если явно не задано иное.
        if (weldingMachine.getRfidEnabled() == null) {
            weldingMachine.setRfidEnabled(true);
        }

        return weldingMachineRepository.save(weldingMachine);
    }

    /** Активные аппараты того же предприятия (organization), что и указанное подразделение. */
    private List<WeldingMachine> activeMachinesInSameOrganization(Integer organizationUnitId) {
        if (organizationUnitId == null) {
            return java.util.Collections.emptyList();
        }
        OrganizationUnit unit = organizationUnitRepository.findById(organizationUnitId).orElse(null);
        List<Integer> unitIds;
        if (unit != null && unit.getOrganizationId() != null) {
            unitIds = organizationUnitRepository.findByOrganizationId(unit.getOrganizationId()).stream()
                    .map(OrganizationUnit::getId)
                    .collect(Collectors.toList());
        } else {
            unitIds = java.util.Collections.singletonList(organizationUnitId);
        }
        if (unitIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return weldingMachineRepository.findByOrganizationUnitIdIn(unitIds).stream()
                .filter(m -> m.getStatus() != GeneralStatus.Deleted)
                .collect(Collectors.toList());
    }

    /** Наименование уже используется другим аппаратом в этом предприятии. */
    public boolean isNameTakenInOrganization(String name, Integer organizationUnitId, Integer excludeId) {
        if (name == null || name.trim().isEmpty() || organizationUnitId == null) {
            return false;
        }
        String target = name.trim();
        return activeMachinesInSameOrganization(organizationUnitId).stream()
                .anyMatch(m -> !Objects.equals(m.getId(), excludeId)
                        && m.getName() != null
                        && m.getName().trim().equalsIgnoreCase(target));
    }

    /** Инвентарный номер уже используется другим аппаратом в этом предприятии. */
    public boolean isInventoryNumberTakenInOrganization(String inventoryNumber, Integer organizationUnitId, Integer excludeId) {
        if (inventoryNumber == null || inventoryNumber.trim().isEmpty() || organizationUnitId == null) {
            return false;
        }
        String target = inventoryNumber.trim();
        return activeMachinesInSameOrganization(organizationUnitId).stream()
                .anyMatch(m -> !Objects.equals(m.getId(), excludeId)
                        && m.getInventoryNumber() != null
                        && m.getInventoryNumber().trim().equalsIgnoreCase(target));
    }

    public WeldingMachine updateWeldingMachine(WeldingMachine incoming) {
        if (incoming == null || incoming.getId() == null) {
            throw new IllegalArgumentException("ID is required for update");
        }

        WeldingMachine existing = weldingMachineRepository.findById(incoming.getId())
                .orElseThrow(() -> new IllegalArgumentException("Welding machine not found"));

        // Обновляем только скалярные поля на managed-сущности. Нельзя save(incoming):
        // у нового объекта пустые @OneToMany с orphanRemoval — Hibernate удалит все states/maintenances.
        if (incoming.getName() != null) {
            existing.setName(incoming.getName());
        }
        if (incoming.getMac() != null && !incoming.getMac().trim().isEmpty()) {
            existing.setMac(incoming.getMac().trim());
        }
        if (incoming.getDeviceModel() != null) {
            existing.setDeviceModel(incoming.getDeviceModel());
        }
        if (incoming.getSerialNumber() != null) {
            String newSerialNumber = incoming.getSerialNumber();
            if (!Objects.equals(existing.getSerialNumber(), newSerialNumber)
                    && weldingMachineRepository.findBySerialNumber(newSerialNumber).isPresent()) {
                throw new IllegalArgumentException("A welding machine with this serial number already exists");
            }
            existing.setSerialNumber(newSerialNumber);
        }
        if (incoming.getInventoryNumber() != null) {
            existing.setInventoryNumber(incoming.getInventoryNumber());
        }
        if (incoming.getYearManufactured() != null) {
            existing.setYearManufactured(incoming.getYearManufactured());
        }
        if (incoming.getDateStartedUsing() != null) {
            existing.setDateStartedUsing(incoming.getDateStartedUsing());
        }
        if (incoming.getLastServiceOn() != null) {
            existing.setLastServiceOn(incoming.getLastServiceOn());
        }
        if (incoming.getModules() != null) {
            existing.setModules(incoming.getModules());
        }
        if (incoming.getMaintenanceRegulation() != null) {
            existing.setMaintenanceRegulation(incoming.getMaintenanceRegulation());
        }
        if (incoming.getMaintenanceInterval() != null) {
            existing.setMaintenanceInterval(incoming.getMaintenanceInterval());
        }
        if (incoming.getMaintenanceIntervalUnit() != null) {
            existing.setMaintenanceIntervalUnit(incoming.getMaintenanceIntervalUnit());
        }
        if (incoming.getManufactureDate() != null) {
            existing.setManufactureDate(incoming.getManufactureDate());
        }
        if (incoming.getRfidEnabled() != null) {
            existing.setRfidEnabled(incoming.getRfidEnabled());
        }
        if (incoming.getUserServiceNotifiedBeforeHours() != null) {
            existing.setUserServiceNotifiedBeforeHours(incoming.getUserServiceNotifiedBeforeHours());
        }
        if (incoming.getStatus() != null) {
            existing.setStatus(incoming.getStatus());
        }

        if (incoming.getWeldingMachineTypeId() != null) {
            WeldingMachineType type = weldingMachineTypeRepository.findById(incoming.getWeldingMachineTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid welding machine type ID"));
            existing.setWeldingMachineTypeId(type.getId());
            existing.setWeldingMachineType(type);
        }

        if (incoming.getOrganizationUnitId() != null) {
            OrganizationUnit orgUnit = organizationUnitRepository.findById(incoming.getOrganizationUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid organization unit ID"));
            existing.setOrganizationUnitId(orgUnit.getId());
            existing.setOrganizationUnit(orgUnit);
        }

        return weldingMachineRepository.save(existing);
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

        long t0 = System.currentTimeMillis();
        log.info("hardDeleteWeldingMachine: start id={}", id);

        weldingMachineRepository.deleteWelderMachineLinks(id);
        log.info("hardDeleteWeldingMachine: id={} welder links deleted (+{} ms)", id, System.currentTimeMillis() - t0);

        weldingMachineRepository.deleteWeldSegmentsByMachineId(id);
        weldingMachineRepository.deleteWeldSegmentDayMarksByMachineId(id);
        log.info("hardDeleteWeldingMachine: id={} weld segments deleted (+{} ms)", id, System.currentTimeMillis() - t0);

        weldingMachineRepository.deleteParameterValuesByMachineId(id);
        log.info("hardDeleteWeldingMachine: id={} parameter values deleted (+{} ms)", id, System.currentTimeMillis() - t0);

        weldingMachineRepository.deleteStatesByMachineId(id);
        log.info("hardDeleteWeldingMachine: id={} states deleted (+{} ms)", id, System.currentTimeMillis() - t0);

        weldingMachineRepository.deleteDailyStatsByMachineId(id);
        weldingMachineRepository.deleteMaintenancesByMachineId(id);
        weldingMachineRepository.deleteLimitProgramsByMachineId(id);

        weldingMachineRepository.deleteById(id);
        log.info("hardDeleteWeldingMachine: id={} done (+{} ms total)", id, System.currentTimeMillis() - t0);
    }
}
