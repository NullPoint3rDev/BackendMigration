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
     * Рассчитывает средние значения тока и напряжения для указанного аппарата за период времени
     * Учитывает только периоды активной сварки (когда ток > 0)
     */
    public AverageValues calculateAverageValues(String mac, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Находим аппарат по MAC
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
            if (!machineOpt.isPresent()) {
                System.out.println("[REPORT-CALC] ❌ Аппарат с MAC " + mac + " не найден");
                return new AverageValues(BigDecimal.ZERO, BigDecimal.ZERO);
            }

            WeldingMachine machine = machineOpt.get();
            System.out.println("[REPORT-CALC] 🔍 Расчет для аппарата: " + machine.getName() + " (MAC: " + mac + ")");
            System.out.println("[REPORT-CALC] 📅 Период: " + startDate + " - " + endDate);

            // Получаем состояния за указанный период
            List<WeldingMachineState> states = weldingMachineStateRepository
                    .findByWeldingMachineIdAndDateRange(machine.getId(), startDate, endDate);

            if (states.isEmpty()) {
                System.out.println("[REPORT-CALC] ⚠️ Нет данных за указанный период");
                return new AverageValues(BigDecimal.ZERO, BigDecimal.ZERO);
            }

            System.out.println("[REPORT-CALC] 📊 Найдено состояний: " + states.size());

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

            System.out.println("[REPORT-CALC] ⚡ Найдено значений тока: " + currentValues.size());
            System.out.println("[REPORT-CALC] 🔌 Найдено значений напряжения: " + voltageValues.size());

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

            System.out.println("[REPORT-CALC] ✅ Средний ток: " + averageCurrent + " А");
            System.out.println("[REPORT-CALC] ✅ Среднее напряжение: " + averageVoltage + " В");
            System.out.println("[REPORT-CALC] ✅ Время сварки: " + weldingTimeSeconds + " с");

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
                            System.out.println("[REPORT-CALC] 🔍 " + propertyCode + ": hex '" + value + "' -> " + decimalValue);
                        } catch (NumberFormatException hexException) {
                            // Если не hex, пробуем как decimal
                            decimalValue = Integer.parseInt(value, 10);
                            System.out.println("[REPORT-CALC] 🔍 " + propertyCode + ": decimal '" + value + "' -> " + decimalValue);
                        }
                        return BigDecimal.valueOf(decimalValue);
                    } catch (NumberFormatException e) {
                        System.out.println("[REPORT-CALC] ❌ " + propertyCode + ": не удалось распарсить '" + value + "'");
                        return BigDecimal.ZERO;
                    }
                })
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0) // Только активная сварка
                .collect(java.util.stream.Collectors.toList());

        if (activeValues.isEmpty()) {
            System.out.println("[REPORT-CALC] ⚠️ Нет активной сварки для " + propertyCode);
            return BigDecimal.ZERO;
        }

        // Рассчитываем среднее значение
        BigDecimal sum = activeValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = sum.divide(BigDecimal.valueOf(activeValues.size()), 1, RoundingMode.HALF_UP);

        System.out.println("[REPORT-CALC] 📈 " + propertyCode + ": " + activeValues.size() + " активных значений, среднее: " + average);

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

        System.out.println("[REPORT-CALC] ========================================");
        System.out.println("[REPORT-CALC] 🔍 РАСЧЕТ ДАННЫХ ПО ДНЯМ");
        System.out.println("[REPORT-CALC] ========================================");
        System.out.println("[REPORT-CALC] MAC: " + mac);
        System.out.println("[REPORT-CALC] Период: " + startDate + " - " + endDate);

        try {
            // Находим аппарат по MAC
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
            if (!machineOpt.isPresent()) {
                System.out.println("[REPORT-CALC] ❌ Аппарат с MAC " + mac + " не найден");
                return dailyData;
            }

            WeldingMachine machine = machineOpt.get();
            System.out.println("[REPORT-CALC] 🔍 Расчет по дням для аппарата: " + machine.getName() + " (MAC: " + mac + ")");
            System.out.println("[REPORT-CALC] 📅 Период: " + startDate + " - " + endDate);

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

                System.out.println("[REPORT-CALC] 📊 " + currentDate.toLocalDate() +
                        ": Ток=" + dayAverages.getAverageCurrent() + "А, Напряжение=" + dayAverages.getAverageVoltage() + "В, Время сварки=" + dayAverages.getWeldingTimeSeconds() + "с");

                // Переходим к следующему дню
                currentDate = currentDate.plusDays(1);
            }

            System.out.println("[REPORT-CALC] ✅ Создано " + dailyData.size() + " записей по дням");

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
        System.out.println("[REPORT-CALC] 🔍 Расчет для периода: " + startDate + " - " + endDate + " (MachineId: " + machineId + ")");

        try {
            // Получаем состояния за указанный период
            List<WeldingMachineState> states = weldingMachineStateRepository
                    .findByWeldingMachineIdAndDateRange(machineId, startDate, endDate);

            System.out.println("[REPORT-CALC] 📊 Найдено состояний за период: " + states.size());

            if (states.isEmpty()) {
                System.out.println("[REPORT-CALC] ⚠️ Нет данных за период " + startDate + " - " + endDate);
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

            System.out.println("[REPORT-CALC] ⚡ Найдено значений тока: " + currentValues.size());
            System.out.println("[REPORT-CALC] 🔌 Найдено значений напряжения: " + voltageValues.size());

            // Логируем первые несколько значений для отладки
            if (!currentValues.isEmpty()) {
                System.out.println("[REPORT-CALC] 🔍 Первые 5 значений тока:");
                currentValues.stream().limit(5).forEach(value ->
                        System.out.println("[REPORT-CALC]   - " + value.getPropertyCode() + ": '" + value.getValue() + "'"));
            }
            if (!voltageValues.isEmpty()) {
                System.out.println("[REPORT-CALC] 🔍 Первые 5 значений напряжения:");
                voltageValues.stream().limit(5).forEach(value ->
                        System.out.println("[REPORT-CALC]   - " + value.getPropertyCode() + ": '" + value.getValue() + "'"));
            }

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

            System.out.println("[REPORT-CALC] ✅ Результат: Ток=" + averageCurrent + "А, Напряжение=" + averageVoltage + "В, Время сварки=" + weldingTimeSeconds + "с");

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

    /**
     * Рассчитывает сегменты швов по порогам: старт при I>3А непрерывно 500мс, стоп при I=0А непрерывно 500мс.
     * Для каждого сегмента считает средний ток/напряжение (вес по длительности) и длительность (с) с округлением 1 знак.
     */
    public List<org.alloy.models.dto.WeldSegmentDTO> calculateWeldSegments(Integer machineId, LocalDateTime startDate, LocalDateTime endDate) {
        List<org.alloy.models.dto.WeldSegmentDTO> result = new ArrayList<>();
        try {
            // Загружаем состояния и сортируем по времени возрастанию
            List<WeldingMachineState> states = weldingMachineStateRepository
                    .findByWeldingMachineIdAndDateRange(machineId, startDate, endDate);
            if (states.isEmpty()) {
                System.out.println("[REPORT-CALC] machineId=" + machineId + " period=" + startDate + ".." + endDate + " -> states=0");
                return result;
            }
            states.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
            List<Long> stateIds = states.stream().map(WeldingMachineState::getId).collect(java.util.stream.Collectors.toList());
            System.out.println("[REPORT-CALC] machineId=" + machineId + " states=" + states.size() + " stateIds=" + stateIds.size());

            // Сначала пробуем Current/Voltage (CORE) — обычно больше покрытие; при пустом результате State.I/State.U (блоки мониторинга)
            java.util.Map<Long, Integer> currentByState = loadParamMapByStateIds(stateIds, "Current");
            if (currentByState.isEmpty()) {
                currentByState = loadParamMapByStateIds(stateIds, "State.I");
            }
            System.out.println("[REPORT-CALC] currentByState.size()=" + currentByState.size());
            java.util.Map<Long, Integer> voltageByState = loadParamMapByStateIds(stateIds, "Voltage");
            boolean voltageInTenths = !voltageByState.isEmpty(); // CORE: Voltage в десятых (175 = 17.5 В)
            if (voltageByState.isEmpty()) {
                voltageByState = loadParamMapByStateIds(stateIds, "State.U");
            }
            System.out.println("[REPORT-CALC] voltageByState.size()=" + voltageByState.size() + " voltageInTenths=" + voltageInTenths);
            if (voltageInTenths && !voltageByState.isEmpty()) {
                voltageByState = voltageByState.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                e -> e.getValue() != 0 ? e.getValue() / 10 : 0));
            }

            boolean inWeld = false;
            long above3AccumMs = 0; // накопитель для старта
            long zeroAccumMs = 0;   // накопитель для остановки
            LocalDateTime weldStartTime = null;

            // Для усреднения по длительности в рамках сегмента
            long segmentDurationMs = 0;
            long segmentWeightedCurrent = 0; // сумма I*dt (в А*мс)
            long segmentWeightedVoltage = 0; // сумма U*dt (в В*мс)

            for (int i = 0; i < states.size(); i++) {
                WeldingMachineState s = states.get(i);
                long dtMs = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
                if (dtMs <= 0) {
                    // Фоллбэк: используем разницу между текущим и следующим состоянием
                    if (i + 1 < states.size()) {
                        LocalDateTime t1 = s.getDateCreated();
                        LocalDateTime t2 = states.get(i + 1).getDateCreated();
                        long diff = java.time.Duration.between(t1, t2).toMillis();
                        if (diff > 0) dtMs = diff;
                    }
                }
                int I = currentByState.getOrDefault(s.getId(), 0);
                int U = voltageByState.getOrDefault(s.getId(), 0);
                if (i < 3) {
                    System.out.println("[REPORT-CALC] state[" + i + "] id=" + s.getId() + " dtMs=" + dtMs + " I=" + I + " U=" + U);
                }

                if (!inWeld) {
                    if (I > 3) {
                        above3AccumMs += dtMs;
                        if (above3AccumMs >= 500) {
                            inWeld = true;
                            weldStartTime = s.getDateCreated();
                            // сбрасываем усреднение
                            segmentDurationMs = 0;
                            segmentWeightedCurrent = 0;
                            segmentWeightedVoltage = 0;
                            zeroAccumMs = 0;
                        }
                    } else {
                        above3AccumMs = 0;
                    }
                }

                if (inWeld) {
                    // накапливаем взвешенные значения
                    segmentDurationMs += dtMs;
                    segmentWeightedCurrent += (long) I * dtMs;
                    segmentWeightedVoltage += (long) U * dtMs;

                    if (I == 0) {
                        zeroAccumMs += dtMs;
                        if (zeroAccumMs >= 500) {
                            // завершить сегмент на текущем состоянии
                            org.alloy.models.dto.WeldSegmentDTO seg = new org.alloy.models.dto.WeldSegmentDTO();
                            seg.setStartTime(weldStartTime != null ? weldStartTime : s.getDateCreated());
                            BigDecimal durationSec = BigDecimal.valueOf(segmentDurationMs / 1000.0).setScale(1, RoundingMode.HALF_UP);
                            seg.setDurationSeconds(durationSec);
                            BigDecimal avgI = segmentDurationMs > 0
                                    ? BigDecimal.valueOf((double) segmentWeightedCurrent / segmentDurationMs).setScale(1, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;
                            BigDecimal avgU = segmentDurationMs > 0
                                    ? BigDecimal.valueOf((double) segmentWeightedVoltage / segmentDurationMs).setScale(1, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;
                            seg.setAverageCurrent(avgI);
                            seg.setAverageVoltage(avgU);
                            result.add(seg);

                            // сброс состояния
                            inWeld = false;
                            above3AccumMs = 0;
                            zeroAccumMs = 0;
                            weldStartTime = null;
                            segmentDurationMs = 0;
                            segmentWeightedCurrent = 0;
                            segmentWeightedVoltage = 0;
                        }
                    } else {
                        // внутри шва, ток не нулевой — сбрасываем накопитель остановки
                        zeroAccumMs = 0;
                    }
                }
            }

            // Если период закончился внутри шва — закрываем сегмент на конце периода
            if (inWeld && segmentDurationMs > 0 && weldStartTime != null) {
                org.alloy.models.dto.WeldSegmentDTO seg = new org.alloy.models.dto.WeldSegmentDTO();
                seg.setStartTime(weldStartTime);
                BigDecimal durationSec = BigDecimal.valueOf(segmentDurationMs / 1000.0).setScale(1, RoundingMode.HALF_UP);
                seg.setDurationSeconds(durationSec);
                BigDecimal avgI = BigDecimal.valueOf((double) segmentWeightedCurrent / segmentDurationMs).setScale(1, RoundingMode.HALF_UP);
                BigDecimal avgU = BigDecimal.valueOf((double) segmentWeightedVoltage / segmentDurationMs).setScale(1, RoundingMode.HALF_UP);
                seg.setAverageCurrent(avgI);
                seg.setAverageVoltage(avgU);
                result.add(seg);
            }

            System.out.println("[REPORT-CALC] segments count=" + result.size());
            if (!result.isEmpty()) {
                org.alloy.models.dto.WeldSegmentDTO first = result.get(0);
                System.out.println("[REPORT-CALC] first segment: avgI=" + first.getAverageCurrent() + " avgU=" + first.getAverageVoltage() + " durationSec=" + first.getDurationSeconds());
            }
        } catch (Exception e) {
            System.err.println("[REPORT-CALC] ❌ Ошибка расчета сегментов швов: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Загружает карту stateId → значение параметра через нативный запрос (колонки welding_machine_stateid, property_code),
     * чтобы корректно читать данные при любой стратегии именования Hibernate.
     */
    private java.util.Map<Long, Integer> loadParamMapByStateIds(List<Long> stateIds, String propertyCode) {
        java.util.Map<Long, Integer> map = new java.util.HashMap<>();
        if (stateIds == null || stateIds.isEmpty()) return map;
        final int BATCH_SIZE = 10_000;
        int totalRows = 0;
        for (int i = 0; i < stateIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, stateIds.size());
            List<Long> batch = stateIds.subList(i, endIndex);
            try {
                List<Object[]> rows = parameterValueRepository.findStateIdAndValueNative(batch, propertyCode);
                totalRows += rows.size();
                for (Object[] row : rows) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        long stateId = ((Number) row[0]).longValue();
                        String valueStr = row[1].toString();
                        map.put(stateId, parseValueToInt(valueStr));
                    }
                }
                if (i == 0 && !rows.isEmpty() && rows.get(0) != null && rows.get(0).length >= 2) {
                    Object[] first = rows.get(0);
                    System.out.println("[REPORT-CALC] loadParam " + propertyCode + " first row: stateId=" + first[0] + " value=" + first[1] + " -> parsed=" + parseValueToInt(first[1].toString()));
                }
            } catch (Exception e) {
                System.err.println("[REPORT-CALC] ⚠️ Ошибка загрузки " + propertyCode + " батч " + i + "-" + endIndex + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("[REPORT-CALC] loadParam " + propertyCode + ": stateIds=" + stateIds.size() + " rows=" + totalRows + " map.size()=" + map.size());
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
