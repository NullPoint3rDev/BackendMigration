package org.alloy.services;

import org.alloy.models.entities.AutomatedReport;
import org.alloy.repositories.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для исправления некорректных данных в автоматических отчетах
 */
@Service
@Transactional
public class AutomatedReportDataFixService {

    @Autowired
    private AutomatedReportService automatedReportService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    /**
     * Исправляет все автоматические отчеты с некорректными данными
     */
    public void fixAllAutomatedReports() {
        System.out.println("DEBUG AutomatedReportDataFixService: Starting data fix for all automated reports");
        
        try {
            List<AutomatedReport> allReports = automatedReportService.getAllAutomatedReports();
            int fixedCount = 0;
            
            for (AutomatedReport report : allReports) {
                if (fixAutomatedReportData(report)) {
                    fixedCount++;
                }
            }
            
            System.out.println("DEBUG AutomatedReportDataFixService: Fixed " + fixedCount + " out of " + allReports.size() + " automated reports");
            
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportDataFixService: Failed to fix automated reports data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Исправляет данные конкретного автоматического отчета
     */
    public boolean fixAutomatedReportData(AutomatedReport automatedReport) {
        boolean needsUpdate = false;
        
        // Исправляем template_type если он null или пустой
        if (automatedReport.getTemplateType() == null || automatedReport.getTemplateType().trim().isEmpty()) {
            automatedReport.setTemplateType("equipment"); // дефолтный тип
            needsUpdate = true;
            System.out.println("DEBUG AutomatedReportDataFixService: Fixed template_type for report: " + automatedReport.getName());
        }
        
        // Исправляем template_name если он null или пустой
        if (automatedReport.getTemplateName() == null || automatedReport.getTemplateName().trim().isEmpty()) {
            automatedReport.setTemplateName("Отчет по работе оборудования"); // дефолтное название
            needsUpdate = true;
            System.out.println("DEBUG AutomatedReportDataFixService: Fixed template_name for report: " + automatedReport.getName());
        }
        
        // Исправляем created_by если он null или ссылается на несуществующего пользователя
        if (automatedReport.getCreatedBy() == null || !userExists(automatedReport.getCreatedBy())) {
            Integer firstUserId = getFirstUserId();
            if (firstUserId != null) {
                automatedReport.setCreatedBy(firstUserId);
                needsUpdate = true;
                System.out.println("DEBUG AutomatedReportDataFixService: Fixed created_by for report: " + automatedReport.getName() + " to user ID: " + firstUserId);
            }
        }
        
        // Сохраняем изменения если они были
        if (needsUpdate) {
            try {
                automatedReportService.updateAutomatedReport(automatedReport);
                System.out.println("DEBUG AutomatedReportDataFixService: Successfully updated automated report: " + automatedReport.getName());
                return true;
            } catch (Exception e) {
                System.err.println("ERROR AutomatedReportDataFixService: Failed to update automated report: " + e.getMessage());
                return false;
            }
        }
        
        return false;
    }

    /**
     * Проверяет, существует ли пользователь с указанным ID
     */
    private boolean userExists(Integer userId) {
        try {
            return userAccountRepository.existsById(userId);
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportDataFixService: Failed to check if user exists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Получает ID первого пользователя в системе
     */
    private Integer getFirstUserId() {
        try {
            return userAccountRepository.findAll().stream()
                .map(user -> user.getId())
                .min(Integer::compareTo)
                .orElse(null);
        } catch (Exception e) {
            System.err.println("ERROR AutomatedReportDataFixService: Failed to get first user ID: " + e.getMessage());
            return null;
        }
    }
}
