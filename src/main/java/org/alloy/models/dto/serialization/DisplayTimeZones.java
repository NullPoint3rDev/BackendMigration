package org.alloy.models.dto.serialization;

import java.time.ZoneId;
import java.time.ZoneOffset;

/** Naive LocalDateTime в БД = UTC; в API отдаём Europe/Moscow с offset. */
public final class DisplayTimeZones {

    public static final ZoneId STORAGE = ZoneOffset.UTC;
    public static final ZoneId DISPLAY = ZoneId.of("Europe/Moscow");

    private DisplayTimeZones() {
    }
}
