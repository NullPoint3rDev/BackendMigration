package org.alloy.protocol.v2;

public class V2Session {
    public String mac;
    public byte protocolVersion;
    public byte deviceType;
    public int sessionNumber;
    public int token;            // 0..65535
    public int historySession;   // через 0x06 / 0x05 recover, иначе = sessionNumber
    /** Границы SD (sync firstSession / 0x0C); −1 если ещё не знаем. */
    public int sdFirstSession = -1;
    public int sdLastSession = -1;
    /** ponytail: in-memory; ceiling — потеря при рестарте; upgrade — читать/писать из БД. */
    public int lastLiveIndex = -1;
    public int lastHistoryIndex = -1;
}
