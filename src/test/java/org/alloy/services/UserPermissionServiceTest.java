package org.alloy.services;

import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.UserPermission;
import org.alloy.repositories.UserPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = UserPermissionService.class)
@ActiveProfiles("test")
public class UserPermissionServiceTest {

    @MockBean
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserPermissionService userPermissionService;

    private UserPermission testUserPermission;

    @BeforeEach
    void setUp() {
        // Создаем тестовое разрешение с минимальным набором полей
        testUserPermission = new UserPermission();
        testUserPermission.setId(1);
        testUserPermission.setName("TEST_PERMISSION");
    }

    /**
     * Тест получения всех разрешений
     * Проверяет, что сервис возвращает список разрешений, полученный из репозитория
     */
    @Test
    void findAll_ShouldReturnAllPermissions() {
        List<UserPermission> permissions = Arrays.asList(testUserPermission);
        when(userPermissionRepository.findAll()).thenReturn(permissions);

        List<UserPermission> result = userPermissionService.findAll();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserPermission.getName(), result.get(0).getName());
        verify(userPermissionRepository, times(1)).findAll();
    }

    /**
     * Тест получения разрешения по ID (существующее)
     */
    @Test
    void findById_WhenExists_ShouldReturnPermission() {
        when(userPermissionRepository.findById(1)).thenReturn(Optional.of(testUserPermission));
        Optional<UserPermission> result = userPermissionService.findById(1);
        assertTrue(result.isPresent());
        assertEquals(testUserPermission.getId(), result.get().getId());
        verify(userPermissionRepository, times(1)).findById(1);
    }

    /**
     * Тест получения разрешения по ID (не существует)
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        when(userPermissionRepository.findById(999)).thenReturn(Optional.empty());
        Optional<UserPermission> result = userPermissionService.findById(999);
        assertFalse(result.isPresent());
        verify(userPermissionRepository, times(1)).findById(999);
    }
}
