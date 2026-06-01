package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.QueuePushEvent;
import org.alloy.services.QueuePushEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для QueuePushEventController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(QueuePushEventController.class)
@Import(TestConfig.class)
@WithMockUser
public class QueuePushEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private QueuePushEventService queuePushEventService;

    private QueuePushEvent testQueuePushEvent;
    private List<QueuePushEvent> testQueuePushEvents;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовое событие
        testQueuePushEvent = new QueuePushEvent();
        testQueuePushEvent.setId(1);

        // Создаем второе тестовое событие
        QueuePushEvent secondEvent = new QueuePushEvent();
        secondEvent.setId(2);

        testQueuePushEvents = Arrays.asList(testQueuePushEvent, secondEvent);
    }

    /**
     * Тест получения всех событий
     */
    @Test
    void getAll_ShouldReturnListOfAllEvents() throws Exception {
        when(queuePushEventService.findAll()).thenReturn(testQueuePushEvents);

        mockMvc.perform(get("/queue-push-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(queuePushEventService).findAll();
    }

    /**
     * Тест получения события по ID
     */
    @Test
    void getById_WhenEventExists_ShouldReturnEvent() throws Exception {
        when(queuePushEventService.findById(1)).thenReturn(Optional.of(testQueuePushEvent));

        mockMvc.perform(get("/queue-push-events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(queuePushEventService).findById(1);
    }

    /**
     * Тест получения несуществующего события по ID
     */
    @Test
    void getById_WhenEventDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(queuePushEventService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/queue-push-events/999"))
                .andExpect(status().isNotFound());

        verify(queuePushEventService).findById(999);
    }

    /**
     * Тест создания нового события
     */
    @Test
    void create_ShouldCreateEvent() throws Exception {
        when(queuePushEventService.save(any(QueuePushEvent.class))).thenReturn(testQueuePushEvent);

        mockMvc.perform(post("/queue-push-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testQueuePushEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(queuePushEventService).save(any(QueuePushEvent.class));
    }

    /**
     * Тест обновления существующего события
     */
    @Test
    void update_WhenEventExists_ShouldUpdateEvent() throws Exception {
        when(queuePushEventService.findById(1)).thenReturn(Optional.of(testQueuePushEvent));
        when(queuePushEventService.save(any(QueuePushEvent.class))).thenReturn(testQueuePushEvent);

        mockMvc.perform(put("/queue-push-events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testQueuePushEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(queuePushEventService).findById(1);
        verify(queuePushEventService).save(any(QueuePushEvent.class));
    }

    /**
     * Тест обновления несуществующего события
     */
    @Test
    void update_WhenEventDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(queuePushEventService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/queue-push-events/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testQueuePushEvent)))
                .andExpect(status().isNotFound());

        verify(queuePushEventService).findById(999);
        verify(queuePushEventService, never()).save(any(QueuePushEvent.class));
    }

    /**
     * Тест удаления существующего события
     */
    @Test
    void delete_WhenEventExists_ShouldDeleteEvent() throws Exception {
        when(queuePushEventService.findById(1)).thenReturn(Optional.of(testQueuePushEvent));
        doNothing().when(queuePushEventService).deleteById(1);

        mockMvc.perform(delete("/queue-push-events/1"))
                .andExpect(status().isNoContent());

        verify(queuePushEventService).findById(1);
        verify(queuePushEventService).deleteById(1);
    }

    /**
     * Тест удаления несуществующего события
     */
    @Test
    void delete_WhenEventDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(queuePushEventService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/queue-push-events/999"))
                .andExpect(status().isNotFound());

        verify(queuePushEventService).findById(999);
        verify(queuePushEventService, never()).deleteById(any(Integer.class));
    }
}
