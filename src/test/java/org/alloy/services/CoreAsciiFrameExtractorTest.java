package org.alloy.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreAsciiFrameExtractorTest {

    private static final String MAC = "C82B9620E506";

    private static final String FRAME_1 =
            ":C82B9620E506;00001CAE0B1A021B071A00460003000000000000003500DC00F500000000000000000190019001920108010A0000000000000000000000000000000004010004000000000000000000001C5D0000000000000000000076";

    private static final String FRAME_2 =
            ":C82B9620E506;00001CAF0B1A031B071A00460003000000000000003500DC00F500000000000000000190019001910108010A0000000000000000000000000000000004010004000000000000000000001C5E00000000000000000000F7";

    @Test
    void pickLastParseableFrame_singleFrame_unchanged() {
        assertEquals(FRAME_1, CoreAsciiFrameExtractor.pickLastParseableFrame(FRAME_1));
        CorePacket p = CorePacketParser.parse(FRAME_1);
        assertNotNull(p);
        assertEquals(3, p.weldingMachineState);
    }

    @Test
    void pickLastParseableFrame_connectBurst_takesLastValidLine() {
        String blob = "CONNECT Core4Machine:" + FRAME_1.substring(1) + "\n"
                + FRAME_2 + "\n"
                + ":C82B9620E5060000000000004010004000000000000000001C4D000000000000000000009E\n"
                + FRAME_1;

        String picked = CoreAsciiFrameExtractor.pickLastParseableFrame(blob);
        assertEquals(FRAME_1, picked);

        CorePacket p = CorePacketParser.parse(picked);
        assertNotNull(p);
        assertEquals(0x1CAEL, p.index);
    }

    @Test
    void pickLastParseableFrame_trailingGarbage_picksLastGoodInMultiLine() {
        String blob = FRAME_1 + "\n00DF\n" + FRAME_2;
        assertEquals(FRAME_2, CoreAsciiFrameExtractor.pickLastParseableFrame(blob));
    }

    @Test
    void looksLikeAsciiCoreChunk_detectsConnectAndColon() {
        assertTrue(CoreAsciiFrameExtractor.looksLikeAsciiCoreChunk(
                ("CONNECT Core4Machine:" + FRAME_1.substring(1)).getBytes()));
        assertTrue(CoreAsciiFrameExtractor.looksLikeAsciiCoreChunk(FRAME_1.getBytes()));
        assertFalse(CoreAsciiFrameExtractor.looksLikeAsciiCoreChunk(new byte[]{0x01, 0x02, 0x03}));
    }

    @Test
    void toCoreFrame_rejectsMissingSemicolonAfterMac() {
        assertNull(CoreAsciiFrameExtractor.toCoreFrame(":C82B9620E506000000000000401"));
    }
}
