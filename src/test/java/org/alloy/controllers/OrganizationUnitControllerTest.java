package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.Organization;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.services.OrganizationUnitService;
import org.alloy.services.Wt2AccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для OrganizationUnitController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(OrganizationUnitController.class)
@Import(TestConfig.class)
@WithMockUser
public class OrganizationUnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private OrganizationUnitService organizationUnitService;

    @MockBean
    private Wt2AccessService wt2AccessService;

    private OrganizationUnit testOrganizationUnit;
    private List<OrganizationUnit> testOrganizationUnits;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую организацию
        Organization organization = new Organization();
        organization.setId(1);
        organization.setName("Тестовая организация");
        organization.setStatus(GeneralStatus.Active);

        // Создаем тестовое подразделение
        testOrganizationUnit = new OrganizationUnit();
        testOrganizationUnit.setId(1);
        testOrganizationUnit.setOrganization(organization);
        testOrganizationUnit.setOrganizationId(1);
        testOrganizationUnit.setName("Тестовое подразделение");
        testOrganizationUnit.setDescription("Описание тестового подразделения");
        testOrganizationUnit.setStatus(GeneralStatus.Active);
        testOrganizationUnit.setDateCreated(LocalDateTime.now());
        testOrganizationUnit.setAddress("Тестовый адрес");
        testOrganizationUnit.setPhone("+7 (999) 123-45-67");
        testOrganizationUnit.setEmail("test@example.com");
        testOrganizationUnit.setParentId(null);

        // Создаем второе тестовое подразделение
        OrganizationUnit secondUnit = new OrganizationUnit();
        secondUnit.setId(2);
        secondUnit.setOrganization(organization);
        secondUnit.setOrganizationId(1);
        secondUnit.setName("Второе подразделение");
        secondUnit.setDescription("Описание второго подразделения");
        secondUnit.setStatus(GeneralStatus.Active);
        secondUnit.setDateCreated(LocalDateTime.now());
        secondUnit.setParentId(1);

        testOrganizationUnits = Arrays.asList(testOrganizationUnit, secondUnit);

        when(wt2AccessService.filterOrganizationUnits(any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Тест получения всех подразделений
     */
    @Test
    void getAllOrganizationUnits_ShouldReturnAllUnits() throws Exception {
        when(organizationUnitService.getAllOrganizationUnits()).thenReturn(testOrganizationUnits);

        mockMvc.perform(get("/organization-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Тестовое подразделение"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Второе подразделение"));

        verify(organizationUnitService).getAllOrganizationUnits();
    }

    /**
     * Тест получения подразделения по ID
     */
    @Test
    void getOrganizationUnitById_WhenUnitExists_ShouldReturnUnit() throws Exception {
        when(organizationUnitService.getOrganizationUnitById(1)).thenReturn(Optional.of(testOrganizationUnit));

        mockMvc.perform(get("/organization-units/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Тестовое подразделение"));

        verify(organizationUnitService).getOrganizationUnitById(1);
    }

    /**
     * Тест получения несуществующего подразделения по ID
     */
    @Test
    void getOrganizationUnitById_WhenUnitDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(organizationUnitService.getOrganizationUnitById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/organization-units/999"))
                .andExpect(status().isNotFound());

        verify(organizationUnitService).getOrganizationUnitById(999);
    }

    /**
     * Тест получения подразделений по ID организации
     */
    @Test
    void getOrganizationUnitsByOrganizationId_ShouldReturnOrganizationUnits() throws Exception {
        when(organizationUnitService.getOrganizationUnitsByOrganizationId(1)).thenReturn(testOrganizationUnits);

        mockMvc.perform(get("/organization-units/organization/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationId").value(1))
                .andExpect(jsonPath("$[1].organizationId").value(1));

        verify(organizationUnitService).getOrganizationUnitsByOrganizationId(1);
    }

    /**
     * Тест получения подразделений по ID родительского подразделения
     */
    @Test
    void getOrganizationUnitsByParentId_ShouldReturnChildUnits() throws Exception {
        when(organizationUnitService.getOrganizationUnitsByParentId(1)).thenReturn(List.of(testOrganizationUnits.get(1)));

        mockMvc.perform(get("/organization-units/parent/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parentId").value(1));

        verify(organizationUnitService).getOrganizationUnitsByParentId(1);
    }

    /**
     * Тест поиска подразделений
     */
    @Test
    void searchOrganizationUnits_ShouldReturnMatchingUnits() throws Exception {
        when(organizationUnitService.searchOrganizationUnits(1, "тест")).thenReturn(List.of(testOrganizationUnit));

        mockMvc.perform(get("/organization-units/search")
                .param("organizationId", "1")
                .param("searchTerm", "тест"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Тестовое подразделение"));

        verify(organizationUnitService).searchOrganizationUnits(1, "тест");
    }

    /**
     * Тест создания нового подразделения
     */
    @Test
    void createOrganizationUnit_ShouldCreateUnit() throws Exception {
        when(organizationUnitService.createOrganizationUnit(any(OrganizationUnit.class))).thenReturn(testOrganizationUnit);

        mockMvc.perform(post("/organization-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testOrganizationUnit)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Тестовое подразделение"));

        verify(organizationUnitService).createOrganizationUnit(any(OrganizationUnit.class));
    }

    /**
     * Тест создания подразделения с невалидными данными
     */
    @Test
    void createOrganizationUnit_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        testOrganizationUnit.setName(null); // Невалидные данные

        when(organizationUnitService.createOrganizationUnit(any(OrganizationUnit.class)))
                .thenThrow(new IllegalArgumentException("Name is required"));

        mockMvc.perform(post("/organization-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testOrganizationUnit)))
                .andExpect(status().isBadRequest());

        verify(organizationUnitService).createOrganizationUnit(any(OrganizationUnit.class));
    }

    /**
     * Тест обновления существующего подразделения
     */
    @Test
    void updateOrganizationUnit_WhenUnitExists_ShouldUpdateUnit() throws Exception {
        when(organizationUnitService.updateOrganizationUnit(any(OrganizationUnit.class))).thenReturn(testOrganizationUnit);

        mockMvc.perform(put("/organization-units/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testOrganizationUnit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(organizationUnitService).updateOrganizationUnit(any(OrganizationUnit.class));
    }

    /**
     * Тест обновления несуществующего подразделения
     */
    @Test
    void updateOrganizationUnit_WhenUnitDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(organizationUnitService.updateOrganizationUnit(any(OrganizationUnit.class)))
                .thenThrow(new IllegalArgumentException("Organization unit not found"));

        mockMvc.perform(put("/organization-units/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testOrganizationUnit)))
                .andExpect(status().isNotFound());

        verify(organizationUnitService).updateOrganizationUnit(any(OrganizationUnit.class));
    }

    /**
     * Тест мягкого удаления существующего подразделения
     */
    @Test
    void deleteOrganizationUnit_WhenUnitExists_ShouldDeleteUnit() throws Exception {
        doNothing().when(organizationUnitService).deleteOrganizationUnit(1);

        mockMvc.perform(delete("/organization-units/1"))
                .andExpect(status().isNoContent());

        verify(organizationUnitService).deleteOrganizationUnit(1);
    }

    /**
     * Тест мягкого удаления несуществующего подразделения
     */
    @Test
    void deleteOrganizationUnit_WhenUnitDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Organization unit not found"))
                .when(organizationUnitService).deleteOrganizationUnit(999);

        mockMvc.perform(delete("/organization-units/999"))
                .andExpect(status().isNotFound());

        verify(organizationUnitService).deleteOrganizationUnit(999);
    }

    /**
     * Тест жесткого удаления существующего подразделения
     */
    @Test
    void hardDeleteOrganizationUnit_WhenUnitExists_ShouldDeleteUnit() throws Exception {
        doNothing().when(organizationUnitService).hardDeleteOrganizationUnit(1);

        mockMvc.perform(delete("/organization-units/1/hard"))
                .andExpect(status().isNoContent());

        verify(organizationUnitService).hardDeleteOrganizationUnit(1);
    }

    /**
     * Тест жесткого удаления несуществующего подразделения
     */
    @Test
    void hardDeleteOrganizationUnit_WhenUnitDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Organization unit not found"))
                .when(organizationUnitService).hardDeleteOrganizationUnit(999);

        mockMvc.perform(delete("/organization-units/999/hard"))
                .andExpect(status().isNotFound());

        verify(organizationUnitService).hardDeleteOrganizationUnit(999);
    }
}
