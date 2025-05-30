package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.UserToken;
import org.alloy.services.UserTokenService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для UserTokenController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(UserTokenController.class)
@Import(TestConfig.class)
@WithMockUser
public class UserTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserTokenService userTokenService;

    private UserToken testUserToken;
    private List<UserToken> testUserTokens;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый токен
        testUserToken = new UserToken();
        testUserToken.setId(1);
        testUserToken.setUserAccountId(1);
        testUserToken.setDateCreated(LocalDateTime.now());
        testUserToken.setDateExpired(LocalDateTime.now().plusDays(1));
        testUserToken.setToken(UUID.randomUUID());
        testUserToken.setType("AUTH");
        testUserToken.setUsed(false);
        testUserToken.setDateUsed(null);

        // Создаем второй тестовый токен
        UserToken secondToken = new UserToken();
        secondToken.setId(2);
        secondToken.setUserAccountId(1);
        secondToken.setDateCreated(LocalDateTime.now());
        secondToken.setDateExpired(LocalDateTime.now().plusDays(1));
        secondToken.setToken(UUID.randomUUID());
        secondToken.setType("RESET_PASSWORD");
        secondToken.setUsed(true);
        secondToken.setDateUsed(LocalDateTime.now());

        // Создаем список тестовых токенов
        testUserTokens = Arrays.asList(testUserToken, secondToken);
    }

    /**
     * Тест получения всех токенов
     */
    @Test
    void getAllTokens_ShouldReturnListOfAllTokens() throws Exception {
        when(userTokenService.getAllUserTokens()).thenReturn(testUserTokens);

        mockMvc.perform(get("/api/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("AUTH"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].type").value("RESET_PASSWORD"));

        verify(userTokenService).getAllUserTokens();
    }

    /**
     * Тест получения токена по ID
     */
    @Test
    void getTokenById_WhenTokenExists_ShouldReturnToken() throws Exception {
        when(userTokenService.getUserTokenById(1)).thenReturn(Optional.of(testUserToken));

        mockMvc.perform(get("/api/tokens/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("AUTH"));

        verify(userTokenService).getUserTokenById(1);
    }

    /**
     * Тест получения несуществующего токена по ID
     */
    @Test
    void getTokenById_WhenTokenDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userTokenService.getUserTokenById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tokens/999"))
                .andExpect(status().isNotFound());

        verify(userTokenService).getUserTokenById(999);
    }

    /**
     * Тест получения токенов по ID пользователя
     */
    @Test
    void getTokensByUserId_ShouldReturnListOfUserTokens() throws Exception {
        when(userTokenService.getUserTokensByUserId(1)).thenReturn(testUserTokens);

        mockMvc.perform(get("/api/tokens/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userAccountId").value(1))
                .andExpect(jsonPath("$[1].userAccountId").value(1));

        verify(userTokenService).getUserTokensByUserId(1);
    }

    /**
     * Тест получения токена по строке токена
     */
    @Test
    void getTokenByTokenString_WhenTokenExists_ShouldReturnToken() throws Exception {
        UUID token = testUserToken.getToken();
        when(userTokenService.getUserTokenByToken(token)).thenReturn(Optional.of(testUserToken));

        mockMvc.perform(get("/api/tokens/token/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token.toString()));

        verify(userTokenService).getUserTokenByToken(token);
    }

    /**
     * Тест получения несуществующего токена по строке токена
     */
    @Test
    void getTokenByTokenString_WhenTokenDoesNotExist_ShouldReturnNotFound() throws Exception {
        UUID nonExistentToken = UUID.randomUUID();
        when(userTokenService.getUserTokenByToken(nonExistentToken)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tokens/token/" + nonExistentToken))
                .andExpect(status().isNotFound());

        verify(userTokenService).getUserTokenByToken(nonExistentToken);
    }

    /**
     * Тест создания нового токена
     */
    @Test
    void createToken_ShouldCreateToken() throws Exception {
        when(userTokenService.createUserToken(any(UserToken.class))).thenReturn(testUserToken);

        mockMvc.perform(post("/api/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("AUTH"));

        verify(userTokenService).createUserToken(any(UserToken.class));
    }

    /**
     * Тест создания токена с невалидными данными
     */
    @Test
    void createToken_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        when(userTokenService.createUserToken(any(UserToken.class)))
                .thenThrow(new IllegalArgumentException("Invalid token data"));

        mockMvc.perform(post("/api/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserToken)))
                .andExpect(status().isBadRequest());

        verify(userTokenService).createUserToken(any(UserToken.class));
    }

    /**
     * Тест обновления существующего токена
     */
    @Test
    void updateToken_WhenTokenExists_ShouldUpdateToken() throws Exception {
        when(userTokenService.updateUserToken(any(UserToken.class))).thenReturn(testUserToken);

        mockMvc.perform(put("/api/tokens/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("AUTH"));

        verify(userTokenService).updateUserToken(any(UserToken.class));
    }

    /**
     * Тест обновления токена с невалидными данными
     */
    @Test
    void updateToken_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        when(userTokenService.updateUserToken(any(UserToken.class)))
                .thenThrow(new IllegalArgumentException("Invalid token data"));

        mockMvc.perform(put("/api/tokens/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserToken)))
                .andExpect(status().isBadRequest());

        verify(userTokenService).updateUserToken(any(UserToken.class));
    }

    /**
     * Тест удаления существующего токена
     */
    @Test
    void deleteToken_WhenTokenExists_ShouldDeleteToken() throws Exception {
        doNothing().when(userTokenService).deleteUserToken(1);

        mockMvc.perform(delete("/api/tokens/1"))
                .andExpect(status().isOk());

        verify(userTokenService).deleteUserToken(1);
    }

    /**
     * Тест удаления несуществующего токена
     */
    @Test
    void deleteToken_WhenTokenDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Token not found"))
                .when(userTokenService).deleteUserToken(999);

        mockMvc.perform(delete("/api/tokens/999"))
                .andExpect(status().isNotFound());

        verify(userTokenService).deleteUserToken(999);
    }

    /**
     * Тест удаления всех токенов пользователя
     */
    @Test
    void deleteAllUserTokens_ShouldDeleteAllUserTokens() throws Exception {
        doNothing().when(userTokenService).deleteAllUserTokens(1);

        mockMvc.perform(delete("/api/tokens/user/1"))
                .andExpect(status().isOk());

        verify(userTokenService).deleteAllUserTokens(1);
    }

    /**
     * Тест очистки истекших токенов
     */
    @Test
    void cleanupExpiredTokens_ShouldDeleteExpiredTokens() throws Exception {
        doNothing().when(userTokenService).deleteExpiredTokens();

        mockMvc.perform(delete("/api/tokens/cleanup/expired"))
                .andExpect(status().isOk());

        verify(userTokenService).deleteExpiredTokens();
    }
}
