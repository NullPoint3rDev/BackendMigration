package org.alloy.protocol.v2;

/**
 * Куда отдать hub-тело из 0x02 / 0x07.
 * null sink = только протокол, без телеметрии.
 */
@FunctionalInterface
public interface V2TelemetrySink {
    /**
     * @param historyRecord true для 0x07 (писать в БД с unix-временем, не затирать live panel)
     */
    void onTelemetry(String mac, byte[] hubBody, boolean historyRecord);
}
