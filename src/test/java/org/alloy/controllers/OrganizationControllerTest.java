package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.Organization;
import org.alloy.services.OrganizationService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для OrganizationController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(OrganizationController.class)
@Import(TestConfig.class)
@WithMockUser
public class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private Wt2AccessService wt2AccessService;

    private Organization testOrganization;
    private List<Organization> testOrganizations;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую организацию
        testOrganization = new Organization();
        testOrganization.setId(1);
        testOrganization.setName("Тестовая организация");
        testOrganization.setDescription("Описание тестовой организации");
        testOrganization.setStatus(GeneralStatus.Active);
        testOrganization.setDateCreated(LocalDateTime.now());
        testOrganization.setAddress("Тестовый адрес");
        testOrganization.setPhone("+7 (999) 123-45-67");
        testOrganization.setEmail("test@example.com");
        testOrganization.setWebsite("https://test.example.com");
        testOrganization.setLogo("test-logo.png");
        testOrganization.setSettings("{}");

        // Создаем вторую тестовую организацию
        Organization secondOrganization = new Organization();
        secondOrganization.setId(2);
        secondOrganization.setName("Вторая организация");
        secondOrganization.setDescription("Описание второй организации");
        secondOrganization.setStatus(GeneralStatus.Active);
        secondOrganization.setDateCreated(LocalDateTime.now());

        testOrganizations = Arrays.asList(testOrganization, secondOrganization);
    }

    /**
     * Тест получения всех организаций
     */
    @Test
    void getAllOrganizations_ShouldReturnAllOrganizations() throws Exception {
        when(organizationService.getAllOrganizations()).thenReturn(testOrganizations);

        mockMvc.perform(get("/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Тестовая организация"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Вторая организация"));

        verify(organizationService).getAllOrganizations();
    }

    /**
     * Тест получения организации по ID
     */
    @Test
    void getOrganizationById_WhenOrganizationExists_ShouldReturnOrganization() throws Exception {
        when(organizationService.getOrganizationById(1)).thenReturn(Optional.of(testOrganization));

        mockMvc.perform(get("/organizations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Тестовая организация"));

        verify(organizationService).getOrganizationById(1);
    }

    /**
     * Тест получения несуществующей организации по ID
     */
    @Test
    void getOrganizationById_WhenOrganizationDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(organizationService.getOrganizationById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/organizations/999"))
                .andExpect(status().isNotFound());

        verify(organizationService).getOrganizationById(999);
    }

    /**
     * Тест поиска организаций
     */
    @Test
    void searchOrganizations_ShouldReturnMatchingOrganizations() throws Exception {
        when(organizationService.searchOrganizations("тест")).thenReturn(List.of(testOrganization));

        mockMvc.perform(get("/organizations/search")
                .param("searchTerm", "тест"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Тестовая организация"));

        verify(organizationService).searchOrganizations("тест");
    }

    /**
     * Тест создания новой организации
     */
    @Test
    void createOrganization_ShouldCreateOrganization() throws Exception {
        when(organizationService.createOrganization(any(Organization.class))).thenReturn(testOrganization);

        mockMvc.perform(post("/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testOrganization)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Тестовая организация"));

        verify(organizationService).createOrganization(any(Organization.class));
    }

    /**
     * Тест создания организации с невалидными данными
     */
    @Test
    void createOrganization_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        testOrganization.setName(null); // Невалидные данные

        when(organizationService.createOrganization(any(Organization.class)))
                .thenThrow(new IllegalArgumentException("Name is required"));

        mockMvc.perform(post("/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testOrganization)))
                .andExpect(status().isBadRequest());

        verify(organizationService).createOrganization(any(Organization.class));
    }

    /**
     * Тест обновления существующей организации
     */
    @Test
    void updateOrganization_WhenOrganizationExists_ShouldUpdateOrganization() throws Exception {
        when(organizationService.updateOrganization(any(Organization.class))).thenReturn(testOrganization);

        mockMvc.perform(put("/organizations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testOrganization)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(organizationService).updateOrganization(any(Organization.class));
    }

    /**
     * Тест обновления несуществующей организации
     */
    @Test
    void updateOrganization_WhenOrganizationDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(organizationService.updateOrganization(any(Organization.class)))
                .thenThrow(new IllegalArgumentException("Organization not found"));

        mockMvc.perform(put("/organizations/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testOrganization)))
                .andExpect(status().isNotFound());

        verify(organizationService).updateOrganization(any(Organization.class));
    }

    /**
     * Тест мягкого удаления существующей организации
     */
    @Test
    void deleteOrganization_WhenOrganizationExists_ShouldDeleteOrganization() throws Exception {
        doNothing().when(organizationService).deleteOrganization(1);

        mockMvc.perform(delete("/organizations/1"))
                .andExpect(status().isNoContent());

        verify(organizationService).deleteOrganization(1);
    }

    /**
     * Тест мягкого удаления несуществующей организации
     */
    @Test
    void deleteOrganization_WhenOrganizationDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Organization not found"))
                .when(organizationService).deleteOrganization(999);

        mockMvc.perform(delete("/organizations/999"))
                .andExpect(status().isNotFound());

        verify(organizationService).deleteOrganization(999);
    }

    /**
     * Тест жесткого удаления существующей организации
     */
    @Test
    void hardDeleteOrganization_WhenOrganizationExists_ShouldDeleteOrganization() throws Exception {
        doNothing().when(organizationService).hardDeleteOrganization(1);

        mockMvc.perform(delete("/organizations/1/hard"))
                .andExpect(status().isNoContent());

        verify(organizationService).hardDeleteOrganization(1);
    }

    /**
     * Тест жесткого удаления несуществующей организации
     */
    @Test
    void hardDeleteOrganization_WhenOrganizationDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Organization not found"))
                .when(organizationService).hardDeleteOrganization(999);

        mockMvc.perform(delete("/organizations/999/hard"))
                .andExpect(status().isNotFound());

        verify(organizationService).hardDeleteOrganization(999);
    }
}
