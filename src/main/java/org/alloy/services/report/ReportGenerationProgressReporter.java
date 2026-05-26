package org.alloy.services.report;

@FunctionalInterface
public interface ReportGenerationProgressReporter {
    ReportGenerationProgressReporter NOOP = (percent, message) -> { };

    void update(int percent, String message);
}
