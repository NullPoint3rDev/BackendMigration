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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            System.out.println("[REPORT-DATA] getWelderWorkDataNew: welderIds=" + welderIds + " period=" + startDateTime + ".." + endDateTime);
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
                System.out.println("[REPORT-DATA] welderId=" + welderId + " rfidCodes=" + rfidCodes.size() + " machineIds=" + machineIds);
                if (machineIds.isEmpty()) continue;

                for (Integer machineId : machineIds) {
                    Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(machineId);
                    String machineName = machineOpt.map(WeldingMachine::getName).orElse("");
                    String equipmentModel = machineOpt.map(m -> m.getDeviceModel() != null ? m.getDeviceModel().name() : "").orElse("");
                    List<org.alloy.models.dto.WeldSegmentDTO> segments = calculationService.calculateWeldSegments(machineId, startDateTime, endDateTime);
                    System.out.println("[REPORT-DATA] machineId=" + machineId + " segments=" + segments.size() + (segments.isEmpty() ? "" : " first: avgI=" + segments.get(0).getAverageCurrent() + " avgU=" + segments.get(0).getAverageVoltage() + " durSec=" + segments.get(0).getDurationSeconds()));
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
            System.out.println("[REPORT-DATA] rawRows=" + rawRows.size());

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
            System.out.println("[REPORT-DATA] merged rows=" + merged.size());

            // Один раз загружаем состояния по всем аппаратам за период (вместо запроса на каждый шов)
            Set<Integer> machineIdsInReport = merged.stream().map(r -> r.machineId).collect(Collectors.toSet());
            Map<Integer, List<WeldingMachineState>> statesByMachineId = new HashMap<>();
            for (Integer mid : machineIdsInReport) {
                statesByMachineId.put(mid, weldingMachineStateRepository.findByWeldingMachineIdAndDateRange(mid, startDateTime, endDateTime));
            }
            // Параметры «режим» одним батчем по всем stateId (скорость проволоки не грузим — аппарат пока не присылает, в отчёте нули)
            List<Long> allStateIds = statesByMachineId.values().stream()
                    .flatMap(list -> list.stream().map(WeldingMachineState::getId))
                    .collect(Collectors.toList());
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
            // Дата и время из посылки аппарата (CORE: Date.*, Time.*) — чтобы в отчёте было «когда аппарат сказал, что идёт сварка»
            Map<Long, LocalDateTime> deviceDateTimeByStateId = buildDeviceDateTimeByStateId(allStateIds);

            for (WeldRow r : merged) {
                if (r.durationSec.longValue() < minDurationSec) continue;
                int currentInt = r.avgCurrent.intValue();
                if (currentInt < 1) currentInt = 0;
                boolean outOfRange = actualMin != null && actualMax != null && (currentInt < actualMin || currentInt > actualMax);

                LocalDateTime segmentEnd = r.startTime.plusSeconds(r.durationSec.longValue());
                LocalDateTime displayDateTime = getDeviceDateTimeFromCache(statesByMachineId.get(r.machineId), r.startTime, segmentEnd, deviceDateTimeByStateId);
                if (displayDateTime == null) displayDateTime = r.startTime;
                String workMode = getWorkModeFromCache(statesByMachineId.get(r.machineId), r.startTime, segmentEnd, workModeByStateId);
                // Аппарат пока не присылает скорость подачи проволоки — в столбце выводим 0; когда будет — подставить getWireFeedFromCache(...)
                BigDecimal energyKwh = calculateEnergyPerWeld(r.avgVoltage, r.avgCurrent, r.durationSec);
                if (result.size() < 2) {
                    System.out.println("[REPORT-DATA] row: avgCurrent=" + r.avgCurrent + " avgVoltage=" + r.avgVoltage + " durationSec=" + r.durationSec + " -> energyKwh=" + energyKwh);
                }

                WelderWorkReportDTO dto = new WelderWorkReportDTO();
                dto.setDate(displayDateTime.toLocalDate());
                dto.setWeldStartTime(displayDateTime.toLocalTime());
                dto.setEquipmentModel(r.equipmentModel);
                dto.setEquipmentName(r.machineName);
                dto.setWorkMode(workMode != null ? workMode : "");
                dto.setWireFeedSpeedMpm(BigDecimal.ZERO);
                dto.setCurrentAmps(r.avgCurrent.setScale(1, RoundingMode.HALF_UP));
                dto.setVoltageVolts(r.avgVoltage.setScale(1, RoundingMode.HALF_UP));
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
            List<EquipmentWeldRow> rawRows = new ArrayList<>();
            for (Integer machineId : machineIds) {
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(machineId);
                String machineName = machineOpt.map(WeldingMachine::getName).orElse("");
                String equipmentModel = machineOpt.map(m -> m.getDeviceModel() != null ? m.getDeviceModel().name() : "").orElse("");
                List<org.alloy.models.dto.WeldSegmentDTO> segments = calculationService.calculateWeldSegments(machineId, startDateTime, endDateTime);
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
            Map<Integer, List<WeldingMachineState>> statesByMachineId = new HashMap<>();
            for (Integer mid : machineIdsInReport) {
                List<WeldingMachineState> list = weldingMachineStateRepository.findByWeldingMachineIdAndDateRange(mid, startDateTime, endDateTime);
                list.sort(Comparator.comparing(WeldingMachineState::getDateCreated));
                statesByMachineId.put(mid, list);
            }
            List<Long> allStateIds = statesByMachineId.values().stream()
                    .flatMap(list -> list.stream().map(WeldingMachineState::getId))
                    .collect(Collectors.toList());
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
            Map<Long, LocalDateTime> deviceDateTimeByStateId = buildDeviceDateTimeByStateId(allStateIds);
            // RFID по stateId из параметров: нативный запрос, при пустой карте — JPA (разные имена колонок в БД)
            Map<Long, String> rfidByStateId = loadRfidByStateIdFromParamsNative(allStateIds);
            if (rfidByStateId.isEmpty() && !allStateIds.isEmpty()) {
                for (String code : new String[] { "RFID.Hex", "RFID", "State.RFID", "Rfid" }) {
                    List<WeldingMachineParameterValue> rfidVals = getParameterValuesInBatches(allStateIds, code);
                    for (WeldingMachineParameterValue pv : rfidVals) {
                        if (pv.getValue() != null && !pv.getValue().trim().isEmpty())
                            rfidByStateId.putIfAbsent(pv.getWeldingMachineStateId(), pv.getValue().trim());
                    }
                    if (!rfidByStateId.isEmpty()) break;
                }
            }
            String sampleRfid = rfidByStateId.isEmpty() ? " (нет RFID в параметрах)" : rfidByStateId.entrySet().stream().limit(2).map(e -> e.getKey() + "->" + e.getValue()).collect(Collectors.joining("; "));
            System.out.println("[REPORT-DATA] equipment report: allStateIds=" + allStateIds.size() + " rfidByStateId=" + rfidByStateId.size() + " " + sampleRfid);

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
            // Предвычисление: по машине и дате — состояния с непустым state.rfid (для fallback по колонке)
            Map<Integer, Map<LocalDate, List<WeldingMachineState>>> statesWithRfidColumnByMachineAndDay = new HashMap<>();
            for (Integer mid : machineIdsInReport) {
                Map<LocalDate, List<WeldingMachineState>> byDay = new HashMap<>();
                for (WeldingMachineState s : statesByMachineId.get(mid)) {
                    if (s.getDateCreated() == null || s.getRfid() == null || s.getRfid().trim().isEmpty()) continue;
                    byDay.computeIfAbsent(s.getDateCreated().toLocalDate(), k -> new ArrayList<>()).add(s);
                }
                statesWithRfidColumnByMachineAndDay.put(mid, byDay);
            }

            for (EquipmentWeldRow r : merged) {
                if (r.durationSec.longValue() < minDurationSec) continue;
                int currentInt = r.avgCurrent.intValue();
                if (currentInt < 1) currentInt = 0;
                boolean outOfRange = actualMin != null && actualMax != null && (currentInt < actualMin || currentInt > actualMax);

                LocalDateTime segmentEnd = r.startTime.plusSeconds(r.durationSec.longValue());
                LocalDateTime displayDateTime = getDeviceDateTimeFromCache(statesByMachineId.get(r.machineId), r.startTime, segmentEnd, deviceDateTimeByStateId);
                if (displayDateTime == null) displayDateTime = r.startTime;
                String workMode = getWorkModeFromCache(statesByMachineId.get(r.machineId), r.startTime, segmentEnd, workModeByStateId);

                String welderFullName = "";
                String welderTabNumber = "";
                String welderProfession = "";
                Long welderId = null;
                WeldingMachineState stateInSegment = getStateInSegment(statesByMachineId.get(r.machineId), r.startTime, segmentEnd, rfidByStateId);
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
                if (result.size() < 3) {
                    System.out.println("[REPORT-DATA] equipment row: segmentStart=" + r.startTime + " stateInSegmentId=" + (stateInSegment != null ? stateInSegment.getId() : null) + " rfidCode=" + rfidCode);
                }
                if (rfidCode != null) {
                    Optional<RfidPass> passOpt = findRfidPassByCode(rfidCode);
                    if (result.size() < 3) {
                        System.out.println("[REPORT-DATA] equipment rfid lookup: code=" + rfidCode + " passFound=" + passOpt.isPresent() + (passOpt.isPresent() && passOpt.get().getWelder() != null ? " welder=" + passOpt.get().getWelder().getName() : ""));
                    }
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
                dto.setWireFeedSpeedMpm(BigDecimal.ZERO);
                dto.setCurrentAmps(r.avgCurrent.setScale(1, RoundingMode.HALF_UP));
                dto.setVoltageVolts(r.avgVoltage.setScale(1, RoundingMode.HALF_UP));
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

    private boolean overlapsSegment(WeldingMachineState s, LocalDateTime segmentStart, LocalDateTime segmentEnd) {
        LocalDateTime stateStart = s.getDateCreated();
        if (stateStart == null) return false;
        long durationMs = s.getStateDurationMs() != null ? s.getStateDurationMs() : 0L;
        LocalDateTime stateEnd = stateStart.plus(durationMs, ChronoUnit.MILLIS);
        return stateStart.isBefore(segmentEnd) && stateEnd.isAfter(segmentStart);
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
     */
    private Optional<RfidPass> findRfidPassByCode(String code) {
        if (code == null || code.trim().isEmpty()) return Optional.empty();
        String trimmed = code.trim();
        Optional<RfidPass> exact = rfidPassRepository.findByCode(trimmed);
        if (exact.isPresent()) return exact;
        String normalized = normalizeHexLeadingZeros(trimmed);
        if (!normalized.equals(trimmed)) return rfidPassRepository.findByCode(normalized);
        return Optional.empty();
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
                Set<Integer> foundMachineIds = new HashSet<>(); // Для отслеживания найденных аппаратов

                for (String rfid : rfidCodes) {
                    // Используем метод репозитория для фильтрации по дате на уровне БД (более эффективно)
                    // Ищем в поле rfid таблицы WeldingMachineState
                    List<WeldingMachineState> statesForRfid = weldingMachineStateRepository
                            .findByRfidAndDateRange(rfid, startDateTime, endDateTime);
                    states.addAll(statesForRfid);

                    // Также ищем в properties через WeldingMachineParameterValue (для старых записей, где RFID сохранен только в properties)
                    List<Long> stateIdsWithRfid = parameterValueRepository.findStateIdsByRfidInProperties(rfid);
                    if (!stateIdsWithRfid.isEmpty()) {
                        // Получаем состояния по найденным ID и фильтруем по дате
                        for (Long stateId : stateIdsWithRfid) {
                            Optional<WeldingMachineState> stateOpt = weldingMachineStateRepository.findById(stateId);
                            if (stateOpt.isPresent()) {
                                WeldingMachineState state = stateOpt.get();
                                // Фильтруем по дате
                                if ((state.getDateCreated().isAfter(startDateTime) || state.getDateCreated().isEqual(startDateTime))
                                        && (state.getDateCreated().isBefore(endDateTime) || state.getDateCreated().isEqual(endDateTime))) {
                                    if (!states.contains(state)) {
                                        states.add(state);
                                    }
                                }
                            }
                        }
                    }

                    // Также находим все уникальные ID аппаратов, которые использовали этот RFID (даже если нет состояний за период)
                    // Ищем в поле rfid
                    List<Integer> machineIdsForRfid = weldingMachineStateRepository
                            .findDistinctWeldingMachineIdsByRfidCodes(Collections.singletonList(rfid));
                    foundMachineIds.addAll(machineIdsForRfid);

                    // Также ищем в properties для получения всех аппаратов, которые использовали этот RFID
                    if (!stateIdsWithRfid.isEmpty()) {
                        for (Long stateId : stateIdsWithRfid) {
                            Optional<WeldingMachineState> stateOpt = weldingMachineStateRepository.findById(stateId);
                            if (stateOpt.isPresent()) {
                                foundMachineIds.add(stateOpt.get().getWeldingMachineId());
                            }
                        }
                    }
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

            // Получаем параметры (разбиваем на батчи, если список слишком большой)
            // PostgreSQL не поддерживает более 32767 параметров в одном запросе
            List<WeldingMachineParameterValue> currentValues = getParameterValuesInBatches(stateIds, "Current");
            List<WeldingMachineParameterValue> voltageValues = getParameterValuesInBatches(stateIds, "Voltage");
            List<WeldingMachineParameterValue> wireMaterialValues = getParameterValuesInBatches(stateIds, "Материал проволоки");
            List<WeldingMachineParameterValue> wireDiameterValues = getParameterValuesInBatches(stateIds, "Диаметр проволоки");

            // Рассчитываем время в сети (сумма всех состояний, когда аппарат включен)
            // Время в сети = сумма всех состояний, независимо от статуса (аппарат включен)
            // Если state_duration_ms = 0, используем разницу между датами состояний
            long totalMs = 0;
            if (states.size() > 1) {
                // Сортируем состояния по дате
                List<WeldingMachineState> sortedStates = new ArrayList<>(states);
                sortedStates.sort((s1, s2) -> s1.getDateCreated().compareTo(s2.getDateCreated()));

                // Рассчитываем время как разницу между первым и последним состоянием
                LocalDateTime firstState = sortedStates.get(0).getDateCreated();
                LocalDateTime lastState = sortedStates.get(sortedStates.size() - 1).getDateCreated();
                totalMs = java.time.Duration.between(firstState, lastState).toMillis();
            } else if (states.size() == 1) {
                // Если только одно состояние, используем его duration или минимальное значение
                WeldingMachineState state = states.get(0);
                if (state.getStateDurationMs() != null && state.getStateDurationMs() > 0) {
                    totalMs = state.getStateDurationMs();
                } else {
                    // Если duration = 0, используем минимальное значение (например, 1 секунда)
                    totalMs = 1000;
                }
            }

            Duration totalTimeInNetwork = Duration.ofMillis(totalMs);
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

            // Расход проволоки - пока всегда 0, так как логика расчета еще не определена
            dto.setWireConsumption(BigDecimal.ZERO);

            return dto;

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка расчета для аппарата " + (machine != null ? machine.getName() : "null") + " (ID=" + (machine != null ? machine.getId() : "null") + "): " + e.getMessage());
            System.err.println("[REPORT-DATA] ❌ Stack trace:");
            e.printStackTrace();
            return null;
        }
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
     * Загружает «Расход проволоки» (или скорость проволоки) нативным запросом по stateIds и заполняет map.
     * Используется, когда загрузка через сущности возвращает пустой результат (snake_case в БД).
     */
    private void loadWireFeedByStateIdNative(List<Long> stateIds, Map<Long, BigDecimal> wireFeedByStateId) {
        if (stateIds == null || stateIds.isEmpty()) return;
        final int BATCH_SIZE = 10_000;
        for (int i = 0; i < stateIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, stateIds.size());
            List<Long> batch = stateIds.subList(i, endIndex);
            try {
                List<Object[]> rows = parameterValueRepository.findStateIdAndValueNative(batch, "Расход проволоки");
                for (Object[] row : rows) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        try {
                            long stateId = ((Number) row[0]).longValue();
                            String valueStr = row[1].toString().trim().replace(",", ".");
                            wireFeedByStateId.put(stateId, new BigDecimal(valueStr));
                        } catch (NumberFormatException ignored) { }
                    }
                }
            } catch (Exception e) {
                System.err.println("[REPORT-DATA] loadWireFeedByStateIdNative батч " + i + "-" + endIndex + ": " + e.getMessage());
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
     * Рассчитывает расход проволоки (кг) на основе времени горения дуги
     * ВРЕМЕННО: всегда возвращает 0, так как логика расчета еще не определена
     */
    private BigDecimal calculateWireConsumption(Duration arcBurningTime) {
        // TODO: Определить логику расчета расхода проволоки
        return BigDecimal.ZERO;
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

        // Формируем итоговый список: для каждого сварщика сначала все его ИП, затем суммарная строка
        for (Integer welderId : sortedWelderIds) {
            List<WireConsumptionReportDTO> welderData = welderDataMap.get(welderId);
            result.addAll(welderData);

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
} 