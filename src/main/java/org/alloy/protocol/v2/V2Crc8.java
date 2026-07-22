package org.alloy.protocol.v2;

/**
 * CRC8 (poly 0x07, init 0x00) — как в CoreOutboundService.
 * Считать по всем байтам кадра без последнего (самого CRC).
 */
public final class V2Crc8 {
    private V2Crc8() {}

    public static int compute(byte[] data, int off, int len) {
        int crc = 0x00;
        for (int i = 0; i < len; i++) {
            crc ^= (data[off + i] & 0xFF);
            for (int b = 0; b < 8; b++) {
                if ((crc & 0x80) != 0) {
                    crc = (crc << 1) ^ 0x07;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFF;
            }
        }
        return crc & 0xFF;
    }

    public static int compute(byte[] data) {
        return compute(data, 0, data.length);
    }
}
