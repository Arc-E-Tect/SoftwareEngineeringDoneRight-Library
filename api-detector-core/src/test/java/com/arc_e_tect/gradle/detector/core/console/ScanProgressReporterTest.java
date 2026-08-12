package com.arc_e_tect.gradle.detector.core.console;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScanProgressReporter")
class ScanProgressReporterTest {

    private final RecordingLogger logger = new RecordingLogger();

    @Test
    @DisplayName("emits nothing before the item-count throttle is reached")
    void emitsNothingBeforeItemCountThrottleIsReached() {
        ScanProgressReporter reporter =
                new ScanProgressReporter(logger, "Scanning", 100, 5, 3600, fixedClock(0));

        for (int i = 0; i < 4; i++) {
            reporter.step();
        }

        assertThat(logger.lifecycleMessages()).isEmpty();
    }

    @Test
    @DisplayName("emits a status line once the item-count throttle is reached")
    void emitsStatusLineOnceItemCountThrottleIsReached() {
        ScanProgressReporter reporter =
                new ScanProgressReporter(logger, "Scanning", 100, 5, 3600, fixedClock(0));

        for (int i = 0; i < 5; i++) {
            reporter.step();
        }

        assertThat(logger.lifecycleMessages()).containsExactly("Scanning: 5/100 (5%)");
    }

    @Test
    @DisplayName("does not emit again until another full item-count throttle window has passed")
    void doesNotEmitAgainUntilAnotherFullItemCountWindow() {
        ScanProgressReporter reporter =
                new ScanProgressReporter(logger, "Scanning", 100, 5, 3600, fixedClock(0));

        for (int i = 0; i < 9; i++) {
            reporter.step();
        }

        assertThat(logger.lifecycleMessages()).hasSize(1);
    }

    @Test
    @DisplayName("emits nothing before the elapsed-time throttle is reached")
    void emitsNothingBeforeElapsedTimeThrottleIsReached() {
        AtomicLong nanos = new AtomicLong(0);
        ScanProgressReporter reporter =
                new ScanProgressReporter(logger, "Scanning", 100, 1_000_000, 2, nanos::get);

        nanos.set(1_000_000_000L); // 1 second, less than the 2-second throttle
        reporter.step();

        assertThat(logger.lifecycleMessages()).isEmpty();
    }

    @Test
    @DisplayName("emits a status line once the elapsed-time throttle is reached, even with few items")
    void emitsStatusLineOnceElapsedTimeThrottleIsReached() {
        AtomicLong nanos = new AtomicLong(0);
        ScanProgressReporter reporter =
                new ScanProgressReporter(logger, "Scanning", 100, 1_000_000, 2, nanos::get);

        nanos.set(2_000_000_000L); // exactly 2 seconds
        reporter.step();

        assertThat(logger.lifecycleMessages()).containsExactly("Scanning: 1/100 (1%)");
    }

    @Test
    @DisplayName("complete() emits a final line even when the last step landed inside the throttle window")
    void completeEmitsFinalLineDespiteThrottleWindow() {
        ScanProgressReporter reporter =
                new ScanProgressReporter(logger, "Scanning", 100, 50, 3600, fixedClock(0));

        reporter.step();
        reporter.complete();

        assertThat(logger.lifecycleMessages()).containsExactly("Scanning: done, 1 item(s)");
    }

    @Test
    @DisplayName("a zero-total determinate reporter emits nothing until complete(), which reports 0 items cleanly")
    void zeroTotalReporterEmitsNothingUntilComplete() {
        ScanProgressReporter reporter = ScanProgressReporter.determinate(logger, "Scanning", 0);

        reporter.complete();

        assertThat(logger.lifecycleMessages()).containsExactly("Scanning: done, 0 item(s)");
    }

    @Test
    @DisplayName("indeterminate mode reports a running count with no total or percentage")
    void indeterminateModeReportsRunningCount() {
        ScanProgressReporter reporter =
                new ScanProgressReporter(logger, "Resolving", -1, 3, 3600, fixedClock(0));

        for (int i = 0; i < 3; i++) {
            reporter.step();
        }

        assertThat(logger.lifecycleMessages()).containsExactly("Resolving: 3 processed so far");
    }

    @Test
    @DisplayName("indeterminate mode's complete() line matches the determinate wording")
    void indeterminateCompleteLineMatchesDeterminateWording() {
        ScanProgressReporter reporter = ScanProgressReporter.indeterminate(logger, "Resolving");

        reporter.step();
        reporter.step();
        reporter.complete();

        assertThat(logger.lifecycleMessages()).contains("Resolving: done, 2 item(s)");
    }

    @Test
    @DisplayName("step(String detail) appends the detail to the line it actually emits")
    void stepWithDetailAppendsDetailToEmittedLine() {
        ScanProgressReporter reporter =
                new ScanProgressReporter(logger, "Scanning", 100, 1, 3600, fixedClock(0));

        reporter.step("UserController.java");

        assertThat(logger.lifecycleMessages()).containsExactly("Scanning: 1/100 (1%) - UserController.java");
    }

    private LongSupplier fixedClock(long value) {
        return () -> value;
    }
}
