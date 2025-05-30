package org.alloy.repositories;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.entities.Organization;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.alloy.TestConfig;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestConfig.class)
public class WeldingMachineRepositoryTest {

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private WeldingMachineTypeRepository weldingMachineTypeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private WeldingMachine weldingMachine1;
    private WeldingMachine weldingMachine2;
    private WeldingMachine weldingMachine3;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        weldingMachineRepository.deleteAll();
        organizationUnitRepository.deleteAll();
        weldingMachineTypeRepository.deleteAll();
        organizationRepository.deleteAll();

        // Создаем организацию
        Organization organization = new Organization();
        organization.setName("Тестовая организация");
        organization.setStatus(GeneralStatus.Active);
        organization = organizationRepository.save(organization);

        // Создаем необходимые подразделения
        OrganizationUnit orgUnit1 = new OrganizationUnit();
        orgUnit1.setName("Подразделение 1");
        orgUnit1.setStatus(GeneralStatus.Active);
        orgUnit1.setOrganizationId(organization.getId());
        orgUnit1 = organizationUnitRepository.save(orgUnit1);

        OrganizationUnit orgUnit2 = new OrganizationUnit();
        orgUnit2.setName("Подразделение 2");
        orgUnit2.setStatus(GeneralStatus.Active);
        orgUnit2.setOrganizationId(organization.getId());
        orgUnit2 = organizationUnitRepository.save(orgUnit2);

        // Создаем необходимые типы сварочных аппаратов
        WeldingMachineType type1 = new WeldingMachineType();
        type1.setName("Тип 1");
        type1.setStatus(GeneralStatus.Active);
        type1 = weldingMachineTypeRepository.save(type1);

        WeldingMachineType type2 = new WeldingMachineType();
        type2.setName("Тип 2");
        type2.setStatus(GeneralStatus.Active);
        type2 = weldingMachineTypeRepository.save(type2);

        // Создаем тестовые данные
        weldingMachine1 = new WeldingMachine();
        weldingMachine1.setOrganizationUnitId(orgUnit1.getId());
        weldingMachine1.setWeldingMachineTypeId(type1.getId());
        weldingMachine1.setStatus(GeneralStatus.Active);
        weldingMachine1.setName("Сварочный аппарат 1");
        weldingMachine1.setMac("00:11:22:33:44:55");
        weldingMachine1.setSerialNumber("SN001");
        weldingMachine1.setMaintenanceInterval(100.0);
        weldingMachine1.setLastServiceOn(LocalDateTime.now().minusDays(50));
        weldingMachine1.setDateCreated(LocalDateTime.now());

        weldingMachine2 = new WeldingMachine();
        weldingMachine2.setOrganizationUnitId(orgUnit1.getId());
        weldingMachine2.setWeldingMachineTypeId(type2.getId());
        weldingMachine2.setStatus(GeneralStatus.Active);
        weldingMachine2.setName("Сварочный аппарат 2");
        weldingMachine2.setMac("00:11:22:33:44:56");
        weldingMachine2.setSerialNumber("SN002");
        weldingMachine2.setMaintenanceInterval(200.0);
        weldingMachine2.setLastServiceOn(LocalDateTime.now().minusDays(150));
        weldingMachine2.setDateCreated(LocalDateTime.now());

        weldingMachine3 = new WeldingMachine();
        weldingMachine3.setOrganizationUnitId(orgUnit2.getId());
        weldingMachine3.setWeldingMachineTypeId(type1.getId());
        weldingMachine3.setStatus(GeneralStatus.Inactive);
        weldingMachine3.setName("Сварочный аппарат 3");
        weldingMachine3.setMac("00:11:22:33:44:57");
        weldingMachine3.setSerialNumber("SN003");
        weldingMachine3.setMaintenanceInterval(150.0);
        weldingMachine3.setLastServiceOn(LocalDateTime.now().minusDays(100));
        weldingMachine3.setDateCreated(LocalDateTime.now());

        // Сохраняем тестовые данные в базу
        weldingMachineRepository.saveAll(Arrays.asList(weldingMachine1, weldingMachine2, weldingMachine3));
    }

    @Test
    void findByOrganizationUnitId_ShouldReturnCorrectMachines() {
        // Проверяем поиск сварочных аппаратов по ID подразделения
        List<WeldingMachine> machines = weldingMachineRepository.findByOrganizationUnitId(1);

        // Проверяем, что найдено 2 аппарата для подразделения с ID=1
        assertThat(machines).hasSize(2);
        assertThat(machines).extracting(WeldingMachine::getName)
                .containsExactlyInAnyOrder("Сварочный аппарат 1", "Сварочный аппарат 2");
    }

    @Test
    void findByWeldingMachineTypeId_ShouldReturnCorrectMachines() {
        // Проверяем поиск сварочных аппаратов по ID типа
        List<WeldingMachine> machines = weldingMachineRepository.findByWeldingMachineTypeId(1);

        // Проверяем, что найдено 2 аппарата типа с ID=1
        assertThat(machines).hasSize(2);
        assertThat(machines).extracting(WeldingMachine::getName)
                .containsExactlyInAnyOrder("Сварочный аппарат 1", "Сварочный аппарат 3");
    }

    @Test
    void findByMac_ShouldReturnCorrectMachine() {
        // Проверяем поиск сварочного аппарата по MAC-адресу
        Optional<WeldingMachine> machine = weldingMachineRepository.findByMac("00:11:22:33:44:55");

        // Проверяем, что найден правильный аппарат
        assertThat(machine).isPresent();
        assertThat(machine.get().getName()).isEqualTo("Сварочный аппарат 1");
    }

    @Test
    void findBySerialNumber_ShouldReturnCorrectMachine() {
        // Проверяем поиск сварочного аппарата по серийному номеру
        Optional<WeldingMachine> machine = weldingMachineRepository.findBySerialNumber("SN002");

        // Проверяем, что найден правильный аппарат
        assertThat(machine).isPresent();
        assertThat(machine.get().getName()).isEqualTo("Сварочный аппарат 2");
    }

    @Test
    void searchWeldingMachines_ShouldReturnMatchingMachines() {
        // Проверяем поиск сварочных аппаратов по поисковому запросу
        List<WeldingMachine> machines = weldingMachineRepository.searchWeldingMachines(1, "аппарат 1");

        // Проверяем, что найден правильный аппарат
        assertThat(machines).hasSize(1);
        assertThat(machines.get(0).getName()).isEqualTo("Сварочный аппарат 1");
    }

    @Test
    void findByIds_ShouldReturnCorrectMachines() {
        // Проверяем поиск сварочных аппаратов по списку ID
        List<WeldingMachine> machines = weldingMachineRepository.findByIds(
                Arrays.asList(weldingMachine1.getId(), weldingMachine3.getId()));

        // Проверяем, что найдены правильные аппараты
        assertThat(machines).hasSize(2);
        assertThat(machines).extracting(WeldingMachine::getName)
                .containsExactlyInAnyOrder("Сварочный аппарат 1", "Сварочный аппарат 3");
    }

    @Test
    void findMachinesNeedingService_ShouldReturnMachinesWithOverdueService() {
        // Проверяем поиск сварочных аппаратов, требующих обслуживания
        LocalDateTime checkDate = LocalDateTime.now().minusDays(120);
        List<WeldingMachine> machines = weldingMachineRepository.findMachinesNeedingService(checkDate);

        // Проверяем, что найдены аппараты, у которых последнее обслуживание было более 120 дней назад
        assertThat(machines).hasSize(1);
        assertThat(machines.get(0).getName()).isEqualTo("Сварочный аппарат 2");
    }
}
