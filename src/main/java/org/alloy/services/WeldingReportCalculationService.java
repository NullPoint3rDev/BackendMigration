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
        List<org.alloy.models.dto.WeldSegmentDTO> result = new ArrayList<>();
        try {
            List<WeldingMachineState> states = weldingMachineStateRepository
                    .findByWeldingMachineIdAndDateRange(machineId, startDate, endDate);
            if (states.isEmpty()) {
                return result;
            }
            states.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
            List<Long> stateIds = states.stream().map(WeldingMachineState::getId).collect(java.util.stream.Collectors.toList());

            // Ток: Current и State.I с COALESCE(value, raw_value); остальные коды — по value
            java.util.Map<Long, Integer> currentByState = new java.util.HashMap<>();
            for (String code : new String[] { "Current", "State.I" }) {
                for (java.util.Map.Entry<Long, Integer> e : loadParamMapByStateIds(stateIds, code, true).entrySet()) {
                    if (e.getValue() != null)
                        currentByState.putIfAbsent(e.getKey(), e.getValue());
                }
            }
            for (String code : new String[] { "I", "Ток" }) {
                for (java.util.Map.Entry<Long, Integer> e : loadParamMapByStateIds(stateIds, code).entrySet()) {
                    if (e.getValue() != null)
                        currentByState.putIfAbsent(e.getKey(), e.getValue());
                }
            }
            // Напряжение: только Voltage (CORE, в десятых)
            java.util.Map<Long, Integer> voltageByState = new java.util.HashMap<>();
            java.util.Map<Long, Integer> voltageFromVoltage = loadParamMapByStateIds(stateIds, "Voltage", true);
            for (Long id : stateIds) {
                if (voltageFromVoltage.containsKey(id) && voltageFromVoltage.get(id) != null && voltageFromVoltage.get(id) != 0) {
                    voltageByState.put(id, voltageFromVoltage.get(id) / 10);
                }
            }

            // Состояние «Сварка»: по колонке welding_machine_status или по параметру
            java.util.Map<Long, String> stateNameByStateId = loadStateNameByStateIds(stateIds);
            // Время аппарата (CORE Date.*/Time.*) — для длительности и отображения, чтобы не было расхождения с серверным временем
            java.util.Map<Long, LocalDateTime> deviceTimeByStateId = loadDeviceDateTimeByStateIds(stateIds);

            boolean inWeld = false;
            LocalDateTime weldStartTime = null;
            Long weldStartStateId = null;
            long segmentWeightedCurrent = 0;
            long segmentWeightedVoltage = 0;

            for (int i = 0; i < states.size(); i++) {
                WeldingMachineState s = states.get(i);
                boolean isWelding = isWeldingState(s, stateNameByStateId);
                long dtMs = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
                if (dtMs <= 0 && i + 1 < states.size()) {
                    long diff = java.time.Duration.between(s.getDateCreated(), states.get(i + 1).getDateCreated()).toMillis();
                    if (diff > 0) dtMs = diff;
                }
                int I = currentByState.getOrDefault(s.getId(), 0);
                int U = voltageByState.getOrDefault(s.getId(), 0);

                if (isWelding) {
                    if (!inWeld) {
                        inWeld = true;
                        weldStartTime = s.getDateCreated();
                        weldStartStateId = s.getId();
                        segmentWeightedCurrent = 0;
                        segmentWeightedVoltage = 0;
                    }
                    if (I > MIN_ARC_CURRENT_AMPS_FOR_REPORT && isValidCurrentVoltagePoint(I, U)) {
                        segmentWeightedCurrent += (long) I * dtMs;
                        segmentWeightedVoltage += (long) U * dtMs;
                    }
                } else {
                    if (inWeld && weldStartTime != null && weldStartStateId != null && s.getDateCreated() != null) {
                        LocalDateTime segmentStartForDto = weldStartTime;
                        LocalDateTime segmentEndTime = s.getDateCreated();
                        long segmentDurationMs = java.time.Duration.between(weldStartTime, segmentEndTime).toMillis();
                        LocalDateTime deviceStart = deviceTimeByStateId.get(weldStartStateId);
                        LocalDateTime deviceEnd = deviceTimeByStateId.get(s.getId());
                        if (deviceStart != null && deviceEnd != null && !deviceEnd.isBefore(deviceStart)) {
                            segmentDurationMs = java.time.Duration.between(deviceStart, deviceEnd).toMillis();
                            segmentStartForDto = deviceStart;
                            segmentEndTime = deviceEnd;
                        }
                        if (i + 1 < states.size()) {
                            WeldingMachineState next = states.get(i + 1);
                            boolean nextWelding = isWeldingState(next, stateNameByStateId);
                            if (nextWelding) {
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
                        if (segmentDurationMs > 0) {
                            LocalDateTime serverEnd = s.getDateCreated();
                            org.alloy.models.dto.WeldSegmentDTO seg = buildSegmentDto(
                                    weldStartTime, serverEnd, segmentDurationMs, segmentWeightedCurrent, segmentWeightedVoltage,
                                    states, currentByState, voltageByState, segmentStartForDto, stateNameByStateId);
                            result.add(seg);
                        }
                    }
                    inWeld = false;
                }
            }

            if (inWeld && weldStartTime != null) {
                long segmentDurationMs = 0;
                for (int j = 0; j < states.size(); j++) {
                    WeldingMachineState sx = states.get(j);
                    boolean w = isWeldingState(sx, stateNameByStateId);
                    if (w && sx.getDateCreated() != null && !sx.getDateCreated().isBefore(weldStartTime)) {
                        long d = sx.getStateDurationMs() != null ? sx.getStateDurationMs() : 0L;
                        if (d <= 0 && j + 1 < states.size())
                            d = java.time.Duration.between(sx.getDateCreated(), states.get(j + 1).getDateCreated()).toMillis();
                        segmentDurationMs += d;
                    } else if (!w) break;
                }
                if (segmentDurationMs > 0) {
                    LocalDateTime segmentEndTime = weldStartTime.plus(segmentDurationMs, java.time.temporal.ChronoUnit.MILLIS);
                    LocalDateTime displayStart = deviceTimeByStateId.get(weldStartStateId);
                    org.alloy.models.dto.WeldSegmentDTO seg = buildSegmentDto(
                            weldStartTime, segmentEndTime, segmentDurationMs, segmentWeightedCurrent, segmentWeightedVoltage,
                            states, currentByState, voltageByState, displayStart, stateNameByStateId);
                    result.add(seg);
                }
            }

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
        java.util.Map<Long, Integer> year = loadParamMapByStateIds(stateIds, "Date.Year");
        java.util.Map<Long, Integer> month = loadParamMapByStateIds(stateIds, "Date.Month");
        java.util.Map<Long, Integer> day = loadParamMapByStateIds(stateIds, "Date.Day");
        java.util.Map<Long, Integer> hour = loadParamMapByStateIds(stateIds, "Time.Hours");
        java.util.Map<Long, Integer> min = loadParamMapByStateIds(stateIds, "Time.Minutes");
        java.util.Map<Long, Integer> sec = loadParamMapByStateIds(stateIds, "Time.Seconds");
        for (Long id : stateIds) {
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
