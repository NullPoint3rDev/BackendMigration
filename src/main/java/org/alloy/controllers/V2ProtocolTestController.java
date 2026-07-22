package org.alloy.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alloy.protocol.v2.V2CommandQueue;
import org.alloy.protocol.v2.V2DebugEvent;
import org.alloy.protocol.v2.V2DebugHub;
import org.alloy.protocol.v2.V2HistoryCommand;
import org.alloy.protocol.v2.V2ProtocolConstants;
import org.alloy.protocol.v2.V2ProtocolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only API for protocol v2 page. Does not affect production monitoring UI.
 */
@RestController
@RequestMapping("/v2-protocol-test")
@CrossOrigin(origins = "*")
public class V2ProtocolTestController {

    private final V2ProtocolService protocolService;
    private final V2DebugHub debugHub;
    private final V2CommandQueue commandQueue;

    public V2ProtocolTestController(V2ProtocolService protocolService) {
        this.protocolService = protocolService;
        this.debugHub = protocolService.getDebugHub();
        this.commandQueue = protocolService.getCommandQueue();
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("testMac", V2ProtocolConstants.TEST_MAC);
        m.put("pendingCommands", commandQueue.pendingCount(V2ProtocolConstants.TEST_MAC));
        return m;
    }

    @GetMapping("/events")
    public List<V2DebugEvent> events(@RequestParam(defaultValue = "0") long afterId) {
        return debugHub.since(afterId);
    }

    /**
     * Body examples:
     * {"type":"REQ_SESSION_INFO","session":1}
     * {"type":"REQ_HISTORY","session":1,"from":41,"to":99}
     * {"type":"PRIO_HISTORY"}
     * {"type":"STOP_HISTORY"}
     */
    @PostMapping("/command")
    public ResponseEntity<?> enqueueCommand(@RequestBody Map<String, Object> body) {
        String type = String.valueOf(body.getOrDefault("type", "")).trim().toUpperCase();
        String mac = V2ProtocolConstants.TEST_MAC;
        V2HistoryCommand cmd;
        try {
            cmd = switch (type) {
                case "REQ_SESSION_INFO", "0x03", "3" ->
                        V2HistoryCommand.requestSessionInfo(asInt(body.get("session")));
                case "REQ_HISTORY", "0x05", "5" ->
                        V2HistoryCommand.requestHistory(
                                asInt(body.get("session")),
                                asInt(body.get("from")),
                                asInt(body.get("to")));
                case "PRIO_HISTORY", "0x08", "8" -> V2HistoryCommand.priorityHistory();
                case "STOP_HISTORY", "0x09", "9" -> V2HistoryCommand.stopHistory();
                default -> null;
            };
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        if (cmd == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "unknown type: " + type));
        }
        commandQueue.enqueue(mac, cmd);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "mac", mac,
                "pending", commandQueue.pendingCount(mac)));
    }

    private static int asInt(Object v) {
        if (v == null) {
            throw new IllegalArgumentException("missing number field");
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }
}
