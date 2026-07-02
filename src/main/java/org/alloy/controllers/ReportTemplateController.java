package org.alloy.controllers;

import org.alloy.models.dto.ReportTemplateDTO;
import org.alloy.services.ReportTemplateService;
import org.alloy.services.UserAccountService;
import org.alloy.services.Wt2AccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/report-templates")
public class ReportTemplateController {

    @Autowired
    private ReportTemplateService templateService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private Wt2AccessService wt2AccessService;

    private static final String REPORTS_PREAUTH =
            "hasAuthority('PERMISSION_WORK_WITH_REPORTS') or hasRole('ADMIN_ALLOY') or hasRole('USER_ALLOY')";

    /**
     * Сохранение/обновление шаблона отчета
     */
    @PreAuthorize(REPORTS_PREAUTH)
    @PostMapping
    public ResponseEntity<ReportTemplateDTO> saveTemplate(@RequestBody ReportTemplateDTO template) {
        try {
            assertReportsAccess();
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            ReportTemplateDTO savedTemplate = templateService.saveTemplate(template, userId);
            return ResponseEntity.ok(savedTemplate);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            System.err.println("Ошибка сохранения шаблона: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получение шаблона отчета по ID
     */
    @PreAuthorize(REPORTS_PREAUTH)
    @GetMapping("/{templateId}")
    public ResponseEntity<ReportTemplateDTO> getTemplate(@PathVariable Long templateId) {
        try {
            assertReportsAccess();
            Optional<ReportTemplateDTO> templateOpt = templateService.getTemplateById(templateId);
            if (templateOpt.isPresent()) {
                return ResponseEntity.ok(templateOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шаблона: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получение всех активных шаблонов
     */
    @PreAuthorize(REPORTS_PREAUTH)
    @GetMapping
    public ResponseEntity<List<ReportTemplateDTO>> getAllTemplates() {
        try {
            assertReportsAccess();
            List<ReportTemplateDTO> templates = templateService.getAllActiveTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шаблонов: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получение шаблонов текущего пользователя
     */
    @PreAuthorize(REPORTS_PREAUTH)
    @GetMapping("/my")
    public ResponseEntity<List<ReportTemplateDTO>> getMyTemplates() {
        try {
            assertReportsAccess();
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<ReportTemplateDTO> templates = templateService.getTemplatesByUser(userId);
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шаблонов: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Удаление шаблона
     */
    @PreAuthorize(REPORTS_PREAUTH)
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        try {
            assertReportsAccess();
            Integer userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            templateService.deleteTemplate(templateId, userId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Ошибка удаления шаблона: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void assertReportsAccess() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        wt2AccessService.assertCanWorkWithReports(principal);
    }

    /**
     * Получает ID текущего пользователя
     */
    private Integer getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            String username = authentication.getName();
            var userOpt = userAccountService.getUserAccountByUserName(username);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Ошибка получения текущего пользователя: " + e.getMessage());
            return null;
        }
    }
}

