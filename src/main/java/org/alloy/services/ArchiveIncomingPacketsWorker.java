package org.alloy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Воркер для обработки входящих пакетов в стиле archive проекта
 * Реализует параллельную обработку с семафором как в оригинальном C# коде
 */
@Service
public class ArchiveIncomingPacketsWorker {
    
    private static final Logger log = LoggerFactory.getLogger(ArchiveIncomingPacketsWorker.class);
    
    // Максимальное количество одновременных задач (как в archive проекте)
    private static final int MAX_CONCURRENCY = 50;
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ExecutorService executorService;
    private Semaphore concurrencySemaphore;
    
    @Autowired
    private ArchiveStylePacketParser packetParser;
    
    @PostConstruct
    public void start() {
        System.out.println("[ARCHIVE-PACKETS-WORKER] 🚀 Запуск воркера обработки входящих пакетов (параллельная обработка)");
        log.info("[ARCHIVE-PACKETS-WORKER] Запуск воркера с максимальной параллельностью: {}", MAX_CONCURRENCY);
        
        // Создаем семафор для контроля параллельности (как в archive проекте)
        concurrencySemaphore = new Semaphore(MAX_CONCURRENCY);
        
        // Создаем пул потоков для обработки пакетов
        executorService = Executors.newCachedThreadPool();
        
        // Запускаем основной поток обработки пакетов
        executorService.submit(this::processPacketsLoop);
    }
    
    /**
     * Основной цикл обработки пакетов (точно как в archive проекте)
     */
    private void processPacketsLoop() {
        log.info("[ARCHIVE-PACKETS-WORKER] Основной цикл обработки пакетов запущен (параллельная обработка)");
        
        do {
            // Проверяем очередь (как в archive проекте)
            ArchivePacket packet;
            while ((packet = ArchiveIncomingPacketsQueue.tryDequeue()) != null) {
                // Создаем финальную копию для использования в лямбде
                final ArchivePacket finalPacket = packet;
                
                // Получаем разрешение семафора
                try {
                    concurrencySemaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("[ARCHIVE-PACKETS-WORKER] Поток обработки пакетов прерван");
                    return;
                }
                
                // Запускаем обработку пакета в отдельной задаче (как Task.Factory.StartNew в C#)
                executorService.submit(() -> {
                    try {
                        // Используем Spring bean парсер (как в archive проекте)
                        packetParser.processPacket(finalPacket);
                    } catch (Exception e) {
                        log.error("[ARCHIVE-PACKETS-WORKER] Ошибка обработки пакета от {}: {}", 
                                finalPacket.getMac(), e.getMessage(), e);
                    } finally {
                        // Освобождаем семафор
                        concurrencySemaphore.release();
                    }
                });
            }
            
            // Минимальная задержка (как m_exit.WaitOne(1) в C#)
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("[ARCHIVE-PACKETS-WORKER] Поток обработки пакетов прерван");
                return;
            }
            
        } while (running.get());
        
        log.info("[ARCHIVE-PACKETS-WORKER] Основной цикл обработки пакетов завершен");
    }
    
    
    /**
     * Получить статистику воркера
     */
    public java.util.Map<String, Object> getWorkerStatistics() {
        return java.util.Map.of(
            "running", running.get(),
            "maxConcurrency", MAX_CONCURRENCY,
            "availablePermits", concurrencySemaphore != null ? concurrencySemaphore.availablePermits() : 0,
            "queueSize", ArchiveIncomingPacketsQueue.size(),
            "queueEmpty", ArchiveIncomingPacketsQueue.isEmpty(),
            "threadPoolActive", executorService != null ? !executorService.isShutdown() : false
        );
    }
    
    @PreDestroy
    public void stop() {
        running.set(false);
        
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
