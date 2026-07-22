package org.alloy.protocol.v2;

import static org.alloy.protocol.v2.V2PacketReader.putU32BE;

public class V2OutboundBuilder {
    private final V2PacketWriter writer = new V2PacketWriter();

    public byte[] syncResponse(byte[] mac6, byte deviceType, int session, int token, V2HistoryCommand cmd) {
        byte[] data = new byte[6 + 1 + 4 + 2];
        System.arraycopy(mac6, 0, data, 0, 6);
        data[6] = deviceType;
        putU32BE(data, 7, session);
        data[11] = (byte) (token >>> 8);
        data[12] = (byte) token;

        byte[] command = cmd == null ? null : cmd.bytes;
        return writer.write(V2ProtocolConstants.TYPE_SYNC, V2PacketWriter.nowTime4(), data, command);
    }

    public byte[] stateAck(int packetIndex, V2HistoryCommand cmd) {
        return indexAck(V2ProtocolConstants.TYPE_STATE, packetIndex, cmd);
    }

    public byte[] sessionInfoAck(int sessionNumber, V2HistoryCommand cmd) {
        byte[] data = new byte[4];
        putU32BE(data, 0, sessionNumber);
        return writer.write(
                V2ProtocolConstants.TYPE_SESSION_INFO,
                V2PacketWriter.nowTime4(),
                data,
                cmd == null ? null : cmd.bytes);
    }

    public byte[] setHistSessionAck(int sessionNumber, V2HistoryCommand cmd) {
        byte[] data = new byte[4];
        putU32BE(data, 0, sessionNumber);
        return writer.write(
                V2ProtocolConstants.TYPE_SET_HIST_SESSION,
                V2PacketWriter.nowTime4(),
                data,
                cmd == null ? null : cmd.bytes);
    }

    public byte[] historyRecordAck(int packetIndex, V2HistoryCommand cmd) {
        return indexAck(V2ProtocolConstants.TYPE_HISTORY_RECORD, packetIndex, cmd);
    }

    private byte[] indexAck(byte type, int packetIndex, V2HistoryCommand cmd) {
        byte[] data = new byte[4];
        putU32BE(data, 0, packetIndex);
        return writer.write(type, V2PacketWriter.nowTime4(), data, cmd == null ? null : cmd.bytes);
    }
}
