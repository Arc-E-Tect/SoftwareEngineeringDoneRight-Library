package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContractHistoryStore")
class ContractHistoryStoreTest {

    @TempDir
    Path tempDir;

    private final ContractHistoryStore store = new ContractHistoryStore();

    @Test
    @DisplayName("load returns an empty map when the file does not exist")
    void loadReturnsEmptyMapWhenFileDoesNotExist() {
        File missingFile = tempDir.resolve("missing.ndjson").toFile();

        assertThat(store.load(missingFile)).isEmpty();
    }

    @Test
    @DisplayName("round-trips a record's every field through save then load")
    void roundTripsRecordFieldsThroughSaveThenLoad() {
        File file = tempDir.resolve("history.ndjson").toFile();
        ContractProgressRecord record = new ContractProgressRecord(
                "3c7a1f0e9b224dd1", HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                Instant.parse("2026-01-10T09:00:00Z"),
                Instant.parse("2026-01-20T14:30:00Z"),
                Instant.parse("2026-02-05T08:15:00Z"),
                Instant.parse("2026-08-12T07:00:00Z"),
                null);

        store.save(file, List.of(record));
        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded.get("3c7a1f0e9b224dd1")).isEqualTo(record);
    }

    @Test
    @DisplayName("round-trips a null declaringClass")
    void roundTripsNullDeclaringClass() {
        File file = tempDir.resolve("history.ndjson").toFile();
        ContractProgressRecord record = new ContractProgressRecord(
                "9e01b4d2f7a63c58", HttpVerb.DELETE, "/orders/{id}", null,
                Instant.parse("2026-03-01T09:00:00Z"), null, null,
                Instant.parse("2026-08-12T07:00:00Z"), null);

        store.save(file, List.of(record));
        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded.get("9e01b4d2f7a63c58").declaringClass()).isNull();
    }

    @Test
    @DisplayName("round-trips a path and declaringClass containing quotes and backslashes")
    void roundTripsQuotesAndBackslashes() {
        File file = tempDir.resolve("history.ndjson").toFile();
        ContractProgressRecord record = new ContractProgressRecord(
                "abc0000000000000", HttpVerb.GET, "/orders/\"weird\"/\\path\\", "com.acme.\"Weird\\Class",
                Instant.parse("2026-01-01T00:00:00Z"), null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null);

        store.save(file, List.of(record));
        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded.get("abc0000000000000")).isEqualTo(record);
    }

    @Test
    @DisplayName("writes records sorted by fingerprint regardless of input order")
    void writesRecordsSortedByFingerprintRegardlessOfInputOrder() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        ContractProgressRecord second = new ContractProgressRecord(
                "bbbb000000000000", HttpVerb.GET, "/b", null,
                Instant.parse("2026-01-01T00:00:00Z"), null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null);
        ContractProgressRecord first = new ContractProgressRecord(
                "aaaa000000000000", HttpVerb.GET, "/a", null,
                Instant.parse("2026-01-01T00:00:00Z"), null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null);

        store.save(file, List.of(second, first));

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(0)).contains("\"aaaa000000000000\"");
        assertThat(lines.get(1)).contains("\"bbbb000000000000\"");
    }

    @Test
    @DisplayName("skips a malformed line and does not include it in the loaded map, without failing")
    void skipsMalformedLineWithoutFailing() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String validLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/a\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), "not valid json at all\n" + validLine + "\n", StandardCharsets.UTF_8);

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded).containsOnlyKeys("aaaa000000000000");
    }

    @Test
    @DisplayName("blank lines in the file are silently skipped")
    void blankLinesAreSilentlySkipped() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String validLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/a\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), "\n" + validLine + "\n\n", StandardCharsets.UTF_8);

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded).hasSize(1);
    }
}
