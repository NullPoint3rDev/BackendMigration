package org.alloy.repositories;

import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.WeldingMachineType;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.alloy.TestConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestConfig.class)
public class WeldingMachineTypeRepositoryTest {

    @Autowired
    private WeldingMachineTypeRepository weldingMachineTypeRepository;

    // Тестовые данные
    private WeldingMachineType type1;
    private WeldingMachineType type2;
    private WeldingMachineType type3;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        weldingMachineTypeRepository.deleteAll();

        // Создаем первый тестовый тип сварочного аппарата
        type1 = new WeldingMachineType();
        type1.setName("Тип 1");
        type1.setDescription("Описание типа 1");
        type1.setStatus(GeneralStatus.Active);
        type1.setDateCreated(LocalDateTime.now());
        type1.setSettings("{\"setting1\": \"value1\"}");
        type1.setPropertyLimits("{\"limit1\": 100}");
        type1.setInbound("{\"inbound1\": \"value1\"}");
        type1.setOutbound("{\"outbound1\": \"value1\"}");
        type1.setPresentation("{\"presentation1\": \"value1\"}");
        type1.setModeDefinitions("{\"mode1\": \"value1\"}");
        type1.setAlertDefinitions("{\"alert1\": \"value1\"}");

        // Создаем второй тестовый тип сварочного аппарата
        type2 = new WeldingMachineType();
        type2.setName("Тип 2");
        type2.setDescription("Описание типа 2");
        type2.setStatus(GeneralStatus.Active);
        type2.setDateCreated(LocalDateTime.now());
        type2.setSettings("{\"setting2\": \"value2\"}");
        type2.setPropertyLimits("{\"limit2\": 200}");
        type2.setInbound("{\"inbound2\": \"value2\"}");
        type2.setOutbound("{\"outbound2\": \"value2\"}");
        type2.setPresentation("{\"presentation2\": \"value2\"}");
        type2.setModeDefinitions("{\"mode2\": \"value2\"}");
        type2.setAlertDefinitions("{\"alert2\": \"value2\"}");

        // Создаем третий тестовый тип сварочного аппарата (неактивный)
        type3 = new WeldingMachineType();
        type3.setName("Тип 3");
        type3.setDescription("Описание типа 3");
        type3.setStatus(GeneralStatus.Inactive);
        type3.setDateCreated(LocalDateTime.now());
        type3.setSettings("{\"setting3\": \"value3\"}");
        type3.setPropertyLimits("{\"limit3\": 300}");
        type3.setInbound("{\"inbound3\": \"value3\"}");
        type3.setOutbound("{\"outbound3\": \"value3\"}");
        type3.setPresentation("{\"presentation3\": \"value3\"}");
        type3.setModeDefinitions("{\"mode3\": \"value3\"}");
        type3.setAlertDefinitions("{\"alert3\": \"value3\"}");

