package org.alloy.protocol.v2;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.alloy.protocol.v2.V2PacketReader.putU32BE;
import static org.alloy.protocol.v2.V2PacketReader.putU32LE;
import static org.alloy.protocol.v2.V2PacketReader.readU16BE;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ponytail: framing/CRC + sync(version) + gap 40→100 → 0x05(41..99).
 * Старый ASCII ':' не должен попадать в v2.
 */
public class V2ProtocolSelfCheck {

    @Test
    void oldAsciiColonIsNeverV2() {
        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        byte[] legacy = ":E072A1D43F18;01010131".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertFalse(inbound.shouldHandleAsV2(conn, legacy));
        assertFalse(conn.active);
    }

    /** Живой sync с платы 188.169.147.80 (NUL в session в логах выглядели как пробелы). */
    @Test
    void realDeviceSyncCrcAndRouting() throws Exception {
        byte[] sync = hex("010E02E072A1D43F18010000001442");
        V2Frame parsed = new V2PacketReader().read(sync);
        assertTrue(parsed.crcOk);
        assertEquals(V2ProtocolConstants.TYPE_SYNC, parsed.type);
        assertEquals(V2ProtocolConstants.PROTOCOL_VERSION, parsed.payload[0]);
        assertEquals("E072A1D43F18", V2PacketReader.macToHex(Arrays.copyOfRange(parsed.payload, 1, 7)));

        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        assertTrue(inbound.shouldHandleAsV2(conn, sync));
        ByteArrayOutputStream sock = new ByteArrayOutputStream();
        inbound.onBytes(conn, sync, sock);
        assertTrue(conn.active);
        assertEquals("E072A1D43F18", conn.mac);
        assertTrue(sock.size() > 0);
        assertTrue(new V2PacketReader().read(sock.toByteArray()).crcOk);
    }

    @Test
    void framingCrcSplitGapAndSyncWithVersion() throws Exception {
        V2PacketWriter writer = new V2PacketWriter();
        byte[] time = new byte[]{0, 0, 0, 1};
        byte[] data = new byte[]{0x11, 0x22};
        byte[] frame = writer.write(V2ProtocolConstants.TYPE_STATE, time, data, null);
        V2Frame parsed = new V2PacketReader().read(frame);
        assertTrue(parsed.crcOk);
        assertEquals(V2ProtocolConstants.TYPE_STATE, parsed.type);
        assertTrue(Arrays.equals(parsed.payload, concat(time, data)));

        byte[] a = writer.write(V2ProtocolConstants.TYPE_STATE, time, new byte[]{1}, null);
        byte[] b = writer.write(V2ProtocolConstants.TYPE_STATE, time, new byte[]{2}, null);
        V2FrameSplitter.SplitResult full = new V2FrameSplitter().split(concat(a, b));
        assertEquals(2, full.frames.size());
        assertEquals(0, full.remainder.length);

        V2HistoryCommand gap = new V2GapService().detectGap(5, 40, 100);
        assertNotNull(gap);
        assertEquals(V2ProtocolConstants.TYPE_REQ_HISTORY, gap.bytes[0]);
        assertEquals(5, readU32BE(gap.bytes, 1));
        assertEquals(41, readU32BE(gap.bytes, 5));
        assertEquals(99, readU32BE(gap.bytes, 9));

        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] mac6 = new byte[]{(byte) 0xE0, 0x72, (byte) 0xA1, (byte) 0xD4, 0x3F, 0x18};
        // version | MAC | deviceType | session
        byte[] syncPayload = new byte[12];
        syncPayload[0] = V2ProtocolConstants.PROTOCOL_VERSION;
        System.arraycopy(mac6, 0, syncPayload, 1, 6);
        syncPayload[7] = 0x01;
        putU32BE(syncPayload, 8, 3);

        byte[] syncFrame = buildDeviceFrame(V2ProtocolConstants.TYPE_SYNC, syncPayload);
        assertTrue(inbound.shouldHandleAsV2(conn, syncFrame));
        inbound.onBytes(conn, syncFrame, sock);
        assertTrue(conn.active);

