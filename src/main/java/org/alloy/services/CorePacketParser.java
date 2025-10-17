package org.alloy.services;

public class CorePacketParser {

    // Ожидается строка вида :<MAC>;INDEX_HEX_DATA[...]
    // INDEX - это первые 8 символов после точки с запятой
    public static CorePacket parse(String frame) {
        if (frame == null) return null;

        int semi = frame.indexOf(';');
        if (semi < 0 || semi + 1 >= frame.length()) return null;

        String hex = frame.substring(semi + 1).trim();
        
        // Проверяем, что есть минимум 8 символов для индекса
        if (hex.length() < 8) return null;
        
        // Извлекаем индекс из первых 8 символов
        String indexHex = hex.substring(0, 8);
        String dataHex = hex.substring(8);
        
        System.out.println("[CORE-PARSER] 🔍 Индекс пакета: " + indexHex);
        System.out.println("[CORE-PARSER] 📦 Данные пакета: " + dataHex);

        // Парсим индекс как uint32 (big-endian)
        byte[] indexBytes = hexStringToByteArray(indexHex);
        if (indexBytes == null || indexBytes.length != 4) return null;
        
        // Парсим остальные данные
        byte[] bytes = hexStringToByteArray(dataHex);
        if (bytes == null) return null;

        CorePacket p = new CorePacket();
        try {
            // Устанавливаем индекс из первых 8 символов
            p.index = readU32BE(indexBytes, 0);
            
            int off = 0;

            p.hours = readU8(bytes, off++);
            p.minutes = readU8(bytes, off++);
            p.seconds = readU8(bytes, off++);
            p.date = readU8(bytes, off++);

            p.month = readU8(bytes, off++);
            p.year = readU8(bytes, off++);
            p.reserve = readU16BE(bytes, off); off += 2;        // uint16

            p.flags = readI8(bytes, off++);
            p.weldingMachineState = readI8(bytes, off++);
            p.gasFlow = readI16BE(bytes, off); off += 2;

            p.weldingCurrent = readI16BE(bytes, off); off += 2;
            p.weldingVoltage = readI16BE(bytes, off); off += 2;

            p.jobNumber = readI16BE(bytes, off); off += 2;
            p.current = readI16BE(bytes, off); off += 2;

            p.voltage = readI16BE(bytes, off); off += 2;
            p.inductance = readI16BE(bytes, off); off += 2;

            p.errors1 = readI16BE(bytes, off); off += 2;
            p.errors2 = readI16BE(bytes, off); off += 2;

            p.errors3 = readI16BE(bytes, off); off += 2;
            p.voltagePhaseA = readI16BE(bytes, off); off += 2;

            p.voltagePhaseB = readI16BE(bytes, off); off += 2;
            p.voltagePhaseC = readI16BE(bytes, off); off += 2;

            p.chillerTemperature1 = readI16BE(bytes, off); off += 2;
            p.chillerTemperature2 = readI16BE(bytes, off); off += 2;

            p.primaryCoilTemperature = readI16BE(bytes, off); off += 2;
            p.secondaryCoilTemperature = readI16BE(bytes, off); off += 2;

            p.wireIndex = readU32BE(bytes, off); off += 4;
        } catch (Exception ex) {
            // если данных меньше — вернём то, что успели распарсить
        }

        return p;
    }

    private static int readU8(byte[] b, int off) { return b[off] & 0xFF; }
    private static int readI8(byte[] b, int off) { return b[off]; }
    private static int readU16BE(byte[] b, int off) { return ((b[off] & 0xFF) << 8) | (b[off+1] & 0xFF); }
    private static int readI16BE(byte[] b, int off) {
        int u = readU16BE(b, off);
        if ((u & 0x8000) != 0) return u - 0x10000;
        return u;
    }
    private static long readU32BE(byte[] b, int off) {
        return ((long)(b[off] & 0xFF) << 24) | ((long)(b[off+1] & 0xFF) << 16) | ((long)(b[off+2] & 0xFF) << 8) | (long)(b[off+3] & 0xFF);
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


