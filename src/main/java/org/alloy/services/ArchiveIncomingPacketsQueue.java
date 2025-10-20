package org.alloy.services;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Очередь входящих пакетов в стиле archive проекта
 */
public class ArchiveIncomingPacketsQueue {
    
    private static final ConcurrentLinkedQueue<ArchivePacket> queue = new ConcurrentLinkedQueue<>();
    
    /**
     * Добавить пакет в очередь
     */
    public static void enqueue(ArchivePacket packet) {
        if (packet != null) {
            queue.add(packet);
        }
    }
    
    /**
     * Извлечь пакет из очереди (неблокирующий)
     */
    public static ArchivePacket tryDequeue() {
        return queue.poll();
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
