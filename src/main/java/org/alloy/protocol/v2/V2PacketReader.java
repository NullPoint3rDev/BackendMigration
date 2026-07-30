package org.alloy.protocol.v2;

import java.util.Arrays;

public class V2PacketReader {

    public V2Frame read(byte[] frameBytes) {
        V2Frame f = new V2Frame();
        if(frameBytes == null || frameBytes.length < 3) {
            f.crcOk = false;
            return f;
        }

        int length = frameBytes[1] & 0xFF;

        if(frameBytes.length != length + 1) {
            f.crcOk = false;
            return f;
        }

        f.type = frameBytes[0];
        f.totalBytes = frameBytes.length;

        int crcExpected = frameBytes[length] & 0xFF;
        int crcActual = V2Crc8.compute(frameBytes, 0, length);
        f.crcOk = (crcExpected == crcActual);

        int payloadLen = length - 2; // without type and length
        if(payloadLen > 0) {
            f.payload = Arrays.copyOfRange(frameBytes, 2, length);
        } else {
            f.payload = new byte[0];
        }

        return f;
    }

    public static int readU32BE(byte[] p, int off) {
        return ((p[off] & 0xFF) << 24)
                | ((p[off + 1] & 0xFF) << 16)
                | ((p[off + 2] & 0xFF) << 8)
                | (p[off + 3] & 0xFF);
    }

    public static void putU32BE(byte[] dest, int off, int value) {
        dest[off] = (byte) (value >>> 24);
        dest[off + 1] = (byte) (value >>> 16);
        dest[off + 2] = (byte) (value >>> 8);
        dest[off + 3] = (byte) value;
    }

    public static int readU16BE(byte[] p, int off) {
        return ((p[off] & 0xFF) << 8) | (p[off + 1] & 0xFF);
    }

    /** Hub blob inside v2 0x02/0x07 — little-endian. */
    public static int readU16LE(byte[] p, int off) {
        return (p[off] & 0xFF) | ((p[off + 1] & 0xFF) << 8);
    }

    public static int readU32LE(byte[] p, int off) {
        return (p[off] & 0xFF)
                | ((p[off + 1] & 0xFF) << 8)
                | ((p[off + 2] & 0xFF) << 16)
                | ((p[off + 3] & 0xFF) << 24);
    }

    public static void putU32LE(byte[] dest, int off, int value) {
        dest[off] = (byte) value;
        dest[off + 1] = (byte) (value >>> 8);
        dest[off + 2] = (byte) (value >>> 16);
        dest[off + 3] = (byte) (value >>> 24);
    }

    public static long readU64LE(byte[] p, int off) {
        return (p[off] & 0xFFL)
                | ((p[off + 1] & 0xFFL) << 8)
                | ((p[off + 2] & 0xFFL) << 16)
                | ((p[off + 3] & 0xFFL) << 24)
                | ((p[off + 4] & 0xFFL) << 32)
                | ((p[off + 5] & 0xFFL) << 40)
                | ((p[off + 6] & 0xFFL) << 48)
                | ((p[off + 7] & 0xFFL) << 56);
    }

    public static int readI16LE(byte[] p, int off) {
        return (short) readU16LE(p, off);
    }

    public static String macToHex(byte[] mac6) {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 6; i++) {
            sb.append(String.format("%02X", mac6[i] & 0xFF));
        }
        return sb.toString();
    }

    public static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
