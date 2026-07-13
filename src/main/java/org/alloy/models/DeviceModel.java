package org.alloy.models;

/**
 * Модели сварочных устройств
 */
public enum DeviceModel {
    MONITORING_BLOCK("Блок мониторинга"),
    CORE("Core");

    private final String displayName;

    DeviceModel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Получить модель устройства по MAC-адресу (для обратной совместимости)
     */
    public static DeviceModel getByMac(String mac) {
        if (mac == null || mac.isEmpty()) return null;
        
        // Хардкод для существующих устройств (fallback, если в БД нет DeviceModel)
        if (mac.equalsIgnoreCase("E09806083396")
                || mac.equalsIgnoreCase("DC4F22763D5C")
                || mac.equalsIgnoreCase("E098060B22D2")
                || mac.equalsIgnoreCase("C82B9620E506")
                || mac.equalsIgnoreCase("ECFABCC9703B")) {
            return CORE;
        }
        if (mac.equalsIgnoreCase("8CAAB50C4254")) {
            return MONITORING_BLOCK;
        }
        
        return null;
    }
}
