package org.alloy.protocol.v2;

public class V2Session {
    public String mac;
    public byte deviceType;
    public int sessionNumber;
    public int token;            // 0..65535
    public int historySession;   // через 0x06, иначе = sessionNumber
    /** ponytail: in-memory; ceiling — потеря при рестарте; upgrade — читать/писать из БД. */
    public int lastLiveIndex = -1;
    public int lastHistoryIndex = -1;
}
