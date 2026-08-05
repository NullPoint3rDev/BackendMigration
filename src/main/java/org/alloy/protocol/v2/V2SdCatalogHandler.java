package org.alloy.protocol.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alloy.protocol.v2.V2PacketReader.readU16BE;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

/**
 * 0x0C — каталог сессий на SD: token | firstSession | lastSession.
 */
public class V2SdCatalogHandler {
    private static final Logger log = LoggerFactory.getLogger(V2SdCatalogHandler.class);
    private static final int PAYLOAD_LEN = 2 + 4 + 4;

    private final V2SessionStore store;
    private final V2OutboundBuilder out;
    private final V2CommandQueue commands;

    public V2SdCatalogHandler(V2SessionStore store, V2OutboundBuilder out, V2CommandQueue commands) {
        this.store = store;
        this.out = out;
        this.commands = commands;
    }

    public byte[] handle(V2Frame frame) {
        byte[] p = frame.payload;
        if (p == null || p.length < PAYLOAD_LEN) {
            log.warn("[V2] sd-catalog payload too short: {}", p == null ? -1 : p.length);
            return null;
        }

        int token = readU16BE(p, 0);
        V2Session s = store.getByToken(token);
        if (s == null) {
            log.warn("[V2] sd-catalog invalid token {}", token);
            return out.error(V2ProtocolConstants.ERR_INVALID_TOKEN, null);
        }

        int firstSession = readU32BE(p, 2);
        int lastSession = readU32BE(p, 6);
        if (firstSession > lastSession) {
            log.warn("[V2] sd-catalog mac={} bad range {}..{}", s.mac, firstSession, lastSession);
            V2HistoryCommand cmd = commands != null ? commands.poll(s.mac, s) : null;
            return out.sdCatalogAck(firstSession, lastSession, cmd);
        }

        s.sdFirstSession = firstSession;
        s.sdLastSession = lastSession;

        V2HistoryCommand pending = commands != null ? commands.poll(s.mac, s) : null;
        V2HistoryCommand walkFirst = V2SdRecover.begin(commands, s.mac, firstSession, lastSession);
        V2HistoryCommand cmd;
        if (pending != null) {
            cmd = pending;
            if (walkFirst != null && commands != null) {
                commands.enqueue(s.mac, walkFirst);
            }
        } else {
            cmd = walkFirst;
        }

        log.info("[V2] sd-catalog mac={} firstSession={} lastSession={} walkQueued",
                s.mac, firstSession, lastSession);
        return out.sdCatalogAck(firstSession, lastSession, cmd);
    }
}
