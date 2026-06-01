package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.EmailTemplate;
import org.alloy.models.entities.InboxMessage;
import org.alloy.services.EmailTemplateService;
import org.alloy.services.InboxMessageService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для InboxMessageController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(InboxMessageController.class)
@Import(TestConfig.class)
@WithMockUser
class InboxMessageControllerTest {

    // MockMvc - основной инструмент для тестирования веб-слоя
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper для сериализации/десериализации JSON
    @Autowired
    private ObjectMapper objectMapper;

    // Мокаем InboxMessageService, так как нам не нужна реальная работа с базой данных
    @MockBean
    private InboxMessageService inboxMessageService;

    // Тестовые данные
    private InboxMessage testInboxMessage;
    private List<InboxMessage> testInboxMessages;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовое сообщение
        testInboxMessage = new InboxMessage();
        testInboxMessage.setId(1);
        testInboxMessage.setUserAccountId(1);
        testInboxMessage.setUserAccountToId(2);
        testInboxMessage.setSubject("Test Subject");
        testInboxMessage.setMessage("Test Message");
        testInboxMessage.setIsRead(false);
        testInboxMessage.setIsDeleted(false);
        testInboxMessage.setType("NOTIFICATION");
        testInboxMessage.setDateCreated(LocalDateTime.now());

        // Создаем второе тестовое сообщение
        InboxMessage secondMessage = new InboxMessage();
        secondMessage.setId(2);
        secondMessage.setUserAccountId(1);
        secondMessage.setUserAccountToId(2);
        secondMessage.setSubject("Test Subject 2");
        secondMessage.setMessage("Test Message 2");
        secondMessage.setIsRead(true);
        secondMessage.setIsDeleted(false);
        secondMessage.setType("ALERT");
        secondMessage.setDateCreated(LocalDateTime.now());

