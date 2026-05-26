package org.alloy.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReportGenerationJobStatusDTO {
    private String jobId;
    /** QUEUED | RUNNING | COMPLETED | FAILED */
    private String status;
    private int percent;
    private String message;
    private String filename;
    private String error;
}
