package org.alloy.controllers;

import org.alloy.models.MacRegistryStatus;
import org.alloy.models.dto.MacAddressRegistryDTO;
import org.alloy.models.dto.MacAddressRegistryPageDTO;
import org.alloy.models.dto.MacEquipmentTypeDTO;
import org.alloy.models.entities.UserAccount;
import org.alloy.services.MacAddressRegistryService;
import org.alloy.services.Wt2AccessService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mac-address-registry")
public class MacAddressRegistryController {

    private final MacAddressRegistryService macAddressRegistryService;
    private final Wt2AccessService wt2AccessService;

    public MacAddressRegistryController(MacAddressRegistryService macAddressRegistryService,
                                        Wt2AccessService wt2AccessService) {
        this.macAddressRegistryService = macAddressRegistryService;
        this.wt2AccessService = wt2AccessService;
    }

    @GetMapping
    public ResponseEntity<MacAddressRegistryPageDTO> list(
            @RequestParam(required = false) String searchMac,
            @RequestParam(required = false) List<Integer> typeIds,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanReadMacRegistry(principal);

        List<MacRegistryStatus> parsedStatuses = null;
        if (statuses != null && !statuses.isEmpty()) {
            parsedStatuses = statuses.stream()
                    .map(this::parseStatus)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(macAddressRegistryService.list(
                searchMac, typeIds, parsedStatuses, dateFrom, dateTo,
                sortField, sortDirection, page, pageSize));
    }

    @GetMapping("/equipment-types")
    public ResponseEntity<List<MacEquipmentTypeDTO>> equipmentTypes() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanReadMacRegistry(principal);
        return ResponseEntity.ok(macAddressRegistryService.listEquipmentTypes());
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<MacAddressRegistryDTO>> waitingForEquipment() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanReadMacRegistry(principal);
        return ResponseEntity.ok(macAddressRegistryService.listWaitingForEquipment());
    }

    @GetMapping("/mac-exists")
    public ResponseEntity<Map<String, Object>> macExists(@RequestParam String mac) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanReadMacRegistry(principal);
        boolean inRegistry = macAddressRegistryService.isMacInRegistry(mac);
        return ResponseEntity.ok(Map.of("exists", inRegistry));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateMacRequest request) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanAddMacRegistry(principal);
        UserAccount actor = wt2AccessService.findActiveByUserName(principal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
        try {
            MacAddressRegistryDTO created = macAddressRegistryService.create(
                    request.getMac(), request.getEquipmentTypeId(), actor);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/block")
    public ResponseEntity<?> block(@RequestBody IdsRequest request) {
        return mutateAdmin(request.getIds(), () -> macAddressRegistryService.blockByIds(request.getIds()));
    }

    @PostMapping("/unblock")
    public ResponseEntity<?> unblock(@RequestBody IdsRequest request) {
        return mutateAdmin(request.getIds(), () -> macAddressRegistryService.unblockByIds(request.getIds()));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody IdsRequest request) {
        return mutateAdmin(request.getIds(), () -> macAddressRegistryService.deleteByIds(request.getIds()));
    }

    private ResponseEntity<?> mutateAdmin(List<Integer> ids, Runnable action) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanAdminMacRegistry(principal);
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Не выбраны записи"));
        }
        try {
            action.run();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private MacRegistryStatus parseStatus(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Unknown status");
        }
        switch (raw.trim().toUpperCase()) {
            case "ACTIVE":
            case "АКТИВЕН":
                return MacRegistryStatus.ACTIVE;
            case "WAITING":
            case "ОЖИДАНИЕ":
                return MacRegistryStatus.WAITING;
            case "BLOCKED":
            case "ЗАБЛОКИРОВАН":
                return MacRegistryStatus.BLOCKED;
            default:
                return MacRegistryStatus.valueOf(raw.trim().toUpperCase());
        }
    }

    public static class CreateMacRequest {
        private String mac;
        private Integer equipmentTypeId;

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public Integer getEquipmentTypeId() {
            return equipmentTypeId;
        }

        public void setEquipmentTypeId(Integer equipmentTypeId) {
            this.equipmentTypeId = equipmentTypeId;
        }
    }

    public static class IdsRequest {
        private List<Integer> ids;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }
    }
}
