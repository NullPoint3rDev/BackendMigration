package org.alloy.services;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Очередь входящих пакетов в стиле archive проекта
 */
public class ArchiveIncomingPacketsQueue {
    
    private static final Logger log = LoggerFactory.getLogger(ArchiveIncomingPacketsQueue.class);
    private static final ConcurrentLinkedQueue<ArchivePacket> queue = new ConcurrentLinkedQueue<>();
    
    /**
     * Добавить пакет в очередь
     */
    public static void enqueue(ArchivePacket packet) {
        if (packet != null) {
            queue.add(packet);
            log.debug("[ARCHIVE-QUEUE] Пакет добавлен в очередь: MAC={}, IP={}, размер очереди={}", 
                    packet.getMac(), packet.getIp(), queue.size());
        }
    }
    
    /**
     * Извлечь пакет из очереди (неблокирующий)
     */
    public static ArchivePacket tryDequeue() {
        ArchivePacket packet = queue.poll();
        if (packet != null) {
            log.debug("[ARCHIVE-QUEUE] Пакет извлечен из очереди: MAC={}, IP={}, размер очереди={}", 
                    packet.getMac(), packet.getIp(), queue.size());
        }
        return packet;
    }
    
    /**
     * Получить размер очереди
     */
    public static int size() {
        return queue.size();
    }
    
    /**
     * Проверить, пуста ли очередь
     */
    public static boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Очистить очередь
     */
    public static void clear() {
        queue.clear();
    }
}
