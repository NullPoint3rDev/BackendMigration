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

            System.out.println("[REPORT-CALC] ✅ Средний ток: " + averageCurrent + " А");
            System.out.println("[REPORT-CALC] ✅ Среднее напряжение: " + averageVoltage + " В");

            return new AverageValues(averageCurrent, averageVoltage);

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
                        // Конвертируем hex в decimal
                        int hexValue = Integer.parseInt(value, 16);
                        return BigDecimal.valueOf(hexValue);
                    } catch (NumberFormatException e) {
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

        public AverageValues(BigDecimal averageCurrent, BigDecimal averageVoltage) {
            this.averageCurrent = averageCurrent;
            this.averageVoltage = averageVoltage;
        }

        public BigDecimal getAverageCurrent() {
            return averageCurrent;
        }

        public BigDecimal getAverageVoltage() {
            return averageVoltage;
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
                dayData.setWeldingMachineId(machine.getId());
                dayData.setWeldingMachineName(machine.getName());
                dayData.setOrganizationUnitName(machine.getOrganizationUnit() != null ? 
                    machine.getOrganizationUnit().getName() : "Не указано");
                
                dailyData.add(dayData);
                
                System.out.println("[REPORT-CALC] 📊 " + currentDate.toLocalDate() + 
                    ": Ток=" + dayAverages.getAverageCurrent() + "А, Напряжение=" + dayAverages.getAverageVoltage() + "В");
                
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

            // Рассчитываем средние значения
            BigDecimal averageCurrent = calculateAverageForActiveWelding(currentValues, "State.I");
            BigDecimal averageVoltage = calculateAverageForActiveWelding(voltageValues, "State.U");

            return new AverageValues(averageCurrent, averageVoltage);

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
        
        public Integer getWeldingMachineId() { return weldingMachineId; }
        public void setWeldingMachineId(Integer weldingMachineId) { this.weldingMachineId = weldingMachineId; }
        
        public String getWeldingMachineName() { return weldingMachineName; }
        public void setWeldingMachineName(String weldingMachineName) { this.weldingMachineName = weldingMachineName; }
        
        public String getOrganizationUnitName() { return organizationUnitName; }
        public void setOrganizationUnitName(String organizationUnitName) { this.organizationUnitName = organizationUnitName; }
    }
}
