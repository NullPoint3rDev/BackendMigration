package org.alloy.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
// import java.time.format.DateTimeFormatter; // пока не используется
import java.util.Map;

/**
 * Сервис для работы с исходящими пакетами в стиле archive проекта
 */
@Service
public class ArchiveStyleOutboundService {
    
    @Value("${welding.archive.outbound.enabled:true}")
    private boolean enabled;
    
    @Value("${welding.archive.outbound.payload:}")
    private String payloadHex;
    
    @Value("${welding.archive.outbound.append_crc8:false}")
    private boolean appendCrc8;
    
    @Value("${welding.archive.timesync.enabled:true}")
    private boolean timeSyncEnabled;
    
    /**
     * Построить сообщение для MAC-адреса
     */
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
     * Построить сообщение синхронизации времени
     * Формат: :MAC;HHMMSSDDMMYY
     */
    public String buildTimeSyncMessage(String mac, boolean appendCrlf) {
        if (!timeSyncEnabled) return null;
        
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
        String hh = to2Hex(now.getHour());
        String mm = to2Hex(now.getMinute());
        String ss = to2Hex(now.getSecond());
        String dd = to2Hex(now.getDayOfMonth());
        String mo = to2Hex(now.getMonthValue());
        int year2 = now.getYear() % 100;
        String yy = to2Hex(year2);
        
        String body = hh + mm + ss + dd + mo + yy;
        if (appendCrlf) {
            body = body + "0D0A";
        }
        
        return ":" + mac.toUpperCase() + ";" + body;
    }
    
    /**
     * Установить пакет для отправки
     */
    public void setOutboundPacket(String mac, String data) {
        if (mac != null && data != null) {
            ArchiveOutboundPacketsRepository.set(mac, data);
        }
    }
    
    /**
     * Получить пакет для отправки
     */
    public ArchivePacket getOutboundPacket(String mac) {
        return ArchiveOutboundPacketsRepository.tryGet(mac);
    }
    
    /**
     * Удалить пакет из очереди отправки
     */
    public void removeOutboundPacket(String mac) {
        ArchiveOutboundPacketsRepository.remove(mac);
    }
    
    /**
     * Проверить, есть ли пакет для отправки
     */
    public boolean hasOutboundPacket(String mac) {
        return ArchiveOutboundPacketsRepository.contains(mac);
    }
    
    /**
     * Построить команду управления для блока мониторинга
     */
    public String buildControlCommand(String mac, Map<String, Object> parameters) {
        if (mac == null || parameters == null) return null;
        
        StringBuilder command = new StringBuilder();
        command.append(":").append(mac.toUpperCase()).append(";");
        
        // Добавляем параметры управления
        // Это упрощенная версия - в реальном проекте может быть более сложная логика
        if (parameters.containsKey("current")) {
            int current = (Integer) parameters.get("current");
            command.append(to2Hex(current));
        }
        
        if (parameters.containsKey("voltage")) {
            int voltage = (Integer) parameters.get("voltage");
            command.append(to2Hex(voltage));
        }
        
        if (parameters.containsKey("gasFlow")) {
            int gasFlow = (Integer) parameters.get("gasFlow");
            command.append(to2Hex(gasFlow));
        }
        
        return command.toString();
    }
    
    /**
     * Построить команду запроса статуса
     */
    public String buildStatusRequest(String mac) {
        if (mac == null) return null;
        return ":" + mac.toUpperCase() + ";STATUS_REQUEST";
    }
    
    /**
     * Построить команду сброса
     */
    public String buildResetCommand(String mac) {
        if (mac == null) return null;
        return ":" + mac.toUpperCase() + ";RESET";
    }
    
    /**
     * Преобразование в 2-символьный hex
     */
    public static String to2Hex(int value) {
        String h = Integer.toHexString(value & 0xFF).toUpperCase();
        return h.length() == 1 ? "0" + h : h;
    }
    
    /**
     * Вычисление CRC8
     */
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
    
    /**
     * Преобразование hex строки в массив байтов
     */
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
     * Получить статистику исходящих пакетов
     */
    public Map<String, Object> getOutboundStatistics() {
        return Map.of(
            "enabled", enabled,
            "timeSyncEnabled", timeSyncEnabled,
            "appendCrc8", appendCrc8,
            "pendingPackets", ArchiveOutboundPacketsRepository.size(),
            "payloadHex", payloadHex != null ? payloadHex : ""
        );
    }
}
