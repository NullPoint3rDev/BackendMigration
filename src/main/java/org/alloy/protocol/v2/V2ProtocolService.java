package org.alloy.protocol.v2;

import java.io.IOException;
import java.io.OutputStream;

import org.alloy.services.WeldingDeviceManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.alloy.protocol.v2.V2PacketReader.bytesToHex;

/**
 * Spring-обёртка v2: телеметрия в deviceManager, debug hub, индексы, команды.
 */
@Service
public class V2ProtocolService {
    private static final Logger log = LoggerFactory.getLogger(V2ProtocolService.class);

    private final V2InboundHandler inbound;
    private final V2CommandQueue commandQueue;
    private final V2DebugHub debugHub;

    public V2ProtocolService(
            WeldingDeviceManagerService deviceManager,
            V2IndexService indexService,
            V2CommandQueue commandQueue,
            V2DebugHub debugHub) {
        this.commandQueue = commandQueue;
        this.debugHub = debugHub;
        V2TelemetrySink sink = (mac, wtinfo) ->
                deviceManager.processDeviceData(":" + mac + ";" + bytesToHex(wtinfo), mac);
        this.inbound = new V2InboundHandler(
                new V2SessionStore(),
                new V2TokenService(),
                new V2OutboundBuilder(),
                new V2GapService(),
                sink,
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

    public V2CommandQueue getCommandQueue() {
        return commandQueue;
    }

    public V2DebugHub getDebugHub() {
        return debugHub;
    }
}
