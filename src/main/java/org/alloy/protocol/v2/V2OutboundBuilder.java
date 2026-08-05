package org.alloy.protocol.v2;

import static org.alloy.protocol.v2.V2PacketReader.putU32BE;

public class V2OutboundBuilder {
    private final V2PacketWriter writer = new V2PacketWriter();

    /**
     * Sync response data after time:
     * version(1) | MAC(6) | deviceType(1) | session(4) | firstSession(4) | token(2)
     */
    public byte[] syncResponse(
            byte protocolVersion,
            byte[] mac6,
            byte deviceType,
            int session,
            int firstSession,
            int token,
            V2HistoryCommand cmd) {
        byte[] data = new byte[1 + 6 + 1 + 4 + 4 + 2];
        data[0] = protocolVersion;
        System.arraycopy(mac6, 0, data, 1, 6);
        data[7] = deviceType;
        putU32BE(data, 8, session);
        putU32BE(data, 12, firstSession);
        data[16] = (byte) (token >>> 8);
        data[17] = (byte) token;

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

    public byte[] historyNotFoundAck(int packetIndex, V2HistoryCommand cmd) {
        return indexAck(V2ProtocolConstants.TYPE_HISTORY_NOT_FOUND, packetIndex, cmd);
    }

    /** 0x0C ACK: firstSession(4) | lastSession(4) */
    public byte[] sdCatalogAck(int firstSession, int lastSession, V2HistoryCommand cmd) {
        byte[] data = new byte[8];
        putU32BE(data, 0, firstSession);
        putU32BE(data, 4, lastSession);
        return writer.write(
                V2ProtocolConstants.TYPE_SD_CATALOG,
                V2PacketWriter.nowTime4(),
                data,
                cmd == null ? null : cmd.bytes);
    }

    /** type 0xFF | time(4) | errorCode(1) | [cmd] | crc */
    public byte[] error(byte errorCode, V2HistoryCommand cmd) {
        return writer.write(
                V2ProtocolConstants.TYPE_ERROR,
                V2PacketWriter.nowTime4(),
                new byte[]{errorCode},
                cmd == null ? null : cmd.bytes);
    }

    private byte[] indexAck(byte type, int packetIndex, V2HistoryCommand cmd) {
        byte[] data = new byte[4];
        putU32BE(data, 0, packetIndex);
        return writer.write(type, V2PacketWriter.nowTime4(), data, cmd == null ? null : cmd.bytes);
    }
}
