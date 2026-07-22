package org.alloy.protocol.v2;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.alloy.protocol.v2.V2PacketReader.bytesToHex;

/**
 * Protocol v2 + перехват TEST_MAC.
 * Телеметрию в боевой deviceManager НЕ пишем — только debug hub / тестовая страница.
 */
@Service
public class V2ProtocolService {
    private static final Logger log = LoggerFactory.getLogger(V2ProtocolService.class);

    private final V2InboundHandler inbound;
    private final V2CommandQueue commandQueue;
    private final V2DebugHub debugHub;

    public V2ProtocolService(
            V2IndexService indexService,
            V2CommandQueue commandQueue,
            V2DebugHub debugHub) {
        this.commandQueue = commandQueue;
        this.debugHub = debugHub;
        // ponytail: null sink — TEST_MAC не кормит боевой мониторинг
        this.inbound = new V2InboundHandler(
                new V2SessionStore(),
                new V2TokenService(),
                new V2OutboundBuilder(),
                new V2GapService(),
                null,
                indexService,
                commandQueue,
                debugHub);
        log.info("[V2] protocol service ready, test MAC={}", V2ProtocolConstants.TEST_MAC);
    }

    public boolean shouldHandleAsV2(V2ConnectionState state, byte[] chunk) {
        return inbound.shouldHandleAsV2(state, chunk);
    }

    public void onBytes(V2ConnectionState state, byte[] chunk, OutputStream out) throws IOException {
        inbound.onBytes(state, chunk, out);
    }

    /**
     * Пока плата шлёт старый ASCII Core (:MAC;hex) — показываем на тестовой странице,
     * в боевой пайплайн не пускаем.
     */
    public void publishLegacyAscii(String mac, String asciiFrame, String direction) {
        if (asciiFrame == null) {
            return;
        }
        byte[] bytes = asciiFrame.getBytes(StandardCharsets.US_ASCII);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("format", "legacy_ascii_core");
        json.put("ascii", asciiFrame);
        int semi = asciiFrame.indexOf(';');
        if (semi >= 0 && semi + 1 < asciiFrame.length()) {
            String payload = asciiFrame.substring(semi + 1).trim();
            json.put("payloadHex", payload);
            json.put("looksLikeTimeSync", payload.contains("01010131") || payload.startsWith("010101"));
        }
        debugHub.publish(new V2DebugEvent(
                direction == null ? "IN" : direction,
                mac != null ? mac : V2ProtocolConstants.TEST_MAC,
                bytesToHex(bytes),
                json));
    }

    public V2CommandQueue getCommandQueue() {
        return commandQueue;
    }

    public V2DebugHub getDebugHub() {
        return debugHub;
    }
}
