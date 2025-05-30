package org.alloy.repositories;

import org.alloy.models.entities.Dump;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки функциональности DumpRepository
 * Использует @DataJpaTest для тестирования JPA репозитория
 * /@DataJpaTest автоматически настраивает тестовую базу данных и контекст JPA
 */
@DataJpaTest
@ActiveProfiles("test")
public class DumpRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DumpRepository dumpRepository;

    private Dump testDump;

    /**
     * Метод, выполняющийся перед каждым тестом
     * Создает тестовый объект Dump для использования в тестах
     */
    @BeforeEach
    void setUp() {
        // Создаем тестовый объект Dump с тестовыми данными
        testDump = new Dump();
        testDump.setMac("00:11:22:33:44:55");
        testDump.setIp("192.168.1.1");
        testDump.setData("Test dump data");
        
        // Сохраняем объект в тестовой базе данных
        entityManager.persist(testDump);
        entityManager.flush();
    }

    /**
     * Тест сохранения нового дампа
     * Проверяет, что дамп корректно сохраняется в базе данных
     */
    @Test
    void save_ShouldSaveDump() {
        // Создаем новый дамп
        Dump newDump = new Dump();
        newDump.setMac("AA:BB:CC:DD:EE:FF");
        newDump.setIp("192.168.1.2");
        newDump.setData("New dump data");
        
        // Сохраняем дамп через репозиторий
        Dump savedDump = dumpRepository.save(newDump);
        
        // Проверяем, что дамп был сохранен
        assertNotNull(savedDump, "Сохраненный дамп не должен быть null");
        assertNotNull(savedDump.getId(), "ID сохраненного дампа не должен быть null");
        assertNotNull(savedDump.getDateCreated(), "Дата создания не должна быть null");
        assertEquals(newDump.getMac(), savedDump.getMac(), "MAC-адрес должен совпадать");
        assertEquals(newDump.getIp(), savedDump.getIp(), "IP-адрес должен совпадать");
        assertEquals(newDump.getData(), savedDump.getData(), "Данные должны совпадать");
        
        // Проверяем, что дамп можно найти в базе данных
        Dump foundDump = entityManager.find(Dump.class, savedDump.getId());
        assertNotNull(foundDump, "Дамп должен быть найден в базе данных");
    }

    /**
     * Тест поиска дампа по ID
     * Проверяет, что дамп корректно находится по его ID
     */
    @Test
    void findById_ShouldReturnDump() {
        // Ищем дамп по ID
        Optional<Dump> foundDump = dumpRepository.findById(testDump.getId());
        
        // Проверяем результаты
        assertTrue(foundDump.isPresent(), "Дамп должен быть найден");
        assertEquals(testDump.getId(), foundDump.get().getId(), "ID найденного дампа должен совпадать");
        assertEquals(testDump.getMac(), foundDump.get().getMac(), "MAC-адрес должен совпадать");
        assertEquals(testDump.getIp(), foundDump.get().getIp(), "IP-адрес должен совпадать");
        assertEquals(testDump.getData(), foundDump.get().getData(), "Данные должны совпадать");
    }

    /**
     * Тест поиска несуществующего дампа
     * Проверяет, что при поиске несуществующего дампа возвращается пустой Optional
     */
    @Test
    void findById_WhenDumpDoesNotExist_ShouldReturnEmpty() {
        // Ищем дамп с несуществующим ID
        Optional<Dump> foundDump = dumpRepository.findById(999);
        
        // Проверяем, что результат пустой
        assertFalse(foundDump.isPresent(), "Результат должен быть пустым");
    }

    /**
     * Тест получения всех дампов
     * Проверяет, что метод findAll возвращает все сохраненные дампы
     */
    @Test
    void findAll_ShouldReturnAllDumps() {
        // Создаем и сохраняем второй дамп
        Dump secondDump = new Dump();
        secondDump.setMac("11:22:33:44:55:66");
        secondDump.setIp("192.168.1.3");
        secondDump.setData("Second dump data");
        entityManager.persist(secondDump);
        entityManager.flush();
        
        // Получаем все дампы
        List<Dump> dumps = dumpRepository.findAll();
        
        // Проверяем результаты
        assertNotNull(dumps, "Список дампов не должен быть null");
        assertTrue(dumps.size() >= 2, "Должно быть найдено как минимум 2 дампа");
        assertTrue(dumps.stream().anyMatch(d -> d.getId().equals(testDump.getId())), 
            "Список должен содержать первый тестовый дамп");
        assertTrue(dumps.stream().anyMatch(d -> d.getId().equals(secondDump.getId())), 
            "Список должен содержать второй тестовый дамп");
    }

    /**
     * Тест удаления дампа
     * Проверяет, что дамп корректно удаляется из базы данных
     */
    @Test
    void delete_ShouldRemoveDump() {
        // Удаляем дамп
        dumpRepository.delete(testDump);
        
        // Проверяем, что дамп больше не существует в базе данных
        Dump deletedDump = entityManager.find(Dump.class, testDump.getId());
        assertNull(deletedDump, "Дамп должен быть удален из базы данных");
    }

    /**
     * Тест проверки существования дампа
     * Проверяет, что метод existsById корректно определяет наличие дампа
     */
    @Test
    void existsById_ShouldReturnCorrectResult() {
        // Проверяем существование сохраненного дампа
        boolean exists = dumpRepository.existsById(testDump.getId());
        assertTrue(exists, "Метод должен вернуть true для существующего дампа");
        
        // Проверяем несуществующий дамп
        boolean notExists = dumpRepository.existsById(999);
        assertFalse(notExists, "Метод должен вернуть false для несуществующего дампа");
    }

    /**
     * Тест автоматического заполнения даты создания
     * Проверяет, что поле dateCreated автоматически заполняется при создании дампа
     */
    @Test
    void save_ShouldSetDateCreated() {
        // Создаем новый дамп
        Dump newDump = new Dump();
        newDump.setMac("FF:EE:DD:CC:BB:AA");
        newDump.setIp("192.168.1.4");
        newDump.setData("Test date creation");
        
        // Сохраняем дамп
        Dump savedDump = dumpRepository.save(newDump);
        
        // Проверяем, что дата создания установлена
        assertNotNull(savedDump.getDateCreated(), "Дата создания не должна быть null");
        assertTrue(savedDump.getDateCreated().isBefore(LocalDateTime.now().plusSeconds(1)), 
            "Дата создания должна быть в прошлом");
    }
}
