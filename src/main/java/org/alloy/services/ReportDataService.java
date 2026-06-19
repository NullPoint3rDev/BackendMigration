package org.alloy.services;

import org.alloy.models.dto.*;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.models.entities.Employee;
import org.alloy.models.entities.WeldingMachineState;
import org.alloy.models.entities.WeldingMachineParameterValue;
import org.alloy.models.entities.Welder;
import org.alloy.repositories.WeldingMachineRepository;
import org.alloy.repositories.EmployeeRepository;
import org.alloy.repositories.WeldingMachineStateRepository;
import org.alloy.repositories.WeldingMachineParameterValueRepository;
import org.alloy.repositories.WelderRepository;
import org.alloy.repositories.RfidPassRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.RfidPass;
import org.alloy.models.EquipmentErrorMessages;
import org.alloy.models.WeldingMachineStatus;
import org.alloy.services.report.ReportGenerationProgressContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportDataService {

    @Autowired
    private WeldingReportCalculationService calculationService;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private WeldingMachineStateRepository weldingMachineStateRepository;

    @Autowired
    private WeldingMachineParameterValueRepository parameterValueRepository;

    @Autowired
    private WelderRepository welderRepository;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private RfidPassRepository rfidPassRepository;

    @Autowired
    private WeldingMachineWeldSegmentCacheService weldSegmentCacheService;

    /**
     * Погонная масса проволоки (кг/м) для перевода скорости подачи (м/мин) в расход массы в отчётах.
     * По умолчанию — сталь Ø1.2 мм (~0.0089). Переопределяется в {@code report.wire.linear-density-kg-per-meter}.
     */
    @Value("${report.wire.linear-density-kg-per-meter:0.000089}")
    private BigDecimal wireLinearDensityKgPerMeter;

    /** Мгновенный расход газа (л/мин), не накопленный счётчик. */
    private static final List<String> GAS_FLOW_PROPERTY_CODES = List.of(
            "State.GasFlow", "GasFlow", "gasFlow");

    public List<WireConsumptionReportDTO> getWireConsumptionData(ReportRequestDTO request) {
        // Моковые данные для демонстрации (старый формат для обратной совместимости)
        List<WireConsumptionReportDTO> data = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            WireConsumptionReportDTO item = new WireConsumptionReportDTO();
            item.setWeldingMachineId(1 + i % 3);
            item.setWeldingMachineName("Аппарат " + (1 + i % 3));
            item.setWelderId(1 + i % 2);
            item.setWelderName("Сварщик " + (1 + i % 2));
            item.setTabNumber("0" + (458 + i * 100));
            item.setProfession("Электросварщик");
            item.setOrganizationUnitName("Цех " + (1 + i % 2));
            item.setTimeInNetwork(Duration.ofHours(3).plusMinutes(4).plusSeconds(52));
            item.setArcBurningTime(Duration.ofHours(22).plusMinutes(5).plusSeconds(36));
            item.setEquipmentEfficiency(BigDecimal.valueOf(29.33 + i * 5));
            item.setTimeOutsideSetCurrentRange(Duration.ofHours(12).plusMinutes(35));
            item.setTimeOutsideActualCurrentRange(Duration.ofHours(1).plusMinutes(5));
            item.setEnergyConsumed(BigDecimal.valueOf(74.3 + i * 20));
            item.setWire("1.2 Св08Г2С");
            item.setWireConsumption(BigDecimal.valueOf(159.18 + i * 50));
            data.add(item);
        }

        return data;
    }

    public List<WelderReportDTO> getWelderReportData(ReportRequestDTO request) {
        List<WelderReportDTO> data = new ArrayList<>();

        try {
            // Если указан конкретный аппарат, используем реальные данные
            if (request.getWeldingMachineId() != null) {
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(request.getWeldingMachineId());
                if (machineOpt.isPresent()) {
                    WeldingMachine machine = machineOpt.get();

                    // Используем переданные даты и время
                    LocalDateTime startDate = request.getDateFrom() != null ?
                            request.getDateFrom() :
                            LocalDateTime.now().minusDays(30);
                    LocalDateTime endDate = request.getDateTo() != null ?
                            request.getDateTo() :
                            LocalDateTime.now();

                    // Рассчитываем средние значения для блока мониторинга ОГК
                    if ("8CAAB50C4254".equals(machine.getMac())) {
                        WeldingReportCalculationService.AverageValues averages =
                                calculationService.calculateAverageValues(machine.getMac(), startDate, endDate);

                        WelderReportDTO item = new WelderReportDTO();
                        item.setWelderId(1);
                        item.setWelderName("Оператор блока мониторинга");
                        item.setWelderEmail("operator@company.com");
                        item.setDate(request.getDateFrom() != null ? request.getDateFrom().toLocalDate() : LocalDate.now());
                        item.setTotalWireConsumption(BigDecimal.ZERO); // Не применимо для блока мониторинга
                        item.setTotalWeldingTime(BigDecimal.ZERO); // Не применимо для блока мониторинга
                        item.setTotalWeldingSessions(0); // Не применимо для блока мониторинга
                        item.setAverageCurrent(averages.getAverageCurrent());
                        item.setAverageVoltage(averages.getAverageVoltage());
                        item.setAverageWireFeedRate(BigDecimal.ZERO); // Не применимо для блока мониторинга
                        item.setOrganizationUnitName(machine.getOrganizationUnit() != null ?
                                machine.getOrganizationUnit().getName() : "Не указано");
                        item.setWeldingMachineName(machine.getName());
                        data.add(item);

                        return data;
                    }
                }
            }

            // Для других аппаратов используем моковые данные (как было)
            LocalDate baseDate = LocalDate.now();

            for (int i = 0; i < 5; i++) {
                WelderReportDTO item = new WelderReportDTO();
                item.setWelderId(1 + i);
                item.setWelderName("Сварщик " + (1 + i));
                item.setWelderEmail("welder" + (1 + i) + "@company.com");
                item.setDate(baseDate.minusDays(i * 7));
                item.setTotalWireConsumption(BigDecimal.valueOf(15.5 + i * 2.3));
                item.setTotalWeldingTime(BigDecimal.valueOf(180 + i * 30));
                item.setTotalWeldingSessions(8 + i * 2);
                item.setAverageCurrent(BigDecimal.valueOf(185 + i * 5));
                item.setAverageVoltage(BigDecimal.valueOf(23.5 + i * 0.3));
                item.setAverageWireFeedRate(BigDecimal.valueOf(5.2 + i * 0.1));
                item.setOrganizationUnitName("Цех " + (1 + i % 2));
                item.setWeldingMachineName("Аппарат " + (1 + i % 3));
                data.add(item);
            }

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка получения данных отчета: " + e.getMessage());
            e.printStackTrace();
            // Возвращаем пустой список в случае ошибки
        }

        return data;
    }

    public List<WorkReportDTO> getWorkReportData(ReportRequestDTO request) {
        List<WorkReportDTO> data = new ArrayList<>();


        try {
            // Если указан конкретный аппарат, используем реальные данные
            if (request.getWeldingMachineId() != null) {
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(request.getWeldingMachineId());
                if (machineOpt.isPresent()) {
                    WeldingMachine machine = machineOpt.get();

                    // Определяем период на основе параметра period
                    LocalDateTime startDate, endDate;
                    if ("DAY".equals(request.getPeriod())) {
                        // За день - используем сегодняшний день
                        LocalDate today = LocalDate.now();
                        startDate = today.atStartOfDay();
                        endDate = today.atTime(23, 59, 59);
                    } else {
                        // Используем переданные даты и время или по умолчанию
                        startDate = request.getDateFrom() != null ?
                                request.getDateFrom() :
                                LocalDateTime.now().minusDays(1);
                        endDate = request.getDateTo() != null ?
                                request.getDateTo() :
                                LocalDateTime.now();
                    }

                    // Рассчитываем средние значения для блока мониторинга ОГК
                    if ("8CAAB50C4254".equals(machine.getMac())) {
                        // Используем расчет по дням для более детального отчета
                        List<WeldingReportCalculationService.DailyAverageValues> dailyData =
                                calculationService.calculateDailyAverageValues(machine.getMac(), startDate, endDate);

                        // Конвертируем данные по дням в WorkReportDTO
                        for (WeldingReportCalculationService.DailyAverageValues dayData : dailyData) {
                            WorkReportDTO item = new WorkReportDTO();
                            item.setWeldingMachineId(dayData.getWeldingMachineId());
                            item.setWeldingMachineName(dayData.getWeldingMachineName());
                            item.setWeldingMachineSerialNumber(machine.getSerialNumber() != null ? machine.getSerialNumber() : "N/A");
                            item.setWelderId(1);
                            item.setWelderName("Оператор блока мониторинга");
                            item.setStartTime(dayData.getStartTime());
                            item.setEndTime(dayData.getEndTime());
                            item.setWeldingTime(dayData.getWeldingTimeSeconds()); // Время сварки в секундах
                            item.setCurrent(dayData.getAverageCurrent());
                            item.setVoltage(dayData.getAverageVoltage());
                            item.setWeldingMode("Мониторинг");
                            item.setWeldingType("Блок мониторинга");
                            item.setWireConsumption(BigDecimal.ZERO); // Не применимо для блока мониторинга
                            item.setWireFeedRate(BigDecimal.ZERO); // Не применимо для блока мониторинга
                            item.setOrganizationUnitName(dayData.getOrganizationUnitName());
                            item.setNotes("Данные за " + dayData.getDate() +
                                    " (Ток: " + dayData.getAverageCurrent() + "А, Напряжение: " + dayData.getAverageVoltage() + "В, Время сварки: " + dayData.getWeldingTimeSeconds() + "с)");
                            data.add(item);
                        }

                        return data;
                    } else {
                    }
                } else {
                }
            } else {
            }

            // Для других аппаратов используем моковые данные (как было)

            // Определяем базовую дату в зависимости от периода
            LocalDateTime baseDate;
            if ("DAY".equals(request.getPeriod())) {
                // За день - используем только сегодняшний день
                baseDate = LocalDateTime.now().withHour(15).withMinute(13).withSecond(0);
            } else {
                // Для других периодов - используем разные даты
                baseDate = LocalDateTime.now();
            }

            for (int i = 0; i < 8; i++) {
                WorkReportDTO item = new WorkReportDTO();
                item.setWeldingMachineId(1 + i % 3);
                item.setWeldingMachineName("Аппарат " + (1 + i % 3));
                item.setWeldingMachineSerialNumber("SN" + (1000 + i));
                item.setWelderId(1 + i % 2);
                item.setWelderName("Сварщик " + (1 + i % 2));

                if ("DAY".equals(request.getPeriod())) {
                    // За день - все записи в один день, но в разное время
                    item.setStartTime(baseDate.plusMinutes(i * 30));
                    item.setEndTime(baseDate.plusMinutes(i * 30).plusMinutes(45 + i * 5));
                } else {
                    // Для других периодов - разные даты
                    item.setStartTime(baseDate.minusHours(i * 2));
                    item.setEndTime(baseDate.minusHours(i * 2).plusMinutes(45 + i * 5));
                }

                item.setWeldingTime(BigDecimal.valueOf(45 + i * 5));
                item.setCurrent(BigDecimal.valueOf(180 + i * 8));
                item.setVoltage(BigDecimal.valueOf(22 + i * 0.4));
                item.setWeldingMode("Ручной");
                item.setWeldingType("MIG/MAG");
                item.setWireConsumption(BigDecimal.valueOf(3.2 + i * 0.4));
                item.setWireFeedRate(BigDecimal.valueOf(5.1 + i * 0.2));
                item.setOrganizationUnitName("Цех " + (1 + i % 2));
                item.setNotes("Сессия " + (i + 1));
                data.add(item);
            }

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка получения данных отчета по работе оборудования: " + e.getMessage());
            e.printStackTrace();
            // Возвращаем пустой список в случае ошибки
        }

        return data;
    }

    /**
     * Возвращает данные для отчета по сварочным швам. На первом этапе используем ту же структуру, что и WorkReportDTO,
     * чтобы отобразить реальные измеренные значения (ток, напряжение, время сварки) и выбранное оборудование.
     */
    public List<WeldSegmentDTO> getWeldsReportData(ReportRequestDTO request) {
        List<WeldSegmentDTO> data = new ArrayList<>();


        try {
            if (request.getWeldingMachineId() != null) {
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(request.getWeldingMachineId());
                if (machineOpt.isPresent()) {
                    WeldingMachine machine = machineOpt.get();

                    // Определяем границы периода
                    LocalDateTime startDate = request.getDateFrom() != null ? request.getDateFrom() : LocalDateTime.now().minusDays(1);
                    LocalDateTime endDate = request.getDateTo() != null ? request.getDateTo() : LocalDateTime.now();

                    // Для блока мониторинга ОГК считаем по дням реальные значения
                    if ("8CAAB50C4254".equals(machine.getMac())) {
                        // Для швов нам нужны сегменты с усреднениями и длительностями
                        List<WeldSegmentDTO> segments = calculationService.calculateWeldSegments(machine.getId(), startDate, endDate);
                        data.addAll(segments);
                        return data;
                    }
                }
            }

            // Для остальных аппаратов также считаем реальные сегменты по данным БД
            if (request.getWeldingMachineId() != null) {
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(request.getWeldingMachineId());
                if (machineOpt.isPresent()) {
                    LocalDateTime startDate = request.getDateFrom() != null ? request.getDateFrom() : LocalDateTime.now().minusDays(1);
                    LocalDateTime endDate = request.getDateTo() != null ? request.getDateTo() : LocalDateTime.now();
                    List<WeldSegmentDTO> segments = calculationService.calculateWeldSegments(machineOpt.get().getId(), startDate, endDate);
                    data.addAll(segments);
                    return data;
                }
            }

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка получения данных отчета по швам: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Получает данные для отчета по расходу проволоки с новой структурой
     * Группирует по сварщикам, сортирует по суммарным значениям
     */
    public List<WireConsumptionReportDTO> getWireConsumptionDataNew(
            WireConsumptionReportTemplateDTO template,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            LocalTime periodStartTime,
            LocalTime periodEndTime) {

        List<WireConsumptionReportDTO> result = new ArrayList<>();

        try {
            LocalDateTime startDateTime = LocalDateTime.of(periodStartDate, periodStartTime != null ? periodStartTime : LocalTime.MIN);
            LocalDateTime endDateTime = LocalDateTime.of(periodEndDate, periodEndTime != null ? periodEndTime : LocalTime.MAX);

            // Получаем список сварщиков на основе шаблона
            Set<Integer> welderIds = getSelectedWelderIds(template);

            // Получаем данные по каждому сварщику
            Map<Integer, List<WireConsumptionReportDTO>> welderDataMap = new HashMap<>();

            for (Integer welderId : welderIds) {
                List<WireConsumptionReportDTO> welderData = getWireConsumptionDataForWelder(
                        welderId, startDateTime, endDateTime, template);

                // Если данных нет, создаем записи с нулевыми значениями для выбранных аппаратов
                if (welderData.isEmpty()) {
                    List<WireConsumptionReportDTO> emptyRecords = createEmptyRecordsForWelder(welderId, template);
                    if (!emptyRecords.isEmpty()) {
                        welderData.addAll(emptyRecords);
                    }
                }

                if (!welderData.isEmpty()) {
                    welderDataMap.put(welderId, welderData);
                }
            }

            // Если сварщиков нет, но выбраны аппараты, создаем записи для аппаратов без сварщика
            if (welderIds.isEmpty() && template.getSelectedEquipmentModels() != null && !template.getSelectedEquipmentModels().isEmpty()) {
                List<WireConsumptionReportDTO> equipmentOnlyData = createRecordsForEquipmentWithoutWelder(
                        startDateTime, endDateTime, template);
                if (!equipmentOnlyData.isEmpty()) {
                    // Используем специальный ключ для записей без сварщика
                    welderDataMap.put(-1, equipmentOnlyData);
                }
            }

            // Группируем и сортируем данные
            result = groupAndSortWireConsumptionData(welderDataMap, template);

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка получения данных отчета по расходу проволоки: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Получает данные для отчёта "По работе сварщика" (швы).
     * Выборка по сварщику и периоду с учётом мин. интервала между швами, мин. длительности шва,
     * диапазона фактического тока (подсветка вне диапазона).
     * Шов = длительность сварки (состояния с током > 3А); при интервале между швами < minInterval — объединяем в один.
     */
    public List<WelderWorkReportDTO> getWelderWorkDataNew(
            WelderWorkReportTemplateDTO template,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            LocalTime periodStartTime,
            LocalTime periodEndTime) {
        List<WelderWorkReportDTO> result = new ArrayList<>();
        if (template == null) return result;

        LocalDateTime startDateTime = LocalDateTime.of(periodStartDate, periodStartTime != null ? periodStartTime : LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(periodEndDate, periodEndTime != null ? periodEndTime : LocalTime.MAX);
        int minIntervalSec = template.getMinIntervalBetweenWeldsSec() != null ? template.getMinIntervalBetweenWeldsSec() : 0;
        int minDurationSec = template.getMinWeldDurationSec() != null ? template.getMinWeldDurationSec() : 0;
        Integer actualMin = template.getActualCurrentMin();
        Integer actualMax = template.getActualCurrentMax();

        List<Integer> welderIds = new ArrayList<>();
        if (template.getSelectedWelderIds() != null && !template.getSelectedWelderIds().isEmpty()) {
            welderIds.addAll(template.getSelectedWelderIds());
        } else if (template.getWelderId() != null) {
            welderIds.add(template.getWelderId().intValue());
        }
        if (welderIds.isEmpty()) return result;

        try {
            Map<Integer, List<WeldingMachineState>> statesByMachineId = new HashMap<>();
            List<WeldRow> rawRows = new ArrayList<>();
            for (Integer welderId : welderIds) {
                List<String> rfidCodes = getRfidCodesForWelder(welderId);
                Set<Integer> machineIds = new HashSet<>();
                if (!rfidCodes.isEmpty()) {
                    for (String rfid : rfidCodes) {
                        List<WeldingMachineState> states = weldingMachineStateRepository.findByRfidAndDateRange(rfid, startDateTime, endDateTime);
                        for (WeldingMachineState s : states) {
                            machineIds.add(s.getWeldingMachineId());
                        }
                    }
                }
                if (machineIds.isEmpty()) continue;

                for (Integer machineId : machineIds) {
                    Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(machineId);
                    String machineName = machineOpt.map(WeldingMachine::getName).orElse("");
                    String equipmentModel = machineOpt.map(m -> m.getDeviceModel() != null ? m.getDeviceModel().name() : "").orElse("");
                    List<WeldingMachineState> machineStates = statesByMachineId.computeIfAbsent(machineId, mid -> {
                        List<WeldingMachineState> list = weldingMachineStateRepository
                                .findByWeldingMachineIdAndDateRange(mid, startDateTime, endDateTime);
                        list.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
                        return list;
                    });
                    List<org.alloy.models.dto.WeldSegmentDTO> segments =
                            resolveWeldSegmentsForReport(machineId, startDateTime, endDateTime, machineStates);
                    for (org.alloy.models.dto.WeldSegmentDTO seg : segments) {
                        if (seg.getStartTime() == null || seg.getDurationSeconds() == null) continue;
                        long durSec = seg.getDurationSeconds().longValue();
                        if (durSec < minDurationSec) continue;
                        WeldRow row = new WeldRow();
                        row.welderId = welderId.longValue();
                        row.machineId = machineId;
                        row.machineName = machineName;
                        row.equipmentModel = equipmentModel;
                        row.startTime = seg.getStartTime();
                        row.durationSec = seg.getDurationSeconds();
                        row.avgCurrent = seg.getAverageCurrent() != null ? seg.getAverageCurrent() : BigDecimal.ZERO;
                        row.avgVoltage = seg.getAverageVoltage() != null ? seg.getAverageVoltage() : BigDecimal.ZERO;
                        rawRows.add(row);
                    }
                }
            }

            rawRows.sort(Comparator.comparing(r -> r.startTime));

            List<WeldRow> merged = new ArrayList<>();
            for (WeldRow row : rawRows) {
                if (merged.isEmpty()) {
                    merged.add(copyWeldRow(row));
                    continue;
                }
                WeldRow last = merged.get(merged.size() - 1);
                long lastEndSec = last.startTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() + last.durationSec.longValue();
                long gapSec = row.startTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() - lastEndSec;
                if (gapSec <= minIntervalSec && last.welderId == row.welderId) {
                    long newDur = last.durationSec.longValue() + row.durationSec.longValue();
                    BigDecimal w1 = last.durationSec;
                    BigDecimal w2 = row.durationSec;
                    BigDecimal avgI = w1.add(w2).compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : last.avgCurrent.multiply(w1).add(row.avgCurrent.multiply(w2)).divide(w1.add(w2), 1, RoundingMode.HALF_UP);
                    BigDecimal avgU = w1.add(w2).compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : last.avgVoltage.multiply(w1).add(row.avgVoltage.multiply(w2)).divide(w1.add(w2), 1, RoundingMode.HALF_UP);
                    last.durationSec = BigDecimal.valueOf(newDur);
                    last.avgCurrent = avgI;
                    last.avgVoltage = avgU;
                } else {
                    merged.add(copyWeldRow(row));
                }
            }
            boolean needSetColumns = templateNeedsSetPointColumns(template.getSelectedColumns());
            boolean needSetCurrent = template.getSelectedColumns() != null && template.getSelectedColumns().contains("setCurrent");
            boolean needSetVoltage = template.getSelectedColumns() != null && template.getSelectedColumns().contains("setVoltage");
            Map<String, Long> idleStateIdByWeldKey = new HashMap<>();
            Map<Long, Integer> setCurrentByStateId = new HashMap<>();
            Map<Long, Integer> setVoltageTenthsByStateId = new HashMap<>();
            if (needSetColumns) {
                Set<Integer> machineIdsInReport = merged.stream().map(r -> r.machineId).collect(Collectors.toSet());
                for (Integer mid : machineIdsInReport) {
                    List<LocalDateTime> weldStarts = new ArrayList<>();
                    List<BigDecimal> weldDurations = new ArrayList<>();
                    for (WeldRow row : merged) {
                        if (row.machineId != mid || row.startTime == null || row.durationSec == null) continue;
                        weldStarts.add(row.startTime);
                        weldDurations.add(row.durationSec);
                    }
                    statesByMachineId.put(mid, calculationService.loadStatesAroundWeldRows(mid, weldStarts, weldDurations));
                }
                List<Long> idleStateIds = new ArrayList<>();
                for (WeldRow r : merged) {
                    WeldingMachineState idle = findLastIdleStateBefore(statesByMachineId.get(r.machineId), r.startTime);
                    if (idle != null) {
                        idleStateIdByWeldKey.put(weldRowKey(r), idle.getId());
                        idleStateIds.add(idle.getId());
                    }
                }
                if (needSetCurrent) loadSetCurrentByStateIds(idleStateIds, setCurrentByStateId);
                if (needSetVoltage) loadSetVoltageTenthsByStateIds(idleStateIds, setVoltageTenthsByStateId);
            }
            List<Long> allStateIds = collectStateIdsForWelderMerged(statesByMachineId, merged);
            Map<Long, String> workModeByStateId = new HashMap<>();
            if (!allStateIds.isEmpty()) {
                for (String code : new String[] { "Метод сварки", "WeldingMachineState", "Режим работы", "State.Mode", "State.Ctrl" }) {
                    List<WeldingMachineParameterValue> workModeVals = getParameterValuesInBatches(allStateIds, code);
                    for (WeldingMachineParameterValue pv : workModeVals) {
                        if (pv.getValue() != null && !pv.getValue().trim().isEmpty())
                            workModeByStateId.putIfAbsent(pv.getWeldingMachineStateId(), pv.getValue().trim());
                    }
                    if (!workModeByStateId.isEmpty()) break;
                }
            }
            Map<Long, BigDecimal> wireFeedByStateId = new HashMap<>();
            loadWireFeedByStateId(allStateIds, wireFeedByStateId);
            Map<Long, BigDecimal> gasFlowByStateId = new HashMap<>();
            loadGasFlowLpmByStateId(allStateIds, gasFlowByStateId);
            // Дата и время из посылки аппарата (CORE: Date.*, Time.*) — чтобы в отчёте было «когда аппарат сказал, что идёт сварка»
            Map<Long, LocalDateTime> deviceDateTimeByStateId = buildDeviceDateTimeByStateId(allStateIds);

            for (WeldRow r : merged) {
                if (r.durationSec.longValue() < minDurationSec) continue;
                int currentInt = r.avgCurrent.intValue();
                if (currentInt < 1) currentInt = 0;
                // Подсветка «вне диапазона» только если в шаблоне явно включены пределы факт. тока (как в шапке Excel).
                boolean outOfRange = Boolean.TRUE.equals(template.getIncludeActualCurrentRange())
                        && actualMin != null && actualMax != null
                        && (currentInt < actualMin || currentInt > actualMax);

                LocalDateTime segmentEnd = r.startTime.plusSeconds(r.durationSec.longValue());
                List<WeldingMachineState> segmentStates = statesInTimeWindow(
                        statesByMachineId.get(r.machineId), r.startTime.minusSeconds(5), segmentEnd.plusSeconds(5));
                LocalDateTime displayDateTime = getDeviceDateTimeFromCache(segmentStates, r.startTime, segmentEnd, deviceDateTimeByStateId);
                if (displayDateTime == null) displayDateTime = r.startTime;
                String workMode = getWorkModeFromCache(segmentStates, r.startTime, segmentEnd, workModeByStateId);
                BigDecimal wireFeedMpm = getWireFeedFromCache(segmentStates, r.startTime, segmentEnd, wireFeedByStateId);
                BigDecimal wireKg = calculateWireConsumptionKgForWeldSegment(segmentStates, r.startTime, segmentEnd, wireFeedByStateId);
                BigDecimal gasL = calculateGasConsumptionLForWeldSegment(segmentStates, r.startTime, segmentEnd, gasFlowByStateId);
                BigDecimal energyKwh = calculateEnergyPerWeld(r.avgVoltage, r.avgCurrent, r.durationSec);

                WelderWorkReportDTO dto = new WelderWorkReportDTO();
                dto.setDate(displayDateTime.toLocalDate());
                dto.setWeldStartTime(displayDateTime.toLocalTime());
                dto.setEquipmentModel(r.equipmentModel);
                dto.setEquipmentName(r.machineName);
                dto.setWorkMode(workMode != null ? workMode : "");
                dto.setWireFeedSpeedMpm(wireFeedMpm != null ? wireFeedMpm : BigDecimal.ZERO);
                dto.setWireConsumptionKg(wireKg != null ? wireKg : BigDecimal.ZERO);
                dto.setGasConsumptionL(gasL != null ? gasL : BigDecimal.ZERO);
                dto.setCurrentAmps(r.avgCurrent.setScale(1, RoundingMode.HALF_UP));
                dto.setVoltageVolts(r.avgVoltage.setScale(1, RoundingMode.HALF_UP));
                if (needSetCurrent || needSetVoltage) {
                    Long idleStateId = idleStateIdByWeldKey.get(weldRowKey(r));
                    if (needSetCurrent) {
                        dto.setSetCurrentAmps(toReportAmps(idleStateId != null ? setCurrentByStateId.get(idleStateId) : null));
                    }
                    if (needSetVoltage) {
                        dto.setSetVoltageVolts(toReportVoltsFromTenths(idleStateId != null ? setVoltageTenthsByStateId.get(idleStateId) : null));
                    }
                }
                dto.setWeldDurationSec(r.durationSec.setScale(1, RoundingMode.HALF_UP));
                dto.setEnergyConsumedKwh(energyKwh);
                dto.setCurrentOutOfRange(outOfRange);
                dto.setWelderId(r.welderId);
                dto.setWeldingMachineId(r.machineId);
                dto.setWeldingMachineName(r.machineName);
                result.add(dto);
            }

            for (int i = 0; i < result.size(); i++) {
                result.get(i).setIndex(i + 1);
            }
        } catch (Exception e) {
            System.err.println("[REPORT-DATA] Ошибка getWelderWorkDataNew: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Получает данные для отчёта "По работе оборудования" (швы).
     * Логика расчёта та же, что у отчёта по сварщику: сегменты по аппарату, объединение по мин. интервалу, мин. длительность шва.
     */
    public List<EquipmentWorkReportDTO> getEquipmentWorkDataNew(
            EquipmentWorkReportTemplateDTO template,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            LocalTime periodStartTime,
            LocalTime periodEndTime) {
        List<EquipmentWorkReportDTO> result = new ArrayList<>();
        if (template == null) return result;

        LocalDateTime startDateTime = LocalDateTime.of(periodStartDate, periodStartTime != null ? periodStartTime : LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(periodEndDate, periodEndTime != null ? periodEndTime : LocalTime.MAX);
        int minIntervalSec = template.getMinIntervalBetweenWeldsSec() != null ? template.getMinIntervalBetweenWeldsSec() : 0;
        int minDurationSec = template.getMinWeldDurationSec() != null ? template.getMinWeldDurationSec() : 0;
        Integer actualMin = template.getActualCurrentMin();
        Integer actualMax = template.getActualCurrentMax();

        List<Integer> machineIds = template.getSelectedEquipmentIds() != null ? new ArrayList<>(template.getSelectedEquipmentIds()) : new ArrayList<>();
        if (machineIds.isEmpty()) return result;

        try {
            Map<Integer, List<WeldingMachineState>> statesByMachineId = new HashMap<>();
            Map<String, Optional<RfidPass>> rfidPassCache = new HashMap<>();
            List<EquipmentWeldRow> rawRows = new ArrayList<>();
            for (Integer machineId : machineIds) {
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(machineId);
                String machineName = machineOpt.map(WeldingMachine::getName).orElse("");
                String equipmentModel = machineOpt.map(m -> m.getDeviceModel() != null ? m.getDeviceModel().name() : "").orElse("");
                Optional<List<WeldSegmentDTO>> cachedSegments = weldSegmentCacheService.findSegmentsForReportIfReady(
                        machineId, startDateTime, endDateTime);
                List<org.alloy.models.dto.WeldSegmentDTO> segments;
                if (cachedSegments.isPresent()) {
                    segments = cachedSegments.get();
                } else {
                    List<WeldingMachineState> machineStates = calculationService.loadStatesForReport(
                            machineId, startDateTime, endDateTime);
                    machineStates.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
                    statesByMachineId.put(machineId, machineStates);
                    segments = calculationService.calculateWeldSegmentsFromStates(machineStates);
                }
                for (org.alloy.models.dto.WeldSegmentDTO seg : segments) {
                    if (seg.getStartTime() == null || seg.getDurationSeconds() == null) continue;
                    long durSec = seg.getDurationSeconds().longValue();
                    if (durSec < minDurationSec) continue;
                    EquipmentWeldRow row = new EquipmentWeldRow();
                    row.machineId = machineId;
                    row.machineName = machineName;
                    row.equipmentModel = equipmentModel;
                    row.startTime = seg.getStartTime();
                    row.durationSec = seg.getDurationSeconds();
                    row.avgCurrent = seg.getAverageCurrent() != null ? seg.getAverageCurrent() : BigDecimal.ZERO;
                    row.avgVoltage = seg.getAverageVoltage() != null ? seg.getAverageVoltage() : BigDecimal.ZERO;
                    rawRows.add(row);
                }
            }

            rawRows.sort(Comparator.comparing(r -> r.startTime));

            List<EquipmentWeldRow> merged = new ArrayList<>();
            for (EquipmentWeldRow row : rawRows) {
                if (merged.isEmpty()) {
                    merged.add(copyEquipmentWeldRow(row));
                    continue;
                }
                EquipmentWeldRow last = merged.get(merged.size() - 1);
                long lastEndSec = last.startTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() + last.durationSec.longValue();
                long gapSec = row.startTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() - lastEndSec;
                if (gapSec <= minIntervalSec && last.machineId == row.machineId) {
                    long newDur = last.durationSec.longValue() + row.durationSec.longValue();
                    BigDecimal w1 = last.durationSec;
                    BigDecimal w2 = row.durationSec;
                    BigDecimal avgI = w1.add(w2).compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : last.avgCurrent.multiply(w1).add(row.avgCurrent.multiply(w2)).divide(w1.add(w2), 1, RoundingMode.HALF_UP);
                    BigDecimal avgU = w1.add(w2).compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : last.avgVoltage.multiply(w1).add(row.avgVoltage.multiply(w2)).divide(w1.add(w2), 1, RoundingMode.HALF_UP);
                    last.durationSec = BigDecimal.valueOf(newDur);
                    last.avgCurrent = avgI;
                    last.avgVoltage = avgU;
                } else {
                    merged.add(copyEquipmentWeldRow(row));
                }
            }

            Set<Integer> machineIdsInReport = merged.stream().map(r -> r.machineId).collect(Collectors.toSet());
            for (Integer mid : machineIdsInReport) {
                if (statesByMachineId.containsKey(mid)) {
                    continue;
                }
                List<LocalDateTime> weldStarts = new ArrayList<>();
                List<BigDecimal> weldDurations = new ArrayList<>();
                for (EquipmentWeldRow row : merged) {
                    if (row.machineId != mid || row.startTime == null || row.durationSec == null) continue;
                    weldStarts.add(row.startTime);
                    weldDurations.add(row.durationSec);
                }
                statesByMachineId.put(mid, calculationService.loadStatesAroundWeldRows(mid, weldStarts, weldDurations));
            }
            boolean needSetColumns = templateNeedsSetPointColumns(template.getSelectedColumns());
            boolean needSetCurrent = template.getSelectedColumns() != null && template.getSelectedColumns().contains("setCurrent");
            boolean needSetVoltage = template.getSelectedColumns() != null && template.getSelectedColumns().contains("setVoltage");
            Map<String, Long> idleStateIdByEquipmentWeldKey = new HashMap<>();
            Map<Long, Integer> setCurrentByStateId = new HashMap<>();
            Map<Long, Integer> setVoltageTenthsByStateId = new HashMap<>();
            if (needSetColumns) {
                for (Integer mid : machineIdsInReport) {
                    List<LocalDateTime> weldStarts = new ArrayList<>();
                    List<BigDecimal> weldDurations = new ArrayList<>();
                    for (EquipmentWeldRow row : merged) {
                        if (row.machineId != mid || row.startTime == null || row.durationSec == null) continue;
                        weldStarts.add(row.startTime);
                        weldDurations.add(row.durationSec);
                    }
                    statesByMachineId.put(mid, calculationService.loadStatesAroundWeldRows(mid, weldStarts, weldDurations));
                }
                List<Long> idleStateIds = new ArrayList<>();
                for (EquipmentWeldRow r : merged) {
                    WeldingMachineState idle = findLastIdleStateBefore(statesByMachineId.get(r.machineId), r.startTime);
                    if (idle != null) {
                        idleStateIdByEquipmentWeldKey.put(equipmentWeldRowKey(r), idle.getId());
                        idleStateIds.add(idle.getId());
                    }
                }
                if (needSetCurrent) loadSetCurrentByStateIds(idleStateIds, setCurrentByStateId);
                if (needSetVoltage) loadSetVoltageTenthsByStateIds(idleStateIds, setVoltageTenthsByStateId);
            }
            List<Long> allStateIds = collectStateIdsForEquipmentMerged(statesByMachineId, merged);
            boolean enrichByMachinePeriod = machineIds.size() == 1 && machineIdsInReport.size() == 1;
            Integer singleMachineId = enrichByMachinePeriod ? machineIds.get(0) : null;

            ReportGenerationProgressContext.update(82, "Обогащение данных швов…");
            Map<Long, String> workModeByStateId = new HashMap<>();
            Map<Long, LocalDateTime> deviceDateTimeByStateId = new HashMap<>();
            Map<Long, BigDecimal> wireFeedByStateId = new HashMap<>();
            int totalStatesLoaded = statesByMachineId.values().stream().mapToInt(List::size).sum();
            Set<Long> enrichStateIdSet = allStateIds.isEmpty() ? Collections.emptySet() : new HashSet<>(allStateIds);
            /**
             * Один аппарат + известный whitelist stateId вокруг швов: всегда батчи IN.
             * Иначе при >35k id попадали в полный JOIN за 7 дней (минуты на БД).
             */
            boolean enrichByStateIds = enrichByMachinePeriod && singleMachineId != null && !enrichStateIdSet.isEmpty();
            boolean enrichByFilteredMachinePeriod = false;
            if (enrichByStateIds) {
                loadEquipmentEnrichmentByStateIds(allStateIds, workModeByStateId, wireFeedByStateId, deviceDateTimeByStateId);
            } else if (enrichByMachinePeriod && singleMachineId != null && !enrichStateIdSet.isEmpty()
                    && enrichStateIdSet.size() < totalStatesLoaded * 0.6) {
                enrichByFilteredMachinePeriod = true;
                loadEquipmentEnrichmentByMachinePeriodFiltered(
                        singleMachineId, startDateTime, endDateTime, enrichStateIdSet,
                        workModeByStateId, wireFeedByStateId, deviceDateTimeByStateId);
            } else if (enrichByMachinePeriod && singleMachineId != null) {
                loadEquipmentEnrichmentByMachinePeriod(
                        singleMachineId, startDateTime, endDateTime,
                        workModeByStateId, wireFeedByStateId, deviceDateTimeByStateId);
            } else {
                if (!allStateIds.isEmpty()) {
                    for (String code : new String[] { "Метод сварки", "WeldingMachineState", "Режим работы", "State.Mode", "State.Ctrl" }) {
                        List<WeldingMachineParameterValue> workModeVals = getParameterValuesInBatches(allStateIds, code);
                        for (WeldingMachineParameterValue pv : workModeVals) {
                            if (pv.getValue() != null && !pv.getValue().trim().isEmpty())
                                workModeByStateId.putIfAbsent(pv.getWeldingMachineStateId(), pv.getValue().trim());
                        }
                        if (!workModeByStateId.isEmpty()) break;
                    }
                }
                deviceDateTimeByStateId = buildDeviceDateTimeByStateId(allStateIds);
                loadWireFeedByStateId(allStateIds, wireFeedByStateId);
            }
            Map<Long, BigDecimal> gasFlowByStateId = new HashMap<>();
            loadGasFlowLpmByStateId(allStateIds, gasFlowByStateId);
            boolean needWelderColumns = templateNeedsWelderColumns(template);
            Map<Long, String> rfidByStateId = needWelderColumns
                    ? (!allStateIds.isEmpty()
                    ? loadRfidByStateIdFromParamsNative(allStateIds)
                    : (enrichByMachinePeriod && singleMachineId != null
                    ? loadRfidByMachinePeriodNative(singleMachineId, startDateTime, endDateTime)
                    : Collections.emptyMap()))
                    : Collections.emptyMap();
            // Предвычисление: по машине — только состояния с RFID в параметрах (для быстрого fallback без перебора всех состояний)
            Map<Integer, List<WeldingMachineState>> statesWithRfidInParamsByMachineId = new HashMap<>();
            if (!rfidByStateId.isEmpty()) {
                for (Integer mid : machineIdsInReport) {
                    List<WeldingMachineState> withRfid = statesByMachineId.get(mid).stream()
                            .filter(s -> rfidByStateId.containsKey(s.getId()))
                            .collect(Collectors.toList());
                    statesWithRfidInParamsByMachineId.put(mid, withRfid);
                }
            }
            // Предвычисление: по машине и дате — состояния с непустым state.rfid (лёгкий запрос, без скана всех состояний)
            Map<Integer, Map<LocalDate, List<WeldingMachineState>>> statesWithRfidColumnByMachineAndDay = new HashMap<>();
            for (Integer mid : machineIdsInReport) {
                Map<LocalDate, List<WeldingMachineState>> byDay = new HashMap<>();
                List<Object[]> rfidRows = weldingMachineStateRepository.findRfidReportRowsByMachineAndDateRange(
                        mid, startDateTime, endDateTime);
                for (Object[] row : rfidRows) {
                    if (row == null || row.length < 3 || row[0] == null || row[1] == null || row[2] == null) continue;
                    WeldingMachineState s = new WeldingMachineState();
                    s.setId(((Number) row[0]).longValue());
                    if (row[1] instanceof Timestamp) {
                        s.setDateCreated(((Timestamp) row[1]).toLocalDateTime());
                    } else if (row[1] instanceof LocalDateTime) {
                        s.setDateCreated((LocalDateTime) row[1]);
                    }
                    s.setRfid(row[2].toString().trim());
                    s.setWeldingMachineId(mid);
                    if (s.getDateCreated() == null) continue;
                    byDay.computeIfAbsent(s.getDateCreated().toLocalDate(), k -> new ArrayList<>()).add(s);
                }
                statesWithRfidColumnByMachineAndDay.put(mid, byDay);
            }

            for (EquipmentWeldRow r : merged) {
                if (r.durationSec.longValue() < minDurationSec) continue;
                int currentInt = r.avgCurrent.intValue();
                if (currentInt < 1) currentInt = 0;
                boolean outOfRange = Boolean.TRUE.equals(template.getIncludeActualCurrentRange())
                        && actualMin != null && actualMax != null
                        && (currentInt < actualMin || currentInt > actualMax);

                LocalDateTime segmentEnd = r.startTime.plusSeconds(r.durationSec.longValue());
                List<WeldingMachineState> segmentStates = statesInTimeWindow(
                        statesByMachineId.get(r.machineId), r.startTime.minusSeconds(5), segmentEnd.plusSeconds(5));
                LocalDateTime displayDateTime = getDeviceDateTimeFromCache(segmentStates, r.startTime, segmentEnd, deviceDateTimeByStateId);
                if (displayDateTime == null) displayDateTime = r.startTime;
                String workMode = getWorkModeFromCache(segmentStates, r.startTime, segmentEnd, workModeByStateId);
                BigDecimal wireFeedMpm = getWireFeedFromCache(segmentStates, r.startTime, segmentEnd, wireFeedByStateId);
                BigDecimal wireKg = calculateWireConsumptionKgForWeldSegment(segmentStates, r.startTime, segmentEnd, wireFeedByStateId);
                BigDecimal gasL = calculateGasConsumptionLForWeldSegment(segmentStates, r.startTime, segmentEnd, gasFlowByStateId);

                String welderFullName = "";
                String welderTabNumber = "";
                String welderProfession = "";
                Long welderId = null;
                WeldingMachineState stateInSegment = getStateInSegment(segmentStates, r.startTime, segmentEnd, rfidByStateId);
                String rfidCode = null;
                if (stateInSegment != null) {
                    rfidCode = (stateInSegment.getRfid() != null && !stateInSegment.getRfid().trim().isEmpty())
                            ? stateInSegment.getRfid().trim()
                            : (rfidByStateId != null ? rfidByStateId.get(stateInSegment.getId()) : null);
                    if (rfidCode != null && rfidCode.isEmpty()) rfidCode = null;
                }
                // Fallback 1: RFID от ближайшего состояния из карты параметров (в пределах 60 мин) — по предвычисленному короткому списку
                if (rfidCode == null && !statesWithRfidInParamsByMachineId.isEmpty()) {
                    List<WeldingMachineState> withRfid = statesWithRfidInParamsByMachineId.get(r.machineId);
                    if (withRfid != null) {
                        WeldingMachineState closestWithRfid = getClosestStateWithRfid(withRfid, r.startTime, rfidByStateId, 60);
                        if (closestWithRfid != null)
                            rfidCode = rfidByStateId.get(closestWithRfid.getId());
                    }
                }
                // Fallback 2: RFID из колонки state.rfid — по предвычисленному списку за тот же день
                if (rfidCode == null) {
                    List<WeldingMachineState> sameDayWithRfid = statesWithRfidColumnByMachineAndDay.getOrDefault(r.machineId, Collections.emptyMap()).get(r.startTime.toLocalDate());
                    if (sameDayWithRfid != null && !sameDayWithRfid.isEmpty()) {
                        WeldingMachineState closestWithStateRfid = getClosestStateByTime(sameDayWithRfid, r.startTime);
                        if (closestWithStateRfid != null && closestWithStateRfid.getRfid() != null && !closestWithStateRfid.getRfid().trim().isEmpty())
                            rfidCode = closestWithStateRfid.getRfid().trim();
                    }
                }
                if (rfidCode != null) {
                    Optional<RfidPass> passOpt = findRfidPassByCode(rfidCode, rfidPassCache);
                    if (passOpt.isPresent() && passOpt.get().getWelder() != null) {
                        Welder w = passOpt.get().getWelder();
                        welderId = w.getId();
                        welderFullName = w.getName() != null ? w.getName() : "";
                        welderTabNumber = w.getEmployeeId() != null ? w.getEmployeeId() : "";
                        welderProfession = (w.getPosition() != null ? w.getPosition() : (w.getGrade() != null ? w.getGrade() : ""));
                    }
                }

                BigDecimal energyKwh = calculateEnergyPerWeld(r.avgVoltage, r.avgCurrent, r.durationSec);

                EquipmentWorkReportDTO dto = new EquipmentWorkReportDTO();
                dto.setDate(displayDateTime.toLocalDate());
                dto.setWeldStartTime(displayDateTime.toLocalTime());
                dto.setEquipmentModel(r.equipmentModel);
                dto.setEquipmentName(r.machineName);
                dto.setWorkMode(workMode != null ? workMode : "");
                dto.setWireFeedSpeedMpm(wireFeedMpm != null ? wireFeedMpm : BigDecimal.ZERO);
                dto.setWireConsumptionKg(wireKg != null ? wireKg : BigDecimal.ZERO);
                dto.setGasConsumptionL(gasL != null ? gasL : BigDecimal.ZERO);
                dto.setCurrentAmps(r.avgCurrent.setScale(1, RoundingMode.HALF_UP));
                dto.setVoltageVolts(r.avgVoltage.setScale(1, RoundingMode.HALF_UP));
                if (needSetCurrent || needSetVoltage) {
                    Long idleStateId = idleStateIdByEquipmentWeldKey.get(equipmentWeldRowKey(r));
                    if (needSetCurrent) {
                        dto.setSetCurrentAmps(toReportAmps(idleStateId != null ? setCurrentByStateId.get(idleStateId) : null));
                    }
                    if (needSetVoltage) {
                        dto.setSetVoltageVolts(toReportVoltsFromTenths(idleStateId != null ? setVoltageTenthsByStateId.get(idleStateId) : null));
                    }
                }
                dto.setWeldDurationSec(r.durationSec.setScale(1, RoundingMode.HALF_UP));
                dto.setEnergyConsumedKwh(energyKwh);
                dto.setCurrentOutOfRange(outOfRange);
                dto.setWeldingMachineId(r.machineId);
                dto.setWeldingMachineName(r.machineName);
                dto.setWelderFullName(welderFullName);
                dto.setWelderTabNumber(welderTabNumber);
                dto.setWelderProfession(welderProfession);
                dto.setWelderId(welderId);
                result.add(dto);
            }

            for (int i = 0; i < result.size(); i++) {
                result.get(i).setIndex(i + 1);
            }
        } catch (Exception e) {
            System.err.println("[REPORT-DATA] Ошибка getEquipmentWorkDataNew: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private static boolean templateNeedsWelderColumns(EquipmentWorkReportTemplateDTO template) {
        if (template == null || template.getSelectedColumns() == null) return true;
        for (String col : template.getSelectedColumns()) {
            if (col == null) continue;
            String c = col.trim();
            if ("welderFullName".equals(c) || "welderTabNumber".equals(c) || "profession".equals(c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean templateNeedsSetPointColumns(List<String> selectedColumns) {
        if (selectedColumns == null || selectedColumns.isEmpty()) return false;
        return selectedColumns.contains("setCurrent") || selectedColumns.contains("setVoltage");
    }

    /** Последнее состояние холостого хода (не Welding) строго до начала шва. */
    private static WeldingMachineState findLastIdleStateBefore(List<WeldingMachineState> sortedAsc, LocalDateTime weldStart) {
        if (sortedAsc == null || sortedAsc.isEmpty() || weldStart == null) return null;
        WeldingMachineState lastIdle = null;
        for (WeldingMachineState s : sortedAsc) {
            if (s.getDateCreated() == null) continue;
            if (!s.getDateCreated().isBefore(weldStart)) break;
            if (s.getWeldingMachineStatus() != WeldingMachineStatus.Welding) {
                lastIdle = s;
            }
        }
        return lastIdle;
    }

    private static String weldRowKey(WeldRow r) {
        return r.machineId + "|" + r.startTime;
    }

    private static String equipmentWeldRowKey(EquipmentWeldRow r) {
        return r.machineId + "|" + r.startTime;
    }

    private void loadSetCurrentByStateIds(List<Long> stateIds, Map<Long, Integer> currentByState) {
        if (stateIds == null || stateIds.isEmpty() || currentByState == null) return;
        List<Long> unique = stateIds.stream().distinct().collect(Collectors.toList());
        List<WeldingMachineParameterValue> currentVals = getParameterValuesInBatches(unique, "Current");
        for (WeldingMachineParameterValue pv : currentVals) {
            if (pv.getValue() == null || pv.getValue().trim().isEmpty()) continue;
            int v = parseParamValueToInt(pv.getValue());
            if (v != 0) currentByState.putIfAbsent(pv.getWeldingMachineStateId(), v);
        }
    }

    private void loadSetVoltageTenthsByStateIds(List<Long> stateIds, Map<Long, Integer> voltageTenthsByState) {
        if (stateIds == null || stateIds.isEmpty() || voltageTenthsByState == null) return;
        List<Long> unique = stateIds.stream().distinct().collect(Collectors.toList());
        List<WeldingMachineParameterValue> voltageVals = getParameterValuesInBatches(unique, "Voltage");
        for (WeldingMachineParameterValue pv : voltageVals) {
            if (pv.getValue() == null || pv.getValue().trim().isEmpty()) continue;
            int v = parseParamValueToInt(pv.getValue());
            if (v != 0) voltageTenthsByState.putIfAbsent(pv.getWeldingMachineStateId(), v);
        }
    }

    private static int parseParamValueToInt(String raw) {
        if (raw == null) return 0;
        String s = raw.trim().replace(',', '.');
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return 0;
        try {
            if (s.contains(".")) return (int) Math.round(Double.parseDouble(s));
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal toReportAmps(Integer amps) {
        if (amps == null || amps == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(amps).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal toReportVoltsFromTenths(Integer tenths) {
        if (tenths == null || tenths == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(tenths).divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
    }

    /** Параметры обогащения только для stateId вокруг швов — по одному property_code (индекс state_id+code). */
    private void loadEquipmentEnrichmentByStateIds(
            List<Long> stateIds,
            Map<Long, String> workModeByStateId,
            Map<Long, BigDecimal> wireFeedByStateId,
            Map<Long, LocalDateTime> deviceDateTimeByStateId) {
        if (stateIds == null || stateIds.isEmpty()) return;
        final int batchSize = 1_000;
        Map<String, Map<Long, Integer>> dateParts = new HashMap<>();
        for (String c : new String[] { "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds" }) {
            dateParts.put(c, new HashMap<>());
        }
        for (String code : new String[] { "Метод сварки", "WeldingMachineState", "Режим работы", "State.Mode", "State.Ctrl" }) {
            mergeEquipmentEnrichmentParamRows(stateIds, batchSize, code, workModeByStateId, wireFeedByStateId, dateParts);
        }
        mergeEquipmentEnrichmentParamRows(stateIds, batchSize, "Расход проволоки", workModeByStateId, wireFeedByStateId, dateParts);
        for (String code : new String[] { "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds" }) {
            mergeEquipmentEnrichmentParamRows(stateIds, batchSize, code, workModeByStateId, wireFeedByStateId, dateParts);
        }
        Set<Long> dateStateIds = new HashSet<>();
        for (Map<Long, Integer> m : dateParts.values()) dateStateIds.addAll(m.keySet());
        for (Long stateId : dateStateIds) {
            Integer year = dateParts.get("Date.Year").get(stateId);
            Integer month = dateParts.get("Date.Month").get(stateId);
            Integer day = dateParts.get("Date.Day").get(stateId);
            Integer hour = dateParts.get("Time.Hours").get(stateId);
            Integer min = dateParts.get("Time.Minutes").get(stateId);
            Integer sec = dateParts.get("Time.Seconds").get(stateId);
            if (year == null || month == null || day == null || hour == null || min == null || sec == null) continue;
            if (year < 100) year = 2000 + year;
            if (month < 1 || month > 12 || day < 1 || day > 31) continue;
            if (hour < 0 || hour > 23 || min < 0 || min > 59 || sec < 0 || sec > 59) continue;
            try {
                deviceDateTimeByStateId.put(stateId, LocalDateTime.of(year, month, day, hour, min, sec));
            } catch (Exception ignored) { }
        }
    }

    private void mergeEquipmentEnrichmentParamRows(
            List<Long> stateIds, int batchSize, String propertyCode,
            Map<Long, String> workModeByStateId,
            Map<Long, BigDecimal> wireFeedByStateId,
            Map<String, Map<Long, Integer>> dateParts) {
        for (int i = 0; i < stateIds.size(); i += batchSize) {
            List<Long> batch = stateIds.subList(i, Math.min(i + batchSize, stateIds.size()));
            try {
                List<Object[]> rows = parameterValueRepository.findStateIdAndValueNativeCoalesce(batch, propertyCode);
                for (Object[] row : rows) {
                    if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
                    long stateId = ((Number) row[0]).longValue();
                    String valueStr = row[1].toString().trim();
                    if (valueStr.isEmpty() || "null".equalsIgnoreCase(valueStr)) continue;
                    if ("Расход проволоки".equals(propertyCode)) {
                        try {
                            wireFeedByStateId.put(stateId, new BigDecimal(valueStr.replace(",", ".")));
                        } catch (NumberFormatException ignored) { }
                    } else if (dateParts != null && dateParts.containsKey(propertyCode)) {
                        try {
                            dateParts.get(propertyCode).put(stateId, Integer.parseInt(valueStr));
                        } catch (NumberFormatException ignored) { }
                    } else {
                        workModeByStateId.putIfAbsent(stateId, valueStr);
                    }
                }
            } catch (Exception e) {
                System.err.println("[REPORT-DATA] equipment enrichment " + propertyCode + " batch " + i + ": " + e.getMessage());
            }
        }
    }

    /**
     * Один JOIN по аппарату+периоду, в карты попадают только stateId из швов (быстрее батчей IN за сутки).
     */
    private void loadEquipmentEnrichmentByMachinePeriodFiltered(
            Integer machineId, LocalDateTime start, LocalDateTime end, Set<Long> allowedStateIds,
            Map<Long, String> workModeByStateId,
            Map<Long, BigDecimal> wireFeedByStateId,
            Map<Long, LocalDateTime> deviceDateTimeByStateId) {
        if (machineId == null || start == null || end == null || allowedStateIds == null || allowedStateIds.isEmpty()) return;
        List<String> codes = Arrays.asList(
                "Метод сварки", "WeldingMachineState", "Режим работы", "State.Mode", "State.Ctrl",
                "Расход проволоки",
                "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds");
        Map<String, Map<Long, Integer>> dateParts = new HashMap<>();
        for (String c : new String[] { "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds" }) {
            dateParts.put(c, new HashMap<>());
        }
        Map<String, Map<Long, String>> workModeByCode = new HashMap<>();
        try {
            List<Object[]> rows = parameterValueRepository.findStateIdCodeAndValueByMachineDateRange(
                    machineId, start, end, codes);
            for (Object[] row : rows) {
                if (row == null || row.length < 3 || row[0] == null || row[1] == null || row[2] == null) continue;
                long stateId = ((Number) row[0]).longValue();
                if (!allowedStateIds.contains(stateId)) continue;
                String code = row[1].toString();
                String valueStr = row[2].toString().trim();
                if (valueStr.isEmpty() || "null".equalsIgnoreCase(valueStr)) continue;
                if ("Расход проволоки".equals(code)) {
                    try {
                        wireFeedByStateId.put(stateId, new BigDecimal(valueStr.replace(",", ".")));
                    } catch (NumberFormatException ignored) { }
                } else if (dateParts.containsKey(code)) {
                    try {
                        dateParts.get(code).put(stateId, Integer.parseInt(valueStr));
                    } catch (NumberFormatException ignored) { }
                } else {
                    workModeByCode.computeIfAbsent(code, k -> new HashMap<>()).put(stateId, valueStr);
                }
            }
        } catch (Exception e) {
            System.err.println("[REPORT-DATA] equipment enrichment filtered period: " + e.getMessage());
        }
        for (String code : new String[] { "Метод сварки", "WeldingMachineState", "Режим работы", "State.Mode", "State.Ctrl" }) {
            Map<Long, String> m = workModeByCode.get(code);
            if (m == null) continue;
            for (Map.Entry<Long, String> e : m.entrySet()) {
                workModeByStateId.putIfAbsent(e.getKey(), e.getValue());
            }
        }
        Set<Long> dateStateIds = new HashSet<>();
        for (Map<Long, Integer> m : dateParts.values()) dateStateIds.addAll(m.keySet());
        for (Long stateId : dateStateIds) {
            if (!allowedStateIds.contains(stateId)) continue;
            Integer year = dateParts.get("Date.Year").get(stateId);
            Integer month = dateParts.get("Date.Month").get(stateId);
            Integer day = dateParts.get("Date.Day").get(stateId);
            Integer hour = dateParts.get("Time.Hours").get(stateId);
            Integer min = dateParts.get("Time.Minutes").get(stateId);
            Integer sec = dateParts.get("Time.Seconds").get(stateId);
            if (year == null || month == null || day == null || hour == null || min == null || sec == null) continue;
            if (year < 100) year = 2000 + year;
            if (month < 1 || month > 12 || day < 1 || day > 31) continue;
            if (hour < 0 || hour > 23 || min < 0 || min > 59 || sec < 0 || sec > 59) continue;
            try {
                deviceDateTimeByStateId.put(stateId, LocalDateTime.of(year, month, day, hour, min, sec));
            } catch (Exception ignored) { }
        }
    }

    /** Один проход JOIN по аппарату+периоду: режим, подача проволоки, Date/Time (вместо 6+ отдельных запросов). */
    private void loadEquipmentEnrichmentByMachinePeriod(
            Integer machineId, LocalDateTime start, LocalDateTime end,
            Map<Long, String> workModeByStateId,
            Map<Long, BigDecimal> wireFeedByStateId,
            Map<Long, LocalDateTime> deviceDateTimeByStateId) {
        if (machineId == null || start == null || end == null) return;
        List<String> codes = Arrays.asList(
                "Метод сварки", "WeldingMachineState", "Режим работы", "State.Mode", "State.Ctrl",
                "Расход проволоки",
                "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds");
        Map<String, Map<Long, Integer>> dateParts = new HashMap<>();
        for (String c : new String[] { "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds" }) {
            dateParts.put(c, new HashMap<>());
        }
        Map<String, Map<Long, String>> workModeByCode = new HashMap<>();
        try {
            List<Object[]> rows = parameterValueRepository.findStateIdCodeAndValueByMachineDateRange(
                    machineId, start, end, codes);
            for (Object[] row : rows) {
                if (row == null || row.length < 3 || row[0] == null || row[1] == null || row[2] == null) continue;
                long stateId = ((Number) row[0]).longValue();
                String code = row[1].toString();
                String valueStr = row[2].toString().trim();
                if (valueStr.isEmpty() || "null".equalsIgnoreCase(valueStr)) continue;
                if ("Расход проволоки".equals(code)) {
                    try {
                        wireFeedByStateId.put(stateId, new BigDecimal(valueStr.replace(",", ".")));
                    } catch (NumberFormatException ignored) { }
                } else if (dateParts.containsKey(code)) {
                    try {
                        dateParts.get(code).put(stateId, Integer.parseInt(valueStr));
                    } catch (NumberFormatException ignored) { }
                } else {
                    workModeByCode.computeIfAbsent(code, k -> new HashMap<>()).put(stateId, valueStr);
                }
            }
        } catch (Exception e) {
            System.err.println("[REPORT-DATA] equipment enrichment combined query: " + e.getMessage());
        }
        for (String code : new String[] { "Метод сварки", "WeldingMachineState", "Режим работы", "State.Mode", "State.Ctrl" }) {
            Map<Long, String> m = workModeByCode.get(code);
            if (m == null) continue;
            for (Map.Entry<Long, String> e : m.entrySet()) {
                workModeByStateId.putIfAbsent(e.getKey(), e.getValue());
            }
        }
        Set<Long> dateStateIds = new HashSet<>();
        for (Map<Long, Integer> m : dateParts.values()) dateStateIds.addAll(m.keySet());
        for (Long stateId : dateStateIds) {
            Integer year = dateParts.get("Date.Year").get(stateId);
            Integer month = dateParts.get("Date.Month").get(stateId);
            Integer day = dateParts.get("Date.Day").get(stateId);
            Integer hour = dateParts.get("Time.Hours").get(stateId);
            Integer min = dateParts.get("Time.Minutes").get(stateId);
            Integer sec = dateParts.get("Time.Seconds").get(stateId);
            if (year == null || month == null || day == null || hour == null || min == null || sec == null) continue;
            if (year < 100) year = 2000 + year;
            if (month < 1 || month > 12 || day < 1 || day > 31) continue;
            if (hour < 0 || hour > 23 || min < 0 || min > 59 || sec < 0 || sec > 59) continue;
            try {
                deviceDateTimeByStateId.put(stateId, LocalDateTime.of(year, month, day, hour, min, sec));
            } catch (Exception ignored) { }
        }
    }

    private void loadWorkModeByMachinePeriod(Integer machineId, LocalDateTime start, LocalDateTime end, Map<Long, String> out) {
        if (machineId == null || start == null || end == null) return;
        for (String code : new String[] { "Метод сварки", "WeldingMachineState", "Режим работы", "State.Mode", "State.Ctrl" }) {
            try {
                for (Object[] row : parameterValueRepository.findStateIdAndValueByMachineDateRange(machineId, start, end, code)) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        String v = row[1].toString().trim();
                        if (!v.isEmpty()) out.putIfAbsent(((Number) row[0]).longValue(), v);
                    }
                }
            } catch (Exception e) {
                System.err.println("[REPORT-DATA] workMode machine+period " + code + ": " + e.getMessage());
            }
            if (!out.isEmpty()) return;
        }
    }

    private Map<Long, LocalDateTime> buildDeviceDateTimeByMachinePeriod(Integer machineId, LocalDateTime start, LocalDateTime end) {
        Map<Long, LocalDateTime> out = new HashMap<>();
        if (machineId == null || start == null || end == null) return out;
        String[] codes = { "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds" };
        Map<String, Map<Long, Integer>> byCode = new HashMap<>();
        for (String code : codes) {
            Map<Long, Integer> map = new HashMap<>();
            try {
                for (Object[] row : parameterValueRepository.findStateIdAndValueByMachineDateRange(machineId, start, end, code)) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        try {
                            map.put(((Number) row[0]).longValue(), Integer.parseInt(row[1].toString().trim()));
                        } catch (NumberFormatException ignored) { }
                    }
                }
            } catch (Exception e) {
                System.err.println("[REPORT-DATA] deviceTime machine+period " + code + ": " + e.getMessage());
            }
            byCode.put(code, map);
        }
        Set<Long> ids = new HashSet<>();
        for (Map<Long, Integer> m : byCode.values()) ids.addAll(m.keySet());
        for (Long stateId : ids) {
            Integer year = byCode.get("Date.Year").get(stateId);
            Integer month = byCode.get("Date.Month").get(stateId);
            Integer day = byCode.get("Date.Day").get(stateId);
            Integer hour = byCode.get("Time.Hours").get(stateId);
            Integer min = byCode.get("Time.Minutes").get(stateId);
            Integer sec = byCode.get("Time.Seconds").get(stateId);
            if (year == null || month == null || day == null || hour == null || min == null || sec == null) continue;
            if (year < 100) year = 2000 + year;
            if (month < 1 || month > 12 || day < 1 || day > 31) continue;
            if (hour < 0 || hour > 23 || min < 0 || min > 59 || sec < 0 || sec > 59) continue;
            try {
                out.put(stateId, LocalDateTime.of(year, month, day, hour, min, sec));
            } catch (Exception ignored) { }
        }
        return out;
    }

    private void loadWireFeedByMachinePeriod(Integer machineId, LocalDateTime start, LocalDateTime end, Map<Long, BigDecimal> wireFeedByStateId) {
        if (machineId == null || start == null || end == null) return;
        try {
            for (Object[] row : parameterValueRepository.findStateIdAndValueByMachineDateRangeCoalesce(machineId, start, end, "Расход проволоки")) {
                if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
                try {
                    String s = row[1].toString().trim().replace(",", ".");
                    if (!s.isEmpty()) wireFeedByStateId.put(((Number) row[0]).longValue(), new BigDecimal(s));
                } catch (NumberFormatException ignored) { }
            }
        } catch (Exception e) {
            System.err.println("[REPORT-DATA] wireFeed machine+period: " + e.getMessage());
        }
    }

    /** RFID в параметрах за период по аппарату (без батчей по tens of thousands stateId). */
    private Map<Long, String> loadRfidByMachinePeriodNative(Integer machineId, LocalDateTime start, LocalDateTime end) {
        Map<Long, String> map = new HashMap<>();
        for (String code : new String[] { "RFID.Hex", "RFID", "State.RFID", "Rfid" }) {
            try {
                for (Object[] row : parameterValueRepository.findStateIdAndValueByMachineDateRange(machineId, start, end, code)) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        String v = row[1].toString().trim();
                        if (!v.isEmpty()) map.putIfAbsent(((Number) row[0]).longValue(), v);
                    }
                }
            } catch (Exception ignored) { }
            if (!map.isEmpty()) break;
        }
        return map;
    }

    /**
     * Швы для отчёта: из материализованного кэша (если сутки пересчитаны), иначе live-расчёт.
     */
    private List<WeldSegmentDTO> resolveWeldSegmentsForReport(
            Integer machineId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            List<WeldingMachineState> machineStates) {
        Optional<List<WeldSegmentDTO>> cached = weldSegmentCacheService.findSegmentsForReportIfReady(
                machineId, periodStart, periodEnd);
        if (cached.isPresent()) {
            return cached.get();
        }
        return calculationService.calculateWeldSegmentsFromStates(machineStates);
    }

    private static class EquipmentWeldRow {
        int machineId;
        String machineName;
        String equipmentModel;
        LocalDateTime startTime;
        BigDecimal durationSec;
        BigDecimal avgCurrent;
        BigDecimal avgVoltage;
    }

    private EquipmentWeldRow copyEquipmentWeldRow(EquipmentWeldRow r) {
        EquipmentWeldRow c = new EquipmentWeldRow();
        c.machineId = r.machineId;
        c.machineName = r.machineName;
        c.equipmentModel = r.equipmentModel;
        c.startTime = r.startTime;
        c.durationSec = r.durationSec;
        c.avgCurrent = r.avgCurrent;
        c.avgVoltage = r.avgVoltage;
        return c;
    }

    private static int lowerBoundByDateCreated(List<WeldingMachineState> sorted, LocalDateTime t) {
        if (sorted == null || sorted.isEmpty()) return 0;
        int lo = 0, hi = sorted.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            LocalDateTime dc = sorted.get(mid).getDateCreated();
            if (dc == null || dc.isBefore(t)) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Подсписок состояний с dateCreated в [fromInclusive, toExclusive), список отсортирован по dateCreated. */
    private static List<WeldingMachineState> statesInTimeWindow(
            List<WeldingMachineState> sorted, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        if (sorted == null || sorted.isEmpty()) return Collections.emptyList();
        int lo = lowerBoundByDateCreated(sorted, fromInclusive);
        int hi = lowerBoundByDateCreated(sorted, toExclusive);
        if (lo >= hi || lo >= sorted.size()) return Collections.emptyList();
        return sorted.subList(lo, Math.min(hi, sorted.size()));
    }

    private static List<Long> collectStateIdsForEquipmentMerged(
            Map<Integer, List<WeldingMachineState>> statesByMachineId, List<EquipmentWeldRow> merged) {
        return collectStateIdsAroundWeldRows(statesByMachineId, merged, r -> r.machineId, r -> r.startTime, r -> r.durationSec);
    }

    private static List<Long> collectStateIdsForWelderMerged(
            Map<Integer, List<WeldingMachineState>> statesByMachineId, List<WeldRow> merged) {
        return collectStateIdsAroundWeldRows(statesByMachineId, merged, r -> r.machineId, r -> r.startTime, r -> r.durationSec);
    }

    private static <R> List<Long> collectStateIdsAroundWeldRows(
            Map<Integer, List<WeldingMachineState>> statesByMachineId,
            List<R> merged,
            java.util.function.Function<R, Integer> machineId,
            java.util.function.Function<R, LocalDateTime> startTime,
            java.util.function.Function<R, BigDecimal> durationSec) {
        if (merged == null || merged.isEmpty()) return Collections.emptyList();
        Set<Long> ids = new LinkedHashSet<>();
        for (R row : merged) {
            LocalDateTime start = startTime.apply(row);
            BigDecimal dur = durationSec.apply(row);
            if (start == null || dur == null) continue;
            LocalDateTime end = start.plusSeconds(dur.longValue());
            List<WeldingMachineState> sorted = statesByMachineId.get(machineId.apply(row));
            if (sorted == null) continue;
            for (WeldingMachineState s : statesInTimeWindow(sorted, start.minusMinutes(5), end.plusMinutes(5))) {
                if (s.getId() != null) ids.add(s.getId());
            }
        }
        return new ArrayList<>(ids);
    }

    /**
     * Находит состояние аппарата, пересекающееся с сегментом [segmentStart, segmentEnd].
     * Список states должен быть отсортирован по dateCreated (отсортирован один раз при загрузке).
     */
    private WeldingMachineState getStateInSegment(List<WeldingMachineState> states, LocalDateTime segmentStart, LocalDateTime segmentEnd, Map<Long, String> rfidByStateId) {
        if (states == null || states.isEmpty()) return null;
        // 1) Предпочитаем состояние, у которого есть RFID в параметрах
        if (rfidByStateId != null && !rfidByStateId.isEmpty()) {
            for (WeldingMachineState s : states) {
                if (overlapsSegment(s, segmentStart, segmentEnd) && rfidByStateId.containsKey(s.getId()))
                    return s;
            }
        }
        // 2) Иначе — с непустым state.rfid
        for (WeldingMachineState s : states) {
            if (overlapsSegment(s, segmentStart, segmentEnd) && s.getRfid() != null && !s.getRfid().trim().isEmpty())
                return s;
        }
        // 3) Любое пересекающееся
        for (WeldingMachineState s : states) {
            if (overlapsSegment(s, segmentStart, segmentEnd)) return s;
        }
        return null;
    }

    /** Ближайшее по времени к segmentStart состояние в списке (минимальная разница по времени). */
    private WeldingMachineState getClosestStateByTime(List<WeldingMachineState> states, LocalDateTime segmentStart) {
        if (states == null || states.isEmpty()) return null;
        WeldingMachineState best = null;
        long bestDiff = Long.MAX_VALUE;
        for (WeldingMachineState s : states) {
            if (s.getDateCreated() == null) continue;
            long diff = Math.abs(java.time.Duration.between(segmentStart, s.getDateCreated()).toMillis());
            if (diff < bestDiff) {
                bestDiff = diff;
                best = s;
            }
        }
        return best;
    }

    /**
     * Пересечение состояния с сегментом шва. Если длительность в БД = 0, считаем состояние «точкой» во времени
     * (пересечение есть, если момент попадает в [segmentStart, segmentEnd)).
     */
    private boolean overlapsSegment(WeldingMachineState s, LocalDateTime segmentStart, LocalDateTime segmentEnd) {
        LocalDateTime stateStart = s.getDateCreated();
        if (stateStart == null) return false;
        long durationMs = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
        if (durationMs <= 0) {
            return !stateStart.isBefore(segmentStart) && stateStart.isBefore(segmentEnd);
        }
        LocalDateTime stateEnd = stateStart.plus(durationMs, ChronoUnit.MILLIS);
        return stateStart.isBefore(segmentEnd) && stateEnd.isAfter(segmentStart);
    }

    /**
     * Эффективная длительность состояния (мс): как в {@link WeldingReportCalculationService} —
     * при нулевом {@code state_duration_ms} берётся разрыв до следующего состояния по времени на том же аппарате.
     */
    private long effectiveStateDurationMs(WeldingMachineState s, List<WeldingMachineState> sortedSameMachineByDate) {
        long d = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
        if (d > 0) return d;
        if (sortedSameMachineByDate == null || sortedSameMachineByDate.isEmpty() || s.getDateCreated() == null) {
            return 0L;
        }
        int i = -1;
        for (int k = 0; k < sortedSameMachineByDate.size(); k++) {
            if (s.getId() != null && s.getId().equals(sortedSameMachineByDate.get(k).getId())) {
                i = k;
                break;
            }
        }
        if (i < 0 || i + 1 >= sortedSameMachineByDate.size()) return 0L;
        LocalDateTime nextT = sortedSameMachineByDate.get(i + 1).getDateCreated();
        if (nextT == null) return 0L;
        long gap = java.time.Duration.between(s.getDateCreated(), nextT).toMillis();
        return gap > 0 ? gap : 0L;
    }

    /**
     * Длительность пересечения интервала состояния с полуинтервалом [segmentStart, segmentEnd), мс.
     * {@code sortedSameMachineByDate} — все состояния аппарата за период, по возрастанию {@code dateCreated}.
     */
    private long overlapDurationMs(WeldingMachineState s, LocalDateTime segmentStart, LocalDateTime segmentEnd,
                                   List<WeldingMachineState> sortedSameMachineByDate) {
        LocalDateTime stateStart = s.getDateCreated();
        if (stateStart == null) return 0L;
        long durationMs = effectiveStateDurationMs(s, sortedSameMachineByDate);
        if (durationMs <= 0) return 0L;
        LocalDateTime stateEnd = stateStart.plus(durationMs, ChronoUnit.MILLIS);
        if (!(stateStart.isBefore(segmentEnd) && stateEnd.isAfter(segmentStart))) return 0L;
        LocalDateTime overlapStart = stateStart.isBefore(segmentStart) ? segmentStart : stateStart;
        LocalDateTime overlapEnd = stateEnd.isAfter(segmentEnd) ? segmentEnd : stateEnd;
        if (!overlapStart.isBefore(overlapEnd)) return 0L;
        return java.time.Duration.between(overlapStart, overlapEnd).toMillis();
    }

    /**
     * Масса проволоки за шов (кг): только состояния Welding,
     * скорость подачи из БД (м/мин), погонная масса из настройки (кг/м).
     */
    private BigDecimal calculateWireConsumptionKgForWeldSegment(
            List<WeldingMachineState> machineStates,
            LocalDateTime segmentStart,
            LocalDateTime segmentEnd,
            Map<Long, BigDecimal> wireFeedByStateId) {
        if (machineStates == null || machineStates.isEmpty()
                || wireFeedByStateId == null || wireFeedByStateId.isEmpty()
                || wireLinearDensityKgPerMeter == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal density = wireLinearDensityKgPerMeter;
        BigDecimal sum = BigDecimal.ZERO;
        List<WeldingMachineState> sorted = new ArrayList<>(machineStates);
        sorted.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
        for (WeldingMachineState s : sorted) {
            if (s.getWeldingMachineStatus() != WeldingMachineStatus.Welding) continue;
            long overlapMs = overlapDurationMs(s, segmentStart, segmentEnd, sorted);
            if (overlapMs <= 0) continue;
            BigDecimal mpm = wireFeedByStateId.get(s.getId());
            if (mpm == null || mpm.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal minutes = BigDecimal.valueOf(overlapMs)
                    .divide(BigDecimal.valueOf(60_000), 8, RoundingMode.HALF_UP);
            sum = sum.add(mpm.multiply(density).multiply(minutes));
        }
        return sum.setScale(5, RoundingMode.HALF_UP);
    }

    /**
     * Расход газа за шов (л): только состояния Welding,
     * мгновенный расход State.GasFlow (л/мин) × время перекрытия с сегментом.
     */
    private BigDecimal calculateGasConsumptionLForWeldSegment(
            List<WeldingMachineState> machineStates,
            LocalDateTime segmentStart,
            LocalDateTime segmentEnd,
            Map<Long, BigDecimal> gasFlowLpmByStateId) {
        if (machineStates == null || machineStates.isEmpty()
                || gasFlowLpmByStateId == null || gasFlowLpmByStateId.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        List<WeldingMachineState> sorted = new ArrayList<>(machineStates);
        sorted.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
        for (WeldingMachineState s : sorted) {
            long overlapMs = overlapDurationMs(s, segmentStart, segmentEnd, sorted);
            if (overlapMs <= 0) continue;
            BigDecimal lpm = gasFlowLpmByStateId.get(s.getId());
            if (lpm == null || lpm.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal minutes = BigDecimal.valueOf(overlapMs)
                    .divide(BigDecimal.valueOf(60_000), 8, RoundingMode.HALF_UP);
            sum = sum.add(lpm.multiply(minutes));
        }
        return sum.setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Суммарный расход проволоки (кг) за период по состояниям сварки: м/мин × кг/м × мин.
     */
    private BigDecimal calculateWireConsumptionKgFromWeldingStates(
            List<WeldingMachineState> states,
            Map<Long, BigDecimal> wireFeedByStateId) {
        if (states == null || states.isEmpty()
                || wireFeedByStateId == null || wireFeedByStateId.isEmpty()
                || wireLinearDensityKgPerMeter == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal density = wireLinearDensityKgPerMeter;
        BigDecimal sum = BigDecimal.ZERO;
        List<WeldingMachineState> sorted = new ArrayList<>(states);
        sorted.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
        for (WeldingMachineState s : states) {
            if (s.getWeldingMachineStatus() != WeldingMachineStatus.Welding) continue;
            long durMs = effectiveStateDurationMs(s, sorted);
            if (durMs <= 0) continue;
            BigDecimal mpm = wireFeedByStateId.get(s.getId());
            if (mpm == null || mpm.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal minutes = BigDecimal.valueOf(durMs)
                    .divide(BigDecimal.valueOf(60_000), 8, RoundingMode.HALF_UP);
            sum = sum.add(mpm.multiply(density).multiply(minutes));
        }
        return sum.setScale(5, RoundingMode.HALF_UP);
    }

    /**
     * Ближайшее по времени к segmentStart состояние из списка, у которого есть RFID в rfidByStateId.
     * Только если разница по времени не больше maxDiffMinutes минут.
     */
    private WeldingMachineState getClosestStateWithRfid(List<WeldingMachineState> states, LocalDateTime segmentStart,
                                                        Map<Long, String> rfidByStateId, long maxDiffMinutes) {
        if (states == null || rfidByStateId == null || rfidByStateId.isEmpty()) return null;
        WeldingMachineState best = null;
        long bestDiffMinutes = maxDiffMinutes + 1;
        for (WeldingMachineState s : states) {
            if (s.getDateCreated() == null || !rfidByStateId.containsKey(s.getId())) continue;
            long diffMinutes = Math.abs(java.time.Duration.between(segmentStart, s.getDateCreated()).toMinutes());
            if (diffMinutes <= maxDiffMinutes && diffMinutes < bestDiffMinutes) {
                bestDiffMinutes = diffMinutes;
                best = s;
            }
        }
        return best;
    }

    /**
     * Ближайшее по времени к segmentStart состояние с непустым state.rfid (колонка RFID в БД), в пределах maxDiffMinutes минут.
     * Используется когда отчёт по сварщику находит данные по state.rfid, а в параметрах за этот день RFID нет.
     */
    private WeldingMachineState getClosestStateWithRfidColumn(List<WeldingMachineState> states, LocalDateTime segmentStart, long maxDiffMinutes) {
        if (states == null) return null;
        WeldingMachineState best = null;
        long bestDiffMinutes = maxDiffMinutes + 1;
        for (WeldingMachineState s : states) {
            if (s.getDateCreated() == null || s.getRfid() == null || s.getRfid().trim().isEmpty()) continue;
            long diffMinutes = Math.abs(java.time.Duration.between(segmentStart, s.getDateCreated()).toMinutes());
            if (diffMinutes <= maxDiffMinutes && diffMinutes < bestDiffMinutes) {
                bestDiffMinutes = diffMinutes;
                best = s;
            }
        }
        return best;
    }

    /**
     * Загружает RFID по stateId из таблицы параметров нативным запросом (колонка property_code в БД).
     * Пробует оба варианта имени колонки: welding_machine_stateid и welding_machine_state_id.
     */
    private Map<Long, String> loadRfidByStateIdFromParamsNative(List<Long> stateIds) {
        Map<Long, String> out = new HashMap<>();
        if (stateIds == null || stateIds.isEmpty()) return out;
        final int BATCH = 10_000;
        for (String propertyCode : new String[] { "RFID.Hex", "RFID", "State.RFID", "Rfid" }) {
            for (int i = 0; i < stateIds.size(); i += BATCH) {
                List<Long> batch = stateIds.subList(i, Math.min(i + BATCH, stateIds.size()));
                try {
                    List<Object[]> rows = parameterValueRepository.findStateIdAndValueNative(batch, propertyCode);
                    for (Object[] row : rows) {
                        if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                            String val = row[1].toString().trim();
                            if (!val.isEmpty()) {
                                long stateId = ((Number) row[0]).longValue();
                                out.putIfAbsent(stateId, val);
                            }
                        }
                    }
                } catch (Exception e1) {
                    try {
                        List<Object[]> rows = parameterValueRepository.findStateIdAndValueNativeUnderscore(batch, propertyCode);
                        for (Object[] row : rows) {
                            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                                String val = row[1].toString().trim();
                                if (!val.isEmpty()) {
                                    long stateId = ((Number) row[0]).longValue();
                                    out.putIfAbsent(stateId, val);
                                }
                            }
                        }
                    } catch (Exception e2) {
                        // оба варианта колонки не подошли
                    }
                }
            }
            if (!out.isEmpty()) break;
        }
        return out;
    }

    /**
     * Поиск пропуска по коду: сначала точное совпадение, затем вариант без ведущих нулей (hex).
     * В БД может быть несколько строк с одним code — берём одну детерминированно (см. pickOneRfidPass).
     */
    private Optional<RfidPass> findRfidPassByCode(String code) {
        return findRfidPassByCode(code, null);
    }

    /**
     * Поиск RFID-пропуска с кэшем на время одной генерации отчёта (избегает повторных запросов на один код).
     */
    private Optional<RfidPass> findRfidPassByCode(String code, Map<String, Optional<RfidPass>> cache) {
        if (code == null || code.trim().isEmpty()) return Optional.empty();
        String trimmed = code.trim();
        if (cache != null) {
            if (cache.containsKey(trimmed)) {
                return cache.get(trimmed);
            }
            Optional<RfidPass> resolved = resolveRfidPassByCode(trimmed);
            cache.put(trimmed, resolved);
            return resolved;
        }
        return resolveRfidPassByCode(trimmed);
    }

    private Optional<RfidPass> resolveRfidPassByCode(String trimmed) {
        Optional<RfidPass> picked = pickOneRfidPass(rfidPassRepository.findAllByCode(trimmed));
        if (picked.isPresent()) return picked;
        String normalized = normalizeHexLeadingZeros(trimmed);
        if (!normalized.equals(trimmed)) {
            return pickOneRfidPass(rfidPassRepository.findAllByCode(normalized));
        }
        return Optional.empty();
    }

    /** При дубликатах code: предпочитаем пропуск с привязкой к сварщику, иначе с меньшим id. */
    private static Optional<RfidPass> pickOneRfidPass(List<RfidPass> list) {
        if (list == null || list.isEmpty()) return Optional.empty();
        if (list.size() == 1) return Optional.of(list.get(0));
        return list.stream()
                .min(Comparator
                        .comparing((RfidPass p) -> p.getWelder() == null ? 1 : 0)
                        .thenComparing(RfidPass::getId, Comparator.nullsLast(Long::compareTo)));
    }

    private static String normalizeHexLeadingZeros(String hex) {
        if (hex == null || hex.isEmpty()) return hex;
        String s = hex.trim();
        if (!s.matches("^[0-9A-Fa-f]+$")) return s;
        s = s.replaceFirst("^0+", "");
        return s.isEmpty() ? "0" : s;
    }

    private static class WeldRow {
        long welderId;
        int machineId;
        String machineName;
        String equipmentModel;
        LocalDateTime startTime;
        BigDecimal durationSec;
        BigDecimal avgCurrent;
        BigDecimal avgVoltage;
    }

    private WeldRow copyWeldRow(WeldRow r) {
        WeldRow c = new WeldRow();
        c.welderId = r.welderId;
        c.machineId = r.machineId;
        c.machineName = r.machineName;
        c.equipmentModel = r.equipmentModel;
        c.startTime = r.startTime;
        c.durationSec = r.durationSec;
        c.avgCurrent = r.avgCurrent;
        c.avgVoltage = r.avgVoltage;
        return c;
    }

    /**
     * Затраченная энергия на шов (кВт*ч) — по той же формуле, что и в отчёте по расходу проволоки:
     * E = P * t, где P = U*I/1000 (кВт), t = длительность шва в часах.
     */
    private BigDecimal calculateEnergyPerWeld(BigDecimal avgVoltage, BigDecimal avgCurrent, BigDecimal durationSec) {
        if (avgVoltage == null || avgCurrent == null || durationSec == null
                || durationSec.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal powerKw = avgVoltage.multiply(avgCurrent).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        BigDecimal timeHours = durationSec.divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
        return powerKw.multiply(timeHours).setScale(4, RoundingMode.HALF_UP);
    }

    /** Собирает по stateId дату/время из посылки аппарата (Date.Year/Month/Day, Time.Hours/Minutes/Seconds). */
    private Map<Long, LocalDateTime> buildDeviceDateTimeByStateId(List<Long> allStateIds) {
        Map<Long, LocalDateTime> out = new HashMap<>();
        if (allStateIds == null || allStateIds.isEmpty()) return out;
        String[] codes = { "Date.Year", "Date.Month", "Date.Day", "Time.Hours", "Time.Minutes", "Time.Seconds" };
        Map<String, Map<Long, Integer>> byCode = new HashMap<>();
        for (String code : codes) {
            List<WeldingMachineParameterValue> vals = getParameterValuesInBatches(allStateIds, code);
            Map<Long, Integer> map = new HashMap<>();
            for (WeldingMachineParameterValue pv : vals) {
                if (pv.getValue() != null && !pv.getValue().trim().isEmpty()) {
                    try {
                        map.put(pv.getWeldingMachineStateId(), Integer.parseInt(pv.getValue().trim()));
                    } catch (NumberFormatException ignored) { }
                }
            }
            byCode.put(code, map);
        }
        for (Long stateId : allStateIds) {
            Integer year = byCode.get("Date.Year").get(stateId);
            Integer month = byCode.get("Date.Month").get(stateId);
            Integer day = byCode.get("Date.Day").get(stateId);
            Integer hour = byCode.get("Time.Hours").get(stateId);
            Integer min = byCode.get("Time.Minutes").get(stateId);
            Integer sec = byCode.get("Time.Seconds").get(stateId);
            if (year == null || month == null || day == null || hour == null || min == null || sec == null) continue;
            if (year < 100) year = 2000 + year;
            if (month < 1 || month > 12 || day < 1 || day > 31) continue;
            if (hour < 0 || hour > 23 || min < 0 || min > 59 || sec < 0 || sec > 59) continue;
            try {
                out.put(stateId, LocalDateTime.of(year, month, day, hour, min, sec));
            } catch (Exception ignored) { }
        }
        return out;
    }

    /** Время начала шва по первому состоянию сегмента: из времени аппарата, если есть. */
    private LocalDateTime getDeviceDateTimeFromCache(List<WeldingMachineState> machineStates,
                                                     LocalDateTime segmentStart, LocalDateTime segmentEnd,
                                                     Map<Long, LocalDateTime> deviceDateTimeByStateId) {
        if (machineStates == null || deviceDateTimeByStateId == null || deviceDateTimeByStateId.isEmpty()) return null;
        for (WeldingMachineState s : machineStates) {
            LocalDateTime t = s.getDateCreated();
            if (t != null && !t.isBefore(segmentStart) && t.isBefore(segmentEnd)) {
                LocalDateTime deviceTime = deviceDateTimeByStateId.get(s.getId());
                if (deviceTime != null) return deviceTime;
            }
        }
        return null;
    }

    /** Режим работы по сегменту из предзагруженных состояний и карты stateId -> значение. */
    private String getWorkModeFromCache(List<WeldingMachineState> machineStates,
                                        LocalDateTime segmentStart, LocalDateTime segmentEnd,
                                        Map<Long, String> workModeByStateId) {
        if (machineStates == null || workModeByStateId.isEmpty()) return null;
        for (WeldingMachineState s : machineStates) {
            LocalDateTime t = s.getDateCreated();
            if (t != null && !t.isBefore(segmentStart) && t.isBefore(segmentEnd)) {
                String v = workModeByStateId.get(s.getId());
                if (v != null && !v.isEmpty()) return v;
            }
        }
        return null;
    }

    /** Скорость подачи проволоки по сегменту — среднее по предзагруженным значениям. */
    private BigDecimal getWireFeedFromCache(List<WeldingMachineState> machineStates,
                                            LocalDateTime segmentStart, LocalDateTime segmentEnd,
                                            Map<Long, BigDecimal> wireFeedByStateId) {
        if (machineStates == null || wireFeedByStateId.isEmpty()) return null;
        List<BigDecimal> inSegment = new ArrayList<>();
        for (WeldingMachineState s : machineStates) {
            LocalDateTime t = s.getDateCreated();
            if (t != null && !t.isBefore(segmentStart) && t.isBefore(segmentEnd)) {
                BigDecimal v = wireFeedByStateId.get(s.getId());
                if (v != null) inSegment.add(v);
            }
        }
        if (inSegment.isEmpty()) return null;
        BigDecimal sum = inSegment.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(inSegment.size()), 1, RoundingMode.HALF_UP);
    }

    private List<String> getRfidCodesForWelder(Integer welderId) {
        List<String> codes = new ArrayList<>();
        Optional<Welder> welderOpt = welderRepository.findById(welderId.longValue());
        if (!welderOpt.isPresent()) return codes;
        Welder w = welderOpt.get();
        if (w.getRfidPasses() != null) {
            for (org.alloy.models.entities.RfidPass p : w.getRfidPasses()) {
                if (p.getCode() != null && !p.getCode().trim().isEmpty()) codes.add(p.getCode().trim());
            }
        }
        if (w.getRfidCode() != null && !w.getRfidCode().trim().isEmpty() && !codes.contains(w.getRfidCode().trim())) {
            codes.add(w.getRfidCode().trim());
        }
        return codes;
    }

    /**
     * Получает список ID сварщиков на основе шаблона
     */
    private Set<Integer> getSelectedWelderIds(WireConsumptionReportTemplateDTO template) {
        Set<Integer> welderIds = new HashSet<>();

        // Если выбраны конкретные сварщики
        if (template.getSelectedWelderIds() != null && !template.getSelectedWelderIds().isEmpty()) {
            welderIds.addAll(template.getSelectedWelderIds());
        }

        // Если выбраны подразделения
        if (template.getSelectedOrganizationUnitIds() != null && !template.getSelectedOrganizationUnitIds().isEmpty()) {
            for (Integer orgUnitId : template.getSelectedOrganizationUnitIds()) {
                if (orgUnitId == null) {
                    continue;
                }
                // Ищем подразделение по ID, затем находим всех Employee с этой связью
                Optional<OrganizationUnit> orgUnitOpt = organizationUnitRepository.findById(orgUnitId);
                if (!orgUnitOpt.isPresent()) {
                    continue;
                }
                // Используем специальный метод репозитория для поиска по Integer ID
                // Это более эффективно, чем findAll() с фильтрацией в памяти
                List<Employee> employees = employeeRepository.findByOrganizationUnitIdInteger(orgUnitId);
                for (Employee emp : employees) {
                    // Пробуем найти сварщика по employeeId
                    if (emp.getId() != null) {
                        // Ищем в таблице Welders по employeeId
                        Welder welder = welderRepository.findByEmployeeId(String.valueOf(emp.getId()));
                        if (welder != null) {
                            welderIds.add(welder.getId().intValue());
                        } else {
                            // Если не найден в Welders, но является сварщиком по типу, используем ID Employee
                            if (emp.getEmployeeType() != null && emp.getEmployeeType().name().equals("WELDER")) {
                                welderIds.add(emp.getId().intValue());
                            }
                        }
                    }
                }
            }
        }

        // Если ничего не выбрано, возвращаем всех сварщиков
        if (welderIds.isEmpty()) {
            List<Welder> allWelders = welderRepository.findAll();
            for (Welder welder : allWelders) {
                welderIds.add(welder.getId().intValue());
            }

            // Также добавляем сварщиков из Employee
            List<Employee> allEmployees = employeeRepository.findAll();
            for (Employee emp : allEmployees) {
                if (emp.getEmployeeType() != null) {
                    // Проверяем, является ли сотрудник сварщиком
                    String employeeTypeName = emp.getEmployeeType().name();
                    if (employeeTypeName.equals("WELDER")) {
                        welderIds.add(emp.getId().intValue());
                    }
                }
            }
        }

        return welderIds;
    }

    /**
     * Получает данные по расходу проволоки для конкретного сварщика из БД
     */
    private List<WireConsumptionReportDTO> getWireConsumptionDataForWelder(
            Integer welderId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            WireConsumptionReportTemplateDTO template) {

        List<WireConsumptionReportDTO> data = new ArrayList<>();

        try {
            // Получаем сварщика
            Optional<Welder> welderOpt = welderRepository.findById(welderId.longValue());
            if (!welderOpt.isPresent()) {
                // Пробуем найти через Employee
                Optional<Employee> employeeOpt = employeeRepository.findById(welderId.longValue());
                if (!employeeOpt.isPresent()) {
                    return data;
                }
                // Используем Employee как сварщика
                return getWireConsumptionDataForEmployee(employeeOpt.get(), startDateTime, endDateTime, template);
            }

            Welder welder = welderOpt.get();

            // Получаем все RFID коды сварщика (из rfidPasses и rfidCode для обратной совместимости)
            List<String> rfidCodes = new ArrayList<>();
            if (welder.getRfidPasses() != null && !welder.getRfidPasses().isEmpty()) {
                // Используем новые RFID пропуска
                for (org.alloy.models.entities.RfidPass pass : welder.getRfidPasses()) {
                    if (pass.getCode() != null && !pass.getCode().trim().isEmpty()) {
                        rfidCodes.add(pass.getCode().trim());
                    }
                }
            }
            // Обратная совместимость: если есть старый rfidCode, добавляем его
            if (welder.getRfidCode() != null && !welder.getRfidCode().trim().isEmpty()) {
                String oldRfid = welder.getRfidCode().trim();
                if (!rfidCodes.contains(oldRfid)) {
                    rfidCodes.add(oldRfid);
                }
            }

            // Получаем список выбранных моделей оборудования (названия аппаратов)
            List<String> selectedEquipmentModels = template.getSelectedEquipmentModels() != null
                    ? template.getSelectedEquipmentModels() : new ArrayList<>();

            // Если у сварщика есть RFID коды, ищем состояния по всем RFID
            if (!rfidCodes.isEmpty()) {
                // Находим все состояния машин с этими RFID за период
                List<WeldingMachineState> states = new ArrayList<>();
                Set<Long> seenStateIds = new HashSet<>();
                Set<Integer> foundMachineIds = new HashSet<>(); // Для отслеживания найденных аппаратов

                for (String rfid : rfidCodes) {
                    // Используем метод репозитория для фильтрации по дате на уровне БД (более эффективно)
                    // Ищем в поле rfid таблицы WeldingMachineState
                    List<WeldingMachineState> statesForRfid = weldingMachineStateRepository
                            .findByRfidAndDateRange(rfid, startDateTime, endDateTime);
                    for (WeldingMachineState s : statesForRfid) {
                        if (seenStateIds.add(s.getId())) {
                            states.add(s);
                        }
                    }

                    // Старые записи: RFID только в properties — раньше брали id за всю историю и делали findById на каждый (очень медленно).
                    // Теперь только id состояний в том же интервале дат + загрузка батчами.
                    List<Long> stateIdsWithRfid = parameterValueRepository.findStateIdsByRfidInPropertiesAndDateRange(
                            rfid, startDateTime, endDateTime);
                    if (!stateIdsWithRfid.isEmpty()) {
                        final int STATE_BATCH = 3000;
                        for (int i = 0; i < stateIdsWithRfid.size(); i += STATE_BATCH) {
                            int endIdx = Math.min(i + STATE_BATCH, stateIdsWithRfid.size());
                            List<Long> batchIds = stateIdsWithRfid.subList(i, endIdx);
                            for (WeldingMachineState state : weldingMachineStateRepository.findAllById(batchIds)) {
                                foundMachineIds.add(state.getWeldingMachineId());
                                if (seenStateIds.add(state.getId())) {
                                    states.add(state);
                                }
                            }
                        }
                    }

                    // Также находим все уникальные ID аппаратов, которые использовали этот RFID (даже если нет состояний за период)
                    List<Integer> machineIdsForRfid = weldingMachineStateRepository
                            .findDistinctWeldingMachineIdsByRfidCodes(Collections.singletonList(rfid));
                    foundMachineIds.addAll(machineIdsForRfid);
                }

                if (!states.isEmpty()) {
                    // Группируем по аппаратам
                    Map<Integer, List<WeldingMachineState>> statesByMachine = states.stream()
                            .collect(Collectors.groupingBy(WeldingMachineState::getWeldingMachineId));

                    // Для каждого аппарата создаем запись
                    for (Map.Entry<Integer, List<WeldingMachineState>> entry : statesByMachine.entrySet()) {
                        Integer machineId = entry.getKey();
                        List<WeldingMachineState> machineStates = entry.getValue();

                        Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(machineId);
                        if (!machineOpt.isPresent()) {
                            continue;
                        }

                        WeldingMachine machine = machineOpt.get();
                        String machineName = machine.getName();

                        // Фильтруем по выбранным моделям оборудования (названиям аппаратов)
                        if (!selectedEquipmentModels.isEmpty()) {
                            boolean isSelected = selectedEquipmentModels.stream()
                                    .anyMatch(selected -> selected != null && selected.equals(machineName));

                            if (!isSelected) {
                                continue;
                            }
                        }

                        WireConsumptionReportDTO item = calculateWireConsumptionForMachine(
                                welder, machine, machineStates, startDateTime, endDateTime, template);

                        if (item != null) {
                            data.add(item);
                        }
                    }
                } else if (!foundMachineIds.isEmpty()) {
                    // Если нет состояний за период, но есть аппараты, которые использовали RFID коды,
                    // создаем записи с наименованием оборудования, но без данных
                    // ВАЖНО: Если selectedEquipmentModels пустой (модели не выбраны вручную),
                    // показываем ВСЕ аппараты, найденные по RFID кодам сварщика
                    for (Integer machineId : foundMachineIds) {
                        Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(machineId);
                        if (!machineOpt.isPresent()) {
                            continue;
                        }

                        WeldingMachine machine = machineOpt.get();
                        String machineName = machine.getName();

                        // Фильтруем по выбранным моделям оборудования (названиям аппаратов), если они указаны
                        // Если модели НЕ выбраны (selectedEquipmentModels пустой), показываем ВСЕ аппараты, найденные по RFID
                        if (!selectedEquipmentModels.isEmpty()) {
                            boolean isSelected = selectedEquipmentModels.stream()
                                    .anyMatch(selected -> selected != null && selected.equals(machineName));

                            if (!isSelected) {
                                continue; // Пропускаем, если модель не выбрана вручную
                            }
                        }
                        // Если selectedEquipmentModels пустой, НЕ пропускаем - показываем все найденные по RFID

                        // Создаем запись с наименованием оборудования, но без данных за период
                        WireConsumptionReportDTO item = calculateWireConsumptionForMachine(
                                welder, machine, new ArrayList<>(), startDateTime, endDateTime, template);

                        if (item != null) {
                            data.add(item);
                        }
                    }
                }
            }

            // Если данных нет (нет RFID или нет состояний по RFID), получаем данные по выбранным аппаратам
            if (data.isEmpty() && !selectedEquipmentModels.isEmpty()) {
                for (String selectedModelName : selectedEquipmentModels) {
                    // Ищем аппарат с таким названием (оптимизированный запрос)
                    List<WeldingMachine> machines = weldingMachineRepository.findByNames(Collections.singletonList(selectedModelName));
                    if (!machines.isEmpty()) {
                        WeldingMachine machine = machines.get(0);

                        // Находим все состояния этого аппарата за период (независимо от RFID)
                        // Используем метод репозитория для фильтрации по дате на уровне БД (более эффективно)
                        List<WeldingMachineState> machineStates = weldingMachineStateRepository
                                .findByWeldingMachineIdAndDateRange(machine.getId(), startDateTime, endDateTime);

                        if (!machineStates.isEmpty()) {
                            WireConsumptionReportDTO item = calculateWireConsumptionForMachine(
                                    welder, machine, machineStates, startDateTime, endDateTime, template);

                            if (item != null) {
                                data.add(item);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка получения данных для сварщика " + welderId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Получает данные для Employee (если сварщик не найден в таблице Welders)
     */
    private List<WireConsumptionReportDTO> getWireConsumptionDataForEmployee(
            Employee employee,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            WireConsumptionReportTemplateDTO template) {

        List<WireConsumptionReportDTO> data = new ArrayList<>();

        // Для Employee пока возвращаем пустой список или используем моковые данные
        // В реальной системе нужно связать Employee с Welder или использовать другую логику

        return data;
    }

    /**
     * Создает пустые записи с нулевыми значениями для сварщика, у которого нет данных
     * Если в шаблоне выбраны модели оборудования, создает запись для каждого выбранного аппарата
     */
    private List<WireConsumptionReportDTO> createEmptyRecordsForWelder(Integer welderId, WireConsumptionReportTemplateDTO template) {
        List<WireConsumptionReportDTO> records = new ArrayList<>();

        try {
            // Получаем базовую информацию о сварщике
            WireConsumptionReportDTO baseRecord = createEmptyRecordForWelder(welderId);
            if (baseRecord == null) {
                return records;
            }

            // Получаем список выбранных моделей оборудования (названия аппаратов)
            List<String> selectedEquipmentModels = template.getSelectedEquipmentModels() != null
                    ? template.getSelectedEquipmentModels() : new ArrayList<>();

            // Если выбраны модели оборудования, создаем запись для каждого выбранного аппарата
            if (!selectedEquipmentModels.isEmpty()) {
                for (String selectedModelName : selectedEquipmentModels) {
                    // Ищем аппарат с таким названием (оптимизированный запрос)
                    List<WeldingMachine> machines = weldingMachineRepository.findByNames(Collections.singletonList(selectedModelName));
                    if (!machines.isEmpty()) {
                        WeldingMachine machine = machines.get(0);

                        WireConsumptionReportDTO record = new WireConsumptionReportDTO();
                        // Копируем базовую информацию о сварщике
                        record.setWelderId(baseRecord.getWelderId());
                        record.setWelderName(baseRecord.getWelderName());
                        record.setTabNumber(baseRecord.getTabNumber());
                        record.setProfession(baseRecord.getProfession());
                        record.setOrganizationUnitId(baseRecord.getOrganizationUnitId());
                        record.setOrganizationUnitName(baseRecord.getOrganizationUnitName());

                        // Заполняем информацию об оборудовании
                        record.setWeldingMachineId(machine.getId());
                        record.setWeldingMachineName(machine.getName()); // Наименование оборудования
                        record.setEquipmentModel(machine.getDeviceModel() != null ? machine.getDeviceModel().name() : ""); // Модель оборудования

                        // Пытаемся получить проволоку из последних состояний аппарата (даже если нет данных за период)
                        String wire = getWireFromLastStates(machine.getId());
                        record.setWire(wire != null && !wire.isEmpty() ? wire : "0");

                        // Все остальные поля - нули
                        record.setTimeInNetwork(Duration.ZERO);
                        record.setArcBurningTime(Duration.ZERO);
                        record.setEquipmentEfficiency(BigDecimal.ZERO);
                        record.setTimeOutsideSetCurrentRange(Duration.ZERO);
                        record.setTimeOutsideActualCurrentRange(Duration.ZERO);
                        record.setEnergyConsumed(BigDecimal.ZERO);
                        record.setWireConsumption(BigDecimal.ZERO);

                        records.add(record);
                    }
                }
            } else {
                // Если модели не выбраны, создаем одну запись без информации об оборудовании
                records.add(baseRecord);
            }

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка создания пустых записей для сварщика " + welderId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return records;
    }

    /**
     * Создает базовую пустую запись с нулевыми значениями для сварщика (без информации об оборудовании)
     */
    private WireConsumptionReportDTO createEmptyRecordForWelder(Integer welderId) {
        try {
            // Пробуем найти сварщика
            Optional<Welder> welderOpt = welderRepository.findById(welderId.longValue());
            if (!welderOpt.isPresent()) {
                // Пробуем найти через Employee
                Optional<Employee> employeeOpt = employeeRepository.findById(welderId.longValue());
                if (!employeeOpt.isPresent()) {
                    return null;
                }
                Employee employee = employeeOpt.get();
                WireConsumptionReportDTO dto = new WireConsumptionReportDTO();
                dto.setWelderId(employee.getId().intValue());
                dto.setWelderName(employee.getFullName() != null ? employee.getFullName() : "");
                dto.setTabNumber(String.valueOf(employee.getId()));
                dto.setProfession(employee.getPosition() != null ? employee.getPosition() : "Электросварщик");
                if (employee.getOrganizationUnit() != null) {
                    dto.setOrganizationUnitId(employee.getOrganizationUnit().getId());
                    dto.setOrganizationUnitName(employee.getOrganizationUnit().getName());
                }
                // Все остальные поля - нули
                dto.setTimeInNetwork(Duration.ZERO);
                dto.setArcBurningTime(Duration.ZERO);
                dto.setEquipmentEfficiency(BigDecimal.ZERO);
                dto.setTimeOutsideSetCurrentRange(Duration.ZERO);
                dto.setTimeOutsideActualCurrentRange(Duration.ZERO);
                dto.setEnergyConsumed(BigDecimal.ZERO);
                dto.setWire("0");
                dto.setWireConsumption(BigDecimal.ZERO);
                return dto;
            }

            Welder welder = welderOpt.get();
            WireConsumptionReportDTO dto = new WireConsumptionReportDTO();
            dto.setWelderId(welder.getId().intValue());
            dto.setWelderName(welder.getName());
            dto.setTabNumber(welder.getEmployeeId());
            dto.setProfession(welder.getPosition() != null ? welder.getPosition() : "Электросварщик");
            // Welder имеет department как строку, а не связь с OrganizationUnit
            if (welder.getDepartment() != null) {
                dto.setOrganizationUnitName(welder.getDepartment());
            }
            // Все остальные поля - нули
            dto.setTimeInNetwork(Duration.ZERO);
            dto.setArcBurningTime(Duration.ZERO);
            dto.setEquipmentEfficiency(BigDecimal.ZERO);
            dto.setTimeOutsideSetCurrentRange(Duration.ZERO);
            dto.setTimeOutsideActualCurrentRange(Duration.ZERO);
            dto.setEnergyConsumed(BigDecimal.ZERO);
            dto.setWire("0");
            dto.setWireConsumption(BigDecimal.ZERO);
            return dto;

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка создания пустой записи для сварщика " + welderId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Создает записи для выбранных аппаратов без сварщика
     */
    private List<WireConsumptionReportDTO> createRecordsForEquipmentWithoutWelder(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            WireConsumptionReportTemplateDTO template) {

        List<WireConsumptionReportDTO> result = new ArrayList<>();

        try {
            List<String> selectedEquipmentModels = template.getSelectedEquipmentModels();
            if (selectedEquipmentModels == null || selectedEquipmentModels.isEmpty()) {
                return result;
            }

            for (String selectedModelName : selectedEquipmentModels) {
                // Ищем аппарат с таким названием (оптимизированный запрос)
                List<WeldingMachine> machines = weldingMachineRepository.findByNames(Collections.singletonList(selectedModelName));
                if (!machines.isEmpty()) {
                    WeldingMachine machine = machines.get(0);

                    // Находим все состояния этого аппарата за период
                    // Используем метод репозитория для фильтрации по дате на уровне БД (более эффективно)
                    List<WeldingMachineState> machineStates = weldingMachineStateRepository
                            .findByWeldingMachineIdAndDateRange(machine.getId(), startDateTime, endDateTime);

                    if (!machineStates.isEmpty()) {
                        // Создаем запись без сварщика
                        WireConsumptionReportDTO dto = calculateWireConsumptionForMachine(
                                null, machine, machineStates, startDateTime, endDateTime, template);

                        if (dto != null) {
                            result.add(dto);
                        }
                    } else {
                        // Создаем пустую запись для аппарата без данных
                        WireConsumptionReportDTO emptyRecord = createEmptyRecordForEquipment(machine, template);
                        if (emptyRecord != null) {
                            result.add(emptyRecord);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка создания записей для аппаратов без сварщика: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Создает пустую запись для аппарата без сварщика
     */
    private WireConsumptionReportDTO createEmptyRecordForEquipment(
            WeldingMachine machine,
            WireConsumptionReportTemplateDTO template) {

        try {
            WireConsumptionReportDTO dto = new WireConsumptionReportDTO();

            // Поля сварщика оставляем пустыми
            dto.setWelderId(null);
            dto.setWelderName(null);
            dto.setTabNumber(null);
            dto.setProfession(null);

            // Заполняем информацию об оборудовании
            dto.setWeldingMachineId(machine.getId());
            dto.setWeldingMachineName(machine.getName());
            dto.setEquipmentModel(machine.getDeviceModel() != null ? machine.getDeviceModel().name() : "");

            // Подразделение берем из машины
            if (machine.getOrganizationUnit() != null) {
                dto.setOrganizationUnitId(machine.getOrganizationUnit().getId());
                dto.setOrganizationUnitName(machine.getOrganizationUnit().getName());
            }

            // Пытаемся получить проволоку из последних состояний аппарата
            String wire = getWireFromLastStates(machine.getId());
            dto.setWire(wire != null && !wire.isEmpty() ? wire : "0");

            // Все остальные поля - нули
            dto.setTimeInNetwork(Duration.ZERO);
            dto.setArcBurningTime(Duration.ZERO);
            dto.setEquipmentEfficiency(BigDecimal.ZERO);
            dto.setTimeOutsideSetCurrentRange(Duration.ZERO);
            dto.setTimeOutsideActualCurrentRange(Duration.ZERO);
            dto.setEnergyConsumed(BigDecimal.ZERO);
            dto.setWireConsumption(BigDecimal.ZERO);

            return dto;
        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка создания пустой записи для аппарата: " + e.getMessage());
            return null;
        }
    }

    /**
     * Рассчитывает расход проволоки для конкретного аппарата
     */
    private WireConsumptionReportDTO calculateWireConsumptionForMachine(
            Welder welder,
            WeldingMachine machine,
            List<WeldingMachineState> states,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            WireConsumptionReportTemplateDTO template) {

        try {
            WireConsumptionReportDTO dto = new WireConsumptionReportDTO();

            // Базовая информация о сварщике (может быть null, если сварщика нет)
            if (welder != null) {
                dto.setWelderId(welder.getId().intValue());
                dto.setWelderName(welder.getName());
                dto.setTabNumber(welder.getEmployeeId());
                dto.setProfession(welder.getPosition() != null ? welder.getPosition() : "Электросварщик");
            } else {
                // Если сварщика нет, поля остаются null
                dto.setWelderId(null);
                dto.setWelderName(null);
                dto.setTabNumber(null);
                dto.setProfession(null);
            }
            dto.setWeldingMachineId(machine.getId());
            // Наименование оборудования - это название аппарата, выбранное в шаблоне
            dto.setWeldingMachineName(machine.getName());
            // Модель оборудования - это модель устройства (deviceModel), соответствующая этому названию
            dto.setEquipmentModel(machine.getDeviceModel() != null ? machine.getDeviceModel().name() : "");

            // Подразделение берем из машины, если не указано - из сварщика (если сварщик есть)
            if (machine.getOrganizationUnit() != null) {
                dto.setOrganizationUnitId(machine.getOrganizationUnit().getId());
                dto.setOrganizationUnitName(machine.getOrganizationUnit().getName());
            } else if (welder != null && welder.getDepartment() != null) {
                dto.setOrganizationUnitName(welder.getDepartment());
            }

            // Собираем ID состояний
            List<Long> stateIds = states.stream()
                    .map(WeldingMachineState::getId)
                    .collect(Collectors.toList());

            Map<Long, BigDecimal> wireFeedByStateId = new HashMap<>();
            loadWireFeedByStateId(stateIds, wireFeedByStateId);

            // Получаем параметры (разбиваем на батчи, если список слишком большой)
            // PostgreSQL не поддерживает более 32767 параметров в одном запросе
            List<WeldingMachineParameterValue> currentValues = getParameterValuesInBatches(stateIds, "Current");
            List<WeldingMachineParameterValue> voltageValues = getParameterValuesInBatches(stateIds, "Voltage");
            List<WeldingMachineParameterValue> wireMaterialValues = getParameterValuesInBatches(stateIds, "Материал проволоки");
            List<WeldingMachineParameterValue> wireDiameterValues = getParameterValuesInBatches(stateIds, "Диаметр проволоки");

            // Время в сети = время авторизованного состояния сварщика на данном аппарате за период:
            // по пропускам сварщика находим активность (состояния с его RFID на этом аппарате),
            // группируем в сессии (близкие по времени состояния — одна сессия), суммируем длительности.
            Duration totalTimeInNetwork = calculateAuthorizedTimeOnMachine(states);
            dto.setTimeInNetwork(totalTimeInNetwork);

            // Рассчитываем время горения дуги (только когда идет сварка - статус Welding)
            Duration arcBurningTime = calculateArcBurningTime(states, currentValues);
            dto.setArcBurningTime(arcBurningTime);

            // Эффективность использования оборудования (%)
            // Эффективность = (время горения дуги / время в сети) * 100
            // Пример: время в сети = 8ч, время горения дуги = 4ч, эффективность = 4/8*100 = 50%
            if (totalTimeInNetwork.toSeconds() > 0) {
                double efficiency = 0.0;
                if (arcBurningTime.toSeconds() > 0) {
                    efficiency = (double) arcBurningTime.toSeconds() / totalTimeInNetwork.toSeconds() * 100.0;
                }
                dto.setEquipmentEfficiency(BigDecimal.valueOf(efficiency).setScale(2, RoundingMode.HALF_UP));
            } else {
                dto.setEquipmentEfficiency(BigDecimal.ZERO);
            }

            // Время работы вне диапазона тока
            Duration timeOutsideSetCurrent = calculateTimeOutsideCurrentRange(
                    states, currentValues, template.getSetCurrentMin(), template.getSetCurrentMax(), true);
            Duration timeOutsideActualCurrent = calculateTimeOutsideCurrentRange(
                    states, currentValues, template.getActualCurrentMin(), template.getActualCurrentMax(), false);
            dto.setTimeOutsideSetCurrentRange(timeOutsideSetCurrent);
            dto.setTimeOutsideActualCurrentRange(timeOutsideActualCurrent);

            // Затраченная энергия (расчет: средний ток * среднее напряжение / 1000 * время в сети)
            BigDecimal energyConsumed = calculateEnergyConsumed(voltageValues, currentValues, totalTimeInNetwork);
            dto.setEnergyConsumed(energyConsumed);

            // Проволока (комбинация материала и диаметра из параметров состояния)
            String wire = getWireFromStates(wireMaterialValues, wireDiameterValues, states);
            dto.setWire(wire != null && !wire.isEmpty() ? wire : "0");

            dto.setWireConsumption(calculateWireConsumptionKgFromWeldingStates(states, wireFeedByStateId));

            return dto;

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка расчета для аппарата " + (machine != null ? machine.getName() : "null") + " (ID=" + (machine != null ? machine.getId() : "null") + "): " + e.getMessage());
            System.err.println("[REPORT-DATA] ❌ Stack trace:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Рассчитывает время авторизованного состояния сварщика на указанном аппарате за период.
     * Состояния уже отфильтрованы по RFID сварщика и по аппарату (передаются в calculateWireConsumptionForMachine).
     * Группируем состояния в сессии: состояния с небольшим разрывом по времени (≤ 10 мин) — одна сессия.
     * Время в сети = сумма длительностей сессий (сумма stateDurationMs по сессии или span сессии, если duration не задан).
     */
    private Duration calculateAuthorizedTimeOnMachine(List<WeldingMachineState> states) {
        if (states == null || states.isEmpty()) {
            return Duration.ZERO;
        }
        List<WeldingMachineState> sorted = new ArrayList<>(states);
        sorted.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
        final long maxGapMs = 10 * 60 * 1000L; // 10 минут — один «выход» не разрывает сессию
        long totalMs = 0;
        int i = 0;
        while (i < sorted.size()) {
            WeldingMachineState first = sorted.get(i);
            LocalDateTime sessionStart = first.getDateCreated();
            long sessionEndMs = sessionStart.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long sessionDurationFromStates = 0;
            int j = i;
            while (j < sorted.size()) {
                WeldingMachineState s = sorted.get(j);
                long stateDur = (s.getStateDurationMs() != null && s.getStateDurationMs() > 0)
                        ? s.getStateDurationMs() : 0;
                sessionDurationFromStates += stateDur;
                LocalDateTime stateEnd = s.getDateCreated().plus(stateDur, ChronoUnit.MILLIS);
                long stateEndMs = stateEnd.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (stateEndMs > sessionEndMs) sessionEndMs = stateEndMs;
                if (j + 1 >= sorted.size()) break;
                WeldingMachineState next = sorted.get(j + 1);
                long gapMs = next.getDateCreated().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - sessionEndMs;
                if (gapMs > maxGapMs) break;
                j++;
            }
            if (sessionDurationFromStates > 0) {
                totalMs += sessionDurationFromStates;
            } else {
                long spanMs = sessionEndMs - sessionStart.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (spanMs <= 0 && j > i) {
                    LocalDateTime lastInSession = sorted.get(j).getDateCreated();
                    spanMs = java.time.Duration.between(sessionStart, lastInSession).toMillis();
                }
                if (spanMs > 0) totalMs += spanMs;
                else totalMs += 1000; // одно состояние без duration — 1 сек
            }
            i = j + 1;
        }
        return Duration.ofMillis(totalMs);
    }

    /**
     * Получает параметры значений для большого списка ID состояний, разбивая на батчи
     * PostgreSQL не поддерживает более 32767 параметров в одном запросе
     */
    private List<WeldingMachineParameterValue> getParameterValuesInBatches(List<Long> stateIds, String propertyCode) {
        if (stateIds == null || stateIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Максимальное количество параметров в одном запросе (оставляем запас)
        // Увеличиваем размер батча для уменьшения количества запросов к БД
        // PostgreSQL поддерживает до 32767 параметров, используем 10000 для баланса между производительностью и надежностью
        final int BATCH_SIZE = 10000;

        // Всегда обрабатываем в батчах, даже для небольших списков, чтобы избежать зависаний
        List<WeldingMachineParameterValue> allValues = new ArrayList<>();

        // Разбиваем список на батчи
        for (int i = 0; i < stateIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, stateIds.size());
            List<Long> batch = stateIds.subList(i, endIndex);

            try {
                List<WeldingMachineParameterValue> batchValues = parameterValueRepository
                        .findByStateIdsAndPropertyCode(batch, propertyCode);
                allValues.addAll(batchValues);
            } catch (Exception e) {
                System.err.println("[REPORT-DATA] ⚠️ Ошибка получения параметров для батча (" + i + "-" + endIndex + "): " + e.getMessage());
                // Продолжаем обработку следующих батчей
            }
        }

        return allValues;
    }

    /**
     * Скорость подачи проволоки (м/мин) по stateId: сначала JPA (Value и при пустом — RawValue),
     * затем нативный запрос COALESCE(value, raw_value) для недостающих id (совместимость со схемой БД).
     */
    private void loadWireFeedByStateId(List<Long> stateIds, Map<Long, BigDecimal> wireFeedByStateId) {
        if (stateIds == null || stateIds.isEmpty()) return;
        List<WeldingMachineParameterValue> jpaRows = getParameterValuesInBatches(stateIds, "Расход проволоки");
        for (WeldingMachineParameterValue pv : jpaRows) {
            if (pv == null || pv.getWeldingMachineStateId() == null) continue;
            String s = pv.getValue();
            if (s == null || s.trim().isEmpty()) s = pv.getRawValue();
            if (s == null || s.trim().isEmpty()) continue;
            try {
                wireFeedByStateId.put(pv.getWeldingMachineStateId(),
                        new BigDecimal(s.trim().replace(",", ".")));
            } catch (NumberFormatException ignored) { }
        }
        List<Long> missing = stateIds.stream()
                .filter(id -> !wireFeedByStateId.containsKey(id))
                .collect(Collectors.toList());
        if (missing.isEmpty()) return;
        final int BATCH_SIZE = 10_000;
        for (int i = 0; i < missing.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, missing.size());
            List<Long> batch = missing.subList(i, endIndex);
            try {
                List<Object[]> rows = parameterValueRepository.findStateIdAndValueNativeCoalesce(batch, "Расход проволоки");
                for (Object[] row : rows) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        try {
                            long stateId = ((Number) row[0]).longValue();
                            String valueStr = row[1].toString().trim().replace(",", ".");
                            if (valueStr.isEmpty()) continue;
                            wireFeedByStateId.put(stateId, new BigDecimal(valueStr));
                        } catch (NumberFormatException ignored) { }
                    }
                }
            } catch (Exception e) {
                System.err.println("[REPORT-DATA] loadWireFeedByStateId native батч " + i + "-" + endIndex + ": " + e.getMessage());
            }
        }
    }

    /** Мгновенный расход газа (л/мин) по stateId — State.GasFlow / GasFlow / gasFlow. */
    private void loadGasFlowLpmByStateId(List<Long> stateIds, Map<Long, BigDecimal> gasFlowLpmByStateId) {
        if (stateIds == null || stateIds.isEmpty() || gasFlowLpmByStateId == null) return;
        for (String code : GAS_FLOW_PROPERTY_CODES) {
            List<Long> missing = stateIds.stream()
                    .filter(id -> !gasFlowLpmByStateId.containsKey(id))
                    .collect(Collectors.toList());
            if (missing.isEmpty()) break;
            List<WeldingMachineParameterValue> jpaRows = getParameterValuesInBatches(missing, code);
            for (WeldingMachineParameterValue pv : jpaRows) {
                if (pv == null || pv.getWeldingMachineStateId() == null) continue;
                String s = pv.getValue();
                if (s == null || s.trim().isEmpty()) s = pv.getRawValue();
                if (s == null || s.trim().isEmpty()) continue;
                try {
                    gasFlowLpmByStateId.putIfAbsent(pv.getWeldingMachineStateId(),
                            new BigDecimal(s.trim().replace(",", ".")));
                } catch (NumberFormatException ignored) { }
            }
            missing = stateIds.stream()
                    .filter(id -> !gasFlowLpmByStateId.containsKey(id))
                    .collect(Collectors.toList());
            if (missing.isEmpty()) break;
            final int batchSize = 10_000;
            for (int i = 0; i < missing.size(); i += batchSize) {
                List<Long> batch = missing.subList(i, Math.min(i + batchSize, missing.size()));
                try {
                    List<Object[]> rows = parameterValueRepository.findStateIdAndValueNativeCoalesce(batch, code);
                    for (Object[] row : rows) {
                        if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
                        try {
                            long stateId = ((Number) row[0]).longValue();
                            String valueStr = row[1].toString().trim().replace(",", ".");
                            if (valueStr.isEmpty()) continue;
                            gasFlowLpmByStateId.putIfAbsent(stateId, new BigDecimal(valueStr));
                        } catch (NumberFormatException ignored) { }
                    }
                } catch (Exception e) {
                    System.err.println("[REPORT-DATA] loadGasFlowLpmByStateId " + code + " batch " + i + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Получает проволоку из последних состояний аппарата (даже если нет данных за период)
     */
    private String getWireFromLastStates(Integer machineId) {
        try {
            // Получаем последние 10 состояний аппарата (оптимизированный запрос через БД)
            List<WeldingMachineState> lastStates = weldingMachineStateRepository
                    .findByWeldingMachineId(machineId).stream()
                    .sorted((s1, s2) -> s2.getDateCreated().compareTo(s1.getDateCreated()))
                    .limit(10)
                    .collect(Collectors.toList());

            if (lastStates.isEmpty()) {
                return null;
            }

            // Собираем ID состояний
            List<Long> stateIds = lastStates.stream()
                    .map(WeldingMachineState::getId)
                    .collect(Collectors.toList());

            // Получаем параметры проволоки (разбиваем на батчи, если список слишком большой)
            List<WeldingMachineParameterValue> wireMaterialValues = getParameterValuesInBatches(stateIds, "Материал проволоки");
            List<WeldingMachineParameterValue> wireDiameterValues = getParameterValuesInBatches(stateIds, "Диаметр проволоки");

            return getWireFromStates(wireMaterialValues, wireDiameterValues, lastStates);
        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка получения проволоки из последних состояний: " + e.getMessage());
            return null;
        }
    }

    /**
     * Получает проволоку (материал и диаметр) из состояний
     */
    private String getWireFromStates(List<WeldingMachineParameterValue> wireMaterialValues,
                                     List<WeldingMachineParameterValue> wireDiameterValues,
                                     List<WeldingMachineState> states) {
        String material = null;
        String diameter = null;

        // Получаем материал проволоки
        if (wireMaterialValues != null && !wireMaterialValues.isEmpty()) {
            for (WeldingMachineParameterValue paramValue : wireMaterialValues) {
                if (paramValue.getValue() != null && !paramValue.getValue().trim().isEmpty()) {
                    material = paramValue.getValue();
                    break;
                }
            }
        }

        // Получаем диаметр проволоки
        if (wireDiameterValues != null && !wireDiameterValues.isEmpty()) {
            for (WeldingMachineParameterValue paramValue : wireDiameterValues) {
                if (paramValue.getValue() != null && !paramValue.getValue().trim().isEmpty()) {
                    diameter = paramValue.getValue();
                    break;
                }
            }
        }

        // Комбинируем материал и диаметр
        if (material != null && diameter != null) {
            return diameter + " " + material;
        } else if (diameter != null) {
            return diameter;
        } else if (material != null) {
            return material;
        }

        return null;
    }

    /**
     * Рассчитывает время горения дуги (время, когда идет сварка)
     * Сварка определяется ТОЛЬКО по статусу состояния (Welding)
     * Не проверяем ток, так как аппарат может быть включен с установленным током, но не варить
     */
    private Duration calculateArcBurningTime(List<WeldingMachineState> states,
                                             List<WeldingMachineParameterValue> currentValues) {
        if (states == null || states.isEmpty()) {
            return Duration.ZERO;
        }

        // Фильтруем состояния со статусом Welding
        List<WeldingMachineState> weldingStates = states.stream()
                .filter(state -> state.getWeldingMachineStatus() != null &&
                        state.getWeldingMachineStatus() == org.alloy.models.WeldingMachineStatus.Welding)
                .sorted((s1, s2) -> s1.getDateCreated().compareTo(s2.getDateCreated()))
                .collect(Collectors.toList());

        if (weldingStates.isEmpty()) {
            return Duration.ZERO;
        }

        // Если stateDurationMs = 0 для всех состояний, используем интервалы между последовательными состояниями
        boolean allDurationsZero = weldingStates.stream()
                .allMatch(s -> s.getStateDurationMs() == null || s.getStateDurationMs() == 0L);

        if (allDurationsZero) {
            if (weldingStates.size() > 1) {
                // Рассчитываем время как сумму интервалов между последовательными состояниями
                // Но только если промежуток между состояниями не слишком большой (максимум 5 минут = 300000 мс)
                // Если промежуток больше, значит это разные сеансы сварки, и мы не учитываем этот промежуток
                // Для последнего состояния используем средний интервал между состояниями
                long totalMs = 0;
                final long MAX_GAP_MS = 5 * 60 * 1000; // 5 минут

                // Сначала считаем сумму всех промежутков, которые меньше MAX_GAP_MS
                long sumOfSmallGaps = 0;
                int countOfSmallGaps = 0;

                for (int i = 1; i < weldingStates.size(); i++) {
                    LocalDateTime prevState = weldingStates.get(i - 1).getDateCreated();
                    LocalDateTime currentState = weldingStates.get(i).getDateCreated();
                    long gapMs = java.time.Duration.between(prevState, currentState).toMillis();

                    // Если промежуток небольшой (в пределах 5 минут), считаем это частью одного сеанса
                    if (gapMs <= MAX_GAP_MS) {
                        sumOfSmallGaps += gapMs;
                        countOfSmallGaps++;
                    }
                    // Если промежуток большой, игнорируем его (это разные сеансы сварки)
                }

                // Если есть промежутки, используем их сумму
                if (countOfSmallGaps > 0) {
                    totalMs = sumOfSmallGaps;

                    // Для последнего состояния добавляем средний интервал между состояниями
                    // (так как у последнего состояния нет следующего состояния)
                    long avgGap = sumOfSmallGaps / countOfSmallGaps;
                    totalMs += avgGap;
                } else {
                    // Если все промежутки были большими, используем минимальное значение для каждого состояния
                    totalMs = weldingStates.size() * 1000; // 1 секунда на каждое состояние
                }

                return Duration.ofMillis(totalMs);
            } else if (weldingStates.size() == 1) {
                // Если только одно состояние, используем минимальное значение (например, 1 секунда)
                return Duration.ofSeconds(1);
            }
        } else {
            // Используем stateDurationMs, если они есть
            long totalMs = 0;
            for (WeldingMachineState state : weldingStates) {
                if (state.getStateDurationMs() != null && state.getStateDurationMs() > 0) {
                    totalMs += state.getStateDurationMs();
                }
            }
            return Duration.ofMillis(totalMs);
        }

        return Duration.ZERO;
    }

    /**
     * Проверяет, является ли значение активной сваркой (ток > 0)
     */
    private boolean isActiveWelding(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            int intValue;
            try {
                intValue = Integer.parseInt(value, 16);
            } catch (NumberFormatException e) {
                intValue = Integer.parseInt(value, 10);
            }
            return intValue > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Рассчитывает время работы вне диапазона тока
     */
    private Duration calculateTimeOutsideCurrentRange(List<WeldingMachineState> states,
                                                      List<WeldingMachineParameterValue> currentValues,
                                                      Integer minCurrent,
                                                      Integer maxCurrent,
                                                      boolean isSetCurrent) {
        if (minCurrent == null || maxCurrent == null) {
            return Duration.ZERO;
        }

        long totalSeconds = 0;

        // Оптимизация: создаем Map для быстрого поиска параметров по stateId
        Map<Long, List<WeldingMachineParameterValue>> currentValuesByStateId = currentValues.stream()
                .collect(Collectors.groupingBy(WeldingMachineParameterValue::getWeldingMachineStateId));

        for (WeldingMachineState state : states) {
            List<WeldingMachineParameterValue> stateCurrents = currentValuesByStateId.getOrDefault(state.getId(), Collections.emptyList());

            boolean isOutsideRange = stateCurrents.stream()
                    .anyMatch(pv -> {
                        int current = parseCurrentValue(pv.getValue());
                        return current < minCurrent || current > maxCurrent;
                    });

            if (isOutsideRange) {
                totalSeconds += state.getStateDurationMs() != null ? state.getStateDurationMs() / 1000 : 0;
            }
        }

        return Duration.ofSeconds(totalSeconds);
    }

    /**
     * Парсит значение тока/напряжения (как в отчёте по расходу проволоки).
     * Поддерживает целые и десятичные строки ("50", "50.0", "13.9", "139") для CORE и блоков мониторинга.
     */
    private int parseCurrentValue(String value) {
        if (value == null || (value = value.trim()).isEmpty()) {
            return 0;
        }
        if (value.contains(".") || value.contains(",")) {
            try {
                String normalized = value.replace(',', '.');
                return (int) Math.round(Double.parseDouble(normalized));
            } catch (NumberFormatException e) {
                // fallback ниже
            }
        }
        try {
            try {
                return Integer.parseInt(value, 16);
            } catch (NumberFormatException e) {
                return Integer.parseInt(value, 10);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Рассчитывает затраченную энергию (кВт*ч)
     * Формула: средний ток * среднее напряжение / 1000 (кВт, округляем до 1 знака) * время в сети (ч)
     * Пример: ток=235А, напряжение=17.5В, время=8ч
     * 235 * 17.5 = 4112.5 Вт
     * 4112.5 / 1000 = 4.1125 кВт -> округляем до 4.1 кВт
     * 4.1 * 8 = 32.8 кВт*ч
     */
    private BigDecimal calculateEnergyConsumed(List<WeldingMachineParameterValue> voltageValues,
                                               List<WeldingMachineParameterValue> currentValues,
                                               Duration timeInNetwork) {
        if (voltageValues.isEmpty() || currentValues.isEmpty() || timeInNetwork.toSeconds() == 0) {
            return BigDecimal.ZERO;
        }

        // Рассчитываем среднее напряжение и средний ток
        BigDecimal avgVoltage = calculateAverage(voltageValues);
        BigDecimal avgCurrent = calculateAverage(currentValues);

        // P = U * I (Вт)
        BigDecimal powerWatts = avgVoltage.multiply(avgCurrent);

        // Переводим в кВт и округляем до 1 знака после запятой
        BigDecimal powerKilowatts = powerWatts.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP);

        // Переводим время в сети в часы
        BigDecimal timeHours = BigDecimal.valueOf(timeInNetwork.toSeconds())
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);

        // E (кВт*ч) = P (кВт) * t (ч)
        BigDecimal energyConsumed = powerKilowatts.multiply(timeHours);

        // Округляем результат до 1 знака после запятой
        return energyConsumed.setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Рассчитывает среднее значение параметра
     */
    private BigDecimal calculateAverage(List<WeldingMachineParameterValue> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> numericValues = values.stream()
                .map(pv -> BigDecimal.valueOf(parseCurrentValue(pv.getValue())))
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (numericValues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = numericValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(numericValues.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Группирует данные по сварщикам и сортирует по суммарным значениям
     */
    private List<WireConsumptionReportDTO> groupAndSortWireConsumptionData(
            Map<Integer, List<WireConsumptionReportDTO>> welderDataMap,
            WireConsumptionReportTemplateDTO template) {

        List<WireConsumptionReportDTO> result = new ArrayList<>();

        // Вычисляем суммарные значения для каждого сварщика
        Map<Integer, WireConsumptionReportDTO> summaryMap = new HashMap<>();

        for (Map.Entry<Integer, List<WireConsumptionReportDTO>> entry : welderDataMap.entrySet()) {
            Integer welderId = entry.getKey();
            List<WireConsumptionReportDTO> welderData = entry.getValue();

            // Создаем суммарную строку
            WireConsumptionReportDTO summary = createSummaryRow(welderData);
            summaryMap.put(welderId, summary);
        }

        // Сортируем сварщиков по выбранной колонке
        List<Integer> sortedWelderIds = new ArrayList<>(summaryMap.keySet());
        sortedWelderIds.sort((id1, id2) -> {
            WireConsumptionReportDTO s1 = summaryMap.get(id1);
            WireConsumptionReportDTO s2 = summaryMap.get(id2);
            return compareByColumn(s1, s2, template.getSortByColumn(), template.getSortDirection());
        });

        // Формируем итоговый список: для каждого сварщика подряд все его строки по аппаратам (без разрывов), затем суммарная строка
        for (Integer welderId : sortedWelderIds) {
            List<WireConsumptionReportDTO> welderData = welderDataMap.get(welderId);
            if (welderData != null) {
                result.addAll(welderData);
            }

            // Добавляем суммарную строку (только если это не записи без сварщика)
            if (welderId != -1) {
                WireConsumptionReportDTO summary = summaryMap.get(welderId);
                if (summary != null) {
                    summary.setIsSummaryRow(true);
                    result.add(summary);
                }
            }
            // Для записей без сварщика (welderId = -1) не добавляем суммарную строку
        }

        return result;
    }

    /**
     * Создает суммарную строку для сварщика
     */
    private WireConsumptionReportDTO createSummaryRow(List<WireConsumptionReportDTO> welderData) {
        if (welderData.isEmpty()) {
            return null;
        }

        WireConsumptionReportDTO first = welderData.get(0);
        WireConsumptionReportDTO summary = new WireConsumptionReportDTO();

        // Копируем информацию о сварщике (может быть null, если сварщика нет)
        summary.setWelderId(first.getWelderId());
        summary.setWelderName(first.getWelderName());
        summary.setTabNumber(first.getTabNumber());
        summary.setProfession(first.getProfession());
        summary.setOrganizationUnitId(first.getOrganizationUnitId());
        summary.setOrganizationUnitName(first.getOrganizationUnitName());

        // Суммируем значения
        Duration totalTimeInNetwork = Duration.ZERO;
        Duration totalArcBurningTime = Duration.ZERO;
        Duration totalTimeOutsideSetCurrent = Duration.ZERO;
        Duration totalTimeOutsideActualCurrent = Duration.ZERO;
        BigDecimal totalEnergy = BigDecimal.ZERO;
        BigDecimal totalWireConsumption = BigDecimal.ZERO;
        BigDecimal avgEfficiency = BigDecimal.ZERO;

        for (WireConsumptionReportDTO item : welderData) {
            if (item.getTimeInNetwork() != null) {
                totalTimeInNetwork = totalTimeInNetwork.plus(item.getTimeInNetwork());
            }
            if (item.getArcBurningTime() != null) {
                totalArcBurningTime = totalArcBurningTime.plus(item.getArcBurningTime());
            }
            if (item.getTimeOutsideSetCurrentRange() != null) {
                totalTimeOutsideSetCurrent = totalTimeOutsideSetCurrent.plus(item.getTimeOutsideSetCurrentRange());
            }
            if (item.getTimeOutsideActualCurrentRange() != null) {
                totalTimeOutsideActualCurrent = totalTimeOutsideActualCurrent.plus(item.getTimeOutsideActualCurrentRange());
            }
            if (item.getEnergyConsumed() != null) {
                totalEnergy = totalEnergy.add(item.getEnergyConsumed());
            }
            if (item.getWireConsumption() != null) {
                totalWireConsumption = totalWireConsumption.add(item.getWireConsumption());
            }
            if (item.getEquipmentEfficiency() != null) {
                avgEfficiency = avgEfficiency.add(item.getEquipmentEfficiency());
            }
        }

        summary.setTimeInNetwork(totalTimeInNetwork);
        summary.setArcBurningTime(totalArcBurningTime);
        summary.setTimeOutsideSetCurrentRange(totalTimeOutsideSetCurrent);
        summary.setTimeOutsideActualCurrentRange(totalTimeOutsideActualCurrent);
        summary.setEnergyConsumed(totalEnergy);
        summary.setWireConsumption(totalWireConsumption);

        // Эффективность рассчитываем на основе общих значений времени в сети и времени горения дуги
        // Эффективность = (время горения дуги / время в сети) * 100
        if (totalTimeInNetwork.toSeconds() > 0) {
            double efficiency = 0.0;
            if (totalArcBurningTime.toSeconds() > 0) {
                efficiency = (double) totalArcBurningTime.toSeconds() / totalTimeInNetwork.toSeconds() * 100.0;
            }
            summary.setEquipmentEfficiency(BigDecimal.valueOf(efficiency).setScale(2, RoundingMode.HALF_UP));
        } else {
            summary.setEquipmentEfficiency(BigDecimal.ZERO);
        }

        summary.setWire(first.getWire()); // Используем проволоку из первой записи

        // Для суммарной строки очищаем поля оборудования (они должны быть пустыми)
        summary.setWeldingMachineId(null);
        summary.setWeldingMachineName(null);
        summary.setEquipmentModel(null);

        return summary;
    }

    /**
     * Сравнивает две строки по указанной колонке
     */
    private int compareByColumn(WireConsumptionReportDTO dto1, WireConsumptionReportDTO dto2,
                                String sortByColumn, String sortDirection) {
        int result = 0;

        if (sortByColumn == null || sortByColumn.isEmpty()) {
            // По умолчанию сортируем по имени сварщика
            String name1 = dto1.getWelderName() != null ? dto1.getWelderName() : "";
            String name2 = dto2.getWelderName() != null ? dto2.getWelderName() : "";
            result = name1.compareTo(name2);
        } else {
            switch (sortByColumn.toUpperCase()) {
                case "WELDER_NAME":
                case "СВАРЩИК":
                    String name1 = dto1.getWelderName() != null ? dto1.getWelderName() : "";
                    String name2 = dto2.getWelderName() != null ? dto2.getWelderName() : "";
                    result = name1.compareTo(name2);
                    break;
                case "WIRE_CONSUMPTION":
                case "РАСХОД":
                    BigDecimal wc1 = dto1.getWireConsumption() != null ? dto1.getWireConsumption() : BigDecimal.ZERO;
                    BigDecimal wc2 = dto2.getWireConsumption() != null ? dto2.getWireConsumption() : BigDecimal.ZERO;
                    result = wc1.compareTo(wc2);
                    break;
                case "ARC_BURNING_TIME":
                case "ВРЕМЯ_ГОРЕНИЯ_ДУГИ":
                    Duration abt1 = dto1.getArcBurningTime() != null ? dto1.getArcBurningTime() : Duration.ZERO;
                    Duration abt2 = dto2.getArcBurningTime() != null ? dto2.getArcBurningTime() : Duration.ZERO;
                    result = abt1.compareTo(abt2);
                    break;
                case "TIME_IN_NETWORK":
                case "ВРЕМЯ_В_СЕТИ":
                    Duration tin1 = dto1.getTimeInNetwork() != null ? dto1.getTimeInNetwork() : Duration.ZERO;
                    Duration tin2 = dto2.getTimeInNetwork() != null ? dto2.getTimeInNetwork() : Duration.ZERO;
                    result = tin1.compareTo(tin2);
                    break;
                default:
                    result = 0;
            }
        }

        return "DESC".equalsIgnoreCase(sortDirection) ? -result : result;
    }

    /** Имя подразделения без lazy OrganizationUnit (фоновые job-отчёты без OSIV). */
    private String resolveOrganizationUnitName(WeldingMachine machine) {
        if (machine == null || machine.getOrganizationUnitId() == null) {
            return "";
        }
        return organizationUnitRepository.findById(machine.getOrganizationUnitId())
                .map(OrganizationUnit::getName)
                .orElse("");
    }

    /**
     * Данные для отчёта по неисправностям оборудования.
     * Берутся состояния со статусом Error за период; количество — число непрерывных отрезков (разрыв ≤1 сек не прерывает).
     */
    @Transactional(readOnly = true)
    public List<EquipmentMalfunctionReportSectionDTO> getEquipmentMalfunctionData(
            EquipmentMalfunctionReportTemplateDTO template,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            LocalTime periodStartTime,
            LocalTime periodEndTime) {
        List<Integer> machineIds = template != null && template.getSelectedEquipmentIds() != null && !template.getSelectedEquipmentIds().isEmpty()
                ? new ArrayList<>(template.getSelectedEquipmentIds())
                : new ArrayList<>();
        LocalDate startDate = periodStartDate != null ? periodStartDate : LocalDate.now();
        LocalDate endDate = periodEndDate != null ? periodEndDate : LocalDate.now();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999_999_999);

        List<EquipmentMalfunctionReportSectionDTO> sections = new ArrayList<>();

        // Когда выбраны конкретные аппараты — нативный запрос с датами; код ошибки извлекаем по значению (1–23) из любой ячейки строки
        final List<Integer> selectedIds = new ArrayList<>(machineIds);
        if (!selectedIds.isEmpty()) {
            List<Object[]> rawForMachines = weldingMachineStateRepository.findErrorStatesNative(selectedIds, startDateTime, endDateTime);
            List<Object[]> rawWarningsForMachines = weldingMachineStateRepository.findStatesNativeWithWarnings(selectedIds, startDateTime, endDateTime);
            Map<Long, String> errorCodeFromParams = loadErrorCodeFromParameters(extractStateIdsFromRawRows(rawForMachines));
            Map<Integer, List<Object[]>> byMachineRaw = rawForMachines.stream()
                    .filter(row -> extractMachineIdFromRow(row, selectedIds) >= 0)
                    .collect(Collectors.groupingBy(row -> extractMachineIdFromRow(row, selectedIds)));
            Map<Integer, List<Object[]>> byMachineWarningsRaw = rawWarningsForMachines == null ? Collections.emptyMap() :
                    rawWarningsForMachines.stream()
                            .filter(row -> extractMachineIdFromRow(row, selectedIds) >= 0)
                            .collect(Collectors.groupingBy(row -> extractMachineIdFromRow(row, selectedIds)));
            for (Integer mid : selectedIds) {
                List<Object[]> machineRows = new ArrayList<>(byMachineRaw.getOrDefault(mid, Collections.emptyList()));
                machineRows.sort(Comparator.comparing(row -> toLocalDateTime(extractDateFromRow(row)), Comparator.nullsLast(Comparator.naturalOrder())));
                List<MalfunctionSegment> segments = buildMalfunctionSegmentsFromRaw(machineRows, mid, errorCodeFromParams);
                Map<String, SummaryAcc> summaryMap = new LinkedHashMap<>();
                Map<String, Map<LocalDate, SummaryAcc>> byDateMap = new LinkedHashMap<>();
                for (MalfunctionSegment seg : segments) {
                    String name = seg.malfunctionName;
                    summaryMap.computeIfAbsent(name, k -> new SummaryAcc()).add(1, seg.durationSeconds);
                    if (seg.date != null) {
                        byDateMap.computeIfAbsent(name, k -> new LinkedHashMap<>())
                                .computeIfAbsent(seg.date, d -> new SummaryAcc()).add(1, seg.durationSeconds, seg.time);
                    }
                }
                // Добавляем предупреждения как отдельные строки в тот же отчёт.
                List<Object[]> warningRows = new ArrayList<>(byMachineWarningsRaw.getOrDefault(mid, Collections.emptyList()));
                warningRows.sort(Comparator.comparing(row -> toLocalDateTime(extractDateFromRow(row)), Comparator.nullsLast(Comparator.naturalOrder())));
                List<MalfunctionSegment> warningSegments = buildWarningSegmentsFromFullStateRows(warningRows);
                for (MalfunctionSegment seg : warningSegments) {
                    String name = seg.malfunctionName;
                    summaryMap.computeIfAbsent(name, k -> new SummaryAcc()).add(1, seg.durationSeconds);
                    if (seg.date != null) {
                        byDateMap.computeIfAbsent(name, k -> new LinkedHashMap<>())
                                .computeIfAbsent(seg.date, d -> new SummaryAcc()).add(1, seg.durationSeconds, seg.time);
                    }
                }
                List<EquipmentMalfunctionSummaryRowDTO> summaryRows = new ArrayList<>();
                for (Map.Entry<String, SummaryAcc> e : summaryMap.entrySet()) {
                    summaryRows.add(new EquipmentMalfunctionSummaryRowDTO(e.getKey(), e.getValue().count, capMalfunctionDurationSec(e.getValue().durationSec)));
                }
                List<EquipmentMalfunctionByDateRowDTO> byDateRows = new ArrayList<>();
                for (Map.Entry<String, Map<LocalDate, SummaryAcc>> e : byDateMap.entrySet()) {
                    for (Map.Entry<LocalDate, SummaryAcc> de : e.getValue().entrySet()) {
                        byDateRows.add(new EquipmentMalfunctionByDateRowDTO(
                                e.getKey(),
                                de.getKey(),
                                formatTimesForReport(de.getValue()),
                                de.getValue().count,
                                capMalfunctionDurationSec(de.getValue().durationSec)));
                    }
                }
                byDateRows.sort(Comparator
                        .comparing(EquipmentMalfunctionByDateRowDTO::getDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(EquipmentMalfunctionByDateRowDTO::getTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(EquipmentMalfunctionByDateRowDTO::getMalfunctionName, Comparator.nullsFirst(Comparator.naturalOrder())));
                String equipmentModel = "";
                String equipmentName = "";
                String equipmentDepartment = "";
                String serialNumber = "";
                String inventoryNumber = "";
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(mid);
                if (machineOpt.isPresent()) {
                    WeldingMachine m = machineOpt.get();
                    equipmentModel = m.getDeviceModel() != null ? m.getDeviceModel().name() : "";
                    equipmentName = m.getName() != null ? m.getName() : "";
                    equipmentDepartment = resolveOrganizationUnitName(m);
                    serialNumber = m.getSerialNumber() != null ? m.getSerialNumber() : "";
                    inventoryNumber = m.getInventoryNumber() != null ? m.getInventoryNumber() : "";
                }
                sections.add(new EquipmentMalfunctionReportSectionDTO(mid, equipmentModel, equipmentName, equipmentDepartment, serialNumber, inventoryNumber, summaryRows, byDateRows));
            }
            return sections;
        }

        // Нет выбранных аппаратов — берём все ошибки из нативного запроса и фильтруем по дате в Java
        List<Object[]> rawErrorStatesAll = weldingMachineStateRepository.findErrorStatesNativeAllUnbounded();
        List<Object[]> rawWarningsAll = weldingMachineStateRepository.findStatesNativeWithWarningsAll(startDateTime, endDateTime);

        boolean hasAnyWarnings = rawWarningsAll != null && rawWarningsAll.stream().anyMatch(r -> hasNonEmptyWarningText(r != null && r.length > 4 ? r[4] : null));
        if ((rawErrorStatesAll == null || rawErrorStatesAll.isEmpty()) && !hasAnyWarnings) return Collections.emptyList();

        if (rawErrorStatesAll == null) rawErrorStatesAll = Collections.emptyList();
        List<Object[]> rawErrorStatesFiltered = rawErrorStatesAll.stream()
                .filter(row -> {
                    LocalDateTime dt = toLocalDateTime(row[2]);
                    if (dt == null) return false;
                    LocalDate d = dt.toLocalDate();
                    return !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .collect(Collectors.toList());
        List<Object[]> rawErrorStates = rawErrorStatesFiltered;
        Map<Integer, List<Object[]>> byMachine = rawErrorStates.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(row -> ((Number) row[1]).intValue()));
        Map<Integer, List<Object[]>> byMachineWarnings = rawWarningsAll == null ? Collections.emptyMap() :
                rawWarningsAll.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(row -> ((Number) row[1]).intValue()));

        // Машины из ошибок + машины из предупреждений (чтобы отчёт не был пустым, если есть только предупреждения)
        Set<Integer> errorMachineIdSet = rawErrorStatesFiltered.stream()
                .filter(Objects::nonNull)
                .map(row -> ((Number) row[1]).intValue())
                .collect(Collectors.toSet());
        Set<Integer> warningMachineIdSet = rawWarningsAll == null ? Collections.emptySet() :
                rawWarningsAll.stream()
                        .filter(r -> hasNonEmptyWarningText(r != null && r.length > 4 ? r[4] : null))
                        .map(r -> ((Number) r[1]).intValue())
                        .collect(Collectors.toSet());
        machineIds = new ArrayList<>();
        machineIds.addAll(errorMachineIdSet);
        machineIds.addAll(warningMachineIdSet);
        machineIds = machineIds.stream().distinct().sorted().collect(Collectors.toList());

        Map<Long, String> errorCodeFromParams = loadErrorCodeFromParameters(extractStateIdsFromRawRows(rawErrorStates));
        for (Integer mid : machineIds) {
            List<Object[]> machineRows = byMachine.getOrDefault(mid, Collections.emptyList());
            machineRows = new ArrayList<>(machineRows);
            machineRows.sort(Comparator.comparing(row -> toLocalDateTime(row[2])));

            List<MalfunctionSegment> segments = buildMalfunctionSegmentsFromRaw(machineRows, mid, errorCodeFromParams);
            Map<String, SummaryAcc> summaryMap = new LinkedHashMap<>();
            Map<String, Map<LocalDate, SummaryAcc>> byDateMap = new LinkedHashMap<>();
            for (MalfunctionSegment seg : segments) {
                String name = seg.malfunctionName;
                summaryMap.computeIfAbsent(name, k -> new SummaryAcc()).add(1, seg.durationSeconds);
                if (seg.date != null) {
                    byDateMap.computeIfAbsent(name, k -> new LinkedHashMap<>())
                            .computeIfAbsent(seg.date, d -> new SummaryAcc()).add(1, seg.durationSeconds, seg.time);
                }
            }

            // Добавляем предупреждения в ту же секцию.
            List<Object[]> warningRows = new ArrayList<>(byMachineWarnings.getOrDefault(mid, Collections.emptyList()));
            warningRows.sort(Comparator.comparing(row -> toLocalDateTime(row[2])));
            List<MalfunctionSegment> warningSegments = buildWarningSegmentsFromFullStateRows(warningRows);
            for (MalfunctionSegment seg : warningSegments) {
                String name = seg.malfunctionName;
                summaryMap.computeIfAbsent(name, k -> new SummaryAcc()).add(1, seg.durationSeconds);
                if (seg.date != null) {
                    byDateMap.computeIfAbsent(name, k -> new LinkedHashMap<>())
                            .computeIfAbsent(seg.date, d -> new SummaryAcc()).add(1, seg.durationSeconds, seg.time);
                }
            }
            List<EquipmentMalfunctionSummaryRowDTO> summaryRows = new ArrayList<>();
            for (Map.Entry<String, SummaryAcc> e : summaryMap.entrySet()) {
                summaryRows.add(new EquipmentMalfunctionSummaryRowDTO(e.getKey(), e.getValue().count, capMalfunctionDurationSec(e.getValue().durationSec)));
            }
            List<EquipmentMalfunctionByDateRowDTO> byDateRows = new ArrayList<>();
            for (Map.Entry<String, Map<LocalDate, SummaryAcc>> e : byDateMap.entrySet()) {
                for (Map.Entry<LocalDate, SummaryAcc> de : e.getValue().entrySet()) {
                    byDateRows.add(new EquipmentMalfunctionByDateRowDTO(
                            e.getKey(),
                            de.getKey(),
                            formatTimesForReport(de.getValue()),
                            de.getValue().count,
                            capMalfunctionDurationSec(de.getValue().durationSec)));
                }
            }
            byDateRows.sort(Comparator
                    .comparing(EquipmentMalfunctionByDateRowDTO::getDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(EquipmentMalfunctionByDateRowDTO::getTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(EquipmentMalfunctionByDateRowDTO::getMalfunctionName, Comparator.nullsFirst(Comparator.naturalOrder())));
            String equipmentModel = "";
            String equipmentName = "";
            String equipmentDepartment = "";
            String serialNumber = "";
            String inventoryNumber = "";
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(mid);
            if (machineOpt.isPresent()) {
                WeldingMachine m = machineOpt.get();
                equipmentModel = m.getDeviceModel() != null ? m.getDeviceModel().name() : "";
                equipmentName = m.getName() != null ? m.getName() : "";
                equipmentDepartment = resolveOrganizationUnitName(m);
                serialNumber = m.getSerialNumber() != null ? m.getSerialNumber() : "";
                inventoryNumber = m.getInventoryNumber() != null ? m.getInventoryNumber() : "";
            }
            sections.add(new EquipmentMalfunctionReportSectionDTO(mid, equipmentModel, equipmentName, equipmentDepartment, serialNumber, inventoryNumber, summaryRows, byDateRows));
        }
        return sections;
    }

    /** Секции по выбранным аппаратам без данных неисправностей (модель, наименование и т.д. заполнены, в таблицах — «нет неисправностей»). */
    private List<EquipmentMalfunctionReportSectionDTO> buildSectionsWithEquipmentOnly(List<Integer> machineIds) {
        List<EquipmentMalfunctionReportSectionDTO> result = new ArrayList<>();
        for (Integer mid : machineIds) {
            String equipmentModel = "";
            String equipmentName = "";
            String equipmentDepartment = "";
            String serialNumber = "";
            String inventoryNumber = "";
            Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(mid);
            if (machineOpt.isPresent()) {
                WeldingMachine m = machineOpt.get();
                equipmentModel = m.getDeviceModel() != null ? m.getDeviceModel().name() : "";
                equipmentName = m.getName() != null ? m.getName() : "";
                equipmentDepartment = resolveOrganizationUnitName(m);
                serialNumber = m.getSerialNumber() != null ? m.getSerialNumber() : "";
                inventoryNumber = m.getInventoryNumber() != null ? m.getInventoryNumber() : "";
            }
            result.add(new EquipmentMalfunctionReportSectionDTO(mid, equipmentModel, equipmentName, equipmentDepartment, serialNumber, inventoryNumber, Collections.emptyList(), Collections.emptyList()));
        }
        return result;
    }

    private static class SummaryAcc {
        int count;
        long durationSec;
        java.util.SortedSet<LocalTime> times = new java.util.TreeSet<>();
        void add(int c, long d) { count += c; durationSec += d; }
        void add(int c, long d, LocalTime t) {
            add(c, d);
            if (t != null) times.add(t);
        }
    }

    private static String formatTimesForReport(SummaryAcc acc) {
        if (acc == null || acc.times == null || acc.times.isEmpty()) return "";
        java.time.format.DateTimeFormatter tf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        return acc.times.stream().map(t -> t.format(tf)).collect(java.util.stream.Collectors.joining("\n"));
    }

    /** Один непрерывный отрезок неисправности (после слияния с разрывом ≤1 сек). */
    private static class MalfunctionSegment {
        final String malfunctionName;
        final LocalDate date;
        final LocalTime time;
        final long durationSeconds;
        MalfunctionSegment(String malfunctionName, LocalDate date, LocalTime time, long durationSeconds) {
            this.malfunctionName = malfunctionName;
            this.date = date;
            this.time = time;
            this.durationSeconds = durationSeconds;
        }
    }

    /** Строит отрезки неисправностей: объединяет подряд идущие состояния с одним errorCode, если разрыв ≤1 сек. */
    private List<MalfunctionSegment> buildMalfunctionSegments(List<WeldingMachineState> states) {
        if (states == null || states.isEmpty()) return Collections.emptyList();
        List<MalfunctionSegment> out = new ArrayList<>();
        final long maxGapMs = 1000L;
        String currentName = null;
        LocalDateTime segmentEnd = null;
        long segmentDurationSec = 0;
        LocalDate segmentDate = null;
        LocalTime segmentStartTime = null;
        for (WeldingMachineState s : states) {
            String name = (s.getErrorCode() != null && !s.getErrorCode().trim().isEmpty()) ? s.getErrorCode().trim() : "Неизвестная ошибка";
            LocalDateTime stateStart = s.getDateCreated();
            long durationMs = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
            LocalDateTime stateEnd = stateStart.plus(durationMs, ChronoUnit.MILLIS);
            long durationSec = (durationMs + 500) / 1000;
            if (currentName != null && currentName.equals(name) && segmentEnd != null && ChronoUnit.MILLIS.between(segmentEnd, stateStart) <= maxGapMs) {
                segmentDurationSec += durationSec;
                segmentEnd = stateEnd;
            } else {
                if (currentName != null) {
                    out.add(new MalfunctionSegment(currentName, segmentDate, segmentStartTime, segmentDurationSec));
                }
                currentName = name;
                segmentEnd = stateEnd;
                segmentDurationSec = durationSec;
                segmentDate = stateStart != null ? stateStart.toLocalDate() : null;
                segmentStartTime = stateStart != null ? stateStart.toLocalTime() : null;
            }
        }
        if (currentName != null) {
            out.add(new MalfunctionSegment(currentName, segmentDate, segmentStartTime, segmentDurationSec));
        }
        return out;
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof java.time.Instant) return LocalDateTime.ofInstant((java.time.Instant) value, java.time.ZoneId.systemDefault());
        if (value instanceof java.time.OffsetDateTime) return ((java.time.OffsetDateTime) value).toLocalDateTime();
        if (value instanceof java.util.Date) return ((java.util.Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        return null;
    }

    /** Ограничивает продолжительность неисправности в отчёте (макс. 2 ч на строку). */
    private static long capMalfunctionDurationSec(long durationSec) {
        final long maxSec = 2L * 3600;
        return durationSec < 0 ? 0 : Math.min(durationSec, maxSec);
    }

    /** Преобразует код ошибки (число 1–23 или строка) в текст. Пустой код — «(без кода)». */
    private static String resolveMalfunctionName(Object errorCodeObj) {
        return EquipmentErrorMessages.resolve(errorCodeObj);
    }

    /** Приводит значение error_code из нативного запроса к виду для resolve: драйвер может вернуть String или Number. */
    private static Object normalizeErrorCodeFromRow(Object raw) {
        if (raw == null) return null;
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            return s.isEmpty() ? null : s;
        }
        if (raw instanceof Number) return String.valueOf(((Number) raw).intValue());
        return String.valueOf(raw).trim();
    }

    /** Извлекает код ошибки (1–23) из строки; не считаем кодом ошибки значение, равное id аппарата (чтобы 8 не превращалось в «Ошибка 8»). */
    private static Object extractErrorCodeFromRow(Object[] row, int excludeMachineId) {
        if (row == null) return null;
        for (Object cell : row) {
            if (cell == null) continue;
            String s = String.valueOf(cell).trim();
            if (s.isEmpty()) continue;
            try {
                int n = Integer.parseInt(s);
                if (n >= 1 && n <= 23 && n != excludeMachineId) return s;
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    /** Извлекает welding_machine_id из строки — число, входящее в список допустимых id (чтобы не зависеть от порядка колонок). */
    private static int extractMachineIdFromRow(Object[] row, List<Integer> machineIds) {
        if (row == null || machineIds == null) return -1;
        for (Object cell : row) {
            if (cell instanceof Number) {
                int id = ((Number) cell).intValue();
                if (machineIds.contains(id)) return id;
            }
        }
        return -1;
    }

    /** Первая ячейка типа Timestamp/LocalDateTime в строке (date_created). */
    private static Object extractDateFromRow(Object[] row) {
        if (row == null) return null;
        for (Object cell : row) {
            if (cell instanceof java.sql.Timestamp || cell instanceof LocalDateTime || cell instanceof java.util.Date) return cell;
        }
        return null;
    }

    /** Число в строке, подходящее под state_duration_ms: 0 или из диапазона (100 ms .. 1 год), чтобы не принять id аппарата (1–999) за длительность. */
    private static long extractDurationMsFromRow(Object[] row) {
        if (row == null) return 0L;
        for (Object cell : row) {
            if (cell instanceof Number) {
                long v = ((Number) cell).longValue();
                if (v >= 0 && (v == 0 || v > 100) && v < 86400000L * 365) return v;
            }
        }
        return 0L;
    }

    /**
     * Строит сегменты неисправностей из сущностей (JPQL). Название — по state.getErrorCode() через resolveMalfunctionName.
     * Длительность — из stateDurationMs или из интервала до следующей записи.
     */
    private List<MalfunctionSegment> buildMalfunctionSegmentsFromEntities(List<WeldingMachineState> states) {
        if (states == null || states.isEmpty()) return Collections.emptyList();
        states = new ArrayList<>(states);
        states.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
        final int n = states.size();
        long[] effectiveDurationMs = new long[n];
        // Ошибки длятся минуты; в БД часто state_duration_ms=0 — ограничиваем 30 мин на сегмент
        final long maxSegmentDurationMs = 30L * 60 * 1000;
        for (int i = 0; i < n; i++) {
            WeldingMachineState s = states.get(i);
            long fromDb = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
            if (fromDb > 0) {
                effectiveDurationMs[i] = Math.min(fromDb, maxSegmentDurationMs);
            } else if (i + 1 < n) {
                LocalDateTime start = s.getDateCreated();
                LocalDateTime nextStart = states.get(i + 1).getDateCreated();
                if (start != null && nextStart != null) {
                    long gapMs = ChronoUnit.MILLIS.between(start, nextStart);
                    if (gapMs < 0) gapMs = 0;
                    effectiveDurationMs[i] = Math.min(gapMs, maxSegmentDurationMs);
                }
            }
        }
        List<MalfunctionSegment> out = new ArrayList<>();
        final long maxGapMs = 1000L;
        String currentName = null;
        LocalDateTime segmentEnd = null;
        long segmentDurationSec = 0;
        LocalDate segmentDate = null;
        LocalTime segmentStartTime = null;
        for (int i = 0; i < n; i++) {
            WeldingMachineState s = states.get(i);
            String name = resolveMalfunctionName(s.getErrorCode());
            LocalDateTime stateStart = s.getDateCreated();
            if (stateStart == null) continue;
            long durationMs = effectiveDurationMs[i];
            LocalDateTime stateEnd = stateStart.plus(durationMs, ChronoUnit.MILLIS);
            long durationSec = (durationMs + 500) / 1000;
            if (currentName != null && currentName.equals(name) && segmentEnd != null && ChronoUnit.MILLIS.between(segmentEnd, stateStart) <= maxGapMs) {
                segmentDurationSec += durationSec;
                segmentEnd = stateEnd;
            } else {
                if (currentName != null) {
                    out.add(new MalfunctionSegment(currentName, segmentDate, segmentStartTime, segmentDurationSec));
                }
                currentName = name;
                segmentEnd = stateEnd;
                segmentDurationSec = durationSec;
                segmentDate = stateStart.toLocalDate();
                segmentStartTime = stateStart.toLocalTime();
            }
        }
        if (currentName != null) {
            out.add(new MalfunctionSegment(currentName, segmentDate, segmentStartTime, segmentDurationSec));
        }
        return out;
    }

    /** Собирает id состояний из сырых строк нативного запроса (колонка 0 = id). */
    private List<Long> extractStateIdsFromRawRows(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        List<Long> ids = new ArrayList<>();
        for (Object[] row : rows) {
            if (row != null && row.length > 0 && row[0] instanceof Number) {
                long id = ((Number) row[0]).longValue();
                if (id > 0) ids.add(id);
            }
        }
        return ids;
    }

    /** Загружает код ошибки из welding_machine_parameter_value по state id (fallback, если в state.error_code пусто). */
    private Map<Long, String> loadErrorCodeFromParameters(List<Long> stateIds) {
        if (stateIds == null || stateIds.isEmpty()) return Collections.emptyMap();
        Map<Long, String> result = new HashMap<>();
        List<String> propertyCodes = Arrays.asList("ErrorCode", "error_code", "Error");
        for (String prop : propertyCodes) {
            try {
                List<Object[]> rows = parameterValueRepository.findStateIdAndValueNativeCoalesce(stateIds, prop);
                if (rows != null) {
                    for (Object[] r : rows) {
                        if (r != null && r.length >= 2 && r[0] instanceof Number) {
                            Long sid = ((Number) r[0]).longValue();
                            String val = r[1] != null ? String.valueOf(r[1]).trim() : null;
                            if (val != null && !val.isEmpty() && !result.containsKey(sid)) result.put(sid, val);
                        }
                    }
                }
            } catch (Exception ignored) { }
        }
        return result;
    }

    /** То же, что buildMalfunctionSegments, но по сырым строкам нативного запроса.
     * machineIdToExclude — id аппарата, не считать его кодом ошибки (чтобы 8 не давало «Ошибка 8»).
     * paramErrorByStateId — код ошибки из параметров (fallback, если в state пусто). */
    private List<MalfunctionSegment> buildMalfunctionSegmentsFromRaw(List<Object[]> rows, int machineIdToExclude, Map<Long, String> paramErrorByStateId) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        final int n = rows.size();
        long[] effectiveDurationMs = new long[n];
        // Нативный запрос: [0]=id, [1]=welding_machineid, [2]=date_created, [3]=state_duration_ms, [4]=error_code
        // Берём длительность только из колонки 3, иначе id (row[0]) ошибочно принимался за duration_ms
        final long maxSegmentDurationMs = 30L * 60 * 1000;
        for (int i = 0; i < n; i++) {
            Object[] row = rows.get(i);
            long fromDb = 0L;
            if (row != null && row.length > 3 && row[3] instanceof Number) {
                fromDb = ((Number) row[3]).longValue();
                if (fromDb < 0) fromDb = 0;
            }
            if (fromDb > 0) {
                effectiveDurationMs[i] = Math.min(fromDb, maxSegmentDurationMs);
            } else {
                LocalDateTime start = toLocalDateTime(extractDateFromRow(row));
                if (i + 1 < n) {
                    LocalDateTime nextStart = toLocalDateTime(extractDateFromRow(rows.get(i + 1)));
                    if (start != null && nextStart != null) {
                        long gapMs = ChronoUnit.MILLIS.between(start, nextStart);
                        if (gapMs < 0) gapMs = 0;
                        effectiveDurationMs[i] = Math.min(gapMs, maxSegmentDurationMs);
                    }
                }
            }
        }
        List<MalfunctionSegment> out = new ArrayList<>();
        final long maxGapMs = 1000L;
        String currentName = null;
        LocalDateTime segmentEnd = null;
        long segmentDurationSec = 0;
        LocalDate segmentDate = null;
        LocalTime segmentStartTime = null;
        for (int i = 0; i < n; i++) {
            Object[] row = rows.get(i);
            Object errorCodeRaw = extractErrorCodeFromRow(row, machineIdToExclude);
            if (errorCodeRaw == null && row.length > 4 && row[4] != null) {
                Object r4 = row[4];
                if (!(r4 instanceof Number) || ((Number) r4).intValue() != machineIdToExclude) errorCodeRaw = r4;
            }
            if (errorCodeRaw == null && paramErrorByStateId != null && row.length > 0 && row[0] instanceof Number) {
                Long stateId = ((Number) row[0]).longValue();
                if (paramErrorByStateId.containsKey(stateId)) errorCodeRaw = paramErrorByStateId.get(stateId);
            }
            String name = resolveMalfunctionName(normalizeErrorCodeFromRow(errorCodeRaw));
            LocalDateTime stateStart = toLocalDateTime(extractDateFromRow(row));
            if (stateStart == null) continue;
            long durationMs = effectiveDurationMs[i];
            LocalDateTime stateEnd = stateStart.plus(durationMs, ChronoUnit.MILLIS);
            long durationSec = (durationMs + 500) / 1000;
            if (currentName != null && currentName.equals(name) && segmentEnd != null && ChronoUnit.MILLIS.between(segmentEnd, stateStart) <= maxGapMs) {
                segmentDurationSec += durationSec;
                segmentEnd = stateEnd;
            } else {
                if (currentName != null) {
                    out.add(new MalfunctionSegment(currentName, segmentDate, segmentStartTime, segmentDurationSec));
                }
                currentName = name;
                segmentEnd = stateEnd;
                segmentDurationSec = durationSec;
                segmentDate = stateStart.toLocalDate();
                segmentStartTime = stateStart.toLocalTime();
            }
        }
        if (currentName != null) {
            out.add(new MalfunctionSegment(currentName, segmentDate, segmentStartTime, segmentDurationSec));
        }
        return out;
    }

    private static boolean hasNonEmptyWarningText(Object warningTextObj) {
        if (warningTextObj == null) return false;
        String s = String.valueOf(warningTextObj).trim();
        if (s.isEmpty()) return false;
        String lower = s.toLowerCase(Locale.ROOT);
        if ("null".equals(lower)) return false;
        // Значение, которое выставляет парсер, когда предупреждений нет
        if ("нет предупреждений".equals(lower) || "no warnings".equals(lower)) return false;
        return true;
    }

    private static List<String> splitWarningMessages(Object warningTextObj) {
        if (!hasNonEmptyWarningText(warningTextObj)) return Collections.emptyList();
        String s = String.valueOf(warningTextObj).trim();
        if (s.isEmpty()) return Collections.emptyList();

        // Парсер/фронт используют разделители запятая/точка с запятой.
        String[] parts = s.split("[,;]");
        return Arrays.stream(parts)
                .map(p -> p == null ? "" : p.trim())
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Строит сегменты предупреждений: объединяет подряд идущие состояния для одного и того же сообщения,
     * если разрыв между концом предыдущего сегмента и началом текущего <= 1 сек.
     *
     * Имена сегментов имеют вид: "Предупреждение: <text>".
     *
     * Ожидаемые колонки в rows (из welding_machine_state_repository.findStatesNativeWithWarnings*):
     * [0]=id, [1]=welding_machineid, [2]=date_created, [3]=state_duration_ms, [4]=warning_text.
     */
    private List<MalfunctionSegment> buildWarningSegmentsFromFullStateRows(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        // 1) Эффективная длительность каждого рядa (как в buildMalfunctionSegmentsFromRaw)
        final int n = rows.size();
        long[] effectiveDurationMs = new long[n];
        final long maxSegmentDurationMs = 30L * 60 * 1000; // ограничение на 30 минут на сегмент
        for (int i = 0; i < n; i++) {
            Object[] row = rows.get(i);
            long fromDb = 0L;
            if (row != null && row.length > 3 && row[3] instanceof Number) {
                fromDb = ((Number) row[3]).longValue();
                if (fromDb < 0) fromDb = 0;
            }
            if (fromDb > 0) {
                effectiveDurationMs[i] = Math.min(fromDb, maxSegmentDurationMs);
            } else {
                LocalDateTime start = toLocalDateTime(extractDateFromRow(row));
                if (i + 1 < n) {
                    LocalDateTime nextStart = toLocalDateTime(extractDateFromRow(rows.get(i + 1)));
                    if (start != null && nextStart != null) {
                        long gapMs = ChronoUnit.MILLIS.between(start, nextStart);
                        if (gapMs < 0) gapMs = 0;
                        effectiveDurationMs[i] = Math.min(gapMs, maxSegmentDurationMs);
                    }
                }
            }
        }

        // 2) Сегментация по конкретным сообщениям предупреждений
        final long maxGapMs = 1000L;
        class Acc {
            int lastIndex;
            LocalDate segmentDate;
            LocalTime segmentStartTime;
            LocalDateTime segmentEnd;
            long durationSeconds;
        }

        Map<String, Acc> accByName = new LinkedHashMap<>();
        List<MalfunctionSegment> out = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            Object[] row = rows.get(i);
            if (row == null) continue;

            LocalDateTime stateStart = toLocalDateTime(extractDateFromRow(row));
            if (stateStart == null) continue;

            long durationMs = effectiveDurationMs[i];
            LocalDateTime stateEnd = stateStart.plus(durationMs, ChronoUnit.MILLIS);
            long durationSec = (durationMs + 500) / 1000;

            Object warningTextObj = row.length > 4 ? row[4] : null;
            List<String> messages = splitWarningMessages(warningTextObj);
            if (messages.isEmpty()) continue;

            // На одном state может быть несколько предупреждений: разделяем на строки.
            // Dedup на уровне state, чтобы не плодить дубликаты при повторяющихся формулировках.
            Set<String> uniqueMessages = new LinkedHashSet<>(messages);
            for (String msg : uniqueMessages) {
                if (msg == null || msg.trim().isEmpty()) continue;
                String name = "" + msg.trim();

                Acc acc = accByName.get(name);
                boolean canMerge = acc != null
                        && acc.lastIndex == i - 1
                        && acc.segmentEnd != null
                        && ChronoUnit.MILLIS.between(acc.segmentEnd, stateStart) <= maxGapMs;

                if (canMerge) {
                    acc.durationSeconds += durationSec;
                    acc.segmentEnd = stateEnd;
                    // segmentDate/segmentStartTime не меняются (они уже от старта сегмента)
                } else {
                    if (acc != null) {
                        out.add(new MalfunctionSegment(name, acc.segmentDate, acc.segmentStartTime, acc.durationSeconds));
                    }
                    Acc next = new Acc();
                    next.lastIndex = i;
                    next.segmentDate = stateStart.toLocalDate();
                    next.segmentStartTime = stateStart.toLocalTime();
                    next.segmentEnd = stateEnd;
                    next.durationSeconds = durationSec;
                    accByName.put(name, next);
                }
            }
        }

        // Добавляем все ещё "открытые" сегменты
        for (Map.Entry<String, Acc> e : accByName.entrySet()) {
            Acc acc = e.getValue();
            if (acc == null || acc.segmentDate == null || acc.segmentStartTime == null) continue;
            out.add(new MalfunctionSegment(e.getKey(), acc.segmentDate, acc.segmentStartTime, acc.durationSeconds));
        }

        out.sort(Comparator
                .comparing((MalfunctionSegment s) -> s.date, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(s -> s.time, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(s -> s.malfunctionName, Comparator.nullsFirst(Comparator.naturalOrder())));

        return out;
    }
} 