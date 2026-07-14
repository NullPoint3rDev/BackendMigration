package org.alloy.services;

import org.alloy.models.dto.serialization.DisplayTimeZones;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Контракт history: Instant (front fromMs/ts) ↔ naive LocalDateTime в БД через UTC,
 * не через JVM default (иначе шовы уезжают на offset относительно saveState UTC).
 */
class TelemetryHistoryTimezoneTest {

    @Test
    void storageZone_roundTripsEpochMs() {
        // 2026-07-14 11:55:00 UTC ≈ 15:55 MSK+4
        long epochMs = Instant.parse("2026-07-14T11:55:00Z").toEpochMilli();
        ZoneId storage = DisplayTimeZones.STORAGE;
        LocalDateTime stored = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), storage);
        assertEquals(LocalDateTime.of(2026, 7, 14, 11, 55, 0), stored);
        assertEquals(epochMs, stored.atZone(storage).toInstant().toEpochMilli());
    }

    @Test
    void moscowWallAsZone_skewsUtcStoredDigits() {
        LocalDateTime utcDigits = LocalDateTime.of(2026, 7, 14, 11, 55, 0);
        long correctTs = utcDigits.atZone(DisplayTimeZones.STORAGE).toInstant().toEpochMilli();
        long wrongTs = utcDigits.atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli();
        assertNotEquals(correctTs, wrongTs);
        assertEquals(3 * 3600_000L, correctTs - wrongTs);
    }
}
