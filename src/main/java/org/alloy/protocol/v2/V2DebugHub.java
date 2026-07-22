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
        List<V2DebugEvent> out = new ArrayList<>();
        for (V2DebugEvent e : events) {
            if (e.id > afterId) {
                out.add(e);
            }
        }
        return out;
    }
}
