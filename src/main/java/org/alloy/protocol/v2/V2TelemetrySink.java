package org.alloy.protocol.v2;

/**
 * Куда отдать WTINFO из 0x02 / 0x07 (например deviceManager).
 * null sink = только протокол, без телеметрии.
 */
@FunctionalInterface
public interface V2TelemetrySink {
    void onTelemetry(String mac, byte[] wtinfoBytes);
}
