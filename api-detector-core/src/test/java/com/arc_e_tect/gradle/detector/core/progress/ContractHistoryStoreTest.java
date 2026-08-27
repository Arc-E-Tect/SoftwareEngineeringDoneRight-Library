package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                Instant.parse("2026-01-15T10:00:00Z"),
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
                Instant.parse("2026-03-01T09:00:00Z"), null, null, null,
                Instant.parse("2026-08-12T07:00:00Z"), null);

        store.save(file, List.of(record));
        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded.get("9e01b4d2f7a63c58").declaringClass()).isNull();
    }

    @Test
    @DisplayName("round-trips a null stubbedAt")
    void roundTripsNullStubbedAt() {
        File file = tempDir.resolve("history.ndjson").toFile();
        ContractProgressRecord record = new ContractProgressRecord(
                "9e01b4d2f7a63c59", HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                Instant.parse("2026-03-01T09:00:00Z"), Instant.parse("2026-03-02T09:00:00Z"), null, null,
                Instant.parse("2026-08-12T07:00:00Z"), null);

        store.save(file, List.of(record));
        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded.get("9e01b4d2f7a63c59").stubbedAt()).isNull();
    }

    @Test
    @DisplayName("round-trips a non-null stubbedAt")
    void roundTripsNonNullStubbedAt() {
        File file = tempDir.resolve("history.ndjson").toFile();
        Instant stubbedAt = Instant.parse("2026-01-15T10:00:00Z");
        ContractProgressRecord record = new ContractProgressRecord(
                "9e01b4d2f7a63c60", HttpVerb.GET, "/orders/{id}", null,
                Instant.parse("2026-01-10T09:00:00Z"), null, stubbedAt, null,
                Instant.parse("2026-08-12T07:00:00Z"), null);

        store.save(file, List.of(record));
        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded.get("9e01b4d2f7a63c60").stubbedAt()).isEqualTo(stubbedAt);
    }

    @Test
    @DisplayName("round-trips a path and declaringClass containing quotes and backslashes")
    void roundTripsQuotesAndBackslashes() {
        File file = tempDir.resolve("history.ndjson").toFile();
        ContractProgressRecord record = new ContractProgressRecord(
                "abc0000000000000", HttpVerb.GET, "/orders/\"weird\"/\\path\\", "com.acme.\"Weird\\Class",
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null,
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
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null);
        ContractProgressRecord first = new ContractProgressRecord(
                "aaaa000000000000", HttpVerb.GET, "/a", null,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null);

        store.save(file, List.of(second, first));

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(1)).contains("\"aaaa000000000000\"");
        assertThat(lines.get(2)).contains("\"bbbb000000000000\"");
    }

    @Test
    @DisplayName("save writes a schema-version marker as the file's first line")
    void saveWritesASchemaVersionMarkerAsTheFilesFirstLine() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();

        store.save(file, List.of());

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(0)).isEqualTo("{\"schemaVersion\":\"1.1.0\"}");
    }

    @Test
    @DisplayName("load tolerates a file with no schema-version marker, exactly like one that has it - "
            + "recomputing the record's fingerprint, since a headerless file predates fingerprint canonicalisation")
    void loadToleratesAFileWithNoSchemaVersionMarker() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String validLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/a\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"stubbedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), validLine + "\n", StandardCharsets.UTF_8);
        String realFingerprint = new EndpointFingerprint().fingerprint(
                new Endpoint(HttpVerb.GET, "/a", null, null, null, 0));

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded).containsOnlyKeys(realFingerprint);
        assertThat(loaded.get(realFingerprint).fingerprint()).isEqualTo(realFingerprint);
    }

    @Test
    @DisplayName("loadLegacy tolerates a leading schema-version marker line")
    void loadLegacyToleratesALeadingSchemaVersionMarker() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String legacyLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/a\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), "{\"schemaVersion\":1}\n" + legacyLine + "\n", StandardCharsets.UTF_8);

        Map<String, ContractProgressRecord> loaded = store.loadLegacy(file);

        assertThat(loaded).containsOnlyKeys("aaaa000000000000");
    }

    @Test
    @DisplayName("skips a malformed line and does not include it in the loaded map, without failing")
    void skipsMalformedLineWithoutFailing() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String validLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/a\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"stubbedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        // A current-version header keeps this test isolated from the fingerprint-migration
        // behaviour covered separately below - it would otherwise recompute "aaaa000000000000"
        // into the record's real fingerprint, unrelated to what this test actually checks.
        Files.writeString(file.toPath(),
                "{\"schemaVersion\":\"1.1.0\"}\nnot valid json at all\n" + validLine + "\n", StandardCharsets.UTF_8);

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded).containsOnlyKeys("aaaa000000000000");
    }

    @Test
    @DisplayName("blank lines in the file are silently skipped")
    void blankLinesAreSilentlySkipped() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String validLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/a\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"stubbedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), "\n" + validLine + "\n\n", StandardCharsets.UTF_8);

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded).hasSize(1);
    }

    @Test
    @DisplayName("load throws LegacyContractHistoryFormatException when the file is in the pre-stubbedAt 9-field format")
    void loadThrowsOnLegacyNineFieldFormat() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String legacyLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/a\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), legacyLine + "\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> store.load(file))
                .isInstanceOf(LegacyContractHistoryFormatException.class)
                .hasMessageContaining(file.toString());
    }

    @Test
    @DisplayName("loadLegacy reads a pre-stubbedAt 9-field file, defaulting stubbedAt to null")
    void loadLegacyReadsNineFieldFormatWithNullStubbedAt() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String legacyLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/a\","
                + "\"declaringClass\":\"com.acme.OrderController\",\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":\"2026-01-02T00:00:00Z\",\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-03T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), legacyLine + "\n", StandardCharsets.UTF_8);

        Map<String, ContractProgressRecord> loaded = store.loadLegacy(file);

        ContractProgressRecord record = loaded.get("aaaa000000000000");
        assertThat(record.declaringClass()).isEqualTo("com.acme.OrderController");
        assertThat(record.implementedAt()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
        assertThat(record.stubbedAt()).isNull();
    }

    @Test
    @DisplayName("loadLegacy returns an empty map when the file does not exist")
    void loadLegacyReturnsEmptyMapWhenFileDoesNotExist() {
        File missingFile = tempDir.resolve("missing.ndjson").toFile();

        assertThat(store.loadLegacy(missingFile)).isEmpty();
    }

    @Test
    @DisplayName("load migrates a legacy bare-integer schema-version marker, recomputing every fingerprint")
    void loadMigratesLegacyBareIntegerSchemaVersionMarker() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String line = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"PUT\",\"path\":\"/customers/{customerId}\","
                + "\"declaringClass\":\"com.acme.CustomerController\",\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":\"2026-01-01T00:00:00Z\",\"stubbedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), "{\"schemaVersion\":1}\n" + line + "\n", StandardCharsets.UTF_8);
        String realFingerprint = new EndpointFingerprint().fingerprint(
                new Endpoint(HttpVerb.PUT, "/customers/{customerId}", null, null, null, 0));

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded).containsOnlyKeys(realFingerprint);
    }

    @Test
    @DisplayName("load merges two records that collide onto the same fingerprint after migration, "
            + "taking the earliest non-null stage timestamp of each")
    void loadMergesCollidingRecordsTakingEarliestStageTimestamps() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        // Both describe PUT /customers/{anything} - only the placeholder's own name differs -
        // so they collide onto the same fingerprint once that stops mattering.
        String declared = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"PUT\",\"path\":\"/customers/{customerId}\","
                + "\"declaringClass\":\"com.acme.CustomerController\",\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":\"2026-01-05T00:00:00Z\",\"stubbedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-05T00:00:00Z\",\"removedAt\":null}";
        String stubbed = "{\"fingerprint\":\"bbbb000000000000\",\"verb\":\"PUT\",\"path\":\"/customers/{id}\","
                + "\"declaringClass\":null,\"declaredAt\":null,"
                + "\"implementedAt\":null,\"stubbedAt\":\"2026-01-02T00:00:00Z\",\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), declared + "\n" + stubbed + "\n", StandardCharsets.UTF_8);
        String realFingerprint = new EndpointFingerprint().fingerprint(
                new Endpoint(HttpVerb.PUT, "/customers/{customerId}", null, null, null, 0));

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded).hasSize(1);
        ContractProgressRecord merged = loaded.get(realFingerprint);
        assertThat(merged.declaringClass()).isEqualTo("com.acme.CustomerController");
        assertThat(merged.declaredAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(merged.implementedAt()).isEqualTo(Instant.parse("2026-01-05T00:00:00Z"));
        assertThat(merged.stubbedAt()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
        assertThat(merged.lastSeenAt()).isEqualTo(Instant.parse("2026-01-10T00:00:00Z"));
    }

    @Test
    @DisplayName("load prefers a colliding record's path from the more authoritative source: declared over stubbed")
    void loadPrefersDeclaredPathOverStubbedPathWhenMergingCollidingRecords() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String declared = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"PUT\",\"path\":\"/customers/{customerId}\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"stubbedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        String stubbed = "{\"fingerprint\":\"bbbb000000000000\",\"verb\":\"PUT\",\"path\":\"/customers/{id}\","
                + "\"declaringClass\":null,\"declaredAt\":null,"
                + "\"implementedAt\":null,\"stubbedAt\":\"2026-01-02T00:00:00Z\",\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-02T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), declared + "\n" + stubbed + "\n", StandardCharsets.UTF_8);
        String realFingerprint = new EndpointFingerprint().fingerprint(
                new Endpoint(HttpVerb.PUT, "/customers/{customerId}", null, null, null, 0));

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded.get(realFingerprint).path()).isEqualTo("/customers/{customerId}");
    }

    @Test
    @DisplayName("load only keeps removedAt on a merged record when both colliding records agree it was removed")
    void loadOnlyKeepsRemovedAtWhenBothCollidingRecordsAgree() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String active = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"PUT\",\"path\":\"/customers/{customerId}\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"stubbedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-05T00:00:00Z\",\"removedAt\":null}";
        String removed = "{\"fingerprint\":\"bbbb000000000000\",\"verb\":\"PUT\",\"path\":\"/customers/{id}\","
                + "\"declaringClass\":null,\"declaredAt\":null,"
                + "\"implementedAt\":null,\"stubbedAt\":\"2026-01-02T00:00:00Z\",\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-03T00:00:00Z\",\"removedAt\":\"2026-01-04T00:00:00Z\"}";
        Files.writeString(file.toPath(), active + "\n" + removed + "\n", StandardCharsets.UTF_8);
        String realFingerprint = new EndpointFingerprint().fingerprint(
                new Endpoint(HttpVerb.PUT, "/customers/{customerId}", null, null, null, 0));

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded.get(realFingerprint).removedAt()).isNull();
    }

    @Test
    @DisplayName("load does not migrate a file already at the current schema version")
    void loadDoesNotMigrateAFileAlreadyAtTheCurrentSchemaVersion() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String line = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"PUT\",\"path\":\"/customers/{customerId}\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"stubbedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(file.toPath(), "{\"schemaVersion\":\"1.1.0\"}\n" + line + "\n", StandardCharsets.UTF_8);

        Map<String, ContractProgressRecord> loaded = store.load(file);

        assertThat(loaded).containsOnlyKeys("aaaa000000000000");
    }

    @Test
    @DisplayName("save appends a migration audit entry recording fromVersion/toVersion/migratedAt "
            + "when the file it overwrites predates the current schema version")
    void saveAppendsMigrationAuditEntryForAStaleFile() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        Files.writeString(file.toPath(), "{\"schemaVersion\":1}\n", StandardCharsets.UTF_8);

        store.save(file, List.of());

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(0))
                .contains("\"schemaVersion\":\"1.1.0\"")
                .contains("\"migrations\":[")
                .contains("\"fromVersion\":\"1.0.0\"")
                .contains("\"toVersion\":\"1.1.0\"")
                .containsPattern("\"migratedAt\":\"[^\"]+\"");
    }

    @Test
    @DisplayName("save does not append a migration audit entry for a brand-new file")
    void saveDoesNotAppendMigrationAuditEntryForBrandNewFile() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();

        store.save(file, List.of());

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(0)).isEqualTo("{\"schemaVersion\":\"1.1.0\"}").doesNotContain("migrations");
    }

    @Test
    @DisplayName("save carries an existing migration audit trail forward unchanged when the file is already current")
    void saveCarriesExistingMigrationAuditTrailForward() throws IOException {
        File file = tempDir.resolve("history.ndjson").toFile();
        String existingHeader = "{\"schemaVersion\":\"1.1.0\",\"migrations\":"
                + "[{\"fromVersion\":\"1.0.0\",\"toVersion\":\"1.1.0\",\"migratedAt\":\"2026-01-01T00:00:00Z\"}]}";
        Files.writeString(file.toPath(), existingHeader + "\n", StandardCharsets.UTF_8);

        store.save(file, List.of());

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines.get(0)).isEqualTo(existingHeader);
    }
}
