package org.alloy.services;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр «живости» устройств: когда MAC в последний раз стучался на сервер.
 * Используется для проверки подключения при добавлении оборудования (кнопка «Проверить соединение»).
 * Помечаем ЛЮБОЙ входящий MAC (в т.ч. ещё не зарегистрированный в БД), чтобы можно было проверить новый аппарат.
 * ponytail: in-memory, сбрасывается при рестарте — этого достаточно для «прямо сейчас онлайн».
 */
@Component
public class DeviceLivenessRegistry {

    private final ConcurrentHashMap<String, Long> lastSeenMsByMac = new ConcurrentHashMap<>();

    private static String normalize(String mac) {
        if (mac == null) return null;
        String norm = mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        return norm.isEmpty() ? null : norm;
    }

    public void markSeen(String mac) {
        String norm = normalize(mac);
        if (norm != null) {
            lastSeenMsByMac.put(norm, System.currentTimeMillis());
        }
    }

    /** Время последней посылки от MAC (epoch ms) или null, если аппарат ни разу не появлялся. */
    public Long getLastSeenMs(String mac) {
        String norm = normalize(mac);
        return norm == null ? null : lastSeenMsByMac.get(norm);
    }
}
