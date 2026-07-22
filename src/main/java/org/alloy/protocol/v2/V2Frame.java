package org.alloy.protocol.v2;

public class V2Frame {
    public byte type;
    /* Всё после length и до crc (без type/length/crc). */

    public byte[] payload;

    public boolean crcOk;
    /* Сколько байт этот кадр занял в буфере (= length + 1). */

    public int totalBytes;
}