        testInboxMessages = Arrays.asList(testInboxMessage, secondMessage);
    }

    /**
     * Тест получения всех сообщений
     */
    @Test
    void getAllInboxMessages_ShouldReturnAllMessages() throws Exception {
        when(inboxMessageService.getAllInboxMessages()).thenReturn(testInboxMessages);

        mockMvc.perform(get("/inbox-messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].subject").value("Test Subject"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].subject").value("Test Subject 2"));

        verify(inboxMessageService).getAllInboxMessages();
    }

    /**
     * Тест получения сообщения по ID
     */
    @Test
    void getInboxMessageById_WhenMessageExists_ShouldReturnMessage() throws Exception {
        when(inboxMessageService.getInboxMessageById(1)).thenReturn(Optional.of(testInboxMessage));

        mockMvc.perform(get("/inbox-messages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.subject").value("Test Subject"));

        verify(inboxMessageService).getInboxMessageById(1);
    }

    /**
     * Тест получения сообщения по несуществующему ID
     */
    @Test
    void getInboxMessageById_WhenMessageDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(inboxMessageService.getInboxMessageById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/inbox-messages/999"))
                .andExpect(status().isNotFound());

        verify(inboxMessageService).getInboxMessageById(999);
    }

    /**
     * Тест получения сообщений пользователя
     */
    @Test
    void getMessagesByUserAccountId_ShouldReturnUserMessages() throws Exception {
        when(inboxMessageService.getMessagesByUserAccountId(1)).thenReturn(testInboxMessages);

        mockMvc.perform(get("/inbox-messages/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userAccountId").value(1))
                .andExpect(jsonPath("$[1].userAccountId").value(1));

        verify(inboxMessageService).getMessagesByUserAccountId(1);
    }

    /**
     * Тест получения непрочитанных сообщений пользователя
     */
    @Test
    void getUnreadMessagesByUserAccountId_ShouldReturnUnreadMessages() throws Exception {
        when(inboxMessageService.getUnreadMessagesByUserAccountId(1))
                .thenReturn(List.of(testInboxMessage));

        mockMvc.perform(get("/inbox-messages/user/1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(false));

        verify(inboxMessageService).getUnreadMessagesByUserAccountId(1);
    }

    /**
     * Тест получения сообщений пользователя по типу
     */
    @Test
    void getMessagesByUserAccountIdAndType_ShouldReturnTypedMessages() throws Exception {
        when(inboxMessageService.getMessagesByUserAccountIdAndType(1, "NOTIFICATION"))
                .thenReturn(List.of(testInboxMessage));

        mockMvc.perform(get("/inbox-messages/user/1/type/NOTIFICATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NOTIFICATION"));

        verify(inboxMessageService).getMessagesByUserAccountIdAndType(1, "NOTIFICATION");
    }

    /**
     * Тест подсчета непрочитанных сообщений пользователя
     */
    @Test
    void countUnreadMessagesByUserAccountId_ShouldReturnCount() throws Exception {
        when(inboxMessageService.countUnreadMessagesByUserAccountId(1)).thenReturn(1L);

        mockMvc.perform(get("/inbox-messages/user/1/unread/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));

        verify(inboxMessageService).countUnreadMessagesByUserAccountId(1);
    }

    /**
     * Тест создания нового сообщения
     */
    @Test
    void createInboxMessage_ShouldCreateMessage() throws Exception {
        when(inboxMessageService.createInboxMessage(any(InboxMessage.class))).thenReturn(testInboxMessage);

        mockMvc.perform(post("/inbox-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInboxMessage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.subject").value("Test Subject"));

        verify(inboxMessageService).createInboxMessage(any(InboxMessage.class));
    }

    /**
     * Тест обновления существующего сообщения
     */
    @Test
    void updateInboxMessage_WhenMessageExists_ShouldUpdateMessage() throws Exception {
        when(inboxMessageService.updateInboxMessage(any(InboxMessage.class))).thenReturn(testInboxMessage);

        mockMvc.perform(put("/inbox-messages/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInboxMessage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(inboxMessageService).updateInboxMessage(any(InboxMessage.class));
    }

    /**
     * Тест обновления несуществующего сообщения
     */
    @Test
    void updateInboxMessage_WhenMessageDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(inboxMessageService.updateInboxMessage(any(InboxMessage.class)))
                .thenThrow(new IllegalArgumentException("Message not found"));

        mockMvc.perform(put("/inbox-messages/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInboxMessage)))
                .andExpect(status().isNotFound());

        verify(inboxMessageService).updateInboxMessage(any(InboxMessage.class));
    }

    /**
     * Тест отметки сообщения как прочитанного
     */
    @Test
    void markInboxMessageAsRead_WhenMessageExists_ShouldMarkAsRead() throws Exception {
        when(inboxMessageService.markInboxMessageAsRead(1)).thenReturn(testInboxMessage);

        mockMvc.perform(put("/inbox-messages/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(inboxMessageService).markInboxMessageAsRead(1);
    }

    /**
     * Тест отметки всех сообщений пользователя как прочитанных
     */
    @Test
    void markAllInboxMessagesAsRead_ShouldMarkAllAsRead() throws Exception {
        doNothing().when(inboxMessageService).markAllInboxMessagesAsRead(1);

        mockMvc.perform(put("/inbox-messages/user/1/read-all"))
                .andExpect(status().isOk());

        verify(inboxMessageService).markAllInboxMessagesAsRead(1);
    }

    /**
     * Тест удаления существующего сообщения
     */
    @Test
    void deleteInboxMessage_WhenMessageExists_ShouldDeleteMessage() throws Exception {
        doNothing().when(inboxMessageService).deleteInboxMessage(1);

        mockMvc.perform(delete("/inbox-messages/1"))
                .andExpect(status().isOk());

        verify(inboxMessageService).deleteInboxMessage(1);
    }

    /**
     * Тест удаления несуществующего сообщения
     */
    @Test
    void deleteInboxMessage_WhenMessageDoesNotExist_ShouldReturnNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Message not found")).when(inboxMessageService).deleteInboxMessage(999);

        mockMvc.perform(delete("/inbox-messages/999"))
                .andExpect(status().isNotFound());

        verify(inboxMessageService).deleteInboxMessage(999);
    }

    /**
     * Тест удаления всех сообщений пользователя
     */
    @Test
    void deleteMessagesByUserAccountId_ShouldDeleteAllUserMessages() throws Exception {
        doNothing().when(inboxMessageService).deleteMessagesByUserAccountId(1);

        mockMvc.perform(delete("/inbox-messages/user/1"))
                .andExpect(status().isOk());

        verify(inboxMessageService).deleteMessagesByUserAccountId(1);
    }
}
