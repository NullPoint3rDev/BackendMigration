package org.alloy.controllers;

import org.alloy.services.*;
import org.alloy.models.weldingmachine.StateSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.alloy.models.dto.WeldingMachineDailyStatsDTO;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;

/**
 * Контроллер для управления archive-style TCP сервером
 */
@RestController
@RequestMapping("/archive-devices")
@CrossOrigin(origins = "*")
public class ArchiveDeviceController {

    @Autowired
    private ArchiveStyleTcpListener tcpListener;

    @Autowired
    private ArchiveStyleOutboundService outboundService;

    @Autowired
    private ArchiveIncomingPacketsWorker packetsWorker;

    @Autowired
    private WeldingDeviceManagerService deviceManager;

    @Autowired
    private TelemetryHistoryService telemetryHistoryService;

    @Autowired
    private WeldingMachineDailyStatsService weldingMachineDailyStatsService;

    /**
     * Получить текущее состояние устройства (как в archive проекте)
     */
    @GetMapping("/panel-state")
    public ResponseEntity<Object> getPanelState(@RequestParam String mac) {
        try {
            // Получаем состояние устройства (как в archive проекте)
            StateSummary state = deviceManager.getDeviceState(mac);

            if (state == null) {
                // Если нет данных - возвращаем null (как в archive проекте)
                return ResponseEntity.ok(null);
            }

            // Проверяем, свежие ли данные (как в archive проекте - 10 секунд)
            boolean isConnected = deviceManager.isDeviceConnected(mac);

            if (!isConnected) {
                // Если данные устарели - возвращаем null (как в archive проекте)
                return ResponseEntity.ok(null);
            }

            // Если данные свежие - возвращаем state (как в archive проекте)
            return ResponseEntity.ok(state);

        } catch (Exception e) {
            // При ошибке возвращаем null (как в archive проекте)
            return ResponseEntity.ok(null);
        }
    }

