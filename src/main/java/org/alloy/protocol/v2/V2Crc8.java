package org.alloy.protocol.v2;

/**
 * CRC-8/ROHC (poly 0x07 reflected → 0xE0, init 0xFF) — как на тестовой плате.
 * Считать по всем байтам кадра без последнего (самого CRC).
 *
 * ponytail: CoreOutboundService.crc8 (init 0x00, MSB) не совпал с живым sync;
 * кадр 010E02E072A1D43F18010000001442 даёт CRC=0x42 только с ROHC.
 */
public final class V2Crc8 {
    private V2Crc8() {}

    public static int compute(byte[] data, int off, int len) {
        int crc = 0xFF;
        for (int i = 0; i < len; i++) {
            crc ^= (data[off + i] & 0xFF);
            for (int b = 0; b < 8; b++) {
                if ((crc & 0x01) != 0) {
                    crc = (crc >>> 1) ^ 0xE0;
                } else {
                    crc >>>= 1;
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
