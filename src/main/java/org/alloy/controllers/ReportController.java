package org.alloy.controllers;

import org.alloy.models.dto.*;
import org.alloy.services.ReportService;
import org.alloy.services.ReportDataService;
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
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDataService reportDataService;

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
        List<String> formats = List.of("EXCEL", "PDF");
        return ResponseEntity.ok(formats);
    }

    @GetMapping("/periods")
    public ResponseEntity<List<String>> getReportPeriods() {
        List<String> periods = List.of("DAY", "MONTH", "YEAR");
        return ResponseEntity.ok(periods);
    }
} 