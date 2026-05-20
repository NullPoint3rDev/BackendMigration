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

        // Парсим все данные как единую структуру WTINFO_TypeDef
        int[] bytes = hexStringToIntArray(hex);
        if (bytes == null) return null;

        // Убрали логирование для ускорения

        CorePacket p = new CorePacket();
        try {
            int off = 0;

            // Парсим поля согласно структуре WTINFO_TypeDef
            // НЕ пропускаем ничего - начинаем с самого начала

            // Парсим поля согласно структуре WTINFO_TypeDef
            // 1. uint32_t Index (big-endian)
            if (off + 3 < bytes.length) {
                p.index = readU32BE(bytes, off); off += 4;
            }

            // 2. uint8_t Hours
            if (off < bytes.length) p.hours = readU8(bytes, off++);

            // 3. uint8_t Minutes
            if (off < bytes.length) p.minutes = readU8(bytes, off++);

            // 4. uint8_t Seconds
            if (off < bytes.length) p.seconds = readU8(bytes, off++);

            // 5. uint8_t Date
            if (off < bytes.length) p.date = readU8(bytes, off++);

            // 6. uint8_t Month
            if (off < bytes.length) p.month = readU8(bytes, off++);

            // 7. uint8_t Year
            if (off < bytes.length) p.year = readU8(bytes, off++);

            // 8. uint16_t reserve (big-endian)
            if (off + 1 < bytes.length) { p.reserve = readU16BE(bytes, off); off += 2; }

            // 9. int8_t flags
            if (off < bytes.length) p.flags = readI8(bytes, off++);

            // 10. int8_t WeldingMachineState
            if (off < bytes.length) p.weldingMachineState = readI8(bytes, off++);

            // 11. int16_t GasFlow (big-endian)
            if (off + 1 < bytes.length) { p.gasFlow = readI16BE(bytes, off); off += 2; }

            // 12. int16_t WeldingCurrent (big-endian)
            if (off + 1 < bytes.length) { p.weldingCurrent = readI16BE(bytes, off); off += 2; }

            // 13. int16_t WeldingVoltage (big-endian)
            if (off + 1 < bytes.length) { p.weldingVoltage = readI16BE(bytes, off); off += 2; }

            // 14. int16_t JobNumber (big-endian)
            if (off + 1 < bytes.length) { p.jobNumber = readI16BE(bytes, off); off += 2; }

            // 15. int16_t Current (big-endian)
            if (off + 1 < bytes.length) { p.current = readI16BE(bytes, off); off += 2; }

            // 16. int16_t Voltage (big-endian) ← ВОТ ОНО!
            if (off + 1 < bytes.length) { p.voltage = readI16BE(bytes, off); off += 2; }

            // 17. int16_t Inductance (big-endian)
            if (off + 1 < bytes.length) { p.inductance = readI16BE(bytes, off); off += 2; }

            // 18. int16_t Errors1 (big-endian)
            if (off + 1 < bytes.length) { p.errors1 = readI16BE(bytes, off); off += 2; }

            // 19. int16_t Errors2 (big-endian)
            if (off + 1 < bytes.length) { p.errors2 = readI16BE(bytes, off); off += 2; }

            // 20. int16_t Errors3 (big-endian)
            if (off + 1 < bytes.length) { p.errors3 = readI16BE(bytes, off); off += 2; }

            // 21. int16_t VoltagePhaseA (big-endian)
            if (off + 1 < bytes.length) { p.voltagePhaseA = readI16BE(bytes, off); off += 2; }

            // 22. int16_t VoltagePhaseB (big-endian)
            if (off + 1 < bytes.length) { p.voltagePhaseB = readI16BE(bytes, off); off += 2; }

            // 23. int16_t VoltagePhaseC (big-endian)
            if (off + 1 < bytes.length) { p.voltagePhaseC = readI16BE(bytes, off); off += 2; }

            // 24. int16_t ChillerTemperature1 (big-endian)
            if (off + 1 < bytes.length) { p.chillerTemperature1 = readI16BE(bytes, off); off += 2; }

            // 25. int16_t ChillerTemperature2 (big-endian)
            if (off + 1 < bytes.length) { p.chillerTemperature2 = readI16BE(bytes, off); off += 2; }

            // 26. int16_t PrimaryCoilTemperature (big-endian)
            if (off + 1 < bytes.length) { p.primaryCoilTemperature = readI16BE(bytes, off); off += 2; }

            // 27. int16_t SecondaryCoilTemperature (big-endian)
            if (off + 1 < bytes.length) { p.secondaryCoilTemperature = readI16BE(bytes, off); off += 2; }

            // 28. uint32_t WireIndex (big-endian)
            if (off + 3 < bytes.length) { p.wireIndex = readU32BE(bytes, off); off += 4; }

            // 29. uint64_t RFID_Data (little-endian)
            if(off + 7 <bytes.length) {p.rfidData = readU64LE(bytes, off); off += 8;}

            // 30-35. six uint8 tail parameters (each 1 byte)
            if (off < bytes.length) p.weldingMode = readU8(bytes, off++);
            if (off < bytes.length) p.weldingMaterial = readU8(bytes, off++);
            if (off < bytes.length) p.weldingGas = readU8(bytes, off++);
            if (off < bytes.length) p.weldingWireDiameter = readU8(bytes, off++);
            if (off < bytes.length) p.burnerMode = readU8(bytes, off++);
            if (off < bytes.length) p.memoryCellNumber = readU8(bytes, off++);

            // 36. Warnings: 3 x int16_t (6 bytes) — immediately after memoryCellNumber
            if (off + 1 < bytes.length) { p.warnings1 = readI16BE(bytes, off); off += 2; }
            if (off + 1 < bytes.length) { p.warnings2 = readI16BE(bytes, off); off += 2; }
            if (off + 1 < bytes.length) { p.warnings3 = readI16BE(bytes, off); off += 2; }

            // 37-38. Время работы и время сварки с момента включения (uint32_t each, big-endian)
            if (off + 3 < bytes.length) { p.workTimeSincePowerOn = readU32BE(bytes, off); off += 4; }
            if (off + 3 < bytes.length) { p.weldingTimeSincePowerOn = readU32BE(bytes, off); off += 4; }

            // Убрали логирование для ускорения
        } catch (Exception ex) {
            // System.out.println("[CORE-PARSER] ❌ Ошибка парсинга: " + ex.getMessage());
            ex.printStackTrace();
            // если данных меньше — вернём то, что успели распарсить
        }

        return p;
    }

    private static int readU8(int[] b, int off) { return b[off] & 0xFF; }
    private static int readI8(int[] b, int off) { return b[off]; }

    // Big-endian для индекса и других uint32
    private static int readU16BE(int[] b, int off) { return ((b[off] & 0xFF) << 8) | (b[off+1] & 0xFF); }
    private static int readI16BE(int[] b, int off) {
        int u = readU16BE(b, off);
        if ((u & 0x8000) != 0) return u - 0x10000;
        return u;
    }
    private static long readU32BE(int[] b, int off) {
        return ((long)(b[off] & 0xFF) << 24) | ((long)(b[off+1] & 0xFF) << 16) | ((long)(b[off+2] & 0xFF) << 8) | (long)(b[off+3] & 0xFF);
    }

    // Little-endian для данных пакета
    private static int readU16LE(int[] b, int off) { return (b[off] & 0xFF) | ((b[off+1] & 0xFF) << 8); }
    private static int readI16LE(int[] b, int off) {
        int u = readU16LE(b, off);
        if ((u & 0x8000) != 0) return u - 0x10000;
        return u;
    }
    private static long readU32LE(int[] b, int off) {
        return (long)(b[off] & 0xFF) | ((long)(b[off+1] & 0xFF) << 8) | ((long)(b[off+2] & 0xFF) << 16) | ((long)(b[off+3] & 0xFF) << 24);
    }

    private static long readU64LE(int[] b, int off) {
        return ((long)(b[off] & 0xFF))
                | ((long)(b[off + 1] & 0xFF) << 8)
                | ((long)(b[off + 2] & 0xFF) << 16)
                | ((long)(b[off + 3] & 0xFF) << 24)
                | ((long)(b[off + 4] & 0xFF) << 32)
                | ((long)(b[off + 5] & 0xFF) << 40)
                | ((long)(b[off + 6] & 0xFF) << 48)
                | ((long)(b[off + 7] & 0xFF) << 56);
    }

    public static int[] hexStringToIntArray(String s) {
        if (s == null) return null;
        int len = s.length();
        if (len % 2 != 0) return null;
        int[] data = new int[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            data[i / 2] = (hi << 4) + lo; // Убираем приведение к byte
        }
        return data;
    }
}


