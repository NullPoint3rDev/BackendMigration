package org.alloy.protocol.v2;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.alloy.protocol.v2.V2PacketReader.putU32BE;
import static org.alloy.protocol.v2.V2PacketReader.readU16BE;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        // time(4) + version(1)+mac(6)+dev(1)+session(4)+token(2) = 18
        assertEquals(4 + 14, syncOut.payload.length);
        assertEquals(V2ProtocolConstants.PROTOCOL_VERSION, syncOut.payload[4]);
        int token = readU16BE(syncOut.payload, 4 + 12);

        sock.reset();
        byte[] wt40 = new byte[8];
        putU32BE(wt40, 0, 40);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_STATE, tokenPayload(token, wt40)), sock);
        assertTrue(sock.size() > 0);

        sock.reset();
        byte[] wt100 = new byte[8];
        putU32BE(wt100, 0, 100);
        inbound.onBytes(conn, buildDeviceFrame(V2ProtocolConstants.TYPE_STATE, tokenPayload(token, wt100)), sock);
        V2Frame ack100 = new V2PacketReader().read(sock.toByteArray());
        assertTrue(ack100.crcOk);
        assertEquals(4 + 4 + 13, ack100.payload.length);
        assertEquals(V2ProtocolConstants.TYPE_REQ_HISTORY, ack100.payload[8]);
        assertEquals(3, readU32BE(ack100.payload, 9));
        assertEquals(41, readU32BE(ack100.payload, 13));
        assertEquals(99, readU32BE(ack100.payload, 17));
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
}
