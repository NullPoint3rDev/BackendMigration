package org.alloy.protocol.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class V2FrameSplitter {

    public static final class SplitResult {
        public final List<byte[]> frames;
        public final byte[] remainder;

        public SplitResult(List<byte[]> frames, byte[] remainder) {
            this.frames = frames;
            this.remainder = remainder;
        }
    }

    public SplitResult split (byte[] buffer) {
        List<byte[]> frames = new ArrayList<>();
        int offset = 0;

        while(offset + 2 <= buffer.length) {
            int length = buffer[offset + 1] & 0xFF; // without CRC
            int total = length + 1; // with CRC

            if (length < 2) {
                break;
            }

            if (offset + total > buffer.length) {
                break;
            }

            frames.add(Arrays.copyOfRange(buffer, offset, offset + total));
            offset += total;
        }

        byte[] remainder = (offset < buffer.length)
                ? Arrays.copyOfRange(buffer, offset, buffer.length)
                : new byte[0];

        return new SplitResult(frames, remainder);
    }
}
