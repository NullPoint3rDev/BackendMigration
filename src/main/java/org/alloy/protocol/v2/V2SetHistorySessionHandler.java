package org.alloy.protocol.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alloy.protocol.v2.V2PacketReader.readU16BE;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

public class V2SetHistorySessionHandler {
    private static final Logger log = LoggerFactory.getLogger(V2SetHistorySessionHandler.class);

    private final V2SessionStore store;
    private final V2OutboundBuilder out;
    private final V2CommandQueue commands;

    public V2SetHistorySessionHandler(V2SessionStore store, V2OutboundBuilder out, V2CommandQueue commands) {
        this.store = store;
        this.out = out;
        this.commands = commands;
    }

    public byte[] handle(V2Frame frame) {
        byte[] p = frame.payload;
        if (p == null || p.length < 6) {
            log.warn("[V2] set-hist-session payload too short");
            return null;
        }

        int token = readU16BE(p, 0);
        V2Session s = store.getByToken(token);
        if (s == null) {
            log.warn("[V2] set-hist-session invalid token {}", token);
            return null;
        }

        int session = readU32BE(p, 2);
        s.historySession = session;
        s.lastHistoryIndex = -1;

        log.info("[V2] set-hist-session mac={} historySession={}", s.mac, session);
        V2HistoryCommand cmd = commands != null ? commands.poll(s.mac) : null;
        return out.setHistSessionAck(session, cmd);
    }
}
