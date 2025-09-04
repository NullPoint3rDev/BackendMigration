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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportHistoryService reportHistoryService;

    @Autowired
    private WeldingMachineService weldingMachineService;

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
        // Добавляем отладочную информацию
        System.out.println("=== DEBUG: generateEquipmentReport ===");
        System.out.println("Format: " + request.getFormat());
        System.out.println("Period: " + request.getPeriod());
        System.out.println("WeldingMachineId: " + request.getWeldingMachineId());
        System.out.println("WeldingMachineId type: " + (request.getWeldingMachineId() != null ? request.getWeldingMachineId().getClass().getSimpleName() : "null"));
        System.out.println("=====================================");
        
        // Получаем название аппарата по ID
        String machineName = "Неизвестный аппарат";
        if (request.getWeldingMachineId() != null) {
            try {
                var machine = weldingMachineService.getWeldingMachineById(request.getWeldingMachineId());
                if (machine.isPresent()) {
                    machineName = machine.get().getName();
                    System.out.println("Найден аппарат: " + machineName);
                } else {
                    machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (не найден)";
                    System.out.println("Аппарат с ID " + request.getWeldingMachineId() + " не найден в базе");
                }
            } catch (Exception e) {
                machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (ошибка загрузки)";
                System.err.println("Ошибка при получении названия аппарата: " + e.getMessage());
            }
        }
        
        // Генерируем отчет по работе оборудования с тестовыми данными в зависимости от периода
        byte[] reportData = generateEquipmentReportWithData(request.getFormat(), request.getWeldingMachineId(), machineName, request.getPeriod());
        
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
            
            System.out.println("Добавляем отчет в историю: " + history.getReportName() + " (" + history.getFormat() + ")");
            reportHistoryService.addReportToHistory(history);
            System.out.println("Отчет успешно добавлен в историю");
            
        } catch (Exception e) {
            System.err.println("Ошибка при добавлении отчета в историю: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reportData;
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
        // Добавляем отладочную информацию
        System.out.println("=== DEBUG: generateMaterialsReport ===");
        System.out.println("Format: " + request.getFormat());
        System.out.println("Period: " + request.getPeriod());
        System.out.println("WeldingMachineId: " + request.getWeldingMachineId());
        System.out.println("WeldingMachineId type: " + (request.getWeldingMachineId() != null ? request.getWeldingMachineId().getClass().getSimpleName() : "null"));
        System.out.println("=====================================");
        
        // Получаем название аппарата по ID
        String machineName = "Неизвестный аппарат";
        if (request.getWeldingMachineId() != null) {
            try {
                var machine = weldingMachineService.getWeldingMachineById(request.getWeldingMachineId());
                if (machine.isPresent()) {
                    machineName = machine.get().getName();
                    System.out.println("Найден аппарат: " + machineName);
                } else {
                    machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (не найден)";
                    System.out.println("Аппарат с ID " + request.getWeldingMachineId() + " не найден в базе");
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
        // Добавляем отладочную информацию
        System.out.println("=== DEBUG: generateErrorsReport ===");
        System.out.println("Format: " + request.getFormat());
        System.out.println("Period: " + request.getPeriod());
        System.out.println("WeldingMachineId: " + request.getWeldingMachineId());
        System.out.println("WeldingMachineId type: " + (request.getWeldingMachineId() != null ? request.getWeldingMachineId().getClass().getSimpleName() : "null"));
        System.out.println("=====================================");
        
        // Получаем название аппарата по ID
        String machineName = "Неизвестный аппарат";
        if (request.getWeldingMachineId() != null) {
            try {
                var machine = weldingMachineService.getWeldingMachineById(request.getWeldingMachineId());
                if (machine.isPresent()) {
                    machineName = machine.get().getName();
                    System.out.println("Найден аппарат: " + machineName);
                } else {
                    machineName = "Аппарат ID: " + request.getWeldingMachineId() + " (не найден)";
                    System.out.println("Аппарат с ID " + request.getWeldingMachineId() + " не найден в базе");
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
            
            // Добавляем информацию о выбранном аппарате
            Row titleRow = sheet.createRow(0);
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
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 8));
            
            // Создаем заголовки (начиная с 2-й строки)
            Row headerRow = sheet.createRow(2);
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
            int rowNum = 3;
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

    private byte[] generateEquipmentPdfWithData(Integer weldingMachineId, String machineName, String period) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
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
            
            // Добавляем информацию о выбранном аппарате
            Row titleRow = sheet.createRow(0);
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
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));
            
            // Создаем заголовки (начиная с 2-й строки) - 8 столбцов согласно скриншоту
            Row headerRow = sheet.createRow(2);
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
            int rowNum = 3;
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
                "ID шва", "Тип шва", "Сварщик", "Оборудование", "Материал", "Длина (мм)",
                "Толщина (мм)", "Ток (А)", "Напряжение (В)", "Скорость (мм/мин)", "Качество (%)", "Дата"
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
            
            // Добавляем информацию о выбранном аппарате
            Row titleRow = sheet.createRow(0);
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
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));
            
            // Создаем заголовки (начиная с 2-й строки)
            Row headerRow = sheet.createRow(2);
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
            int rowNum = 3;
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
                row.createCell(1).setCellValue(item.getWeldingMachineSerialNumber() != null ? item.getWeldingMachineSerialNumber() : "");
                row.createCell(2).setCellValue(item.getWelderName() != null ? item.getWelderName() : "");
                row.createCell(3).setCellValue(item.getDate() != null ? item.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "");
                row.createCell(4).setCellValue(item.getWireConsumption() != null ? item.getWireConsumption().doubleValue() : 0.0);
                row.createCell(5).setCellValue(item.getWireFeedRate() != null ? item.getWireFeedRate().doubleValue() : 0.0);
                row.createCell(6).setCellValue(item.getWeldingTime() != null ? item.getWeldingTime().doubleValue() : 0.0);
                row.createCell(7).setCellValue(item.getCurrent() != null ? item.getCurrent().doubleValue() : 0.0);
                row.createCell(8).setCellValue(item.getVoltage() != null ? item.getVoltage().doubleValue() : 0.0);
                row.createCell(9).setCellValue(item.getWeldingMode() != null ? item.getWeldingMode() : "");
                row.createCell(10).setCellValue(item.getWeldingType() != null ? item.getWeldingType() : "");
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
            document.add(new Paragraph("Серийный номер: " + (item.getWeldingMachineSerialNumber() != null ? item.getWeldingMachineSerialNumber() : "")));
            document.add(new Paragraph("Сварщик: " + (item.getWelderName() != null ? item.getWelderName() : "")));
            document.add(new Paragraph("Дата: " + (item.getDate() != null ? item.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "")));
            document.add(new Paragraph("Расход проволоки (кг): " + (item.getWireConsumption() != null ? item.getWireConsumption().toString() : "0")));
            document.add(new Paragraph("Скорость подачи (м/мин): " + (item.getWireFeedRate() != null ? item.getWireFeedRate().toString() : "0")));
            document.add(new Paragraph("Время сварки (мин): " + (item.getWeldingTime() != null ? item.getWeldingTime().toString() : "0")));
            document.add(new Paragraph("Ток (А): " + (item.getCurrent() != null ? item.getCurrent().toString() : "0")));
            document.add(new Paragraph("Напряжение (В): " + (item.getVoltage() != null ? item.getVoltage().toString() : "0")));
            document.add(new Paragraph("Режим сварки: " + (item.getWeldingMode() != null ? item.getWeldingMode() : "")));
            document.add(new Paragraph("Тип сварки: " + (item.getWeldingType() != null ? item.getWeldingType() : "")));
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
} 