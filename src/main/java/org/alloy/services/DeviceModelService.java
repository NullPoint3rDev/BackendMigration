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
        
        Optional<WeldingMachine> machine = weldingMachineRepository.findByMac(normalizedMac);
        if (machine.isPresent() && machine.get().getDeviceModel() != null) {
            return machine.get().getDeviceModel();
        }

        // Если в БД нет, используем обратную совместимость
        return DeviceModel.getByMac(normalizedMac);
    }

    /**
     * Проверить соответствие модели устройства и формата пакета
     */
    public boolean isPacketFormatMatches(String mac, String packetData) {
        DeviceModel expectedModel = getDeviceModelByMac(mac);
        if (expectedModel == null) {
            return false;
        }

        switch (expectedModel) {
            case CORE:
                // Core пакеты обычно содержат определенные поля
                return packetData.contains("CURRENT") || packetData.contains("VOLTAGE") || 
                       packetData.contains("POWER") || packetData.contains("TEMPERATURE");
            case MONITORING_BLOCK:
                // Блок мониторинга имеет другой формат пакетов
                return packetData.contains("=") && packetData.contains(";");
            default:
                return false;
        }
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
