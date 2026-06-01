package org.alloy.services;

import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.Notification;
import org.alloy.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для NotificationService
 * Проверяет корректность работы сервиса уведомлений
 */
@SpringBootTest(classes = NotificationService.class)
@ActiveProfiles("test")
public class NotificationServiceTest {

    @MockBean
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    private Notification testNotification;
    private LocalDateTime testDateTime;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект Notification с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.now();
        testNotification = new Notification();
        testNotification.setId(1);
        testNotification.setUserAccountId(1);
        testNotification.setTitle("Test Notification");
        testNotification.setMessage("This is a test notification");
        testNotification.setType("INFO");
        testNotification.setDateCreated(testDateTime);
        testNotification.setIsRead(false);
    }

    /**
     * Тест метода getAllNotifications()
     * Проверяет корректность получения всех уведомлений
     */
    @Test
    void getAllNotifications_ShouldReturnAllNotifications() {
        // Подготовка данных
        List<Notification> expectedNotifications = Arrays.asList(testNotification);
        when(notificationRepository.findAll()).thenReturn(expectedNotifications);

        // Выполнение теста
        List<Notification> actualNotifications = notificationService.getAllNotifications();

        // Проверка результатов
        assertNotNull(actualNotifications);
        assertEquals(expectedNotifications.size(), actualNotifications.size());
        assertEquals(expectedNotifications.get(0).getId(), actualNotifications.get(0).getId());

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).findAll();
    }

    /**
     * Тест метода getNotificationById() с существующим ID
     * Проверяет корректность получения уведомления по существующему ID
     */
    @Test
    void getNotificationById_WhenExists_ShouldReturnNotification() {
        // Подготовка данных
        when(notificationRepository.findById(1)).thenReturn(Optional.of(testNotification));

        // Выполнение теста
        Optional<Notification> result = notificationService.getNotificationById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testNotification.getId(), result.get().getId());
        assertEquals(testNotification.getTitle(), result.get().getTitle());

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).findById(1);
    }

    /**
     * Тест метода getNotificationsByUserAccountId()
     * Проверяет корректность получения уведомлений по ID пользователя
     */
    @Test
    void getNotificationsByUserAccountId_ShouldReturnUserNotifications() {
        // Подготовка данных
        List<Notification> expectedNotifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserAccountId(1)).thenReturn(expectedNotifications);

        // Выполнение теста
        List<Notification> actualNotifications = notificationService.getNotificationsByUserAccountId(1);

        // Проверка результатов
        assertNotNull(actualNotifications);
        assertEquals(expectedNotifications.size(), actualNotifications.size());
        assertEquals(expectedNotifications.get(0).getUserAccountId(), actualNotifications.get(0).getUserAccountId());

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).findByUserAccountId(1);
    }

    /**
     * Тест метода getUnreadNotificationsByUserAccountId()
     * Проверяет корректность получения непрочитанных уведомлений пользователя
     */
    @Test
    void getUnreadNotificationsByUserAccountId_ShouldReturnUnreadNotifications() {
        // Подготовка данных
        List<Notification> expectedNotifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserAccountIdAndIsReadFalse(1)).thenReturn(expectedNotifications);

        // Выполнение теста
        List<Notification> actualNotifications = notificationService.getUnreadNotificationsByUserAccountId(1);

        // Проверка результатов
        assertNotNull(actualNotifications);
        assertEquals(expectedNotifications.size(), actualNotifications.size());
        assertFalse(actualNotifications.get(0).getIsRead());

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).findByUserAccountIdAndIsReadFalse(1);
    }

    /**
     * Тест метода getNotificationsByUserAccountIdAndType()
     * Проверяет корректность получения уведомлений по ID пользователя и типу
     */
    @Test
    void getNotificationsByUserAccountIdAndType_ShouldReturnFilteredNotifications() {
        // Подготовка данных
        List<Notification> expectedNotifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserAccountIdAndType(1, "INFO")).thenReturn(expectedNotifications);

        // Выполнение теста
        List<Notification> actualNotifications = notificationService.getNotificationsByUserAccountIdAndType(1, "INFO");

        // Проверка результатов
        assertNotNull(actualNotifications);
        assertEquals(expectedNotifications.size(), actualNotifications.size());
        assertEquals("INFO", actualNotifications.get(0).getType());

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).findByUserAccountIdAndType(1, "INFO");
    }

    /**
     * Тест метода createNotification() с валидными данными
     * Проверяет корректность создания нового уведомления
     */
    @Test
    void createNotification_WithValidData_ShouldCreateNotification() {
        // Подготовка данных
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Выполнение теста
        Notification createdNotification = notificationService.createNotification(testNotification);

        // Проверка результатов
        assertNotNull(createdNotification);
        assertEquals(testNotification.getTitle(), createdNotification.getTitle());
        assertEquals(testNotification.getMessage(), createdNotification.getMessage());
        assertNotNull(createdNotification.getDateCreated());
        assertFalse(createdNotification.getIsRead());

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    /**
     * Тест метода createNotification() с невалидными данными
     * Проверяет корректность обработки ошибок при создании уведомления
     */
    @Test
    void createNotification_WithInvalidData_ShouldThrowException() {
        // Подготовка данных
        Notification invalidNotification = new Notification();
        invalidNotification.setUserAccountId(null);
        invalidNotification.setTitle(null);
        invalidNotification.setMessage(null);

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            notificationService.createNotification(invalidNotification)
        );
    }

    /**
     * Тест метода updateNotification()
     * Проверяет корректность обновления уведомления
     */
    @Test
    void updateNotification_ShouldUpdateNotification() {
        // Подготовка данных
        when(notificationRepository.findById(1)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Выполнение теста
        Notification updatedNotification = notificationService.updateNotification(testNotification);

        // Проверка результатов
        assertNotNull(updatedNotification);
        assertEquals(testNotification.getDateCreated(), updatedNotification.getDateCreated());

        // Проверка вызовов методов репозитория
        verify(notificationRepository, times(1)).findById(1);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    /**
     * Тест метода markNotificationAsRead()
     * Проверяет корректность отметки уведомления как прочитанного
     */
    @Test
    void markNotificationAsRead_ShouldMarkNotificationAsRead() {
        // Подготовка данных
        when(notificationRepository.findById(1)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Выполнение теста
        Notification markedNotification = notificationService.markNotificationAsRead(1);

        // Проверка результатов
        assertTrue(markedNotification.getIsRead());
        assertNotNull(markedNotification.getDateRead());

        // Проверка вызовов методов репозитория
        verify(notificationRepository, times(1)).findById(1);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    /**
     * Тест метода markAllNotificationsAsRead()
     * Проверяет корректность отметки всех уведомлений как прочитанных
     */
    @Test
    void markAllNotificationsAsRead_ShouldMarkAllNotificationsAsRead() {
        // Подготовка данных
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserAccountIdAndIsReadFalse(1)).thenReturn(notifications);

        // Выполнение теста
        notificationService.markAllNotificationsAsRead(1);

        // Проверка результатов
        assertTrue(notifications.get(0).getIsRead());
        assertNotNull(notifications.get(0).getDateRead());

        // Проверка вызовов методов репозитория
        verify(notificationRepository, times(1)).findByUserAccountIdAndIsReadFalse(1);
        verify(notificationRepository, times(1)).saveAll(notifications);
    }

    /**
     * Тест метода deleteNotification()
     * Проверяет корректность удаления уведомления
     */
    @Test
    void deleteNotification_ShouldDeleteNotification() {
        // Подготовка данных
        when(notificationRepository.existsById(1)).thenReturn(true);

        // Выполнение теста
        notificationService.deleteNotification(1);

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).deleteById(1);
    }

    /**
     * Тест метода deleteNotificationsByUserAccountId()
     * Проверяет корректность удаления всех уведомлений пользователя
     */
    @Test
    void deleteNotificationsByUserAccountId_ShouldDeleteAllUserNotifications() {
        // Выполнение теста
        notificationService.deleteNotificationsByUserAccountId(1);

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).deleteByUserAccountId(1);
    }

    /**
     * Тест метода cleanupOldNotifications()
     * Проверяет корректность очистки старых уведомлений
     */
    @Test
    void cleanupOldNotifications_ShouldDeleteOldNotifications() {
        // Подготовка данных
        LocalDateTime oldDate = LocalDateTime.now().minusDays(30);

        // Выполнение теста
        notificationService.cleanupOldNotifications(oldDate);

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).deleteByDateCreatedBefore(oldDate);
    }

    /**
     * Тест метода countUnreadNotificationsByUserAccountId()
     * Проверяет корректность подсчета непрочитанных уведомлений пользователя
     */
    @Test
    void countUnreadNotificationsByUserAccountId_ShouldReturnCorrectCount() {
        // Подготовка данных
        when(notificationRepository.countByUserAccountIdAndIsReadFalse(1)).thenReturn(5L);

        // Выполнение теста
        long count = notificationService.countUnreadNotificationsByUserAccountId(1);

        // Проверка результатов
        assertEquals(5L, count);

        // Проверка вызова метода репозитория
        verify(notificationRepository, times(1)).countByUserAccountIdAndIsReadFalse(1);
    }
}
