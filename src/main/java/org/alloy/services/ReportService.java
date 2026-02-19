package org.alloy.services;

import org.alloy.models.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.alloy.models.ReportHistory;
import org.alloy.services.WeldingMachineService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportHistoryService reportHistoryService;

    @Autowired
    private WeldingMachineService weldingMachineService;

    @Autowired
    private ReportDataService reportDataService;

    /**
     * Генерирует даты для отчета в зависимости от выбранного периода
     */
    private String[] generateDatesForPeriod(String period, int count) {
        String[] dates = new String[count];
        java.time.LocalDate baseDate = java.time.LocalDate.now();

        switch (period.toUpperCase()) {
            case "DAY":
                // За день - все даты одинаковые
                String dayDate = baseDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                for (int i = 0; i < count; i++) {
                    dates[i] = dayDate;
                }
                break;

            case "WEEK":
                // За неделю - даты в пределах текущей недели
                java.time.LocalDate weekStart = baseDate.minusDays(baseDate.getDayOfWeek().getValue() - 1);
                for (int i = 0; i < count; i++) {
                    dates[i] = weekStart.plusDays(i % 7).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                break;

            case "MONTH":
                // За месяц - даты в пределах текущего месяца
                for (int i = 0; i < count; i++) {
                    int dayOfMonth = 1 + (i % baseDate.lengthOfMonth());
                    dates[i] = baseDate.withDayOfMonth(dayOfMonth).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                break;

            case "QUARTER":
                // За квартал - даты в пределах текущего квартала
                int quarter = (baseDate.getMonthValue() - 1) / 3;
                java.time.LocalDate quarterStart = baseDate.withMonth(quarter * 3 + 1).withDayOfMonth(1);
                for (int i = 0; i < count; i++) {
                    dates[i] = quarterStart.plusDays(i % 90).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                break;

            case "YEAR":
                // За год - даты в пределах текущего года
                java.time.LocalDate yearStart = baseDate.withMonth(1).withDayOfMonth(1);
                for (int i = 0; i < count; i++) {
                    dates[i] = yearStart.plusDays(i % 365).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                break;

            default:
                // По умолчанию - случайные даты в текущем году
                for (int i = 0; i < count; i++) {
                    dates[i] = "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28));
                }
                break;
        }

        return dates;
    }

    /**
     * Генерирует информацию о временном интервале для отчета
     */
    private String[] generatePeriodInfo(String period) {
        java.time.LocalDate baseDate = java.time.LocalDate.now();
        java.time.LocalDateTime startDateTime, endDateTime;
        String periodDescription;

        switch (period.toUpperCase()) {
            case "DAY":
                startDateTime = baseDate.atTime(4, 0, 0); // 04:00:00
                endDateTime = baseDate.plusDays(1).atTime(4, 0, 0); // 04:00:00 следующего дня
                periodDescription = "Отчёт за целый день";
                break;

            case "WEEK":
                java.time.LocalDate weekStart = baseDate.minusDays(baseDate.getDayOfWeek().getValue() - 1);
                startDateTime = weekStart.atTime(4, 0, 0);
                endDateTime = weekStart.plusDays(7).atTime(4, 0, 0);
                periodDescription = "Отчёт за неделю";
                break;

            case "MONTH":
                startDateTime = baseDate.withDayOfMonth(1).atTime(4, 0, 0);
                endDateTime = baseDate.withDayOfMonth(baseDate.lengthOfMonth()).plusDays(1).atTime(4, 0, 0);
                periodDescription = "Отчёт за месяц";
                break;

            case "QUARTER":
                int quarter = (baseDate.getMonthValue() - 1) / 3;
                java.time.LocalDate quarterStart = baseDate.withMonth(quarter * 3 + 1).withDayOfMonth(1);
                startDateTime = quarterStart.atTime(4, 0, 0);
                endDateTime = quarterStart.plusMonths(3).atTime(4, 0, 0);
                periodDescription = "Отчёт за квартал";
                break;

            case "YEAR":
                startDateTime = baseDate.withMonth(1).withDayOfMonth(1).atTime(4, 0, 0);
                endDateTime = baseDate.withMonth(12).withDayOfMonth(31).plusDays(1).atTime(4, 0, 0);
                periodDescription = "Отчёт за год";
                break;

            default:
                startDateTime = baseDate.atTime(4, 0, 0);
                endDateTime = baseDate.plusDays(1).atTime(4, 0, 0);
                periodDescription = "Отчёт за день";
                break;
        }

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        String startStr = startDateTime.format(formatter);
        String endStr = endDateTime.format(formatter);

        return new String[]{
                "Отчёт по расходам за период с " + startStr + " по " + endStr,
                periodDescription
        };
    }

    public byte[] generateWireConsumptionReport(List<WireConsumptionReportDTO> data, String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateWireConsumptionExcel(data);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateWireConsumptionPdf(data);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    public byte[] generateWelderReport(List<WelderReportDTO> data, String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateWelderExcel(data);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateWelderPdf(data);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    public byte[] generateWorkReport(List<WorkReportDTO> data, String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateWorkExcel(data);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateWorkPdf(data);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    // Новые методы для отчетов согласно требованиям

    public byte[] generateEquipmentReport(ReportRequestDTO request) throws IOException {

        // Получаем название аппарата по ID
        String machineName = "Неизвестный аппарат";
        if (request.getWeldingMachineId() != null) {
            try {
                var machine = weldingMachineService.getWeldingMachineById(request.getWeldingMachineId());
                if (machine.isPresent()) {
                    machineName = machine.get().getName();
                } else {
                    machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (не найден)";
                }
            } catch (Exception e) {
                machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (ошибка загрузки)";
                System.err.println("Ошибка при получении названия аппарата: " + e.getMessage());
            }
        }

        // Получаем реальные данные из ReportDataService
        List<WorkReportDTO> data = reportDataService.getWorkReportData(request);

        // Генерируем отчет с реальными данными
        byte[] reportData = generateEquipmentReportWithRealData(data, request.getFormat(), request.getWeldingMachineId(), machineName, request.getPeriod(), request.getSelectedColumns());

        try {
            // Записываем в историю
            String fileName = "equipment_report_" + System.currentTimeMillis() + getFileExtension(request.getFormat());
            ReportHistory history = new ReportHistory(
                    "equipment",
                    "Отчет по работе оборудования",
                    request.getFormat(),
                    request.getPeriod(),
                    fileName,
                    (long) reportData.length,
                    "Система"
            );

            reportHistoryService.addReportToHistory(history);

        } catch (Exception e) {
            System.err.println("Ошибка при добавлении отчета в историю: " + e.getMessage());
            e.printStackTrace();
        }

        return reportData;
    }

    private byte[] generateEquipmentReportWithRealData(List<WorkReportDTO> data, String format, Integer weldingMachineId, String machineName, String period, List<String> selectedColumns) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateEquipmentExcelWithRealData(data, weldingMachineId, machineName, period, selectedColumns);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateEquipmentPdfWithRealData(data, weldingMachineId, machineName, period, selectedColumns);
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateEquipmentCsvWithRealData(data, weldingMachineId, machineName, period, selectedColumns);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateEquipmentPdfWithRealData(List<WorkReportDTO> data, Integer weldingMachineId, String machineName, String period, List<String> selectedColumns) throws IOException {
        // Пока используем старый метод с тестовыми данными
        return generateEquipmentPdfWithData(weldingMachineId, machineName, period);
    }

    private byte[] generateEquipmentCsvWithRealData(List<WorkReportDTO> data, Integer weldingMachineId, String machineName, String period, List<String> selectedColumns) throws IOException {
        // Пока используем старый метод с тестовыми данными
        return generateEquipmentCsvWithData(weldingMachineId, machineName, period);
    }

    private byte[] generateEquipmentReportWithData(String format, Integer weldingMachineId, String machineName, String period) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateEquipmentExcelWithData(weldingMachineId, machineName, period);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateEquipmentPdfWithData(weldingMachineId, machineName, period);
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateEquipmentCsvWithData(weldingMachineId, machineName, period);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    public byte[] generateWeldersReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по работе сварщиков с тестовыми данными
        byte[] reportData = generateWeldersReportWithData(request.getFormat());

        // Записываем в историю
        String fileName = "welders_report_" + System.currentTimeMillis() + getFileExtension(request.getFormat());
        ReportHistory history = new ReportHistory(
                "welders",
                "Отчет по работе сварщиков",
                request.getFormat(),
                request.getPeriod(),
                fileName,
                (long) reportData.length,
                "Система"
        );
        reportHistoryService.addReportToHistory(history);

        return reportData;
    }

    public byte[] generateMaterialsReport(ReportRequestDTO request) throws IOException {

        // Получаем название аппарата по ID
        String machineName = "Неизвестный аппарат";
        if (request.getWeldingMachineId() != null) {
            try {
                var machine = weldingMachineService.getWeldingMachineById(request.getWeldingMachineId());
                if (machine.isPresent()) {
                    machineName = machine.get().getName();
                } else {
                    machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (не найден)";
                }
            } catch (Exception e) {
                machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (ошибка загрузки)";
                System.err.println("Ошибка при получении названия аппарата: " + e.getMessage());
            }
        }

        // Генерируем отчет по расходу материалов с тестовыми данными в зависимости от периода
        byte[] reportData = generateMaterialsReportWithData(request.getFormat(), request.getWeldingMachineId(), machineName, request.getPeriod());

        // Записываем в историю
        String fileName = "materials_report_" + System.currentTimeMillis() + getFileExtension(request.getFormat());
        ReportHistory history = new ReportHistory(
                "materials",
                "Отчет по расходу материалов",
                request.getFormat(),
                request.getPeriod(),
                fileName,
                (long) reportData.length,
                "Система"
        );
        reportHistoryService.addReportToHistory(history);

        return reportData;
    }

    public byte[] generateWeldsReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по сварочным швам с тестовыми данными
        byte[] reportData = generateWeldsReportWithData(request.getFormat());

        // Записываем в историю
        String fileName = "welds_report_" + System.currentTimeMillis() + getFileExtension(request.getFormat());
        ReportHistory history = new ReportHistory(
                "welds",
                "Отчет по сварочным швам",
                request.getFormat(),
                request.getPeriod(),
                fileName,
                (long) reportData.length,
                "Система"
        );
        reportHistoryService.addReportToHistory(history);

        return reportData;
    }



    public byte[] generateErrorsReport(ReportRequestDTO request) throws IOException {

        // Получаем название аппарата по ID
        String machineName = "Неизвестный аппарат";
        if (request.getWeldingMachineId() != null) {
            try {
                var machine = weldingMachineService.getWeldingMachineById(request.getWeldingMachineId());
                if (machine.isPresent()) {
                    machineName = machine.get().getName();
                } else {
                    machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (не найден)";
                }
            } catch (Exception e) {
                machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (ошибка загрузки)";
                System.err.println("Ошибка при получении названия аппарата: " + e.getMessage());
            }
        }

        // Генерируем отчет по ошибкам с тестовыми данными в зависимости от периода
        byte[] reportData = generateErrorsReportWithData(request.getFormat(), request.getWeldingMachineId(), machineName, request.getPeriod());

        // Записываем в историю
        String fileName = "errors_report_" + System.currentTimeMillis() + getFileExtension(request.getFormat());
        ReportHistory history = new ReportHistory(
                "errors",
                "Отчет по ошибкам оборудования",
                request.getFormat(),
                request.getPeriod(),
                fileName,
                (long) reportData.length,
                "Система"
        );
        reportHistoryService.addReportToHistory(history);

        return reportData;
    }

    public byte[] generateViolationsReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по нарушениям с тестовыми данными
        byte[] reportData = generateViolationsReportWithData(request.getFormat());

        // Записываем в историю
        String fileName = "violations_report_" + System.currentTimeMillis() + getFileExtension(request.getFormat());
        ReportHistory history = new ReportHistory(
                "violations",
                "Отчет по нарушениям при сварке",
                request.getFormat(),
                request.getPeriod(),
                fileName,
                (long) reportData.length,
                "Система"
        );
        reportHistoryService.addReportToHistory(history);

        return reportData;
    }

    public byte[] generateTasksReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по заданиям с тестовыми данными
        byte[] reportData = generateTasksReportWithData(request.getFormat());

        // Записываем в историю
        String fileName = "tasks_report_" + System.currentTimeMillis() + getFileExtension(request.getFormat());
        ReportHistory history = new ReportHistory(
                "tasks",
                "Отчет по сварочным заданиям",
                request.getFormat(),
                request.getPeriod(),
                fileName,
                (long) reportData.length,
                "Система"
        );
        reportHistoryService.addReportToHistory(history);

        return reportData;
    }

    private byte[] generatePlaceholderReport(String title, String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generatePlaceholderExcel(title);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generatePlaceholderPdf(title);
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generatePlaceholderCsv(title);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    /**
     * Получить расширение файла для формата отчета
     */
    private String getFileExtension(String format) {
        switch (format.toUpperCase()) {
            case "EXCEL":
                return ".xlsx";
            case "PDF":
                return ".pdf";
            case "CSV":
                return ".csv";
            default:
                return ".txt";
        }
    }

    private byte[] generatePlaceholderExcel(String title) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет");

            Row headerRow = sheet.createRow(0);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue(title);

            Row infoRow = sheet.createRow(1);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue("Это заглушка для отчета. Реальная реализация будет добавлена позже.");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generatePlaceholderPdf(String title) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(title));
        document.add(new Paragraph("Это заглушка для отчета. Реальная реализация будет добавлена позже."));

        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generatePlaceholderCsv(String title) throws IOException {
        String csvContent = title + "\nЭто заглушка для отчета. Реальная реализация будет добавлена позже.";
        return csvContent.getBytes("UTF-8");
    }

    // Вспомогательный метод для добавления BOM к CSV файлам
    private byte[] addBomToCsv(String csvContent) throws IOException {
        byte[] csvBytes = csvContent.getBytes("UTF-8");
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}; // UTF-8 BOM
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);
        return result;
    }

    // Методы для генерации отчетов с тестовыми данными

    private byte[] generateMaterialsReportWithData(String format, Integer weldingMachineId, String machineName, String period) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateMaterialsExcelWithData(weldingMachineId, machineName, period);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateMaterialsPdfWithData(weldingMachineId, machineName, period);
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateMaterialsCsvWithData(weldingMachineId, machineName, period);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }



    private byte[] generateErrorsReportWithData(String format, Integer weldingMachineId, String machineName, String period) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateMalfunctionExcelData(weldingMachineId, machineName, period);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateMalfunctionPdfData(weldingMachineId, machineName, period);
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateMalfunctionCsvData(weldingMachineId, machineName, period);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateEquipmentExcelWithData(Integer weldingMachineId, String machineName, String period) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе оборудования");

            // Добавляем информацию о временном интервале
            String[] periodInfo = generatePeriodInfo(period);

            Row periodRow = sheet.createRow(0);
            Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue(periodInfo[0]);
            CellStyle periodStyle = workbook.createCellStyle();
            Font periodFont = workbook.createFont();
            periodFont.setBold(true);
            periodFont.setFontHeightInPoints((short) 12);
            periodStyle.setFont(periodFont);
            periodCell.setCellStyle(periodStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 8));

            // Добавляем описание периода
            Row descriptionRow = sheet.createRow(1);
            Cell descriptionCell = descriptionRow.createCell(0);
            descriptionCell.setCellValue(periodInfo[1]);
            CellStyle descriptionStyle = workbook.createCellStyle();
            Font descriptionFont = workbook.createFont();
            descriptionFont.setFontHeightInPoints((short) 10);
            descriptionStyle.setFont(descriptionFont);
            descriptionCell.setCellStyle(descriptionStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 8));

            // Добавляем информацию о выбранном аппарате
            Row titleRow = sheet.createRow(2);
            Cell titleCell = titleRow.createCell(0);
            String machineInfo = (weldingMachineId != null) ?
                    "Отчет по работе оборудования ID: " + weldingMachineId + " (" + machineName + ")" :
                    "Отчет по работе оборудования (ID не указан)";
            titleCell.setCellValue(machineInfo);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 8));

            // Создаем заголовки (начиная с 5-й строки)
            Row headerRow = sheet.createRow(5);
            String[] headers = {
                    "Дата", "Время", "Сварщик", "Сила тока, А",
                    "Масса проволоки, кг", "Напряжение, V", "Проволока, м/мин",
                    "Газ, л/мин", "Время сварки (с)"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем данными для конкретного аппарата
            int rowNum = 6;
            String[] welderNames = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};

            // Генерируем даты в зависимости от периода
            String[] dates = generateDatesForPeriod(period, 15);

            // Генерируем данные только для выбранного аппарата
            for (int i = 0; i < 15; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dates[i]);
                row.createCell(1).setCellValue(String.format("%02d:%02d", (int)(Math.random() * 24), (int)(Math.random() * 60))); // Время
                row.createCell(2).setCellValue(welderNames[i % welderNames.length]); // Сварщик
                row.createCell(3).setCellValue(180 + (int)(Math.random() * 120)); // Сила тока, А (180-300)
                row.createCell(4).setCellValue(15.5 + (Math.random() * 84.5)); // Масса проволоки, кг (15.5-100)
                row.createCell(5).setCellValue(20 + (int)(Math.random() * 10)); // Напряжение, V (20-30)
                row.createCell(6).setCellValue(200 + (int)(Math.random() * 300)); // Проволока, м/мин (200-500)
                row.createCell(7).setCellValue(15 + (int)(Math.random() * 25)); // Газ, л/мин (15-40)
                row.createCell(8).setCellValue(30 + (int)(Math.random() * 270)); // Время сварки (с) (30-300)
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateEquipmentExcelWithRealData(List<WorkReportDTO> data, Integer weldingMachineId, String machineName, String period, List<String> selectedColumns) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе оборудования");

            // Добавляем информацию о временном интервале
            String[] periodInfo = generatePeriodInfo(period);

            Row periodRow = sheet.createRow(0);
            Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue(periodInfo[0]);
            CellStyle periodStyle = workbook.createCellStyle();
            Font periodFont = workbook.createFont();
            periodFont.setBold(true);
            periodFont.setFontHeightInPoints((short) 12);
            periodStyle.setFont(periodFont);
            periodCell.setCellStyle(periodStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 8));

            // Добавляем описание периода
            Row descriptionRow = sheet.createRow(1);
            Cell descriptionCell = descriptionRow.createCell(0);
            descriptionCell.setCellValue(periodInfo[1]);
            CellStyle descriptionStyle = workbook.createCellStyle();
            Font descriptionFont = workbook.createFont();
            descriptionFont.setFontHeightInPoints((short) 10);
            descriptionStyle.setFont(descriptionFont);
            descriptionCell.setCellStyle(descriptionStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 8));

            // Добавляем информацию о выбранном аппарате
            Row titleRow = sheet.createRow(2);
            Cell titleCell = titleRow.createCell(0);
            String machineInfo = (weldingMachineId != null) ?
                    "Отчет по работе оборудования ID: " + weldingMachineId + " (" + machineName + ")" :
                    "Отчет по работе оборудования (ID не указан)";
            titleCell.setCellValue(machineInfo);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 8));

            // Создаем заголовки (начиная с 5-й строки)
            Row headerRow = sheet.createRow(5);

            // Определяем столбцы на основе выбранных или используем все по умолчанию
            List<String> headers = getSelectedHeaders(selectedColumns);

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем реальными данными
            int rowNum = 6;
            for (WorkReportDTO item : data) {
                Row row = sheet.createRow(rowNum++);
                fillRowWithData(row, item, headers);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateEquipmentPdfWithData(Integer weldingMachineId, String machineName, String period) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Добавляем информацию о временном интервале
        String[] periodInfo = generatePeriodInfo(period);

        Paragraph periodTitle = new Paragraph(periodInfo[0]);
        periodTitle.setFontSize(14);
        periodTitle.setBold();
        document.add(periodTitle);

        Paragraph periodDescription = new Paragraph(periodInfo[1]);
        periodDescription.setFontSize(12);
        document.add(periodDescription);
        document.add(new Paragraph(""));

        // Заголовок отчета с информацией об аппарате
        String titleText = (weldingMachineId != null) ?
                "Отчет по работе сварочного оборудования ID: " + weldingMachineId + " (" + machineName + ")" :
                "Отчет по работе сварочного оборудования (ID не указан)";
        Paragraph title = new Paragraph(titleText);
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));

        // Данные для конкретного аппарата
        String[] welderNames = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};

        // Генерируем даты в зависимости от периода
        String[] dates = generateDatesForPeriod(period, 12);

        for (int i = 0; i < 12; i++) {
            document.add(new Paragraph("Сессия " + (i + 1) + ":"));
            document.add(new Paragraph("Дата: " + dates[i]));
            document.add(new Paragraph("Время: " + String.format("%02d:%02d", (int)(Math.random() * 24), (int)(Math.random() * 60))));
            document.add(new Paragraph("Сварщик: " + welderNames[i % welderNames.length]));
            document.add(new Paragraph("Сила тока: " + (180 + (int)(Math.random() * 120)) + " А"));
            document.add(new Paragraph("Масса проволоки: " + String.format("%.1f", 15.5 + (Math.random() * 84.5)) + " кг"));
            document.add(new Paragraph("Напряжение: " + (20 + (int)(Math.random() * 10)) + " В"));
            document.add(new Paragraph("Скорость проволоки: " + (200 + (int)(Math.random() * 300)) + " м/мин"));
            document.add(new Paragraph("Расход газа: " + (15 + (int)(Math.random() * 25)) + " л/мин"));
            document.add(new Paragraph("Время сварки: " + (30 + (int)(Math.random() * 270)) + " с"));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateEquipmentCsvWithData(Integer weldingMachineId, String machineName, String period) throws IOException {
        StringBuilder csv = new StringBuilder();

        // Добавляем информацию о временном интервале
        String[] periodInfo = generatePeriodInfo(period);
        csv.append(periodInfo[0]).append("\n");
        csv.append(periodInfo[1]).append("\n");

        String machineInfo = (weldingMachineId != null) ?
                "Отчет по работе оборудования ID: " + weldingMachineId + " (" + machineName + ")" :
                "Отчет по работе оборудования (ID не указан)";
        csv.append(machineInfo).append("\n");
        csv.append("Дата,Время,Сварщик,Сила тока (А),Масса проволоки (кг),Напряжение (В),Проволока (м/мин),Газ (л/мин),Время сварки (с)\n");

        String[] welderNames = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};

        // Генерируем даты в зависимости от периода
        String[] dates = generateDatesForPeriod(period, 12);

        for (int i = 0; i < 12; i++) {
            csv.append(dates[i]).append(","); // Дата в зависимости от периода
            csv.append(String.format("%02d:%02d", (int)(Math.random() * 24), (int)(Math.random() * 60))).append(",");
            csv.append(welderNames[i % welderNames.length]).append(",");
            csv.append(180 + (int)(Math.random() * 24)).append(",");
            csv.append(String.format("%.1f", 15.5 + (Math.random() * 84.5))).append(",");
            csv.append(20 + (int)(Math.random() * 10)).append(",");
            csv.append(200 + (int)(Math.random() * 300)).append(",");
            csv.append(15 + (int)(Math.random() * 25)).append(",");
            csv.append(30 + (int)(Math.random() * 270));
            csv.append("\n");
        }

        return addBomToCsv(csv.toString());
    }

    private byte[] generateWeldersReportWithData(String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateWeldersExcelWithData();
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateWeldersPdfWithData();
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateWeldersCsvWithData();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateWeldersExcelWithData() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе сварщиков");

            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "ID сварщика", "ФИО", "Подразделение", "Квалификация", "Время работы (часы)",
                    "Количество выполненных швов", "Расход проволоки (кг)", "Средний ток (А)",
                    "Среднее напряжение (В)", "Качество работы (%)", "Дата последней работы"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем тестовыми данными
            int rowNum = 1;
            String[] names = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н.", "Морозов М.М.", "Волков В.В.", "Алексеев А.А."};
            String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок"};
            String[] qualifications = {"3 разряд", "4 разряд", "5 разряд", "6 разряд"};

            for (int i = 0; i < 20; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(2000 + i);
                row.createCell(1).setCellValue(names[i % names.length]);
                row.createCell(2).setCellValue(departments[i % departments.length]);
                row.createCell(3).setCellValue(qualifications[i % qualifications.length]);
                row.createCell(4).setCellValue(160 + (int)(Math.random() * 200)); // 160-360 часов
                row.createCell(5).setCellValue(50 + (int)(Math.random() * 150)); // 50-200 швов
                row.createCell(6).setCellValue(8.5 + (Math.random() * 41.5)); // 8.5-50 кг
                row.createCell(7).setCellValue(180 + (int)(Math.random() * 120)); // 180-300 А
                row.createCell(8).setCellValue(20 + (int)(Math.random() * 10)); // 20-30 В
                row.createCell(9).setCellValue(85 + (int)(Math.random() * 15)); // 85-100%
                row.createCell(10).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateWeldersPdfWithData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Заголовок отчета
        Paragraph title = new Paragraph("Отчет по работе сварщиков");
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));

        // Данные
        String[] names = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н.", "Морозов М.М.", "Волков В.В.", "Алексеев А.А."};
        String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок"};
        String[] qualifications = {"3 разряд", "4 разряд", "5 разряд", "6 разряд"};

        for (int i = 0; i < 20; i++) {
            document.add(new Paragraph("Сварщик " + (i + 1) + ":"));
            document.add(new Paragraph("ID: " + (2000 + i)));
            document.add(new Paragraph("ФИО: " + names[i % names.length]));
            document.add(new Paragraph("Подразделение: " + departments[i % departments.length]));
            document.add(new Paragraph("Квалификация: " + qualifications[i % qualifications.length]));
            document.add(new Paragraph("Время работы: " + (160 + (int)(Math.random() * 200)) + " часов"));
            document.add(new Paragraph("Выполнено швов: " + (50 + (int)(Math.random() * 150))));
            document.add(new Paragraph("Расход проволоки: " + String.format("%.1f", 8.5 + (Math.random() * 41.5)) + " кг"));
            document.add(new Paragraph("Средний ток: " + (180 + (int)(Math.random() * 120)) + " А"));
            document.add(new Paragraph("Среднее напряжение: " + (20 + (int)(Math.random() * 10)) + " В"));
            document.add(new Paragraph("Качество работы: " + (85 + (int)(Math.random() * 15)) + "%"));
            document.add(new Paragraph("Дата последней работы: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateWeldersCsvWithData() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID сварщика,ФИО,Подразделение,Квалификация,Время работы (часы),Количество выполненных швов,Расход проволоки (кг),Средний ток (А),Среднее напряжение (В),Качество работы (%),Дата последней работы\n");

        String[] names = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н.", "Морозов М.М.", "Волков В.В.", "Алексеев А.А."};
        String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок"};
        String[] qualifications = {"3 разряд", "4 разряд", "5 разряд", "6 разряд"};

        for (int i = 0; i < 18; i++) {
            csv.append(2000 + i).append(",");
            csv.append(names[i % names.length]).append(",");
            csv.append(departments[i % departments.length]).append(",");
            csv.append(qualifications[i % qualifications.length]).append(",");
            csv.append(160 + (int)(Math.random() * 200)).append(",");
            csv.append(50 + (int)(Math.random() * 150)).append(",");
            csv.append(String.format("%.1f", 8.5 + (Math.random() * 41.5))).append(",");
            csv.append(180 + (int)(Math.random() * 120)).append(",");
            csv.append(20 + (int)(Math.random() * 10)).append(",");
            csv.append(85 + (int)(Math.random() * 15)).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28)));
            csv.append("\n");
        }

        return addBomToCsv(csv.toString());
    }

    private byte[] generateMaterialsExcelWithData(Integer weldingMachineId, String machineName, String period) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по расходу проволоки");

            // Добавляем информацию о временном интервале
            String[] periodInfo = generatePeriodInfo(period);

            Row periodRow = sheet.createRow(0);
            Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue(periodInfo[0]);
            CellStyle periodStyle = workbook.createCellStyle();
            Font periodFont = workbook.createFont();
            periodFont.setBold(true);
            periodFont.setFontHeightInPoints((short) 12);
            periodStyle.setFont(periodFont);
            periodCell.setCellStyle(periodStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));

            // Добавляем описание периода
            Row descriptionRow = sheet.createRow(1);
            Cell descriptionCell = descriptionRow.createCell(0);
            descriptionCell.setCellValue(periodInfo[1]);
            CellStyle descriptionStyle = workbook.createCellStyle();
            Font descriptionFont = workbook.createFont();
            descriptionFont.setFontHeightInPoints((short) 10);
            descriptionStyle.setFont(descriptionFont);
            descriptionCell.setCellStyle(descriptionStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 7));

            // Добавляем информацию о выбранном аппарате
            Row titleRow = sheet.createRow(2);
            Cell titleCell = titleRow.createCell(0);
            String machineInfo = (weldingMachineId != null) ?
                    "Отчет по расходу проволоки ID: " + weldingMachineId + " (" + machineName + ")" :
                    "Отчет по расходу проволоки (ID не указан)";
            titleCell.setCellValue(machineInfo);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 7));

            // Создаем заголовки (начиная с 5-й строки) - 8 столбцов согласно скриншоту
            Row headerRow = sheet.createRow(5);
            String[] headers = {
                    "Сварщик", "Должность", "Время в сети", "Время горения дуги",
                    "Эффективность работы", "Энергия", "Проволока", "Расход, кг"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                // Устанавливаем ширину столбцов согласно новым заголовкам
                if (i == 0 || i == 1) { // Сварщик и Должность - широкие
                    sheet.setColumnWidth(i, 5000);
                } else if (i == 2 || i == 3) { // Время в сети и Время горения дуги - средние
                    sheet.setColumnWidth(i, 4000);
                } else if (i == 4) { // Эффективность работы - узкий
                    sheet.setColumnWidth(i, 3000);
                } else if (i == 5) { // Энергия - средний
                    sheet.setColumnWidth(i, 3500);
                } else if (i == 6) { // Проволока - средний
                    sheet.setColumnWidth(i, 4000);
                } else { // Расход, кг - средний
                    sheet.setColumnWidth(i, 3500);
                }
            }

            // Заполняем тестовыми данными для конкретного аппарата
            int rowNum = 6;
            String[] welderNames = {"Тест", "Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
            String[] positions = {"Электросварщик", "Сварщик 6 разряда", "Сварщик 5 разряда", "Сварщик 4 разряда", "Сварщик 3 разряда"};
            String[] wireTypes = {"1.2 Св08Г2С", "1.2 Св08Г2С", "1.0 Св08Г2С", "1.2 Св08Г2С", "1.0 Св08Г2С"};

            for (int i = 0; i < 20; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(welderNames[i % welderNames.length]); // Сварщик
                row.createCell(1).setCellValue(positions[i % positions.length]); // Должность
                row.createCell(2).setCellValue(String.format("%02d:%02d:%02d",
                        8 + (int)(Math.random() * 8), (int)(Math.random() * 60), (int)(Math.random() * 60))); // Время в сети (08:00-16:00)
                row.createCell(3).setCellValue(String.format("%02d:%02d:%02d",
                        1 + (int)(Math.random() * 3), (int)(Math.random() * 60), (int)(Math.random() * 60))); // Время горения дуги (01:00-04:00)
                row.createCell(4).setCellValue(String.format("%.2f%%", 20.0 + (Math.random() * 40.0))); // Эффективность работы (20-60%)
                row.createCell(5).setCellValue(String.format("%.1f кВт*ч", 5.0 + (Math.random() * 10.0))); // Энергия (5-15 кВт*ч)
                row.createCell(6).setCellValue(wireTypes[i % wireTypes.length]); // Проволока
                row.createCell(7).setCellValue(String.format("%.2f", 10.0 + (Math.random() * 20.0))); // Расход, кг (10-30 кг)
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateMaterialsPdfWithData(Integer weldingMachineId, String machineName, String period) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Добавляем информацию о временном интервале
        String[] periodInfo = generatePeriodInfo(period);

        Paragraph periodTitle = new Paragraph(periodInfo[0]);
        periodTitle.setFontSize(14);
        periodTitle.setBold();
        document.add(periodTitle);

        Paragraph periodDescription = new Paragraph(periodInfo[1]);
        periodDescription.setFontSize(12);
        document.add(periodDescription);
        document.add(new Paragraph(""));

        // Заголовок отчета с информацией об аппарате
        String titleText = (weldingMachineId != null) ?
                "Отчет по расходу проволоки ID: " + weldingMachineId + " (" + machineName + ")" :
                "Отчет по расходу проволоки (ID не указан)";
        Paragraph title = new Paragraph(titleText);
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));

        // Данные для конкретного аппарата
        String[] welderNames = {"Тест", "Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
        String[] positions = {"Электросварщик", "Сварщик 6 разряда", "Сварщик 5 разряда", "Сварщик 4 разряда", "Сварщик 3 разряда"};
        String[] wireTypes = {"1.2 Св08Г2С", "1.2 Св08Г2С", "1.0 Св08Г2С", "1.2 Св08Г2С", "1.0 Св08Г2С"};

        for (int i = 0; i < 18; i++) {
            document.add(new Paragraph("Запись " + (i + 1) + ":"));
            document.add(new Paragraph("Сварщик: " + welderNames[i % welderNames.length]));
            document.add(new Paragraph("Должность: " + positions[i % positions.length]));
            document.add(new Paragraph("Время в сети: " + String.format("%02d:%02d:%02d",
                    8 + (int)(Math.random() * 8), (int)(Math.random() * 60), (int)(Math.random() * 60))));
            document.add(new Paragraph("Время горения дуги: " + String.format("%02d:%02d:%02d",
                    1 + (int)(Math.random() * 3), (int)(Math.random() * 60), (int)(Math.random() * 60))));
            document.add(new Paragraph("Эффективность работы: " + String.format("%.2f%%", 20.0 + (Math.random() * 40.0))));
            document.add(new Paragraph("Энергия: " + String.format("%.1f кВт*ч", 5.0 + (Math.random() * 10.0))));
            document.add(new Paragraph("Проволока: " + wireTypes[i % wireTypes.length]));
            document.add(new Paragraph("Расход: " + String.format("%.2f кг", 10.0 + (Math.random() * 20.0))));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateMaterialsCsvWithData(Integer weldingMachineId, String machineName, String period) throws IOException {
        StringBuilder csv = new StringBuilder();

        // Добавляем информацию о временном интервале
        String[] periodInfo = generatePeriodInfo(period);
        csv.append(periodInfo[0]).append("\n");
        csv.append(periodInfo[1]).append("\n");

        String machineInfo = (weldingMachineId != null) ?
                "Отчет по расходу проволоки ID: " + weldingMachineId + " (" + machineName + ")" :
                "Отчет по расходу проволоки (ID не указан)";
        csv.append(machineInfo).append("\n");
        csv.append("Сварщик,Должность,Время в сети,Время горения дуги,Эффективность работы,Энергия,Проволока,Расход (кг)\n");

        String[] welderNames = {"Тест", "Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
        String[] positions = {"Электросварщик", "Сварщик 6 разряда", "Сварщик 5 разряда", "Сварщик 4 разряда", "Сварщик 3 разряда"};
        String[] wireTypes = {"1.2 Св08Г2С", "1.2 Св08Г2С", "1.0 Св08Г2С", "1.2 Св08Г2С", "1.0 Св08Г2С"};

        for (int i = 0; i < 20; i++) {
            csv.append(welderNames[i % welderNames.length]).append(",");
            csv.append(positions[i % positions.length]).append(",");
            csv.append(String.format("%02d:%02d:%02d",
                    8 + (int)(Math.random() * 8), (int)(Math.random() * 60), (int)(Math.random() * 60))).append(",");
            csv.append(String.format("%02d:%02d:%02d",
                    1 + (int)(Math.random() * 3), (int)(Math.random() * 60), (int)(Math.random() * 60))).append(",");
            csv.append(String.format("%.2f%%", 20.0 + (Math.random() * 40.0))).append(",");
            csv.append(String.format("%.1f кВт*ч", 5.0 + (Math.random() * 10.0))).append(",");
            csv.append(wireTypes[i % wireTypes.length]).append(",");
            csv.append(String.format("%.2f", 10.0 + (Math.random() * 20.0)));
            csv.append("\n");
        }

        return addBomToCsv(csv.toString());
    }

    // Методы для генерации отчета по сварочным швам
    private byte[] generateWeldsReportWithData(String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateWeldsExcelWithData();
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateWeldsPdfWithData();
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateWeldsCsvWithData();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateWeldsExcelWithData() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по сварочным швам");

            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Средний ток", "Среднее напряжение", "Начало шва", "Длительность шва (с)"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем тестовыми данными
            int rowNum = 1;
            String[] weldTypes = {"Стыковой", "Угловой", "Тавровый", "Нахлесточный", "Торцевой"};
            String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};
            String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250"};
            String[] materials = {"Сталь Ст3", "Нержавеющая сталь", "Алюминий", "Чугун"};

            for (int i = 0; i < 30; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(4000 + i);
                row.createCell(1).setCellValue(weldTypes[i % weldTypes.length]);
                row.createCell(2).setCellValue(welders[i % welders.length]);
                row.createCell(3).setCellValue(equipment[i % equipment.length]);
                row.createCell(4).setCellValue(materials[i % materials.length]);
                row.createCell(5).setCellValue(50 + (int)(Math.random() * 450)); // 50-500 мм
                row.createCell(6).setCellValue(3.0 + (Math.random() * 17.0)); // 3-20 мм
                row.createCell(7).setCellValue(150 + (int)(Math.random() * 150)); // 150-300 А
                row.createCell(8).setCellValue(18 + (int)(Math.random() * 12)); // 18-30 В
                row.createCell(9).setCellValue(200 + (int)(Math.random() * 300)); // 200-500 мм/мин
                row.createCell(10).setCellValue(85 + (int)(Math.random() * 15)); // 85-100%
                row.createCell(11).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateWeldsPdfWithData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Заголовок отчета
        Paragraph title = new Paragraph("Отчет по сварочным швам");
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));

        // Данные
        String[] weldTypes = {"Стыковой", "Угловой", "Тавровый", "Нахлесточный", "Торцевой"};
        String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};
        String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250"};
        String[] materials = {"Сталь Ст3", "Нержавеющая сталь", "Алюминий", "Чугун"};

        for (int i = 0; i < 25; i++) {
            document.add(new Paragraph("Сварочный шов " + (i + 1) + ":"));
            document.add(new Paragraph("ID: " + (4000 + i)));
            document.add(new Paragraph("Тип шва: " + weldTypes[i % weldTypes.length]));
            document.add(new Paragraph("Сварщик: " + welders[i % welders.length]));
            document.add(new Paragraph("Оборудование: " + equipment[i % equipment.length]));
            document.add(new Paragraph("Материал: " + materials[i % materials.length]));
            document.add(new Paragraph("Длина: " + (50 + (int)(Math.random() * 450)) + " мм"));
            document.add(new Paragraph("Толщина: " + String.format("%.1f", 3.0 + (Math.random() * 15.0)) + " мм"));
            document.add(new Paragraph("Ток: " + (150 + (int)(Math.random() * 150)) + " А"));
            document.add(new Paragraph("Напряжение: " + (18 + (int)(Math.random() * 12)) + " В"));
            document.add(new Paragraph("Скорость: " + (200 + (int)(Math.random() * 300)) + " мм/мин"));
            document.add(new Paragraph("Качество: " + (85 + (int)(Math.random() * 15)) + "%"));
            document.add(new Paragraph("Дата: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateWeldsCsvWithData() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID шва,Тип шва,Сварщик,Оборудование,Материал,Длина (мм),Толщина (мм),Ток (А),Напряжение (В),Скорость (мм/мин),Качество (%),Дата\n");

        String[] weldTypes = {"Стыковой", "Угловой", "Тавровый", "Нахлесточный", "Торцевой"};
        String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};
        String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250"};
        String[] materials = {"Сталь Ст3", "Нержавеющая сталь", "Алюминий", "Чугун"};

        for (int i = 0; i < 25; i++) {
            csv.append(4000 + i).append(",");
            csv.append(weldTypes[i % weldTypes.length]).append(",");
            csv.append(welders[i % welders.length]).append(",");
            csv.append(equipment[i % equipment.length]).append(",");
            csv.append(materials[i % materials.length]).append(",");
            csv.append(50 + (int)(Math.random() * 450)).append(",");
            csv.append(String.format("%.1f", 3.0 + (Math.random() * 15.0))).append(",");
            csv.append(150 + (int)(Math.random() * 150)).append(",");
            csv.append(18 + (int)(Math.random() * 12)).append(",");
            csv.append(200 + (int)(Math.random() * 300)).append(",");
            csv.append(85 + (int)(Math.random() * 15)).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28)));
            csv.append("\n");
        }

        return addBomToCsv(csv.toString());
    }

    // Методы для генерации отчета по ошибкам
    private byte[] generateMalfunctionExcelData(Integer weldingMachineId, String machineName, String period) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по ошибкам оборудования");

            // Добавляем информацию о временном интервале
            String[] periodInfo = generatePeriodInfo(period);

            Row periodRow = sheet.createRow(0);
            Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue(periodInfo[0]);
            CellStyle periodStyle = workbook.createCellStyle();
            Font periodFont = workbook.createFont();
            periodFont.setBold(true);
            periodFont.setFontHeightInPoints((short) 12);
            periodStyle.setFont(periodFont);
            periodCell.setCellStyle(periodStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            // Добавляем описание периода
            Row descriptionRow = sheet.createRow(1);
            Cell descriptionCell = descriptionRow.createCell(0);
            descriptionCell.setCellValue(periodInfo[1]);
            CellStyle descriptionStyle = workbook.createCellStyle();
            Font descriptionFont = workbook.createFont();
            descriptionFont.setFontHeightInPoints((short) 10);
            descriptionStyle.setFont(descriptionFont);
            descriptionCell.setCellStyle(descriptionStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 4));

            // Добавляем информацию о выбранном аппарате
            Row titleRow = sheet.createRow(2);
            Cell titleCell = titleRow.createCell(0);
            String machineInfo = (weldingMachineId != null) ?
                    "Отчет по ошибкам оборудования ID: " + weldingMachineId + " (" + machineName + ")" :
                    "Отчет по ошибкам оборудования (ID не указан)";
            titleCell.setCellValue(machineInfo);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 4));

            // Создаем заголовки (начиная с 5-й строки)
            Row headerRow = sheet.createRow(5);
            String[] headers = {
                    "Дата", "Оборудование", "Тип неисправности", "Описание", "Статус"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                // Устанавливаем разную ширину столбцов согласно макету
                if (i == 0 || i == 4) { // Первый и последний столбцы - узкие
                    sheet.setColumnWidth(i, 2500);
                } else if (i == 2) { // Третий столбец - самый широкий
                    sheet.setColumnWidth(i, 6000);
                } else { // Второй и четвертый столбцы - средние
                    sheet.setColumnWidth(i, 4000);
                }
            }

            // Заполняем тестовыми данными для конкретного аппарата
            int rowNum = 6;
            String[] errorTypes = {"Электрическая", "Механическая", "Термическая", "Программная", "Сетевая"};
            String[] descriptions = {"Короткое замыкание", "Износ деталей", "Перегрев", "Сбой ПО", "Потеря связи"};
            String[] statuses = {"Открыта", "В работе", "Решена", "Закрыта"};

            // Генерируем даты в зависимости от периода
            String[] dates = generateDatesForPeriod(period, 20);

            for (int i = 0; i < 20; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dates[i]); // Дата
                row.createCell(1).setCellValue(machineName); // Оборудование
                row.createCell(2).setCellValue(errorTypes[i % errorTypes.length]); // Тип неисправности
                row.createCell(3).setCellValue(descriptions[i % descriptions.length]); // Описание
                row.createCell(4).setCellValue(statuses[i % statuses.length]); // Статус
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateMalfunctionPdfData(Integer weldingMachineId, String machineName, String period) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Добавляем информацию о временном интервале
        String[] periodInfo = generatePeriodInfo(period);

        Paragraph periodTitle = new Paragraph(periodInfo[0]);
        periodTitle.setFontSize(14);
        periodTitle.setBold();
        document.add(periodTitle);

        Paragraph periodDescription = new Paragraph(periodInfo[1]);
        periodDescription.setFontSize(12);
        document.add(periodDescription);
        document.add(new Paragraph(""));

        // Заголовок отчета с информацией об аппарате
        String titleText = (weldingMachineId != null) ?
                "Отчет по ошибкам сварочного оборудования ID: " + weldingMachineId + " (" + machineName + ")" :
                "Отчет по ошибкам сварочного оборудования (ID не указан)";
        Paragraph title = new Paragraph(titleText);
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));

        // Данные для конкретного аппарата
        String[] errorTypes = {"Электрическая", "Механическая", "Термическая", "Программная", "Сетевая"};
        String[] descriptions = {"Короткое замыкание", "Износ деталей", "Перегрев", "Сбой ПО", "Потеря связи"};
        String[] statuses = {"Открыта", "В работе", "Решена", "Закрыта"};

        // Генерируем даты в зависимости от периода
        String[] dates = generateDatesForPeriod(period, 18);

        for (int i = 0; i < 18; i++) {
            document.add(new Paragraph("Неисправность " + (i + 1) + ":"));
            document.add(new Paragraph("Дата: " + dates[i]));
            document.add(new Paragraph("Оборудование: " + machineName)); // Используем название выбранного аппарата
            document.add(new Paragraph("Тип неисправности: " + errorTypes[i % errorTypes.length]));
            document.add(new Paragraph("Описание: " + descriptions[i % descriptions.length]));
            document.add(new Paragraph("Статус: " + statuses[i % statuses.length]));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateMalfunctionCsvData(Integer weldingMachineId, String machineName, String period) throws IOException {
        StringBuilder csv = new StringBuilder();

        // Добавляем информацию о временном интервале
        String[] periodInfo = generatePeriodInfo(period);
        csv.append(periodInfo[0]).append("\n");
        csv.append(periodInfo[1]).append("\n");

        String machineInfo = (weldingMachineId != null) ?
                "Отчет по ошибкам оборудования ID: " + weldingMachineId + " (" + machineName + ")" :
                "Отчет по ошибкам оборудования (ID не указан)";
        csv.append(machineInfo).append("\n");
        csv.append("Дата,Оборудование,Тип неисправности,Описание,Статус\n");

        String[] errorTypes = {"Электрическая", "Механическая", "Термическая", "Программная", "Сетевая"};
        String[] descriptions = {"Короткое замыкание", "Износ деталей", "Перегрев", "Сбой ПО", "Потеря связи"};
        String[] statuses = {"Открыта", "В работе", "Решена", "Закрыта"};

        // Генерируем даты в зависимости от периода
        String[] dates = generateDatesForPeriod(period, 18);

        for (int i = 0; i < 18; i++) {
            csv.append(dates[i]).append(",");
            csv.append(machineName).append(","); // Используем название выбранного аппарата
            csv.append(errorTypes[i % errorTypes.length]).append(",");
            csv.append(descriptions[i % descriptions.length]).append(",");
            csv.append(statuses[i % statuses.length]);
            csv.append("\n");
        }

        return addBomToCsv(csv.toString());
    }

    // Методы для генерации отчета по нарушениям
    private byte[] generateViolationsReportWithData(String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateViolationsExcelWithData();
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateViolationsPdfWithData();
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateViolationsCsvWithData();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateViolationsExcelWithData() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по нарушениям");

            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "ID нарушения", "Шов", "Сварщик", "Тип нарушения", "Описание", "Критичность",
                    "Дата обнаружения", "Дата исправления", "Ответственный", "Статус", "Штраф"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем тестовыми данными
            int rowNum = 1;
            String[] weldTypes = {"Стыковой", "Угловой", "Тавровый", "Нахлесточный"};
            String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
            String[] violationTypes = {"Технологическое", "Качественное", "Временное", "Безопасности"};
            String[] descriptions = {"Несоответствие режиму", "Пористость", "Превышение времени", "Отсутствие СИЗ"};
            String[] criticality = {"Низкая", "Средняя", "Высокая", "Критическая"};
            String[] responsible = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
            String[] statuses = {"Обнаружено", "Исправляется", "Исправлено", "Проверено"};

            for (int i = 0; i < 18; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(6000 + i);
                row.createCell(1).setCellValue(weldTypes[i % weldTypes.length] + " шов №" + (i + 1));
                row.createCell(2).setCellValue(welders[i % welders.length]);
                row.createCell(3).setCellValue(violationTypes[i % violationTypes.length]);
                row.createCell(4).setCellValue(descriptions[i % descriptions.length]);
                row.createCell(5).setCellValue(criticality[i % criticality.length]);
                row.createCell(6).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
                row.createCell(7).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
                row.createCell(8).setCellValue(responsible[i % responsible.length]);
                row.createCell(9).setCellValue(statuses[i % statuses.length]);
                row.createCell(10).setCellValue(1000 + (int)(Math.random() * 9000)); // 1000-10000 руб
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateViolationsPdfWithData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Заголовок отчета
        Paragraph title = new Paragraph("Отчет по нарушениям при сварке");
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));

        // Данные
        String[] weldTypes = {"Стыковой", "Угловой", "Тавровый", "Нахлесточный"};
        String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
        String[] violationTypes = {"Технологическое", "Качественное", "Временное", "Безопасности"};
        String[] descriptions = {"Несоответствие режиму", "Пористость", "Превышение времени", "Отсутствие СИЗ"};
        String[] criticality = {"Низкая", "Средняя", "Высокая", "Критическая"};
        String[] responsible = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
        String[] statuses = {"Обнаружено", "Исправляется", "Исправлено", "Проверено"};

        for (int i = 0; i < 15; i++) {
            document.add(new Paragraph("Нарушение " + (i + 1) + ":"));
            document.add(new Paragraph("ID: " + (6000 + i)));
            document.add(new Paragraph("Шов: " + weldTypes[i % weldTypes.length] + " шов №" + (i + 1)));
            document.add(new Paragraph("Сварщик: " + welders[i % welders.length]));
            document.add(new Paragraph("Тип нарушения: " + violationTypes[i % violationTypes.length]));
            document.add(new Paragraph("Описание: " + descriptions[i % descriptions.length]));
            document.add(new Paragraph("Критичность: " + criticality[i % criticality.length]));
            document.add(new Paragraph("Дата обнаружения: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("Дата исправления: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("Ответственный: " + responsible[i % responsible.length]));
            document.add(new Paragraph("Статус: " + statuses[i % statuses.length]));
            document.add(new Paragraph("Штраф: " + (1000 + (int)(Math.random() * 9000)) + " руб"));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateViolationsCsvWithData() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID нарушения,Шов,Сварщик,Тип нарушения,Описание,Критичность,Дата обнаружения,Дата исправления,Ответственный,Статус,Штраф\n");

        String[] weldTypes = {"Стыковой", "Угловой", "Тавровый", "Нахлесточный"};
        String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
        String[] violationTypes = {"Технологическое", "Качественное", "Временное", "Безопасности"};
        String[] descriptions = {"Несоответствие режиму", "Пористость", "Превышение времени", "Отсутствие СИЗ"};
        String[] criticality = {"Низкая", "Средняя", "Высокая", "Критическая"};
        String[] responsible = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
        String[] statuses = {"Обнаружено", "Исправляется", "Исправлено", "Проверено"};

        for (int i = 0; i < 15; i++) {
            csv.append(6000 + i).append(",");
            csv.append(weldTypes[i % weldTypes.length] + " шов №" + (i + 1)).append(",");
            csv.append(welders[i % welders.length]).append(",");
            csv.append(violationTypes[i % violationTypes.length]).append(",");
            csv.append(descriptions[i % descriptions.length]).append(",");
            csv.append(criticality[i % criticality.length]).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28))).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28))).append(",");
            csv.append(responsible[i % responsible.length]).append(",");
            csv.append(statuses[i % statuses.length]).append(",");
            csv.append(1000 + (int)(Math.random() * 9000));
            csv.append("\n");
        }

        return addBomToCsv(csv.toString());
    }

    // Методы для генерации отчета по заданиям
    private byte[] generateTasksReportWithData(String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateTasksExcelWithData();
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateTasksPdfWithData();
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateTasksCsvWithData();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateTasksExcelWithData() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по заданиям");

            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "ID задания", "Название", "Описание", "Сварщик", "Оборудование", "Материал",
                    "Плановая дата", "Фактическая дата", "Статус", "Прогресс (%)", "Приоритет"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем тестовыми данными
            int rowNum = 1;
            String[] taskNames = {"Сварка трубопровода", "Ремонт конструкции", "Изготовление детали", "Сборка узла", "Восстановление износа"};
            String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};
            String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250"};
            String[] materials = {"Сталь Ст3", "Нержавеющая сталь", "Алюминий", "Чугун"};
            String[] statuses = {"Новое", "В работе", "На проверке", "Завершено", "Отменено"};
            String[] priorities = {"Низкий", "Средний", "Высокий", "Критический"};

            for (int i = 0; i < 25; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(7000 + i);
                row.createCell(1).setCellValue(taskNames[i % taskNames.length] + " №" + (i + 1));
                row.createCell(2).setCellValue("Детальное описание задания " + (i + 1));
                row.createCell(3).setCellValue(welders[i % welders.length]);
                row.createCell(4).setCellValue(equipment[i % equipment.length]);
                row.createCell(5).setCellValue(materials[i % materials.length]);
                row.createCell(6).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
                row.createCell(7).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
                row.createCell(8).setCellValue(statuses[i % statuses.length]);
                row.createCell(9).setCellValue(10 + (int)(Math.random() * 90)); // 10-100%
                row.createCell(10).setCellValue(priorities[i % priorities.length]);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateTasksPdfWithData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Заголовок отчета
        Paragraph title = new Paragraph("Отчет по сварочным заданиям");
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));



        // Данные
        String[] taskNames = {"Сварка трубопровода", "Ремонт конструкции", "Изготовление детали", "Сборка узла", "Восстановление износа"};
        String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};
        String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250"};
        String[] materials = {"Сталь Ст3", "Нержавеющая сталь", "Алюминий", "Чугун"};
        String[] statuses = {"Новое", "В работе", "На проверке", "Завершено", "Отменено"};
        String[] priorities = {"Низкий", "Средний", "Высокий", "Критический"};

        for (int i = 0; i < 20; i++) {
            document.add(new Paragraph("Задание " + (i + 1) + ":"));
            document.add(new Paragraph("ID: " + (7000 + i)));
            document.add(new Paragraph("Название: " + taskNames[i % taskNames.length] + " №" + (i + 1)));
            document.add(new Paragraph("Описание: Детальное описание задания " + (i + 1)));
            document.add(new Paragraph("Сварщик: " + welders[i % welders.length]));
            document.add(new Paragraph("Оборудование: " + equipment[i % equipment.length]));
            document.add(new Paragraph("Материал: " + materials[i % materials.length]));
            document.add(new Paragraph("Плановая дата: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("Фактическая дата: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("Статус: " + statuses[i % statuses.length]));
            document.add(new Paragraph("Прогресс: " + (10 + (int)(Math.random() * 10)) + "%"));
            document.add(new Paragraph("Приоритет: " + priorities[i % priorities.length]));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateTasksCsvWithData() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID задания,Название,Описание,Сварщик,Оборудование,Материал,Плановая дата,Фактическая дата,Статус,Прогресс (%),Приоритет\n");

        String[] taskNames = {"Сварка трубопровода", "Ремонт конструкции", "Изготовление детали", "Сборка узла", "Восстановление износа"};
        String[] welders = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К.", "Новиков Н.Н."};
        String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250"};
        String[] materials = {"Сталь Ст3", "Нержавеющая сталь", "Алюминий", "Чугун"};
        String[] statuses = {"Новое", "В работе", "На проверке", "Завершено", "Отменено"};
        String[] priorities = {"Низкий", "Средний", "Высокий", "Критический"};

        for (int i = 0; i < 20; i++) {
            csv.append(7000 + i).append(",");
            csv.append(taskNames[i % taskNames.length] + " №" + (i + 1)).append(",");
            csv.append("Детальное описание задания " + (i + 1)).append(",");
            csv.append(welders[i % welders.length]).append(",");
            csv.append(equipment[i % equipment.length]).append(",");
            csv.append(materials[i % materials.length]).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28))).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28))).append(",");
            csv.append(statuses[i % statuses.length]).append(",");
            csv.append(10 + (int)(Math.random() * 90)).append(",");
            csv.append(priorities[i % priorities.length]);
            csv.append("\n");
        }

        return addBomToCsv(csv.toString());
    }

    private byte[] generateWireConsumptionExcel(List<WireConsumptionReportDTO> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Расход проволоки");

            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Аппарат", "Серийный номер", "Сварщик", "Дата", "Расход проволоки (кг)",
                    "Скорость подачи (м/мин)", "Время сварки (мин)", "Ток (А)", "Напряжение (В)",
                    "Режим сварки", "Тип сварки", "Подразделение"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем данные
            int rowNum = 1;
            for (WireConsumptionReportDTO item : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "");
                row.createCell(1).setCellValue(""); // Serial number removed from DTO
                row.createCell(2).setCellValue(item.getWelderName() != null ? item.getWelderName() : "");
                row.createCell(3).setCellValue(""); // Date removed from DTO
                row.createCell(4).setCellValue(item.getWireConsumption() != null ? item.getWireConsumption().doubleValue() : 0.0);
                row.createCell(5).setCellValue(0.0); // WireFeedRate removed from DTO
                row.createCell(6).setCellValue(item.getArcBurningTime() != null ? item.getArcBurningTime().toMinutes() : 0.0);
                row.createCell(7).setCellValue(0.0); // Current removed from DTO
                row.createCell(8).setCellValue(0.0); // Voltage removed from DTO
                row.createCell(9).setCellValue(""); // WeldingMode removed from DTO
                row.createCell(10).setCellValue(""); // WeldingType removed from DTO
                row.createCell(11).setCellValue(item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateWireConsumptionPdf(List<WireConsumptionReportDTO> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Заголовок
        Paragraph title = new Paragraph("Отчет по расходу проволоки");
        title.setFontSize(18);
        // title.setTextAlignment(TextAlignment.CENTER); // Временно отключено
        document.add(title);

        // Упрощенный список вместо таблицы
        document.add(new Paragraph("Данные по расходу проволоки:"));
        document.add(new Paragraph(""));

        for (WireConsumptionReportDTO item : data) {
            document.add(new Paragraph("Аппарат: " + (item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "")));
            document.add(new Paragraph("Сварщик: " + (item.getWelderName() != null ? item.getWelderName() : "")));
            document.add(new Paragraph("Расход проволоки (кг): " + (item.getWireConsumption() != null ? item.getWireConsumption().toString() : "0")));
            document.add(new Paragraph("Время горения дуги: " + (item.getArcBurningTime() != null ? formatDuration(item.getArcBurningTime()) : "0")));
            document.add(new Paragraph("Время в сети: " + (item.getTimeInNetwork() != null ? formatDuration(item.getTimeInNetwork()) : "0")));
            document.add(new Paragraph("Подразделение: " + (item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "")));
            document.add(new Paragraph("---"));
        }
        document.close();

        return outputStream.toByteArray();
    }

    private byte[] generateWelderExcel(List<WelderReportDTO> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по сварщику");

            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Сварщик", "Email", "Дата", "Общий расход проволоки (кг)", "Общее время сварки (мин)",
                    "Количество сессий", "Средний ток (А)", "Среднее напряжение (В)",
                    "Средняя скорость подачи (м/мин)", "Подразделение", "Аппарат"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем данные
            int rowNum = 1;
            for (WelderReportDTO item : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getWelderName() != null ? item.getWelderName() : "");
                row.createCell(1).setCellValue(item.getWelderEmail() != null ? item.getWelderEmail() : "");
                row.createCell(2).setCellValue(item.getDate() != null ? item.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "");
                row.createCell(3).setCellValue(item.getTotalWireConsumption() != null ? item.getTotalWireConsumption().doubleValue() : 0.0);
                row.createCell(4).setCellValue(item.getTotalWeldingTime() != null ? item.getTotalWeldingTime().doubleValue() : 0.0);
                row.createCell(5).setCellValue(item.getTotalWeldingSessions() != null ? item.getTotalWeldingSessions() : 0);
                row.createCell(6).setCellValue(item.getAverageCurrent() != null ? item.getAverageCurrent().doubleValue() : 0.0);
                row.createCell(7).setCellValue(item.getAverageVoltage() != null ? item.getAverageVoltage().doubleValue() : 0.0);
                row.createCell(8).setCellValue(item.getAverageWireFeedRate() != null ? item.getAverageWireFeedRate().doubleValue() : 0.0);
                row.createCell(9).setCellValue(item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "");
                row.createCell(10).setCellValue(item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateWelderPdf(List<WelderReportDTO> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Заголовок
        Paragraph title = new Paragraph("Отчет по сварщику");
        title.setFontSize(18);
        // title.setTextAlignment(TextAlignment.CENTER); // Временно отключено
        document.add(title);

        // Упрощенный список вместо таблицы
        document.add(new Paragraph("Данные по сварщику:"));
        document.add(new Paragraph(""));

        for (WelderReportDTO item : data) {
            document.add(new Paragraph("Сварщик: " + (item.getWelderName() != null ? item.getWelderName() : "")));
            document.add(new Paragraph("Email: " + (item.getWelderEmail() != null ? item.getWelderEmail() : "")));
            document.add(new Paragraph("Дата: " + (item.getDate() != null ? item.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "")));
            document.add(new Paragraph("Общий расход проволоки (кг): " + (item.getTotalWireConsumption() != null ? item.getTotalWireConsumption().toString() : "0")));
            document.add(new Paragraph("Общее время сварки (мин): " + (item.getTotalWeldingTime() != null ? item.getTotalWeldingTime().toString() : "0")));
            document.add(new Paragraph("Количество сессий: " + (item.getTotalWeldingSessions() != null ? item.getTotalWeldingSessions().toString() : "0")));
            document.add(new Paragraph("Средний ток (А): " + (item.getAverageCurrent() != null ? item.getAverageCurrent().toString() : "0")));
            document.add(new Paragraph("Среднее напряжение (В): " + (item.getAverageVoltage() != null ? item.getAverageVoltage().toString() : "0")));
            document.add(new Paragraph("Средняя скорость подачи (м/мин): " + (item.getAverageWireFeedRate() != null ? item.getAverageWireFeedRate().toString() : "0")));
            document.add(new Paragraph("Подразделение: " + (item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "")));
            document.add(new Paragraph("Аппарат: " + (item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "")));
            document.add(new Paragraph("---"));
        }
        document.close();

        return outputStream.toByteArray();
    }

    private byte[] generateWorkExcel(List<WorkReportDTO> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе");

            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Аппарат", "Серийный номер", "Сварщик", "Время начала", "Время окончания",
                    "Время сварки (мин)", "Ток (А)", "Напряжение (В)", "Режим сварки", "Тип сварки",
                    "Расход проволоки (кг)", "Скорость подачи (м/мин)", "Подразделение", "Примечания"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Заполняем данные
            int rowNum = 1;
            for (WorkReportDTO item : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "");
                row.createCell(1).setCellValue(item.getWeldingMachineSerialNumber() != null ? item.getWeldingMachineSerialNumber() : "");
                row.createCell(2).setCellValue(item.getWelderName() != null ? item.getWelderName() : "");
                row.createCell(3).setCellValue(item.getStartTime() != null ? item.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "");
                row.createCell(4).setCellValue(item.getEndTime() != null ? item.getEndTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "");
                row.createCell(5).setCellValue(item.getWeldingTime() != null ? item.getWeldingTime().doubleValue() : 0.0);
                row.createCell(6).setCellValue(item.getCurrent() != null ? item.getCurrent().doubleValue() : 0.0);
                row.createCell(7).setCellValue(item.getVoltage() != null ? item.getVoltage().doubleValue() : 0.0);
                row.createCell(8).setCellValue(item.getWeldingMode() != null ? item.getWeldingMode() : "");
                row.createCell(9).setCellValue(item.getWeldingType() != null ? item.getWeldingType() : "");
                row.createCell(10).setCellValue(item.getWireConsumption() != null ? item.getWireConsumption().doubleValue() : 0.0);
                row.createCell(11).setCellValue(item.getWireFeedRate() != null ? item.getWireFeedRate().doubleValue() : 0.0);
                row.createCell(12).setCellValue(item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "");
                row.createCell(13).setCellValue(item.getNotes() != null ? item.getNotes() : "");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateWorkPdf(List<WorkReportDTO> data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Заголовок
        Paragraph title = new Paragraph("Отчет по работе");
        title.setFontSize(18);
        // title.setTextAlignment(TextAlignment.CENTER); // Временно отключено
        document.add(title);

        // Упрощенный список вместо таблицы
        document.add(new Paragraph("Данные по работе:"));
        document.add(new Paragraph(""));

        for (WorkReportDTO item : data) {
            document.add(new Paragraph("Аппарат: " + (item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "")));
            document.add(new Paragraph("Серийный номер: " + (item.getWeldingMachineSerialNumber() != null ? item.getWeldingMachineSerialNumber() : "")));
            document.add(new Paragraph("Сварщик: " + (item.getWelderName() != null ? item.getWelderName() : "")));
            document.add(new Paragraph("Время начала: " + (item.getStartTime() != null ? item.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "")));
            document.add(new Paragraph("Время окончания: " + (item.getEndTime() != null ? item.getEndTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "")));
            document.add(new Paragraph("Время сварки (мин): " + (item.getWeldingTime() != null ? item.getWeldingTime().toString() : "0")));
            document.add(new Paragraph("Ток (А): " + (item.getCurrent() != null ? item.getCurrent().toString() : "0")));
            document.add(new Paragraph("Напряжение (В): " + (item.getVoltage() != null ? item.getVoltage().toString() : "0")));
            document.add(new Paragraph("Режим сварки: " + (item.getWeldingMode() != null ? item.getWeldingMode() : "")));
            document.add(new Paragraph("Тип сварки: " + (item.getWeldingType() != null ? item.getWeldingType() : "")));
            document.add(new Paragraph("Расход проволоки (кг): " + (item.getWireConsumption() != null ? item.getWireConsumption().toString() : "0")));
            document.add(new Paragraph("Скорость подачи (м/мин): " + (item.getWireFeedRate() != null ? item.getWireFeedRate().toString() : "0")));
            document.add(new Paragraph("Подразделение: " + (item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "")));
            document.add(new Paragraph("Примечания: " + (item.getNotes() != null ? item.getNotes() : "")));
            document.add(new Paragraph("---"));
        }
        document.close();

        return outputStream.toByteArray();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    /**
     * Определяет заголовки столбцов на основе выбранных пользователем
     */
    private List<String> getSelectedHeaders(List<String> selectedColumns) {
        // Все доступные столбцы для отчета по оборудованию
        Map<String, String> allColumns = new HashMap<>();
        allColumns.put("Сварщик", "Сварщик");
        allColumns.put("Режим", "Режим");
        allColumns.put("Сила тока", "Сила тока, А");
        allColumns.put("Масса проволоки", "Масса проволоки, кг");
        allColumns.put("Напряжение", "Напряжение, V");
        allColumns.put("Проволока", "Проволока, м/мин");
        allColumns.put("Газ л/мин", "Газ, л/мин");
        allColumns.put("Время сварки (с)", "Время сварки (с)");

        // Если не выбраны столбцы или список пустой, используем все
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            return Arrays.asList(
                    "Дата", "Время", "Сварщик", "Сила тока, А",
                    "Масса проволоки, кг", "Напряжение, V", "Проволока, м/мин",
                    "Газ, л/мин", "Время сварки (с)"
            );
        }

        // Создаем список заголовков на основе выбранных столбцов
        List<String> headers = new ArrayList<>();
        headers.add("Дата"); // Дата всегда первая
        headers.add("Время"); // Время всегда второе

        for (String selectedColumn : selectedColumns) {
            String header = allColumns.get(selectedColumn);
            if (header != null && !headers.contains(header)) {
                headers.add(header);
            }
        }

        return headers;
    }

    /**
     * Заполняет строку Excel данными на основе выбранных столбцов
     */
    private void fillRowWithData(Row row, WorkReportDTO item, List<String> headers) {
        int cellIndex = 0;

        for (String header : headers) {
            Cell cell = row.createCell(cellIndex++);

            switch (header) {
                case "Дата":
                    if (item.getStartTime() != null) {
                        cell.setCellValue(item.getStartTime().toLocalDate().toString());
                    } else {
                        cell.setCellValue("N/A");
                    }
                    break;

                case "Время":
                    if (item.getStartTime() != null) {
                        cell.setCellValue(item.getStartTime().toLocalTime().toString());
                    } else {
                        cell.setCellValue("N/A");
                    }
                    break;

                case "Сварщик":
                    cell.setCellValue(item.getWelderName() != null ? item.getWelderName() : "N/A");
                    break;

                case "Сила тока, А":
                    if (item.getCurrent() != null) {
                        cell.setCellValue(item.getCurrent().doubleValue());
                    } else {
                        cell.setCellValue(0.0);
                    }
                    break;

                case "Масса проволоки, кг":
                    if (item.getWireConsumption() != null) {
                        cell.setCellValue(item.getWireConsumption().doubleValue());
                    } else {
                        cell.setCellValue(0.0);
                    }
                    break;

                case "Напряжение, V":
                    if (item.getVoltage() != null) {
                        cell.setCellValue(item.getVoltage().doubleValue());
                    } else {
                        cell.setCellValue(0.0);
                    }
                    break;

                case "Проволока, м/мин":
                    if (item.getWireFeedRate() != null) {
                        cell.setCellValue(item.getWireFeedRate().doubleValue());
                    } else {
                        cell.setCellValue(0.0);
                    }
                    break;

                case "Газ, л/мин":
                    cell.setCellValue(0.0); // Не применимо для блока мониторинга
                    break;

                case "Время сварки (с)":
                    if (item.getWeldingTime() != null) {
                        cell.setCellValue(item.getWeldingTime().doubleValue());
                    } else {
                        cell.setCellValue(0.0);
                    }
                    break;

                default:
                    cell.setCellValue("N/A");
                    break;
            }
        }
    }

    /**
     * Генерирует отчет по расходу проволоки в новом формате
     */
    public byte[] generateWireConsumptionReportNew(
            List<WireConsumptionReportDTO> data,
            WireConsumptionReportTemplateDTO template,
            java.time.LocalDate periodStartDate,
            java.time.LocalDate periodEndDate,
            java.time.LocalTime periodStartTime,
            java.time.LocalTime periodEndTime) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по расходу проволоки");

            // Стили
            CellStyle lightBlueStyle = createLightBlueStyle(workbook);
            CellStyle lightGreenStyle = createLightGreenStyle(workbook);
            CellStyle summaryRowStyle = createSummaryRowStyle(workbook);
            CellStyle dataStyle = workbook.createCellStyle();

            int currentRow = 0;

            // Заголовок отчета
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Отчет по расходу проволоки за период:");
            titleCell.setCellStyle(createHeaderStyle(workbook));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9));

            // Период и пределы тока
            currentRow = addReportHeaderInfo(sheet, currentRow, template, periodStartDate, periodEndDate, periodStartTime, periodEndTime, lightBlueStyle);

            // Заголовки таблицы (две строки)
            currentRow = addTableHeaders(sheet, currentRow, template, lightBlueStyle, lightGreenStyle);

            // Данные
            currentRow = addTableData(sheet, currentRow, data, template, dataStyle, summaryRowStyle);

            // Автоподбор ширины колонок
            List<ColumnDefinition> columns = getColumnDefinitions(template);
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Генерирует отчёт "По работе сварщика" в формате XLSX.
     * Сейчас реализуем шапку файла согласно требованиям; табличная часть будет расширяться по ТЗ.
     */
    public byte[] generateWelderWorkReportNew(
            List<WelderWorkReportDTO> data,
            WelderWorkReportTemplateDTO template,
            java.time.LocalDate periodStartDate,
            java.time.LocalDate periodEndDate,
            java.time.LocalTime periodStartTime,
            java.time.LocalTime periodEndTime) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе сварщика");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle labelStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            labelStyle.setFont(boldFont);

            // Шапка — строки/ячейки по ТЗ
            // Excel indices: row0 = 1, col0 = A

            // Row 1 (index 0)
            Row r1 = sheet.createRow(0);
            // B1-C1
            Cell b1 = r1.createCell(1);
            b1.setCellValue("Отчет по работе сварщика:");
            b1.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 1, 2));

            // E1: "ФИО сварщика", F1: ФИО
            Cell e1 = r1.createCell(4);
            e1.setCellValue("ФИО сварщика");
            e1.setCellStyle(labelStyle);
            Cell f1 = r1.createCell(5);
            f1.setCellValue(template != null && template.getWelderFullName() != null ? template.getWelderFullName() : "");

            // Row 2 (index 1): E2/F2, G2/H2
            Row r2 = sheet.createRow(1);
            Cell e2 = r2.createCell(4);
            e2.setCellValue("таб. №");
            e2.setCellStyle(labelStyle);
            Cell f2 = r2.createCell(5);
            f2.setCellValue(template != null && template.getWelderTabNumber() != null ? template.getWelderTabNumber() : "");

            Cell g2 = r2.createCell(6);
            g2.setCellValue("Профессия:");
            g2.setCellStyle(labelStyle);
            Cell h2 = r2.createCell(7);
            h2.setCellValue(template != null && template.getWelderProfession() != null ? template.getWelderProfession() : "");

            // Row 3 (index 2): E3/F3
            Row r3 = sheet.createRow(2);
            Cell e3 = r3.createCell(4);
            e3.setCellValue("Подразделение:");
            e3.setCellStyle(labelStyle);
            Cell f3 = r3.createCell(5);
            f3.setCellValue(template != null && template.getWelderDepartment() != null ? template.getWelderDepartment() : "");

            // Row 5 (index 4): E5/F5/G5/H5 — за период: с, дата, время
            Row r5 = sheet.createRow(4);
            Cell e5 = r5.createCell(4);
            e5.setCellValue("за период:");
            e5.setCellStyle(labelStyle);
            Cell f5 = r5.createCell(5);
            f5.setCellValue("с");
            f5.setCellStyle(labelStyle);
            Cell g5 = r5.createCell(6);
            g5.setCellValue(periodStartDate != null ? periodStartDate.toString() : "");
            r5.createCell(7).setCellValue("00:00");

            // Row 6 (index 5): F6/G6/H6 — по, дата, время
            Row r6 = sheet.createRow(5);
            Cell f6 = r6.createCell(5);
            f6.setCellValue("по");
            f6.setCellStyle(labelStyle);
            Cell g6 = r6.createCell(6);
            g6.setCellValue(periodEndDate != null ? periodEndDate.toString() : "");
            r6.createCell(7).setCellValue(getPeriodEndTimeDisplay(periodEndDate));

            // Блок по току/интервалам — только если выбран пункт
            boolean includeActualRange = template != null && Boolean.TRUE.equals(template.getIncludeActualCurrentRange());
            int headerRowIndex = 6; // следующий свободный ряд после периода (строки 4, 5)
            if (includeActualRange) {
                // Row 7 (index 6): B7-D7
                Row r7 = sheet.createRow(6);
                Cell b7 = r7.createCell(1);
                b7.setCellValue("Разрешенный диапазон фактического тока, А");
                b7.setCellStyle(labelStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(6, 6, 1, 3));

                // Row 8 (index 7): B8/C8
                Row r8 = sheet.createRow(7);
                Cell b8 = r8.createCell(1);
                b8.setCellValue("min");
                b8.setCellStyle(labelStyle);
                Cell c8 = r8.createCell(2);
                if (template.getActualCurrentMin() != null) {
                    c8.setCellValue(template.getActualCurrentMin());
                }

                // Row 9 (index 8): B9/C9
                Row r9 = sheet.createRow(8);
                Cell b9 = r9.createCell(1);
                b9.setCellValue("max");
                b9.setCellStyle(labelStyle);
                Cell c9 = r9.createCell(2);
                if (template.getActualCurrentMax() != null) {
                    c9.setCellValue(template.getActualCurrentMax());
                }
                headerRowIndex = 9; // следующий свободный после 6,7,8
            }
            // Минимальный интервал между швами — только если включён и задан
            if (template != null && template.getMinIntervalBetweenWeldsSec() != null) {
                Row rMinInterval = sheet.createRow(headerRowIndex);
                rMinInterval.createCell(4).setCellValue("Минимальный интервал между швами, с");
                rMinInterval.getCell(4).setCellStyle(labelStyle);
                rMinInterval.createCell(5).setCellValue(template.getMinIntervalBetweenWeldsSec());
                headerRowIndex++;
            }
            // Минимальный учитываемый шов — только если включён и задан
            if (template != null && template.getMinWeldDurationSec() != null) {
                Row rMinWeld = sheet.createRow(headerRowIndex);
                rMinWeld.createCell(4).setCellValue("Минимальный учитываемый шов, с");
                rMinWeld.getCell(4).setCellStyle(labelStyle);
                rMinWeld.createCell(5).setCellValue(template.getMinWeldDurationSec());
                headerRowIndex++;
            }

            // Таблица: обязательные колонки + выбранные в шаблоне (selectedColumns)
            List<WelderWorkColumnDef> columnDefs = getWelderWorkReportColumnDefinitions(template);
            Row headerRow = sheet.createRow(headerRowIndex);
            for (int c = 0; c < columnDefs.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(columnDefs.get(c).header);
                cell.setCellStyle(labelStyle);
            }

            CellStyle outOfRangeRowStyle = createOutOfRangeRowStyle(workbook);
            CellStyle normalDataStyle = workbook.createCellStyle();

            int rowIdx = headerRowIndex + 1;
            if (data != null) {
                for (WelderWorkReportDTO item : data) {
                    Row row = sheet.createRow(rowIdx++);
                    boolean highlight = Boolean.TRUE.equals(item.getCurrentOutOfRange());
                    CellStyle rowStyle = highlight ? outOfRangeRowStyle : normalDataStyle;
                    for (int col = 0; col < columnDefs.size(); col++) {
                        Cell cell = row.createCell(col);
                        String key = columnDefs.get(col).key;
                        CellStyle cellStyle = rowStyle;
                        if ("currentAmps".equals(key) || "voltageVolts".equals(key) || "weldDurationSec".equals(key)) {
                            String format = "voltageVolts".equals(key) ? "0.0" : "0";
                            cellStyle = createCellStyleWithNumberFormat(workbook, rowStyle, format);
                        }
                        cell.setCellStyle(cellStyle);
                        setWelderWorkCellValue(cell, item, key);
                    }
                }
            }

            int maxColumn = columnDefs.size() - 1;
            for (int i = 0; i <= maxColumn; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, (int) (currentWidth * 1.1));
            }
            int minWidthForTitle = 3000;
            if (sheet.getColumnWidth(1) + sheet.getColumnWidth(2) < minWidthForTitle) {
                sheet.setColumnWidth(1, minWidthForTitle / 2);
                sheet.setColumnWidth(2, minWidthForTitle / 2);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Генерирует отчёт "По работе сварщика" в формате XLSX для нескольких сварщиков:
     * один лист, подряд блоки «заголовок (ФИО, подразделение, таб. №) + таблица» для каждого сварщика.
     * Если по сварщику данных нет — выводится одна строка с нулями.
     */
    public byte[] generateWelderWorkReportMultiSection(
            List<WelderWorkReportSectionDTO> sections,
            WelderWorkReportTemplateDTO template,
            java.time.LocalDate periodStartDate,
            java.time.LocalDate periodEndDate,
            java.time.LocalTime periodStartTime,
            java.time.LocalTime periodEndTime) throws IOException {
        if (sections == null || sections.isEmpty()) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Отчет по работе сварщика");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе сварщика");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle labelStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            labelStyle.setFont(boldFont);

            List<WelderWorkColumnDef> columnDefs = getWelderWorkReportColumnDefinitions(template);
            CellStyle outOfRangeRowStyle = createOutOfRangeRowStyle(workbook);
            CellStyle normalDataStyle = workbook.createCellStyle();

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("Отчет по работе сварщика:");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 1, 2));

            boolean periodAndRangeWritten = false;

            for (int s = 0; s < sections.size(); s++) {
                WelderWorkReportSectionDTO section = sections.get(s);
                if (s > 0) {
                    rowIdx++;
                }

                // Заголовок блока: ФИО, таб. №, Подразделение
                Row r1 = sheet.createRow(rowIdx++);
                Cell e1 = r1.createCell(4);
                e1.setCellValue("ФИО сварщика");
                e1.setCellStyle(labelStyle);
                Cell f1 = r1.createCell(5);
                f1.setCellValue(section.getWelderFullName() != null ? section.getWelderFullName() : "");

                Row r2 = sheet.createRow(rowIdx++);
                Cell e2 = r2.createCell(4);
                e2.setCellValue("таб. №");
                e2.setCellStyle(labelStyle);
                Cell f2 = r2.createCell(5);
                f2.setCellValue(section.getWelderTabNumber() != null ? section.getWelderTabNumber() : "");

                Row r3 = sheet.createRow(rowIdx++);
                Cell e3 = r3.createCell(4);
                e3.setCellValue("Подразделение:");
                e3.setCellStyle(labelStyle);
                Cell f3 = r3.createCell(5);
                f3.setCellValue(section.getWelderDepartment() != null ? section.getWelderDepartment() : "");

                rowIdx++;

                if (!periodAndRangeWritten) {
                    Row r5 = sheet.createRow(rowIdx++);
                    Cell e5 = r5.createCell(4);
                    e5.setCellValue("за период:");
                    e5.setCellStyle(labelStyle);
                    Cell f5 = r5.createCell(5);
                    f5.setCellValue("с");
                    f5.setCellStyle(labelStyle);
                    Cell g5 = r5.createCell(6);
                    g5.setCellValue(periodStartDate != null ? periodStartDate.toString() : "");
                    r5.createCell(7).setCellValue("00:00");

                    Row r6 = sheet.createRow(rowIdx++);
                    Cell f6 = r6.createCell(5);
                    f6.setCellValue("по");
                    f6.setCellStyle(labelStyle);
                    Cell g6 = r6.createCell(6);
                    g6.setCellValue(periodEndDate != null ? periodEndDate.toString() : "");
                    r6.createCell(7).setCellValue(getPeriodEndTimeDisplay(periodEndDate));

                    boolean includeActualRange = template != null && Boolean.TRUE.equals(template.getIncludeActualCurrentRange());
                    if (includeActualRange) {
                        Row r7 = sheet.createRow(rowIdx++);
                        Cell b7 = r7.createCell(1);
                        b7.setCellValue("Разрешенный диапазон фактического тока, А");
                        b7.setCellStyle(labelStyle);
                        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 3));

                        Row r8 = sheet.createRow(rowIdx++);
                        Cell b8 = r8.createCell(1);
                        b8.setCellValue("min");
                        b8.setCellStyle(labelStyle);
                        Cell c8 = r8.createCell(2);
                        if (template.getActualCurrentMin() != null) c8.setCellValue(template.getActualCurrentMin());

                        Row r9 = sheet.createRow(rowIdx++);
                        Cell b9 = r9.createCell(1);
                        b9.setCellValue("max");
                        b9.setCellStyle(labelStyle);
                        Cell c9 = r9.createCell(2);
                        if (template.getActualCurrentMax() != null) c9.setCellValue(template.getActualCurrentMax());
                    }
                    // Минимальный интервал между швами — только если включён и задан
                    if (template != null && template.getMinIntervalBetweenWeldsSec() != null) {
                        Row rMinInterval = sheet.createRow(rowIdx++);
                        rMinInterval.createCell(4).setCellValue("Минимальный интервал между швами, с");
                        rMinInterval.getCell(4).setCellStyle(labelStyle);
                        rMinInterval.createCell(5).setCellValue(template.getMinIntervalBetweenWeldsSec());
                    }
                    // Минимальный учитываемый шов — только если включён и задан
                    if (template != null && template.getMinWeldDurationSec() != null) {
                        Row rMinWeld = sheet.createRow(rowIdx++);
                        rMinWeld.createCell(4).setCellValue("Минимальный учитываемый шов, с");
                        rMinWeld.getCell(4).setCellStyle(labelStyle);
                        rMinWeld.createCell(5).setCellValue(template.getMinWeldDurationSec());
                    }
                    periodAndRangeWritten = true;
                    rowIdx++;
                }

                // Заголовок таблицы
                Row headerRow = sheet.createRow(rowIdx++);
                for (int c = 0; c < columnDefs.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(columnDefs.get(c).header);
                    cell.setCellStyle(labelStyle);
                }

                List<WelderWorkReportDTO> rows = section.getRows();
                if (rows == null) rows = Collections.emptyList();
                if (rows.isEmpty()) {
                    WelderWorkReportDTO zeroRow = createZeroWelderWorkRow();
                    rows = Collections.singletonList(zeroRow);
                }
                for (WelderWorkReportDTO item : rows) {
                    Row row = sheet.createRow(rowIdx++);
                    boolean highlight = Boolean.TRUE.equals(item.getCurrentOutOfRange());
                    CellStyle rowStyle = highlight ? outOfRangeRowStyle : normalDataStyle;
                    for (int col = 0; col < columnDefs.size(); col++) {
                        Cell cell = row.createCell(col);
                        String key = columnDefs.get(col).key;
                        CellStyle cellStyle = rowStyle;
                        if ("currentAmps".equals(key) || "voltageVolts".equals(key) || "weldDurationSec".equals(key)) {
                            String format = "voltageVolts".equals(key) ? "0.0" : "0";
                            cellStyle = createCellStyleWithNumberFormat(workbook, rowStyle, format);
                        }
                        cell.setCellStyle(cellStyle);
                        setWelderWorkCellValue(cell, item, key);
                    }
                }
            }

            int maxColumn = columnDefs.size() - 1;
            for (int i = 0; i <= maxColumn; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, (int) (currentWidth * 1.1));
            }
            int minWidthForTitle = 3000;
            if (sheet.getColumnWidth(1) + sheet.getColumnWidth(2) < minWidthForTitle) {
                sheet.setColumnWidth(1, minWidthForTitle / 2);
                sheet.setColumnWidth(2, minWidthForTitle / 2);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private WelderWorkReportDTO createZeroWelderWorkRow() {
        WelderWorkReportDTO row = new WelderWorkReportDTO();
        row.setIndex(1);
        row.setDate(java.time.LocalDate.now());
        row.setWeldStartTime(java.time.LocalTime.MIDNIGHT);
        row.setWorkMode("");
        row.setCurrentAmps(BigDecimal.ZERO);
        row.setVoltageVolts(BigDecimal.ZERO);
        row.setWeldDurationSec(BigDecimal.ZERO);
        row.setWireFeedSpeedMpm(BigDecimal.ZERO);
        row.setWireConsumptionKg(BigDecimal.ZERO);
        row.setEnergyConsumedKwh(BigDecimal.ZERO);
        row.setGasConsumptionL(BigDecimal.ZERO);
        row.setEquipmentModel("");
        row.setEquipmentName("");
        row.setCurrentOutOfRange(false);
        return row;
    }

    private static class WelderWorkColumnDef {
        final String key;
        final String header;
        WelderWorkColumnDef(String key, String header) {
            this.key = key;
            this.header = header;
        }
    }

    private List<WelderWorkColumnDef> getWelderWorkReportColumnDefinitions(WelderWorkReportTemplateDTO template) {
        List<WelderWorkColumnDef> list = new ArrayList<>();
        list.add(new WelderWorkColumnDef("index", "№ п/п"));
        list.add(new WelderWorkColumnDef("date", "Дата"));
        list.add(new WelderWorkColumnDef("weldStartTime", "Время начала шва"));
        list.add(new WelderWorkColumnDef("workMode", "Режим работы оборудования"));
        list.add(new WelderWorkColumnDef("currentAmps", "Рабочий ток А"));
        list.add(new WelderWorkColumnDef("voltageVolts", "Рабочее напряжение В"));
        list.add(new WelderWorkColumnDef("weldDurationSec", "Время шва с"));
        List<String> selected = template != null && template.getSelectedColumns() != null
                ? template.getSelectedColumns() : Collections.emptyList();
        if (selected.contains("equipmentModel")) list.add(new WelderWorkColumnDef("equipmentModel", "Модель оборудования"));
        if (selected.contains("equipmentName")) list.add(new WelderWorkColumnDef("equipmentName", "Наименование оборудования"));
        if (selected.contains("wireFeedSpeed")) list.add(new WelderWorkColumnDef("wireFeedSpeed", "Скорость подачи проволоки, м/мин"));
        if (selected.contains("consumption")) list.add(new WelderWorkColumnDef("wireConsumptionKg", "Расход проволоки, кг"));
        if (selected.contains("energyConsumed")) list.add(new WelderWorkColumnDef("energyConsumedKwh", "Затраченная энергия на шов, кВт*ч"));
        if (selected.contains("gasConsumption")) list.add(new WelderWorkColumnDef("gasConsumptionL", "Расход газа, л"));
        return list;
    }

    private void setWelderWorkCellValue(Cell cell, WelderWorkReportDTO item, String key) {
        switch (key) {
            case "index":
                if (item.getIndex() != null) cell.setCellValue(item.getIndex());
                break;
            case "date":
                cell.setCellValue(item.getDate() != null ? item.getDate().toString() : "");
                break;
            case "weldStartTime":
                cell.setCellValue(item.getWeldStartTime() != null
                        ? item.getWeldStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "");
                break;
            case "workMode":
                cell.setCellValue(item.getWorkMode() != null ? item.getWorkMode() : "");
                break;
            case "currentAmps":
                if (item.getCurrentAmps() != null) cell.setCellValue(item.getCurrentAmps().doubleValue());
                break;
            case "voltageVolts":
                if (item.getVoltageVolts() != null) cell.setCellValue(item.getVoltageVolts().doubleValue());
                break;
            case "weldDurationSec":
                if (item.getWeldDurationSec() != null) cell.setCellValue(item.getWeldDurationSec().doubleValue());
                break;
            case "equipmentModel":
                cell.setCellValue(item.getEquipmentModel() != null ? item.getEquipmentModel() : "");
                break;
            case "equipmentName":
                cell.setCellValue(item.getEquipmentName() != null ? item.getEquipmentName() : "");
                break;
            case "wireFeedSpeed":
                if (item.getWireFeedSpeedMpm() != null) cell.setCellValue(item.getWireFeedSpeedMpm().doubleValue());
                break;
            case "wireConsumptionKg":
                if (item.getWireConsumptionKg() != null) cell.setCellValue(item.getWireConsumptionKg().doubleValue());
                break;
            case "energyConsumedKwh":
                if (item.getEnergyConsumedKwh() != null) cell.setCellValue(item.getEnergyConsumedKwh().doubleValue());
                break;
            case "gasConsumptionL":
                if (item.getGasConsumptionL() != null) cell.setCellValue(item.getGasConsumptionL().doubleValue());
                break;
            default:
                cell.setCellValue("");
                break;
        }
    }

    // ---------- Отчёт "По работе оборудования" ----------

    private static class EquipmentWorkColumnDef {
        final String key;
        final String header;
        EquipmentWorkColumnDef(String key, String header) { this.key = key; this.header = header; }
    }

    private List<EquipmentWorkColumnDef> getEquipmentWorkReportColumnDefinitions(EquipmentWorkReportTemplateDTO template) {
        List<EquipmentWorkColumnDef> list = new ArrayList<>();
        list.add(new EquipmentWorkColumnDef("index", "№ п/п"));
        list.add(new EquipmentWorkColumnDef("date", "Дата"));
        list.add(new EquipmentWorkColumnDef("weldStartTime", "Время начала шва"));
        List<String> selected = template != null && template.getSelectedColumns() != null ? template.getSelectedColumns() : Collections.emptyList();
        if (selected.contains("welderFullName")) list.add(new EquipmentWorkColumnDef("welderFullName", "ФИО сварщика"));
        if (selected.contains("welderTabNumber")) list.add(new EquipmentWorkColumnDef("welderTabNumber", "таб. № сварщика"));
        if (selected.contains("profession")) list.add(new EquipmentWorkColumnDef("welderProfession", "Профессия:"));
        list.add(new EquipmentWorkColumnDef("workMode", "Режим работы оборудования"));
        list.add(new EquipmentWorkColumnDef("currentAmps", "Рабочий ток А"));
        list.add(new EquipmentWorkColumnDef("voltageVolts", "Рабочее напряжение В"));
        if (selected.contains("wireFeedSpeed")) list.add(new EquipmentWorkColumnDef("wireFeedSpeed", "Скорость подачи проволоки, м/мин"));
        list.add(new EquipmentWorkColumnDef("weldDurationSec", "Время шва, с"));
        if (selected.contains("consumption")) list.add(new EquipmentWorkColumnDef("wireConsumptionKg", "Расход проволоки, кг"));
        if (selected.contains("energyConsumed")) list.add(new EquipmentWorkColumnDef("energyConsumedKwh", "Затраченная энергия на шов, кВт*ч"));
        if (selected.contains("gasConsumption")) list.add(new EquipmentWorkColumnDef("gasConsumptionL", "Расход газа, л"));
        return list;
    }

    /** Для отчёта по работе оборудования: записать число только если значение не null и не ноль (иначе ячейка остаётся пустой). */
    private static boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private void setEquipmentWorkCellValue(Cell cell, EquipmentWorkReportDTO item, String key) {
        switch (key) {
            case "index":
                if (item.getIndex() != null) cell.setCellValue(item.getIndex());
                break;
            case "date":
                cell.setCellValue(item.getDate() != null ? item.getDate().toString() : "");
                break;
            case "weldStartTime":
                cell.setCellValue(item.getWeldStartTime() != null ? item.getWeldStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "");
                break;
            case "welderFullName":
                cell.setCellValue(item.getWelderFullName() != null ? item.getWelderFullName() : "");
                break;
            case "welderTabNumber":
                cell.setCellValue(item.getWelderTabNumber() != null ? item.getWelderTabNumber() : "");
                break;
            case "welderProfession":
                cell.setCellValue(item.getWelderProfession() != null ? item.getWelderProfession() : "");
                break;
            case "workMode":
                cell.setCellValue(item.getWorkMode() != null ? item.getWorkMode() : "");
                break;
            case "currentAmps":
                if (!isNullOrZero(item.getCurrentAmps())) cell.setCellValue(item.getCurrentAmps().doubleValue());
                break;
            case "voltageVolts":
                if (!isNullOrZero(item.getVoltageVolts())) cell.setCellValue(item.getVoltageVolts().doubleValue());
                break;
            case "wireFeedSpeed":
                if (!isNullOrZero(item.getWireFeedSpeedMpm())) cell.setCellValue(item.getWireFeedSpeedMpm().doubleValue());
                break;
            case "weldDurationSec":
                if (!isNullOrZero(item.getWeldDurationSec())) cell.setCellValue(item.getWeldDurationSec().doubleValue());
                break;
            case "wireConsumptionKg":
                if (!isNullOrZero(item.getWireConsumptionKg())) cell.setCellValue(item.getWireConsumptionKg().doubleValue());
                break;
            case "energyConsumedKwh":
                if (!isNullOrZero(item.getEnergyConsumedKwh())) cell.setCellValue(item.getEnergyConsumedKwh().doubleValue());
                break;
            case "gasConsumptionL":
                if (!isNullOrZero(item.getGasConsumptionL())) cell.setCellValue(item.getGasConsumptionL().doubleValue());
                break;
            default:
                cell.setCellValue("");
                break;
        }
    }

    private EquipmentWorkReportDTO createZeroEquipmentWorkRow() {
        EquipmentWorkReportDTO row = new EquipmentWorkReportDTO();
        row.setIndex(1);
        row.setDate(java.time.LocalDate.now());
        row.setWeldStartTime(java.time.LocalTime.MIDNIGHT);
        row.setWorkMode("");
        row.setCurrentAmps(BigDecimal.ZERO);
        row.setVoltageVolts(BigDecimal.ZERO);
        row.setWeldDurationSec(BigDecimal.ZERO);
        row.setWireFeedSpeedMpm(BigDecimal.ZERO);
        row.setWireConsumptionKg(BigDecimal.ZERO);
        row.setEnergyConsumedKwh(BigDecimal.ZERO);
        row.setGasConsumptionL(BigDecimal.ZERO);
        row.setCurrentOutOfRange(false);
        row.setWelderFullName("");
        row.setWelderTabNumber("");
        row.setWelderProfession("");
        return row;
    }

    /**
     * Генерирует отчёт "По работе оборудования" в формате XLSX (один блок или один аппарат).
     * Заголовок такой же по структуре, как у отчёта по сварщику: название отчёта, модель/наименование/подразделение, период, диапазон тока, таблица.
     */
    public byte[] generateEquipmentWorkReportNew(
            List<EquipmentWorkReportDTO> data,
            EquipmentWorkReportTemplateDTO template,
            java.time.LocalDate periodStartDate,
            java.time.LocalDate periodEndDate,
            java.time.LocalTime periodStartTime,
            java.time.LocalTime periodEndTime) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе оборудования");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle labelStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            labelStyle.setFont(boldFont);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("Отчет по работе оборудования (швы):");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 1, 2));

            Row r1 = sheet.createRow(rowIdx++);
            r1.createCell(4).setCellValue("Модель оборудования");
            r1.getCell(4).setCellStyle(labelStyle);
            r1.createCell(5).setCellValue(template != null && template.getEquipmentModel() != null ? template.getEquipmentModel() : "");

            Row r2 = sheet.createRow(rowIdx++);
            r2.createCell(4).setCellValue("Наименование оборудования");
            r2.getCell(4).setCellStyle(labelStyle);
            r2.createCell(5).setCellValue(template != null && template.getEquipmentName() != null ? template.getEquipmentName() : "");

            Row r3 = sheet.createRow(rowIdx++);
            r3.createCell(4).setCellValue("Подразделение:");
            r3.getCell(4).setCellStyle(labelStyle);
            r3.createCell(5).setCellValue(template != null && template.getEquipmentDepartment() != null ? template.getEquipmentDepartment() : "");

            rowIdx++;
            Row r5 = sheet.createRow(rowIdx++);
            r5.createCell(4).setCellValue("за период:");
            r5.getCell(4).setCellStyle(labelStyle);
            r5.createCell(5).setCellValue("с");
            r5.getCell(5).setCellStyle(labelStyle);
            r5.createCell(6).setCellValue(periodStartDate != null ? periodStartDate.toString() : "");
            r5.createCell(7).setCellValue("00:00");

            Row r6 = sheet.createRow(rowIdx++);
            r6.createCell(5).setCellValue("по");
            r6.getCell(5).setCellStyle(labelStyle);
            r6.createCell(6).setCellValue(periodEndDate != null ? periodEndDate.toString() : "");
            r6.createCell(7).setCellValue(getPeriodEndTimeDisplay(periodEndDate));

            boolean includeActualRange = template != null && Boolean.TRUE.equals(template.getIncludeActualCurrentRange());
            if (includeActualRange) {
                Row r7 = sheet.createRow(rowIdx++);
                Cell b7 = r7.createCell(1);
                b7.setCellValue("Разрешенный диапазон фактического тока, А");
                b7.setCellStyle(labelStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 3));
                Row r8 = sheet.createRow(rowIdx++);
                r8.createCell(1).setCellValue("min");
                r8.getCell(1).setCellStyle(labelStyle);
                if (template.getActualCurrentMin() != null) r8.createCell(2).setCellValue(template.getActualCurrentMin());
                r8.createCell(5).setCellValue("Минимальный интервал между швами, с");
                r8.getCell(5).setCellStyle(labelStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 5, 7));
                if (template.getMinIntervalBetweenWeldsSec() != null) r8.createCell(8).setCellValue(template.getMinIntervalBetweenWeldsSec());
                Row r9 = sheet.createRow(rowIdx++);
                r9.createCell(1).setCellValue("max");
                r9.getCell(1).setCellStyle(labelStyle);
                if (template.getActualCurrentMax() != null) r9.createCell(2).setCellValue(template.getActualCurrentMax());
                r9.createCell(5).setCellValue("Минимальный учитываемый шов, с");
                r9.getCell(5).setCellStyle(labelStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 5, 6));
                if (template.getMinWeldDurationSec() != null) r9.createCell(8).setCellValue(template.getMinWeldDurationSec());
            }
            rowIdx++;

            List<EquipmentWorkColumnDef> columnDefs = getEquipmentWorkReportColumnDefinitions(template);
            Row headerRow = sheet.createRow(rowIdx++);
            for (int c = 0; c < columnDefs.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(columnDefs.get(c).header);
                cell.setCellStyle(labelStyle);
            }

            CellStyle outOfRangeRowStyle = createOutOfRangeRowStyle(workbook);
            CellStyle normalDataStyle = workbook.createCellStyle();
            // Подсвечивать оранжевым «вне диапазона» только если в шаблоне включены пределы факт.тока
            boolean useOutOfRangeHighlight = template != null && Boolean.TRUE.equals(template.getIncludeActualCurrentRange());
            if (data != null) {
                for (EquipmentWorkReportDTO item : data) {
                    Row row = sheet.createRow(rowIdx++);
                    boolean highlight = useOutOfRangeHighlight && Boolean.TRUE.equals(item.getCurrentOutOfRange());
                    CellStyle rowStyle = highlight ? outOfRangeRowStyle : normalDataStyle;
                    for (int col = 0; col < columnDefs.size(); col++) {
                        Cell cell = row.createCell(col);
                        String key = columnDefs.get(col).key;
                        CellStyle cellStyle = rowStyle;
                        if ("voltageVolts".equals(key) || "weldDurationSec".equals(key) || "currentAmps".equals(key)) {
                            String format = "voltageVolts".equals(key) ? "0.0" : "0";
                            cellStyle = createCellStyleWithNumberFormat(workbook, rowStyle, format);
                        }
                        cell.setCellStyle(cellStyle);
                        setEquipmentWorkCellValue(cell, item, key);
                    }
                }
            }

            int maxColumn = columnDefs.size() - 1;
            for (int i = 0; i <= maxColumn; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, (int) (currentWidth * 1.1));
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Генерирует отчёт "По работе оборудования" для нескольких аппаратов: один лист, блоки «заголовок + таблица» подряд.
     */
    public byte[] generateEquipmentWorkReportMultiSection(
            List<EquipmentWorkReportSectionDTO> sections,
            EquipmentWorkReportTemplateDTO template,
            java.time.LocalDate periodStartDate,
            java.time.LocalDate periodEndDate,
            java.time.LocalTime periodStartTime,
            java.time.LocalTime periodEndTime) throws IOException {
        if (sections == null || sections.isEmpty()) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Отчет по работе оборудования");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе оборудования");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle labelStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            labelStyle.setFont(boldFont);

            List<EquipmentWorkColumnDef> columnDefs = getEquipmentWorkReportColumnDefinitions(template);
            CellStyle outOfRangeRowStyle = createOutOfRangeRowStyle(workbook);
            CellStyle normalDataStyle = workbook.createCellStyle();

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("Отчет по работе оборудования (швы):");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 1, 2));

            boolean periodAndRangeWritten = false;

            for (int s = 0; s < sections.size(); s++) {
                EquipmentWorkReportSectionDTO section = sections.get(s);
                if (s > 0) {
                    for (int i = 0; i < 3; i++) {
                        sheet.createRow(rowIdx++);
                    }
                }

                Row r1 = sheet.createRow(rowIdx++);
                r1.createCell(4).setCellValue("Модель оборудования");
                r1.getCell(4).setCellStyle(labelStyle);
                r1.createCell(5).setCellValue(section.getEquipmentModel() != null ? section.getEquipmentModel() : "");

                Row r2 = sheet.createRow(rowIdx++);
                r2.createCell(4).setCellValue("Наименование оборудования");
                r2.getCell(4).setCellStyle(labelStyle);
                r2.createCell(5).setCellValue(section.getEquipmentName() != null ? section.getEquipmentName() : "");

                Row r3 = sheet.createRow(rowIdx++);
                r3.createCell(4).setCellValue("Подразделение:");
                r3.getCell(4).setCellStyle(labelStyle);
                r3.createCell(5).setCellValue(section.getEquipmentDepartment() != null ? section.getEquipmentDepartment() : "");

                rowIdx++;

                if (!periodAndRangeWritten) {
                    Row r5 = sheet.createRow(rowIdx++);
                    r5.createCell(4).setCellValue("за период:");
                    r5.getCell(4).setCellStyle(labelStyle);
                    r5.createCell(5).setCellValue("с");
                    r5.getCell(5).setCellStyle(labelStyle);
                    r5.createCell(6).setCellValue(periodStartDate != null ? periodStartDate.toString() : "");
                    r5.createCell(7).setCellValue("00:00");
                    Row r6 = sheet.createRow(rowIdx++);
                    r6.createCell(5).setCellValue("по");
                    r6.getCell(5).setCellStyle(labelStyle);
                    r6.createCell(6).setCellValue(periodEndDate != null ? periodEndDate.toString() : "");
                    r6.createCell(7).setCellValue(getPeriodEndTimeDisplay(periodEndDate));
                    boolean includeActualRange = template != null && Boolean.TRUE.equals(template.getIncludeActualCurrentRange());
                    if (includeActualRange) {
                        Row r7 = sheet.createRow(rowIdx++);
                        r7.createCell(1).setCellValue("Разрешенный диапазон фактического тока, А");
                        r7.getCell(1).setCellStyle(labelStyle);
                        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 3));
                        Row r8 = sheet.createRow(rowIdx++);
                        r8.createCell(1).setCellValue("min");
                        r8.getCell(1).setCellStyle(labelStyle);
                        if (template.getActualCurrentMin() != null) r8.createCell(2).setCellValue(template.getActualCurrentMin());
                        Row r9 = sheet.createRow(rowIdx++);
                        r9.createCell(1).setCellValue("max");
                        r9.getCell(1).setCellStyle(labelStyle);
                        if (template.getActualCurrentMax() != null) r9.createCell(2).setCellValue(template.getActualCurrentMax());
                    }
                    // Минимальный интервал между швами и минимальный учитываемый шов — выводим всегда, когда заданы
                    if (template != null && (template.getMinIntervalBetweenWeldsSec() != null || template.getMinWeldDurationSec() != null)) {
                        Row rMinInterval = sheet.createRow(rowIdx++);
                        rMinInterval.createCell(4).setCellValue("Минимальный интервал между швами, с");
                        rMinInterval.getCell(4).setCellStyle(labelStyle);
                        if (template.getMinIntervalBetweenWeldsSec() != null) rMinInterval.createCell(5).setCellValue(template.getMinIntervalBetweenWeldsSec());
                        Row rMinWeld = sheet.createRow(rowIdx++);
                        rMinWeld.createCell(4).setCellValue("Минимальный учитываемый шов, с");
                        rMinWeld.getCell(4).setCellStyle(labelStyle);
                        if (template.getMinWeldDurationSec() != null) rMinWeld.createCell(5).setCellValue(template.getMinWeldDurationSec());
                    }
                    periodAndRangeWritten = true;
                    rowIdx++;
                }

                Row headerRow = sheet.createRow(rowIdx++);
                for (int c = 0; c < columnDefs.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(columnDefs.get(c).header);
                    cell.setCellStyle(labelStyle);
                }

                List<EquipmentWorkReportDTO> rows = section.getRows();
                if (rows == null) rows = Collections.emptyList();
                if (rows.isEmpty()) rows = Collections.singletonList(createZeroEquipmentWorkRow());
                // Подсвечивать оранжевым «вне диапазона» только если в шаблоне включены пределы факт.тока
                boolean useOutOfRangeHighlight = template != null && Boolean.TRUE.equals(template.getIncludeActualCurrentRange());
                for (EquipmentWorkReportDTO item : rows) {
                    Row row = sheet.createRow(rowIdx++);
                    boolean highlight = useOutOfRangeHighlight && Boolean.TRUE.equals(item.getCurrentOutOfRange());
                    CellStyle rowStyle = highlight ? outOfRangeRowStyle : normalDataStyle;
                    for (int col = 0; col < columnDefs.size(); col++) {
                        Cell cell = row.createCell(col);
                        String key = columnDefs.get(col).key;
                        CellStyle cellStyle = rowStyle;
                        if ("voltageVolts".equals(key) || "weldDurationSec".equals(key) || "currentAmps".equals(key)) {
                            String format = "voltageVolts".equals(key) ? "0.0" : "0";
                            cellStyle = createCellStyleWithNumberFormat(workbook, rowStyle, format);
                        }
                        cell.setCellStyle(cellStyle);
                        setEquipmentWorkCellValue(cell, item, key);
                    }
                }
            }

            int maxColumn = columnDefs.size() - 1;
            for (int i = 0; i <= maxColumn; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, (int) (currentWidth * 1.1));
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private CellStyle createOutOfRangeRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /** Создаёт стиль на основе базового с заданным числовым форматом (для отчёта по работе оборудования). */
    private CellStyle createCellStyleWithNumberFormat(Workbook workbook, CellStyle baseStyle, String dataFormat) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        DataFormat df = workbook.createDataFormat();
        style.setDataFormat(df.getFormat(dataFormat));
        return style;
    }

    /**
     * Добавляет информацию о периоде и пределах тока
     */
    private int addReportHeaderInfo(Sheet sheet, int startRow,
                                    WireConsumptionReportTemplateDTO template,
                                    LocalDate periodStartDate,
                                    LocalDate periodEndDate,
                                    LocalTime periodStartTime,
                                    LocalTime periodEndTime,
                                    CellStyle style) {
        int row = startRow;

        // Период - первая строка: "с:" дата время [Дни недели] (время начала периода — всегда 00:00)
        Row periodRow1 = sheet.createRow(row++);
        periodRow1.createCell(0).setCellValue("с:");
        int colIndex = 1;
        if (periodStartDate != null) {
            periodRow1.createCell(colIndex++).setCellValue(periodStartDate.toString());
        }
        periodRow1.createCell(colIndex++).setCellValue("00:00");

        // Добавляем дни недели, если они выбраны
        if (template.getSelectedDays() != null && !template.getSelectedDays().isEmpty()) {
            System.out.println("[REPORT-SERVICE] 📅 Добавляем дни недели в отчет: " + template.getSelectedDays());
            periodRow1.createCell(colIndex++).setCellValue("Дни недели");
            String daysString = String.join(", ", template.getSelectedDays());
            periodRow1.createCell(colIndex++).setCellValue(daysString);
        } else {
            System.out.println("[REPORT-SERVICE] ⚠️ selectedDays пуст или null: " +
                    (template.getSelectedDays() == null ? "null" : "empty"));
        }

        // Период - вторая строка: "по:" дата время (если periodEndTime не задано — 23:59 или текущее время при отчёте «на сегодня»)
        Row periodRow2 = sheet.createRow(row++);
        periodRow2.createCell(0).setCellValue("по:");
        if (periodEndDate != null) {
            periodRow2.createCell(1).setCellValue(periodEndDate.toString());
        }
        periodRow2.createCell(2).setCellValue(periodEndTime != null ? formatTime(periodEndTime) : getPeriodEndTimeDisplay(periodEndDate));

        // Пределы устанавливаемого и фактического тока - новый формат
        // Проверяем, нужно ли показывать хотя бы один из диапазонов
        boolean showSetCurrent = template.getSelectedColumns() != null && template.getSelectedColumns().contains("workOutsideSetCurrent");
        boolean showActualCurrent = template.getSelectedColumns() != null && template.getSelectedColumns().contains("workOutsideActualCurrent");

        if (showSetCurrent || showActualCurrent) {
            // Строка 1: Заголовки диапазонов
            Row headerRow = sheet.createRow(row++);
            if (showSetCurrent) {
                headerRow.createCell(0).setCellValue("Разрешенный диапазон устанавливаемого тока, А");
            }
            // Ячейки 1-2 пустые (пропуск между диапазонами - две ячейки)
            if (showActualCurrent) {
                headerRow.createCell(3).setCellValue("Разрешенный диапазон фактического тока, А");
            }

            // Строка 2: min значения
            Row minRow = sheet.createRow(row++);
            if (showSetCurrent) {
                minRow.createCell(0).setCellValue("min");
                if (template.getSetCurrentMin() != null) {
                    minRow.createCell(1).setCellValue(template.getSetCurrentMin());
                }
            }
            // Ячейки 2-3 пустые (пропуск между диапазонами - две ячейки)
            if (showActualCurrent) {
                minRow.createCell(3).setCellValue("min");
                if (template.getActualCurrentMin() != null) {
                    minRow.createCell(4).setCellValue(template.getActualCurrentMin());
                }
            }

            // Строка 3: max значения
            Row maxRow = sheet.createRow(row++);
            if (showSetCurrent) {
                maxRow.createCell(0).setCellValue("max");
                if (template.getSetCurrentMax() != null) {
                    maxRow.createCell(1).setCellValue(template.getSetCurrentMax());
                }
            }
            // Ячейки 2-3 пустые (пропуск между диапазонами - две ячейки)
            if (showActualCurrent) {
                maxRow.createCell(3).setCellValue("max");
                if (template.getActualCurrentMax() != null) {
                    maxRow.createCell(4).setCellValue(template.getActualCurrentMax());
                }
            }
        }

        row++; // Пустая строка

        return row;
    }

    /**
     * Структура колонки отчета
     */
    private static class ColumnDefinition {
        String key;
        String header;
        boolean alwaysInclude;

        ColumnDefinition(String key, String header, boolean alwaysInclude) {
            this.key = key;
            this.header = header;
            this.alwaysInclude = alwaysInclude;
        }
    }

    /**
     * Определяет порядок и названия колонок отчета
     */
    private List<ColumnDefinition> getColumnDefinitions(WireConsumptionReportTemplateDTO template) {
        List<ColumnDefinition> columns = new ArrayList<>();
        List<String> selectedColumns = template.getSelectedColumns() != null
                ? template.getSelectedColumns() : new ArrayList<>();

        // Всегда включаемые колонки
        columns.add(new ColumnDefinition("serialNumber", "№ п/п", true));
        columns.add(new ColumnDefinition("welder", "Сварщик", true));

        // Условно включаемые колонки (в порядке отображения)
        if (selectedColumns.contains("tableNumber")) {
            columns.add(new ColumnDefinition("tableNumber", "Таб. №", false));
        }
        if (selectedColumns.contains("profession")) {
            columns.add(new ColumnDefinition("profession", "Профессия", false));
        }
        if (selectedColumns.contains("department")) {
            columns.add(new ColumnDefinition("department", "Подразделение", false));
        }
        if (selectedColumns.contains("equipmentName")) {
            columns.add(new ColumnDefinition("equipmentName", "Наименование оборудования", false));
        }
        if (selectedColumns.contains("timeOnline")) {
            columns.add(new ColumnDefinition("timeOnline", "Время в сети", false));
        }
        if (selectedColumns.contains("equipmentModel")) {
            columns.add(new ColumnDefinition("equipmentModel", "Модель оборудования", false));
        }
        if (selectedColumns.contains("arcBurningTime")) {
            columns.add(new ColumnDefinition("arcBurningTime", "Время горения дуги", false));
        }
        if (selectedColumns.contains("efficiency")) {
            columns.add(new ColumnDefinition("efficiency", "Эффективность использования оборудования, %", false));
        }
        if (selectedColumns.contains("workOutsideSetCurrent")) {
            columns.add(new ColumnDefinition("workOutsideSetCurrent", "Время работы вне диапазона устанавливаемого тока", false));
        }
        if (selectedColumns.contains("workOutsideActualCurrent")) {
            columns.add(new ColumnDefinition("workOutsideActualCurrent", "Время работы вне диапазона фактического тока", false));
        }
        if (selectedColumns.contains("energyConsumed")) {
            columns.add(new ColumnDefinition("energyConsumed", "Затраченная энергия, кВт*ч", false));
        }

        // Всегда включаемые колонки в конце
        columns.add(new ColumnDefinition("wire", "Проволока", true));
        columns.add(new ColumnDefinition("wireConsumption", "Расход, кг", true));

        return columns;
    }

    /**
     * Добавляет заголовки таблицы на основе выбранных колонок
     */
    private int addTableHeaders(Sheet sheet, int startRow,
                                WireConsumptionReportTemplateDTO template,
                                CellStyle lightBlueStyle, CellStyle lightGreenStyle) {
        int row = startRow;

        List<ColumnDefinition> columns = getColumnDefinitions(template);

        // Создаем одну строку заголовков
        Row headerRow = sheet.createRow(row++);

        int colIndex = 0;
        for (ColumnDefinition col : columns) {
            Cell cell = headerRow.createCell(colIndex++);
            cell.setCellValue(col.header);
            cell.setCellStyle(lightGreenStyle);
        }

        return row;
    }

    /**
     * Добавляет данные таблицы на основе выбранных колонок
     */
    private int addTableData(Sheet sheet, int startRow,
                             List<WireConsumptionReportDTO> data,
                             WireConsumptionReportTemplateDTO template,
                             CellStyle dataStyle, CellStyle summaryRowStyle) {
        int row = startRow;

        List<ColumnDefinition> columns = getColumnDefinitions(template);
        int serialNumber = 1;

        for (WireConsumptionReportDTO item : data) {
            Row dataRow = sheet.createRow(row++);
            CellStyle rowStyle = item.getIsSummaryRow() != null && item.getIsSummaryRow()
                    ? summaryRowStyle : dataStyle;

            int colIndex = 0;
            for (ColumnDefinition col : columns) {
                Cell cell = dataRow.createCell(colIndex++);
                cell.setCellStyle(rowStyle);

                switch (col.key) {
                    case "serialNumber":
                        if (item.getIsSummaryRow() == null || !item.getIsSummaryRow()) {
                            cell.setCellValue(serialNumber++);
                        } else {
                            cell.setCellValue(""); // Пусто для суммарной строки
                        }
                        break;
                    case "welder":
                        cell.setCellValue(item.getWelderName() != null ? item.getWelderName() : "");
                        break;
                    case "tableNumber":
                        cell.setCellValue(item.getTabNumber() != null ? item.getTabNumber() : "");
                        break;
                    case "profession":
                        cell.setCellValue(item.getProfession() != null ? item.getProfession() : "");
                        break;
                    case "department":
                        cell.setCellValue(item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "");
                        break;
                    case "equipmentName":
                        cell.setCellValue(item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "");
                        break;
                    case "timeOnline":
                        cell.setCellValue(formatDuration(item.getTimeInNetwork()));
                        break;
                    case "equipmentModel":
                        cell.setCellValue(item.getEquipmentModel() != null ? item.getEquipmentModel() : "");
                        break;
                    case "arcBurningTime":
                        cell.setCellValue(formatDuration(item.getArcBurningTime()));
                        break;
                    case "efficiency":
                        if (item.getEquipmentEfficiency() != null) {
                            cell.setCellValue(item.getEquipmentEfficiency().doubleValue());
                        } else {
                            cell.setCellValue(0.0);
                        }
                        break;
                    case "workOutsideSetCurrent":
                        cell.setCellValue(formatDuration(item.getTimeOutsideSetCurrentRange()));
                        break;
                    case "workOutsideActualCurrent":
                        cell.setCellValue(formatDuration(item.getTimeOutsideActualCurrentRange()));
                        break;
                    case "energyConsumed":
                        if (item.getEnergyConsumed() != null) {
                            cell.setCellValue(item.getEnergyConsumed().doubleValue());
                        } else {
                            cell.setCellValue(0.0);
                        }
                        break;
                    case "wire":
                        cell.setCellValue(item.getWire() != null ? item.getWire() : "");
                        break;
                    case "wireConsumption":
                        if (item.getWireConsumption() != null) {
                            cell.setCellValue(item.getWireConsumption().doubleValue());
                        } else {
                            cell.setCellValue(0.0);
                        }
                        break;
                    default:
                        cell.setCellValue("");
                }
            }
        }

        return row;
    }

    /**
     * Форматирует Duration в строку формата "д:ч:мм:сс"
     */
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "";
        }
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds);
    }

    /**
     * Форматирует LocalTime в строку формата "HH:mm:ss"
     */
    private String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
    }

    /** Время окончания периода для отчёта: 23:59 или текущее время, если дата окончания — сегодня и 23:59 ещё не наступило. */
    private String getPeriodEndTimeDisplay(LocalDate periodEndDate) {
        if (periodEndDate == null) return "";
        LocalTime now = LocalTime.now();
        if (periodEndDate.equals(LocalDate.now()) && now.isBefore(LocalTime.of(23, 59))) {
            return formatTime(now);
        }
        return formatTime(LocalTime.of(23, 59));
    }

    /**
     * Создает стиль для светло-голубых ячеек
     */
    private CellStyle createLightBlueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        style.setFont(font);
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return style;
    }

    /**
     * Создает стиль для светло-зеленых ячеек
     */
    private CellStyle createLightGreenStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        style.setFont(font);
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return style;
    }

    /**
     * Создает стиль для светло-голубых ячеек (для сортировки)
     */
    private CellStyle createLightCyanStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        style.setFont(font);
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.AQUA.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return style;
    }

    /**
     * Создает стиль для суммарных строк
     */
    private CellStyle createSummaryRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return style;
    }
} 