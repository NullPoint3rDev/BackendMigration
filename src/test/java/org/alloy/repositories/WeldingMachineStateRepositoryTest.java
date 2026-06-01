package org.alloy.repositories;

import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.Organization;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class WeldingMachineStateRepositoryTest {

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private WeldingMachineTypeRepository weldingMachineTypeRepository;

    private WeldingMachineState state1;
    private WeldingMachineState state2;
    private WeldingMachineState state3;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        weldingMachineStateRepository.deleteAll();
        weldingMachineRepository.deleteAll();
        organizationUnitRepository.deleteAll();
        organizationRepository.deleteAll();
        weldingMachineTypeRepository.deleteAll();

        // Создаем тестовую организацию
        Organization organization = new Organization();
        organization.setName("Test Organization");
        organization.setStatus(GeneralStatus.Active);
        organization.setDateCreated(LocalDateTime.now());
        organizationRepository.save(organization);

        // Создаем тестовое подразделение
        OrganizationUnit organizationUnit = new OrganizationUnit();
        organizationUnit.setOrganizationId(organization.getId());
        organizationUnit.setName("Test Unit");
        organizationUnit.setStatus(GeneralStatus.Active);
        organizationUnit.setDateCreated(LocalDateTime.now());
        organizationUnitRepository.save(organizationUnit);

        // Создаем тестовый тип сварочного аппарата
        WeldingMachineType machineType = new WeldingMachineType();
        machineType.setName("Test Type");
        machineType.setStatus(GeneralStatus.Active);
        machineType.setDateCreated(LocalDateTime.now());
        weldingMachineTypeRepository.save(machineType);

        // Создаем тестовые сварочные аппараты
        WeldingMachine machine1 = new WeldingMachine();
        machine1.setOrganizationUnitId(organizationUnit.getId());
        machine1.setWeldingMachineTypeId(machineType.getId());
        machine1.setStatus(GeneralStatus.Active);
        machine1.setName("Test Machine 1");
        weldingMachineRepository.save(machine1);

        WeldingMachine machine2 = new WeldingMachine();
        machine2.setOrganizationUnitId(organizationUnit.getId());
        machine2.setWeldingMachineTypeId(machineType.getId());
        machine2.setStatus(GeneralStatus.Active);
        machine2.setName("Test Machine 2");
        weldingMachineRepository.save(machine2);

        // Создаем тестовые данные
        state1 = new WeldingMachineState();
        state1.setWeldingMachineId(machine1.getId());
        state1.setWeldingMachineStatus(WeldingMachineStatus.Welding);
        state1.setDateCreated(LocalDateTime.now().minusHours(2));
        state1.setStateDurationMs(3600000L); // 1 час
        state1.setLimitsExceeded(false);

        state2 = new WeldingMachineState();
        state2.setWeldingMachineId(machine1.getId());
        state2.setWeldingMachineStatus(WeldingMachineStatus.Idle);
        state2.setDateCreated(LocalDateTime.now().minusHours(1));
        state2.setStateDurationMs(1800000L); // 30 минут
        state2.setLimitsExceeded(true);

        state3 = new WeldingMachineState();
        state3.setWeldingMachineId(machine2.getId());
        state3.setWeldingMachineStatus(WeldingMachineStatus.Welding);
        state3.setDateCreated(LocalDateTime.now());
        state3.setStateDurationMs(900000L); // 15 минут
        state3.setLimitsExceeded(false);

        // Сохраняем тестовые данные в базу
        weldingMachineStateRepository.saveAll(List.of(state1, state2, state3));
    }

    @Test
    void findByWeldingMachineId_ShouldReturnAllStatesForMachine() {
        // Проверяем поиск всех состояний для конкретного сварочного аппарата
        List<WeldingMachineState> states = weldingMachineStateRepository.findByWeldingMachineId(state1.getWeldingMachineId());

        // Проверяем, что найдено 2 состояния для аппарата с ID=1
        assertThat(states).hasSize(2);
        assertThat(states).extracting(WeldingMachineState::getWeldingMachineStatus)
                .containsExactlyInAnyOrder(WeldingMachineStatus.Welding, WeldingMachineStatus.Idle);
    }

    @Test
    void findTopByWeldingMachineIdOrderByDateCreatedDesc_ShouldReturnLatestState() {
        // Проверяем поиск последнего состояния для конкретного сварочного аппарата
        Optional<WeldingMachineState> latestState = weldingMachineStateRepository
                .findTopByWeldingMachineIdOrderByDateCreatedDesc(state1.getWeldingMachineId());

        // Проверяем, что найдено последнее состояние
        assertThat(latestState).isPresent();
        assertThat(latestState.get().getWeldingMachineStatus()).isEqualTo(WeldingMachineStatus.Idle);
        assertThat(latestState.get().getDateCreated()).isAfter(state1.getDateCreated());
    }

    @Test
    void findByWeldingMachineIdAndWeldingMachineStatus_ShouldReturnStatesWithSpecificStatus() {
        // Проверяем поиск состояний с определенным статусом для конкретного сварочного аппарата
        List<WeldingMachineState> weldingStates = weldingMachineStateRepository
                .findByWeldingMachineIdAndWeldingMachineStatus(state1.getWeldingMachineId(), WeldingMachineStatus.Welding);

        // Проверяем, что найдено состояние со статусом Welding
        assertThat(weldingStates).hasSize(1);
        assertThat(weldingStates.get(0).getWeldingMachineStatus()).isEqualTo(WeldingMachineStatus.Welding);
    }

    @Test
    void findByWeldingMachineIdAndDateRange_ShouldReturnStatesInDateRange() {
        // Проверяем поиск состояний в определенном временном диапазоне
        LocalDateTime startDate = LocalDateTime.now().minusHours(3);
        LocalDateTime endDate = LocalDateTime.now().minusMinutes(30);

        List<WeldingMachineState> states = weldingMachineStateRepository
                .findByWeldingMachineIdAndDateRange(state1.getWeldingMachineId(), startDate, endDate);

        // Проверяем, что найдено состояние в указанном диапазоне
        assertThat(states).hasSize(1);
        assertThat(states.get(0).getDateCreated()).isBetween(startDate, endDate);
    }

    @Test
    void findStatesWithExceededLimits_ShouldReturnStatesWithExceededLimits() {
        // Проверяем поиск состояний с превышенными лимитами
        List<WeldingMachineState> states = weldingMachineStateRepository.findStatesWithExceededLimits(state1.getWeldingMachineId());

        // Проверяем, что найдено состояние с превышенными лимитами
        assertThat(states).hasSize(1);
        assertThat(states.get(0).getLimitsExceeded()).isTrue();
    }
}
