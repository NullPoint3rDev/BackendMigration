package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.TestConfig;
import org.alloy.models.entities.QueueTask;
import org.alloy.services.QueueTaskService;
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
 * Тесты для QueueTaskController.
 * Использует @WebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(TestConfig.class) импортирует конфигурацию для тестов.
 */
@WebMvcTest(QueueTaskController.class)
@Import(TestConfig.class)
@WithMockUser
public class QueueTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private QueueTaskService queueTaskService;

    private QueueTask testQueueTask;
    private List<QueueTask> testQueueTasks;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовую задачу
        testQueueTask = new QueueTask();
        testQueueTask.setId(1);
        testQueueTask.setScheduleTaskId(2);
        testQueueTask.setDateCreated(LocalDateTime.now());
        testQueueTask.setTaskName("Тестовая задача");
        testQueueTask.setPriority(3);
        testQueueTask.setStatus(4);
        testQueueTask.setUserAccountId(5);
        testQueueTask.setDateFinished(LocalDateTime.now());
        testQueueTask.setDateStarted(LocalDateTime.now());
        testQueueTask.setScheduledOn(LocalDateTime.now());
        testQueueTask.setStatusMessage("В процессе");
        testQueueTask.setStatusResult("Выполнено");
        testQueueTask.setTaskParametersJson("{\"param1\": \"value1\"}");
        testQueueTask.setTaskResultJson("{\"result\": \"success\"}");

        // Создаем вторую тестовую задачу
        QueueTask secondTask = new QueueTask();
        secondTask.setId(2);
        secondTask.setScheduleTaskId(3);
        secondTask.setDateCreated(LocalDateTime.now());
        secondTask.setTaskName("Вторая задача");
        secondTask.setPriority(1);
        secondTask.setStatus(2);
        secondTask.setUserAccountId(6);
        secondTask.setDateFinished(LocalDateTime.now());
        secondTask.setDateStarted(LocalDateTime.now());
        secondTask.setScheduledOn(LocalDateTime.now());
        secondTask.setStatusMessage("Ожидает");
        secondTask.setStatusResult("В очереди");
        secondTask.setTaskParametersJson("{\"param2\": \"value2\"}");
        secondTask.setTaskResultJson("{\"result\": \"pending\"}");

        testQueueTasks = Arrays.asList(testQueueTask, secondTask);
    }

    /**
     * Тест получения всех задач
     */
    @Test
    void getAll_ShouldReturnListOfAllTasks() throws Exception {
        when(queueTaskService.findAll()).thenReturn(testQueueTasks);

        mockMvc.perform(get("/api/queue-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].taskName").value("Тестовая задача"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].taskName").value("Вторая задача"));

        verify(queueTaskService).findAll();
    }

    /**
     * Тест получения задачи по ID
     */
    @Test
    void getById_WhenTaskExists_ShouldReturnTask() throws Exception {
        when(queueTaskService.findById(1)).thenReturn(Optional.of(testQueueTask));

        mockMvc.perform(get("/api/queue-tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taskName").value("Тестовая задача"))
                .andExpect(jsonPath("$.statusMessage").value("В процессе"));

        verify(queueTaskService).findById(1);
    }

    /**
     * Тест получения несуществующей задачи по ID
     */
    @Test
    void getById_WhenTaskDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(queueTaskService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/queue-tasks/999"))
                .andExpect(status().isNotFound());

        verify(queueTaskService).findById(999);
    }

    /**
     * Тест создания новой задачи
     */
    @Test
    void create_ShouldCreateTask() throws Exception {
        when(queueTaskService.save(any(QueueTask.class))).thenReturn(testQueueTask);

        mockMvc.perform(post("/api/queue-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testQueueTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taskName").value("Тестовая задача"));

        verify(queueTaskService).save(any(QueueTask.class));
    }

    /**
     * Тест обновления существующей задачи
     */
    @Test
    void update_WhenTaskExists_ShouldUpdateTask() throws Exception {
        when(queueTaskService.findById(1)).thenReturn(Optional.of(testQueueTask));
        when(queueTaskService.save(any(QueueTask.class))).thenReturn(testQueueTask);

        mockMvc.perform(put("/api/queue-tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testQueueTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taskName").value("Тестовая задача"));

        verify(queueTaskService).findById(1);
        verify(queueTaskService).save(any(QueueTask.class));
    }

    /**
     * Тест обновления несуществующей задачи
     */
    @Test
    void update_WhenTaskDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(queueTaskService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/queue-tasks/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(testQueueTask)))
                .andExpect(status().isNotFound());

        verify(queueTaskService).findById(999);
        verify(queueTaskService, never()).save(any(QueueTask.class));
    }

    /**
     * Тест удаления существующей задачи
     */
    @Test
    void delete_WhenTaskExists_ShouldDeleteTask() throws Exception {
        when(queueTaskService.findById(1)).thenReturn(Optional.of(testQueueTask));
        doNothing().when(queueTaskService).deleteById(1);

        mockMvc.perform(delete("/api/queue-tasks/1"))
                .andExpect(status().isNoContent());

        verify(queueTaskService).findById(1);
        verify(queueTaskService).deleteById(1);
    }

    /**
     * Тест удаления несуществующей задачи
     */
    @Test
    void delete_WhenTaskDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(queueTaskService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/queue-tasks/999"))
                .andExpect(status().isNotFound());

        verify(queueTaskService).findById(999);
        verify(queueTaskService, never()).deleteById(any(Integer.class));
    }
}
