package org.alloy.services.report;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Прогресс генерации отчёта в фоновой задаче (ThreadLocal). Синхронные эндпоинты /generate не устанавливают контекст.
 */
public final class ReportGenerationProgressContext {

    private static final ThreadLocal<Holder> CURRENT = new ThreadLocal<>();
    /** Счётчик активных job-отчётов (для паузы фонового пересчёта кэша швов на других потоках). */
    private static final AtomicInteger ACTIVE_REPORT_JOBS = new AtomicInteger(0);

    private ReportGenerationProgressContext() {
    }

    public static void run(String jobId, ReportGenerationProgressReporter reporter, Runnable task) {
        Holder holder = new Holder(jobId, reporter != null ? reporter : ReportGenerationProgressReporter.NOOP);
        CURRENT.set(holder);
        ACTIVE_REPORT_JOBS.incrementAndGet();
        try {
            task.run();
        } finally {
            ACTIVE_REPORT_JOBS.decrementAndGet();
            CURRENT.remove();
        }
    }

    public static boolean isReportGenerationActive() {
        return ACTIVE_REPORT_JOBS.get() > 0;
    }

    public static void update(int percent, String message) {
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