        // Сохраняем тестовые данные в базу
        weldingMachineTypeRepository.saveAll(List.of(type1, type2, type3));
    }

    @Test
    void findByName_ShouldReturnWeldingMachineType() {
        // Проверяем поиск типа сварочного аппарата по имени
        Optional<WeldingMachineType> foundType = weldingMachineTypeRepository.findByName("Тип 1");

        // Проверяем, что тип найден и все его поля соответствуют ожидаемым
        assertThat(foundType).isPresent();
        assertThat(foundType.get().getName()).isEqualTo("Тип 1");
        assertThat(foundType.get().getDescription()).isEqualTo("Описание типа 1");
        assertThat(foundType.get().getStatus()).isEqualTo(GeneralStatus.Active);
        assertThat(foundType.get().getSettings()).isEqualTo("{\"setting1\": \"value1\"}");
        assertThat(foundType.get().getPropertyLimits()).isEqualTo("{\"limit1\": 100}");
        assertThat(foundType.get().getInbound()).isEqualTo("{\"inbound1\": \"value1\"}");
        assertThat(foundType.get().getOutbound()).isEqualTo("{\"outbound1\": \"value1\"}");
        assertThat(foundType.get().getPresentation()).isEqualTo("{\"presentation1\": \"value1\"}");
        assertThat(foundType.get().getModeDefinitions()).isEqualTo("{\"mode1\": \"value1\"}");
        assertThat(foundType.get().getAlertDefinitions()).isEqualTo("{\"alert1\": \"value1\"}");
    }

    @Test
    void findByName_ShouldReturnEmpty_WhenTypeNotFound() {
        // Проверяем поиск несуществующего типа сварочного аппарата
        Optional<WeldingMachineType> foundType = weldingMachineTypeRepository.findByName("Несуществующий тип");

        // Проверяем, что тип не найден
        assertThat(foundType).isEmpty();
    }

    @Test
    void findByStatus_ShouldReturnTypesWithSpecificStatus() {
        // Проверяем поиск типов сварочных аппаратов по статусу
        List<WeldingMachineType> activeTypes = weldingMachineTypeRepository.findByStatus(GeneralStatus.Active);
        List<WeldingMachineType> inactiveTypes = weldingMachineTypeRepository.findByStatus(GeneralStatus.Inactive);

        // Проверяем, что найдено правильное количество типов для каждого статуса
        assertThat(activeTypes).hasSize(2);
        assertThat(inactiveTypes).hasSize(1);

        // Проверяем, что все найденные типы имеют правильный статус
        assertThat(activeTypes).allMatch(type -> type.getStatus() == GeneralStatus.Active);
        assertThat(inactiveTypes).allMatch(type -> type.getStatus() == GeneralStatus.Inactive);
    }

    @Test
    void searchWeldingMachineTypes_ShouldReturnTypesMatchingSearchTerm() {
        // Проверяем поиск типов сварочных аппаратов по поисковому запросу
        List<WeldingMachineType> typesByName = weldingMachineTypeRepository.searchWeldingMachineTypes("Тип 1");
        List<WeldingMachineType> typesByDescription = weldingMachineTypeRepository.searchWeldingMachineTypes("Описание типа 2");
        List<WeldingMachineType> typesByPartialMatch = weldingMachineTypeRepository.searchWeldingMachineTypes("тип");
        List<WeldingMachineType> typesByNonExistentTerm = weldingMachineTypeRepository.searchWeldingMachineTypes("несуществующий");

        // Проверяем результаты поиска
        assertThat(typesByName).hasSize(1);
        assertThat(typesByName.get(0).getName()).isEqualTo("Тип 1");

        assertThat(typesByDescription).hasSize(1);
        assertThat(typesByDescription.get(0).getDescription()).isEqualTo("Описание типа 2");

        assertThat(typesByPartialMatch).hasSize(3);
        assertThat(typesByPartialMatch).extracting(WeldingMachineType::getName)
                .containsExactlyInAnyOrder("Тип 1", "Тип 2", "Тип 3");

        assertThat(typesByNonExistentTerm).isEmpty();
    }

    @Test
    void searchWeldingMachineTypes_ShouldBeCaseInsensitive() {
        // Проверяем, что поиск не чувствителен к регистру
        List<WeldingMachineType> typesByUpperCase = weldingMachineTypeRepository.searchWeldingMachineTypes("ТИП 1");
        List<WeldingMachineType> typesByLowerCase = weldingMachineTypeRepository.searchWeldingMachineTypes("тип 1");
        List<WeldingMachineType> typesByMixedCase = weldingMachineTypeRepository.searchWeldingMachineTypes("ТиП 1");

        // Проверяем, что все варианты поиска возвращают одинаковый результат
        assertThat(typesByUpperCase).hasSize(1);
        assertThat(typesByLowerCase).hasSize(1);
        assertThat(typesByMixedCase).hasSize(1);

        assertThat(typesByUpperCase.get(0).getName()).isEqualTo("Тип 1");
        assertThat(typesByLowerCase.get(0).getName()).isEqualTo("Тип 1");
        assertThat(typesByMixedCase.get(0).getName()).isEqualTo("Тип 1");
    }
}
