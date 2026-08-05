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
    private final V2IndexService indexService;

    public V2SessionInfoHandler(
            V2SessionStore store,
            V2OutboundBuilder out,
            V2CommandQueue commands,
            V2IndexService indexService) {
        this.store = store;
        this.out = out;
        this.commands = commands;
        this.indexService = indexService;
    }

    public byte[] handle(V2Frame frame) {
        byte[] p = frame.payload;
        // full: token(2)+session(4)+first/last idx+time (22);
        // short: token(2)+session(4) (6) — сессия не найдена на плате
        if (p == null || p.length < 6) {
            log.warn("[V2] session-info payload too short");
            return null;
        }

        int token = readU16BE(p, 0);
        V2Session s = store.getByToken(token);
        if (s == null) {
            log.warn("[V2] session-info invalid token {}", token);
            return out.error(V2ProtocolConstants.ERR_INVALID_TOKEN, null);
        }

        int session = readU32BE(p, 2);
        boolean found = p.length >= 22;
        if (found) {
            int firstIdx = readU32BE(p, 6);
            int firstTime = readU32BE(p, 10);
            int lastIdx = readU32BE(p, 14);
            int lastTime = readU32BE(p, 18);
            log.info(
                    "[V2] session-info mac={} session={} first={}/{} last={}/{}",
                    s.mac, session, firstIdx, firstTime, lastIdx, lastTime);
            enqueueRecover(s, session, firstIdx, lastIdx);
        } else {
            log.info("[V2] session-info mac={} session={} notFound", s.mac, session);
        }

        V2HistoryCommand cmd = commands != null ? commands.poll(s.mac) : null;
        return out.sessionInfoAck(session, cmd);
    }

    private void enqueueRecover(V2Session s, int session, int firstIdx, int lastIdx) {
        if (commands == null) {
            return;
        }
        int serverLast = -1;
        if (indexService != null) {
            int live = indexService.getLastIndex(s.mac, session, V2IndexService.CHANNEL_LIVE);
            int hist = indexService.getLastIndex(s.mac, session, V2IndexService.CHANNEL_HISTORY);
            serverLast = Math.max(live, hist);
        } else if (session == s.sessionNumber) {
            // in-memory last* относятся только к текущей live-сессии
            serverLast = Math.max(s.lastLiveIndex, s.lastHistoryIndex);
        }
        V2HistoryCommand recover = V2HistoryRecover.plan(session, serverLast, firstIdx, lastIdx);
        if (recover == null) {
            log.info("[V2] recover skip mac={} session={} serverLast={} device={}-{}",
                    s.mac, session, serverLast, firstIdx, lastIdx);
            return;
        }
        // historySession выставит плата пакетом 0x06 перед выгрузкой
        // вперёд оставшихся 0x03: реальная выгрузка важнее опроса каталога
        commands.enqueueFirst(s.mac, V2HistoryCommand.priorityHistory());
        commands.enqueueFirst(s.mac, recover);
        log.info("[V2] recover enqueue mac={} session={} serverLast={} → {}..{}",
                s.mac, session, serverLast, serverLast < 0 ? firstIdx : serverLast + 1, lastIdx);
    }
}
