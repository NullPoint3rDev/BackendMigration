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
}
