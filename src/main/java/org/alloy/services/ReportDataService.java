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
                    
                    // Конвертируем LocalDate в LocalDateTime для расчета
                    LocalDateTime startDate = request.getDateFrom() != null ? 
                        request.getDateFrom().atStartOfDay() : 
                        LocalDateTime.now().minusDays(30);
                    LocalDateTime endDate = request.getDateTo() != null ? 
                        request.getDateTo().atTime(23, 59, 59) : 
                        LocalDateTime.now();
                    
                    // Рассчитываем средние значения для блока мониторинга ОГК
                    if ("8CAAB50C4254".equals(machine.getMac())) {
                        WeldingReportCalculationService.AverageValues averages = 
                            calculationService.calculateAverageValues(machine.getMac(), startDate, endDate);
                        
                        WelderReportDTO item = new WelderReportDTO();
                        item.setWelderId(1);
                        item.setWelderName("Оператор блока мониторинга");
                        item.setWelderEmail("operator@company.com");
                        item.setDate(request.getDateFrom() != null ? request.getDateFrom() : LocalDate.now());
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
        // Моковые данные для демонстрации
        List<WorkReportDTO> data = new ArrayList<>();
        
        LocalDateTime baseDate = LocalDateTime.now();
        
        for (int i = 0; i < 8; i++) {
            WorkReportDTO item = new WorkReportDTO();
            item.setWeldingMachineId(1 + i % 3);
            item.setWeldingMachineName("Аппарат " + (1 + i % 3));
            item.setWeldingMachineSerialNumber("SN" + (1000 + i));
            item.setWelderId(1 + i % 2);
            item.setWelderName("Сварщик " + (1 + i % 2));
            item.setStartTime(baseDate.minusHours(i * 2));
            item.setEndTime(baseDate.minusHours(i * 2).plusMinutes(45 + i * 5));
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
        
        return data;
    }
} 