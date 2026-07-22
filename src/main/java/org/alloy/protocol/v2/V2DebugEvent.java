package org.alloy.protocol.v2;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Событие для тестовой страницы (poll). */
public class V2DebugEvent {
    private static final AtomicLong SEQ = new AtomicLong(1);

    public final long id;
    public final long tsEpochMs;
    public final String direction; // IN | OUT
    public final String mac;
    public final String rawHex;
    public final Map<String, Object> json;

    public V2DebugEvent(String direction, String mac, String rawHex, Map<String, Object> json) {
        this.id = SEQ.getAndIncrement();
        this.tsEpochMs = Instant.now().toEpochMilli();
        this.direction = direction;
        this.mac = mac;
        this.rawHex = rawHex;
        this.json = json != null ? json : new LinkedHashMap<>();
    }
}
