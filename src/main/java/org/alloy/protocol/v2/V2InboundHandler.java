package org.alloy.protocol.v2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alloy.protocol.v2.V2PacketReader.bytesToHex;
import static org.alloy.protocol.v2.V2PacketReader.macToHex;
import static org.alloy.protocol.v2.V2PacketReader.readU16BE;

/**
 * Точка входа v2: хвост сокета → split → CRC → dispatch → ответ.
 */
public class V2InboundHandler {
    private static final Logger log = LoggerFactory.getLogger(V2InboundHandler.class);

    private final V2FrameSplitter splitter = new V2FrameSplitter();
    private final V2PacketReader reader = new V2PacketReader();
    private final V2SessionStore store;
    private final V2SyncHandler syncHandler;
    private final V2StateHandler stateHandler;
    private final V2SessionInfoHandler sessionInfoHandler;
    private final V2SetHistorySessionHandler setHistSessionHandler;
    private final V2HistoryRecordHandler historyRecordHandler;
    private final V2DebugHub debugHub;

    public V2InboundHandler(
            V2SessionStore store,
            V2TokenService tokens,
            V2OutboundBuilder out,
            V2GapService gap,
            V2TelemetrySink telemetry,
            V2IndexService indexService,
            V2CommandQueue commands,
            V2DebugHub debugHub) {
        this.store = store;
        this.debugHub = debugHub;
        this.syncHandler = new V2SyncHandler(store, tokens, out, commands);
        this.stateHandler = new V2StateHandler(store, out, gap, telemetry, indexService, commands);
        this.sessionInfoHandler = new V2SessionInfoHandler(store, out, commands);
        this.setHistSessionHandler = new V2SetHistorySessionHandler(store, out, commands);
        this.historyRecordHandler = new V2HistoryRecordHandler(store, out, gap, telemetry, indexService, commands);
    }

    /** Для unit-тестов без Spring/БД. */
    public V2InboundHandler() {
        this(new V2SessionStore(), new V2TokenService(), new V2OutboundBuilder(), new V2GapService(),
                null, null, null, null);
    }

    public V2SessionStore getStore() {
        return store;
    }

    public void onBytes(V2ConnectionState state, byte[] chunk, OutputStream socketOut) throws IOException {
        if (chunk == null || chunk.length == 0) {
            return;
        }

        byte[] buf = concat(state.tail, chunk);
        V2FrameSplitter.SplitResult split = splitter.split(buf);
        state.tail = split.remainder;

        for (byte[] one : split.frames) {
            V2Frame frame = reader.read(one);
            if (!frame.crcOk) {
                log.warn("[V2] CRC fail type={} len={}", one.length > 0 ? one[0] : -1, one.length);
                continue;
            }

            String macHint = resolveMacHint(state, frame);
            if (debugHub != null) {
                debugHub.publish(new V2DebugEvent("IN", macHint, bytesToHex(one), V2FrameJson.fromInbound(frame)));
            }

            byte[] response = dispatch(frame);
            if (response != null) {
                if (frame.type == V2ProtocolConstants.TYPE_SYNC && frame.payload != null && frame.payload.length >= 6) {
                    state.mac = macToHex(Arrays.copyOfRange(frame.payload, 0, 6));
                    state.active = V2ProtocolConstants.isTestMac(state.mac);
                } else if (macHint != null && !macHint.isEmpty()) {
                    state.mac = macHint;
                    state.active = V2ProtocolConstants.isTestMac(macHint);
                }

                if (debugHub != null) {
                    debugHub.publish(new V2DebugEvent(
                            "OUT",
                            state.mac != null ? state.mac : macHint,
                            bytesToHex(response),
                            V2FrameJson.fromOutbound(response)));
                }
                if (socketOut != null) {
                    socketOut.write(response);
                    socketOut.flush();
                }
            }
        }
    }

    /**
     * Можно ли отдать этот chunk в v2 (только TEST_MAC).
     * Не активирует сокет для чужих MAC.
     */
    public boolean shouldHandleAsV2(V2ConnectionState state, byte[] chunk) {
        if (state != null && state.active) {
            return true;
        }
        if (chunk == null || chunk.length < 3 || chunk[0] == ':') {
            return false;
        }
        byte[] buf = concat(state != null ? state.tail : new byte[0], chunk);
        V2FrameSplitter.SplitResult split = splitter.split(buf);
        for (byte[] one : split.frames) {
            V2Frame frame = reader.read(one);
            if (!frame.crcOk) {
                continue;
            }
            if (frame.type == V2ProtocolConstants.TYPE_SYNC
                    && frame.payload != null
                    && frame.payload.length >= 6) {
                String mac = macToHex(Arrays.copyOfRange(frame.payload, 0, 6));
                return V2ProtocolConstants.isTestMac(mac);
            }
            if ((frame.type == V2ProtocolConstants.TYPE_STATE
                    || frame.type == V2ProtocolConstants.TYPE_HISTORY_RECORD
                    || frame.type == V2ProtocolConstants.TYPE_SESSION_INFO
                    || frame.type == V2ProtocolConstants.TYPE_SET_HIST_SESSION)
                    && frame.payload != null
                    && frame.payload.length >= 2) {
                int token = readU16BE(frame.payload, 0);
                V2Session s = store.getByToken(token);
                return s != null && V2ProtocolConstants.isTestMac(s.mac);
            }
        }
        return false;
    }

    private String resolveMacHint(V2ConnectionState state, V2Frame frame) {
        if (state != null && state.mac != null && !state.mac.isEmpty()) {
            return state.mac;
        }
        if (frame.type == V2ProtocolConstants.TYPE_SYNC
                && frame.payload != null
                && frame.payload.length >= 6) {
            return macToHex(Arrays.copyOfRange(frame.payload, 0, 6));
        }
        if (frame.payload != null && frame.payload.length >= 2) {
            V2Session s = store.getByToken(readU16BE(frame.payload, 0));
            if (s != null) {
                return s.mac;
            }
        }
        return V2ProtocolConstants.TEST_MAC;
    }

    private byte[] dispatch(V2Frame frame) {
        return switch (frame.type) {
            case V2ProtocolConstants.TYPE_SYNC -> syncHandler.handle(frame);
            case V2ProtocolConstants.TYPE_STATE -> stateHandler.handle(frame);
            case V2ProtocolConstants.TYPE_SESSION_INFO -> sessionInfoHandler.handle(frame);
            case V2ProtocolConstants.TYPE_SET_HIST_SESSION -> setHistSessionHandler.handle(frame);
            case V2ProtocolConstants.TYPE_HISTORY_RECORD -> historyRecordHandler.handle(frame);
            default -> {
                log.warn("[V2] unknown type 0x{}", Integer.toHexString(frame.type & 0xFF));
                yield null;
            }
        };
    }

    private static byte[] concat(byte[] a, byte[] b) {
        if (a == null || a.length == 0) {
            return Arrays.copyOf(b, b.length);
        }
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
