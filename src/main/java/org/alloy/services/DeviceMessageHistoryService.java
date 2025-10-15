package org.alloy.services;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
public class DeviceMessageHistoryService {

    // История сообщений (последние 100)
    private final ConcurrentLinkedQueue<DeviceMessage> messageHistory = new ConcurrentLinkedQueue<>();
    private final int MAX_HISTORY_SIZE = 100;

    /**
     * Добавить сообщение в историю
     */
    public void addMessage(String mac, String data, String type) {
        DeviceMessage message = new DeviceMessage();
        message.setMac(mac);
        message.setData(data);
        message.setType(type);
        message.setTimestamp(LocalDateTime.now());
        
        messageHistory.offer(message);
        
        // Ограничиваем размер истории
        while (messageHistory.size() > MAX_HISTORY_SIZE) {
            messageHistory.poll();
        }
    }

    /**
     * Получить историю сообщений для конкретной платы
     */
    public List<DeviceMessage> getDeviceHistory(String mac) {
        return messageHistory.stream()
            .filter(msg -> msg.getMac().equalsIgnoreCase(mac))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(100)
            .collect(Collectors.toList());
    }

    /**
     * Получить всю историю сообщений
     */
    public List<DeviceMessage> getAllHistory() {
        return messageHistory.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(100)
            .collect(Collectors.toList());
    }

    /**
     * Очистить историю сообщений
     */
    public void clearHistory() {
        messageHistory.clear();
    }

    /**
     * Получить статистику по сообщениям
     */
    public java.util.Map<String, Long> getMessageStatistics() {
        return messageHistory.stream()
            .collect(Collectors.groupingBy(
                DeviceMessage::getMac,
                Collectors.counting()
            ));
    }

    /**
     * Получить общее количество сообщений
     */
    public int getTotalMessageCount() {
        return messageHistory.size();
    }

    // Внутренний класс для сообщений
    public static class DeviceMessage {
        private String mac;
        private String data;
        private String type; // "received" или "sent"
        private LocalDateTime timestamp;

        // Getters and setters
        public String getMac() { return mac; }
        public void setMac(String mac) { this.mac = mac; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
