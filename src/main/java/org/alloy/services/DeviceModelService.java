package org.alloy.services;

import org.alloy.models.DeviceModel;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис для работы с моделями устройств
 */
@Service
public class DeviceModelService {

    /** Отладочный MAC для разработчиков: ввод xxxxxxxxxxxx, в БД — XXXXXXXXXXXX. */
    public static final String DEBUG_MAC = "XXXXXXXXXXXX";

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

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
        if (mac == null || mac.isEmpty()) {
            return null;
        }

        // Нормализуем MAC-адрес (убираем двоеточия, делаем заглавными)
        String normalizedMac = normalizeMac(mac);
        Optional<WeldingMachine> machine = weldingMachineRepository.findByMac(normalizedMac);
        if (machine.isPresent() && machine.get().getDeviceModel() != null) {
            DeviceModel model = machine.get().getDeviceModel();
            return model;
        }

        // Если в БД нет, используем обратную совместимость
        DeviceModel fallbackModel = DeviceModel.getByMac(normalizedMac);
        return fallbackModel;
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
        DeviceModel model = getDeviceModelByMac(mac);
        if (model == DeviceModel.MONITORING_BLOCK) {
            return false;
        }
        if (model == DeviceModel.CORE || isCoreMacInConfig(mac)) {
            return true;
        }
        return isCorePacketFormat(packetData);
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
     * ponytail: запрос в БД на каждый пакет (как и getDeviceModelByMac); при росте нагрузки — кэш по MAC.
     */
    public boolean isRfidEnabledByMac(String mac) {
        if (mac == null || mac.isEmpty()) {
            return true;
        }
        Optional<WeldingMachine> machine = weldingMachineRepository.findByMac(normalizeMac(mac));
        if (machine.isPresent() && machine.get().getRfidEnabled() != null) {
            return machine.get().getRfidEnabled();
        }
        return true;
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
}
