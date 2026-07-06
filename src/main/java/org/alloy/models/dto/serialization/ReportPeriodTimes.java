package org.alloy.models.dto.serialization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Период отчёта задаётся в Europe/Moscow (как в UI); в БД — naive UTC.
 */
public final class ReportPeriodTimes {

    private ReportPeriodTimes() {
    }

    public static LocalDateTime nowDisplay() {
        return LocalDateTime.now(DisplayTimeZones.DISPLAY);
    }

    public static LocalDate displayDateToday() {
        return nowDisplay().toLocalDate();
    }

    public static LocalTime displayTimeNow() {
        return nowDisplay().toLocalTime();
    }

    /** Московские дата+время → UTC naive для запросов к БД. */
    public static LocalDateTime displayLocalToStorage(LocalDate date, LocalTime time) {
        if (date == null) {
            return null;
        }
        LocalTime t = time != null ? time : LocalTime.MIN;
        return LocalDateTime.of(date, t)
                .atZone(DisplayTimeZones.DISPLAY)
                .withZoneSameInstant(DisplayTimeZones.STORAGE)
                .toLocalDateTime();
    }

    /** UTC naive из БД → Москва (для заголовка отчёта). */
    public static LocalDateTime storageToDisplay(LocalDateTime storage) {
        if (storage == null) {
            return null;
        }
        return storage.atZone(DisplayTimeZones.STORAGE)
                .withZoneSameInstant(DisplayTimeZones.DISPLAY)
                .toLocalDateTime();
    }
}
