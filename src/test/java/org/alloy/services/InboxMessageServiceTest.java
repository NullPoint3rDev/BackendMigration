package org.alloy.services;

import org.alloy.ServiceTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.InboxMessage;
import org.alloy.repositories.InboxMessageRepository;
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
 * Тестовый класс для InboxMessageService
 * Проверяет корректность работы сервиса входящих сообщений
 */
@SpringBootTest(classes = InboxMessageService.class)
@ActiveProfiles("test")
@Import(ServiceTestConfig.class)
public class InboxMessageServiceTest {

    @MockBean
    private InboxMessageRepository inboxMessageRepository;

    @Autowired
    private InboxMessageService inboxMessageService;

    private InboxMessage testMessage;
    private LocalDateTime testDateTime;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект InboxMessage с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.now();
        testMessage = new InboxMessage();
        testMessage.setId(1);
        testMessage.setUserAccountId(1);
        testMessage.setUserAccountToId(2);
        testMessage.setDateCreated(testDateTime);
        testMessage.setSubject("Test Subject");
        testMessage.setMessage("Test Message");
        testMessage.setIsRead(false);
        testMessage.setIsDeleted(false);
        testMessage.setType("TEST");
    }

    /**
     * Тест метода getAllInboxMessages()
     * Проверяет корректность получения всех входящих сообщений
     */
    @Test
    void getAllInboxMessages_ShouldReturnAllMessages() {
        // Подготовка данных
        List<InboxMessage> expectedMessages = Arrays.asList(testMessage);
        when(inboxMessageRepository.findAll()).thenReturn(expectedMessages);

        // Выполнение теста
        List<InboxMessage> actualMessages = inboxMessageService.getAllInboxMessages();

        // Проверка результатов
        assertNotNull(actualMessages);
        assertEquals(expectedMessages.size(), actualMessages.size());
        assertEquals(expectedMessages.get(0).getId(), actualMessages.get(0).getId());

        // Проверка вызова метода репозитория
        verify(inboxMessageRepository, times(1)).findAll();
    }

    /**
     * Тест метода getInboxMessageById() с существующим ID
     * Проверяет корректность получения сообщения по существующему ID
     */
    @Test
    void getInboxMessageById_WhenExists_ShouldReturnMessage() {
        // Подготовка данных
        when(inboxMessageRepository.findById(1)).thenReturn(Optional.of(testMessage));

        // Выполнение теста
        Optional<InboxMessage> result = inboxMessageService.getInboxMessageById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testMessage.getId(), result.get().getId());
        assertEquals(testMessage.getSubject(), result.get().getSubject());

        // Проверка вызова метода репозитория
        verify(inboxMessageRepository, times(1)).findById(1);
    }

    /**
     * Тест метода getMessagesByUserAccountId()
     * Проверяет корректность получения сообщений по ID пользователя
     */
    @Test
    void getMessagesByUserAccountId_ShouldReturnUserMessages() {
        // Подготовка данных
        List<InboxMessage> expectedMessages = Arrays.asList(testMessage);
        when(inboxMessageRepository.findByUserAccountId(1)).thenReturn(expectedMessages);

        // Выполнение теста
        List<InboxMessage> actualMessages = inboxMessageService.getMessagesByUserAccountId(1);

        // Проверка результатов
        assertNotNull(actualMessages);
        assertEquals(expectedMessages.size(), actualMessages.size());
        assertEquals(expectedMessages.get(0).getUserAccountId(), actualMessages.get(0).getUserAccountId());

        // Проверка вызова метода репозитория
        verify(inboxMessageRepository, times(1)).findByUserAccountId(1);
    }

    /**
     * Тест метода getUnreadMessagesByUserAccountId()
     * Проверяет корректность получения непрочитанных сообщений пользователя
     */
    @Test
    void getUnreadMessagesByUserAccountId_ShouldReturnUnreadMessages() {
        // Подготовка данных
        List<InboxMessage> expectedMessages = Arrays.asList(testMessage);
        when(inboxMessageRepository.findUnreadMessagesByUserAccountIdOrderByDateCreatedDesc(1))
                .thenReturn(expectedMessages);

        // Выполнение теста
        List<InboxMessage> actualMessages = inboxMessageService.getUnreadMessagesByUserAccountId(1);

        // Проверка результатов
        assertNotNull(actualMessages);
        assertEquals(expectedMessages.size(), actualMessages.size());
        assertFalse(actualMessages.get(0).getIsRead());

        // Проверка вызова метода репозитория
        verify(inboxMessageRepository, times(1))
                .findUnreadMessagesByUserAccountIdOrderByDateCreatedDesc(1);
    }

    /**
     * Тест метода createInboxMessage() с валидными данными
     * Проверяет корректность создания нового сообщения
     */
    @Test
    void createInboxMessage_WithValidData_ShouldCreateMessage() {
        // Подготовка данных
        when(inboxMessageRepository.save(any(InboxMessage.class))).thenReturn(testMessage);

        // Выполнение теста
        InboxMessage createdMessage = inboxMessageService.createInboxMessage(testMessage);

        // Проверка результатов
        assertNotNull(createdMessage);
        assertEquals(testMessage.getSubject(), createdMessage.getSubject());
        assertEquals(testMessage.getMessage(), createdMessage.getMessage());
        assertFalse(createdMessage.getIsRead());

        // Проверка вызова метода репозитория
        verify(inboxMessageRepository, times(1)).save(any(InboxMessage.class));
    }

    /**
     * Тест метода createInboxMessage() с невалидными данными
     * Проверяет корректность обработки ошибок при создании сообщения
     */
    @Test
    void createInboxMessage_WithInvalidData_ShouldThrowException() {
        // Подготовка данных
        InboxMessage invalidMessage = new InboxMessage();
        invalidMessage.setUserAccountId(null);
        invalidMessage.setSubject(null);
        invalidMessage.setMessage(null);

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            inboxMessageService.createInboxMessage(invalidMessage)
        );
    }

    /**
     * Тест метода markInboxMessageAsRead()
     * Проверяет корректность отметки сообщения как прочитанного
     */
    @Test
    void markInboxMessageAsRead_ShouldMarkMessageAsRead() {
        // Подготовка данных
        when(inboxMessageRepository.findById(1)).thenReturn(Optional.of(testMessage));
        when(inboxMessageRepository.save(any(InboxMessage.class))).thenReturn(testMessage);

        // Выполнение теста
        InboxMessage markedMessage = inboxMessageService.markInboxMessageAsRead(1);

        // Проверка результатов
        assertNotNull(markedMessage);
        assertTrue(markedMessage.getIsRead());
        assertNotNull(markedMessage.getDateRead());

        // Проверка вызовов методов репозитория
        verify(inboxMessageRepository, times(1)).findById(1);
        verify(inboxMessageRepository, times(1)).save(any(InboxMessage.class));
    }

    /**
     * Тест метода markAllInboxMessagesAsRead()
     * Проверяет корректность отметки всех сообщений как прочитанных
     */
    @Test
    void markAllInboxMessagesAsRead_ShouldMarkAllMessagesAsRead() {
        // Подготовка данных
        List<InboxMessage> unreadMessages = Arrays.asList(testMessage);
        when(inboxMessageRepository.findByUserAccountIdAndIsReadFalse(1)).thenReturn(unreadMessages);
        when(inboxMessageRepository.saveAll(any())).thenReturn(unreadMessages);

        // Выполнение теста
        inboxMessageService.markAllInboxMessagesAsRead(1);

        // Проверка результатов
        verify(inboxMessageRepository, times(1)).findByUserAccountIdAndIsReadFalse(1);
        verify(inboxMessageRepository, times(1)).saveAll(any());
    }

    /**
     * Тест метода deleteInboxMessage()
     * Проверяет корректность удаления сообщения
     */
    @Test
    void deleteInboxMessage_ShouldDeleteMessage() {
        // Подготовка данных
        when(inboxMessageRepository.existsById(1)).thenReturn(true);

        // Выполнение теста
        inboxMessageService.deleteInboxMessage(1);

        // Проверка вызова метода репозитория
        verify(inboxMessageRepository, times(1)).deleteById(1);
    }

    /**
     * Тест метода deleteInboxMessage() с несуществующим ID
     * Проверяет корректность обработки ошибки при удалении несуществующего сообщения
     */
    @Test
    void deleteInboxMessage_WithNonExistentId_ShouldThrowException() {
        // Подготовка данных
        when(inboxMessageRepository.existsById(999)).thenReturn(false);

        // Проверка результатов
        assertThrows(IllegalArgumentException.class, () -> 
            inboxMessageService.deleteInboxMessage(999)
        );
    }

    /**
     * Тест метода deleteMessagesByUserAccountId()
     * Проверяет корректность удаления всех сообщений пользователя
     */
    @Test
    void deleteMessagesByUserAccountId_ShouldDeleteAllUserMessages() {
        // Выполнение теста
        inboxMessageService.deleteMessagesByUserAccountId(1);

        // Проверка вызова метода репозитория
        verify(inboxMessageRepository, times(1)).deleteByUserAccountId(1);
    }

    /**
     * Тест метода countUnreadMessagesByUserAccountId()
     * Проверяет корректность подсчета непрочитанных сообщений
     */
    @Test
    void countUnreadMessagesByUserAccountId_ShouldReturnCorrectCount() {
        // Подготовка данных
        when(inboxMessageRepository.countUnreadMessagesByUserAccountId(1)).thenReturn(5L);

        // Выполнение теста
        long count = inboxMessageService.countUnreadMessagesByUserAccountId(1);

        // Проверка результатов
        assertEquals(5L, count);

        // Проверка вызова метода репозитория
        verify(inboxMessageRepository, times(1)).countUnreadMessagesByUserAccountId(1);
    }
}
