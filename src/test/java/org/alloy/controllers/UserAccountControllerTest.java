package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.UserRepository;
import org.alloy.repositories.UserRoleRepository;
import org.alloy.security.SessionManagementService;
import org.alloy.services.EmailVerificationService;
import org.alloy.services.UserAccountService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для UserAccountController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(UserAccountController.class)
@Import(TestConfig.class)
@WithMockUser
public class UserAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserAccountService userAccountService;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private Wt2AccessService wt2AccessService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private SessionManagementService sessionManagementService;

    private UserAccount testUserAccount;
    private List<UserAccount> testUserAccounts;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUserAccount = new UserAccount();
        testUserAccount.setId(1);
        testUserAccount.setUserName("testuser");
        testUserAccount.setName("Test User");
        testUserAccount.setEmail("test@example.com");
        testUserAccount.setOrganizationUnitId(1);
        testUserAccount.setUserRoleId(1);
        testUserAccount.setStatus(GeneralStatus.Active);
        testUserAccount.setDateCreated(LocalDateTime.now());
        testUserAccount.setPasswordHash("password".getBytes());

        // Создаем второго тестового пользователя
        UserAccount secondUser = new UserAccount();
        secondUser.setId(2);
        secondUser.setUserName("seconduser");
        secondUser.setName("Second User");
        secondUser.setEmail("second@example.com");
        secondUser.setOrganizationUnitId(1);
        secondUser.setUserRoleId(2);
        secondUser.setStatus(GeneralStatus.Active);
        secondUser.setDateCreated(LocalDateTime.now());
        secondUser.setPasswordHash("password".getBytes());

        // Создаем список тестовых пользователей
        testUserAccounts = Arrays.asList(testUserAccount, secondUser);
    }

    /**
     * Тест получения всех пользователей
     */
    @Test
    void getAllUserAccounts_ShouldReturnListOfAllUsers() throws Exception {
        when(userAccountService.getAllUserAccounts()).thenReturn(testUserAccounts);

        mockMvc.perform(get("/user-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userName").value("testuser"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].userName").value("seconduser"));

        verify(userAccountService).getAllUserAccounts();
    }

    /**
     * Тест получения пользователя по ID
     */
    @Test
    void getUserAccountById_WhenUserExists_ShouldReturnUser() throws Exception {
        when(userAccountService.getUserAccountById(1)).thenReturn(Optional.of(testUserAccount));

        mockMvc.perform(get("/user-accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userAccountService).getUserAccountById(1);
    }

    /**
     * Тест получения несуществующего пользователя по ID
     */
    @Test
    void getUserAccountById_WhenUserDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userAccountService.getUserAccountById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/user-accounts/999"))
                .andExpect(status().isNotFound());

        verify(userAccountService).getUserAccountById(999);
    }

    /**
     * Тест получения пользователя по имени пользователя
     */
    @Test
    void getUserAccountByUserName_WhenUserExists_ShouldReturnUser() throws Exception {
        when(userAccountService.getUserAccountByUserName("testuser")).thenReturn(Optional.of(testUserAccount));

        mockMvc.perform(get("/user-accounts/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("testuser"));

        verify(userAccountService).getUserAccountByUserName("testuser");
    }

    /**
     * Тест получения пользователя по email
     */
    @Test
    void getUserAccountByEmail_WhenUserExists_ShouldReturnUser() throws Exception {
        when(userAccountService.getUserAccountByEmail("test@example.com")).thenReturn(Optional.of(testUserAccount));

        mockMvc.perform(get("/user-accounts/email/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userAccountService).getUserAccountByEmail("test@example.com");
    }

    /**
     * Тест получения пользователей по ID подразделения
     */
    @Test
    void getUserAccountsByOrganizationUnitId_ShouldReturnUsers() throws Exception {
        when(userAccountService.getUserAccountsByOrganizationUnitId(1)).thenReturn(testUserAccounts);

        mockMvc.perform(get("/user-accounts/organization-unit/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationUnitId").value(1))
                .andExpect(jsonPath("$[1].organizationUnitId").value(1));

        verify(userAccountService).getUserAccountsByOrganizationUnitId(1);
    }

    /**
     * Тест получения пользователей по ID роли
     */
    @Test
    void getUserAccountsByUserRoleId_ShouldReturnUsers() throws Exception {
        when(userAccountService.getUserAccountsByUserRoleId(1)).thenReturn(List.of(testUserAccount));

        mockMvc.perform(get("/user-accounts/user-role/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userRoleId").value(1));

        verify(userAccountService).getUserAccountsByUserRoleId(1);
    }

    /**
     * Тест поиска пользователей
     */
    @Test
    void searchUserAccounts_ShouldReturnMatchingUsers() throws Exception {
        when(userAccountService.searchUserAccounts(1, "test")).thenReturn(List.of(testUserAccount));

        mockMvc.perform(get("/user-accounts/search")
                .param("organizationUnitId", "1")
                .param("searchTerm", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("testuser"));

        verify(userAccountService).searchUserAccounts(1, "test");
    }

    /**
     * Тест создания нового пользователя
     */
    @Test
    void createUserAccount_ShouldCreateUser() throws Exception {
        when(userAccountService.createUserAccount(any(UserAccount.class))).thenReturn(testUserAccount);

        mockMvc.perform(post("/user-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("testuser"));

        verify(userAccountService).createUserAccount(any(UserAccount.class));
    }

    /**
     * Тест обновления существующего пользователя
     */
    @Test
    void updateUserAccount_WhenUserExists_ShouldUpdateUser() throws Exception {
        when(userAccountService.updateUserAccount(any(UserAccount.class))).thenReturn(testUserAccount);

        mockMvc.perform(put("/user-accounts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserAccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("testuser"));

        verify(userAccountService).updateUserAccount(any(UserAccount.class));
    }

    /**
     * Тест удаления пользователя
     */
    @Test
    void deleteUserAccount_WhenUserExists_ShouldDeleteUser() throws Exception {
        doNothing().when(userAccountService).deleteUserAccount(1);

        mockMvc.perform(delete("/user-accounts/1"))
                .andExpect(status().isNoContent());

        verify(userAccountService).deleteUserAccount(1);
    }

    /**
     * Тест жесткого удаления пользователя
     */
    @Test
    void hardDeleteUserAccount_WhenUserExists_ShouldHardDeleteUser() throws Exception {
        doNothing().when(userAccountService).hardDeleteUserAccount(1);

        mockMvc.perform(delete("/user-accounts/1/hard"))
                .andExpect(status().isNoContent());

        verify(userAccountService).hardDeleteUserAccount(1);
    }

    /**
     * Тест аутентификации пользователя
     */
    @Test
    void authenticateUser_WithValidCredentials_ShouldReturnUser() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("userName", "testuser");
        credentials.put("password", "password");

        when(userAccountService.authenticateUser("testuser", "password"))
                .thenReturn(Optional.of(testUserAccount));

        mockMvc.perform(post("/user-accounts/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("testuser"));

        verify(userAccountService).authenticateUser("testuser", "password");
    }

    /**
     * Тест аутентификации с неверными учетными данными
     */
    @Test
    void authenticateUser_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("userName", "testuser");
        credentials.put("password", "wrongpassword");

        when(userAccountService.authenticateUser("testuser", "wrongpassword"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/user-accounts/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isUnauthorized());

        verify(userAccountService).authenticateUser("testuser", "wrongpassword");
    }
}
