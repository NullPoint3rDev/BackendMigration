package org.alloy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Воркер для обработки входящих пакетов в стиле archive проекта
 */
@Service
public class ArchiveIncomingPacketsWorker {
    
    private static final Logger log = LoggerFactory.getLogger(ArchiveIncomingPacketsWorker.class);
    
    private volatile boolean running = true;
    private ScheduledExecutorService executorService;
    
    @Autowired
    private ArchiveStylePacketParser packetParser;
    
    @PostConstruct
    public void start() {
        System.out.println("[ARCHIVE-PACKETS-WORKER] 🚀 Запуск воркера обработки входящих пакетов");
        log.info("[ARCHIVE-PACKETS-WORKER] Запуск воркера обработки входящих пакетов");
        
        // Создаем пул потоков для обработки пакетов
        executorService = Executors.newScheduledThreadPool(2);
        
        // Запускаем основной поток обработки пакетов
        executorService.submit(this::processPacketsLoop);
        
        // Запускаем поток для периодической очистки очереди
        executorService.scheduleWithFixedDelay(this::cleanupQueue, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Основной цикл обработки пакетов
     */
    private void processPacketsLoop() {
        log.info("[ARCHIVE-PACKETS-WORKER] Основной цикл обработки пакетов запущен");
        
        while (running) {
            try {
                // Извлекаем пакет из очереди
                ArchivePacket packet = ArchiveIncomingPacketsQueue.tryDequeue();
                
                if (packet != null) {
                    // Обрабатываем пакет
                    processPacket(packet);
                } else {
                    // Если очередь пуста, ждем немного
                    Thread.sleep(100);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("[ARCHIVE-PACKETS-WORKER] Поток обработки пакетов прерван");
                break;
            } catch (Exception e) {
                log.error("[ARCHIVE-PACKETS-WORKER] Ошибка в цикле обработки пакетов", e);
                try {
                    Thread.sleep(1000); // Ждем секунду при ошибке
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("[ARCHIVE-PACKETS-WORKER] Основной цикл обработки пакетов завершен");
    }
    
    /**
     * Обработка отдельного пакета
     */
    private void processPacket(ArchivePacket packet) {
        try {
            log.debug("[ARCHIVE-PACKETS-WORKER] Обработка пакета от {}: {}", 
                    packet.getMac(), packet.getData());
            
            // Передаем пакет в парсер
            packetParser.processPacket(packet);
            
            log.debug("[ARCHIVE-PACKETS-WORKER] Пакет от {} обработан успешно", packet.getMac());
            
        } catch (Exception e) {
            log.error("[ARCHIVE-PACKETS-WORKER] Ошибка обработки пакета от {}: {}", 
                    packet.getMac(), e.getMessage(), e);
        }
    }
    
    /**
     * Периодическая очистка очереди
     */
    private void cleanupQueue() {
        try {
            int queueSize = ArchiveIncomingPacketsQueue.size();
            if (queueSize > 1000) {
                log.warn("[ARCHIVE-PACKETS-WORKER] Очередь переполнена: {} пакетов", queueSize);
                // Можно добавить логику очистки старых пакетов
            }
            
            log.debug("[ARCHIVE-PACKETS-WORKER] Размер очереди: {} пакетов", queueSize);
            
        } catch (Exception e) {
            log.error("[ARCHIVE-PACKETS-WORKER] Ошибка очистки очереди", e);
        }
    }
    
    /**
     * Получить статистику воркера
     */
    public java.util.Map<String, Object> getWorkerStatistics() {
        return java.util.Map.of(
            "running", running,
            "queueSize", ArchiveIncomingPacketsQueue.size(),
            "queueEmpty", ArchiveIncomingPacketsQueue.isEmpty(),
            "threadPoolActive", executorService != null ? executorService.toString() : "null"
        );
    }
    
    @PreDestroy
    public void stop() {
        running = false;
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("[ARCHIVE-PACKETS-WORKER] 🔚 Воркер обработки пакетов остановлен");
        log.info("[ARCHIVE-PACKETS-WORKER] Воркер остановлен");
    }
}
