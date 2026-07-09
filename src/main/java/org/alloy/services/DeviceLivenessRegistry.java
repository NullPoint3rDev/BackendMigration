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
        if (DeviceModelService.isDebugMac(mac)) {
            return DeviceModelService.DEBUG_MAC;
        }
        String norm = mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        return norm.isEmpty() ? null : norm;
    }

    public void markSeen(String mac) {
        String norm = normalize(mac);
        if (norm != null && !DeviceModelService.DEBUG_MAC.equals(norm)) {
            lastSeenMsByMac.put(norm, System.currentTimeMillis());
        }
    }

    /** Время последней посылки от MAC (epoch ms) или null, если аппарат ни разу не появлялся. */
    public Long getLastSeenMs(String mac) {
        String norm = normalize(mac);
        if (norm == null) return null;
        // Отладочный MAC всегда «онлайн» — проверка соединения в модалке проходит сразу.
        if (DeviceModelService.DEBUG_MAC.equals(norm)) {
            return System.currentTimeMillis();
        }
        return lastSeenMsByMac.get(norm);
    }
}
