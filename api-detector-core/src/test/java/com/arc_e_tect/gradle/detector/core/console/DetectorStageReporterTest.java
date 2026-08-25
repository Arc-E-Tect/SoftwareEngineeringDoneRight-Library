package com.arc_e_tect.gradle.detector.core.console;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DetectorStageReporter")
class DetectorStageReporterTest {

    private final RecordingLogger logger = new RecordingLogger();

    @Test
    @DisplayName("emits a numbered header line at LIFECYCLE level for each stage, in order")
    void emitsNumberedHeaderLineForEachStage() {
        DetectorStageReporter reporter = new DetectorStageReporter(logger, "Doppelganger API Detector", 3);

        reporter.stage("Scanning @RestController classes");
        reporter.stage("Collecting OpenAPI endpoints");
        reporter.stage("Scanning Spring RestDocs verification evidence");

        assertThat(logger.lifecycleMessages()).containsExactly(
                "Doppelganger API Detector: [1/3] Scanning @RestController classes...",
                "Doppelganger API Detector: [2/3] Collecting OpenAPI endpoints...",
                "Doppelganger API Detector: [3/3] Scanning Spring RestDocs verification evidence...");
    }

    @Test
    @DisplayName("never emits at INFO level")
    void neverEmitsAtInfoLevel() {
        DetectorStageReporter reporter = new DetectorStageReporter(logger, "Shadow API Detector", 1);

        reporter.stage("Scanning @RestController classes");

        assertThat(logger.infoMessages()).isEmpty();
    }
}
