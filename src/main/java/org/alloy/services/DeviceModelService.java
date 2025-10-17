package org.alloy.services;

import org.alloy.models.DeviceModel;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис для работы с моделями устройств
 */
@Service
public class DeviceModelService {

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    /**
     * Получить модель устройства по MAC-адресу из базы данных
     */
    public DeviceModel getDeviceModelByMac(String mac) {
        if (mac == null || mac.isEmpty()) {
            return null;
        }

        // Нормализуем MAC-адрес (убираем двоеточия, делаем заглавными)
        String normalizedMac = normalizeMac(mac);
        System.out.println("[DEVICE-MODEL-SERVICE] 🔍 Поиск модели для MAC: " + mac + " (нормализованный: " + normalizedMac + ")");
        
        Optional<WeldingMachine> machine = weldingMachineRepository.findByMac(normalizedMac);
        if (machine.isPresent() && machine.get().getDeviceModel() != null) {
            DeviceModel model = machine.get().getDeviceModel();
            System.out.println("[DEVICE-MODEL-SERVICE] ✅ Найдена модель в БД: " + model + " для MAC: " + normalizedMac);
            return model;
        }

        // Если в БД нет, используем обратную совместимость
        DeviceModel fallbackModel = DeviceModel.getByMac(normalizedMac);
        if (fallbackModel != null) {
            System.out.println("[DEVICE-MODEL-SERVICE] 🔄 Используется fallback модель: " + fallbackModel + " для MAC: " + normalizedMac);
        } else {
            System.out.println("[DEVICE-MODEL-SERVICE] ❌ Модель не найдена ни в БД, ни в fallback для MAC: " + normalizedMac);
        }
        return fallbackModel;
    }

    /**
     * Проверить соответствие модели устройства и формата пакета
     */
    public boolean isPacketFormatMatches(String mac, String packetData) {
        DeviceModel expectedModel = getDeviceModelByMac(mac);
        if (expectedModel == null) {
            System.out.println("[DEVICE-MODEL-SERVICE] ❌ Модель не найдена для MAC: " + mac);
            return false;
        }

        boolean matches = false;
        switch (expectedModel) {
            case CORE:
                // Core пакеты обычно содержат определенные поля
                matches = packetData.contains("CURRENT") || packetData.contains("VOLTAGE") || 
                         packetData.contains("POWER") || packetData.contains("TEMPERATURE");
                System.out.println("[DEVICE-MODEL-SERVICE] 🔍 Проверка CORE для MAC " + mac + ": " + matches);
                break;
            case MONITORING_BLOCK:
                // Блок мониторинга имеет формат: :MAC;данные
                boolean hasMac = packetData.contains(":" + mac);
                boolean hasSemicolon = packetData.contains(";");
                matches = hasMac && hasSemicolon;
                System.out.println("[DEVICE-MODEL-SERVICE] 🔍 Проверка MONITORING_BLOCK для MAC " + mac + ":");
                System.out.println("[DEVICE-MODEL-SERVICE]   - Содержит ':" + mac + "': " + hasMac);
                System.out.println("[DEVICE-MODEL-SERVICE]   - Содержит ';': " + hasSemicolon);
                System.out.println("[DEVICE-MODEL-SERVICE]   - Результат: " + matches);
                break;
            default:
                matches = false;
                System.out.println("[DEVICE-MODEL-SERVICE] ❌ Неизвестная модель: " + expectedModel);
        }
        
        return matches;
    }

    /**
     * Нормализовать MAC-адрес (убрать двоеточия, сделать заглавными)
     */
    public String normalizeMac(String mac) {
        if (mac == null) {
            return null;
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
        String normalized = normalizeMac(mac);
        return normalized.length() == 12 && normalized.matches("[0-9A-F]+");
    }
}
