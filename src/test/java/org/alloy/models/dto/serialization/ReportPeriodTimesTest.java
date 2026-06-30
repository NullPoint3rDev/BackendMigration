package org.alloy.models.dto.serialization;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportPeriodTimesTest {

    @Test
    void displayLocalToStorage_convertsMoscowToUtc() {
        LocalDateTime utc = ReportPeriodTimes.displayLocalToStorage(
                LocalDate.of(2026, 6, 30), LocalTime.of(14, 17, 4));
        assertEquals(LocalDateTime.of(2026, 6, 30, 11, 17, 4), utc);
    }

    @Test
    void storageToDisplay_convertsUtcToMoscow() {
        LocalDateTime msk = ReportPeriodTimes.storageToDisplay(
                LocalDateTime.of(2026, 6, 30, 11, 17, 4));
        assertEquals(LocalDateTime.of(2026, 6, 30, 14, 17, 4), msk);
    }
}
