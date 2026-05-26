package org.alloy.services;

import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.alloy.repositories.WeldingMachineParameterValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.alloy.models.WeldingMachineStatus;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WeldingReportCalculationService {

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private WeldingMachineParameterValueRepository parameterValueRepository;

    /**
     * Минимальный ток (А), выше которого учитываем I и U при расчёте средних по шву
     * (требуется состояние «Сварка» и ток строго больше этого порога).
     */
    private static final int MIN_ARC_CURRENT_AMPS_FOR_REPORT = 10;
    private static final double SEGMENT_VOLTAGE_OUTLIER_RATIO = 0.35d;

    /**
     * Рассчитывает средние значения тока и напряжения для указанного аппарата за период времени
     * Учитывает только периоды активной сварки (когда ток > 0)
     */
    public AverageValues calculateAverageValues(String mac, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Находим аппарат по MAC
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
            if (!machineOpt.isPresent()) {
                return new AverageValues(BigDecimal.ZERO, BigDecimal.ZERO);
            }

            WeldingMachine machine = machineOpt.get();

            // Получаем состояния за указанный период
            List<WeldingMachineState> states = weldingMachineStateRepository
                    .findByWeldingMachineIdAndDateRange(machine.getId(), startDate, endDate);

            if (states.isEmpty()) {
                return new AverageValues(BigDecimal.ZERO, BigDecimal.ZERO);
            }


            // Собираем ID состояний
            List<Long> stateIds = states.stream()
                    .map(WeldingMachineState::getId)
                    .collect(java.util.stream.Collectors.toList());

            // Получаем значения тока (State.I) только для активной сварки
            List<WeldingMachineParameterValue> currentValues = parameterValueRepository
                    .findByStateIdsAndPropertyCode(stateIds, "State.I");

            // Получаем значения напряжения (State.U) только для активной сварки
            List<WeldingMachineParameterValue> voltageValues = parameterValueRepository
                    .findByStateIdsAndPropertyCode(stateIds, "State.U");


            // Фильтруем только активную сварку (ток > 0) и рассчитываем средние значения
            BigDecimal averageCurrent = calculateAverageForActiveWelding(currentValues, "State.I");
            BigDecimal averageVoltage = calculateAverageForActiveWelding(voltageValues, "State.U");

            // Рассчитываем время сварки
            long activeWeldingCount = currentValues.stream()
                    .map(WeldingMachineParameterValue::getValue)
                    .filter(value -> value != null && !value.trim().isEmpty())
                    .mapToLong(value -> {
                        try {
                            int hexValue = Integer.parseInt(value, 16);
                            return hexValue > 0 ? 1 : 0; // Считаем только активную сварку
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .sum();

            BigDecimal weldingTimeSeconds = BigDecimal.valueOf(activeWeldingCount);


            return new AverageValues(averageCurrent, averageVoltage, weldingTimeSeconds);

        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ❌ Ошибка расчета средних значений: " + e.getMessage());
            e.printStackTrace();
            return new AverageValues(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /**
     * Рассчитывает среднее значение для активной сварки (когда ток > 0)
     */
    private BigDecimal calculateAverageForActiveWelding(List<WeldingMachineParameterValue> values, String propertyCode) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Фильтруем только значения > 0 (активная сварка)
        List<BigDecimal> activeValues = values.stream()
                .map(WeldingMachineParameterValue::getValue)
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(value -> {
                    try {
                        // Сначала пробуем как hex, если не получается - как decimal
                        int decimalValue;
                        try {
                            decimalValue = Integer.parseInt(value, 16);
                        } catch (NumberFormatException hexException) {
                            // Если не hex, пробуем как decimal
                            decimalValue = Integer.parseInt(value, 10);
                        }
                        return BigDecimal.valueOf(decimalValue);
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                })
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0) // Только активная сварка
                .collect(java.util.stream.Collectors.toList());

        if (activeValues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Рассчитываем среднее значение
        BigDecimal sum = activeValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = sum.divide(BigDecimal.valueOf(activeValues.size()), 1, RoundingMode.HALF_UP);


        return average;
    }

    /**
     * Класс для хранения средних значений
     */
    public static class AverageValues {
        private final BigDecimal averageCurrent;
        private final BigDecimal averageVoltage;
        private final BigDecimal weldingTimeSeconds;

        public AverageValues(BigDecimal averageCurrent, BigDecimal averageVoltage) {
            this.averageCurrent = averageCurrent;
            this.averageVoltage = averageVoltage;
            this.weldingTimeSeconds = BigDecimal.ZERO;
        }

        public AverageValues(BigDecimal averageCurrent, BigDecimal averageVoltage, BigDecimal weldingTimeSeconds) {
            this.averageCurrent = averageCurrent;
            this.averageVoltage = averageVoltage;
            this.weldingTimeSeconds = weldingTimeSeconds;
        }

        public BigDecimal getAverageCurrent() {
            return averageCurrent;
        }

        public BigDecimal getAverageVoltage() {
            return averageVoltage;
        }

        public BigDecimal getWeldingTimeSeconds() {
            return weldingTimeSeconds;
        }
    }

    /**
     * Рассчитывает средние значения тока и напряжения по дням для указанного аппарата за период времени
     * Возвращает список данных по дням для отчета по работе оборудования
     */
    public List<DailyAverageValues> calculateDailyAverageValues(String mac, LocalDateTime startDate, LocalDateTime endDate) {
        List<DailyAverageValues> dailyData = new ArrayList<>();


        try {
            // Находим аппарат по MAC
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
            if (!machineOpt.isPresent()) {
                return dailyData;
            }

            WeldingMachine machine = machineOpt.get();

            // Генерируем список дней в периоде
            LocalDateTime currentDate = startDate.toLocalDate().atStartOfDay();
            LocalDateTime endOfPeriod = endDate.toLocalDate().atTime(23, 59, 59);

            while (!currentDate.isAfter(endOfPeriod)) {
                LocalDateTime dayStart = currentDate;
                LocalDateTime dayEnd = currentDate.toLocalDate().atTime(23, 59, 59);

                // Рассчитываем средние значения для этого дня
                AverageValues dayAverages = calculateAverageValuesForPeriod(machine.getId(), dayStart, dayEnd);

                // Создаем запись для дня
                DailyAverageValues dayData = new DailyAverageValues();
                dayData.setDate(currentDate.toLocalDate());
                dayData.setStartTime(dayStart);
                dayData.setEndTime(dayEnd);
                dayData.setAverageCurrent(dayAverages.getAverageCurrent());
                dayData.setAverageVoltage(dayAverages.getAverageVoltage());
                dayData.setWeldingTimeSeconds(dayAverages.getWeldingTimeSeconds());
                dayData.setWeldingMachineId(machine.getId());
                dayData.setWeldingMachineName(machine.getName());
                dayData.setOrganizationUnitName(machine.getOrganizationUnit() != null ?
                        machine.getOrganizationUnit().getName() : "Не указано");

                dailyData.add(dayData);


                // Переходим к следующему дню
                currentDate = currentDate.plusDays(1);
            }


        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ❌ Ошибка расчета данных по дням: " + e.getMessage());
            e.printStackTrace();
        }

        return dailyData;
    }

    /**
     * Рассчитывает средние значения для конкретного периода (вспомогательный метод)
     */
    private AverageValues calculateAverageValuesForPeriod(Integer machineId, LocalDateTime startDate, LocalDateTime endDate) {

        try {
            // Получаем состояния за указанный период
            List<WeldingMachineState> states = weldingMachineStateRepository
                    .findByWeldingMachineIdAndDateRange(machineId, startDate, endDate);


            if (states.isEmpty()) {
                return new AverageValues(BigDecimal.ZERO, BigDecimal.ZERO);
            }

            // Собираем ID состояний
            List<Long> stateIds = states.stream()
                    .map(WeldingMachineState::getId)
                    .collect(java.util.stream.Collectors.toList());

            // Получаем значения тока и напряжения
            List<WeldingMachineParameterValue> currentValues = parameterValueRepository
                    .findByStateIdsAndPropertyCode(stateIds, "State.I");
            List<WeldingMachineParameterValue> voltageValues = parameterValueRepository
                    .findByStateIdsAndPropertyCode(stateIds, "State.U");


            // Логируем первые несколько значений для отладки

            // Рассчитываем средние значения
            BigDecimal averageCurrent = calculateAverageForActiveWelding(currentValues, "State.I");
            BigDecimal averageVoltage = calculateAverageForActiveWelding(voltageValues, "State.U");

            // Рассчитываем время сварки (количество активных записей * интервал между записями)
            long activeWeldingCount = currentValues.stream()
                    .map(WeldingMachineParameterValue::getValue)
                    .filter(value -> value != null && !value.trim().isEmpty())
                    .mapToLong(value -> {
                        try {
                            int hexValue = Integer.parseInt(value, 16);
                            return hexValue > 0 ? 1 : 0; // Считаем только активную сварку
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .sum();

            // Предполагаем интервал между записями 1 секунда (данные приходят каждую секунду)
            BigDecimal weldingTimeSeconds = BigDecimal.valueOf(activeWeldingCount);


            return new AverageValues(averageCurrent, averageVoltage, weldingTimeSeconds);

        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ❌ Ошибка расчета для периода: " + e.getMessage());
            return new AverageValues(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /**
     * Класс для хранения средних значений по дням
     */
    public static class DailyAverageValues {
        private java.time.LocalDate date;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal averageCurrent;
        private BigDecimal averageVoltage;
        private BigDecimal weldingTimeSeconds; // время сварки в секундах
        private Integer weldingMachineId;
        private String weldingMachineName;
        private String organizationUnitName;

        // Getters and setters
        public java.time.LocalDate getDate() { return date; }
        public void setDate(java.time.LocalDate date) { this.date = date; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public BigDecimal getAverageCurrent() { return averageCurrent; }
        public void setAverageCurrent(BigDecimal averageCurrent) { this.averageCurrent = averageCurrent; }

        public BigDecimal getAverageVoltage() { return averageVoltage; }
        public void setAverageVoltage(BigDecimal averageVoltage) { this.averageVoltage = averageVoltage; }

        public BigDecimal getWeldingTimeSeconds() { return weldingTimeSeconds; }
        public void setWeldingTimeSeconds(BigDecimal weldingTimeSeconds) { this.weldingTimeSeconds = weldingTimeSeconds; }

        public Integer getWeldingMachineId() { return weldingMachineId; }
        public void setWeldingMachineId(Integer weldingMachineId) { this.weldingMachineId = weldingMachineId; }

        public String getWeldingMachineName() { return weldingMachineName; }
        public void setWeldingMachineName(String weldingMachineName) { this.weldingMachineName = weldingMachineName; }

        public String getOrganizationUnitName() { return organizationUnitName; }
        public void setOrganizationUnitName(String organizationUnitName) { this.organizationUnitName = organizationUnitName; }
    }

    private static boolean isWeldingState(WeldingMachineState s, java.util.Map<Long, String> stateNameByStateId) {
        if (s == null) return false;
        return (s.getWeldingMachineStatus() == WeldingMachineStatus.Welding)
                || "Сварка".equalsIgnoreCase(stateNameByStateId.getOrDefault(s.getId(), "").trim());
    }

    private static boolean isValidCurrentVoltagePoint(int currentAmps, int voltageVolts) {
        return currentAmps > 0 && voltageVolts >= 0 && voltageVolts <= currentAmps;
    }

    private static double calculateMedian(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0d;
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compareTo);
        int n = sorted.size();
        if ((n & 1) == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0d;
    }

    private static boolean isWithinSegmentMedianBand(int voltageVolts, double medianVolts) {
        if (medianVolts <= 0d) return true;
        return Math.abs(voltageVolts - medianVolts) <= (medianVolts * SEGMENT_VOLTAGE_OUTLIER_RATIO);
    }

    /**
     * Рассчитывает сегменты швов по статусу аппарата:
     * — Начало шва: первое состояние со статусом «Сварка» (Welding).
     * — Конец шва: первое следующее состояние, которое не «Сварка».
     * — Время шва: сумма длительностей всех последовательных состояний «Сварка» в этом интервале (секунды).
     */
    public List<org.alloy.models.dto.WeldSegmentDTO> calculateWeldSegments(Integer machineId, LocalDateTime startDate, LocalDateTime endDate) {
        List<WeldingMachineState> states = weldingMachineStateRepository
                .findByWeldingMachineIdAndDateRange(machineId, startDate, endDate);
        if (states.isEmpty()) {
            return new ArrayList<>();
        }
        states.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
        return calculateWeldSegmentsFromStates(states);
    }

    /**
     * Расчёт сегментов швов по уже загруженным состояниям аппарата (без повторного запроса в БД).
     */
    public List<org.alloy.models.dto.WeldSegmentDTO> calculateWeldSegmentsFromStates(List<WeldingMachineState> states) {
        List<org.alloy.models.dto.WeldSegmentDTO> result = new ArrayList<>();
        if (states == null || states.isEmpty()) {
            return result;
        }
        try {
            List<WeldingMachineState> sorted = new ArrayList<>(states);
            sorted.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
            List<Long> fullStateIds = sorted.stream().map(WeldingMachineState::getId).collect(java.util.stream.Collectors.toList());
            Integer machineId = sorted.stream().map(WeldingMachineState::getWeldingMachineId)
                    .filter(java.util.Objects::nonNull).findFirst().orElse(null);
            LocalDateTime rangeStart = null;
            LocalDateTime rangeEnd = null;
            for (WeldingMachineState s : sorted) {
                if (s.getDateCreated() == null) continue;
                if (rangeStart == null || s.getDateCreated().isBefore(rangeStart)) rangeStart = s.getDateCreated();
                if (rangeEnd == null || s.getDateCreated().isAfter(rangeEnd)) rangeEnd = s.getDateCreated();
            }
            boolean useMachineDateRange = machineId != null && rangeStart != null && rangeEnd != null;
            boolean largePeriod = sorted.size() >= LARGE_PERIOD_STATE_COUNT;
            long phase1Ms = System.currentTimeMillis();

            java.util.Map<Long, String> stateNameByStateId = useMachineDateRange
                    ? loadStateNameByMachineDateRange(machineId, rangeStart, rangeEnd)
                    : loadStateNameByStateIds(fullStateIds);
            java.util.Map<Long, LocalDateTime> deviceTimeByStateId = new java.util.HashMap<>();
            if (useMachineDateRange && !largePeriod) {
                deviceTimeByStateId = loadDeviceDateTimeByMachinePeriodCombined(machineId, rangeStart, rangeEnd);
            } else if (!useMachineDateRange) {
                deviceTimeByStateId = loadDeviceDateTimeByStateIds(collectWeldingRelatedStateIds(sorted));
            }

            java.util.List<PendingWeldSegment> pending = new java.util.ArrayList<>();
            boolean inWeld = false;
            LocalDateTime weldStartTime = null;
            Long weldStartStateId = null;

            for (int i = 0; i < sorted.size(); i++) {
                WeldingMachineState s = sorted.get(i);
                boolean isWelding = isWeldingState(s, stateNameByStateId);

                if (isWelding) {
                    if (!inWeld) {
                        inWeld = true;
                        weldStartTime = s.getDateCreated();
                        weldStartStateId = s.getId();
                    }
                } else {
                    if (inWeld && weldStartTime != null && weldStartStateId != null && s.getDateCreated() != null) {
                        PendingWeldSegment p = buildPendingWeldSegment(
                                sorted, i, weldStartTime, weldStartStateId, s, stateNameByStateId, deviceTimeByStateId);
                        if (p != null) pending.add(p);
                    }
                    inWeld = false;
                }
            }
            if (inWeld && weldStartTime != null) {
                PendingWeldSegment p = buildPendingOpenWeldSegment(
                        sorted, weldStartTime, weldStartStateId, stateNameByStateId, deviceTimeByStateId);
                if (p != null) pending.add(p);
            }

            if (largePeriod && useMachineDateRange && !pending.isEmpty()) {
                java.util.Set<Long> deviceStateIds = collectDeviceTimeStateIdsForPending(sorted, pending);
                deviceTimeByStateId = loadDeviceDateTimeByStateIds(new java.util.ArrayList<>(deviceStateIds));
                refinePendingSegmentsWithDeviceTime(sorted, pending, stateNameByStateId, deviceTimeByStateId);
            }

            java.util.Set<Long> iuStateIds = collectStateIdsAroundPendingSegments(sorted, pending);
            long phase2Ms = System.currentTimeMillis();
            java.util.Map<Long, Integer> currentByState = new java.util.HashMap<>();
            java.util.Map<Long, Integer> voltageByState = new java.util.HashMap<>();
            loadCurrentAndVoltageForWeldSegments(
                    machineId, rangeStart, rangeEnd, fullStateIds, iuStateIds, sorted.size(),
                    useMachineDateRange, currentByState, voltageByState);
            long phase3Ms = System.currentTimeMillis();

            for (PendingWeldSegment p : pending) {
                List<WeldingMachineState> windowStates = statesInTimeWindow(
                        sorted, p.weldStartTime.minusMinutes(5), p.segmentEndTime.plusMinutes(5));
                LocalDateTime serverEnd = p.serverEnd != null ? p.serverEnd : p.segmentEndTime;
                result.add(buildSegmentDto(
                        p.weldStartTime, serverEnd, p.segmentDurationMs, 0, 0,
                        windowStates, currentByState, voltageByState, p.segmentStartForDto, stateNameByStateId));
            }

            System.out.println("[REPORT-CALC] machineId=" + machineId + " states=" + sorted.size()
                    + " segments=" + result.size() + " iuIds=" + iuStateIds.size()
                    + " largePeriod=" + largePeriod + " deviceTimeKeys=" + deviceTimeByStateId.size()
                    + " phase1Ms=" + (phase2Ms - phase1Ms) + " iuLoadMs=" + (phase3Ms - phase2Ms)
                    + " buildMs=" + (System.currentTimeMillis() - phase3Ms)
                    + " I=" + currentByState.size() + " U=" + voltageByState.size());

        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ❌ Ошибка расчета сегментов швов: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Строит DTO сегмента. Окно [weldStartTime, segmentEndTime] — серверное (для поиска состояний).
     * startTime в DTO — серверное (weldStartTime), чтобы в ReportDataService по нему находились состояния для скорости проволоки и даты.
     */
    private org.alloy.models.dto.WeldSegmentDTO buildSegmentDto(
            LocalDateTime weldStartTime, LocalDateTime segmentEndTime, long segmentDurationMs,
            long segmentWeightedCurrent, long segmentWeightedVoltage,
            List<WeldingMachineState> states,
            java.util.Map<Long, Integer> currentByState, java.util.Map<Long, Integer> voltageByState,
            LocalDateTime displayStartTime,
            java.util.Map<Long, String> stateNameByStateId) {
        org.alloy.models.dto.WeldSegmentDTO seg = new org.alloy.models.dto.WeldSegmentDTO();
        seg.setStartTime(weldStartTime);
        BigDecimal durationSec = BigDecimal.valueOf(segmentDurationMs / 1000.0).setScale(1, RoundingMode.HALF_UP);
        seg.setDurationSeconds(durationSec);

        long segmentWindowMs = segmentEndTime != null ? java.time.Duration.between(weldStartTime, segmentEndTime).toMillis() : segmentDurationMs;
        if (segmentWindowMs <= 0) segmentWindowMs = segmentDurationMs;
        List<Integer> candidateVoltages = new ArrayList<>();
        class Point {
            long overlapDt;
            int i;
            int u;
            Point(long overlapDt, int i, int u) {
                this.overlapDt = overlapDt;
                this.i = i;
                this.u = u;
            }
        }
        List<Point> points = new ArrayList<>();

        for (int idx = 0; idx < states.size(); idx++) {
            WeldingMachineState s = states.get(idx);
            if (s.getDateCreated() == null) continue;
            long dtMs = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
            if (dtMs <= 0) {
                if (idx + 1 < states.size() && states.get(idx + 1).getDateCreated() != null) {
                    long diff = java.time.Duration.between(s.getDateCreated(), states.get(idx + 1).getDateCreated()).toMillis();
                    if (diff > 0) dtMs = diff;
                } else if (segmentEndTime != null) {
                    long diffToSegmentEnd = java.time.Duration.between(s.getDateCreated(), segmentEndTime).toMillis();
                    if (diffToSegmentEnd > 0) dtMs = diffToSegmentEnd;
                }
            }
            if (dtMs <= 0) continue;
            java.time.LocalDateTime stateEnd = s.getDateCreated().plus(dtMs, java.time.temporal.ChronoUnit.MILLIS);
            if (!stateEnd.isAfter(weldStartTime) || (segmentEndTime != null && !s.getDateCreated().isBefore(segmentEndTime))) continue;
            long overlapStartMs = java.time.Duration.between(weldStartTime, s.getDateCreated()).toMillis();
            if (overlapStartMs < 0) overlapStartMs = 0;
            long stateEndFromStart = java.time.Duration.between(weldStartTime, stateEnd).toMillis();
            long overlapEndMs = stateEndFromStart > segmentWindowMs ? segmentWindowMs : stateEndFromStart;
            if (overlapEndMs < 0) continue;
            long overlapDt = overlapEndMs - overlapStartMs;
            if (overlapDt <= 0) continue;
            int I = currentByState.getOrDefault(s.getId(), 0);
            int U = voltageByState.getOrDefault(s.getId(), 0);
            if (!isWeldingState(s, stateNameByStateId) || I <= MIN_ARC_CURRENT_AMPS_FOR_REPORT || !isValidCurrentVoltagePoint(I, U)) continue;
            points.add(new Point(overlapDt, I, U));
            candidateVoltages.add(U);
        }

        double medianVoltage = calculateMedian(candidateVoltages);
        long weightedI = 0;
        long weightedU = 0;
        long totalDt = 0;
        for (Point p : points) {
            if (!isWithinSegmentMedianBand(p.u, medianVoltage)) continue;
            weightedI += (long) p.i * p.overlapDt;
            weightedU += (long) p.u * p.overlapDt;
            totalDt += p.overlapDt;
        }
        if (totalDt > 0) {
            seg.setAverageCurrent(BigDecimal.valueOf((double) weightedI / totalDt).setScale(1, RoundingMode.HALF_UP));
            seg.setAverageVoltage(BigDecimal.valueOf((double) weightedU / totalDt).setScale(1, RoundingMode.HALF_UP));
        } else {
            // Fallback: ближайшее по времени состояние с I/U
            WeldingMachineState nearest = null;
            long nearestDistMs = Long.MAX_VALUE;
            for (WeldingMachineState s : states) {
                if (s.getDateCreated() == null) continue;
                int I = currentByState.getOrDefault(s.getId(), 0);
                int U = voltageByState.getOrDefault(s.getId(), 0);
                if (!isWeldingState(s, stateNameByStateId) || I <= MIN_ARC_CURRENT_AMPS_FOR_REPORT || !isValidCurrentVoltagePoint(I, U)) continue;
                long dist = Math.min(
                        Math.abs(java.time.Duration.between(weldStartTime, s.getDateCreated()).toMillis()),
                        Math.abs(java.time.Duration.between(segmentEndTime, s.getDateCreated()).toMillis()));
                if (dist < nearestDistMs) {
                    nearestDistMs = dist;
                    nearest = s;
                }
            }
            if (nearest != null) {
                int I = currentByState.getOrDefault(nearest.getId(), 0);
                int U = voltageByState.getOrDefault(nearest.getId(), 0);
                seg.setAverageCurrent(BigDecimal.valueOf(I).setScale(1, RoundingMode.HALF_UP));
                seg.setAverageVoltage(BigDecimal.valueOf(U).setScale(1, RoundingMode.HALF_UP));
            } else {
                seg.setAverageCurrent(BigDecimal.ZERO);
                seg.setAverageVoltage(BigDecimal.ZERO);
            }
        }
        return seg;
    }

    /** Порог: за длинный период не грузим Date./Time.* на все состояния — только границы швов. */
    private static final int LARGE_PERIOD_STATE_COUNT = 80_000;

    private static class PendingWeldSegment {
        LocalDateTime weldStartTime;
        Long weldStartStateId;
        Long weldEndStateId;
        int endIndex = -1;
        LocalDateTime segmentEndTime;
        LocalDateTime serverEnd;
        LocalDateTime segmentStartForDto;
        long segmentDurationMs;
    }

    private PendingWeldSegment buildPendingWeldSegment(
            List<WeldingMachineState> sorted, int endIndex, LocalDateTime weldStartTime, Long weldStartStateId,
            WeldingMachineState endState, java.util.Map<Long, String> stateNameByStateId,
            java.util.Map<Long, LocalDateTime> deviceTimeByStateId) {
        LocalDateTime segmentStartForDto = weldStartTime;
        LocalDateTime segmentEndTime = endState.getDateCreated();
        long segmentDurationMs = java.time.Duration.between(weldStartTime, segmentEndTime).toMillis();
        LocalDateTime deviceStart = deviceTimeByStateId.get(weldStartStateId);
        LocalDateTime deviceEnd = deviceTimeByStateId.get(endState.getId());
        if (deviceStart != null && deviceEnd != null && !deviceEnd.isBefore(deviceStart)) {
            segmentDurationMs = java.time.Duration.between(deviceStart, deviceEnd).toMillis();
            segmentStartForDto = deviceStart;
            segmentEndTime = deviceEnd;
        }
        if (endIndex + 1 < sorted.size()) {
            WeldingMachineState next = sorted.get(endIndex + 1);
            if (isWeldingState(next, stateNameByStateId)) {
                LocalDateTime nextDevice = deviceTimeByStateId.get(next.getId());
                LocalDateTime startForDur = deviceStart != null ? deviceStart : weldStartTime;
                if (nextDevice != null && startForDur != null && nextDevice.isAfter(startForDur)) {
                    long durToNext = java.time.Duration.between(startForDur, nextDevice).toMillis();
                    if (durToNext > 0 && durToNext < segmentDurationMs) {
                        segmentDurationMs = durToNext;
                        segmentEndTime = nextDevice;
                    }
                }
            }
        }
        if (segmentDurationMs <= 0) return null;
        PendingWeldSegment p = new PendingWeldSegment();
        p.weldStartTime = weldStartTime;
        p.weldStartStateId = weldStartStateId;
        p.weldEndStateId = endState.getId();
        p.endIndex = endIndex;
        p.segmentEndTime = segmentEndTime;
        p.serverEnd = endState.getDateCreated();
        p.segmentStartForDto = segmentStartForDto;
        p.segmentDurationMs = segmentDurationMs;
        return p;
    }

    private PendingWeldSegment buildPendingOpenWeldSegment(
            List<WeldingMachineState> sorted, LocalDateTime weldStartTime, Long weldStartStateId,
            java.util.Map<Long, String> stateNameByStateId,
            java.util.Map<Long, LocalDateTime> deviceTimeByStateId) {
        long segmentDurationMs = 0;
        for (int j = 0; j < sorted.size(); j++) {
            WeldingMachineState sx = sorted.get(j);
            if (isWeldingState(sx, stateNameByStateId) && sx.getDateCreated() != null
                    && !sx.getDateCreated().isBefore(weldStartTime)) {
                long d = sx.getStateDurationMs() != null ? sx.getStateDurationMs() : 0L;
                if (d <= 0 && j + 1 < sorted.size()) {
                    d = java.time.Duration.between(sx.getDateCreated(), sorted.get(j + 1).getDateCreated()).toMillis();
                }
                segmentDurationMs += d;
            } else if (!isWeldingState(sx, stateNameByStateId)) {
                break;
            }
        }
        if (segmentDurationMs <= 0) return null;
        PendingWeldSegment p = new PendingWeldSegment();
        p.weldStartTime = weldStartTime;
        p.weldStartStateId = weldStartStateId;
        p.segmentDurationMs = segmentDurationMs;
        p.segmentEndTime = weldStartTime.plus(segmentDurationMs, java.time.temporal.ChronoUnit.MILLIS);
        p.segmentStartForDto = deviceTimeByStateId.getOrDefault(weldStartStateId, weldStartTime);
        p.serverEnd = p.segmentEndTime;
        return p;
    }

    private static java.util.Set<Long> collectStateIdsAroundPendingSegments(
            List<WeldingMachineState> sorted, java.util.List<PendingWeldSegment> pending) {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (PendingWeldSegment p : pending) {
            for (WeldingMachineState s : statesInTimeWindow(
                    sorted, p.weldStartTime.minusMinutes(5), p.segmentEndTime.plusMinutes(5))) {
                if (s.getId() != null) ids.add(s.getId());
            }
        }
        return ids;
    }

    /** stateId для Date./Time.* только на границах швов (не весь период 300k+). */
    private static java.util.Set<Long> collectDeviceTimeStateIdsForPending(
            List<WeldingMachineState> sorted, java.util.List<PendingWeldSegment> pending) {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (PendingWeldSegment p : pending) {
            if (p.weldStartStateId != null) ids.add(p.weldStartStateId);
            if (p.weldEndStateId != null) ids.add(p.weldEndStateId);
            if (p.endIndex >= 0 && p.endIndex + 1 < sorted.size()) {
                WeldingMachineState next = sorted.get(p.endIndex + 1);
                if (next.getId() != null) ids.add(next.getId());
            }
        }
        return ids;
    }

    private void refinePendingSegmentsWithDeviceTime(
            List<WeldingMachineState> sorted,
            java.util.List<PendingWeldSegment> pending,
            java.util.Map<Long, String> stateNameByStateId,
            java.util.Map<Long, LocalDateTime> deviceTimeByStateId) {
        if (deviceTimeByStateId == null || deviceTimeByStateId.isEmpty()) return;
        for (PendingWeldSegment p : pending) {
            if (p.endIndex >= 0 && p.weldEndStateId != null && p.endIndex < sorted.size()) {
                WeldingMachineState endState = sorted.get(p.endIndex);
                PendingWeldSegment refined = buildPendingWeldSegment(
                        sorted, p.endIndex, p.weldStartTime, p.weldStartStateId, endState,
                        stateNameByStateId, deviceTimeByStateId);
                if (refined != null) copyPendingTiming(p, refined);
            } else if (p.weldStartStateId != null) {
                PendingWeldSegment refined = buildPendingOpenWeldSegment(
                        sorted, p.weldStartTime, p.weldStartStateId, stateNameByStateId, deviceTimeByStateId);
                if (refined != null) copyPendingTiming(p, refined);
            }
        }
    }

    private static void copyPendingTiming(PendingWeldSegment target, PendingWeldSegment source) {
        target.segmentEndTime = source.segmentEndTime;
        target.serverEnd = source.serverEnd;
        target.segmentStartForDto = source.segmentStartForDto;
        target.segmentDurationMs = source.segmentDurationMs;
    }

    /** Ток/напряжение: по окнам швов (батчи), либо один JOIN на весь период, если окон почти весь интервал. */
    private void loadCurrentAndVoltageForWeldSegments(
            Integer machineId, LocalDateTime rangeStart, LocalDateTime rangeEnd, List<Long> fullStateIds,
            java.util.Set<Long> iuStateIds, int totalStates, boolean useMachineDateRange,
            java.util.Map<Long, Integer> currentByState, java.util.Map<Long, Integer> voltageByState) {
        int iuCount = iuStateIds != null ? iuStateIds.size() : 0;
        boolean useFullPeriod = !useMachineDateRange || iuCount == 0
                || iuCount >= totalStates * 0.65;
        if (useFullPeriod && useMachineDateRange) {
            loadArcElectricalByMachinePeriodCombined(machineId, rangeStart, rangeEnd, currentByState, voltageByState);
            return;
        }
        java.util.List<Long> idList = new java.util.ArrayList<>(iuStateIds);
        for (String code : new String[] { "Current", "State.I" }) {
            mergeParamMap(currentByState, loadParamMapByStateIds(idList, code, true));
        }
        for (String code : new String[] { "I", "Ток" }) {
            mergeParamMap(currentByState, loadParamMapByStateIds(idList, code, false));
        }
        java.util.Map<Long, Integer> voltageRaw = loadParamMapByStateIds(idList, "Voltage", true);
        for (java.util.Map.Entry<Long, Integer> e : voltageRaw.entrySet()) {
            if (e.getValue() != null && e.getValue() != 0) {
                voltageByState.put(e.getKey(), e.getValue() / 10);
            }
        }
    }

    private void loadArcElectricalByMachinePeriodCombined(
            Integer machineId, LocalDateTime start, LocalDateTime end,
            java.util.Map<Long, Integer> currentByState, java.util.Map<Long, Integer> voltageByState) {
        if (machineId == null || start == null || end == null) return;
        try {
            List<Object[]> rows = parameterValueRepository.findStateIdCodeAndValueByMachineDateRange(
                    machineId, start, end,
                    java.util.Arrays.asList("Current", "State.I", "I", "Ток", "Voltage"));
            for (Object[] row : rows) {
                if (row == null || row.length < 3 || row[0] == null || row[1] == null || row[2] == null) continue;
                long stateId = ((Number) row[0]).longValue();
                String code = row[1].toString();
                String valueStr = row[2].toString();
                if ("null".equalsIgnoreCase(valueStr.trim())) continue;
                if ("Voltage".equals(code)) {
                    int v = parseValueToInt(valueStr);
                    if (v != 0) voltageByState.put(stateId, v / 10);
                } else {
                    currentByState.putIfAbsent(stateId, parseValueToInt(valueStr));
                }
            }
        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ⚠️ combined I/U machine+period: " + e.getMessage());
        }
    }

    /** stateId для загрузки параметров: Welding + соседние состояния (границы шва). */
    private static List<Long> collectWeldingRelatedStateIds(List<WeldingMachineState> sorted) {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (int i = 0; i < sorted.size(); i++) {
            WeldingMachineState s = sorted.get(i);
            if (s.getId() == null) continue;
            if (s.getWeldingMachineStatus() == WeldingMachineStatus.Welding) {
                ids.add(s.getId());
                if (i > 0 && sorted.get(i - 1).getId() != null) ids.add(sorted.get(i - 1).getId());
                if (i + 1 < sorted.size() && sorted.get(i + 1).getId() != null) ids.add(sorted.get(i + 1).getId());
            }
        }
        if (ids.isEmpty()) {
            return sorted.stream().map(WeldingMachineState::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
        }
        return new ArrayList<>(ids);
    }

    private static int lowerBoundByDateCreated(List<WeldingMachineState> sorted, java.time.LocalDateTime t) {
        int lo = 0, hi = sorted.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            java.time.LocalDateTime dc = sorted.get(mid).getDateCreated();
            if (dc == null || dc.isBefore(t)) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private static List<WeldingMachineState> statesInTimeWindow(
            List<WeldingMachineState> sorted, java.time.LocalDateTime fromInclusive, java.time.LocalDateTime toExclusive) {
        if (sorted == null || sorted.isEmpty()) return java.util.Collections.emptyList();
        int lo = lowerBoundByDateCreated(sorted, fromInclusive);
        int hi = lowerBoundByDateCreated(sorted, toExclusive);
        if (lo >= hi || lo >= sorted.size()) return java.util.Collections.emptyList();
        return sorted.subList(lo, Math.min(hi, sorted.size()));
    }

    private static void mergeParamMap(java.util.Map<Long, Integer> target, java.util.Map<Long, Integer> source) {
        if (source == null) return;
        for (java.util.Map.Entry<Long, Integer> e : source.entrySet()) {
            if (e.getValue() != null) target.putIfAbsent(e.getKey(), e.getValue());
        }
    }

    private java.util.Map<Long, Integer> loadParamMapForPeriod(
            Integer machineId, LocalDateTime rangeStart, LocalDateTime rangeEnd,
            List<Long> fallbackStateIds, String propertyCode, boolean useCoalesce, boolean useMachineDateRange) {
        if (useMachineDateRange) {
            return loadParamMapByMachineDateRange(machineId, rangeStart, rangeEnd, propertyCode, useCoalesce);
        }
        return loadParamMapByStateIds(fallbackStateIds, propertyCode, useCoalesce);
    }

    private java.util.Map<Long, Integer> loadParamMapByMachineDateRange(
            Integer machineId, LocalDateTime start, LocalDateTime end, String propertyCode, boolean useCoalesce) {
        java.util.Map<Long, Integer> map = new java.util.HashMap<>();
        if (machineId == null || start == null || end == null) return map;
        try {
            List<Object[]> rows = useCoalesce
                    ? parameterValueRepository.findStateIdAndValueByMachineDateRangeCoalesce(machineId, start, end, propertyCode)
                    : parameterValueRepository.findStateIdAndValueByMachineDateRange(machineId, start, end, propertyCode);
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    long stateId = ((Number) row[0]).longValue();
                    String valueStr = row[1].toString();
                    if ("null".equalsIgnoreCase(valueStr.trim())) continue;
                    map.put(stateId, parseValueToInt(valueStr));
                }
            }
        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ⚠️ machine+period " + propertyCode + ": " + e.getMessage());
        }
        return map;
    }

    private java.util.Map<Long, String> loadStateNameByMachineDateRange(
            Integer machineId, LocalDateTime start, LocalDateTime end) {
        java.util.Map<Long, String> map = new java.util.HashMap<>();
        if (machineId == null || start == null || end == null) return map;
        try {
            List<Object[]> rows = parameterValueRepository.findStateIdAndValueByMachineDateRangeAndCodes(
                    machineId, start, end, java.util.Arrays.asList("WeldingMachineState", "Состояние аппарата"));
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    long stateId = ((Number) row[0]).longValue();
                    map.putIfAbsent(stateId, row[1].toString().trim());
                }
            }
        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ⚠️ machine+period state names: " + e.getMessage());
        }
        return map;
    }

    /** Date./Time.* за период одним JOIN (не десятки батчей по stateId). */
    private java.util.Map<Long, LocalDateTime> loadDeviceDateTimeByMachinePeriodCombined(
            Integer machineId, LocalDateTime start, LocalDateTime end) {
        java.util.Map<Long, LocalDateTime> out = new java.util.HashMap<>();
        if (machineId == null || start == null || end == null) return out;
        java.util.Map<String, java.util.Map<Long, Integer>> parts = new java.util.HashMap<>();
        for (String c : new String[] { "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds" }) {
            parts.put(c, new java.util.HashMap<>());
        }
        try {
            List<Object[]> rows = parameterValueRepository.findStateIdCodeAndValueByMachineDateRange(
                    machineId, start, end, java.util.Arrays.asList(
                            "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds"));
            for (Object[] row : rows) {
                if (row == null || row.length < 3 || row[0] == null || row[1] == null || row[2] == null) continue;
                long stateId = ((Number) row[0]).longValue();
                String code = row[1].toString();
                java.util.Map<Long, Integer> map = parts.get(code);
                if (map == null) continue;
                try {
                    map.put(stateId, Integer.parseInt(row[2].toString().trim()));
                } catch (NumberFormatException ignored) { }
            }
        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ⚠️ device time machine+period: " + e.getMessage());
        }
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (java.util.Map<Long, Integer> m : parts.values()) ids.addAll(m.keySet());
        for (Long id : ids) {
            Integer y = parts.get("Date.Year").get(id);
            Integer mo = parts.get("Date.Month").get(id);
            Integer d = parts.get("Date.Day").get(id);
            Integer h = parts.get("Time.Hours").get(id);
            Integer mi = parts.get("Time.Minutes").get(id);
            Integer se = parts.get("Time.Seconds").get(id);
            if (y == null || mo == null || d == null || h == null || mi == null || se == null) continue;
            if (y < 100) y = 2000 + y;
            if (mo < 1 || mo > 12 || d < 1 || d > 31) continue;
            if (h < 0 || h > 23 || mi < 0 || mi > 59 || se < 0 || se > 59) continue;
            try {
                out.put(id, LocalDateTime.of(y, mo, d, h, mi, se));
            } catch (Exception ignored) { }
        }
        return out;
    }

    private java.util.Map<Long, LocalDateTime> loadDeviceDateTimeByMachineDateRange(
            Integer machineId, LocalDateTime start, LocalDateTime end) {
        java.util.Map<Long, LocalDateTime> out = new java.util.HashMap<>();
        if (machineId == null || start == null || end == null) return out;
        java.util.Map<Long, Integer> year = loadParamMapByMachineDateRange(machineId, start, end, "Date.Year", false);
        java.util.Map<Long, Integer> month = loadParamMapByMachineDateRange(machineId, start, end, "Date.Month", false);
        java.util.Map<Long, Integer> day = loadParamMapByMachineDateRange(machineId, start, end, "Date.Day", false);
        java.util.Map<Long, Integer> hour = loadParamMapByMachineDateRange(machineId, start, end, "Time.Hours", false);
        java.util.Map<Long, Integer> min = loadParamMapByMachineDateRange(machineId, start, end, "Time.Minutes", false);
        java.util.Map<Long, Integer> sec = loadParamMapByMachineDateRange(machineId, start, end, "Time.Seconds", false);
        java.util.Set<Long> ids = new java.util.HashSet<>();
        ids.addAll(year.keySet());
        ids.addAll(month.keySet());
        ids.addAll(day.keySet());
        for (Long id : ids) {
            Integer y = year.get(id);
            Integer mo = month.get(id);
            Integer d = day.get(id);
            Integer h = hour.get(id);
            Integer mi = min.get(id);
            Integer se = sec.get(id);
            if (y == null || mo == null || d == null || h == null || mi == null || se == null) continue;
            if (y < 100) y = 2000 + y;
            if (mo < 1 || mo > 12 || d < 1 || d > 31) continue;
            if (h < 0 || h > 23 || mi < 0 || mi > 59 || se < 0 || se > 59) continue;
            try {
                out.put(id, LocalDateTime.of(y, mo, d, h, mi, se));
            } catch (Exception ignored) { }
        }
        return out;
    }

    /**
     * Загружает карту stateId → значение параметра (value или, при useCoalesce, COALESCE(value, raw_value)).
     */
    private java.util.Map<Long, Integer> loadParamMapByStateIds(List<Long> stateIds, String propertyCode) {
        return loadParamMapByStateIds(stateIds, propertyCode, false);
    }

    private java.util.Map<Long, Integer> loadParamMapByStateIds(List<Long> stateIds, String propertyCode, boolean useCoalesce) {
        java.util.Map<Long, Integer> map = new java.util.HashMap<>();
        if (stateIds == null || stateIds.isEmpty()) return map;
        final int BATCH_SIZE = 10_000;
        for (int i = 0; i < stateIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, stateIds.size());
            List<Long> batch = stateIds.subList(i, endIndex);
            try {
                List<Object[]> rows = useCoalesce
                        ? parameterValueRepository.findStateIdAndValueNativeCoalesce(batch, propertyCode)
                        : parameterValueRepository.findStateIdAndValueNative(batch, propertyCode);
                for (Object[] row : rows) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        long stateId = ((Number) row[0]).longValue();
                        String valueStr = row[1].toString();
                        if ("null".equalsIgnoreCase(valueStr.trim())) continue;
                        map.put(stateId, parseValueToInt(valueStr));
                    }
                }
            } catch (Exception e) {
                System.err.println("[REPORT-CALC] ⚠️ Ошибка загрузки " + propertyCode + " батч " + i + "-" + endIndex + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * Загружает время аппарата (CORE: Date.Year/Month/Day, Time.Hours/Minutes/Seconds) по stateId.
     */
    private java.util.Map<Long, LocalDateTime> loadDeviceDateTimeByStateIds(List<Long> stateIds) {
        java.util.Map<Long, LocalDateTime> out = new java.util.HashMap<>();
        if (stateIds == null || stateIds.isEmpty()) return out;
        java.util.Map<String, java.util.Map<Long, Integer>> parts = new java.util.HashMap<>();
        for (String c : new String[] { "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds" }) {
            parts.put(c, new java.util.HashMap<>());
        }
        final int batchSize = 10_000;
        java.util.List<String> codes = java.util.Arrays.asList(
                "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds");
        for (int i = 0; i < stateIds.size(); i += batchSize) {
            java.util.List<Long> batch = stateIds.subList(i, Math.min(i + batchSize, stateIds.size()));
            try {
                java.util.List<Object[]> rows = parameterValueRepository.findStateIdCodeAndValueNativeByStateIds(batch, codes);
                for (Object[] row : rows) {
                    if (row == null || row.length < 3 || row[0] == null || row[1] == null || row[2] == null) continue;
                    long stateId = ((Number) row[0]).longValue();
                    String code = row[1].toString();
                    java.util.Map<Long, Integer> map = parts.get(code);
                    if (map == null) continue;
                    try {
                        map.put(stateId, Integer.parseInt(row[2].toString().trim()));
                    } catch (NumberFormatException ignored) { }
                }
            } catch (Exception e) {
                System.err.println("[REPORT-CALC] ⚠️ device time batch: " + e.getMessage());
            }
        }
        java.util.Set<Long> ids = new java.util.HashSet<>(stateIds);
        for (Long id : ids) {
            Integer y = parts.get("Date.Year").get(id);
            Integer mo = parts.get("Date.Month").get(id);
            Integer d = parts.get("Date.Day").get(id);
            Integer h = parts.get("Time.Hours").get(id);
            Integer mi = parts.get("Time.Minutes").get(id);
            Integer se = parts.get("Time.Seconds").get(id);
            if (y == null || mo == null || d == null || h == null || mi == null || se == null) continue;
            if (y < 100) y = 2000 + y;
            if (mo < 1 || mo > 12 || d < 1 || d > 31) continue;
            if (h < 0 || h > 23 || mi < 0 || mi > 59 || se < 0 || se > 59) continue;
            try {
                out.put(id, LocalDateTime.of(y, mo, d, h, mi, se));
            } catch (Exception ignored) { }
        }
        return out;
    }

    /**
     * Загружает для каждого stateId строковое значение состояния (WeldingMachineState или Состояние аппарата).
     * Нужно для определения «Сварка» по параметру, если в колонке welding_machine_status не проставлено Welding.
     */
    private java.util.Map<Long, String> loadStateNameByStateIds(List<Long> stateIds) {
        java.util.Map<Long, String> map = new java.util.HashMap<>();
        if (stateIds == null || stateIds.isEmpty()) return map;
        final int BATCH_SIZE = 10_000;
        for (int i = 0; i < stateIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, stateIds.size());
            List<Long> batch = stateIds.subList(i, endIndex);
            for (String propertyCode : new String[] { "WeldingMachineState", "Состояние аппарата" }) {
                try {
                    List<Object[]> rows = parameterValueRepository.findStateIdAndValueNative(batch, propertyCode);
                    for (Object[] row : rows) {
                        if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                            long stateId = ((Number) row[0]).longValue();
                            map.putIfAbsent(stateId, row[1].toString().trim());
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return map;
    }

    private int parseValueToInt(String raw) {
        if (raw == null || (raw = raw.trim()).isEmpty()) return 0;
        // CORE и др. могут писать значения с десятичной точкой ("50.0", "139.0", "13.9") — парсим как double
        if (raw.contains(".") || raw.contains(",")) {
            try {
                String normalized = raw.replace(',', '.');
                return (int) Math.round(Double.parseDouble(normalized));
            } catch (NumberFormatException e) {
                // fallback ниже
            }
        }
        // Целые: десятичное или hex
        if (raw.matches("-?\\d+")) {
            try {
                return Integer.parseInt(raw, 10);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        try {
            return Integer.parseInt(raw, 16);
        } catch (NumberFormatException e) {
            try {
                return Integer.parseInt(raw, 10);
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }
}
