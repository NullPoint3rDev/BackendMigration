package org.alloy.services;

import org.alloy.AlloyServiceTest;
import org.alloy.models.entities.UserAct;
import org.alloy.repositories.UserActRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@AlloyServiceTest(UserActService.class)
public class UserActServiceTest {

    @MockBean
    private UserActRepository userActRepository;

    @Autowired
    private UserActService userActService;

    private UserAct testUserAct;

    @BeforeEach
    void setUp() {
        // Создаем тестовое действие пользователя с минимальным набором полей
        testUserAct = new UserAct();
        testUserAct.setId(1);
        testUserAct.setUserAccountId(2);
        testUserAct.setType("LOGIN");
        testUserAct.setDescription("User logged in");
        testUserAct.setDateCreated(LocalDateTime.now());
    }

    /**
     * Тест получения всех действий пользователя
     * Проверяет, что сервис возвращает список действий, полученный из репозитория
     */
    @Test
    void getAllUserActs_ShouldReturnAllActs() {
        List<UserAct> acts = Arrays.asList(testUserAct);
        when(userActRepository.findAll()).thenReturn(acts);

        List<UserAct> result = userActService.getAllUserActs();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserAct.getType(), result.get(0).getType());
        verify(userActRepository, times(1)).findAll();
    }

    /**
     * Тест получения действия пользователя по ID (существующий)
     */
    @Test
    void getUserActById_WhenExists_ShouldReturnAct() {
        when(userActRepository.findById(1)).thenReturn(Optional.of(testUserAct));
        Optional<UserAct> result = userActService.getUserActById(1);
        assertTrue(result.isPresent());
        assertEquals(testUserAct.getId(), result.get().getId());
        verify(userActRepository, times(1)).findById(1);
    }

    /**
     * Тест получения действия пользователя по ID (не существует)
     */
    @Test
    void getUserActById_WhenNotExists_ShouldReturnEmpty() {
        when(userActRepository.findById(999)).thenReturn(Optional.empty());
        Optional<UserAct> result = userActService.getUserActById(999);
        assertFalse(result.isPresent());
        verify(userActRepository, times(1)).findById(999);
    }

    /**
     * Тест получения действий пользователя по ID пользователя
     */
    @Test
    void getUserActsByUserId_ShouldReturnActs() {
        List<UserAct> acts = Arrays.asList(testUserAct);
        when(userActRepository.findByUserAccountId(2)).thenReturn(acts);
        List<UserAct> result = userActService.getUserActsByUserId(2);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserAct.getUserAccountId(), result.get(0).getUserAccountId());
        verify(userActRepository, times(1)).findByUserAccountId(2);
    }

    /**
     * Тест получения действий пользователя по ID пользователя и типу
     */
    @Test
    void getUserActsByUserIdAndType_ShouldReturnActs() {
        List<UserAct> acts = Arrays.asList(testUserAct);
        when(userActRepository.findByUserAccountIdAndType(2, "LOGIN")).thenReturn(acts);
        List<UserAct> result = userActService.getUserActsByUserIdAndType(2, "LOGIN");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserAct.getType(), result.get(0).getType());
        verify(userActRepository, times(1)).findByUserAccountIdAndType(2, "LOGIN");
    }

    /**
     * Тест получения действий пользователя по ID пользователя и диапазону дат
     */
    @Test
    void getUserActsByUserIdAndDateRange_ShouldReturnActs() {
        List<UserAct> acts = Arrays.asList(testUserAct);
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        when(userActRepository.findUserActsByUserAccountIdAndDateRange(2, startDate, endDate)).thenReturn(acts);
        List<UserAct> result = userActService.getUserActsByUserIdAndDateRange(2, startDate, endDate);
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userActRepository, times(1)).findUserActsByUserAccountIdAndDateRange(2, startDate, endDate);
    }

    /**
     * Тест получения действий пользователя по ID пользователя, типу и диапазону дат
     */
    @Test
    void getUserActsByUserIdAndTypeAndDateRange_ShouldReturnActs() {
        List<UserAct> acts = Arrays.asList(testUserAct);
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        when(userActRepository.findUserActsByUserAccountIdAndTypeAndDateRange(2, "LOGIN", startDate, endDate)).thenReturn(acts);
        List<UserAct> result = userActService.getUserActsByUserIdAndTypeAndDateRange(2, "LOGIN", startDate, endDate);
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userActRepository, times(1)).findUserActsByUserAccountIdAndTypeAndDateRange(2, "LOGIN", startDate, endDate);
    }

    /**
     * Тест подсчета действий пользователя по ID пользователя, типу и диапазону дат
     */
    @Test
    void countUserActsByUserIdAndTypeAndDateRange_ShouldReturnCount() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        when(userActRepository.countUserActsByUserAccountIdAndTypeAndDateRange(2, "LOGIN", startDate, endDate)).thenReturn(1L);
        long result = userActService.countUserActsByUserIdAndTypeAndDateRange(2, "LOGIN", startDate, endDate);
        assertEquals(1L, result);
        verify(userActRepository, times(1)).countUserActsByUserAccountIdAndTypeAndDateRange(2, "LOGIN", startDate, endDate);
    }
}
