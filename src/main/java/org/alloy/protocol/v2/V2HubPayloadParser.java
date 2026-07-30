package org.alloy.protocol.v2;

import static org.alloy.protocol.v2.V2PacketReader.readI16LE;
import static org.alloy.protocol.v2.V2PacketReader.readU16LE;
import static org.alloy.protocol.v2.V2PacketReader.readU32LE;
import static org.alloy.protocol.v2.V2PacketReader.readU64LE;

/**
 * Парсит hub-тело после v2 token (LE). Не трогает Core/ASCII.
 * Размер: 80 байт (index … warnings3), без hub type/len/crc.
 */
public final class V2HubPayloadParser {
    /** index(0)..warnings3(78..79) */
    public static final int BODY_LEN = 80;

    private V2HubPayloadParser() {}

    /** @return null если blob короче минимума */
    public static V2HubPayload parse(byte[] body) {
        if (body == null || body.length < BODY_LEN) {
            return null;
        }
        V2HubPayload h = new V2HubPayload();
        h.packetIndex = readU32LE(body, 0);
        h.unixTime = readU32LE(body, 4) & 0xFFFFFFFFL;
        h.machineState = body[8] & 0xFF;
        h.jobNumber = readU16LE(body, 9);
        h.weldMode = body[11] & 0xFF;
        h.wireMaterial = body[12] & 0xFF;
        h.weldGas = body[13] & 0xFF;
        h.wireDiameter = body[14] & 0xFF;
        h.torchMode = body[15] & 0xFF;
        h.setCurrentA = readU16LE(body, 16);
        h.setWireSpeedMPerMin = fixed10(readI16LE(body, 18));
        h.setVoltageV = fixed10(readI16LE(body, 20));
        h.memoryCell = body[22] & 0xFF;
        h.setInductance = body[23]; // i8
        h.actualCurrentA = readU16LE(body, 24);
        h.actualVoltageV = fixed10(readI16LE(body, 26));
        h.passNumber = readU64LE(body, 28);
        h.mainsPhaseAV = readU16LE(body, 36);
        h.mainsPhaseBV = readU16LE(body, 38);
        h.mainsPhaseCV = readU16LE(body, 40);
        h.tempBvoInC = fixed10(readI16LE(body, 42));
        h.tempBvoOutC = fixed10(readI16LE(body, 44));
        h.tempRadiatorC = fixed10(readI16LE(body, 46));
        h.tempTerminalPlusC = fixed10(readI16LE(body, 48));
        h.tempTerminalMinusC = fixed10(readI16LE(body, 50));
        h.gasFlowLPerMin = fixed10(readI16LE(body, 52));
        h.gasTotalL = readU32LE(body, 54) & 0xFFFFFFFFL;
        h.wireTotalM = readU32LE(body, 58) & 0xFFFFFFFFL;
        h.uptimeSec = readU32LE(body, 62) & 0xFFFFFFFFL;
        h.weldTimeSec = readU32LE(body, 66) & 0xFFFFFFFFL;
        h.errors1 = readU16LE(body, 68);
        h.errors2 = readU16LE(body, 70);
        h.errors3 = readU16LE(body, 72);
        h.warnings1 = readU16LE(body, 74);
        h.warnings2 = readU16LE(body, 76);
        h.warnings3 = readU16LE(body, 78);
        return h;
    }

    /** Index для ACK/gap: LE u32 в начале blob; −1 если нет 4 байт. */
    public static int readPacketIndex(byte[] body) {
        if (body == null || body.length < 4) {
            return -1;
        }
        return readU32LE(body, 0);
    }

    private static double fixed10(int raw) {
        return raw / 10.0;
    }
}
