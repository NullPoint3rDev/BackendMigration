package org.alloy.protocol.v2;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alloy.protocol.v2.V2PacketReader.macToHex;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

/**
 * Sync 0x01 inbound payload:
 * version(1) | MAC(6) | deviceType(1) | session(4)
 */
public class V2SyncHandler {
    private static final Logger log = LoggerFactory.getLogger(V2SyncHandler.class);
    private static final int SYNC_PAYLOAD_LEN = 1 + 6 + 1 + 4;

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
        if (p == null || p.length < SYNC_PAYLOAD_LEN) {
            log.warn("[V2] sync payload too short: {}", p == null ? -1 : p.length);
            return null;
        }

        byte version = p[0];
        if (!V2ProtocolConstants.isSupportedProtocolVersion(version)) {
            log.warn("[V2] unsupported protocol version 0x{}", Integer.toHexString(version & 0xFF));
            return null;
        }

        byte[] mac6 = Arrays.copyOfRange(p, 1, 7);
        String mac = macToHex(mac6);
        byte deviceType = p[7];
        int session = readU32BE(p, 8);
        int token = tokens.nextToken();

        V2Session s = new V2Session();
        s.mac = mac;
        s.protocolVersion = version;
        s.deviceType = deviceType;
        s.sessionNumber = session;
        s.historySession = session;
        s.token = token;
        store.put(s);

        V2HistoryCommand cmd = commands != null ? commands.poll(mac) : null;
        log.info("[V2] sync mac={} version={} session={} token={}", mac, version & 0xFF, session, token);
        return out.syncResponse(version, mac6, deviceType, session, token, cmd);
    }
}
