package org.alloy.services;

import jakarta.persistence.criteria.Predicate;
import org.alloy.models.MacRegistryStatus;
import org.alloy.models.dto.MacAddressRegistryDTO;
import org.alloy.models.dto.MacAddressRegistryPageDTO;
import org.alloy.models.dto.MacEquipmentTypeDTO;
import org.alloy.models.entities.MacAddressRegistry;
import org.alloy.models.entities.MacEquipmentType;
import org.alloy.models.entities.UserAccount;
import org.alloy.repositories.MacAddressRegistryRepository;
import org.alloy.repositories.MacEquipmentTypeRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MacAddressRegistryService {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd.MM.yy/HH:mm");
    private static final long SESSION_GAP_MS = 10 * 60 * 1000L;

    private final MacAddressRegistryRepository registryRepository;
    private final MacEquipmentTypeRepository equipmentTypeRepository;
    private final WeldingMachineRepository weldingMachineRepository;
    private final DeviceModelService deviceModelService;
    private final ArchiveStyleTcpListener archiveStyleTcpListener;

    @Autowired
    public MacAddressRegistryService(MacAddressRegistryRepository registryRepository,
                                     MacEquipmentTypeRepository equipmentTypeRepository,
                                     WeldingMachineRepository weldingMachineRepository,
                                     DeviceModelService deviceModelService,
                                     @Lazy ArchiveStyleTcpListener archiveStyleTcpListener) {
        this.registryRepository = registryRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
        this.weldingMachineRepository = weldingMachineRepository;
        this.deviceModelService = deviceModelService;
        this.archiveStyleTcpListener = archiveStyleTcpListener;
    }

    public List<MacEquipmentTypeDTO> listEquipmentTypes() {
        return equipmentTypeRepository.findAll(Sort.by("name")).stream()
                .map(this::toTypeDto)
                .collect(Collectors.toList());
    }

    public MacAddressRegistryPageDTO list(
            String searchMac,
            List<Integer> typeIds,
            List<MacRegistryStatus> statuses,
            LocalDate dateFrom,
            LocalDate dateTo,
            String sortField,
            String sortDirection,
            int page,
            int pageSize
    ) {
        Specification<MacAddressRegistry> spec = buildSpec(searchMac, typeIds, statuses, dateFrom, dateTo);
        Sort sort = resolveSort(sortField, sortDirection);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(pageSize, 1), sort);
        Page<MacAddressRegistry> result = registryRepository.findAll(spec, pageable);

        MacAddressRegistryPageDTO dto = new MacAddressRegistryPageDTO();
        dto.setItems(result.getContent().stream().map(this::toDto).collect(Collectors.toList()));
        dto.setTotal(result.getTotalElements());
        dto.setPage(result.getNumber());
        dto.setPageSize(result.getSize());
        return dto;
    }

    public MacAddressRegistryDTO create(String mac, Integer equipmentTypeId, UserAccount actor) {
        String normalized = normalizeMacOrThrow(mac);
        if (registryRepository.existsByMac(normalized)) {
            throw new IllegalArgumentException("MAC-адрес уже есть в реестре");
        }
        if (weldingMachineRepository.findActiveByMac(normalized).isPresent()) {
            throw new IllegalArgumentException("MAC-адрес уже привязан к аппарату");
        }
        MacEquipmentType type = equipmentTypeRepository.findById(equipmentTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный тип оборудования"));

        MacAddressRegistry row = new MacAddressRegistry();
        row.setMac(normalized);
        row.setMacEquipmentTypeId(type.getId());
        row.setStatus(MacRegistryStatus.WAITING);
        row.setEnteredByName(resolveEnteredByName(actor));
        row.setSessionCount(0L);
        row.setDateCreated(LocalDateTime.now(ZoneOffset.UTC));
        MacAddressRegistry saved = registryRepository.save(row);
        archiveStyleTcpListener.invalidateMacAllowCache(normalized);
        return toDto(saved);
    }

    public void blockByIds(List<Integer> ids) {
        applyBlockedState(ids, MacRegistryStatus.BLOCKED);
    }

    public void unblockByIds(List<Integer> ids) {
        applyBlockedState(ids, MacRegistryStatus.WAITING);
    }

    public void deleteByIds(List<Integer> ids) {
        List<MacAddressRegistry> rows = registryRepository.findByIdIn(ids);
        for (MacAddressRegistry row : rows) {
            if (row.getWeldingMachineId() != null) {
                throw new IllegalArgumentException(
                        "Нельзя удалить MAC, привязанный к аппарату: " + formatMacForDisplay(row.getMac()));
            }
        }
        List<String> macs = rows.stream().map(MacAddressRegistry::getMac).collect(Collectors.toList());
        registryRepository.deleteAll(rows);
        macs.forEach(archiveStyleTcpListener::invalidateMacAllowCache);
    }

    public boolean isMacInRegistry(String mac) {
        String normalized = deviceModelService.normalizeMac(mac);
        if (!deviceModelService.isValidMacFormat(normalized)) {
            return false;
        }
        return registryRepository.existsByMac(normalized);
    }

    public boolean isAllowedForTcp(String mac) {
        String normalized = deviceModelService.normalizeMac(mac);
        if (!deviceModelService.isValidMacFormat(normalized)) {
            return false;
        }
        return registryRepository.findByMac(normalized)
                .map(row -> row.getStatus() != MacRegistryStatus.BLOCKED)
                .orElse(false);
    }

    public void assertMacAvailableForEquipmentCreate(String mac) {
        String normalized = normalizeMacOrThrow(mac);
        MacAddressRegistry row = registryRepository.findByMac(normalized)
                .orElseThrow(() -> new IllegalArgumentException(
                        "MAC-адрес не найден в реестре. Сначала добавьте его на странице MAC Адреса."));
        if (row.getStatus() == MacRegistryStatus.BLOCKED) {
            throw new IllegalArgumentException("MAC-адрес заблокирован");
        }
        if (row.getStatus() != MacRegistryStatus.WAITING) {
            throw new IllegalArgumentException("MAC-адрес уже привязан к аппарату или недоступен");
        }
    }

    public void activateForWeldingMachine(String mac, Integer weldingMachineId) {
        String normalized = normalizeMacOrThrow(mac);
        MacAddressRegistry row = registryRepository.findByMac(normalized)
                .orElseThrow(() -> new IllegalArgumentException("MAC-адрес не найден в реестре"));
        if (row.getStatus() == MacRegistryStatus.BLOCKED) {
            throw new IllegalArgumentException("MAC-адрес заблокирован");
        }
        row.setStatus(MacRegistryStatus.ACTIVE);
        row.setWeldingMachineId(weldingMachineId);
        registryRepository.save(row);
        archiveStyleTcpListener.invalidateMacAllowCache(normalized);
    }

    public void releaseWeldingMachineMac(String mac) {
        if (mac == null || mac.isBlank()) {
            return;
        }
        String normalized = deviceModelService.normalizeMac(mac);
        registryRepository.findByMac(normalized).ifPresent(row -> {
            row.setStatus(MacRegistryStatus.WAITING);
            row.setWeldingMachineId(null);
            registryRepository.save(row);
            archiveStyleTcpListener.invalidateMacAllowCache(normalized);
        });
    }

    public void onWeldingMachineMacChanged(String oldMac, String newMac, Integer weldingMachineId) {
        if (oldMac != null && !oldMac.isBlank()) {
            releaseWeldingMachineMac(oldMac);
        }
        if (newMac != null && !newMac.isBlank()) {
            activateForWeldingMachine(newMac, weldingMachineId);
        }
    }

    /** Первый пакет после паузы >10 мин — новая сессия. */
    public void recordPacket(String mac) {
        String normalized = deviceModelService.normalizeMac(mac);
        if (!deviceModelService.isValidMacFormat(normalized)) {
            return;
        }
        registryRepository.findByMac(normalized).ifPresent(row -> {
            if (row.getStatus() == MacRegistryStatus.BLOCKED) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last = row.getLastPacketAt();
            if (last == null || Duration.between(last, now).toMillis() > SESSION_GAP_MS) {
                row.setSessionCount(row.getSessionCount() + 1L);
            }
            row.setLastPacketAt(now);
            registryRepository.save(row);
        });
    }

    public List<MacAddressRegistryDTO> listWaitingForEquipment() {
        return registryRepository.findByStatus(MacRegistryStatus.WAITING).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void applyBlockedState(List<Integer> ids, MacRegistryStatus targetStatus) {
        List<MacAddressRegistry> rows = registryRepository.findByIdIn(ids);
        for (MacAddressRegistry row : rows) {
            if (targetStatus == MacRegistryStatus.BLOCKED) {
                if (row.getWeldingMachineId() != null) {
                    throw new IllegalArgumentException(
                            "Нельзя заблокировать MAC, привязанный к аппарату: "
                                    + formatMacForDisplay(row.getMac()));
                }
                row.setStatus(MacRegistryStatus.BLOCKED);
            } else if (row.getStatus() == MacRegistryStatus.BLOCKED) {
                row.setStatus(row.getWeldingMachineId() != null
                        ? MacRegistryStatus.ACTIVE
                        : MacRegistryStatus.WAITING);
            }
            archiveStyleTcpListener.invalidateMacAllowCache(row.getMac());
        }
        registryRepository.saveAll(rows);
    }

    private Specification<MacAddressRegistry> buildSpec(
            String searchMac,
            List<Integer> typeIds,
            List<MacRegistryStatus> statuses,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (searchMac != null && !searchMac.isBlank()) {
                String digits = searchMac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
                if (!digits.isEmpty()) {
                    predicates.add(cb.like(root.get("mac"), "%" + digits + "%"));
                }
            }
            if (typeIds != null && !typeIds.isEmpty()) {
                predicates.add(root.get("macEquipmentTypeId").in(typeIds));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (dateFrom != null) {
                LocalDateTime fromUtc = dateFrom.atStartOfDay(MOSCOW).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateCreated"), fromUtc));
            }
            if (dateTo != null) {
                LocalDateTime toUtc = dateTo.plusDays(1).atStartOfDay(MOSCOW)
                        .withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
                predicates.add(cb.lessThan(root.get("dateCreated"), toUtc));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort resolveSort(String sortField, String sortDirection) {
        String field = sortField == null ? "id" : sortField;
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        switch (field) {
            case "mac":
                return Sort.by(dir, "mac");
            case "equipmentTypeName":
                return Sort.by(dir, "macEquipmentTypeId");
            case "dateCreated":
                return Sort.by(dir, "dateCreated");
            case "enteredByName":
                return Sort.by(dir, "enteredByName");
            case "sessionCount":
                return Sort.by(dir, "sessionCount");
            case "status":
                return Sort.by(dir, "status");
            default:
                return Sort.by(dir, "id");
        }
    }

    private String normalizeMacOrThrow(String mac) {
        if (mac == null || mac.isBlank()) {
            throw new IllegalArgumentException("MAC-адрес обязателен");
        }
        String normalized = deviceModelService.normalizeMac(mac);
        if (!deviceModelService.isValidMacFormat(normalized)) {
            throw new IllegalArgumentException("MAC-адрес должен содержать 12 символов (0-9, A-F)");
        }
        return normalized;
    }

    private String resolveEnteredByName(UserAccount actor) {
        if (actor == null) {
            return "—";
        }
        if (actor.getName() != null && !actor.getName().isBlank()) {
            return actor.getName().trim();
        }
        return actor.getUserName() != null ? actor.getUserName() : "—";
    }

    public static String formatMacForDisplay(String normalizedMac) {
        if (normalizedMac == null || normalizedMac.length() != 12) {
            return normalizedMac;
        }
        return normalizedMac.substring(0, 4) + ' '
                + normalizedMac.substring(4, 8) + ' '
                + normalizedMac.substring(8, 12);
    }

    private MacAddressRegistryDTO toDto(MacAddressRegistry row) {
        MacAddressRegistryDTO dto = new MacAddressRegistryDTO();
        dto.setId(row.getId());
        dto.setMac(formatMacForDisplay(row.getMac()));
        dto.setEquipmentTypeId(row.getMacEquipmentTypeId());
        dto.setStatus(row.getStatus());
        dto.setStatusLabel(statusLabel(row.getStatus()));
        dto.setDateCreated(row.getDateCreated());
        if (row.getDateCreated() != null) {
            dto.setDateCreatedDisplay(row.getDateCreated().atZone(ZoneId.of("UTC"))
                    .withZoneSameInstant(MOSCOW)
                    .format(DISPLAY_FMT));
        }
        dto.setEnteredByName(row.getEnteredByName());
        dto.setSessionCount(row.getSessionCount());
        dto.setWeldingMachineId(row.getWeldingMachineId());
        if (row.getEquipmentType() != null) {
            dto.setEquipmentTypeName(row.getEquipmentType().getName());
        } else {
            equipmentTypeRepository.findById(row.getMacEquipmentTypeId())
                    .ifPresent(t -> dto.setEquipmentTypeName(t.getName()));
        }
        return dto;
    }

    private MacEquipmentTypeDTO toTypeDto(MacEquipmentType type) {
        MacEquipmentTypeDTO dto = new MacEquipmentTypeDTO();
        dto.setId(type.getId());
        dto.setName(type.getName());
        return dto;
    }

    private static String statusLabel(MacRegistryStatus status) {
        if (status == null) {
            return "—";
        }
        switch (status) {
            case ACTIVE:
                return "Активен";
            case BLOCKED:
                return "Заблокирован";
            case WAITING:
                return "Ожидание";
            default:
                return status.name();
        }
    }
}
