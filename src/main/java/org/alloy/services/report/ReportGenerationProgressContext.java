package org.alloy.services.report;

/**
 * Прогресс генерации отчёта в фоновой задаче (ThreadLocal). Синхронные эндпоинты /generate не устанавливают контекст.
 */
public final class ReportGenerationProgressContext {

    private static final ThreadLocal<Holder> CURRENT = new ThreadLocal<>();

    private ReportGenerationProgressContext() {
    }

    public static void run(String jobId, ReportGenerationProgressReporter reporter, Runnable task) {
        Holder holder = new Holder(jobId, reporter != null ? reporter : ReportGenerationProgressReporter.NOOP);
        CURRENT.set(holder);
        try {
            task.run();
        } finally {
            CURRENT.remove();
        }
    }

    static void update(int percent, String message) {
        Holder h = CURRENT.get();
        if (h != null) {
            h.reporter.update(percent, message);
        }
    }

    public static String currentJobId() {
        Holder h = CURRENT.get();
        return h != null ? h.jobId : null;
    }

    private static final class Holder {
        final String jobId;
        final ReportGenerationProgressReporter reporter;

        Holder(String jobId, ReportGenerationProgressReporter reporter) {
            this.jobId = jobId;
            this.reporter = reporter;
        }
    }
}
