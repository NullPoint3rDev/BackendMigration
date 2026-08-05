package org.alloy.protocol.v2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Service;

import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

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

    /** Poll + если уходит 0x05 — привязать historySession к сессии из команды. */
    public V2HistoryCommand poll(String mac, V2Session session) {
        V2HistoryCommand cmd = poll(mac);
        bindHistorySession(cmd, session);
        return cmd;
    }

    static void bindHistorySession(V2HistoryCommand cmd, V2Session session) {
        if (cmd == null || session == null || cmd.bytes == null || cmd.bytes.length < 5) {
            return;
        }
        if ((cmd.bytes[0] & 0xFF) != (V2ProtocolConstants.TYPE_REQ_HISTORY & 0xFF)) {
            return;
        }
        int sess = readU32BE(cmd.bytes, 1);
        if (session.historySession != sess) {
            session.historySession = sess;
            session.lastHistoryIndex = -1;
        }
    }

    public int pendingCount(String mac) {
        ConcurrentLinkedQueue<V2HistoryCommand> q = byMac.get(mac);
        return q == null ? 0 : q.size();
    }
}