    /**
     * История телеметрии по аппарату за период (максимум 24 часа).
     * fromMs/toMs — epoch millis (клиент присылает из datetime-local).
     */
    /**
     * Суточная статистика мониторинга (проволока кг, таймеры активности) — пересчёт из БД.
     */
    @GetMapping("/daily-stats")
    public ResponseEntity<?> getDailyStats(
            @RequestParam String mac,
            @RequestParam(required = false) String date
    ) {
        try {
            LocalDate statDate = null;
            if (date != null && !date.isBlank()) {
                statDate = LocalDate.parse(date.trim());
            }
            WeldingMachineDailyStatsDTO dto = weldingMachineDailyStatsService.getDailyStatsByMac(mac, statDate);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/telemetry-history")
    public ResponseEntity<?> getTelemetryHistory(
            @RequestParam String mac,
            @RequestParam long fromMs,
            @RequestParam long toMs
    ) {
        if (toMs <= fromMs) {
            return ResponseEntity.badRequest().body(Map.of("error", "\"toMs\" must be greater than \"fromMs\""));
        }
        long max = 24L * 60L * 60L * 1000L;
        if (toMs - fromMs > max) {
            return ResponseEntity.badRequest().body(Map.of("error", "Max period is 24 hours"));
        }
        List<?> points = telemetryHistoryService.getTelemetryHistory(mac, fromMs, toMs);
        return ResponseEntity.ok(points);
    }

    /**
     * Получить статистику подключений
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getConnectionStatistics() {
        Map<String, Object> stats = tcpListener.getConnectionStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Получить статистику воркера пакетов
     */
    @GetMapping("/worker-statistics")
    public ResponseEntity<Map<String, Object>> getWorkerStatistics() {
        Map<String, Object> stats = packetsWorker.getWorkerStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Получить статистику исходящих пакетов
     */
    @GetMapping("/outbound-statistics")
    public ResponseEntity<Map<String, Object>> getOutboundStatistics() {
        Map<String, Object> stats = outboundService.getOutboundStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Отправить команду устройству
     */
    @PostMapping("/send-command")
    public ResponseEntity<Map<String, Object>> sendCommand(
            @RequestParam String mac,
            @RequestParam String command) {

        try {
            outboundService.setOutboundPacket(mac, command);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Команда отправлена",
                    "mac", mac,
                    "command", command
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Ошибка отправки команды: " + e.getMessage(),
                    "mac", mac,
                    "command", command
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Отправить команду синхронизации времени
     */
    @PostMapping("/send-timesync")
    public ResponseEntity<Map<String, Object>> sendTimeSync(@RequestParam String mac) {
        try {
            String timeSyncCommand = outboundService.buildTimeSyncMessage(mac, true);
            outboundService.setOutboundPacket(mac, timeSyncCommand);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Команда синхронизации времени отправлена",
                    "mac", mac,
                    "command", timeSyncCommand
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Ошибка отправки синхронизации времени: " + e.getMessage(),
                    "mac", mac
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Отправить команду запроса статуса
     */
    @PostMapping("/request-status")
    public ResponseEntity<Map<String, Object>> requestStatus(@RequestParam String mac) {
        try {
            String statusCommand = outboundService.buildStatusRequest(mac);
            outboundService.setOutboundPacket(mac, statusCommand);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Запрос статуса отправлен",
                    "mac", mac,
                    "command", statusCommand
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Ошибка отправки запроса статуса: " + e.getMessage(),
                    "mac", mac
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Отправить команду сброса
     */
    @PostMapping("/reset-device")
    public ResponseEntity<Map<String, Object>> resetDevice(@RequestParam String mac) {
        try {
            String resetCommand = outboundService.buildResetCommand(mac);
            outboundService.setOutboundPacket(mac, resetCommand);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Команда сброса отправлена",
                    "mac", mac,
                    "command", resetCommand
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Ошибка отправки команды сброса: " + e.getMessage(),
                    "mac", mac
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Отправить команду управления
     */
    @PostMapping("/send-control")
    public ResponseEntity<Map<String, Object>> sendControl(
            @RequestParam String mac,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer voltage,
            @RequestParam(required = false) Integer gasFlow) {

        try {
            Map<String, Object> parameters = Map.of(
                    "current", current != null ? current : 0,
                    "voltage", voltage != null ? voltage : 0,
                    "gasFlow", gasFlow != null ? gasFlow : 0
            );

            String controlCommand = outboundService.buildControlCommand(mac, parameters);
            outboundService.setOutboundPacket(mac, controlCommand);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Команда управления отправлена",
                    "mac", mac,
                    "command", controlCommand,
                    "parameters", parameters
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Ошибка отправки команды управления: " + e.getMessage(),
                    "mac", mac
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Очистить очередь исходящих пакетов
     */
    @PostMapping("/clear-outbound-queue")
    public ResponseEntity<Map<String, Object>> clearOutboundQueue() {
        try {
            ArchiveOutboundPacketsRepository.clear();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Очередь исходящих пакетов очищена"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Ошибка очистки очереди: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Получить размер очереди входящих пакетов
     */
    @GetMapping("/incoming-queue-size")
    public ResponseEntity<Map<String, Object>> getIncomingQueueSize() {
        try {
            int queueSize = ArchiveIncomingPacketsQueue.size();
            boolean isEmpty = ArchiveIncomingPacketsQueue.isEmpty();

            Map<String, Object> response = Map.of(
                    "queueSize", queueSize,
                    "isEmpty", isEmpty
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "error", "Ошибка получения размера очереди: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Получить размер очереди исходящих пакетов
     */
    @GetMapping("/outbound-queue-size")
    public ResponseEntity<Map<String, Object>> getOutboundQueueSize() {
        try {
            int queueSize = ArchiveOutboundPacketsRepository.size();
            boolean isEmpty = ArchiveOutboundPacketsRepository.isEmpty();

            Map<String, Object> response = Map.of(
                    "queueSize", queueSize,
                    "isEmpty", isEmpty
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "error", "Ошибка получения размера очереди: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(response);
        }
    }
}
