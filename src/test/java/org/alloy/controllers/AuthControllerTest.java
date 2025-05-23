package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.security.AccountLockedException;
import org.alloy.security.AuthenticationService;
import org.alloy.security.PasswordValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.alloy.TestConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для AuthController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 */
@WebMvcTest(AuthController.class)
@Import(TestConfig.class)
@WithMockUser
class AuthControllerTest {

    // MockMvc - основной инструмент для тестирования веб-слоя
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper для сериализации/десериализации JSON
    @Autowired
    private ObjectMapper objectMapper;

    // Мокаем AuthenticationService, так как нам не нужна реальная аутентификация
    @MockBean
    private AuthenticationService authenticationService;

    // Тестовые данные, которые будут использоваться в тестах
    private AuthController.LoginRequest loginRequest;
    private AuthController.PasswordValidationRequest passwordRequest;
    private AuthenticationService.AuthenticationResponse authResponse;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый запрос на вход
        loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("testpass");

        // Создаем тестовый запрос на валидацию пароля
        passwordRequest = new AuthController.PasswordValidationRequest();
        passwordRequest.setPassword("TestPass123!");

        // Создаем тестовый ответ аутентификации
        authResponse = new AuthenticationService.AuthenticationResponse("test-token", "test-session");
    }

    /**
     * Тест успешной аутентификации пользователя
     */
    @Test
    void authenticateUser_Success() throws Exception {
        // Настраиваем мок для возврата успешного ответа
        when(authenticationService.authenticate(anyString(), anyString(), any(HttpServletRequest.class)))
                .thenReturn(authResponse);

        // Выполняем POST запрос на /api/auth/login
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                // Проверяем статус ответа и содержимое JSON
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.sessionId").value("test-session"));

        // Проверяем, что сервис был вызван с правильными параметрами
        verify(authenticationService).authenticate(loginRequest.getUsername(), loginRequest.getPassword(), any(HttpServletRequest.class));
    }

    /**
     * Тест аутентификации с заблокированным аккаунтом
     */
    @Test
    void authenticateUser_AccountLocked() throws Exception {
        // Настраиваем мок для выброса исключения заблокированного аккаунта
        when(authenticationService.authenticate(anyString(), anyString(), any(HttpServletRequest.class)))
                .thenThrow(new AccountLockedException("Account is locked"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Account Locked"));

        verify(authenticationService).authenticate(loginRequest.getUsername(), loginRequest.getPassword(), any(HttpServletRequest.class));
    }

    /**
     * Тест аутентификации с неверными учетными данными
     */
    @Test
    void authenticateUser_AuthenticationFailed() throws Exception {
        // Настраиваем мок для выброса исключения неверных учетных данных
        when(authenticationService.authenticate(anyString(), anyString(), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication Failed"));

        verify(authenticationService).authenticate(loginRequest.getUsername(), loginRequest.getPassword(), any(HttpServletRequest.class));
    }

    /**
     * Тест успешного выхода из системы
     */
    @Test
    void logoutUser_Success() throws Exception {
        // Настраиваем мок для успешного выхода
        doNothing().when(authenticationService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authenticationService).logout("test-token");
    }

    /**
     * Тест ошибки при выходе из системы
     */
    @Test
    void logoutUser_Failure() throws Exception {
        // Настраиваем мок для выброса исключения при выходе
        doThrow(new RuntimeException("Logout failed")).when(authenticationService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Logout failed"));

        verify(authenticationService).logout("test-token");
    }

    /**
     * Тест успешной валидации пароля
     */
    @Test
    void validatePassword_Success() throws Exception {
        // Настраиваем мок для успешной валидации
        doNothing().when(authenticationService).validatePassword(anyString());

        mockMvc.perform(post("/api/auth/validate-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password is valid"));

        verify(authenticationService).validatePassword(passwordRequest.getPassword());
    }

    /**
     * Тест ошибки валидации пароля
     */
    @Test
    void validatePassword_ValidationFailed() throws Exception {
        // Создаем список ошибок валидации
        List<String> validationErrors = Arrays.asList(
            "Password must be at least 8 characters",
            "Password must contain at least one uppercase letter"
        );

        // Настраиваем мок для выброса исключения валидации
        PasswordValidationException exception = new PasswordValidationException("Invalid password", validationErrors);
        doThrow(exception).when(authenticationService).validatePassword(anyString());

        mockMvc.perform(post("/api/auth/validate-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password Validation Failed"))
                .andExpect(jsonPath("$.validationErrors").exists());

        verify(authenticationService).validatePassword(passwordRequest.getPassword());
    }
}
