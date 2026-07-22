package org.alloy.protocol.v2;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alloy.protocol.v2.V2PacketReader.macToHex;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

public class V2SyncHandler {
    private static final Logger log = LoggerFactory.getLogger(V2SyncHandler.class);

    private final V2SessionStore store;
    private final V2TokenService tokens;
    private final V2OutboundBuilder out;
    private final V2CommandQueue commands;

    public V2SyncHandler(
            V2SessionStore store,
            V2TokenService tokens,
            V2OutboundBuilder out,
            V2CommandQueue commands) {
        this.store = store;
        this.tokens = tokens;
        this.out = out;
        this.commands = commands;
    }

    public byte[] handle(V2Frame frame) {
        byte[] p = frame.payload;
        if (p == null || p.length < 11) {
            log.warn("[V2] sync payload too short: {}", p == null ? -1 : p.length);
            return null;
        }

        byte[] mac6 = Arrays.copyOfRange(p, 0, 6);
        String mac = macToHex(mac6);
        if (!V2ProtocolConstants.isTestMac(mac)) {
            log.warn("[V2] sync from unexpected MAC {}", mac);
            return null;
        }

        byte deviceType = p[6];
        int session = readU32BE(p, 7);
        int token = tokens.nextToken();

        V2Session s = new V2Session();
        s.mac = mac;
        s.deviceType = deviceType;
        s.sessionNumber = session;
        s.historySession = session;
        s.token = token;
        store.put(s);

        V2HistoryCommand cmd = commands != null ? commands.poll(mac) : null;
        log.info("[V2] sync mac={} session={} token={}", mac, session, token);
        return out.syncResponse(mac6, deviceType, session, token, cmd);
    }
}
