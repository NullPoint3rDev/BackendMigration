package org.alloy.protocol.v2;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Service;

/**
 * Очередь команд сервера → плата (одна на ответ).
 * UI кладёт команду; хендлер забирает при следующем ACK.
 */
@Service
public class V2CommandQueue {
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<V2HistoryCommand>> byMac =
            new ConcurrentHashMap<>();

    /**
     * Кладёт команду в хвост, если такой же ещё нет в очереди.
     * Дедуп нужен из-за пачки sync подряд: каждый заводит обход SD и без него
     * очередь распухает в N раз, вытесняя восстановление дыр.
     * ponytail: сравнение линейным перебором, очередь порядка десятков команд.
     */
    public void enqueue(String mac, V2HistoryCommand cmd) {
        if (mac == null || cmd == null) {
            return;
        }
        ConcurrentLinkedDeque<V2HistoryCommand> q =
                byMac.computeIfAbsent(mac, m -> new ConcurrentLinkedDeque<>());
        synchronized (q) {
            for (V2HistoryCommand pending : q) {
                if (Arrays.equals(pending.bytes, cmd.bytes)) {
                    return;
                }
            }
            q.addLast(cmd);
        }
    }

    /** Восстановление пропущенных записей важнее обхода каталога — идёт в голову очереди. */
    public void enqueueFirst(String mac, V2HistoryCommand cmd) {
        if (mac == null || cmd == null) {
            return;
        }
        ConcurrentLinkedDeque<V2HistoryCommand> q =
                byMac.computeIfAbsent(mac, m -> new ConcurrentLinkedDeque<>());
        synchronized (q) {
            for (V2HistoryCommand pending : q) {
                if (Arrays.equals(pending.bytes, cmd.bytes)) {
                    return;
                }
            }
            q.addFirst(cmd);
        }
    }

    public V2HistoryCommand poll(String mac) {
        if (mac == null) {
            return null;
        }
        ConcurrentLinkedDeque<V2HistoryCommand> q = byMac.get(mac);
        return q == null ? null : q.poll();
    }

    public int pendingCount(String mac) {
        ConcurrentLinkedDeque<V2HistoryCommand> q = byMac.get(mac);
        return q == null ? 0 : q.size();
    }
}
