package org.alloy.services;

import org.springframework.test.context.ActiveProfiles;
import org.alloy.models.entities.Dump;
import org.alloy.repositories.DumpRepository;
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
 * Тестовый класс для DumpService
 * Проверяет корректность работы сервиса дампов
 */
@SpringBootTest(classes = DumpService.class)
@ActiveProfiles("test")
public class DumpServiceTest {

    @MockBean
    private DumpRepository dumpRepository;

    @Autowired
    private DumpService dumpService;

    private Dump testDump;

    /**
     * Инициализация тестовых данных перед каждым тестом
     * Создает тестовый объект Dump с необходимыми полями
     */
    @BeforeEach
    void setUp() {
        testDump = new Dump();
        testDump.setId(1);
        testDump.setDateCreated(LocalDateTime.now());
        testDump.setMac("00:11:22:33:44:55");
        testDump.setIp("192.168.1.1");
        testDump.setData("Test dump data");
    }

    /**
     * Тест метода findAll()
     * Проверяет корректность получения всех дампов
     */
    @Test
    void findAll_ShouldReturnAllDumps() {
        // Подготовка данных
        List<Dump> expectedDumps = Arrays.asList(testDump);
        when(dumpRepository.findAll()).thenReturn(expectedDumps);

        // Выполнение теста
        List<Dump> actualDumps = dumpService.findAll();

        // Проверка результатов
        assertNotNull(actualDumps);
        assertEquals(expectedDumps.size(), actualDumps.size());
        assertEquals(expectedDumps.get(0).getId(), actualDumps.get(0).getId());
        assertEquals(expectedDumps.get(0).getMac(), actualDumps.get(0).getMac());
        assertEquals(expectedDumps.get(0).getIp(), actualDumps.get(0).getIp());
        assertEquals(expectedDumps.get(0).getData(), actualDumps.get(0).getData());

        // Проверка вызова метода репозитория
        verify(dumpRepository, times(1)).findAll();
    }

    /**
     * Тест метода findById() с существующим ID
     * Проверяет корректность получения дампа по существующему ID
     */
    @Test
    void findById_WhenExists_ShouldReturnDump() {
        // Подготовка данных
        when(dumpRepository.findById(1)).thenReturn(Optional.of(testDump));

        // Выполнение теста
        Optional<Dump> result = dumpService.findById(1);

        // Проверка результатов
        assertTrue(result.isPresent());
        assertEquals(testDump.getId(), result.get().getId());
        assertEquals(testDump.getMac(), result.get().getMac());
        assertEquals(testDump.getIp(), result.get().getIp());
        assertEquals(testDump.getData(), result.get().getData());

        // Проверка вызова метода репозитория
        verify(dumpRepository, times(1)).findById(1);
    }

    /**
     * Тест метода findById() с несуществующим ID
     * Проверяет корректность обработки случая, когда дамп не найден
     */
    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // Подготовка данных
        when(dumpRepository.findById(999)).thenReturn(Optional.empty());

        // Выполнение теста
        Optional<Dump> result = dumpService.findById(999);

        // Проверка результатов
        assertFalse(result.isPresent());

        // Проверка вызова метода репозитория
        verify(dumpRepository, times(1)).findById(999);
    }

    /**
     * Тест метода save()
     * Проверяет корректность сохранения дампа
     */
    @Test
    void save_ShouldReturnSavedDump() {
        // Подготовка данных
        when(dumpRepository.save(any(Dump.class))).thenReturn(testDump);

        // Выполнение теста
        Dump savedDump = dumpService.save(testDump);

        // Проверка результатов
        assertNotNull(savedDump);
        assertEquals(testDump.getId(), savedDump.getId());
        assertEquals(testDump.getMac(), savedDump.getMac());
        assertEquals(testDump.getIp(), savedDump.getIp());
        assertEquals(testDump.getData(), savedDump.getData());

        // Проверка вызова метода репозитория
        verify(dumpRepository, times(1)).save(testDump);
    }

    /**
     * Тест метода deleteById()
     * Проверяет корректность удаления дампа
     */
    @Test
    void deleteById_ShouldDeleteDump() {
        // Выполнение теста
        dumpService.deleteById(1);

        // Проверка вызова метода репозитория
        verify(dumpRepository, times(1)).deleteById(1);
    }
}
