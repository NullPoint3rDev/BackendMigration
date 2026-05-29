package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.GeneralStatus;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineTypeRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Profile("!staging")
@Transactional
public class DataInitializationService {

    private final WeldingMachineRepository weldingMachineRepository;
    private final WeldingMachineTypeRepository weldingMachineTypeRepository;
    private final OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    public DataInitializationService(WeldingMachineRepository weldingMachineRepository,
                                     WeldingMachineTypeRepository weldingMachineTypeRepository,
                                     OrganizationUnitRepository organizationUnitRepository) {
        this.weldingMachineRepository = weldingMachineRepository;
        this.weldingMachineTypeRepository = weldingMachineTypeRepository;
        this.organizationUnitRepository = organizationUnitRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        System.out.println("[DATA-INIT] 🚀 Инициализация данных приложения...");

        // Добавляем новый аппарат "Блок мониторинга ОГК"
        addNewWeldingMachine();

        System.out.println("[DATA-INIT] ✅ Инициализация данных завершена");
    }

    private void addNewWeldingMachine() {
        String machineName = "Блок мониторинга ОГК";
        String machineMac = "8CAAB50C4254";

        // Проверяем, не существует ли уже аппарат с таким MAC
        Optional<WeldingMachine> existingMachine = weldingMachineRepository.findAll().stream()
                .filter(machine -> machineMac.equals(machine.getMac()))
                .findFirst();

        if (existingMachine.isPresent()) {
            System.out.println("[DATA-INIT] ℹ️ Аппарат с MAC " + machineMac + " уже существует: " + existingMachine.get().getName());
            return;
        }

        try {
            // Находим или создаем тип аппарата "Блок мониторинга"
            WeldingMachineType machineType = findOrCreateMachineType("Блок мониторинга", "Блок мониторинга сварочных процессов");

            // Находим подразделение (предполагаем, что есть подразделение с ALLOY в названии)
            OrganizationUnit organizationUnit = findOrganizationUnit();

            if (organizationUnit == null) {
                System.out.println("[DATA-INIT] ⚠️ Не найдено подходящее подразделение для аппарата");
                return;
            }

            // Создаем новый аппарат
            WeldingMachine newMachine = new WeldingMachine();
            newMachine.setName(machineName);
            newMachine.setMac(machineMac);
            newMachine.setSerialNumber("BM-OGK-001");
            newMachine.setInventoryNumber("INV-OGK-001");
            newMachine.setStatus(GeneralStatus.Active);
            newMachine.setDescription("Блок мониторинга сварочных процессов ОГК");
            newMachine.setOrganizationUnitId(organizationUnit.getId());
            newMachine.setWeldingMachineTypeId(machineType.getId());

            WeldingMachine savedMachine = weldingMachineRepository.save(newMachine);

            System.out.println("[DATA-INIT] ✅ Добавлен новый аппарат: " + savedMachine.getName() + " (MAC: " + savedMachine.getMac() + ")");
            System.out.println("[DATA-INIT] 📍 ID: " + savedMachine.getId() + ", Подразделение: " + organizationUnit.getName());

        } catch (Exception e) {
            System.err.println("[DATA-INIT] ❌ Ошибка при добавлении аппарата: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private WeldingMachineType findOrCreateMachineType(String typeName, String description) {
        // Ищем существующий тип
        Optional<WeldingMachineType> existingType = weldingMachineTypeRepository.findAll().stream()
                .filter(type -> typeName.equals(type.getName()))
                .findFirst();

        if (existingType.isPresent()) {
            return existingType.get();
        }

        // Создаем новый тип
        WeldingMachineType newType = new WeldingMachineType();
        newType.setName(typeName);
        newType.setDescription(description);
        newType.setStatus(GeneralStatus.Active);

        WeldingMachineType savedType = weldingMachineTypeRepository.save(newType);
        System.out.println("[DATA-INIT] ✅ Создан новый тип аппарата: " + savedType.getName());

        return savedType;
    }

    private OrganizationUnit findOrganizationUnit() {
        // Ищем подразделение с ALLOY в названии
        Optional<OrganizationUnit> alloyUnit = organizationUnitRepository.findAll().stream()
                .filter(unit -> unit.getName() != null &&
                        (unit.getName().contains("ALLOY") ||
                                unit.getName().contains("Конструкторский") ||
                                unit.getName().contains("конструкторский")))
                .findFirst();

        if (alloyUnit.isPresent()) {
            return alloyUnit.get();
        }

        // Если не найдено, берем первое доступное подразделение
        Optional<OrganizationUnit> firstUnit = organizationUnitRepository.findAll().stream().findFirst();

        if (firstUnit.isPresent()) {
            System.out.println("[DATA-INIT] ⚠️ Используем первое доступное подразделение: " + firstUnit.get().getName());
            return firstUnit.get();
        }

        return null;
    }
}
