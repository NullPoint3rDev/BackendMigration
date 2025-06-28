package org.alloy.services;

import org.alloy.models.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportDataService {

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
        // Моковые данные для демонстрации
        List<WelderReportDTO> data = new ArrayList<>();
        
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