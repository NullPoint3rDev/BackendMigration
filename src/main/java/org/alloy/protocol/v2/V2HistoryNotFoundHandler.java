package org.alloy.protocol.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alloy.protocol.v2.V2PacketReader.readU16BE;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

/**
 * 0x0A — историческая запись не найдена на SD: token(2) + index(4) BE.
 */
public class V2HistoryNotFoundHandler {
    private static final Logger log = LoggerFactory.getLogger(V2HistoryNotFoundHandler.class);

    private final V2SessionStore store;
    private final V2OutboundBuilder out;
    private final V2CommandQueue commands;

    public V2HistoryNotFoundHandler(V2SessionStore store, V2OutboundBuilder out, V2CommandQueue commands) {
        this.store = store;
        this.out = out;
        this.commands = commands;
    }

    public byte[] handle(V2Frame frame) {
        byte[] p = frame.payload;
        if (p == null || p.length < 2 + 4) {
            log.warn("[V2] history-not-found payload too short");
            return null;
        }

        int token = readU16BE(p, 0);
        V2Session s = store.getByToken(token);
        if (s == null) {
            log.warn("[V2] history-not-found invalid token {}", token);
            return out.error(V2ProtocolConstants.ERR_INVALID_TOKEN, null);
        }

        int index = readU32BE(p, 2);
        log.info("[V2] history-not-found mac={} session={} index={}", s.mac, s.historySession, index);

        V2HistoryCommand cmd = commands != null ? commands.poll(s.mac, s) : null;
        return out.historyNotFoundAck(index, cmd);
    }
}
