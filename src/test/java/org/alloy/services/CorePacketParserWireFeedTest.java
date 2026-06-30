package org.alloy.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorePacketParserWireFeedTest {

    @Test
    void parse_instantWireFeed_afterYear_isTenthsOfMetersPerMinute() {
        // index=0, time zeros, year=26, wire feed raw=122 → 12.2 m/min
        String hex = "00000000" + "00000001011A" + "007A";
        CorePacket packet = CorePacketParser.parse(";" + hex);

        assertEquals(122, packet.instantWireFeedSpeedTenths);
        assertEquals(12.2, packet.getDisplayInstantWireFeedMpm(), 0.001);
    }
}
