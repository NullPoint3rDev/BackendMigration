package org.alloy.services;

import org.alloy.models.entities.WeldingMachineState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Длительности сегментов состояний (как в ReportDataService). */
public final class WeldingStateDurationUtil {

    private WeldingStateDurationUtil() {
    }

    public static long effectiveStateDurationMs(
            WeldingMachineState s,
            List<WeldingMachineState> sortedSameMachineByDate,
            LocalDateTime openEndIfLast) {
        long d = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
        if (d > 0) {
            return d;
        }
        if (sortedSameMachineByDate == null || sortedSameMachineByDate.isEmpty() || s.getDateCreated() == null) {
            return 0L;
        }
        int i = indexOfState(s, sortedSameMachineByDate);
        if (i < 0) {
            return 0L;
        }
        if (i + 1 < sortedSameMachineByDate.size()) {
            LocalDateTime nextT = sortedSameMachineByDate.get(i + 1).getDateCreated();
            if (nextT == null) {
                return 0L;
            }
            long gap = Duration.between(s.getDateCreated(), nextT).toMillis();
            return gap > 0 ? gap : 0L;
        }
        if (openEndIfLast != null && openEndIfLast.isAfter(s.getDateCreated())) {
            long gap = Duration.between(s.getDateCreated(), openEndIfLast).toMillis();
            return gap > 0 ? gap : 0L;
        }
        return 0L;
    }

    public static long overlapDurationMs(
            WeldingMachineState s,
            LocalDateTime segmentStart,
            LocalDateTime segmentEnd,
            List<WeldingMachineState> sortedSameMachineByDate,
            LocalDateTime openEndIfLast) {
        LocalDateTime stateStart = s.getDateCreated();
        if (stateStart == null) {
            return 0L;
        }
        long durationMs = effectiveStateDurationMs(s, sortedSameMachineByDate, openEndIfLast);
        if (durationMs <= 0) {
            return 0L;
        }
        LocalDateTime stateEnd = stateStart.plus(durationMs, java.time.temporal.ChronoUnit.MILLIS);
        if (!(stateStart.isBefore(segmentEnd) && stateEnd.isAfter(segmentStart))) {
            return 0L;
        }
        LocalDateTime overlapStart = stateStart.isBefore(segmentStart) ? segmentStart : stateStart;
        LocalDateTime overlapEnd = stateEnd.isAfter(segmentEnd) ? segmentEnd : stateEnd;
        if (!overlapStart.isBefore(overlapEnd)) {
            return 0L;
        }
        return Duration.between(overlapStart, overlapEnd).toMillis();
    }

    private static int indexOfState(WeldingMachineState s, List<WeldingMachineState> sorted) {
        if (s.getId() == null) {
            return -1;
        }
        for (int k = 0; k < sorted.size(); k++) {
            if (s.getId().equals(sorted.get(k).getId())) {
                return k;
            }
        }
        return -1;
    }
}
