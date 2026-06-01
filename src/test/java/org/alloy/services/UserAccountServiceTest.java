package org.alloy.services;

import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = UserAccountService.class)
@ActiveProfiles("test")
public class UserAccountServiceTest {

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserAccountService userAccountService;

    private UserAccount testUserAccount;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя с минимальным набором полей
        testUserAccount = new UserAccount();
        testUserAccount.setId(1);
        testUserAccount.setUserName("testuser");
        testUserAccount.setEmail("test@example.com");
        testUserAccount.setUserRoleId(2);
        testUserAccount.setOrganizationUnitId(3);
        testUserAccount.setStatus(org.alloy.models.GeneralStatus.Active);
        testUserAccount.setDateCreated(LocalDateTime.now());
        testUserAccount.setPasswordHash("password".getBytes());
    }

    /**
     * Тест получения всех пользователей
     * Проверяет, что сервис возвращает список пользователей, полученный из репозитория
     */
    @Test
    void getAllUserAccounts_ShouldReturnAllUsers() {
        List<UserAccount> users = Arrays.asList(testUserAccount);
        when(userAccountRepository.findAll()).thenReturn(users);

        List<UserAccount> result = userAccountService.getAllUserAccounts();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserAccount.getUserName(), result.get(0).getUserName());
        verify(userAccountRepository, times(1)).findAll();
    }

    /**
     * Тест получения пользователя по ID (существующий)
     */
    @Test
    void getUserAccountById_WhenExists_ShouldReturnUser() {
        when(userAccountRepository.findById(1)).thenReturn(Optional.of(testUserAccount));
        Optional<UserAccount> result = userAccountService.getUserAccountById(1);
        assertTrue(result.isPresent());
        assertEquals(testUserAccount.getId(), result.get().getId());
        verify(userAccountRepository, times(1)).findById(1);
    }

    /**
     * Тест получения пользователя по ID (не существует)
     */
    @Test
    void getUserAccountById_WhenNotExists_ShouldReturnEmpty() {
        when(userAccountRepository.findById(999)).thenReturn(Optional.empty());
        Optional<UserAccount> result = userAccountService.getUserAccountById(999);
        assertFalse(result.isPresent());
        verify(userAccountRepository, times(1)).findById(999);
    }

    /**
     * Тест получения пользователя по userName
     */
    @Test
    void getUserAccountByUserName_ShouldReturnUser() {
        when(userAccountRepository.findByUserName("testuser")).thenReturn(Optional.of(testUserAccount));
        Optional<UserAccount> result = userAccountService.getUserAccountByUserName("testuser");
        assertTrue(result.isPresent());
        assertEquals(testUserAccount.getUserName(), result.get().getUserName());
        verify(userAccountRepository, times(1)).findByUserName("testuser");
    }

    /**
     * Тест получения пользователя по email
     */
    @Test
    void getUserAccountByEmail_ShouldReturnUser() {
        when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUserAccount));
        Optional<UserAccount> result = userAccountService.getUserAccountByEmail("test@example.com");
        assertTrue(result.isPresent());
        assertEquals(testUserAccount.getEmail(), result.get().getEmail());
        verify(userAccountRepository, times(1)).findByEmail("test@example.com");
    }

    /**
     * Тест получения пользователей по organizationUnitId
     */
    @Test
    void getUserAccountsByOrganizationUnitId_ShouldReturnUsers() {
        List<UserAccount> users = Arrays.asList(testUserAccount);
        when(userAccountRepository.findByOrganizationUnitId(3)).thenReturn(users);
        List<UserAccount> result = userAccountService.getUserAccountsByOrganizationUnitId(3);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserAccount.getOrganizationUnitId(), result.get(0).getOrganizationUnitId());
        verify(userAccountRepository, times(1)).findByOrganizationUnitId(3);
    }

    /**
     * Тест получения пользователей по userRoleId
     */
    @Test
    void getUserAccountsByUserRoleId_ShouldReturnUsers() {
        List<UserAccount> users = Arrays.asList(testUserAccount);
        when(userAccountRepository.findByUserRoleId(2)).thenReturn(users);
        List<UserAccount> result = userAccountService.getUserAccountsByUserRoleId(2);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserAccount.getUserRoleId(), result.get(0).getUserRoleId());
        verify(userAccountRepository, times(1)).findByUserRoleId(2);
    }

    /**
     * Тест поиска пользователей по organizationUnitId и поисковому термину
     */
    @Test
    void searchUserAccounts_ShouldReturnUsers() {
        List<UserAccount> users = Arrays.asList(testUserAccount);
        when(userAccountRepository.searchUserAccounts(3, "test")).thenReturn(users);
        List<UserAccount> result = userAccountService.searchUserAccounts(3, "test");
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userAccountRepository, times(1)).searchUserAccounts(3, "test");
    }
}
