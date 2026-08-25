package com.arc_e_tect.gradle.detector.core.console;

import org.gradle.api.logging.Logger;

/**
 * Emits the always-visible, top-level "[N/M] stage" header lines that mark the major phases of a
 * detector plugin run (controller scanning, OpenAPI endpoint collection, verification-evidence
 * scanning, ...), e.g. {@code "Doppelganger API Detector: [1/3] Scanning @RestController
 * classes..."}.
 *
 * <p>Always emitted at {@code LIFECYCLE} level - unlike the per-item detail a {@link
 * ScanProgressReporter} reports for the work inside each stage, which is {@code INFO}-only and
 * thus hidden unless the build is run with {@code --info}. Pairing the two keeps default output to
 * one line per stage plus a completion summary, while still letting {@code --info} reveal
 * per-batch progress within a stage.</p>
 */
public final class DetectorStageReporter {

    private final Logger logger;
    private final String pluginLabel;
    private final int totalStages;

    private int currentStage;

    /**
     * Creates a reporter for a plugin run with a known, fixed number of stages.
     *
     * @param logger      the logger stage headers are emitted to, at {@code LIFECYCLE} level
     * @param pluginLabel short label identifying the plugin, e.g. {@code "Doppelganger API Detector"}
     * @param totalStages the total number of stages the run will report
     */
    public DetectorStageReporter(Logger logger, String pluginLabel, int totalStages) {
        this.logger = logger;
        this.pluginLabel = pluginLabel;
        this.totalStages = totalStages;
    }

    /**
     * Advances to the next stage and emits its header line, e.g. {@code "Doppelganger API
     * Detector: [2/3] Collecting OpenAPI endpoints..."}. Calling this more times than {@code
     * totalStages} keeps counting upward rather than clamping - callers are expected to call it
     * exactly {@code totalStages} times, once per stage, in order.
     *
     * @param description short description of the stage being entered, without a trailing
     *                    ellipsis or punctuation, e.g. {@code "Collecting OpenAPI endpoints"}
     */
    public void stage(String description) {
        currentStage++;
        logger.lifecycle(pluginLabel + ": [" + currentStage + "/" + totalStages + "] " + description + "...");
    }
}
