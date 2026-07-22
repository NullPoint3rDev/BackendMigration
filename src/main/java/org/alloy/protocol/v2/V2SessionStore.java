package org.alloy.protocol.v2;

import java.util.concurrent.ConcurrentHashMap;

public class V2SessionStore {
    private final ConcurrentHashMap<Integer, V2Session> byToken = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, V2Session> byMac = new ConcurrentHashMap<>();

    public void put(V2Session s) {
        // если у mac уже была сессия — удалить старый token из byToken
        V2Session old = byMac.put(s.mac, s);
        if(old != null) {
            byToken.remove(old.token);
        }
        byToken.put(s.token, s);
    }

    public V2Session getByToken(int token) {
        return byToken.get(token);
    }

    public V2Session getByMac(String mac) {
        return byMac.get(mac);
    }
}
