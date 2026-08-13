package com.arc_e_tect.gradle.detector.core.progress;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Writes the {@code == Progress Over Time} AsciiDoc table section shared by Shadow, Mirage, and
 * Doppelganger API Detector's own reports, from a loaded/advanced {@link ContractProgressRecord}
 * history map.
 *
 * <p>Always renders all three lifecycle stages - declared, implemented, verified - regardless of
 * which stages the calling plugin itself has evidence for: a plugin that never observes
 * verification evidence (Shadow, Mirage) simply never advances {@code verifiedAt} on any record,
 * so that row correctly and honestly reads {@code 0} rather than being omitted.</p>
 */
public class ContractProgressTableWriter {

    private static final DateTimeFormatter TRACKED_SINCE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    /** Creates a new {@code ContractProgressTableWriter}. */
    public ContractProgressTableWriter() {}

    /**
     * Writes the {@code == Progress Over Time} section to {@code writer}, or nothing at all when
     * {@code history} is empty.
     *
     * @param writer  the AsciiDoc output to append to
     * @param history the contract progress history to summarise, keyed by fingerprint
     */
    public void write(PrintWriter writer, Map<String, ContractProgressRecord> history) {
        if (history.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        Instant trackedSince = history.values().stream()
                .flatMap(record -> Stream.of(
                        record.declaredAt(), record.implementedAt(), record.verifiedAt(),
                        record.lastSeenAt(), record.removedAt()))
                .filter(instant -> instant != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        long removedNotSeen = history.values().stream().filter(record -> record.removedAt() != null).count();

        writer.println("== Progress Over Time");
        writer.println();
        writer.println("[cols=\"1,1\",options=\"header\"]");
        writer.println("|===");
        writer.println("| Metric | Value");
        writer.println();
        writer.println("| Tracked since");
        writer.println("| " + (trackedSince != null ? TRACKED_SINCE_FORMATTER.format(trackedSince) : "N/A"));
        writer.println();
        writeWindowedMetric(writer, "Declared", history, now, ContractProgressRecord::declaredAt);
        writeWindowedMetric(writer, "Implemented", history, now, ContractProgressRecord::implementedAt);
        writeWindowedMetric(writer, "Verified", history, now, ContractProgressRecord::verifiedAt);
        writer.println("| Removed (no longer seen)");
        writer.println("| " + removedNotSeen);
        writer.println("|===");
        writer.println();
    }

    private void writeWindowedMetric(PrintWriter writer, String label, Map<String, ContractProgressRecord> history,
            Instant now, Function<ContractProgressRecord, Instant> timestamp) {
        writer.println("| " + label + " in the last 7 days");
        writer.println("| " + countWithin(history, now, Duration.ofDays(7), timestamp));
        writer.println();
        writer.println("| " + label + " in the last 30 days");
        writer.println("| " + countWithin(history, now, Duration.ofDays(30), timestamp));
        writer.println();
    }

    private long countWithin(Map<String, ContractProgressRecord> history, Instant now, Duration window,
            Function<ContractProgressRecord, Instant> timestamp) {
        Instant threshold = now.minus(window);
        return history.values().stream()
                .map(timestamp)
                .filter(recordedAt -> recordedAt != null && recordedAt.isAfter(threshold))
                .count();
    }
}
