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
        System.out.println("[CORE-PARSER] 📏 Длина данных: " + dataHex.length() + " символов (" + (dataHex.length()/2) + " байт)");
        
        // Отладочный вывод первых 32 символов данных
        if (dataHex.length() >= 32) {
            System.out.println("[CORE-PARSER] 🔍 Первые 32 символа: " + dataHex.substring(0, 32));
        }

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

            // Парсим остальные поля согласно структуре WTINFO_TypeDef
            // БЕЗ пропуска - начинаем с начала данных
            
            p.hours = readU8(bytes, off++);                    // uint8_t Hours
            p.minutes = readU8(bytes, off++);                  // uint8_t Minutes
            p.seconds = readU8(bytes, off++);                  // uint8_t Seconds
            p.date = readU8(bytes, off++);                     // uint8_t Date

            p.month = readU8(bytes, off++);                    // uint8_t Month
            p.year = readU8(bytes, off++);                     // uint8_t Year
            p.reserve = readU16LE(bytes, off); off += 2;       // uint16_t reserve (little-endian)

            p.flags = readI8(bytes, off++);                    // int8_t flags
            p.weldingMachineState = readI8(bytes, off++);      // int8_t WeldingMachineState
            p.gasFlow = readI16LE(bytes, off); off += 2;       // int16_t GasFlow (little-endian)

            p.weldingCurrent = readI16LE(bytes, off); off += 2; // int16_t WeldingCurrent (little-endian)
            p.weldingVoltage = readI16LE(bytes, off); off += 2; // int16_t WeldingVoltage (little-endian)

            p.jobNumber = readI16LE(bytes, off); off += 2;     // int16_t JobNumber (little-endian)
            p.current = readI16LE(bytes, off); off += 2;       // int16_t Current (little-endian)

            p.voltage = readI16LE(bytes, off); off += 2;       // int16_t Voltage (little-endian)
            p.inductance = readI16LE(bytes, off); off += 2;    // int16_t Inductance (little-endian)

            p.errors1 = readI16LE(bytes, off); off += 2;       // int16_t Errors1 (little-endian)
            p.errors2 = readI16LE(bytes, off); off += 2;       // int16_t Errors2 (little-endian)

            p.errors3 = readI16LE(bytes, off); off += 2;       // int16_t Errors3 (little-endian)
            p.voltagePhaseA = readI16LE(bytes, off); off += 2; // int16_t VoltagePhaseA (little-endian)

            p.voltagePhaseB = readI16LE(bytes, off); off += 2; // int16_t VoltagePhaseB (little-endian)
            p.voltagePhaseC = readI16LE(bytes, off); off += 2; // int16_t VoltagePhaseC (little-endian)

            p.chillerTemperature1 = readI16LE(bytes, off); off += 2; // int16_t ChillerTemperature1 (little-endian)
            p.chillerTemperature2 = readI16LE(bytes, off); off += 2; // int16_t ChillerTemperature2 (little-endian)

            p.primaryCoilTemperature = readI16LE(bytes, off); off += 2;   // int16_t PrimaryCoilTemperature (little-endian)
            p.secondaryCoilTemperature = readI16LE(bytes, off); off += 2; // int16_t SecondaryCoilTemperature (little-endian)

            p.wireIndex = readU32LE(bytes, off); off += 4;     // uint32_t WireIndex (little-endian)
            
            System.out.println("[CORE-PARSER] ✅ Пакет успешно распарсен. Смещение: " + off + " байт");
            System.out.println("[CORE-PARSER] 📊 Ключевые параметры:");
            System.out.println("[CORE-PARSER]   - JobNumber: " + p.jobNumber + " (0x" + String.format("%04X", p.jobNumber) + ")");
            System.out.println("[CORE-PARSER]   - Current: " + p.current + " (0x" + String.format("%04X", p.current) + ")");
            System.out.println("[CORE-PARSER]   - Voltage: " + p.voltage + " (0x" + String.format("%04X", p.voltage) + ")");
            System.out.println("[CORE-PARSER]   - WeldingCurrent: " + p.weldingCurrent + " (0x" + String.format("%04X", p.weldingCurrent) + ")");
            System.out.println("[CORE-PARSER]   - WeldingVoltage: " + p.weldingVoltage + " (0x" + String.format("%04X", p.weldingVoltage) + ")");
            
            // Отладочный вывод hex данных для анализа
            System.out.println("[CORE-PARSER] 🔍 Анализ hex данных:");
            if (dataHex.length() >= 32) {
                System.out.println("[CORE-PARSER]   - Первые 32 символа: " + dataHex.substring(0, 32));
            }
            if (dataHex.length() >= 48) {
                System.out.println("[CORE-PARSER]   - Символы 32-48: " + dataHex.substring(32, 48));
            }
        } catch (Exception ex) {
            System.out.println("[CORE-PARSER] ❌ Ошибка парсинга: " + ex.getMessage());
            ex.printStackTrace();
            // если данных меньше — вернём то, что успели распарсить
        }

        return p;
    }

    private static int readU8(byte[] b, int off) { return b[off] & 0xFF; }
    private static int readI8(byte[] b, int off) { return b[off]; }
    
    // Big-endian для индекса и других uint32
    private static int readU16BE(byte[] b, int off) { return ((b[off] & 0xFF) << 8) | (b[off+1] & 0xFF); }
    private static int readI16BE(byte[] b, int off) {
        int u = readU16BE(b, off);
        if ((u & 0x8000) != 0) return u - 0x10000;
        return u;
    }
    private static long readU32BE(byte[] b, int off) {
        return ((long)(b[off] & 0xFF) << 24) | ((long)(b[off+1] & 0xFF) << 16) | ((long)(b[off+2] & 0xFF) << 8) | (long)(b[off+3] & 0xFF);
    }
    
    // Little-endian для данных пакета
    private static int readU16LE(byte[] b, int off) { return (b[off] & 0xFF) | ((b[off+1] & 0xFF) << 8); }
    private static int readI16LE(byte[] b, int off) {
        int u = readU16LE(b, off);
        if ((u & 0x8000) != 0) return u - 0x10000;
        return u;
    }
    private static long readU32LE(byte[] b, int off) {
        return (long)(b[off] & 0xFF) | ((long)(b[off+1] & 0xFF) << 8) | ((long)(b[off+2] & 0xFF) << 16) | ((long)(b[off+3] & 0xFF) << 24);
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


