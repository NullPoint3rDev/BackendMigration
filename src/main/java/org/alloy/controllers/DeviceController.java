package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.WeldingMachineStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;
import org.springframework.web.bind.annotation.RequestParam;
import org.alloy.services.WeldingDataParserService;

@Controller
public class DeviceController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final WeldingDataParserService weldingDataParserService;

    @Autowired
    public DeviceController(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper, WeldingDataParserService weldingDataParserService) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.weldingDataParserService = weldingDataParserService;
    }

    public void sendDeviceData(String data) {
        try {
            messagingTemplate.convertAndSend("/topic/device", data);
            System.out.println("[DEVICE-CONTROLLER] 📤 Отправка сырых данных: " + data);
        } catch (Exception e) {
            System.err.println("[DEVICE-CONTROLLER] ❌ Ошибка отправки данных: " + e.getMessage());
        }
    }
    
    public void sendDeviceState(StateSummary state) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            DeviceStateMessage message = new DeviceStateMessage();
            message.setTimestamp(timestamp);
            message.setState(state);
            String jsonData = objectMapper.writeValueAsString(message);
            
            System.out.println("[DEVICE-CONTROLLER] 📤 Отправка состояния: " + jsonData);
            messagingTemplate.convertAndSend("/topic/device-state", jsonData);
            System.out.println("[DEVICE-CONTROLLER] ✅ Состояние отправлено на /topic/device-state");
            
        } catch (Exception e) {
            System.err.println("[DEVICE-CONTROLLER] ❌ Ошибка отправки состояния: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendDeviceState(StateSummary state, String mac) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            DeviceStateMessage message = new DeviceStateMessage();
            message.setTimestamp(timestamp);
            message.setState(state);
            message.setMac(mac);
            String jsonData = objectMapper.writeValueAsString(message);
            
            // Публикуем ТОЛЬКО в персональный топик устройства (БЕЗ ЛОГИРОВАНИЯ!)
            messagingTemplate.convertAndSend("/topic/device-state/" + mac, jsonData);
            
            // Также отправляем данные в старом формате, но ТОЛЬКО в персональный топик устройства
            if (state.getProperties() != null) {
                StringBuilder dataString = new StringBuilder();
                dataString.append(timestamp).append("|");
                dataString.append(mac).append(":");
                
                for (Map.Entry<String, StateSummaryPropertyValue> entry : state.getProperties().entrySet()) {
                    dataString.append(entry.getKey()).append(":").append(entry.getValue().getValue()).append(";");
                }
                
                String rawData = dataString.toString();
                messagingTemplate.convertAndSend("/topic/device/" + mac, rawData);
            }
            
        } catch (Exception e) {
            System.err.println("[DEVICE-CONTROLLER] ❌ Ошибка отправки состояния: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/device-command")
    @SendTo("/topic/device-response")
    public String handleDeviceCommand(String command) {
        System.out.println("[DEVICE-CONTROLLER] Получена команда: " + command);
        return "Команда получена: " + command;
    }
    
    // Test endpoint for WebSocket
    @GetMapping("/test-websocket")
    public ResponseEntity<String> testWebSocket() {
        try {
            StateSummary testState = new StateSummary();
            testState.setStatus(WeldingMachineStatus.Idle);
            testState.setDateCreated(LocalDateTime.now());
            testState.setLastDatetimeUpdate(LocalDateTime.now());

            sendDeviceState(testState);
            return ResponseEntity.ok("Тестовое сообщение отправлено через WebSocket");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }

    // Test endpoint for current position testing
    @GetMapping("/test-current-position")
    public ResponseEntity<String> testCurrentPosition(@RequestParam(defaultValue = "6") int position) {
        try {
            // Создаем тестовые данные с током в указанной позиции
            String testData = "8CAAB50C4254;";
            
            // Заполняем данные до нужной позиции
            for (int i = 0; i < position; i += 2) {
                testData += "00";
            }
            
            // Добавляем ток 65 (41 в hex) в указанную позицию
            testData += "41";
            
            // Добавляем остальные данные
            for (int i = position + 2; i < 50; i += 2) {
                testData += "00";
            }
            
            System.out.println("[TEST] Тестовые данные: " + testData);
            System.out.println("[TEST] Позиция тока: " + position);
            
            // Парсим данные
            StateSummary state = weldingDataParserService.parseWeldingData(testData, "8CAAB50C4254");
            
            // Отправляем через WebSocket
            sendDeviceState(state);
            
            return ResponseEntity.ok("Тест позиции " + position + " выполнен. Данные: " + testData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }

    // Внутренний класс для сообщений
    public static class DeviceStateMessage {
        private String timestamp;
        private String mac;
        private StateSummary state;

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public StateSummary getState() {
            return state;
        }

        public void setState(StateSummary state) {
            this.state = state;
        }
    }
}