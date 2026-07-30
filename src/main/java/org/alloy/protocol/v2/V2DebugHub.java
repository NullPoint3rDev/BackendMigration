package org.alloy.protocol.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Service;

/** In-memory кольцо последних событий v2 для poll на тестовой странице. */
@Service
public class V2DebugHub {
    private static final int MAX = 500;
    private final ConcurrentLinkedDeque<V2DebugEvent> events = new ConcurrentLinkedDeque<>();

    public void publish(V2DebugEvent event) {
        events.addLast(event);
        while (events.size() > MAX) {
            events.pollFirst();
        }
    }

    /** События с id > afterId, новые первыми не сортируем — по порядку записи. */
    public List<V2DebugEvent> since(long afterId) {
        return since(afterId, null);
    }

    /** @param macFilter если не null/пусто — только события этого MAC (нормализованного). */
    public List<V2DebugEvent> since(long afterId, String macFilter) {
        String want = V2ProtocolConstants.normalizeMac(macFilter);
        List<V2DebugEvent> out = new ArrayList<>();
        for (V2DebugEvent e : events) {
            if (e.id <= afterId) {
                continue;
            }
            if (!want.isEmpty() && !want.equals(V2ProtocolConstants.normalizeMac(e.mac))) {
                continue;
            }
            out.add(e);
        }
        return out;
    }
}
