package org.alloy.services;

import org.alloy.models.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;


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
        // Генерируем отчет по работе оборудования с тестовыми данными
        return generateEquipmentReportWithData(request.getFormat());
    }

    public byte[] generateWeldersReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по работе сварщиков с тестовыми данными
        return generateWeldersReportWithData(request.getFormat());
    }

    public byte[] generateMaterialsReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по расходу материалов с тестовыми данными
        return generateMaterialsReportWithData(request.getFormat());
    }

    public byte[] generateWeldsReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по сварочным швам с тестовыми данными
        return generateWeldsReportWithData(request.getFormat());
    }



    public byte[] generateErrorsReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по ошибкам с тестовыми данными
        return generateErrorsReportWithData(request.getFormat());
    }

    public byte[] generateViolationsReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по нарушениям с тестовыми данными
        return generateViolationsReportWithData(request.getFormat());
    }

    public byte[] generateTasksReport(ReportRequestDTO request) throws IOException {
        // Генерируем отчет по заданиям с тестовыми данными
        return generateTasksReportWithData(request.getFormat());
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
    
    private byte[] generateEquipmentReportWithData(String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateEquipmentExcelWithData();
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateEquipmentPdfWithData();
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateEquipmentCsvWithData();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateEquipmentExcelWithData() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по работе оборудования");
            
            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "ID оборудования", "Название", "Серийный номер", "Подразделение", 
                "Время работы (часы)", "Расход проволоки (кг)", "Количество сварщиков",
                "Средний ток (А)", "Среднее напряжение (В)", "Статус", "Последнее обслуживание"
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
            String[] equipmentNames = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250", "Аппарат плазменной резки", "Сварочный робот"};
            String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок", "Склад"};
            String[] statuses = {"Работает", "Техобслуживание", "Ремонт", "Резерв", "Неисправен"};
            
            for (int i = 0; i < 15; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(1000 + i);
                row.createCell(1).setCellValue(equipmentNames[i % equipmentNames.length]);
                row.createCell(2).setCellValue("SN-" + (2023000 + i));
                row.createCell(3).setCellValue(departments[i % departments.length]);
                row.createCell(4).setCellValue(120 + (int)(Math.random() * 480)); // 120-600 часов
                row.createCell(5).setCellValue(15.5 + (Math.random() * 84.5)); // 15.5-100 кг
                row.createCell(6).setCellValue(1 + (int)(Math.random() * 4)); // 1-4 сварщика
                row.createCell(7).setCellValue(180 + (int)(Math.random() * 120)); // 180-300 А
                row.createCell(8).setCellValue(20 + (int)(Math.random() * 10)); // 20-30 В
                row.createCell(9).setCellValue(statuses[i % statuses.length]);
                row.createCell(10).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateEquipmentPdfWithData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Заголовок отчета
        Paragraph title = new Paragraph("Отчет по работе сварочного оборудования");
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));
        
        // Данные
        String[] equipmentNames = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250", "Аппарат плазменной резки", "Сварочный робот"};
        String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок", "Склад"};
        String[] statuses = {"Работает", "Техобслуживание", "Ремонт", "Резерв", "Неисправен"};
        
        for (int i = 0; i < 12; i++) {
            document.add(new Paragraph("Оборудование " + (i + 1) + ":"));
            document.add(new Paragraph("  ID: " + (1000 + i)));
            document.add(new Paragraph("  Название: " + equipmentNames[i % equipmentNames.length]));
            document.add(new Paragraph("  Серийный номер: SN-" + (2023000 + i)));
            document.add(new Paragraph("  Подразделение: " + departments[i % departments.length]));
            document.add(new Paragraph("  Время работы: " + (120 + (int)(Math.random() * 480)) + " часов"));
            document.add(new Paragraph("  Расход проволоки: " + String.format("%.1f", 15.5 + (Math.random() * 84.5)) + " кг"));
            document.add(new Paragraph("  Количество сварщиков: " + (1 + (int)(Math.random() * 4))));
            document.add(new Paragraph("  Средний ток: " + (180 + (int)(Math.random() * 120)) + " А"));
            document.add(new Paragraph("  Среднее напряжение: " + (20 + (int)(Math.random() * 10)) + " В"));
            document.add(new Paragraph("  Статус: " + statuses[i % statuses.length]));
            document.add(new Paragraph("  Последнее обслуживание: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateEquipmentCsvWithData() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID оборудования,Название,Серийный номер,Подразделение,Время работы (часы),Расход проволоки (кг),Количество сварщиков,Средний ток (А),Среднее напряжение (В),Статус,Последнее обслуживание\n");
        
        String[] equipmentNames = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250", "Аппарат плазменной резки", "Сварочный робот"};
        String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок", "Склад"};
        String[] statuses = {"Работает", "Техобслуживание", "Ремонт", "Резерв", "Неисправен"};
        
        for (int i = 0; i < 12; i++) {
            csv.append(1000 + i).append(",");
            csv.append(equipmentNames[i % equipmentNames.length]).append(",");
            csv.append("SN-").append(2023000 + i).append(",");
            csv.append(departments[i % departments.length]).append(",");
            csv.append(120 + (int)(Math.random() * 480)).append(",");
            csv.append(String.format("%.1f", 15.5 + (Math.random() * 84.5))).append(",");
            csv.append(1 + (int)(Math.random() * 4)).append(",");
            csv.append(180 + (int)(Math.random() * 120)).append(",");
            csv.append(20 + (int)(Math.random() * 10)).append(",");
            csv.append(statuses[i % statuses.length]).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28)));
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
            document.add(new Paragraph("  ID: " + (2000 + i)));
            document.add(new Paragraph("  ФИО: " + names[i % names.length]));
            document.add(new Paragraph("  Подразделение: " + departments[i % departments.length]));
            document.add(new Paragraph("  Квалификация: " + qualifications[i % qualifications.length]));
            document.add(new Paragraph("  Время работы: " + (160 + (int)(Math.random() * 200)) + " часов"));
            document.add(new Paragraph("  Выполнено швов: " + (50 + (int)(Math.random() * 150))));
            document.add(new Paragraph("  Расход проволоки: " + String.format("%.1f", 8.5 + (Math.random() * 41.5)) + " кг"));
            document.add(new Paragraph("  Средний ток: " + (180 + (int)(Math.random() * 120)) + " А"));
            document.add(new Paragraph("  Среднее напряжение: " + (20 + (int)(Math.random() * 10)) + " В"));
            document.add(new Paragraph("  Качество работы: " + (85 + (int)(Math.random() * 15)) + "%"));
            document.add(new Paragraph("  Дата последней работы: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
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

    private byte[] generateMaterialsReportWithData(String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateMaterialsExcelWithData();
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateMaterialsPdfWithData();
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateMaterialsCsvWithData();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateMaterialsExcelWithData() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по расходу материалов");
            
            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "ID материала", "Название", "Тип", "Подразделение", "Расход (кг)",
                "Остаток (кг)", "Стоимость за кг", "Дата поставки", "Поставщик", "Статус"
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
            String[] materialNames = {"Проволока ER70S-6", "Электроды УОНИ-13/55", "Флюс АН-348А", "Газ CO2", "Газ Ar"};
            String[] types = {"Проволока", "Электроды", "Флюс", "Газ", "Газ"};
            String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок"};
            String[] suppliers = {"ООО МеталлСнаб", "ИП Иванов", "ООО ГазТрейд", "ООО ЭлектродСтрой"};
            String[] statuses = {"В наличии", "Заканчивается", "Под заказ", "Нет в наличии"};
            
            for (int i = 0; i < 25; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(3000 + i);
                row.createCell(1).setCellValue(materialNames[i % materialNames.length]);
                row.createCell(2).setCellValue(types[i % types.length]);
                row.createCell(3).setCellValue(departments[i % departments.length]);
                row.createCell(4).setCellValue(25.5 + (Math.random() * 74.5)); // 25.5-100 кг
                row.createCell(5).setCellValue(5.0 + (Math.random() * 45.0)); // 5.0-50 кг
                row.createCell(6).setCellValue(120.0 + (Math.random() * 180.0)); // 120-300 руб/кг
                row.createCell(7).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
                row.createCell(8).setCellValue(suppliers[i % suppliers.length]);
                row.createCell(9).setCellValue(statuses[i % statuses.length]);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateMaterialsPdfWithData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Заголовок отчета
        Paragraph title = new Paragraph("Отчет по расходу материалов");
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));
        
        // Данные
        String[] materialNames = {"Проволока ER70S-6", "Электроды УОНИ-13/55", "Флюс АН-348А", "Газ CO2", "Газ Ar"};
        String[] types = {"Проволока", "Электроды", "Флюс", "Газ", "Газ"};
        String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок"};
        String[] suppliers = {"ООО МеталлСнаб", "ИП Иванов", "ООО ГазТрейд", "ООО ЭлектродСтрой"};
        String[] statuses = {"В наличии", "Заканчивается", "Под заказ", "Нет в наличии"};
        
        for (int i = 0; i < 20; i++) {
            document.add(new Paragraph("Материал " + (i + 1) + ":"));
            document.add(new Paragraph("  ID: " + (3000 + i)));
            document.add(new Paragraph("  Название: " + materialNames[i % materialNames.length]));
            document.add(new Paragraph("  Тип: " + types[i % types.length]));
            document.add(new Paragraph("  Подразделение: " + departments[i % departments.length]));
            document.add(new Paragraph("  Расход: " + String.format("%.1f", 25.5 + (Math.random() * 74.5)) + " кг"));
            document.add(new Paragraph("  Остаток: " + String.format("%.1f", 5.0 + (Math.random() * 84.5)) + " кг"));
            document.add(new Paragraph("  Стоимость: " + String.format("%.0f", 120.0 + (Math.random() * 180.0)) + " руб/кг"));
            document.add(new Paragraph("  Дата поставки: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("  Поставщик: " + suppliers[i % suppliers.length]));
            document.add(new Paragraph("  Статус: " + statuses[i % statuses.length]));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateMaterialsCsvWithData() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID материала,Название,Тип,Подразделение,Расход (кг),Остаток (кг),Стоимость за кг,Дата поставки,Поставщик,Статус\n");
        
        String[] materialNames = {"Проволока ER70S-6", "Электроды УОНИ-13/55", "Флюс АН-348А", "Газ CO2", "Газ Ar"};
        String[] types = {"Проволока", "Электроды", "Флюс", "Газ", "Газ"};
        String[] departments = {"Цех №1", "Цех №2", "Сборочный участок", "Ремонтный участок"};
        String[] suppliers = {"ООО МеталлСнаб", "ИП Иванов", "ООО ГазТрейд", "ООО ЭлектродСтрой"};
        String[] statuses = {"В наличии", "Заканчивается", "Под заказ", "Нет в наличии"};
        
        for (int i = 0; i < 20; i++) {
            csv.append(3000 + i).append(",");
            csv.append(materialNames[i % materialNames.length]).append(",");
            csv.append(types[i % types.length]).append(",");
            csv.append(departments[i % departments.length]).append(",");
            csv.append(String.format("%.1f", 25.5 + (Math.random() * 74.5))).append(",");
            csv.append(String.format("%.1f", 5.0 + (Math.random() * 84.5))).append(",");
            csv.append(String.format("%.0f", 120.0 + (Math.random() * 180.0))).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28))).append(",");
            csv.append(suppliers[i % suppliers.length]).append(",");
            csv.append(statuses[i % statuses.length]);
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
            document.add(new Paragraph("  ID: " + (4000 + i)));
            document.add(new Paragraph("  Тип шва: " + weldTypes[i % weldTypes.length]));
            document.add(new Paragraph("  Сварщик: " + welders[i % welders.length]));
            document.add(new Paragraph("  Оборудование: " + equipment[i % equipment.length]));
            document.add(new Paragraph("  Материал: " + materials[i % materials.length]));
            document.add(new Paragraph("  Длина: " + (50 + (int)(Math.random() * 450)) + " мм"));
            document.add(new Paragraph("  Толщина: " + String.format("%.1f", 3.0 + (Math.random() * 15.0)) + " мм"));
            document.add(new Paragraph("  Ток: " + (150 + (int)(Math.random() * 150)) + " А"));
            document.add(new Paragraph("  Напряжение: " + (18 + (int)(Math.random() * 12)) + " В"));
            document.add(new Paragraph("  Скорость: " + (200 + (int)(Math.random() * 300)) + " мм/мин"));
            document.add(new Paragraph("  Качество: " + (85 + (int)(Math.random() * 15)) + "%"));
            document.add(new Paragraph("  Дата: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
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
    private byte[] generateErrorsReportWithData(String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return generateErrorsExcelWithData();
        } else if ("PDF".equalsIgnoreCase(format)) {
            return generateErrorsPdfWithData();
        } else if ("CSV".equalsIgnoreCase(format)) {
            return generateErrorsCsvWithData();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    private byte[] generateErrorsExcelWithData() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчет по ошибкам оборудования");
            
            // Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "ID ошибки", "Оборудование", "Тип ошибки", "Описание", "Критичность", "Дата возникновения",
                "Дата устранения", "Время простоя (часы)", "Стоимость ремонта", "Ответственный", "Статус"
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
            String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250", "Аппарат плазменной резки"};
            String[] errorTypes = {"Электрическая", "Механическая", "Термическая", "Программная", "Сетевая"};
            String[] descriptions = {"Короткое замыкание", "Износ деталей", "Перегрев", "Сбой ПО", "Потеря связи"};
            String[] criticality = {"Низкая", "Средняя", "Высокая", "Критическая"};
            String[] responsible = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
            String[] statuses = {"Открыта", "В работе", "Решена", "Закрыта"};
            
            for (int i = 0; i < 20; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(5000 + i);
                row.createCell(1).setCellValue(equipment[i % equipment.length]);
                row.createCell(2).setCellValue(errorTypes[i % errorTypes.length]);
                row.createCell(3).setCellValue(descriptions[i % descriptions.length]);
                row.createCell(4).setCellValue(criticality[i % criticality.length]);
                row.createCell(5).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
                row.createCell(6).setCellValue("2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28)));
                row.createCell(7).setCellValue(2 + (int)(Math.random() * 48)); // 2-50 часов
                row.createCell(8).setCellValue(5000 + (int)(Math.random() * 45000)); // 5000-50000 руб
                row.createCell(9).setCellValue(responsible[i % responsible.length]);
                row.createCell(10).setCellValue(statuses[i % statuses.length]);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] generateErrorsPdfWithData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Заголовок отчета
        Paragraph title = new Paragraph("Отчет по ошибкам сварочного оборудования");
        title.setFontSize(18);
        title.setBold();
        document.add(title);
        document.add(new Paragraph(""));
        
        // Данные
        String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250", "Аппарат плазменной резки"};
        String[] errorTypes = {"Электрическая", "Механическая", "Термическая", "Программная", "Сетевая"};
        String[] descriptions = {"Короткое замыкание", "Износ деталей", "Перегрев", "Сбой ПО", "Потеря связи"};
        String[] criticality = {"Низкая", "Средняя", "Высокая", "Критическая"};
        String[] responsible = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
        String[] statuses = {"Открыта", "В работе", "Решена", "Закрыта"};
        
        for (int i = 0; i < 18; i++) {
            document.add(new Paragraph("Ошибка " + (i + 1) + ":"));
            document.add(new Paragraph("  ID: " + (5000 + i)));
            document.add(new Paragraph("  Оборудование: " + equipment[i % equipment.length]));
            document.add(new Paragraph("  Тип ошибки: " + errorTypes[i % errorTypes.length]));
            document.add(new Paragraph("  Описание: " + descriptions[i % descriptions.length]));
            document.add(new Paragraph("  Критичность: " + criticality[i % criticality.length]));
            document.add(new Paragraph("  Дата возникновения: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("  Дата устранения: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("  Время простоя: " + (2 + (int)(Math.random() * 12)) + " часов"));
            document.add(new Paragraph("  Стоимость ремонта: " + (5000 + (int)(Math.random() * 45000)) + " руб"));
            document.add(new Paragraph("  Ответственный: " + responsible[i % responsible.length]));
            document.add(new Paragraph("  Статус: " + statuses[i % statuses.length]));
            document.add(new Paragraph(""));
        }
        document.close();
        return outputStream.toByteArray();
    }

    private byte[] generateErrorsCsvWithData() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID ошибки,Оборудование,Тип ошибки,Описание,Критичность,Дата возникновения,Дата устранения,Время простоя (часы),Стоимость ремонта,Ответственный,Статус\n");
        
        String[] equipment = {"Сварочный аппарат TIG-200", "Полуавтомат MIG-350", "Инвертор MMA-250", "Аппарат плазменной резки"};
        String[] errorTypes = {"Электрическая", "Механическая", "Термическая", "Программная", "Сетевая"};
        String[] descriptions = {"Короткое замыкание", "Износ деталей", "Перегрев", "Сбой ПО", "Потеря связи"};
        String[] criticality = {"Низкая", "Средняя", "Высокая", "Критическая"};
        String[] responsible = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Козлов К.К."};
        String[] statuses = {"Открыта", "В работе", "Решена", "Закрыта"};
        
        for (int i = 0; i < 18; i++) {
            csv.append(5000 + i).append(",");
            csv.append(equipment[i % equipment.length]).append(",");
            csv.append(errorTypes[i % errorTypes.length]).append(",");
            csv.append(descriptions[i % descriptions.length]).append(",");
            csv.append(criticality[i % criticality.length]).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28))).append(",");
            csv.append("2024-").append(String.format("%02d", 1 + (int)(Math.random() * 12))).append("-").append(String.format("%02d", 1 + (int)(Math.random() * 28))).append(",");
            csv.append(2 + (int)(Math.random() * 12)).append(",");
            csv.append(5000 + (int)(Math.random() * 45000)).append(",");
            csv.append(responsible[i % responsible.length]).append(",");
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
            document.add(new Paragraph("  ID: " + (6000 + i)));
            document.add(new Paragraph("  Шов: " + weldTypes[i % weldTypes.length] + " шов №" + (i + 1)));
            document.add(new Paragraph("  Сварщик: " + welders[i % welders.length]));
            document.add(new Paragraph("  Тип нарушения: " + violationTypes[i % violationTypes.length]));
            document.add(new Paragraph("  Описание: " + descriptions[i % descriptions.length]));
            document.add(new Paragraph("  Критичность: " + criticality[i % criticality.length]));
            document.add(new Paragraph("  Дата обнаружения: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("  Дата исправления: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("  Ответственный: " + responsible[i % responsible.length]));
            document.add(new Paragraph("  Статус: " + statuses[i % statuses.length]));
            document.add(new Paragraph("  Штраф: " + (1000 + (int)(Math.random() * 9000)) + " руб"));
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
            document.add(new Paragraph("  ID: " + (7000 + i)));
            document.add(new Paragraph("  Название: " + taskNames[i % taskNames.length] + " №" + (i + 1)));
            document.add(new Paragraph("  Описание: Детальное описание задания " + (i + 1)));
            document.add(new Paragraph("  Сварщик: " + welders[i % welders.length]));
            document.add(new Paragraph("  Оборудование: " + equipment[i % equipment.length]));
            document.add(new Paragraph("  Материал: " + materials[i % materials.length]));
            document.add(new Paragraph("  Плановая дата: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("  Фактическая дата: " + "2024-" + String.format("%02d", 1 + (int)(Math.random() * 12)) + "-" + String.format("%02d", 1 + (int)(Math.random() * 28))));
            document.add(new Paragraph("  Статус: " + statuses[i % statuses.length]));
            document.add(new Paragraph("  Прогресс: " + (10 + (int)(Math.random() * 90)) + "%"));
            document.add(new Paragraph("  Приоритет: " + priorities[i % priorities.length]));
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