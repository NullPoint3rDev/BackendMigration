package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.UserPermission;
import org.alloy.models.entities.UserRole;
import org.alloy.models.entities.UserRolePermission;
import org.alloy.services.UserRolePermissionService;
import org.alloy.services.UserRolePermissionServiceTest;
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
 * Тесты для UserRolePermissionController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(UserRolePermissionController.class)
@Import(TestConfig.class)
@WithMockUser
public class UserRolePermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRolePermissionService userRolePermissionService;

    private UserRolePermission testUserRolePermission;
    private List<UserRolePermission> testUserRolePermissions;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую роль
        UserRole userRole = new UserRole();
        userRole.setId(1);
        userRole.setName("ADMIN");

        // Создаем тестовое разрешение
        UserPermission userPermission = new UserPermission();
        userPermission.setId(1);
        userPermission.setName("READ_USERS");

        // Создаем тестовую связь роли и разрешения
        testUserRolePermission = new UserRolePermission();
        testUserRolePermission.setId(1);
        testUserRolePermission.setUserRole(userRole);
        testUserRolePermission.setUserPermission(userPermission);
        testUserRolePermission.setRead(true);
        testUserRolePermission.setWrite(true);

        // Создаем вторую тестовую связь роли и разрешения
        UserRolePermission secondUserRolePermission = new UserRolePermission();
        secondUserRolePermission.setId(2);
        secondUserRolePermission.setUserRole(userRole);
        secondUserRolePermission.setUserPermission(userPermission);
        secondUserRolePermission.setRead(true);
        secondUserRolePermission.setWrite(false);

        // Создаем список тестовых связей ролей и разрешений
        testUserRolePermissions = Arrays.asList(testUserRolePermission, secondUserRolePermission);
    }

    /**
     * Тест получения всех связей ролей и разрешений
     */
    @Test
    void getAll_ShouldReturnListOfAllRolePermissions() throws Exception {
        when(userRolePermissionService.findAll()).thenReturn(testUserRolePermissions);

        mockMvc.perform(get("/user-role-permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(userRolePermissionService).findAll();
    }

    /**
     * Тест получения связи роли и разрешения по ID
     */
    @Test
    void getById_WhenRolePermissionExists_ShouldReturnRolePermission() throws Exception {
        when(userRolePermissionService.findById(1)).thenReturn(Optional.of(testUserRolePermission));

        mockMvc.perform(get("/user-role-permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userRolePermissionService).findById(1);
    }

    /**
     * Тест получения несуществующей связи роли и разрешения по ID
     */
    @Test
    void getById_WhenRolePermissionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userRolePermissionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/user-role-permissions/999"))
                .andExpect(status().isNotFound());

        verify(userRolePermissionService).findById(999);
    }

    /**
     * Тест создания новой связи роли и разрешения
     */
    @Test
    void create_ShouldCreateRolePermission() throws Exception {
        when(userRolePermissionService.save(any(UserRolePermission.class))).thenReturn(testUserRolePermission);

        mockMvc.perform(post("/user-role-permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserRolePermission)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(userRolePermissionService).save(any(UserRolePermission.class));
    }

    /**
     * Тест обновления существующей связи роли и разрешения
     */
    @Test
    void update_WhenRolePermissionExists_ShouldUpdateRolePermission() throws Exception {
        when(userRolePermissionService.save(any(UserRolePermission.class))).thenReturn(testUserRolePermission);

        mockMvc.perform(put("/user-role-permissions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserRolePermission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userRolePermissionService).save(any(UserRolePermission.class));
    }

    /**
     * Тест обновления несуществующей связи роли и разрешения
     */
    @Test
    void update_WhenRolePermissionDoesNotExist_ShouldStillSave() throws Exception {
        when(userRolePermissionService.save(any(UserRolePermission.class))).thenReturn(testUserRolePermission);

        mockMvc.perform(put("/user-role-permissions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserRolePermission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userRolePermissionService).save(any(UserRolePermission.class));
    }

    /**
     * Тест удаления существующей связи роли и разрешения
     */
    @Test
    void delete_WhenRolePermissionExists_ShouldDeleteRolePermission() throws Exception {
        when(userRolePermissionService.findById(1)).thenReturn(Optional.of(testUserRolePermission));
        doNothing().when(userRolePermissionService).deleteById(1);

        mockMvc.perform(delete("/user-role-permissions/1"))
                .andExpect(status().isNoContent());

        verify(userRolePermissionService).findById(1);
        verify(userRolePermissionService).deleteById(1);
    }

    /**
     * Тест удаления несуществующей связи роли и разрешения
     */
    @Test
    void delete_WhenRolePermissionDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userRolePermissionService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/user-role-permissions/999"))
                .andExpect(status().isNotFound());

        verify(userRolePermissionService).findById(999);
        verify(userRolePermissionService, never()).deleteById(anyInt());
    }
}
