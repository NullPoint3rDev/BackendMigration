package org.alloy.services;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Репозиторий исходящих пакетов в стиле archive проекта
 */
public class ArchiveOutboundPacketsRepository {
    
    private static final ConcurrentHashMap<String, ArchivePacket> repository = new ConcurrentHashMap<>();
    
    /**
     * Установить пакет для MAC-адреса
     */
    public static void set(String mac, String data) {
        if (mac != null && data != null) {
            ArchivePacket packet = new ArchivePacket();
            packet.setMac(mac.toUpperCase());
            packet.setData(data);
            repository.put(mac.toUpperCase(), packet);
        }
    }
    
    /**
     * Установить пакет
     */
    public static void set(ArchivePacket packet) {
        if (packet != null && packet.getMac() != null) {
            repository.put(packet.getMac().toUpperCase(), packet);
        }
    }
    
    /**
     * Получить пакет по MAC-адресу (неблокирующий)
     */
    public static ArchivePacket tryGet(String mac) {
        if (mac == null) {
            return null;
        }
        return repository.get(mac.toUpperCase());
    }
    
    /**
     * Удалить пакет по MAC-адресу
     */
    public static void remove(String mac) {
        if (mac != null) {
            repository.remove(mac.toUpperCase());
        }
    }
    
    /**
     * Получить размер репозитория
     */
    public static int size() {
        return repository.size();
    }
    
    /**
     * Проверить, пуст ли репозиторий
     */
    public static boolean isEmpty() {
        return repository.isEmpty();
    }
    
    /**
     * Очистить репозиторий
     */
    public static void clear() {
        repository.clear();
    }
    
    /**
     * Проверить, содержит ли репозиторий пакет для MAC
     */
    public static boolean contains(String mac) {
        return mac != null && repository.containsKey(mac.toUpperCase());
    }
}
