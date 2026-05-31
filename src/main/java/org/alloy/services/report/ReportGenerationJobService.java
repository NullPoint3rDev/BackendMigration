package org.alloy.services.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.metrics.ReportMetrics;
import org.alloy.models.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ReportGenerationJobService {

    private static final long JOB_TTL_MS = 60 * 60 * 1000L;

    private final ConcurrentHashMap<String, JobEntry> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    private ReportGenerationExecutor reportGenerationExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReportMetrics reportMetrics;

    public ReportGenerationJobStatusDTO startJob(ReportGenerationJobStartDTO request, Integer userId) {
        if (request == null || request.getReportType() == null || request.getReportType().isBlank()) {
            throw new IllegalArgumentException("reportType обязателен");
        }
        if (request.getPayload() == null) {
            throw new IllegalArgumentException("payload обязателен");
        }
        String jobId = UUID.randomUUID().toString();
        JobEntry entry = new JobEntry(userId);
        jobs.put(jobId, entry);
        entry.setStatus("RUNNING");
        entry.update(0, "Запуск формирования отчёта…");
        executor.submit(() -> runJob(jobId, request.getReportType().trim(), request.getPayload()));
        return toDto(jobId, entry);
    }

    private void runJob(String jobId, String reportType, Map<String, Object> payload) {
        JobEntry entry = jobs.get(jobId);
        if (entry == null) return;
        try {
            ReportGenerationProgressContext.run(jobId, entry::update, () -> {
                ReportFileResult file = dispatch(reportType, payload);
                entry.complete(file);
            });
        } catch (Exception e) {
            entry.fail(e.getMessage() != null ? e.getMessage() : "Ошибка генерации отчёта");
            System.err.println("[REPORT-JOB] jobId=" + jobId + " failed: " + e.getMessage());
            e.printStackTrace();
        }
        purgeExpired();
    }

    private ReportFileResult dispatch(String reportType, Map<String, Object> payload) {
        long start = System.nanoTime();
        boolean success = false;
        try {
            ReportFileResult result = dispatchInternal(reportType, payload);
            success = true;
            return result;
        } finally {
            reportMetrics.record(reportType, "manual", success, System.nanoTime() - start);
        }
    }

    private ReportFileResult dispatchInternal(String reportType, Map<String, Object> payload) {
        String type = reportType.toUpperCase();
        switch (type) {
            case "WIRE_CONSUMPTION":
            case "ПО РАСХОДУ ПРОВОЛОКИ":
                WireConsumptionReportGenerationDTO wire = objectMapper.convertValue(payload, WireConsumptionReportGenerationDTO.class);
                return reportGenerationExecutor.buildWireConsumptionReport(wire);
            case "WELDER_WORK":
            case "ПО РАБОТЕ СВАРЩИКА (ШВЫ)":
            case "ПО РАБОТЕ СВАРЩИКА":
                WelderWorkReportGenerationDTO welder = objectMapper.convertValue(payload, WelderWorkReportGenerationDTO.class);
                return reportGenerationExecutor.buildWelderWorkReport(welder);
            case "EQUIPMENT_WORK":
            case "ПО РАБОТЕ ОБОРУДОВАНИЯ (ШВЫ)":
            case "ПО РАБОТЕ ОБОРУДОВАНИЯ":
                EquipmentWorkReportGenerationDTO equipment = objectMapper.convertValue(payload, EquipmentWorkReportGenerationDTO.class);
                return reportGenerationExecutor.buildEquipmentWorkReport(equipment);
            case "EQUIPMENT_MALFUNCTION":
            case "ПО НЕИСПРАВНОСТЯМ ОБОРУДОВАНИЯ":
                EquipmentMalfunctionReportGenerationDTO malfunction = objectMapper.convertValue(payload, EquipmentMalfunctionReportGenerationDTO.class);
                return reportGenerationExecutor.buildEquipmentMalfunctionReport(malfunction);
            default:
                throw new IllegalArgumentException("Неизвестный тип отчёта: " + reportType);
        }
    }

    public ReportGenerationJobStatusDTO getStatus(String jobId, Integer userId) {
        JobEntry entry = jobs.get(jobId);
        if (entry == null) {
            return null;
        }
        if (userId != null && entry.userId != null && !entry.userId.equals(userId)) {
            return null;
        }
        return toDto(jobId, entry);
    }

    public ReportFileResult getResult(String jobId, Integer userId) {
        JobEntry entry = jobs.get(jobId);
        if (entry == null || !"COMPLETED".equals(entry.status)) {
            return null;
        }
        if (userId != null && entry.userId != null && !entry.userId.equals(userId)) {
            return null;
        }
        return entry.result;
    }

    private static ReportGenerationJobStatusDTO toDto(String jobId, JobEntry entry) {
        ReportGenerationJobStatusDTO dto = new ReportGenerationJobStatusDTO();
        dto.setJobId(jobId);
        dto.setStatus(entry.status);
        dto.setPercent(entry.percent);
        dto.setMessage(entry.message);
        dto.setFilename(entry.result != null ? entry.result.getFilename() : null);
        dto.setError(entry.error);
        return dto;
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        jobs.entrySet().removeIf(e -> now - e.getValue().createdAtMs > JOB_TTL_MS);
    }

    private static final class JobEntry {
        final Integer userId;
        final long createdAtMs = System.currentTimeMillis();
        volatile String status = "QUEUED";
        volatile int percent;
        volatile String message = "";
        volatile String error;
        volatile ReportFileResult result;

        JobEntry(Integer userId) {
            this.userId = userId;
        }

        void setStatus(String status) {
            this.status = status;
        }

        void update(int percent, String message) {
            this.percent = Math.max(0, Math.min(100, percent));
            if (message != null && !message.isBlank()) {
                this.message = message;
            }
            this.status = "RUNNING";
        }

        void complete(ReportFileResult file) {
            this.result = file;
            this.percent = 100;
            this.message = "Отчёт готов";
            this.status = "COMPLETED";
        }

        void fail(String error) {
            this.error = error;
            this.message = "Ошибка формирования";
            this.status = "FAILED";
        }
    }
}
