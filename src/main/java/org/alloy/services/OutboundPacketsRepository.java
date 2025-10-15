package org.alloy.services;

import java.util.concurrent.ConcurrentHashMap;

public class OutboundPacketsRepository {

    public static class Packet {
        public String mac;
        public String data;
    }

    private static final ConcurrentHashMap<String, Packet> dict = new ConcurrentHashMap<>();

    public static void set(String mac, String data) {
        if (mac == null || data == null) return;
        Packet p = new Packet();
        p.mac = mac.toUpperCase();
        p.data = data;
        dict.put(p.mac, p);
    }

    public static Packet tryGet(String mac) {
        if (mac == null) return null;
        return dict.get(mac.toUpperCase());
    }
}


