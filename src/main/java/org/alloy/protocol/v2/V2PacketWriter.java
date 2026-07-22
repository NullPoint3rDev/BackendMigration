package org.alloy.protocol.v2;

import static org.alloy.protocol.v2.V2PacketReader.putU32BE;

public class V2PacketWriter {

    /**
     * @param type    например TYPE_SYNC
     * @param time4   ровно 4 байта (unix sec BE — уточните с прошивкой)
     * @param data    обязательная часть после time (может быть пустой)
     * @param command необязательная команда сервера (0x03/05/08/09) или null
     */
    public byte[] write(byte type, byte[] time4, byte[] data, byte[] command) {
        if(time4 == null || time4.length != 4) {
            throw new IllegalArgumentException("time must be 4 bytes");
        }
        if(data == null) data = new byte[0];
        if(command == null) command = new byte[0];

        // length = type(1) + length(1) + time(4) + data + command  — БЕЗ crc
        int length = 2 + 4 + data.length + command.length;
        byte[] out = new byte[length + 1];

        out[0] = type;
        out[1] = (byte) length;
        System.arraycopy(time4, 0, out, 2, 4);
        System.arraycopy(data, 0, out, 6, data.length);
        System.arraycopy(command, 0, out, 6 + data.length, command.length);

        out[length] = (byte) V2Crc8.compute(out, 0, length);
        return out;
    }

    public static byte[] nowTime4() {
        int sec = (int) (System.currentTimeMillis() / 1000L);
        byte[] t = new byte[4];
        putU32BE(t, 0, sec);
        return t;
    }
}
