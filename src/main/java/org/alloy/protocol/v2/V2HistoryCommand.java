package org.alloy.protocol.v2;

import static org.alloy.protocol.v2.V2PacketReader.putU32BE;

public class V2HistoryCommand {

    public final byte[] bytes;
    private V2HistoryCommand(byte[] bytes) {
        this.bytes = bytes;
    }
    public static V2HistoryCommand requestSessionInfo(int sessionNo) {
        byte[] b = new byte[5];
        b[0] = V2ProtocolConstants.TYPE_REQ_SESSION_INFO; // 0x03
        putU32BE(b, 1, sessionNo);
        return new V2HistoryCommand(b);
    }
    public static V2HistoryCommand requestHistory(int sessionNo, int fromIncl, int toIncl) {
        byte[] b = new byte[13];
        b[0] = V2ProtocolConstants.TYPE_REQ_HISTORY; // 0x05
        putU32BE(b, 1, sessionNo);
        putU32BE(b, 5, fromIncl);
        putU32BE(b, 9, toIncl);
        return new V2HistoryCommand(b);
    }
    public static V2HistoryCommand priorityHistory() {
        return new V2HistoryCommand(new byte[]{ V2ProtocolConstants.TYPE_PRIO_HISTORY });
    }
    public static V2HistoryCommand stopHistory() {
        return new V2HistoryCommand(new byte[]{ V2ProtocolConstants.TYPE_STOP_HISTORY });
    }
}
