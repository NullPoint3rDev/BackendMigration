package org.alloy.protocol.v2;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alloy.protocol.v2.V2PacketReader.readU16BE;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

public class V2HistoryRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(V2HistoryRecordHandler.class);

    private final V2SessionStore store;
    private final V2OutboundBuilder out;
    private final V2GapService gapService;
    private final V2TelemetrySink telemetry;
    private final V2IndexService indexService;
    private final V2CommandQueue commands;

    public V2HistoryRecordHandler(
            V2SessionStore store,
            V2OutboundBuilder out,
            V2GapService gapService,
            V2TelemetrySink telemetry,
            V2IndexService indexService,
            V2CommandQueue commands) {
        this.store = store;
        this.out = out;
        this.gapService = gapService;
        this.telemetry = telemetry;
        this.indexService = indexService;
        this.commands = commands;
    }

    public byte[] handle(V2Frame frame) {
        byte[] p = frame.payload;
        if (p == null || p.length < 2 + 4) {
            log.warn("[V2] history-record payload too short");
            return null;
        }

        int token = readU16BE(p, 0);
        V2Session s = store.getByToken(token);
        if (s == null) {
            log.warn("[V2] history-record invalid token {}", token);
            return null;
        }

        byte[] wtinfo = Arrays.copyOfRange(p, 2, p.length);
        int index = readU32BE(wtinfo, 0);

        if (telemetry != null) {
            try {
                telemetry.onTelemetry(s.mac, wtinfo);
            } catch (Exception e) {
                log.warn("[V2] telemetry sink failed mac={}: {}", s.mac, e.getMessage());
            }
        }

        int lastInDb = indexService != null
                ? indexService.getLastIndex(s.mac, s.historySession, V2IndexService.CHANNEL_HISTORY)
                : s.lastHistoryIndex;

        V2HistoryCommand gap = gapService.detectGap(s.historySession, lastInDb, index);
        V2HistoryCommand cmd = commands != null ? commands.poll(s.mac) : null;
        if (cmd == null) {
            cmd = gap;
        }

        if (indexService != null) {
            indexService.saveLastIndex(s.mac, s.historySession, V2IndexService.CHANNEL_HISTORY, index);
        }
        s.lastHistoryIndex = index;

        log.debug("[V2] history mac={} session={} index={}", s.mac, s.historySession, index);
        return out.historyRecordAck(index, cmd);
    }
}
