package org.alloy.services;

import org.alloy.models.DeviceModel;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для работы с моделями устройств
 */
@Service
public class DeviceModelService {

    /** Отладочный MAC для разработчиков: ввод xxxxxxxxxxxx, в БД — XXXXXXXXXXXX. */
    public static final String DEBUG_MAC = "XXXXXXXXXXXX";

    /** ponytail: in-memory TTL; ceiling — до 60s stale после смены модели/RFID в БД. */
    private static final long MAC_META_CACHE_TTL_MS = 60_000L;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    private final ConcurrentHashMap<String, CachedMachineMeta> macMetaCache = new ConcurrentHashMap<>();

    @Value("${welding.core.macs:E09806083396,DC4F22763D5C,E098060B22D2,C82B9620E506}")
    private String coreMacsConfig;

    /** true, если MAC — отладочный (12 символов x/X, разделители игнорируются). */
    public static boolean isDebugMac(String mac) {
        if (mac == null || mac.isEmpty()) {
            return false;
        }
        String cleaned = mac.replaceAll("[^0-9A-Fa-fxX]", "");
        return cleaned.length() == 12 && cleaned.equalsIgnoreCase("xxxxxxxxxxxx");
    }

    /**
     * Получить модель устройства по MAC-адресу из базы данных
     */
    public DeviceModel getDeviceModelByMac(String mac) {
        CachedMachineMeta meta = loadMachineMeta(mac);
        return meta != null ? meta.model : null;
    }

    /** Core по MAC (БД, fallback getByMac, welding.core.macs). */
    public boolean isCoreDevice(String mac) {
        DeviceModel model = getDeviceModelByMac(mac);
        if (model == DeviceModel.CORE) {
            return true;
        }
        if (model == DeviceModel.MONITORING_BLOCK) {
            return false;
        }
        return isCoreMacInConfig(mac);
    }

    /**
     * Core-парсер: явная модель/MAC или длинный hex-payload WTINFO (не архивный блок ~76 nibbles).
     */
    public boolean shouldUseCoreParser(String mac, String packetData) {
        if (isCoreMacInConfig(mac)) {
            return true;
        }
        if (isCorePacketFormat(packetData)) {
            return true;
        }
        DeviceModel model = getDeviceModelByMac(mac);
        if (model == DeviceModel.MONITORING_BLOCK) {
            return false;
        }
        return model == DeviceModel.CORE;
    }

    /** ponytail: длина hex ≥100 — WTINFO Core; архивный блок мониторинга ~76 nibbles. */
    public static boolean isCorePacketFormat(String packetData) {
        if (packetData == null) {
            return false;
        }
        int semi = packetData.indexOf(';');
        if (semi < 0 || semi + 1 >= packetData.length()) {
            return false;
        }
        String payload = packetData.substring(semi + 1).trim();
        return payload.length() >= 100 && payload.matches("[0-9A-Fa-f]+");
    }

    private boolean isCoreMacInConfig(String mac) {
        if (mac == null || mac.isEmpty()) {
            return false;
        }
        String normalized = normalizeMac(mac);
        for (String part : coreMacsConfig.split(",")) {
            if (normalized.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверить соответствие модели устройства и формата пакета
     */
    public boolean isPacketFormatMatches(String mac, String packetData) {
        DeviceModel expectedModel = getDeviceModelByMac(mac);
        if (expectedModel == null) {
            return false;
        }

        boolean matches = false;
        switch (expectedModel) {
            case CORE:
                // Core пакеты обычно содержат определенные поля
                matches = packetData.contains("CURRENT") || packetData.contains("VOLTAGE") || 
                         packetData.contains("POWER") || packetData.contains("TEMPERATURE");
                break;
            case MONITORING_BLOCK:
                // Блок мониторинга имеет формат: :MAC;данные
                boolean hasMac = packetData.contains(":" + mac);
                boolean hasSemicolon = packetData.contains(";");
                matches = hasMac && hasSemicolon;
                break;
            default:
                matches = false;
        }
        
        return matches;
    }

    /**
     * Активирован ли RFID у аппарата с этим MAC. По умолчанию (нет в БД/поле null) — true.
     */
    public boolean isRfidEnabledByMac(String mac) {
        CachedMachineMeta meta = loadMachineMeta(mac);
        return meta == null || meta.rfidEnabled;
    }

    private CachedMachineMeta loadMachineMeta(String mac) {
        if (mac == null || mac.isEmpty()) {
            return null;
        }
        String normalizedMac = normalizeMac(mac);
        long now = System.currentTimeMillis();
        CachedMachineMeta cached = macMetaCache.get(normalizedMac);
        if (cached != null && now - cached.checkedAtMs < MAC_META_CACHE_TTL_MS) {
            return cached;
        }

        Optional<WeldingMachine> machine = weldingMachineRepository.findByMac(normalizedMac);
        DeviceModel model;
        boolean rfidEnabled = true;
        if (machine.isPresent()) {
            WeldingMachine wm = machine.get();
            model = wm.getDeviceModel() != null ? wm.getDeviceModel() : DeviceModel.getByMac(normalizedMac);
            if (wm.getRfidEnabled() != null) {
                rfidEnabled = wm.getRfidEnabled();
            }
        } else {
            model = DeviceModel.getByMac(normalizedMac);
        }

        CachedMachineMeta meta = new CachedMachineMeta(model, rfidEnabled, now);
        macMetaCache.put(normalizedMac, meta);
        return meta;
    }

    /**
     * Нормализовать MAC-адрес (убрать двоеточия, сделать заглавными).
     * Отладочный xxxxxxxxxxxx сохраняется как XXXXXXXXXXXX (иначе x вырежется как не-hex).
     */
    public String normalizeMac(String mac) {
        if (mac == null) {
            return null;
        }
        if (isDebugMac(mac)) {
            return DEBUG_MAC;
        }
        return mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
    }

    /**
     * Валидировать формат MAC-адреса
     */
    public boolean isValidMacFormat(String mac) {
        if (mac == null || mac.isEmpty()) {
            return false;
        }
        if (isDebugMac(mac)) {
            return true;
        }
        String normalized = normalizeMac(mac);
        return normalized.length() == 12 && normalized.matches("[0-9A-F]+");
    }

    private static final class CachedMachineMeta {
        final DeviceModel model;
        final boolean rfidEnabled;
        final long checkedAtMs;

        CachedMachineMeta(DeviceModel model, boolean rfidEnabled, long checkedAtMs) {
            this.model = model;
            this.rfidEnabled = rfidEnabled;
            this.checkedAtMs = checkedAtMs;
        }
    }
}
