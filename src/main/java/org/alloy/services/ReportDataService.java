package org.alloy.services;

import org.alloy.models.dto.*;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReportDataService {

    @Autowired
    private WeldingReportCalculationService calculationService;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    public List<WireConsumptionReportDTO> getWireConsumptionData(ReportRequestDTO request) {
        // Моковые данные для демонстрации
        List<WireConsumptionReportDTO> data = new ArrayList<>();
        
        LocalDateTime baseDate = LocalDateTime.now();
        
        for (int i = 0; i < 10; i++) {
            WireConsumptionReportDTO item = new WireConsumptionReportDTO();
            item.setWeldingMachineId(1 + i % 3);
            item.setWeldingMachineName("Аппарат " + (1 + i % 3));
            item.setWeldingMachineSerialNumber("SN" + (1000 + i));
            item.setWelderId(1 + i % 2);
            item.setWelderName("Сварщик " + (1 + i % 2));
            item.setDate(baseDate.minusDays(i));
            item.setWireConsumption(BigDecimal.valueOf(2.5 + i * 0.3));
            item.setWireFeedRate(BigDecimal.valueOf(5.0 + i * 0.2));
            item.setWeldingTime(BigDecimal.valueOf(30 + i * 5));
            item.setCurrent(BigDecimal.valueOf(180 + i * 10));
            item.setVoltage(BigDecimal.valueOf(22 + i * 0.5));
            item.setWeldingMode("Ручной");
            item.setWeldingType("MIG/MAG");
            item.setOrganizationUnitName("Цех " + (1 + i % 2));
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
                        
                        System.out.println("[REPORT-DATA] ✅ Создан отчет для блока мониторинга ОГК с реальными данными");
                        return data;
                    }
                }
            }
            
            // Для других аппаратов используем моковые данные (как было)
            System.out.println("[REPORT-DATA] ⚠️ Используем моковые данные для аппарата");
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
        
        System.out.println("[REPORT-DATA] ========================================");
        System.out.println("[REPORT-DATA] 🔍 ЗАПРОС ОТЧЕТА ПО РАБОТЕ ОБОРУДОВАНИЯ");
        System.out.println("[REPORT-DATA] ========================================");
        System.out.println("[REPORT-DATA]   WeldingMachineId: " + request.getWeldingMachineId());
        System.out.println("[REPORT-DATA]   Period: " + request.getPeriod());
        System.out.println("[REPORT-DATA]   DateFrom: " + request.getDateFrom());
        System.out.println("[REPORT-DATA]   DateTo: " + request.getDateTo());
        System.out.println("[REPORT-DATA]   ReportType: " + request.getReportType());
        System.out.println("[REPORT-DATA]   Format: " + request.getFormat());
        
        try {
            // Если указан конкретный аппарат, используем реальные данные
            if (request.getWeldingMachineId() != null) {
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(request.getWeldingMachineId());
                if (machineOpt.isPresent()) {
                    WeldingMachine machine = machineOpt.get();
                    System.out.println("[REPORT-DATA] 🔍 Найден аппарат: " + machine.getName() + " (MAC: " + machine.getMac() + ")");
                    
                    // Определяем период на основе параметра period
                    LocalDateTime startDate, endDate;
                    if ("DAY".equals(request.getPeriod())) {
                        // За день - используем сегодняшний день
                        LocalDate today = LocalDate.now();
                        startDate = today.atStartOfDay();
                        endDate = today.atTime(23, 59, 59);
                        System.out.println("[REPORT-DATA] 📅 Период 'DAY': " + startDate + " - " + endDate);
                    } else {
                        // Используем переданные даты и время или по умолчанию
                        startDate = request.getDateFrom() != null ? 
                            request.getDateFrom() : 
                            LocalDateTime.now().minusDays(1);
                        endDate = request.getDateTo() != null ? 
                            request.getDateTo() : 
                            LocalDateTime.now();
                        System.out.println("[REPORT-DATA] 📅 Период по датам: " + startDate + " - " + endDate);
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
                        
                        System.out.println("[REPORT-DATA] ✅ Создан отчет по работе оборудования для блока мониторинга ОГК с " + 
                            dailyData.size() + " записями по дням");
                        return data;
                    } else {
                        System.out.println("[REPORT-DATA] ⚠️ Аппарат не является блоком мониторинга ОГК (MAC: " + machine.getMac() + ")");
                    }
                } else {
                    System.out.println("[REPORT-DATA] ❌ Аппарат с ID " + request.getWeldingMachineId() + " не найден");
                }
            } else {
                System.out.println("[REPORT-DATA] ⚠️ WeldingMachineId не указан в запросе");
            }
            
            // Для других аппаратов используем моковые данные (как было)
            System.out.println("[REPORT-DATA] ⚠️ Используем моковые данные для отчета по работе оборудования");
            
            // Определяем базовую дату в зависимости от периода
            LocalDateTime baseDate;
            if ("DAY".equals(request.getPeriod())) {
                // За день - используем только сегодняшний день
                baseDate = LocalDateTime.now().withHour(15).withMinute(13).withSecond(0);
                System.out.println("[REPORT-DATA] 📅 Моковые данные за день: " + baseDate.toLocalDate());
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

        System.out.println("[REPORT-DATA] ========================================");
        System.out.println("[REPORT-DATA] 🔍 ЗАПРОС ОТЧЕТА ПО СВАРОЧНЫМ ШВАМ");
        System.out.println("[REPORT-DATA] ========================================");
        System.out.println("[REPORT-DATA]   WeldingMachineId: " + request.getWeldingMachineId());
        System.out.println("[REPORT-DATA]   Period: " + request.getPeriod());
        System.out.println("[REPORT-DATA]   DateFrom: " + request.getDateFrom());
        System.out.println("[REPORT-DATA]   DateTo: " + request.getDateTo());

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
                        System.out.println("[REPORT-DATA] ✅ Создан отчет по швам: " + data.size() + " сегментов");
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
                    System.out.println("[REPORT-DATA] ✅ Создан отчет по швам (общий): " + data.size() + " сегментов");
                    return data;
                }
            }

        } catch (Exception e) {
            System.err.println("[REPORT-DATA] ❌ Ошибка получения данных отчета по швам: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }
} 