package org.alloy.services;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CorePacketParser {

    // Ожидается строка вида :<MAC>;HEX_DATA[...]
    public static CorePacket parse(String frame) {
        if (frame == null) return null;

        int semi = frame.indexOf(';');
        if (semi < 0 || semi + 1 >= frame.length()) return null;

        String hex = frame.substring(semi + 1).trim();

        // Последние 2 байта могут быть CRC/концовка, но структура фиксирована, возьмём минимально нужную длину
        byte[] bytes = hexStringToByteArray(hex);
        if (bytes == null) return null;

        // Парсинг little-endian полей, как у STM (уточнено на практике: Index = 4 байта LE и т.д.)
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        CorePacket p = new CorePacket();
        try {
            // Позиции согласно WTINFO_TypeDef
            p.index = intToUint(bb.getInt());            // uint32

            p.hours = byteToUint(bb.get());              // uint8
            p.minutes = byteToUint(bb.get());            // uint8
            p.seconds = byteToUint(bb.get());            // uint8
            p.date = byteToUint(bb.get());               // uint8

            p.month = byteToUint(bb.get());              // uint8
            p.year = byteToUint(bb.get());               // uint8
            p.reserve = shortToUint(bb.getShort());      // uint16

            p.flags = bb.get();                          // int8
            p.weldingMachineState = bb.get();            // int8
            p.gasFlow = bb.getShort();                   // int16

            p.weldingCurrent = bb.getShort();            // int16
            p.weldingVoltage = bb.getShort();            // int16

            p.jobNumber = bb.getShort();                 // int16
            p.current = bb.getShort();                   // int16

            p.voltage = bb.getShort();                   // int16
            p.inductance = bb.getShort();                // int16

            p.errors1 = bb.getShort();                   // int16
            p.errors2 = bb.getShort();                   // int16

            p.errors3 = bb.getShort();                   // int16
            p.voltagePhaseA = bb.getShort();             // int16

            p.voltagePhaseB = bb.getShort();             // int16
            p.voltagePhaseC = bb.getShort();             // int16

            p.chillerTemperature1 = bb.getShort();       // int16
            p.chillerTemperature2 = bb.getShort();       // int16

            p.primaryCoilTemperature = bb.getShort();    // int16
            p.secondaryCoilTemperature = bb.getShort();  // int16

            p.wireIndex = intToUint(bb.getInt());        // uint32
        } catch (Exception ex) {
            // если данных меньше — вернём то, что успели распарсить
        }

        return p;
    }

    private static long intToUint(int v) {
        return v & 0xFFFFFFFFL;
    }

    private static int shortToUint(short v) {
        return v & 0xFFFF;
    }

    private static int byteToUint(byte v) {
        return v & 0xFF;
    }

    public static byte[] hexStringToByteArray(String s) {
        if (s == null) return null;
        int len = s.length();
        if (len % 2 != 0) return null;
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}


