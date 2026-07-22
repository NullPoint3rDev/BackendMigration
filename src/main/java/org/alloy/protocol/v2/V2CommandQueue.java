package org.alloy.protocol.v2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Service;

/**
 * Очередь команд сервера → плата (одна на ответ).
 * UI кладёт команду; хендлер забирает при следующем ACK.
 */
@Service
public class V2CommandQueue {
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<V2HistoryCommand>> byMac =
            new ConcurrentHashMap<>();

    public void enqueue(String mac, V2HistoryCommand cmd) {
        if (mac == null || cmd == null) {
            return;
        }
        byMac.computeIfAbsent(mac, m -> new ConcurrentLinkedQueue<>()).add(cmd);
    }

    public V2HistoryCommand poll(String mac) {
        if (mac == null) {
            return null;
        }
        ConcurrentLinkedQueue<V2HistoryCommand> q = byMac.get(mac);
        return q == null ? null : q.poll();
    }

    public int pendingCount(String mac) {
        ConcurrentLinkedQueue<V2HistoryCommand> q = byMac.get(mac);
        return q == null ? 0 : q.size();
    }
}