        V2Frame syncOut = new V2PacketReader().read(sock.toByteArray());
        assertTrue(syncOut.crcOk);
        assertEquals(V2ProtocolConstants.TYPE_SYNC, syncOut.type);
        // time(4) + version+mac+dev+session+firstSession+token(18) + recover 0x03(5)
        assertEquals(4 + 18 + 5, syncOut.payload.length);
        assertEquals(V2ProtocolConstants.PROTOCOL_VERSION, syncOut.payload[4]);
        assertEquals(V2ProtocolConstants.TYPE_REQ_SESSION_INFO, syncOut.payload[4 + 18]);
        // legacy sync session=3 → walk first=2
        assertEquals(2, readU32BE(syncOut.payload, 4 + 18 + 1));
        int token = readU16BE(syncOut.payload, 4 + 16);

        sock.reset();
        byte[] wt40 = new byte[8];
        putU32LE(wt40, 0, 40);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_STATE, tokenPayload(token, wt40)), sock);
        assertTrue(sock.size() > 0);

        sock.reset();
        byte[] wt100 = new byte[8];
        putU32LE(wt100, 0, 100);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_STATE, tokenPayload(token, wt100)), sock);
        V2Frame ack100 = new V2PacketReader().read(sock.toByteArray());
        assertTrue(ack100.crcOk);
        assertEquals(4 + 4 + 13, ack100.payload.length);
        assertEquals(V2ProtocolConstants.TYPE_REQ_HISTORY, ack100.payload[8]);
        assertEquals(3, readU32BE(ack100.payload, 9));
        assertEquals(41, readU32BE(ack100.payload, 13));
        assertEquals(99, readU32BE(ack100.payload, 17));
    }

    @Test
    void invalidTokenSendsErrorFf02() throws Exception {
        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] wt = new byte[8];
        putU32LE(wt, 0, 1);
        // token 0xAABB not in store
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_STATE, tokenPayload(0xAABB, wt)), sock);

        assertTrue(sock.size() > 0);
        V2Frame err = new V2PacketReader().read(sock.toByteArray());
        assertTrue(err.crcOk);
        assertEquals(V2ProtocolConstants.TYPE_ERROR, err.type);
        // time(4) + errorCode(1)
        assertEquals(5, err.payload.length);
        assertEquals(V2ProtocolConstants.ERR_INVALID_TOKEN, err.payload[4]);
    }

    @Test
    void sessionInfoNotFoundShortFormStillAcks() throws Exception {
        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] mac6 = new byte[]{(byte) 0xE0, 0x72, (byte) 0xA1, (byte) 0xD4, 0x3F, 0x18};
        byte[] syncPayload = new byte[12];
        syncPayload[0] = V2ProtocolConstants.PROTOCOL_VERSION;
        System.arraycopy(mac6, 0, syncPayload, 1, 6);
        syncPayload[7] = 0x01;
        putU32BE(syncPayload, 8, 1);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SYNC, syncPayload), sock);
        int token = readU16BE(new V2PacketReader().read(sock.toByteArray()).payload, 4 + 16);

        sock.reset();
        // short 0x04: token(2) + session(4) only — session not found
        byte[] shortInfo = new byte[6];
        shortInfo[0] = (byte) (token >>> 8);
        shortInfo[1] = (byte) token;
        putU32BE(shortInfo, 2, 99);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SESSION_INFO, shortInfo), sock);

        V2Frame ack = new V2PacketReader().read(sock.toByteArray());
        assertTrue(ack.crcOk);
        assertEquals(V2ProtocolConstants.TYPE_SESSION_INFO, ack.type);
        assertEquals(99, readU32BE(ack.payload, 4));
    }

    @Test
    void historyRecoverPlansGapFromSessionInfo() {
        assertNull(V2HistoryRecover.plan(1, 100, 1, 100));
        V2HistoryCommand full = V2HistoryRecover.plan(5, -1, 10, 50);
        assertNotNull(full);
        assertEquals(V2ProtocolConstants.TYPE_REQ_HISTORY, full.bytes[0]);
        assertEquals(5, readU32BE(full.bytes, 1));
        assertEquals(10, readU32BE(full.bytes, 5));
        assertEquals(50, readU32BE(full.bytes, 9));

        V2HistoryCommand partial = V2HistoryRecover.plan(5, 20, 1, 50);
        assertNotNull(partial);
        assertEquals(21, readU32BE(partial.bytes, 5));
        assertEquals(50, readU32BE(partial.bytes, 9));
    }

    @Test
    void hubStateMapperMapsWeldingAndUnix() {
        V2HubPayload h = new V2HubPayload();
        h.machineState = 1;
        h.unixTime = 1_700_000_000L;
        h.actualCurrentA = 120;
        h.actualVoltageV = 22.5;
        h.packetIndex = 9;
        var s = V2HubStateMapper.toStateSummary(h, true);
        assertNotNull(s);
        assertEquals(org.alloy.models.WeldingMachineStatus.Welding, s.getStatus());
        assertTrue(s.isOfflineData());
        assertEquals("120", s.getProperties().get("State.I").getValue());
        assertEquals(java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(1_700_000_000L), java.time.ZoneOffset.UTC),
                s.getDateCreated());
    }

    @Test
    void syncPiggybacksSessionInfoRecover() throws Exception {
        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] mac6 = new byte[]{0x3C, 0x0F, 0x02, (byte) 0xC4, 0x05, (byte) 0x84};
        // new sync: + firstSession
        byte[] syncPayload = new byte[16];
        syncPayload[0] = V2ProtocolConstants.PROTOCOL_VERSION;
        System.arraycopy(mac6, 0, syncPayload, 1, 6);
        syncPayload[7] = 0x01;
        putU32BE(syncPayload, 8, 69);
        putU32BE(syncPayload, 12, 68);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SYNC, syncPayload), sock);

        V2Frame syncOut = new V2PacketReader().read(sock.toByteArray());
        assertTrue(syncOut.crcOk);
        assertEquals(4 + 18 + 5, syncOut.payload.length);
        assertEquals(V2ProtocolConstants.TYPE_REQ_SESSION_INFO, syncOut.payload[4 + 18]);
        assertEquals(68, readU32BE(syncOut.payload, 4 + 18 + 1));
        assertEquals(68, readU32BE(syncOut.payload, 4 + 12)); // echo firstSession
    }

    /** Sync firstSession..session → 0x03 walk → 0x05 на отстающей сессии. */
    @Test
    void syncFirstSessionWalkRecoversHistory() throws Exception {
        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] mac6 = new byte[]{0x3C, 0x0F, 0x02, (byte) 0xC4, 0x05, (byte) 0x84};
        byte[] syncPayload = new byte[16];
        syncPayload[0] = V2ProtocolConstants.PROTOCOL_VERSION;
        System.arraycopy(mac6, 0, syncPayload, 1, 6);
        syncPayload[7] = 0x01;
        putU32BE(syncPayload, 8, 70);
        putU32BE(syncPayload, 12, 69);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SYNC, syncPayload), sock);

        V2Frame syncOut = new V2PacketReader().read(sock.toByteArray());
        int token = readU16BE(syncOut.payload, 4 + 16);
        assertEquals(V2ProtocolConstants.TYPE_REQ_SESSION_INFO, syncOut.payload[4 + 18]);
        assertEquals(69, readU32BE(syncOut.payload, 4 + 18 + 1));

        // 0x04 session 69 → 0x05 идёт вперёд оставшегося обхода 0x03(70)
        sock.reset();
        byte[] info69 = fullSessionInfo(token, 69, 10, 50);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SESSION_INFO, info69), sock);
        V2Frame ack1 = new V2PacketReader().read(sock.toByteArray());
        assertEquals(V2ProtocolConstants.TYPE_REQ_HISTORY, ack1.payload[4 + 4]);
        assertEquals(69, readU32BE(ack1.payload, 4 + 4 + 1));
        assertEquals(10, readU32BE(ack1.payload, 4 + 4 + 5));
        assertEquals(50, readU32BE(ack1.payload, 4 + 4 + 9));

        byte[] short70 = new byte[6];
        short70[0] = (byte) (token >>> 8);
        short70[1] = (byte) token;
        putU32BE(short70, 2, 70);

        sock.reset();
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SESSION_INFO, short70), sock);
        assertEquals(V2ProtocolConstants.TYPE_PRIO_HISTORY,
                new V2PacketReader().read(sock.toByteArray()).payload[4 + 4]);

        // обход не потерян — 0x03(70) приезжает следом
        sock.reset();
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SESSION_INFO, short70), sock);
        V2Frame ack3 = new V2PacketReader().read(sock.toByteArray());
        assertEquals(V2ProtocolConstants.TYPE_REQ_SESSION_INFO, ack3.payload[4 + 4]);
        assertEquals(70, readU32BE(ack3.payload, 4 + 4 + 1));
    }

    /**
     * Главный регресс: пока очередь занята обходом SD, дыра в live-индексах
     * не должна теряться — last_index уезжает вперёд и второго шанса не будет.
     */
    @Test
    void liveGapSurvivesBusyQueue() throws Exception {
        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] mac6 = new byte[]{0x3C, 0x0F, 0x02, (byte) 0xC4, 0x05, (byte) 0x84};
        byte[] syncPayload = new byte[16];
        syncPayload[0] = V2ProtocolConstants.PROTOCOL_VERSION;
        System.arraycopy(mac6, 0, syncPayload, 1, 6);
        syncPayload[7] = 0x01;
        putU32BE(syncPayload, 8, 74);
        putU32BE(syncPayload, 12, 72); // обход 72..74 забивает очередь
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SYNC, syncPayload), sock);
        int token = readU16BE(new V2PacketReader().read(sock.toByteArray()).payload, 4 + 16);

        byte[] idx200 = new byte[8];
        putU32LE(idx200, 0, 200);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_STATE, tokenPayload(token, idx200)), sock);

        // обрыв связи: следующий live-пакет прилетает с 871
        sock.reset();
        byte[] idx871 = new byte[8];
        putU32LE(idx871, 0, 871);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_STATE, tokenPayload(token, idx871)), sock);
        // этот ACK уносит команду обхода, но дыра встала в голову очереди
        assertEquals(V2ProtocolConstants.TYPE_REQ_SESSION_INFO,
                new V2PacketReader().read(sock.toByteArray()).payload[8]);

        sock.reset();
        byte[] idx872 = new byte[8];
        putU32LE(idx872, 0, 872);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_STATE, tokenPayload(token, idx872)), sock);
        V2Frame ack = new V2PacketReader().read(sock.toByteArray());
        assertEquals(V2ProtocolConstants.TYPE_REQ_HISTORY, ack.payload[8]);
        assertEquals(74, readU32BE(ack.payload, 9));
        assertEquals(201, readU32BE(ack.payload, 13));
        assertEquals(870, readU32BE(ack.payload, 17));
    }

    /** Пачка sync подряд не должна множить обход SD. */
    @Test
    void duplicateWalkCommandsAreNotQueuedTwice() {
        V2CommandQueue q = new V2CommandQueue();
        for (int i = 0; i < 7; i++) {
            V2SdRecover.begin(q, "AABB", 52, 74);
        }
        assertEquals(74 - 52, q.pendingCount("AABB"));
    }

    /**
     * Сессия истории берётся из 0x06 (не из отправленного 0x05),
     * повторно приехавший индекс в телеметрию не уходит.
     */
    @Test
    void historySessionFromSetHistSessionAndDuplicateSkipped() throws Exception {
        List<String> saved = new ArrayList<>();
        V2TelemetrySink sink = (mac, body, history) ->
                saved.add(history + ":" + V2HubPayloadParser.readPacketIndex(body));
        V2InboundHandler inbound = new V2InboundHandler(
                new V2SessionStore(), new V2TokenService(), new V2OutboundBuilder(),
                new V2GapService(), sink, null, new V2CommandQueue(), null);
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] mac6 = new byte[]{0x3C, 0x0F, 0x02, (byte) 0xC4, 0x05, (byte) 0x84};
        byte[] syncPayload = new byte[16];
        syncPayload[0] = V2ProtocolConstants.PROTOCOL_VERSION;
        System.arraycopy(mac6, 0, syncPayload, 1, 6);
        syncPayload[7] = 0x01;
        putU32BE(syncPayload, 8, 70);
        putU32BE(syncPayload, 12, 70);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SYNC, syncPayload), sock);
        int token = readU16BE(new V2PacketReader().read(sock.toByteArray()).payload, 4 + 16);

        // плата объявляет, что выгружает сессию 61
        byte[] setSession = new byte[6];
        setSession[0] = (byte) (token >>> 8);
        setSession[1] = (byte) token;
        putU32BE(setSession, 2, 61);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SET_HIST_SESSION, setSession), sock);
        assertEquals(61, inbound.getStore().getByMac("3C0F02C40584").historySession);

        byte[] rec = new byte[V2HubPayloadParser.BODY_LEN];
        putU32LE(rec, 0, 7);
        putU32LE(rec, 4, 1_700_000_000);
        inbound.onBytes(conn, buildDeviceFrame(
                V2ProtocolConstants.TYPE_HISTORY_RECORD, tokenPayload(token, rec)), sock);
        inbound.onBytes(conn, buildDeviceFrame(
                V2ProtocolConstants.TYPE_HISTORY_RECORD, tokenPayload(token, rec)), sock);

        assertEquals(List.of("true:7"), saved);
        assertEquals(61, inbound.getStore().getByMac("3C0F02C40584").historySession);
    }

    @Test
    void sdCatalog0cWalksSessionsAndAcks() throws Exception {
        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] mac6 = new byte[]{0x3C, 0x0F, 0x02, (byte) 0xC4, 0x05, (byte) 0x84};
        byte[] syncPayload = new byte[16];
        syncPayload[0] = V2ProtocolConstants.PROTOCOL_VERSION;
        System.arraycopy(mac6, 0, syncPayload, 1, 6);
        syncPayload[7] = 0x01;
        putU32BE(syncPayload, 8, 10);
        putU32BE(syncPayload, 12, 10); // walk only current on sync
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SYNC, syncPayload), sock);
        int token = readU16BE(new V2PacketReader().read(sock.toByteArray()).payload, 4 + 16);

        // drain sync's 0x03(10)
        sock.reset();
        byte[] short10 = new byte[6];
        short10[0] = (byte) (token >>> 8);
        short10[1] = (byte) token;
        putU32BE(short10, 2, 10);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SESSION_INFO, short10), sock);

        sock.reset();
        byte[] cat = new byte[10];
        cat[0] = (byte) (token >>> 8);
        cat[1] = (byte) token;
        putU32BE(cat, 2, 50);
        putU32BE(cat, 6, 52);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SD_CATALOG, cat), sock);

        V2Frame ack = new V2PacketReader().read(sock.toByteArray());
        assertTrue(ack.crcOk);
        assertEquals(V2ProtocolConstants.TYPE_SD_CATALOG, ack.type);
        assertEquals(50, readU32BE(ack.payload, 4));
        assertEquals(52, readU32BE(ack.payload, 8));
        assertEquals(V2ProtocolConstants.TYPE_REQ_SESSION_INFO, ack.payload[4 + 8]);
        assertEquals(50, readU32BE(ack.payload, 4 + 8 + 1));
        assertEquals(50, inbound.getStore().getByMac("3C0F02C40584").sdFirstSession);
        assertEquals(52, inbound.getStore().getByMac("3C0F02C40584").sdLastSession);
    }

    @Test
    void sdRecoverCapsLongRange() {
        V2CommandQueue q = new V2CommandQueue();
        V2HistoryCommand first = V2SdRecover.begin(q, "AABB", 1, 100);
        assertNotNull(first);
        assertEquals(100 - V2SdRecover.MAX_SESSIONS + 1, readU32BE(first.bytes, 1));
        // first returned + (MAX-1) queued
        assertEquals(V2SdRecover.MAX_SESSIONS - 1, q.pendingCount("AABB"));
    }

    @Test
    void hubPayloadLittleEndianParse() {
        byte[] body = new byte[V2HubPayloadParser.BODY_LEN];
        putU32LE(body, 0, 81);
        putU32LE(body, 4, 1_700_000_000);
        body[8] = 1; // сварка
        body[9] = 7;
        body[10] = 0; // job 7
        body[11] = 3; // MIG/MAG
        // set current 250 A LE
        body[16] = (byte) 250;
        body[17] = 0;
        // set voltage 23.5 V → 235 LE
        body[20] = (byte) 235;
        body[21] = 0;
        // inductance -3
        body[23] = (byte) -3;
        // radiator -199.9 → -1999 sentinel
        body[46] = (byte) 0x31;
        body[47] = (byte) 0xF8; // -1999 as i16 LE
        // warnings3 bit0
        body[78] = 0x01;
        body[79] = 0x00;

        assertNull(V2HubPayloadParser.parse(new byte[79]));
        V2HubPayload h = V2HubPayloadParser.parse(body);
        assertNotNull(h);
        assertEquals(81, h.packetIndex);
        assertEquals(1_700_000_000L, h.unixTime);
        assertEquals(1, h.machineState);
        assertEquals(7, h.jobNumber);
        assertEquals(3, h.weldMode);
        assertEquals(250, h.setCurrentA);
        assertEquals(23.5, h.setVoltageV, 1e-9);
        assertEquals(-3, h.setInductance);
        assertEquals(-199.9, h.tempRadiatorC, 1e-9);
        assertEquals(1, h.warnings3);
        assertEquals(81, V2HubPayloadParser.readPacketIndex(body));
    }

    @Test
    void historyNotFound0aAcksIndex() throws Exception {
        V2InboundHandler inbound = new V2InboundHandler();
        V2ConnectionState conn = new V2ConnectionState();
        ByteArrayOutputStream sock = new ByteArrayOutputStream();

        byte[] mac6 = new byte[]{(byte) 0x3C, 0x0F, 0x02, (byte) 0xC4, 0x05, (byte) 0x84};
        byte[] syncPayload = new byte[12];
        syncPayload[0] = V2ProtocolConstants.PROTOCOL_VERSION;
        System.arraycopy(mac6, 0, syncPayload, 1, 6);
        syncPayload[7] = 0x01;
        putU32BE(syncPayload, 8, 1);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_SYNC, syncPayload), sock);
        int token = readU16BE(new V2PacketReader().read(sock.toByteArray()).payload, 4 + 16);

        sock.reset();
        byte[] missing = new byte[6];
        missing[0] = (byte) (token >>> 8);
        missing[1] = (byte) token;
        putU32BE(missing, 2, 42);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_HISTORY_NOT_FOUND, missing), sock);

        V2Frame ack = new V2PacketReader().read(sock.toByteArray());
        assertTrue(ack.crcOk);
        assertEquals(V2ProtocolConstants.TYPE_HISTORY_NOT_FOUND, ack.type);
        assertEquals(42, readU32BE(ack.payload, 4));
    }

    private static byte[] fullSessionInfo(int token, int session, int firstIdx, int lastIdx) {
        byte[] info = new byte[22];
        info[0] = (byte) (token >>> 8);
        info[1] = (byte) token;
        putU32BE(info, 2, session);
        putU32BE(info, 6, firstIdx);
        putU32BE(info, 10, 100);
        putU32BE(info, 14, lastIdx);
        putU32BE(info, 18, 200);
        return info;
    }

    private static byte[] tokenPayload(int token, byte[] wtinfo) {
        byte[] p = new byte[2 + wtinfo.length];
        p[0] = (byte) (token >>> 8);
        p[1] = (byte) token;
        System.arraycopy(wtinfo, 0, p, 2, wtinfo.length);
        return p;
    }

    private static byte[] buildDeviceFrame(byte type, byte[] payload) {
        int length = 2 + payload.length;
        byte[] out = new byte[length + 1];
        out[0] = type;
        out[1] = (byte) length;
        System.arraycopy(payload, 0, out, 2, payload.length);
        out[length] = (byte) V2Crc8.compute(out, 0, length);
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
