package org.alloy.protocol.v2;

import java.security.SecureRandom;

public class V2TokenService {
    private final SecureRandom rnd = new SecureRandom();

    /** 2 байта, диапазон 1..65535. */
    public int nextToken() {
        return rnd.nextInt(0xFFFF) + 1;
    }
}
