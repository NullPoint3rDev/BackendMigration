package org.alloy.services;

import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.UserRolePermission;
import org.alloy.repositories.UserRolePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = UserRolePermissionService.class)
@ActiveProfiles("test")
public class UserRolePermissionServiceTest {

    @MockBean
    private UserRolePermissionRepository userRolePermissionRepository;

    @Autowired
    private UserRolePermissionService userRolePermissionService;

    private UserRolePermission testUserRolePermission;

    @BeforeEach
    void setUp() {
        // Создаем тестовый объект UserRolePermission
        testUserRolePermission = new UserRolePermission();
        testUserRolePermission.setId(1);
        testUserRolePermission.setUserRoleId(10);
        testUserRolePermission.setUserPermissionId(20);
        testUserRolePermission.setRead(true);
        testUserRolePermission.setWrite(false);
    }

    /**
     * Тест метода findAll()
     * Проверяет корректность получения всех связей ролей и разрешений
     */
    @Test
    void findAll_ShouldReturnAllUserRolePermissions() {
        // Подготавливаем тестовые данные
        List<UserRolePermission> expectedPermissions = Arrays.asList(testUserRolePermission);
        when(userRolePermissionRepository.findAll()).thenReturn(expectedPermissions);

        // Вызываем тестируемый метод
        List<UserRolePermission> result = userRolePermissionService.findAll();

        // Проверяем результаты
        assertNotNull(result, "Список разрешений не должен быть null");
        assertEquals(expectedPermissions.size(), result.size(), "Размер списка должен совпадать");
        assertEquals(testUserRolePermission.getId(), result.get(0).getId(), "ID должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(userRolePermissionRepository, times(1)).findAll();
    }

    /**
     * Тест метода findById() - случай когда объект существует
     * Проверяет корректность получения связи роли и разрешения по ID
     */
    @Test
    void findById_WhenExists_ShouldReturnUserRolePermission() {
        // Подготавливаем тестовые данные
        when(userRolePermissionRepository.findById(1)).thenReturn(Optional.of(testUserRolePermission));

        // Вызываем тестируемый метод
        Optional<UserRolePermission> result = userRolePermissionService.findById(1);

        // Проверяем результаты
        assertTrue(result.isPresent(), "Результат должен содержать объект");
        assertEquals(testUserRolePermission.getId(), result.get().getId(), "ID должен совпадать");
        assertEquals(testUserRolePermission.getUserRoleId(), result.get().getUserRoleId(), "ID роли должен совпадать");
        assertEquals(testUserRolePermission.getUserPermissionId(), result.get().getUserPermissionId(), "ID разрешения должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(userRolePermissionRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() - случай когда объект не существует
     * Проверяет корректность обработки ситуации, когда связь не найдена
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготавливаем тестовые данные
        when(userRolePermissionRepository.findById(999)).thenReturn(Optional.empty());

        // Вызываем тестируемый метод
        Optional<UserRolePermission> result = userRolePermissionService.findById(999);

        // Проверяем результаты
        assertFalse(result.isPresent(), "Результат должен быть пустым");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(userRolePermissionRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения новой связи роли и разрешения
     */
    @Test
    void save_ShouldReturnSavedUserRolePermission() {
        // Подготавливаем тестовые данные
        when(userRolePermissionRepository.save(any(UserRolePermission.class))).thenReturn(testUserRolePermission);

        // Вызываем тестируемый метод
        UserRolePermission result = userRolePermissionService.save(testUserRolePermission);

        // Проверяем результаты
        assertNotNull(result, "Сохраненный объект не должен быть null");
        assertEquals(testUserRolePermission.getId(), result.getId(), "ID должен совпадать");
        assertEquals(testUserRolePermission.getUserRoleId(), result.getUserRoleId(), "ID роли должен совпадать");
        assertEquals(testUserRolePermission.getUserPermissionId(), result.getUserPermissionId(), "ID разрешения должен совпадать");
        
        // Проверяем, что метод репозитория был вызван ровно один раз
        verify(userRolePermissionRepository, times(1)).save(testUserRolePermission);
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления связи роли и разрешения
     */
    @Test
    void deleteById_ShouldCallRepositoryDelete() {
        // Вызываем тестируемый метод
        userRolePermissionService.deleteById(1);

        // Проверяем, что метод репозитория был вызван ровно один раз с правильным ID
        verify(userRolePermissionRepository, times(1)).deleteById(1);
    }
}
