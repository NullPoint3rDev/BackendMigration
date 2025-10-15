package org.alloy.services;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IncomingPacketsQueue {

    public static class Packet {
        public String ip;
        public String mac;
        public String data;
        public Instant serverDatetime;
    }

    private static final ConcurrentLinkedQueue<Packet> queue = new ConcurrentLinkedQueue<>();

    public static void enqueue(Packet p) {
        if (p != null) queue.add(p);
    }

    public static Packet tryDequeue() {
        return queue.poll();
    }
}


