package com.arc_e_tect.gradle.detector.core.console;

import org.gradle.api.logging.Logger;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Emits periodic, low-overhead {@code LIFECYCLE}-level status lines for a long-running scan loop
 * (controller scanning, OpenAPI {@code $ref} resolution, verification-evidence scanning, ...), so
 * a consumer watching the build knows the task is still alive and roughly how far along it is.
 *
 * <p>Deliberately built only on the public {@link Logger} API - one plain line per status update,
 * never overwriting a line in place - rather than Gradle's internal, unsupported rich-console
 * single-line progress indicator. These plugins are published to the Gradle Plugin Portal for
 * consumers on Gradle versions this codebase doesn't control; the internal progress API has
 * changed shape across versions and offers no compatibility guarantee. This is a deliberate
 * trade-off in exchange for forward/backward compatibility, not an oversight.</p>
 *
 * <p>A status line is emitted every {@code everyNItems} items <strong>or</strong> every
 * {@code everySeconds} seconds since the last emission, whichever comes first - never on every
 * single item, and never silent for more than {@code everySeconds} regardless of how many items
 * are processed in between. {@link #complete()} always emits a final summary line, even if the
 * most recent {@link #step()} landed inside the throttle window - the final line is never
 * suppressed by throttling.</p>
 */
public final class ScanProgressReporter {

    /** Default number of items between emitted status lines, absent an explicit override. */
    public static final int DEFAULT_EVERY_N_ITEMS = 50;

    /** Default number of seconds between emitted status lines, absent an explicit override. */
    public static final long DEFAULT_EVERY_SECONDS = 2;

    private static final int INDETERMINATE_TOTAL = -1;

    private final Logger logger;
    private final String phaseLabel;
    private final int total;
    private final int everyNItems;
    private final long everyNanos;
    private final LongSupplier nanoTimeSource;

    private int count;
    private int lastEmittedCount;
    private long lastEmittedNanos;

    /**
     * Creates a reporter with the default throttle (every {@value #DEFAULT_EVERY_N_ITEMS} items or
     * every {@value #DEFAULT_EVERY_SECONDS} seconds) and the real wall-clock time source. Prefer
     * {@link #determinate(Logger, String, int)} or {@link #indeterminate(Logger, String)}.
     *
     * @param logger     the logger status lines are emitted to, at {@code LIFECYCLE} level
     * @param phaseLabel short label identifying the scan phase, e.g. {@code "Scanning @RestController classes"}
     * @param total      the total number of items expected, or a negative number for an
     *                   indeterminate-total scan (the total isn't known ahead of time)
     */
    public ScanProgressReporter(Logger logger, String phaseLabel, int total) {
        this(logger, phaseLabel, total, DEFAULT_EVERY_N_ITEMS, DEFAULT_EVERY_SECONDS, System::nanoTime);
    }

    /**
     * Creates a reporter with an explicit throttle and the real wall-clock time source.
     *
     * @param logger       the logger status lines are emitted to, at {@code LIFECYCLE} level
     * @param phaseLabel   short label identifying the scan phase
     * @param total        the total number of items expected, or a negative number for an
     *                     indeterminate-total scan
     * @param everyNItems  emit a status line at least this often, counted in processed items
     * @param everySeconds emit a status line at least this often, counted in elapsed seconds since
     *                     the last emission
     */
    public ScanProgressReporter(Logger logger, String phaseLabel, int total, int everyNItems, long everySeconds) {
        this(logger, phaseLabel, total, everyNItems, everySeconds, System::nanoTime);
    }

    /**
     * Creates a reporter with an explicit throttle and time source, for use by tests that need to
     * control elapsed time without a real {@code Thread.sleep}.
     *
     * @param logger         the logger status lines are emitted to, at {@code LIFECYCLE} level
     * @param phaseLabel     short label identifying the scan phase
     * @param total          the total number of items expected, or a negative number for an
     *                       indeterminate-total scan
     * @param everyNItems    emit a status line at least this often, counted in processed items
     * @param everySeconds   emit a status line at least this often, counted in elapsed seconds
     *                       since the last emission
     * @param nanoTimeSource nanosecond tick source, normally {@code System::nanoTime}
     */
    ScanProgressReporter(
            Logger logger, String phaseLabel, int total, int everyNItems, long everySeconds,
            LongSupplier nanoTimeSource) {
        this.logger = logger;
        this.phaseLabel = phaseLabel;
        this.total = total;
        this.everyNItems = everyNItems;
        this.everyNanos = TimeUnit.SECONDS.toNanos(everySeconds);
        this.nanoTimeSource = nanoTimeSource;
        this.lastEmittedNanos = nanoTimeSource.getAsLong();
    }

    /**
     * Creates a reporter for a scan whose total item count is known ahead of time. Emitted lines
     * include a running fraction and percentage, e.g. {@code "Scanning @RestController classes: 150/438 (34%)"}.
     *
     * @param logger     the logger status lines are emitted to, at {@code LIFECYCLE} level
     * @param phaseLabel short label identifying the scan phase
     * @param total      the total number of items expected; {@code 0} is valid and not an error
     * @return a new determinate-mode reporter, using the default throttle
     */
    public static ScanProgressReporter determinate(Logger logger, String phaseLabel, int total) {
        return new ScanProgressReporter(logger, phaseLabel, total);
    }

    /**
     * Creates a reporter for a scan whose total item count isn't known ahead of time. Emitted
     * lines report only a running count, e.g. {@code "Resolving OpenAPI documents: 27 processed so far"}.
     *
     * @param logger     the logger status lines are emitted to, at {@code LIFECYCLE} level
     * @param phaseLabel short label identifying the scan phase
     * @return a new indeterminate-mode reporter, using the default throttle
     */
    public static ScanProgressReporter indeterminate(Logger logger, String phaseLabel) {
        return new ScanProgressReporter(logger, phaseLabel, INDETERMINATE_TOTAL);
    }

    /**
     * Records one item processed, emitting a status line if the throttle window has elapsed.
     * Equivalent to {@link #step(String)} with no detail.
     */
    public void step() {
        step(null);
    }

    /**
     * Records one item processed, emitting a status line - with {@code detail} appended, when
     * given - if the throttle window has elapsed.
     *
     * @param detail short description of the current item, appended to the line only when this
     *               call itself results in an emission; or {@code null}/blank for no detail
     */
    public void step(String detail) {
        count++;
        long now = nanoTimeSource.getAsLong();
        boolean dueByCount = (count - lastEmittedCount) >= everyNItems;
        boolean dueByTime = (now - lastEmittedNanos) >= everyNanos;
        if (dueByCount || dueByTime) {
            logger.lifecycle(progressLine(detail));
            lastEmittedCount = count;
            lastEmittedNanos = now;
        }
    }

    /**
     * Emits a final summary line unconditionally, regardless of throttling state - the last
     * {@link #step()} may have landed inside the throttle window, but this line is never
     * suppressed. Safe to call on a reporter that never had {@link #step()} called at all (the
     * {@code 0}-items-processed case).
     */
    public void complete() {
        logger.lifecycle(phaseLabel + ": done, " + count + " item(s)");
    }

    private String progressLine(String detail) {
        String base = total >= 0
                ? phaseLabel + ": " + count + "/" + total + " (" + percentage() + "%)"
                : phaseLabel + ": " + count + " processed so far";
        return isBlank(detail) ? base : base + " - " + detail;
    }

    private long percentage() {
        return total == 0 ? 100 : Math.round(100.0 * count / total);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
