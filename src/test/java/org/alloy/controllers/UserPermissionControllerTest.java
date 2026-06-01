package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.UserPermission;
import org.alloy.services.UserPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для UserPermissionController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(UserPermissionController.class)
@Import(TestConfig.class)
@WithMockUser
public class UserPermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserPermissionService userPermissionService;

    private UserPermission testUserPermission;
    private List<UserPermission> testUserPermissions;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовое разрешение
        testUserPermission = new UserPermission();
        testUserPermission.setId(1);
        testUserPermission.setName("READ_USERS");

        // Создаем второе тестовое разрешение
        UserPermission secondPermission = new UserPermission();
        secondPermission.setId(2);
        secondPermission.setName("WRITE_USERS");

        // Создаем список тестовых разрешений
        testUserPermissions = Arrays.asList(testUserPermission, secondPermission);
    }

    /**
     * Тест получения всех разрешений
     */
    @Test
    void getAll_ShouldReturnListOfAllPermissions() throws Exception {
        when(userPermissionService.findAll()).thenReturn(testUserPermissions);

        mockMvc.perform(get("/user-permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("READ_USERS"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("WRITE_USERS"));

        verify(userPermissionService).findAll();
    }

    /**
     * Тест получения разрешения по ID
     */
    @Test
    void getById_WhenPermissionExists_ShouldReturnPermission() throws Exception {
        when(userPermissionService.findById(1)).thenReturn(Optional.of(testUserPermission));

        mockMvc.perform(get("/user-permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("READ_USERS"));

        verify(userPermissionService).findById(1);
    }

    /**
     * Тест получения несуществующего разрешения по ID
     */
    @Test
    void getById_WhenPermissionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userPermissionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/user-permissions/999"))
                .andExpect(status().isNotFound());

        verify(userPermissionService).findById(999);
    }

    /**
     * Тест создания нового разрешения
     */
    @Test
    void create_ShouldCreatePermission() throws Exception {
        when(userPermissionService.save(any(UserPermission.class))).thenReturn(testUserPermission);

        mockMvc.perform(post("/user-permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserPermission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("READ_USERS"));

        verify(userPermissionService).save(any(UserPermission.class));
    }

    /**
     * Тест обновления существующего разрешения
     */
    @Test
    void update_WhenPermissionExists_ShouldUpdatePermission() throws Exception {
        when(userPermissionService.findById(1)).thenReturn(Optional.of(testUserPermission));
        when(userPermissionService.save(any(UserPermission.class))).thenReturn(testUserPermission);

        mockMvc.perform(put("/user-permissions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserPermission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("READ_USERS"));

        verify(userPermissionService).findById(1);
        verify(userPermissionService).save(any(UserPermission.class));
    }

    /**
     * Тест обновления несуществующего разрешения
     */
    @Test
    void update_WhenPermissionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userPermissionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/user-permissions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserPermission)))
                .andExpect(status().isNotFound());

        verify(userPermissionService).findById(999);
        verify(userPermissionService, never()).save(any(UserPermission.class));
    }

    /**
     * Тест удаления существующего разрешения
     */
    @Test
    void delete_WhenPermissionExists_ShouldDeletePermission() throws Exception {
        when(userPermissionService.findById(1)).thenReturn(Optional.of(testUserPermission));
        doNothing().when(userPermissionService).deleteById(1);

        mockMvc.perform(delete("/user-permissions/1"))
                .andExpect(status().isNoContent());

        verify(userPermissionService).findById(1);
        verify(userPermissionService).deleteById(1);
    }

    /**
     * Тест удаления несуществующего разрешения
     */
    @Test
    void delete_WhenPermissionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userPermissionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/user-permissions/999"))
                .andExpect(status().isNotFound());

        verify(userPermissionService).findById(999);
        verify(userPermissionService, never()).deleteById(anyInt());
    }
}
