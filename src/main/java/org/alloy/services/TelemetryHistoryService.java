package org.alloy.services;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.dto.TelemetryHistoryPointDTO;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.repositories.WeldingMachineParameterValueRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TelemetryHistoryService {

    /** Слишком большой IN по state id даёт тяжёлый план, долго держит соединение и может рвать PG → батчи. */
    private static final int PARAMETER_QUERY_BATCH_SIZE = 2500;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private WeldingMachineParameterValueRepository weldingMachineParameterValueRepository;

    // В БД (WT2) ток/напряжение хранятся как propertyCode = Current / Voltage.
    // Для Core-пакетов вне режима сварки в эти же ключи попадают установки/холостой ход (см. WeldingDataParserService),
    // поэтому для графика «сварки» отдаём значения только при статусе Welding.
    private static final List<String> CURRENT_CODES = List.of("Current");
    private static final List<String> VOLTAGE_CODES = List.of("Voltage");
    /** Мгновенный расход газа (л/мин), не накопленный счётчик. */
    private static final List<String> GAS_FLOW_CODES = List.of("State.GasFlow", "GasFlow", "gasFlow");
    private static final List<String> MAINS_VOLTAGE_A_CODES = List.of(
            "Напряжение фазы А", "VoltagePhaseA", "voltagePhaseA", "voltage_phase_a", "Voltage_Phase_A"
    );
    private static final List<String> MAINS_VOLTAGE_B_CODES = List.of(
            "Напряжение фазы B", "Напряжение фазы В", "VoltagePhaseB", "voltagePhaseB", "voltage_phase_b", "Voltage_Phase_B"
    );
    private static final List<String> MAINS_VOLTAGE_C_CODES = List.of(
            "Напряжение фазы С", "VoltagePhaseC", "voltagePhaseC", "voltage_phase_c", "Voltage_Phase_C"
    );
    private static final List<String> PRIMARY_COIL_TEMP_CODES = List.of(
            "Температура первичной обмотки", "PrimaryCoilTemperature", "primaryCoilTemperature"
    );
    private static final List<String> SECONDARY_COIL_TEMP_CODES = List.of(
            "Температура вторичной обмотки", "SecondaryCoilTemperature", "secondaryCoilTemperature"
    );
    private static final List<String> CHILLER_TEMP_IN_CODES = List.of(
            "Температура охлаждающей жидкости на входе", "ChillerTemperature1", "chillerTemperature1"
    );
    private static final List<String> CHILLER_TEMP_OUT_CODES = List.of(
            "Температура охлаждающей жидкости на выходе", "ChillerTemperature2", "chillerTemperature2"
    );
    private static final List<String> PARAMETER_CODES_FOR_HISTORY = List.of(
            "Current", "Voltage",
            "State.GasFlow", "GasFlow", "gasFlow",
            "Напряжение фазы А", "VoltagePhaseA", "voltagePhaseA", "voltage_phase_a", "Voltage_Phase_A",
            "Напряжение фазы B", "Напряжение фазы В", "VoltagePhaseB", "voltagePhaseB", "voltage_phase_b", "Voltage_Phase_B",
            "Напряжение фазы С", "VoltagePhaseC", "voltagePhaseC", "voltage_phase_c", "Voltage_Phase_C",
            "Температура первичной обмотки", "PrimaryCoilTemperature", "primaryCoilTemperature",
            "Температура вторичной обмотки", "SecondaryCoilTemperature", "secondaryCoilTemperature",
            "Температура охлаждающей жидкости на входе", "ChillerTemperature1", "chillerTemperature1",
            "Температура охлаждающей жидкости на выходе", "ChillerTemperature2", "chillerTemperature2",
            "RFID.Hex", "RFID", "Rfid", "rfid",
            "Состояние аппарата", "WeldingMachineState", "State.WeldingMachineState"
    );

    public List<TelemetryHistoryPointDTO> getTelemetryHistory(String mac, long fromMs, long toMs) {
        if (mac == null || mac.isBlank()) return List.of();
        Optional<WeldingMachine> machineOpt = weldingMachineRepository.findByMac(mac);
        if (machineOpt.isEmpty()) return List.of();
        Integer machineId = machineOpt.get().getId();

        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime from = LocalDateTime.ofInstant(Instant.ofEpochMilli(fromMs), zone);
        LocalDateTime to = LocalDateTime.ofInstant(Instant.ofEpochMilli(toMs), zone);

        List<WeldingMachineState> states = weldingMachineStateRepository.findByWeldingMachineIdAndDateRangeAsc(machineId, from, to);
        if (states.isEmpty()) return List.of();

        List<Long> stateIds = states.stream().map(WeldingMachineState::getId).collect(Collectors.toList());
        Map<Long, Map<String, String>> byState = loadParameterValuesBatched(stateIds);

        List<TelemetryHistoryPointDTO> result = new ArrayList<>(states.size());
        for (WeldingMachineState s : states) {
            long ts = s.getDateCreated() != null ? s.getDateCreated().atZone(zone).toInstant().toEpochMilli() : 0L;
            Map<String, String> map = byState.getOrDefault(s.getId(), Map.of());
            String machineStateText = MonitorActivityClassifier.pickMachineStateText(map);
            BigDecimal currentAmps = MonitorActivityClassifier.pickCurrentAmps(map);
            BigDecimal voltageVolts = MonitorActivityClassifier.pickVoltageVolts(map);
            BigDecimal gasFlow = MonitorActivityClassifier.pickGasFlowLpm(map);
            boolean welding = MonitorActivityClassifier.isWelding(s, machineStateText, currentAmps);
            Double current = welding ? toDouble(currentAmps) : null;
            Double voltage = welding ? toDouble(voltageVolts) : null;
            Double setCurrent = welding ? null : toDouble(currentAmps);
            Double setVoltage = welding ? null : toDouble(voltageVolts);
            Double gasFlowLpm = toDouble(gasFlow);
            Double mainsVoltageA = parseFirstDouble(map, MAINS_VOLTAGE_A_CODES);
            Double mainsVoltageB = parseFirstDouble(map, MAINS_VOLTAGE_B_CODES);
            Double mainsVoltageC = parseFirstDouble(map, MAINS_VOLTAGE_C_CODES);
            Double primaryCoilTemperature = parseFirstDouble(map, PRIMARY_COIL_TEMP_CODES);
            Double secondaryCoilTemperature = parseFirstDouble(map, SECONDARY_COIL_TEMP_CODES);
            Double chillerTemperatureIn = parseFirstDouble(map, CHILLER_TEMP_IN_CODES);
            Double chillerTemperatureOut = parseFirstDouble(map, CHILLER_TEMP_OUT_CODES);
            String rfid = (s.getRfid() != null && !s.getRfid().isBlank()) ? s.getRfid() : parseFirstString(map, List.of("RFID.Hex", "RFID", "Rfid", "rfid"));
            String status = s.getWeldingMachineStatus() != null ? s.getWeldingMachineStatus().name() : null;
            result.add(new TelemetryHistoryPointDTO(
                    ts,
                    current,
                    voltage,
                    setCurrent,
                    setVoltage,
                    gasFlowLpm,
                    mainsVoltageA,
                    mainsVoltageB,
                    mainsVoltageC,
                    primaryCoilTemperature,
                    secondaryCoilTemperature,
                    chillerTemperatureIn,
                    chillerTemperatureOut,
                    rfid,
                    s.getErrorCode(),
                    status,
                    machineStateText,
                    welding
            ));
        }
        return result;
    }

    private Map<Long, Map<String, String>> loadParameterValuesBatched(List<Long> stateIds) {
        Map<Long, Map<String, String>> byState = new HashMap<>();
        if (stateIds == null || stateIds.isEmpty()) {
            return byState;
        }
        for (int from = 0; from < stateIds.size(); from += PARAMETER_QUERY_BATCH_SIZE) {
            int to = Math.min(from + PARAMETER_QUERY_BATCH_SIZE, stateIds.size());
            List<Long> batch = stateIds.subList(from, to);
            List<WeldingMachineParameterValue> values =
                    weldingMachineParameterValueRepository.findByStateIdsAndPropertyCodes(batch, PARAMETER_CODES_FOR_HISTORY);
            for (WeldingMachineParameterValue v : values) {
                byState.computeIfAbsent(v.getWeldingMachineStateId(), k -> new HashMap<>())
                        .put(v.getPropertyCode(), v.getValue() != null ? v.getValue() : v.getRawValue());
            }
        }
        return byState;
    }

    private static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private static Double parseFirstDouble(Map<String, String> map, List<String> keys) {
        for (String k : keys) {
            String v = map.get(k);
            if (v == null) continue;
            try {
                return Double.parseDouble(v.trim().replace(',', '.'));
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private static String parseFirstString(Map<String, String> map, List<String> keys) {
        for (String k : keys) {
            String v = map.get(k);
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}

