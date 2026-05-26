package org.alloy.controllers;

import org.alloy.models.dto.ReportGenerationJobStartDTO;
import org.alloy.models.dto.ReportGenerationJobStatusDTO;
import org.alloy.services.UserAccountService;
import org.alloy.services.report.ReportFileResult;
import org.alloy.services.report.ReportGenerationJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports/generation-jobs")
public class ReportGenerationJobController {

    @Autowired
    private ReportGenerationJobService jobService;

    @Autowired
    private UserAccountService userAccountService;

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @PostMapping
    public ResponseEntity<ReportGenerationJobStatusDTO> startJob(@RequestBody ReportGenerationJobStartDTO request) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jobService.startJob(request, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/{jobId}")
    public ResponseEntity<ReportGenerationJobStatusDTO> getStatus(@PathVariable String jobId) {
        Integer userId = getCurrentUserId();
        ReportGenerationJobStatusDTO status = jobService.getStatus(jobId, userId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @PreAuthorize("hasAuthority('PERMISSION_WORK_WITH_REPORTS')")
    @GetMapping("/{jobId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String jobId) {
        Integer userId = getCurrentUserId();
        ReportFileResult file = jobService.getResult(jobId, userId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return file.toResponseEntity();
    }

    private Integer getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            String username = authentication.getName();
            return userAccountService.getUserAccountByUserName(username)
                    .map(u -> u.getId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
