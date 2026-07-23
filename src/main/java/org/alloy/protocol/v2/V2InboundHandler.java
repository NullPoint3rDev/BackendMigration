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
 * Роутинг снаружи: первый байт 0x01 + version 0x02 (старый путь ':' сюда не попадает).
 */
public class V2InboundHandler {
    private static final Logger log = LoggerFactory.getLogger(V2InboundHandler.class);
    private static final int SYNC_PAYLOAD_MIN = 1 + 6 + 1 + 4;

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
                if (frame.type == V2ProtocolConstants.TYPE_SYNC
                        && frame.payload != null
                        && frame.payload.length >= SYNC_PAYLOAD_MIN) {
                    state.mac = macToHex(Arrays.copyOfRange(frame.payload, 1, 7));
                    state.active = V2ProtocolConstants.isSupportedProtocolVersion(frame.payload[0]);
                } else if (macHint != null && !macHint.isEmpty()) {
                    state.mac = macHint;
                    if (!state.active) {
                        V2Session s = store.getByToken(
                                frame.payload != null && frame.payload.length >= 2
                                        ? readU16BE(frame.payload, 0) : -1);
                        state.active = s != null
                                && V2ProtocolConstants.isSupportedProtocolVersion(s.protocolVersion);
                    }
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
     * Отдать chunk в v2?
     * — уже active / есть хвост v2
     * — или первый байт 0x01 и (неполный кадр | version==0x02)
     * ':' никогда не сюда.
     */
    public boolean shouldHandleAsV2(V2ConnectionState state, byte[] chunk) {
        if (state != null && state.active) {
            return true;
        }
        if (state != null && state.tail != null && state.tail.length > 0) {
            return true;
        }
        if (chunk == null || chunk.length == 0 || chunk[0] == ':') {
            return false;
        }
        // Новые устройства начинают с sync 0x01; иначе — не наш протокол
        if (chunk[0] != V2ProtocolConstants.TYPE_SYNC) {
            return false;
        }

        byte[] buf = concat(state != null ? state.tail : new byte[0], chunk);
        V2FrameSplitter.SplitResult split = splitter.split(buf);
        if (split.frames.isEmpty()) {
            // неполный sync — буферизуем в v2, не отдаём в ASCII
            return true;
        }

        for (byte[] one : split.frames) {
            V2Frame frame = reader.read(one);
            if (!frame.crcOk) {
                continue;
            }
            if (frame.type == V2ProtocolConstants.TYPE_SYNC
                    && frame.payload != null
                    && frame.payload.length >= SYNC_PAYLOAD_MIN) {
                return V2ProtocolConstants.isSupportedProtocolVersion(frame.payload[0]);
            }
            if (frame.payload != null && frame.payload.length >= 2) {
                V2Session s = store.getByToken(readU16BE(frame.payload, 0));
                return s != null && V2ProtocolConstants.isSupportedProtocolVersion(s.protocolVersion);
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
                && frame.payload.length >= SYNC_PAYLOAD_MIN) {
            return macToHex(Arrays.copyOfRange(frame.payload, 1, 7));
        }
        if (frame.payload != null && frame.payload.length >= 2) {
            V2Session s = store.getByToken(readU16BE(frame.payload, 0));
            if (s != null) {
                return s.mac;
            }
        }
        return "";
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
