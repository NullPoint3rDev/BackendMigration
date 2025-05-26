package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.*;
import org.alloy.services.NotificationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для NotificationController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(NotificationController.class)
@Import(TestConfig.class)
@WithMockUser
public class NotificationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private NotificationService notificationService;

    private Notification testNotification;
    private List<Notification> testNotifications;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем первое тестовое уведомление
        testNotification = new Notification();
        testNotification.setId(1);
        testNotification.setDateCreated(LocalDateTime.now());
        testNotification.setLink("https://weldtelecom.com");
        testNotification.setMessage("Отчет о загрузке оборудования готов!");
        testNotification.setTitle("Отчет");
        testNotification.setIsRead(false);
        testNotification.setType("Info");
        testNotification.setDateRead(LocalDateTime.now());
        testNotification.setUserAccount(new UserAccount());
        testNotification.setUserAccountId(1);

        // Создаем второе тестовое уведомление
        Notification secondNotification = new Notification();
        secondNotification.setId(2);
        secondNotification.setDateCreated(LocalDateTime.now());
        secondNotification.setLink("https://weldtelecom.com");
        secondNotification.setMessage("Отчет по сотрудникам готов!");
        secondNotification.setTitle("Сотрудники");
        secondNotification.setIsRead(true);
        secondNotification.setType("Debug");
        secondNotification.setDateRead(LocalDateTime.now());
        secondNotification.setUserAccount(new UserAccount());
        secondNotification.setUserAccountId(2);

        testNotifications = Arrays.asList(testNotification, secondNotification);
    }

    /**
     * Тест получения всех уведомлений
     */
    @Test
    void getAllNotifications_ShouldReturnAllNotifications() throws Exception {
        when(notificationService.getAllNotifications()).thenReturn(testNotifications);

        mvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Отчет"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Сотрудники"));

        verify(notificationService).getAllNotifications();
    }

    /**
     * Тест получения уведомления по ID
     */
    @Test
    void getNotificationById_WhenNotificationExists_ShouldReturnNotification() throws Exception {
        when(notificationService.getNotificationById(1)).thenReturn(Optional.of(testNotification));

        mvc.perform(get("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Отчет"));

        verify(notificationService).getNotificationById(1);
    }

    /**
     * Тест получения несуществующего уведомления по ID
     */
    @Test
    void getNotificationById_WhenNotificationDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(notificationService.getNotificationById(999)).thenReturn(Optional.empty());

        mvc.perform(get("/api/notifications/999"))
                .andExpect(status().isNotFound());

        verify(notificationService).getNotificationById(999);
    }

    /**
     * Тест получения уведомлений пользователя
     */
    @Test
    void getNotificationsByUserAccountId_ShouldReturnUserNotifications() throws Exception {
        when(notificationService.getNotificationsByUserAccountId(1)).thenReturn(testNotifications);

        mvc.perform(get("/api/notifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userAccountId").value(1))
                .andExpect(jsonPath("$[1].userAccountId").value(2));

        verify(notificationService).getNotificationsByUserAccountId(1);
    }

    /**
     * Тест получения непрочитанных уведомлений пользователя
     */
    @Test
    void getUnreadNotificationsByUserAccountId_ShouldReturnUnreadNotifications() throws Exception {
        when(notificationService.getUnreadNotificationsByUserAccountId(1))
                .thenReturn(List.of(testNotification));

        mvc.perform(get("/api/notifications/user/1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(false));

        verify(notificationService).getUnreadNotificationsByUserAccountId(1);
    }

    /**
     * Тест получения уведомлений пользователя по типу
     */
    @Test
    void getNotificationsByUserAccountIdAndType_ShouldReturnTypedNotifications() throws Exception {
        when(notificationService.getNotificationsByUserAccountIdAndType(1, "Info"))
                .thenReturn(List.of(testNotification));

        mvc.perform(get("/api/notifications/user/1/type/Info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("Info"));

        verify(notificationService).getNotificationsByUserAccountIdAndType(1, "Info");
    }

    /**
     * Тест подсчета непрочитанных уведомлений пользователя
     */
    @Test
    void countUnreadNotificationsByUserAccountId_ShouldReturnCount() throws Exception {
        when(notificationService.countUnreadNotificationsByUserAccountId(1)).thenReturn(1L);

        mvc.perform(get("/api/notifications/user/1/unread/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));

        verify(notificationService).countUnreadNotificationsByUserAccountId(1);
    }

    /**
     * Тест создания нового уведомления
     */
    @Test
    void createNotification_ShouldCreateNotification() throws Exception {
        when(notificationService.createNotification(any(Notification.class))).thenReturn(testNotification);

        mvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testNotification)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Отчет"));

        verify(notificationService).createNotification(any(Notification.class));
    }

    /**
     * Тест создания уведомления с невалидными данными
     */
    @Test
    void createNotification_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        testNotification.setUserAccountId(null); // Невалидные данные

        when(notificationService.createNotification(any(Notification.class)))
                .thenThrow(new IllegalArgumentException("User Account ID is required"));

        mvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testNotification)))
                .andExpect(status().isBadRequest());

        verify(notificationService).createNotification(any(Notification.class));
    }

    /**
     * Тест обновления существующего уведомления
     */
    @Test
    void updateNotification_WhenNotificationExists_ShouldUpdateNotification() throws Exception {
        when(notificationService.updateNotification(any(Notification.class))).thenReturn(testNotification);

        mvc.perform(put("/api/notifications/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testNotification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(notificationService).updateNotification(any(Notification.class));
    }

    /**
     * Тест обновления несуществующего уведомления
     */
    @Test
    void updateNotification_WhenNotificationDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(notificationService.updateNotification(any(Notification.class)))
                .thenThrow(new IllegalArgumentException("Notification not found"));

        mvc.perform(put("/api/notifications/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testNotification)))
                .andExpect(status().isNotFound());

        verify(notificationService).updateNotification(any(Notification.class));
    }

    /**
     * Тест отметки уведомления как прочитанного
     */
    @Test
    void markNotificationAsRead_WhenNotificationExists_ShouldMarkAsRead() throws Exception {
        when(notificationService.markNotificationAsRead(1)).thenReturn(testNotification);

        mvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(notificationService).markNotificationAsRead(1);
    }

    /**
     * Тест отметки несуществующего уведомления как прочитанного
     */
    @Test
    void markNotificationAsRead_WhenNotificationDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(notificationService.markNotificationAsRead(999))
                .thenThrow(new IllegalArgumentException("Notification not found"));

        mvc.perform(put("/api/notifications/999/read"))
                .andExpect(status().isNotFound());

        verify(notificationService).markNotificationAsRead(999);
    }

    /**
     * Тест отметки всех уведомлений пользователя как прочитанных
     */
    @Test
    void markAllNotificationsAsRead_ShouldMarkAllAsRead() throws Exception {
        doNothing().when(notificationService).markAllNotificationsAsRead(1);

        mvc.perform(put("/api/notifications/user/1/read-all"))
                .andExpect(status().isOk());

        verify(notificationService).markAllNotificationsAsRead(1);
    }

    /**
     * Тест удаления существующего уведомления
     */
    @Test
    void deleteNotification_WhenNotificationExists_ShouldDeleteNotification() throws Exception {
        doNothing().when(notificationService).deleteNotification(1);

        mvc.perform(delete("/api/notifications/1"))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(1);
    }

    /**
     * Тест удаления несуществующего уведомления
     */
    @Test
    void deleteNotification_WhenNotificationDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Notification not found"))
                .when(notificationService).deleteNotification(999);

        mvc.perform(delete("/api/notifications/999"))
                .andExpect(status().isNotFound());

        verify(notificationService).deleteNotification(999);
    }

    /**
     * Тест удаления всех уведомлений пользователя
     */
    @Test
    void deleteNotificationsByUserAccountId_ShouldDeleteAllUserNotifications() throws Exception {
        doNothing().when(notificationService).deleteNotificationsByUserAccountId(1);

        mvc.perform(delete("/api/notifications/user/1"))
                .andExpect(status().isOk());

        verify(notificationService).deleteNotificationsByUserAccountId(1);
    }
}
