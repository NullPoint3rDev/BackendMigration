package org.alloy.controllers;

import org.alloy.models.dto.*;
import org.alloy.models.ReportHistory;
import org.alloy.services.ReportService;
import org.alloy.services.ReportDataService;
import org.alloy.services.ReportHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDataService reportDataService;

    @Autowired
    private ReportHistoryService reportHistoryService;

    @PostMapping("/wire-consumption")
    public ResponseEntity<byte[]> generateWireConsumptionReport(@RequestBody ReportRequestDTO request) {
        try {
            List<WireConsumptionReportDTO> data = reportDataService.getWireConsumptionData(request);
            byte[] reportBytes = reportService.generateWireConsumptionReport(data, request.getFormat());

            String filename = "wire_consumption_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/welder")
    public ResponseEntity<byte[]> generateWelderReport(@RequestBody ReportRequestDTO request) {
        try {
            List<WelderReportDTO> data = reportDataService.getWelderReportData(request);
            byte[] reportBytes = reportService.generateWelderReport(data, request.getFormat());

            String filename = "welder_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/work")
    public ResponseEntity<byte[]> generateWorkReport(@RequestBody ReportRequestDTO request) {
        try {
            List<WorkReportDTO> data = reportDataService.getWorkReportData(request);
            byte[] reportBytes = reportService.generateWorkReport(data, request.getFormat());

            String filename = "work_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getReportTypes() {
        List<String> reportTypes = List.of("WIRE_CONSUMPTION", "WELDER_REPORT", "WORK_REPORT");
        return ResponseEntity.ok(reportTypes);
    }

    @GetMapping("/formats")
    public ResponseEntity<List<String>> getReportFormats() {
        List<String> formats = List.of("EXCEL", "PDF", "CSV");
        return ResponseEntity.ok(formats);
    }

    @GetMapping("/periods")
    public ResponseEntity<List<String>> getReportPeriods() {
        List<String> periods = List.of("DAY", "WEEK", "MONTH", "QUARTER", "YEAR", "CUSTOM");
        return ResponseEntity.ok(periods);
    }

    // Новые эндпоинты для отчетов согласно требованиям
    
    @PostMapping("/equipment")
    public ResponseEntity<byte[]> generateEquipmentReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по работе оборудования
            byte[] reportBytes = reportService.generateEquipmentReport(request);
            
            String filename = "equipment_work_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/welders")
    public ResponseEntity<byte[]> generateWeldersReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по работе сварщиков
            byte[] reportBytes = reportService.generateWeldersReport(request);
            
            String filename = "welders_work_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/materials")
    public ResponseEntity<byte[]> generateMaterialsReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по расходу материалов
            byte[] reportBytes = reportService.generateMaterialsReport(request);
            
            String filename = "materials_consumption_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/welds")
    public ResponseEntity<byte[]> generateWeldsReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по сварочным швам
            byte[] reportBytes = reportService.generateWeldsReport(request);
            
            String filename = "welds_quality_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @PostMapping("/errors")
    public ResponseEntity<byte[]> generateErrorsReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по ошибкам
            byte[] reportBytes = reportService.generateErrorsReport(request);
            
            String filename = "equipment_errors_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/violations")
    public ResponseEntity<byte[]> generateViolationsReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по нарушениям
            byte[] reportBytes = reportService.generateViolationsReport(request);
            
            String filename = "welds_violations_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/tasks")
    public ResponseEntity<byte[]> generateTasksReport(@RequestBody ReportRequestDTO request) {
        try {
            // TODO: Реализовать генерацию отчета по заданиям
            byte[] reportBytes = reportService.generateTasksReport(request);
            
            String filename = "welding_tasks_report_" + System.currentTimeMillis();
            if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
                filename += ".xlsx";
            } else if ("PDF".equalsIgnoreCase(request.getFormat())) {
                filename += ".pdf";
            } else if ("CSV".equalsIgnoreCase(request.getFormat())) {
                filename += ".csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Методы для работы с историей отчетов
    
    /**
     * Получить последние отчеты для определенного типа
     */
    @GetMapping("/history/{reportType}")
    public ResponseEntity<List<ReportHistory>> getRecentReports(@PathVariable String reportType) {
        System.out.println("ReportController: Запрос истории для типа '" + reportType + "'");
        System.out.println("ReportController: URL: /reports/history/" + reportType);
        
        try {
            List<ReportHistory> reports = reportHistoryService.getRecentReports(reportType);
            System.out.println("ReportController: Возвращаем " + reports.size() + " отчетов");
            
            // Добавляем детальное логирование
            if (reports.isEmpty()) {
                System.out.println("ReportController: История пуста для типа '" + reportType + "'");
            } else {
                System.out.println("ReportController: Детали отчетов:");
                for (ReportHistory report : reports) {
                    System.out.println("  - " + report.getReportName() + " (" + report.getFormat() + ") - " + report.getGeneratedAt());
                }
            }
            
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            System.err.println("ReportController: Ошибка при получении истории: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить все отчеты из истории
     */
    @GetMapping("/history")
    public ResponseEntity<List<ReportHistory>> getAllReports() {
        System.out.println("ReportController: Запрос всех отчетов из истории");
        
        try {
            List<ReportHistory> reports = reportHistoryService.getAllReports();
            System.out.println("ReportController: Возвращаем " + reports.size() + " отчетов из БД");
            
            // Добавляем детальное логирование
            if (reports.isEmpty()) {
                System.out.println("ReportController: История пуста");
            } else {
                System.out.println("ReportController: Детали отчетов:");
                for (ReportHistory report : reports) {
                    System.out.println("  - " + report.getReportName() + " (" + report.getFormat() + ") - " + report.getGeneratedAt() + " - Auto: " + report.getIsAutoGenerated());
                }
            }
            
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            System.err.println("ReportController: Ошибка при получении всех отчетов: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Очистить все отчеты из истории
     */
    @DeleteMapping("/history/clear")
    public ResponseEntity<String> clearAllReports() {
        try {
            reportHistoryService.clearAllReports();
            System.out.println("ReportController: All reports cleared from history");
            return ResponseEntity.ok("All reports cleared successfully");
        } catch (Exception e) {
            System.err.println("ReportController: Error clearing reports: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error clearing reports: " + e.getMessage());
        }
    }
    
    /**
     * Получить все типы отчетов, для которых есть история
     */
    @GetMapping("/history/types")
    public ResponseEntity<java.util.Set<String>> getHistoryReportTypes() {
        java.util.Set<String> types = reportHistoryService.getReportTypes();
        return ResponseEntity.ok(types);
    }
    
    /**
     * Получить общее количество отчетов в истории
     */
    @GetMapping("/history/count")
    public ResponseEntity<Integer> getTotalReportsCount() {
        int count = reportHistoryService.getTotalReportsCount();
        return ResponseEntity.ok(count);
    }
    
    /**
     * Очистить историю для определенного типа отчета
     */
    @DeleteMapping("/history/{reportType}")
    public ResponseEntity<Void> clearHistory(@PathVariable String reportType) {
        reportHistoryService.clearHistory(reportType);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Очистить всю историю
     */
    @DeleteMapping("/history/all")
    public ResponseEntity<Void> clearAllHistory() {
        reportHistoryService.clearAllHistory();
        return ResponseEntity.ok().build();
    }
    
    /**
     * Инициализировать тестовые данные
     */
    @PostMapping("/history/init-test-data")
    public ResponseEntity<Void> initializeTestData() {
        System.out.println("ReportController: Инициализация тестовых данных");
        System.out.println("ReportController: URL: /reports/history/init-test-data");
        
        try {
            reportHistoryService.initializeTestData();
            System.out.println("ReportController: Тестовые данные инициализированы");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("ReportController: Ошибка при инициализации: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 