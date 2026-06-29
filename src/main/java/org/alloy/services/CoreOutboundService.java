package org.alloy.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class CoreOutboundService {

    @Value("${core.outbound.enabled:true}")
    private boolean enabled;

    // Чистый HEX без пробелов; без двоеточия/точки с запятой/CRC
    @Value("${core.outbound.payload:}")
    private String payloadHex;

    // Если true — посчитать CRC8 (poly 0x07) по телу и добавить 1 байт (2 hex)
    @Value("${core.outbound.append_crc8:false}")
    private boolean appendCrc8;

    @Value("${core.timesync.enabled:true}")
    private boolean timeSyncEnabled;

    public String buildMessageForMac(String mac) {
        if (!enabled) return null;
        if (payloadHex == null || payloadHex.isEmpty()) return null;

        String body = payloadHex.toUpperCase();
        if (appendCrc8) {
            byte[] bytes = hexStringToByteArray(body);
            if (bytes == null) return null;
            int crc = crc8(bytes);
            String crcHex = to2Hex(crc);
            body = body + crcHex;
        }

        return ":" + mac.toUpperCase() + ";" + body;
    }

        /**
         * Возвращает сообщение синхронизации времени формата :MAC;HHMMSSDDMMYY[0D0A]
         * где каждая компонента записана как 2-символьный HEX (00-FF).
         */
        public String buildTimeSyncMessage(String mac, boolean appendCrlf) {
            if (!timeSyncEnabled) return null;
            java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Europe/Moscow"));
            String hh = to2Hex(now.getHour());
            String mm = to2Hex(now.getMinute());
            String ss = to2Hex(now.getSecond());
            String dd = to2Hex(now.getDayOfMonth());
            String mo = to2Hex(now.getMonthValue());
            int year2 = now.getYear() % 100;
            String yy = to2Hex(year2);
            String body = hh + mm + ss + dd + mo + yy;
           // if (appendCrlf) body = body + "0D0A";
            return ":" + mac.toUpperCase() + ";" + body;
    }

    public static String to2Hex(int value) {
        String h = Integer.toHexString(value & 0xFF).toUpperCase();
        return h.length() == 1 ? "0" + h : h;
    }

    public static int crc8(byte[] data) {
        int crc = 0x00;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = (crc << 1) ^ 0x07;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFF;
            }
        }
        return crc & 0xFF;
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
    
    /**
     * Проверяет, является ли устройство Core по MAC-адресу
     */
    public boolean isCoreDevice(String mac) {
        if (mac == null) return false;
        
        // Проверяем по MAC-адресу
        return mac.equalsIgnoreCase("E09806083396") || mac.equalsIgnoreCase("ECFABCCA42E8");
    }
    
    /**
     * Создает эхо-ответ для Core устройства (возвращает те же данные, что получили)
     */
    public String buildEchoResponse(String originalData) {
        if (originalData == null || originalData.isEmpty()) {
            return null;
        }
        
        // Просто возвращаем те же данные, что получили
        return originalData;
    }
}


