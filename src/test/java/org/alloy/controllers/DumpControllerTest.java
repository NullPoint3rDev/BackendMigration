package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.AlloyWebMvcTest;
import org.alloy.models.entities.Dump;
import org.alloy.services.DumpService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для DumpController.
 * Использует @AlloyWebMvcTest для тестирования только веб-слоя без поднятия полного контекста приложения.
 * /@WithMockUser обеспечивает аутентифицированного пользователя для тестов.
 * /@Import(MvcTestConfig.class) импортирует конфигурацию для тестов.
 */
@AlloyWebMvcTest(DumpController.class)
@WithMockUser
class DumpControllerTest {

    // MockMvc - основной инструмент для тестирования веб-слоя
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper для сериализации/десериализации JSON
    @Autowired
    private ObjectMapper objectMapper;

    // Мокаем DumpService, так как нам не нужна реальная работа с базой данных
    @MockBean
    private DumpService dumpService;

    // Тестовые данные
    private Dump testDump;
    private List<Dump> testDumps;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый дамп
        testDump = new Dump();
        testDump.setId(1);
        testDump.setMac("00:11:22:33:44:55");
        testDump.setIp("192.168.1.1");
        testDump.setData("Test data");
        testDump.setDateCreated(LocalDateTime.now());

        // Создаем список тестовых дампов
        Dump secondDump = new Dump();
        secondDump.setId(2);
        secondDump.setMac("AA:BB:CC:DD:EE:FF");
        secondDump.setIp("192.168.1.2");
        secondDump.setData("Test data 2");
        secondDump.setDateCreated(LocalDateTime.now());

        testDumps = Arrays.asList(testDump, secondDump);
    }

    /**
     * Тест получения всех дампов
     */
    @Test
    void getAll_ShouldReturnAllDumps() throws Exception {
        // Настраиваем мок для возврата списка дампов
        when(dumpService.findAll()).thenReturn(testDumps);

        // Выполняем GET запрос на /api/dumps
        mockMvc.perform(get("/dumps"))
                // Проверяем статус ответа и содержимое JSON
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].mac").value("00:11:22:33:44:55"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].mac").value("AA:BB:CC:DD:EE:FF"));

        // Проверяем, что сервис был вызван
        verify(dumpService).findAll();
    }

    /**
     * Тест получения дампа по существующему ID
     */
    @Test
    void getById_WhenDumpExists_ShouldReturnDump() throws Exception {
        // Настраиваем мок для возврата дампа
        when(dumpService.findById(1)).thenReturn(Optional.of(testDump));

        // Выполняем GET запрос на /api/dumps/1
        mockMvc.perform(get("/dumps/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mac").value("00:11:22:33:44:55"));

        verify(dumpService).findById(1);
    }

    /**
     * Тест получения дампа по несуществующему ID
     */
    @Test
    void getById_WhenDumpDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Настраиваем мок для возврата пустого Optional
        when(dumpService.findById(999)).thenReturn(Optional.empty());

        // Выполняем GET запрос на /api/dumps/999
        mockMvc.perform(get("/dumps/999"))
                .andExpect(status().isNotFound());

        verify(dumpService).findById(999);
    }

    /**
     * Тест создания нового дампа
     */
    @Test
    void create_ShouldCreateNewDump() throws Exception {
        // Настраиваем мок для сохранения дампа
        when(dumpService.save(any(Dump.class))).thenReturn(testDump);

        // Выполняем POST запрос на /api/dumps
        mockMvc.perform(post("/dumps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDump)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mac").value("00:11:22:33:44:55"));

        verify(dumpService).save(any(Dump.class));
    }

    /**
     * Тест обновления существующего дампа
     */
    @Test
    void update_WhenDumpExists_ShouldUpdateDump() throws Exception {
        // Настраиваем мок для поиска и сохранения дампа
        when(dumpService.findById(1)).thenReturn(Optional.of(testDump));
        when(dumpService.save(any(Dump.class))).thenReturn(testDump);

        // Выполняем PUT запрос на /api/dumps/1
        mockMvc.perform(put("/dumps/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDump)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mac").value("00:11:22:33:44:55"));

        verify(dumpService).findById(1);
        verify(dumpService).save(any(Dump.class));
    }

    /**
     * Тест обновления несуществующего дампа
     */
    @Test
    void update_WhenDumpDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Настраиваем мок для возврата пустого Optional
        when(dumpService.findById(999)).thenReturn(Optional.empty());

        // Выполняем PUT запрос на /api/dumps/999
        mockMvc.perform(put("/dumps/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDump)))
                .andExpect(status().isNotFound());

        verify(dumpService).findById(999);
        verify(dumpService, never()).save(any(Dump.class));
    }

    /**
     * Тест удаления существующего дампа
     */
    @Test
    void delete_WhenDumpExists_ShouldDeleteDump() throws Exception {
        // Настраиваем мок для поиска дампа
        when(dumpService.findById(1)).thenReturn(Optional.of(testDump));
        doNothing().when(dumpService).deleteById(anyInt());

        // Выполняем DELETE запрос на /api/dumps/1
        mockMvc.perform(delete("/dumps/1"))
                .andExpect(status().isNoContent());

        verify(dumpService).findById(1);
        verify(dumpService).deleteById(1);
    }

    /**
     * Тест удаления несуществующего дампа
     */
    @Test
    void delete_WhenDumpDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Настраиваем мок для возврата пустого Optional
        when(dumpService.findById(999)).thenReturn(Optional.empty());

        // Выполняем DELETE запрос на /api/dumps/999
        mockMvc.perform(delete("/dumps/999"))
                .andExpect(status().isNotFound());

        verify(dumpService).findById(999);
        verify(dumpService, never()).deleteById(anyInt());
    }
}
