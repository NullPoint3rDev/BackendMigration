package org.alloy.repositories;

import org.alloy.models.entities.InboxMessage;
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
 * Тестовый класс для проверки функциональности InboxMessageRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
public class InboxMessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InboxMessageRepository inboxMessageRepository;

    private InboxMessage testMessage;
    private UserAccount testUser;
    private UserAccount testUserTo;
    private UserRole testRole;
    private static final String TEST_TYPE = "NOTIFICATION";

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовые объекты UserRole, UserAccount и InboxMessage для использования в тестах
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

        // Создаем тестовых пользователей
        testUser = new UserAccount();
        testUser.setUserName("testUser");
        testUser.setEmail("test@example.com");
        testUser.setStatus(GeneralStatus.Active);
        testUser.setUserRoleId(testRole.getId());
        testUser = entityManager.persist(testUser);

        testUserTo = new UserAccount();
        testUserTo.setUserName("testUserTo");
        testUserTo.setEmail("test2@example.com");
        testUserTo.setStatus(GeneralStatus.Active);
        testUserTo.setUserRoleId(testRole.getId());
        testUserTo = entityManager.persist(testUserTo);
        entityManager.flush();
        
        // Создаем тестовый объект InboxMessage с тестовыми данными
        testMessage = new InboxMessage();
        testMessage.setUserAccountId(testUser.getId());
        testMessage.setUserAccountToId(testUserTo.getId());
        testMessage.setSubject("Test Subject");
        testMessage.setMessage("Test Message");
        testMessage.setIsRead(false);
        testMessage.setIsDeleted(false);
        testMessage.setType(TEST_TYPE);
        
        // Сохраняем объект в тестовой базе данных
        testMessage = entityManager.persist(testMessage);
        entityManager.flush();
    }

    /**
     * Тест сохранения нового сообщения
     * Проверяет, что сообщение корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveInboxMessage() {
        // Создаем новое сообщение
        InboxMessage newMessage = new InboxMessage();
        newMessage.setUserAccountId(testUser.getId());
        newMessage.setUserAccountToId(testUserTo.getId());
        newMessage.setSubject("New Subject");
        newMessage.setMessage("New Message");
        newMessage.setIsRead(false);
        newMessage.setIsDeleted(false);
        newMessage.setType(TEST_TYPE);
        
        // Сохраняем сообщение через репозиторий
        InboxMessage savedMessage = inboxMessageRepository.save(newMessage);
        
        // Проверяем, что сообщение было сохранено
        assertNotNull(savedMessage, "Сохраненное сообщение не должно быть null");
        assertNotNull(savedMessage.getId(), "ID сохраненного сообщения не должен быть null");
        assertNotNull(savedMessage.getDateCreated(), "Дата создания не должна быть null");
        assertEquals(newMessage.getUserAccountId(), savedMessage.getUserAccountId(), "ID отправителя должен совпадать");
        assertEquals(newMessage.getUserAccountToId(), savedMessage.getUserAccountToId(), "ID получателя должен совпадать");
        assertEquals(newMessage.getSubject(), savedMessage.getSubject(), "Тема должна совпадать");
        assertEquals(newMessage.getMessage(), savedMessage.getMessage(), "Сообщение должно совпадать");
        assertEquals(newMessage.getType(), savedMessage.getType(), "Тип должен совпадать");
        
        // Проверяем, что сообщение можно найти в базе данных
        InboxMessage foundMessage = entityManager.find(InboxMessage.class, savedMessage.getId());
        assertNotNull(foundMessage, "Сообщение должно быть найдено в базе данных");
    }

    /**
     * Тест поиска сообщений по ID пользователя
     * Проверяет, что метод findByUserAccountId возвращает все сообщения пользователя
     */
    @Test
    void findByUserAccountId_ShouldReturnUserMessages() {
        // Создаем второе сообщение для того же пользователя
        final InboxMessage secondMessage = new InboxMessage();
        secondMessage.setUserAccountId(testUser.getId());
        secondMessage.setUserAccountToId(testUserTo.getId());
        secondMessage.setSubject("Second Subject");
        secondMessage.setMessage("Second Message");
        secondMessage.setIsRead(true);
        secondMessage.setIsDeleted(false);
        secondMessage.setType(TEST_TYPE);
        entityManager.persist(secondMessage);
        entityManager.flush();
        
        // Получаем все сообщения пользователя
        List<InboxMessage> messages = inboxMessageRepository.findByUserAccountId(testUser.getId());
        
        // Проверяем результаты
        assertNotNull(messages, "Список сообщений не должен быть null");
        assertTrue(messages.size() >= 2, "Должно быть найдено как минимум 2 сообщения");
        assertTrue(messages.stream().anyMatch(m -> m.getId().equals(testMessage.getId())), 
            "Список должен содержать первое тестовое сообщение");
        assertTrue(messages.stream().anyMatch(m -> m.getId().equals(secondMessage.getId())), 
            "Список должен содержать второе тестовое сообщение");
    }

    /**
     * Тест поиска непрочитанных сообщений
     * Проверяет, что метод findByUserAccountIdAndIsReadFalse возвращает только непрочитанные сообщения
     */
    @Test
    void findByUserAccountIdAndIsReadFalse_ShouldReturnUnreadMessages() {
        // Получаем непрочитанные сообщения
        List<InboxMessage> unreadMessages = inboxMessageRepository.findByUserAccountIdAndIsReadFalse(testUser.getId());
        
        // Проверяем результаты
        assertNotNull(unreadMessages, "Список непрочитанных сообщений не должен быть null");
        assertTrue(unreadMessages.stream().allMatch(m -> !m.getIsRead()), 
            "Все сообщения должны быть непрочитанными");
        assertTrue(unreadMessages.stream().anyMatch(m -> m.getId().equals(testMessage.getId())), 
            "Список должен содержать тестовое сообщение");
    }

    /**
     * Тест поиска сообщений по типу
     * Проверяет, что метод findByUserAccountIdAndType возвращает сообщения определенного типа
     */
    @Test
    void findByUserAccountIdAndType_ShouldReturnMessagesByType() {
        // Получаем сообщения определенного типа
        List<InboxMessage> typedMessages = inboxMessageRepository.findByUserAccountIdAndType(testUser.getId(), TEST_TYPE);
        
        // Проверяем результаты
        assertNotNull(typedMessages, "Список сообщений по типу не должен быть null");
        assertTrue(typedMessages.stream().allMatch(m -> TEST_TYPE.equals(m.getType())), 
            "Все сообщения должны быть указанного типа");
        assertTrue(typedMessages.stream().anyMatch(m -> m.getId().equals(testMessage.getId())), 
            "Список должен содержать тестовое сообщение");
    }

    /**
     * Тест подсчета непрочитанных сообщений
     * Проверяет, что метод countUnreadMessagesByUserAccountId возвращает корректное количество
     */
    @Test
    void countUnreadMessagesByUserAccountId_ShouldReturnCorrectCount() {
        // Получаем количество непрочитанных сообщений
        long unreadCount = inboxMessageRepository.countUnreadMessagesByUserAccountId(testUser.getId());
        
        // Проверяем результат
        assertTrue(unreadCount >= 1, "Должно быть как минимум 1 непрочитанное сообщение");
    }

    /**
     * Тест удаления сообщений пользователя
     * Проверяет, что метод deleteByUserAccountId корректно удаляет все сообщения пользователя
     */
    @Test
    void deleteByUserAccountId_ShouldRemoveAllUserMessages() {
        // Удаляем все сообщения пользователя
        inboxMessageRepository.deleteByUserAccountId(testUser.getId());
        
        // Проверяем, что сообщения были удалены
        List<InboxMessage> remainingMessages = inboxMessageRepository.findByUserAccountId(testUser.getId());
        assertTrue(remainingMessages.isEmpty(), "Все сообщения пользователя должны быть удалены");
    }

    /**
     * Тест сортировки непрочитанных сообщений по дате создания
     * Проверяет, что метод findUnreadMessagesByUserAccountIdOrderByDateCreatedDesc
     * возвращает сообщения в правильном порядке
     */
    @Test
    void findUnreadMessagesByUserAccountIdOrderByDateCreatedDesc_ShouldReturnOrderedMessages() {
        // Создаем второе непрочитанное сообщение
        final InboxMessage secondMessage = new InboxMessage();
        secondMessage.setUserAccountId(testUser.getId());
        secondMessage.setUserAccountToId(testUserTo.getId());
        secondMessage.setSubject("Second Subject");
        secondMessage.setMessage("Second Message");
        secondMessage.setIsRead(false);
        secondMessage.setIsDeleted(false);
        secondMessage.setType(TEST_TYPE);
        entityManager.persist(secondMessage);
        entityManager.flush();
        
        // Получаем непрочитанные сообщения, отсортированные по дате
        List<InboxMessage> orderedMessages = inboxMessageRepository
            .findUnreadMessagesByUserAccountIdOrderByDateCreatedDesc(testUser.getId());
        
        // Проверяем результаты
        assertNotNull(orderedMessages, "Список сообщений не должен быть null");
        assertTrue(orderedMessages.size() >= 2, "Должно быть как минимум 2 сообщения");
        
        // Проверяем порядок сортировки
        for (int i = 0; i < orderedMessages.size() - 1; i++) {
            assertTrue(orderedMessages.get(i).getDateCreated()
                .isAfter(orderedMessages.get(i + 1).getDateCreated()) ||
                orderedMessages.get(i).getDateCreated()
                .isEqual(orderedMessages.get(i + 1).getDateCreated()),
                "Сообщения должны быть отсортированы по убыванию даты создания");
        }
    }
}
