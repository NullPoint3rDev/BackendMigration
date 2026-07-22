package org.alloy.protocol.v2;

/** Хвост неполного кадра + флаг v2 на TCP-соединение. */
public class V2ConnectionState {
    public byte[] tail = new byte[0];
    /** После успешного sync с TEST_MAC — весь сокет идёт в v2. */
    public boolean active;
    public String mac = "";
}
