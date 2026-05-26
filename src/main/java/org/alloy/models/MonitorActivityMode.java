package org.alloy.models;

/** Режим активности для суточных плиток мониторинга (как на DeviceMonitorPage). */
public enum MonitorActivityMode {
    off,
    /** В БД хранится в колонке {@code standby_ms} (историческое имя). */
    error,
    on,
    welding
}
