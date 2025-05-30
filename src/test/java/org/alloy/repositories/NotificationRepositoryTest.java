package org.alloy.repositories;

import org.alloy.models.entities.Notification;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.entities.UserRole;
import org.alloy.models.GeneralStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности NotificationRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
public class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    private Notification testNotification;
    private UserAccount testUser;
    private UserRole testRole;
    private static final String TEST_TITLE = "Test Notification";
    private static final String TEST_MESSAGE = "This is a test notification";
    private static final String TEST_TYPE = "SYSTEM";
    private static final String TEST_LINK = "http://test.com";

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовые объекты UserRole, UserAccount и Notification
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую роль
        testRole = new UserRole();
        testRole.setName("Test Role");
        testRole.setDescription("Test Role Description");
        testRole.setStatus(GeneralStatus.Active);
        testRole = entityManager.persist(testRole);
        entityManager.flush();

        // Создаем тестового пользователя
        testUser = new UserAccount();
        testUser.setUserName("testUser");
        testUser.setEmail("test@example.com");
        testUser.setStatus(GeneralStatus.Active);
        testUser.setUserRoleId(testRole.getId());
        testUser = entityManager.persist(testUser);
        entityManager.flush();

        // Создаем тестовое уведомление
        testNotification = new Notification();
        testNotification.setUserAccountId(testUser.getId());
        testNotification.setTitle(TEST_TITLE);
        testNotification.setMessage(TEST_MESSAGE);
        testNotification.setType(TEST_TYPE);
        testNotification.setLink(TEST_LINK);
        testNotification.setIsRead(false);
        
        // Сохраняем объект в тестовой базе данных
        testNotification = entityManager.persist(testNotification);
        entityManager.flush();
    }

    /**
     * Тест сохранения нового уведомления
     * Проверяет, что уведомление корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveNotification() {
        // Создаем новое уведомление
        Notification newNotification = new Notification();
        newNotification.setUserAccountId(testUser.getId());
        newNotification.setTitle("New Notification");
        newNotification.setMessage("New notification message");
        newNotification.setType(TEST_TYPE);
        newNotification.setIsRead(false);
        
        // Сохраняем уведомление через репозиторий
        Notification savedNotification = notificationRepository.save(newNotification);
        
        // Проверяем, что уведомление было сохранено
        assertNotNull(savedNotification, "Сохраненное уведомление не должно быть null");
        assertNotNull(savedNotification.getId(), "ID сохраненного уведомления не должен быть null");
        assertNotNull(savedNotification.getDateCreated(), "Дата создания не должна быть null");
        assertEquals(newNotification.getUserAccountId(), savedNotification.getUserAccountId(), 
            "ID пользователя должен совпадать");
        assertEquals(newNotification.getTitle(), savedNotification.getTitle(), 
            "Заголовок должен совпадать");
        assertEquals(newNotification.getMessage(), savedNotification.getMessage(), 
            "Сообщение должно совпадать");
        assertEquals(newNotification.getType(), savedNotification.getType(), 
            "Тип должен совпадать");
        assertEquals(newNotification.getIsRead(), savedNotification.getIsRead(), 
            "Статус прочтения должен совпадать");
        
        // Проверяем, что уведомление можно найти в базе данных
        Notification foundNotification = entityManager.find(Notification.class, savedNotification.getId());
        assertNotNull(foundNotification, "Уведомление должно быть найдено в базе данных");
    }

    /**
     * Тест поиска уведомлений по ID пользователя
     * Проверяет, что метод findByUserAccountId возвращает все уведомления пользователя
     */
    @Test
    void findByUserAccountId_ShouldReturnUserNotifications() {
        // Создаем второе уведомление для того же пользователя
        Notification secondNotification = new Notification();
        secondNotification.setUserAccountId(testUser.getId());
        secondNotification.setTitle("Second Notification");
        secondNotification.setMessage("Second notification message");
        secondNotification.setType(TEST_TYPE);
        secondNotification.setIsRead(false);
        entityManager.persist(secondNotification);
        entityManager.flush();
        
        // Получаем все уведомления пользователя
        List<Notification> notifications = notificationRepository.findByUserAccountId(testUser.getId());
        
        // Проверяем результаты
        assertNotNull(notifications, "Список уведомлений не должен быть null");
        assertTrue(notifications.size() >= 2, "Должно быть найдено как минимум 2 уведомления");
        assertTrue(notifications.stream().anyMatch(n -> n.getId().equals(testNotification.getId())), 
            "Список должен содержать первое тестовое уведомление");
        assertTrue(notifications.stream().anyMatch(n -> n.getId().equals(secondNotification.getId())), 
            "Список должен содержать второе тестовое уведомление");
    }

    /**
     * Тест поиска непрочитанных уведомлений
     * Проверяет, что метод findByUserAccountIdAndIsReadFalse возвращает только непрочитанные уведомления
     */
    @Test
    void findByUserAccountIdAndIsReadFalse_ShouldReturnUnreadNotifications() {
        // Создаем прочитанное уведомление
        Notification readNotification = new Notification();
        readNotification.setUserAccountId(testUser.getId());
        readNotification.setTitle("Read Notification");
        readNotification.setMessage("Read notification message");
        readNotification.setType(TEST_TYPE);
        readNotification.setIsRead(true);
        readNotification.setDateRead(LocalDateTime.now());
        entityManager.persist(readNotification);
        entityManager.flush();
        
        // Получаем непрочитанные уведомления
        List<Notification> unreadNotifications = notificationRepository.findByUserAccountIdAndIsReadFalse(testUser.getId());
        
        // Проверяем результаты
        assertNotNull(unreadNotifications, "Список уведомлений не должен быть null");
        assertTrue(unreadNotifications.stream().allMatch(n -> !n.getIsRead()), 
            "Все уведомления должны быть непрочитанными");
        assertTrue(unreadNotifications.stream().anyMatch(n -> n.getId().equals(testNotification.getId())), 
            "Список должен содержать тестовое уведомление");
        assertFalse(unreadNotifications.stream().anyMatch(n -> n.getId().equals(readNotification.getId())), 
            "Список не должен содержать прочитанное уведомление");
    }

    /**
     * Тест поиска уведомлений по типу
     * Проверяет, что метод findByUserAccountIdAndType возвращает уведомления указанного типа
     */
    @Test
    void findByUserAccountIdAndType_ShouldReturnNotificationsByType() {
        // Создаем уведомление другого типа
        Notification otherTypeNotification = new Notification();
        otherTypeNotification.setUserAccountId(testUser.getId());
        otherTypeNotification.setTitle("Other Type Notification");
        otherTypeNotification.setMessage("Other type notification message");
        otherTypeNotification.setType("OTHER");
        otherTypeNotification.setIsRead(false);
        entityManager.persist(otherTypeNotification);
        entityManager.flush();
        
        // Получаем уведомления указанного типа
        List<Notification> typeNotifications = notificationRepository.findByUserAccountIdAndType(testUser.getId(), TEST_TYPE);
        
        // Проверяем результаты
        assertNotNull(typeNotifications, "Список уведомлений не должен быть null");
        assertTrue(typeNotifications.stream().allMatch(n -> n.getType().equals(TEST_TYPE)), 
            "Все уведомления должны быть указанного типа");
        assertTrue(typeNotifications.stream().anyMatch(n -> n.getId().equals(testNotification.getId())), 
            "Список должен содержать тестовое уведомление");
        assertFalse(typeNotifications.stream().anyMatch(n -> n.getId().equals(otherTypeNotification.getId())), 
            "Список не должен содержать уведомление другого типа");
    }

    /**
     * Тест подсчета непрочитанных уведомлений
     * Проверяет, что метод countUnreadNotificationsByUserAccountId возвращает корректное количество
     */
    @Test
    void countUnreadNotificationsByUserAccountId_ShouldReturnCorrectCount() {
        // Создаем второе непрочитанное уведомление
        Notification secondUnreadNotification = new Notification();
        secondUnreadNotification.setUserAccountId(testUser.getId());
        secondUnreadNotification.setTitle("Second Unread Notification");
        secondUnreadNotification.setMessage("Second unread notification message");
        secondUnreadNotification.setType(TEST_TYPE);
        secondUnreadNotification.setIsRead(false);
        entityManager.persist(secondUnreadNotification);
        entityManager.flush();
        
        // Получаем количество непрочитанных уведомлений
        long count = notificationRepository.countUnreadNotificationsByUserAccountId(testUser.getId());
        
        // Проверяем результат
        assertTrue(count >= 2, "Должно быть как минимум 2 непрочитанных уведомления");
    }

    /**
     * Тест поиска старых прочитанных уведомлений
     * Проверяет, что метод findOldReadNotifications возвращает прочитанные уведомления старше указанной даты
     */
    @Test
    void findOldReadNotifications_ShouldReturnOldReadNotifications() {
        // Создаем прочитанное уведомление
        Notification readNotification = new Notification();
        readNotification.setUserAccountId(testUser.getId());
        readNotification.setTitle("Read Notification");
        readNotification.setMessage("Read notification message");
        readNotification.setType(TEST_TYPE);
        readNotification.setIsRead(true);
        readNotification.setDateRead(LocalDateTime.now());
        entityManager.persist(readNotification);
        entityManager.flush();
        
        // Получаем старые прочитанные уведомления
        LocalDateTime oldDate = LocalDateTime.now().plusDays(1); // Дата в будущем, чтобы не получить текущие уведомления
        List<Notification> oldNotifications = notificationRepository.findOldReadNotifications(oldDate);
        
        // Проверяем результаты
        assertNotNull(oldNotifications, "Список уведомлений не должен быть null");
        assertTrue(oldNotifications.stream().allMatch(n -> n.getIsRead()), 
            "Все уведомления должны быть прочитанными");
    }

    /**
     * Тест удаления уведомлений пользователя
     * Проверяет, что метод deleteByUserAccountId корректно удаляет все уведомления пользователя
     */
    @Test
    void deleteByUserAccountId_ShouldDeleteUserNotifications() {
        // Удаляем уведомления пользователя
        notificationRepository.deleteByUserAccountId(testUser.getId());
        
        // Проверяем, что уведомления были удалены
        List<Notification> notifications = notificationRepository.findByUserAccountId(testUser.getId());
        assertTrue(notifications.isEmpty(), "Список уведомлений должен быть пустым");
    }

    /**
     * Тест удаления старых уведомлений
     * Проверяет, что метод deleteByDateCreatedBefore корректно удаляет уведомления старше указанной даты
     */
    @Test
    void deleteByDateCreatedBefore_ShouldDeleteOldNotifications() {
        // Удаляем старые уведомления
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        notificationRepository.deleteByDateCreatedBefore(futureDate);
        
        // Проверяем, что уведомления были удалены
        List<Notification> notifications = notificationRepository.findByUserAccountId(testUser.getId());
        assertTrue(notifications.isEmpty(), "Список уведомлений должен быть пустым");
    }
}
