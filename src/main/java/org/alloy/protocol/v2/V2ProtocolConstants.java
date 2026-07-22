package org.alloy.protocol.v2;

public final class V2ProtocolConstants {

    public static final String TEST_MAC = "E072A1D43F18";

    public static final byte TYPE_SYNC =  0x01;
    public static final byte TYPE_STATE = 0x02;
    public static final byte TYPE_REQ_SESSION_INFO = 0x03;
    public static final byte TYPE_SESSION_INFO = 0x04;
    public static final byte TYPE_REQ_HISTORY = 0x05;
    public static final byte TYPE_SET_HIST_SESSION = 0x06;
    public static final byte TYPE_HISTORY_RECORD = 0x07;
    public static final byte TYPE_PRIO_HISTORY = 0x08;
    public static final byte TYPE_STOP_HISTORY = 0x09;

    public static final byte ERR_UNKNOWN_DEVICE = 0x01;
    public static final byte ERR_INVALID_TOKEN = 0x02;
    public static final byte ERR_CRC = 0x03;
    public static final byte ERR_UNKNOWN = (byte) 0xFF;

    public static boolean isTestMac(String mac) {
        if (mac == null || mac.isEmpty()) {
            return false;
        }
        String cleaned = mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        return TEST_MAC.equals(cleaned);
    }
}
