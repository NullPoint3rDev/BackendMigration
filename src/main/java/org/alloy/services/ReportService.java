package org.alloy.services;

import org.alloy.models.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

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
        // TODO: Реализовать генерацию отчета по работе оборудования
        // Пока что возвращаем заглушку
        return generatePlaceholderReport("Отчет по работе оборудования", request.getFormat());
    }

    public byte[] generateWeldersReport(ReportRequestDTO request) throws IOException {
        // TODO: Реализовать генерацию отчета по работе сварщиков
        // Пока что возвращаем заглушку
        return generatePlaceholderReport("Отчет по работе сварщиков", request.getFormat());
    }

    public byte[] generateMaterialsReport(ReportRequestDTO request) throws IOException {
        // TODO: Реализовать генерацию отчета по расходу материалов
        // Пока что возвращаем заглушку
        return generatePlaceholderReport("Отчет по расходу материалов", request.getFormat());
    }

    public byte[] generateWeldsReport(ReportRequestDTO request) throws IOException {
        // TODO: Реализовать генерацию отчета по сварочным швам
        // Пока что возвращаем заглушку
        return generatePlaceholderReport("Отчет по сварочным швам", request.getFormat());
    }

    public byte[] generateNotificationsReport(ReportRequestDTO request) throws IOException {
        // TODO: Реализовать генерацию отчета по уведомлениям
        // Пока что возвращаем заглушку
        return generatePlaceholderReport("Отчет по уведомлениям", request.getFormat());
    }

    public byte[] generateErrorsReport(ReportRequestDTO request) throws IOException {
        // TODO: Реализовать генерацию отчета по ошибкам
        // Пока что возвращаем заглушку
        return generatePlaceholderReport("Отчет по ошибкам сварочного оборудования", request.getFormat());
    }

    public byte[] generateViolationsReport(ReportRequestDTO request) throws IOException {
        // TODO: Реализовать генерацию отчета по нарушениям
        // Пока что возвращаем заглушку
        return generatePlaceholderReport("Перечень швов, выполненных с нарушением", request.getFormat());
    }

    public byte[] generateTasksReport(ReportRequestDTO request) throws IOException {
        // TODO: Реализовать генерацию отчета по заданиям
        // Пока что возвращаем заглушку
        return generatePlaceholderReport("Отчет о выполнении сварочного задания", request.getFormat());
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