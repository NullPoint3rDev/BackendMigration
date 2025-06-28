package org.alloy.services;

import org.alloy.models.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
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
        title.setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Таблица
        Table table = new Table(12);
        table.setWidth(500);

        // Заголовки таблицы
        String[] headers = {
            "Аппарат", "Серийный номер", "Сварщик", "Дата", "Расход проволоки (кг)",
            "Скорость подачи (м/мин)", "Время сварки (мин)", "Ток (А)", "Напряжение (В)",
            "Режим сварки", "Тип сварки", "Подразделение"
        };

        for (String header : headers) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)).setBold());
        }

        // Данные
        for (WireConsumptionReportDTO item : data) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingMachineSerialNumber() != null ? item.getWeldingMachineSerialNumber() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWelderName() != null ? item.getWelderName() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getDate() != null ? item.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWireConsumption() != null ? item.getWireConsumption().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWireFeedRate() != null ? item.getWireFeedRate().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingTime() != null ? item.getWeldingTime().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getCurrent() != null ? item.getCurrent().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getVoltage() != null ? item.getVoltage().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingMode() != null ? item.getWeldingMode() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingType() != null ? item.getWeldingType() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "")));
        }

        document.add(table);
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
        title.setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Таблица
        Table table = new Table(11);
        table.setWidth(500);

        // Заголовки таблицы
        String[] headers = {
            "Сварщик", "Email", "Дата", "Общий расход проволоки (кг)", "Общее время сварки (мин)",
            "Количество сессий", "Средний ток (А)", "Среднее напряжение (В)", 
            "Средняя скорость подачи (м/мин)", "Подразделение", "Аппарат"
        };

        for (String header : headers) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)).setBold());
        }

        // Данные
        for (WelderReportDTO item : data) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWelderName() != null ? item.getWelderName() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWelderEmail() != null ? item.getWelderEmail() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getDate() != null ? item.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getTotalWireConsumption() != null ? item.getTotalWireConsumption().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getTotalWeldingTime() != null ? item.getTotalWeldingTime().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getTotalWeldingSessions() != null ? item.getTotalWeldingSessions().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getAverageCurrent() != null ? item.getAverageCurrent().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getAverageVoltage() != null ? item.getAverageVoltage().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getAverageWireFeedRate() != null ? item.getAverageWireFeedRate().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "")));
        }

        document.add(table);
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
        title.setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Таблица
        Table table = new Table(14);
        table.setWidth(500);

        // Заголовки таблицы
        String[] headers = {
            "Аппарат", "Серийный номер", "Сварщик", "Время начала", "Время окончания",
            "Время сварки (мин)", "Ток (А)", "Напряжение (В)", "Режим сварки", "Тип сварки",
            "Расход проволоки (кг)", "Скорость подачи (м/мин)", "Подразделение", "Примечания"
        };

        for (String header : headers) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)).setBold());
        }

        // Данные
        for (WorkReportDTO item : data) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingMachineName() != null ? item.getWeldingMachineName() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingMachineSerialNumber() != null ? item.getWeldingMachineSerialNumber() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWelderName() != null ? item.getWelderName() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getStartTime() != null ? item.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getEndTime() != null ? item.getEndTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingTime() != null ? item.getWeldingTime().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getCurrent() != null ? item.getCurrent().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getVoltage() != null ? item.getVoltage().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingMode() != null ? item.getWeldingMode() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWeldingType() != null ? item.getWeldingType() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWireConsumption() != null ? item.getWireConsumption().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getWireFeedRate() != null ? item.getWireFeedRate().toString() : "0")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getOrganizationUnitName() != null ? item.getOrganizationUnitName() : "")));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(item.getNotes() != null ? item.getNotes() : "")));
        }

        document.add(table);
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