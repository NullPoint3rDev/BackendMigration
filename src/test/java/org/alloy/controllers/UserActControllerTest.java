package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.AlloyWebMvcTest;
import org.alloy.models.entities.UserAct;
import org.alloy.services.UserActService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для UserActController.
 * Использует @AlloyWebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(MvcTestConfig.class) импортирует конфигурацию для тестов.
 */
@AlloyWebMvcTest(UserActController.class)
@WithMockUser
public class UserActControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserActService userActService;

    private UserAct testUserAct;
    private List<UserAct> testUserActs;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовое действие пользователя
        testUserAct = new UserAct();
        testUserAct.setId(1);
        testUserAct.setUserAccountId(1);
        testUserAct.setType("LOGIN");
        testUserAct.setDescription("User logged in");
        testUserAct.setIpAddress("127.0.0.1");
        testUserAct.setUserAgent("Mozilla/5.0");
        testUserAct.setDateCreated(LocalDateTime.now());

        // Создаем второе тестовое действие
        UserAct secondUserAct = new UserAct();
        secondUserAct.setId(2);
        secondUserAct.setUserAccountId(1);
        secondUserAct.setType("LOGOUT");
        secondUserAct.setDescription("User logged out");
        secondUserAct.setIpAddress("127.0.0.1");
        secondUserAct.setUserAgent("Mozilla/5.0");
        secondUserAct.setDateCreated(LocalDateTime.now());

        // Создаем список тестовых действий
        testUserActs = Arrays.asList(testUserAct, secondUserAct);
    }

    /**
     * Тест получения всех действий пользователей
     */
    @Test
    void getAllUserActs_ShouldReturnListOfAllActs() throws Exception {
        when(userActService.getAllUserActs()).thenReturn(testUserActs);

        mockMvc.perform(get("/user-acts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("LOGIN"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].type").value("LOGOUT"));

        verify(userActService).getAllUserActs();
    }

    /**
     * Тест получения действия по ID
     */
    @Test
    void getUserActById_WhenActExists_ShouldReturnAct() throws Exception {
        when(userActService.getUserActById(1)).thenReturn(Optional.of(testUserAct));

        mockMvc.perform(get("/user-acts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("LOGIN"));

        verify(userActService).getUserActById(1);
    }

    /**
     * Тест получения несуществующего действия по ID
     */
    @Test
    void getUserActById_WhenActDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userActService.getUserActById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/user-acts/999"))
                .andExpect(status().isNotFound());

        verify(userActService).getUserActById(999);
    }

    /**
     * Тест получения действий по ID пользователя
     */
    @Test
    void getUserActsByUserId_ShouldReturnUserActs() throws Exception {
        when(userActService.getUserActsByUserId(1)).thenReturn(testUserActs);

        mockMvc.perform(get("/user-acts/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userAccountId").value(1))
                .andExpect(jsonPath("$[1].userAccountId").value(1));

        verify(userActService).getUserActsByUserId(1);
    }

    /**
     * Тест получения действий по ID пользователя и типу
     */
    @Test
    void getUserActsByUserIdAndType_ShouldReturnFilteredActs() throws Exception {
        when(userActService.getUserActsByUserIdAndType(1, "LOGIN")).thenReturn(List.of(testUserAct));

        mockMvc.perform(get("/user-acts/user/1/type/LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("LOGIN"));

        verify(userActService).getUserActsByUserIdAndType(1, "LOGIN");
    }

    /**
     * Тест получения действий по ID пользователя и диапазону дат
     */
    @Test
    void getUserActsByUserIdAndDateRange_ShouldReturnFilteredActs() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        when(userActService.getUserActsByUserIdAndDateRange(1, startDate, endDate))
                .thenReturn(testUserActs);

        mockMvc.perform(get("/user-acts/user/1/date-range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userAccountId").value(1));

        verify(userActService).getUserActsByUserIdAndDateRange(1, startDate, endDate);
    }

    /**
     * Тест получения действий по ID пользователя, типу и диапазону дат
     */
    @Test
    void getUserActsByUserIdAndTypeAndDateRange_ShouldReturnFilteredActs() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        when(userActService.getUserActsByUserIdAndTypeAndDateRange(1, "LOGIN", startDate, endDate))
                .thenReturn(List.of(testUserAct));

        mockMvc.perform(get("/user-acts/user/1/type/LOGIN/date-range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("LOGIN"));

        verify(userActService).getUserActsByUserIdAndTypeAndDateRange(1, "LOGIN", startDate, endDate);
    }

    /**
     * Тест подсчета действий по ID пользователя, типу и диапазону дат
     */
    @Test
    void countUserActsByUserIdAndTypeAndDateRange_ShouldReturnCount() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        when(userActService.countUserActsByUserIdAndTypeAndDateRange(1, "LOGIN", startDate, endDate))
                .thenReturn(1L);

        mockMvc.perform(get("/user-acts/user/1/type/LOGIN/count")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        verify(userActService).countUserActsByUserIdAndTypeAndDateRange(1, "LOGIN", startDate, endDate);
    }

    /**
     * Тест создания нового действия
     */
    @Test
    void createUserAct_ShouldCreateAct() throws Exception {
        when(userActService.createUserAct(any(UserAct.class))).thenReturn(testUserAct);

        mockMvc.perform(post("/user-acts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserAct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("LOGIN"));

        verify(userActService).createUserAct(any(UserAct.class));
    }

    /**
     * Тест обновления существующего действия
     */
    @Test
    void updateUserAct_WhenActExists_ShouldUpdateAct() throws Exception {
        when(userActService.updateUserAct(any(UserAct.class))).thenReturn(testUserAct);

        mockMvc.perform(put("/user-acts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUserAct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("LOGIN"));

        verify(userActService).updateUserAct(any(UserAct.class));
    }

    /**
     * Тест удаления действия
     */
    @Test
    void deleteUserAct_WhenActExists_ShouldDeleteAct() throws Exception {
        doNothing().when(userActService).deleteUserAct(1);

        mockMvc.perform(delete("/user-acts/1"))
                .andExpect(status().isOk());

        verify(userActService).deleteUserAct(1);
    }

    /**
     * Тест удаления всех действий пользователя
     */
    @Test
    void deleteAllUserActs_ShouldDeleteAllUserActs() throws Exception {
        doNothing().when(userActService).deleteAllUserActs(1);

        mockMvc.perform(delete("/user-acts/user/1"))
                .andExpect(status().isOk());

        verify(userActService).deleteAllUserActs(1);
    }

    /**
     * Тест очистки старых действий
     */
    @Test
    void cleanupOldUserActs_ShouldDeleteOldActs() throws Exception {
        LocalDateTime date = LocalDateTime.now().minusDays(30);
        doNothing().when(userActService).cleanupOldUserActs(date);

        mockMvc.perform(delete("/user-acts/cleanup")
                .param("date", date.toString()))
                .andExpect(status().isOk());

        verify(userActService).cleanupOldUserActs(date);
    }
}
