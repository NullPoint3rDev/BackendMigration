package org.alloy.protocol.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alloy.protocol.v2.V2PacketReader.readU16BE;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

public class V2SessionInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(V2SessionInfoHandler.class);

    private final V2SessionStore store;
    private final V2OutboundBuilder out;
    private final V2CommandQueue commands;

    public V2SessionInfoHandler(V2SessionStore store, V2OutboundBuilder out, V2CommandQueue commands) {
        this.store = store;
        this.out = out;
        this.commands = commands;
    }

    public byte[] handle(V2Frame frame) {
        byte[] p = frame.payload;
        if (p == null || p.length < 2 + 4 * 5) {
            log.warn("[V2] session-info payload too short");
            return null;
        }

        int token = readU16BE(p, 0);
        V2Session s = store.getByToken(token);
        if (s == null) {
            log.warn("[V2] session-info invalid token {}", token);
            return null;
        }

        int session = readU32BE(p, 2);
        int firstIdx = readU32BE(p, 6);
        int firstTime = readU32BE(p, 10);
        int lastIdx = readU32BE(p, 14);
        int lastTime = readU32BE(p, 18);

        log.info(
                "[V2] session-info mac={} session={} first={}/{} last={}/{}",
                s.mac, session, firstIdx, firstTime, lastIdx, lastTime);

        V2HistoryCommand cmd = commands != null ? commands.poll(s.mac) : null;
        return out.sessionInfoAck(session, cmd);
    }
}
