package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContractProgressTableWriter")
class ContractProgressTableWriterTest {

    private final ContractProgressTableWriter writer = new ContractProgressTableWriter();

    @Test
    @DisplayName("writes nothing when history is empty")
    void writesNothingWhenHistoryIsEmpty() {
        String output = render(Map.of());

        assertThat(output).isEmpty();
    }

    @Test
    @DisplayName("reports the earliest non-null timestamp across all records as a human-friendly Tracked since")
    void reportsEarliestTimestampAsHumanFriendlyTrackedSince() {
        ContractProgressRecord record = new ContractProgressRecord(
                "fp1", HttpVerb.GET, "/orders/{id}", "com.example.OrderController",
                Instant.parse("2026-01-14T09:02:11Z"), Instant.parse("2026-02-20T11:15:44Z"), null,
                Instant.parse("2026-02-20T11:15:44Z"), null);

        String output = render(Map.of("fp1", record));

        assertThat(output)
                .contains("2026-01-14 09:02:11 UTC")
                .doesNotContain("2026-01-14T09:02:11Z");
    }

    @Test
    @DisplayName("reports N/A when no record has any non-null timestamp")
    void reportsNaWhenNoTimestampsPresent() {
        ContractProgressRecord record = new ContractProgressRecord(
                "fp1", HttpVerb.GET, "/orders/{id}", null, null, null, null, null, null);

        String output = render(Map.of("fp1", record));

        assertThat(output).contains("| Tracked since" + System.lineSeparator() + "| N/A");
    }

    @Test
    @DisplayName("counts records declared, implemented, and verified within the last 7 and 30 days")
    void countsRecordsWithinLast7And30Days() {
        Instant recentlyDeclared = Instant.now().minus(Duration.ofDays(2));
        Instant recentlyImplemented = Instant.now().minus(Duration.ofDays(10));
        Instant recentlyVerified = Instant.now().minus(Duration.ofDays(20));
        Instant longAgo = Instant.now().minus(Duration.ofDays(40));
        Map<String, ContractProgressRecord> history = Map.of(
                "fp1", new ContractProgressRecord(
                        "fp1", HttpVerb.GET, "/orders", "com.example.OrderController",
                        recentlyDeclared, recentlyImplemented, recentlyVerified, recentlyDeclared, null),
                "fp2", new ContractProgressRecord(
                        "fp2", HttpVerb.GET, "/orders/{id}", "com.example.OrderController",
                        longAgo, longAgo, longAgo, longAgo, null));

        String output = render(history);

        assertThat(output)
                .contains("| Declared in the last 7 days" + System.lineSeparator() + "| 1")
                .contains("| Declared in the last 30 days" + System.lineSeparator() + "| 1")
                .contains("| Implemented in the last 7 days" + System.lineSeparator() + "| 0")
                .contains("| Implemented in the last 30 days" + System.lineSeparator() + "| 1")
                .contains("| Verified in the last 7 days" + System.lineSeparator() + "| 0")
                .contains("| Verified in the last 30 days" + System.lineSeparator() + "| 1");
    }

    @Test
    @DisplayName("reports zero verified when no record has ever been verified, e.g. Shadow/Mirage history")
    void reportsZeroVerifiedWhenNeverObserved() {
        ContractProgressRecord record = new ContractProgressRecord(
                "fp1", HttpVerb.GET, "/orders", "com.example.OrderController",
                Instant.now(), Instant.now(), null, Instant.now(), null);

        String output = render(Map.of("fp1", record));

        assertThat(output)
                .contains("| Verified in the last 7 days" + System.lineSeparator() + "| 0")
                .contains("| Verified in the last 30 days" + System.lineSeparator() + "| 0");
    }

    @Test
    @DisplayName("counts records with a non-null removedAt as removed")
    void countsRecordsWithNonNullRemovedAtAsRemoved() {
        Map<String, ContractProgressRecord> history = Map.of(
                "fp1", new ContractProgressRecord(
                        "fp1", HttpVerb.GET, "/orders", "com.example.OrderController",
                        Instant.parse("2026-01-01T00:00:00Z"), null, null,
                        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z")),
                "fp2", new ContractProgressRecord(
                        "fp2", HttpVerb.GET, "/orders/{id}", "com.example.OrderController",
                        Instant.parse("2026-01-01T00:00:00Z"), null, null,
                        Instant.parse("2026-01-01T00:00:00Z"), null));

        String output = render(history);

        assertThat(output).contains("| Removed (no longer seen)" + System.lineSeparator() + "| 1");
    }

    private String render(Map<String, ContractProgressRecord> history) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            writer.write(printWriter, history);
        }
        return stringWriter.toString();
    }
}
