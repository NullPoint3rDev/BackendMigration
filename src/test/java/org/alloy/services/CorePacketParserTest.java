package org.alloy.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Legacy ASCII {@code :MAC;HEX} → {@link CorePacketParser} (WTINFO).
 */
class CorePacketParserTest {

    private static final String MAC = "E09806083396";

    @Test
    void parse_nullOrMalformed_returnsNull() {
        assertNull(CorePacketParser.parse(null));
        assertNull(CorePacketParser.parse(":AABBCCDDEEFF"));
        assertNull(CorePacketParser.parse(":AABBCCDDEEFF;ABCD")); // < 8 hex nibbles after ;
        assertNull(CorePacketParser.parse(":AABBCCDDEEFF;ABCDEFGHI")); // odd hex length
        assertNull(CorePacketParser.parse(":AABBCCDDEEFF;ZZZZZZZZ"));
    }

    @Test
    void parse_knownWtinfo_readsIndexStateCurrentVoltageAndGasTail() {
        byte[] wt = new byte[86];
        putU32BE(wt, 0, 0x0000_0042);          // index
        wt[4] = 12; wt[5] = 30; wt[6] = 45;    // H:M:S
        wt[7] = 24; wt[8] = 7; wt[9] = 26;     // D/M/Y
        // reserve @10-11
        wt[12] = 0;                             // flags
        wt[13] = 1;                             // weldingMachineState = welding
        putI16BE(wt, 14, 250);                  // gasFlow (десятые)
        putI16BE(wt, 16, 180);                  // weldingCurrent
        putI16BE(wt, 18, 270);                  // weldingVoltage (десятые В)
        putI16BE(wt, 20, 3);                    // jobNumber
        putI16BE(wt, 22, 150);                  // current (уставка)
        putI16BE(wt, 24, 50);                   // voltage
        // inductance..warnings left 0; skip to extended gas @80
        putU32BE(wt, 72, 1000);                 // workTimeSincePowerOn
        putU32BE(wt, 76, 200);                  // weldingTimeSincePowerOn
        putU16BE(wt, 80, 35);                   // instantGasFlowLpm
        putU32BE(wt, 82, 1234);                 // gasConsumptionSincePowerOnLiters

        String frame = ":" + MAC + ";" + toHex(wt);
        CorePacket p = CorePacketParser.parse(frame);

        assertNotNull(p);
        assertEquals(0x42L, p.index);
        assertEquals(12, p.hours);
        assertEquals(30, p.minutes);
        assertEquals(45, p.seconds);
        assertEquals(24, p.date);
        assertEquals(7, p.month);
        assertEquals(26, p.year);
        assertEquals(1, p.weldingMachineState);
        assertEquals(250, p.gasFlow);
        assertEquals(180, p.weldingCurrent);
        assertEquals(270, p.weldingVoltage);
        assertEquals(3, p.jobNumber);
        assertEquals(150, p.current);
        assertEquals(50, p.voltage);
        assertEquals(1000L, p.workTimeSincePowerOn);
        assertEquals(200L, p.weldingTimeSincePowerOn);
        assertTrue(p.hasExtendedGasMetrics);
        assertEquals(35, p.instantGasFlowLpm);
        assertEquals(1234L, p.gasConsumptionSincePowerOnLiters);
        assertEquals(18.0, p.getDisplayVoltage(), 0.01);
        assertEquals(180, p.getDisplayCurrent(), 0.01);
    }

    @Test
    void parse_shortPayload_noExtendedGasFlag() {
        byte[] wt = new byte[28];
        putU32BE(wt, 0, 7);
        wt[13] = 0; // idle
        putI16BE(wt, 16, 0);
        putI16BE(wt, 22, 152);

        CorePacket p = CorePacketParser.parse(":" + MAC + ";" + toHex(wt));
        assertNotNull(p);
        assertEquals(7L, p.index);
        assertFalse(p.hasExtendedGasMetrics);
    }

    @Test
    void hexStringToIntArray_rejectsOddOrBadDigits() {
        assertNull(CorePacketParser.hexStringToIntArray("ABC"));
        assertNull(CorePacketParser.hexStringToIntArray("GG"));
        assertEquals(2, CorePacketParser.hexStringToIntArray("00FF").length);
        assertEquals(0xFF, CorePacketParser.hexStringToIntArray("00FF")[1]);
    }

    private static void putU16BE(byte[] b, int off, int v) {
        b[off] = (byte) ((v >>> 8) & 0xFF);
        b[off + 1] = (byte) (v & 0xFF);
    }

    private static void putI16BE(byte[] b, int off, int v) {
        putU16BE(b, off, v & 0xFFFF);
    }

    private static void putU32BE(byte[] b, int off, int v) {
        b[off] = (byte) ((v >>> 24) & 0xFF);
        b[off + 1] = (byte) ((v >>> 16) & 0xFF);
        b[off + 2] = (byte) ((v >>> 8) & 0xFF);
        b[off + 3] = (byte) (v & 0xFF);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            sb.append(String.format("%02X", value & 0xFF));
        }
        return sb.toString();
    }
}
