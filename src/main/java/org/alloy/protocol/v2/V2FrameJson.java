package org.alloy.protocol.v2;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.alloy.protocol.v2.V2PacketReader.bytesToHex;
import static org.alloy.protocol.v2.V2PacketReader.macToHex;
import static org.alloy.protocol.v2.V2PacketReader.readU16BE;
import static org.alloy.protocol.v2.V2PacketReader.readU32BE;

final class V2FrameJson {
    private V2FrameJson() {}

    static Map<String, Object> fromInbound(V2Frame frame) {
        Map<String, Object> m = base(frame);
        byte[] p = frame.payload != null ? frame.payload : new byte[0];
        m.put("payloadHex", bytesToHex(p));
        try {
            switch (frame.type) {
                case V2ProtocolConstants.TYPE_SYNC -> {
                    // version(1) | MAC(6) | deviceType(1) | session(4)
                    if (p.length >= 12) {
                        m.put("protocolVersion", p[0] & 0xFF);
                        m.put("mac", macToHex(Arrays.copyOfRange(p, 1, 7)));
                        m.put("deviceType", p[7] & 0xFF);
                        m.put("session", readU32BE(p, 8));
                    }
                }
                case V2ProtocolConstants.TYPE_STATE, V2ProtocolConstants.TYPE_HISTORY_RECORD -> {
                    if (p.length >= 6) {
                        m.put("token", readU16BE(p, 0));
                        m.put("packetIndex", readU32BE(p, 2));
                        m.put("wtinfoHex", bytesToHex(Arrays.copyOfRange(p, 2, p.length)));
                    }
                }
                case V2ProtocolConstants.TYPE_SESSION_INFO -> {
                    if (p.length >= 22) {
                        m.put("token", readU16BE(p, 0));
                        m.put("session", readU32BE(p, 2));
                        m.put("firstIndex", readU32BE(p, 6));
                        m.put("firstTime", readU32BE(p, 10));
                        m.put("lastIndex", readU32BE(p, 14));
                        m.put("lastTime", readU32BE(p, 18));
                    }
                }
                case V2ProtocolConstants.TYPE_SET_HIST_SESSION -> {
                    if (p.length >= 6) {
                        m.put("token", readU16BE(p, 0));
                        m.put("session", readU32BE(p, 2));
                    }
                }
                default -> {
                }
            }
        } catch (Exception ignored) {
            m.put("parseError", true);
        }
        return m;
    }

    static Map<String, Object> fromOutbound(byte[] response) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (response == null || response.length < 3) {
            m.put("rawHex", response == null ? "" : bytesToHex(response));
            return m;
        }
        V2Frame f = new V2PacketReader().read(response);
        m.putAll(base(f));
        m.put("rawHex", bytesToHex(response));
        byte[] p = f.payload != null ? f.payload : new byte[0];
        // server payload: time(4) + data + optional command
        if (p.length >= 4) {
            m.put("serverTime", readU32BE(p, 0));
            byte[] dataAndCmd = Arrays.copyOfRange(p, 4, p.length);
            m.put("dataHex", bytesToHex(dataAndCmd));
            if (dataAndCmd.length >= 1) {
                int maybeCmd = dataAndCmd[dataAndCmd.length - 1] & 0xFF;
                // better: detect known command prefixes near end
                for (int i = 0; i < dataAndCmd.length; i++) {
                    int t = dataAndCmd[i] & 0xFF;
                    if (t == 0x03 || t == 0x05 || t == 0x08 || t == 0x09) {
                        m.put("serverCommandType", t);
                        m.put("serverCommandHex", bytesToHex(Arrays.copyOfRange(dataAndCmd, i, dataAndCmd.length)));
                        break;
                    }
                }
            }
        }
        return m;
    }

    private static Map<String, Object> base(V2Frame f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", f.type & 0xFF);
        m.put("typeHex", String.format("0x%02X", f.type & 0xFF));
        m.put("crcOk", f.crcOk);
        m.put("totalBytes", f.totalBytes);
        return m;
    }
}
