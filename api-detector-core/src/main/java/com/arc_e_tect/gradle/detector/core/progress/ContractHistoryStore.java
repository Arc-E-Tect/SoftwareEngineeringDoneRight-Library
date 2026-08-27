package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes {@link ContractProgressRecord}s as newline-delimited JSON (NDJSON), one record
 * per line, sorted by {@link ContractProgressRecord#fingerprint()} so that git diffs of the
 * persisted file are minimal and stable regardless of how endpoints are reordered.
 *
 * <p>The schema is fixed and flat, so the serializer/parser is hand-rolled (no JSON library
 * dependency) - only {@link ContractProgressRecord#declaringClass()} and
 * {@link ContractProgressRecord#path()} are free text and need escaping. Warnings are logged via
 * {@code java.util.logging}, the JDK's own built-in facility, rather than adding a logging
 * dependency this plain library otherwise has no need for.</p>
 *
 * <p>{@link #load(File)} recognises the pre-{@code stubbedAt} 9-field format left behind by
 * versions of this library before {@code stubbedAt} existed, and refuses to load it - see
 * {@link LegacyContractHistoryFormatException}. A caller migrating such a file forward should use
 * {@link #loadLegacy(File)} instead to read its content. This is a genuinely <em>structural</em>
 * change - the 9-field format has no {@code stubbedAt} to read at all, so recovering it correctly
 * requires re-scanning live source, which only a calling task (not this plain-data class) can do -
 * and so stays an explicit, manually-invoked, reviewable migration.</p>
 *
 * <p>{@link #save(File, Collection)} always writes a {@code {"schemaVersion":"x.y.z"}} marker,
 * following semver, as the file's own first line - purely additive: a file written before this
 * existed simply has no such line (equivalent to schema version {@value #BASELINE_SCHEMA_VERSION}),
 * and every read method tolerates that by treating a missing/unrecognised marker exactly like a
 * present {@value #BASELINE_SCHEMA_VERSION} one - the marker is never required on read.</p>
 *
 * <p>Unlike the 9-to-10-field change, a {@code schemaVersion} bump for a change that only alters
 * how a value already present in every record is <em>derived</em> - e.g.
 * {@link EndpointFingerprint} canonicalising a path-variable placeholder's name before hashing, so
 * {@code "{customerId}"} and {@code "{id}"} now fingerprint identically - never needs external
 * re-scanning, and so is migrated automatically by {@link #load(File)} rather than requiring a
 * separate task: every record's {@link ContractProgressRecord#fingerprint()} is recomputed from
 * its own {@link ContractProgressRecord#verb()}/{@link ContractProgressRecord#path()}, and any two
 * records that land on the same recomputed fingerprint - i.e. were previously persisted
 * separately only because of a naming difference the new algorithm no longer distinguishes - are
 * merged into one, keeping the earliest non-null stage timestamp of each and the more recent of
 * the two {@code lastSeenAt}. The next {@link #save(File, Collection)} of that file persists the
 * upgrade, appending a {@link SchemaMigration} entry to the header's own audit trail recording the
 * {@code fromVersion}/{@code toVersion}/{@code migratedAt} - the full trail is carried forward on
 * every save, not just the most recent entry.</p>
 */
public class ContractHistoryStore {

    private static final Logger LOGGER = Logger.getLogger(ContractHistoryStore.class.getName());

    /**
     * The schema version every contract history file predates having an explicit marker for, and
     * that a present-but-unrecognised/legacy bare-integer marker (e.g. {@code {"schemaVersion":1}},
     * this class's own previous marker shape) is treated as being at.
     */
    private static final String BASELINE_SCHEMA_VERSION = "1.0.0";

    /**
     * The current schema version, written as part of the file's own first line by every
     * {@link #save}. See this class's own javadoc for what is - and is not - migrated
     * automatically as this advances.
     */
    private static final String CURRENT_SCHEMA_VERSION = "1.1.0";

    private static final Pattern LEGACY_SCHEMA_VERSION_LINE = Pattern.compile("^\\{\"schemaVersion\":(\\d+)\\}$");
    private static final Pattern SCHEMA_VERSION_LINE = Pattern.compile(
            "^\\{\"schemaVersion\":\"([^\"]+)\"(?:,\"migrations\":\\[(.*)])?}$");
    private static final Pattern MIGRATION_ENTRY = Pattern.compile(
            "\\{\"fromVersion\":\"([^\"]+)\",\"toVersion\":\"([^\"]+)\",\"migratedAt\":\"([^\"]+)\"}");

    private static final String STRING_FIELD = "\"((?:[^\"\\\\]|\\\\.)*)\"";
    private static final String NULLABLE_STRING_FIELD = "(?:null|" + STRING_FIELD + ")";
    private static final String INSTANT_FIELD = "(null|\"[^\"]*\")";
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\{"
            + "\"fingerprint\":" + STRING_FIELD + ","
            + "\"verb\":" + STRING_FIELD + ","
            + "\"path\":" + STRING_FIELD + ","
            + "\"declaringClass\":" + NULLABLE_STRING_FIELD + ","
            + "\"declaredAt\":" + INSTANT_FIELD + ","
            + "\"implementedAt\":" + INSTANT_FIELD + ","
            + "\"stubbedAt\":" + INSTANT_FIELD + ","
            + "\"verifiedAt\":" + INSTANT_FIELD + ","
            + "\"lastSeenAt\":" + INSTANT_FIELD + ","
            + "\"removedAt\":" + INSTANT_FIELD
            + "\\}$");

    /**
     * Matches the pre-{@code stubbedAt} 9-field line shape, used only to distinguish a genuinely
     * legacy-format file (see {@link LegacyContractHistoryFormatException}) from a line that's
     * simply malformed.
     */
    private static final Pattern LEGACY_LINE_PATTERN = Pattern.compile(
            "^\\{"
            + "\"fingerprint\":" + STRING_FIELD + ","
            + "\"verb\":" + STRING_FIELD + ","
            + "\"path\":" + STRING_FIELD + ","
            + "\"declaringClass\":" + NULLABLE_STRING_FIELD + ","
            + "\"declaredAt\":" + INSTANT_FIELD + ","
            + "\"implementedAt\":" + INSTANT_FIELD + ","
            + "\"verifiedAt\":" + INSTANT_FIELD + ","
            + "\"lastSeenAt\":" + INSTANT_FIELD + ","
            + "\"removedAt\":" + INSTANT_FIELD
            + "\\}$");

    /** Creates a new {@code ContractHistoryStore}. */
    public ContractHistoryStore() {}

    /**
     * Loads the contract progress history from {@code file}, automatically migrating it in memory
     * to {@value #CURRENT_SCHEMA_VERSION} when it predates that - see this class's own javadoc.
     *
     * @param file the NDJSON history file; need not exist
     * @return the records keyed by fingerprint; empty when {@code file} doesn't exist. A line that
     *         fails to parse is skipped with a {@code WARN}-level log message identifying the line
     *         number - it never fails the build.
     * @throws LegacyContractHistoryFormatException if {@code file} is written in the pre-
     *         {@code stubbedAt} 9-field format; use {@link #loadLegacy(File)} to read it instead
     */
    public Map<String, ContractProgressRecord> load(File file) {
        Map<String, ContractProgressRecord> records = new LinkedHashMap<>();
        if (!file.isFile()) {
            return records;
        }

        List<String> lines = readLines(file);
        Header header = parseHeader(lines);

        for (int i = header.headerLinePresent() ? 1 : 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            ContractProgressRecord record = parseLine(line);
            if (record != null) {
                records.put(record.fingerprint(), record);
                continue;
            }
            if (LEGACY_LINE_PATTERN.matcher(line).matches()) {
                throw new LegacyContractHistoryFormatException(file);
            }
            LOGGER.log(Level.WARNING,
                    "apiDetectorCore: skipping malformed contract history line {0} in {1}",
                    new Object[] {i + 1, file});
        }

        if (compareVersions(header.schemaVersion(), CURRENT_SCHEMA_VERSION) < 0) {
            LOGGER.log(Level.WARNING,
                    "apiDetectorCore: migrating {0} from format {1} to {2} - every endpoint fingerprint is "
                            + "recomputed from its own verb/path, merging any records that were only ever "
                            + "persisted separately because of a path-variable naming difference the new "
                            + "algorithm no longer distinguishes. The upgrade is saved back the next time "
                            + "this file is written.",
                    new Object[] {file, header.schemaVersion(), CURRENT_SCHEMA_VERSION});
            records = migrateFingerprints(records);
        }
        return records;
    }

    /**
     * Loads {@code file} as the pre-{@code stubbedAt} 9-field NDJSON format, for use by a
     * migration task upgrading it to the current 10-field format. Every parsed record's
     * {@link ContractProgressRecord#stubbedAt()} is {@code null}, since the legacy format has no
     * such field to read it from.
     *
     * @param file the legacy-format NDJSON history file; need not exist
     * @return the records keyed by fingerprint; empty when {@code file} doesn't exist. A line that
     *         fails to parse is skipped with a {@code WARN}-level log message identifying the line
     *         number - it never fails the build.
     */
    public Map<String, ContractProgressRecord> loadLegacy(File file) {
        Map<String, ContractProgressRecord> records = new LinkedHashMap<>();
        if (!file.isFile()) {
            return records;
        }

        List<String> lines = readLines(file);
        Header header = parseHeader(lines);

        for (int i = header.headerLinePresent() ? 1 : 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            ContractProgressRecord record = parseLegacyLine(line);
            if (record == null) {
                LOGGER.log(Level.WARNING,
                        "apiDetectorCore: skipping malformed legacy contract history line {0} in {1}",
                        new Object[] {i + 1, file});
                continue;
            }
            records.put(record.fingerprint(), record);
        }
        return records;
    }

    /**
     * Writes {@code records} to {@code file} as NDJSON, sorted by
     * {@link ContractProgressRecord#fingerprint()}, overwriting any existing content. The header's
     * migration audit trail from any existing {@code file} is carried forward, with a new
     * {@link SchemaMigration} entry appended whenever {@code file}'s previous {@code schemaVersion}
     * predated {@value #CURRENT_SCHEMA_VERSION} - see this class's own javadoc.
     *
     * @param file    the NDJSON history file to write
     * @param records the records to persist
     */
    public void save(File file, Collection<ContractProgressRecord> records) {
        List<ContractProgressRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(ContractProgressRecord::fingerprint));

        Header previous = file.isFile()
                ? parseHeader(readLines(file)) : new Header(CURRENT_SCHEMA_VERSION, List.of(), false);
        List<SchemaMigration> migrations = new ArrayList<>(previous.migrations());
        if (compareVersions(previous.schemaVersion(), CURRENT_SCHEMA_VERSION) < 0) {
            migrations.add(new SchemaMigration(previous.schemaVersion(), CURRENT_SCHEMA_VERSION, Instant.now()));
        }

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println(headerJson(migrations));
            for (ContractProgressRecord record : sorted) {
                writer.println(toJson(record));
            }
        } catch (IOException e) {
            throw new IllegalStateException("apiDetectorCore: could not write contract history file: " + file, e);
        }
    }

    /**
     * Recomputes every record's fingerprint from its own verb/path via a fresh
     * {@link EndpointFingerprint}, merging any two (or more) records that land on the same
     * recomputed fingerprint via {@link #mergeColliding(ContractProgressRecord, ContractProgressRecord)}.
     */
    private Map<String, ContractProgressRecord> migrateFingerprints(Map<String, ContractProgressRecord> records) {
        EndpointFingerprint fingerprinter = new EndpointFingerprint();
        Map<String, ContractProgressRecord> migrated = new LinkedHashMap<>();
        for (ContractProgressRecord record : records.values()) {
            String fingerprint = fingerprinter.fingerprint(record);
            ContractProgressRecord recomputed = withFingerprint(record, fingerprint);
            migrated.merge(fingerprint, recomputed, this::mergeColliding);
        }
        return migrated;
    }

    private ContractProgressRecord withFingerprint(ContractProgressRecord record, String fingerprint) {
        return new ContractProgressRecord(fingerprint, record.verb(), record.path(), record.declaringClass(),
                record.declaredAt(), record.implementedAt(), record.stubbedAt(), record.verifiedAt(),
                record.lastSeenAt(), record.removedAt());
    }

    /**
     * Merges two records that collided onto the same recomputed fingerprint - i.e. two records
     * only ever persisted separately because of a path-variable naming difference the current
     * {@link EndpointFingerprint} algorithm no longer distinguishes - taking the earliest non-null
     * timestamp of each lifecycle stage (each is a genuine "first observed" moment, so the earlier
     * of two independent observations is the more accurate one), the more recent of the two
     * {@code lastSeenAt} (the opposite - the latest observation is the most accurate one),
     * {@code removedAt} only when <em>both</em> records agree the endpoint was removed (evidence
     * from either alone that it's still around should win), a non-null {@code declaringClass} over
     * a null one, and a path from whichever record carries the most authoritative evidence for it
     * (declared, then implemented, then verified, then stubbed).
     */
    private ContractProgressRecord mergeColliding(ContractProgressRecord a, ContractProgressRecord b) {
        Instant removedAt = a.removedAt() != null && b.removedAt() != null
                ? earliest(a.removedAt(), b.removedAt()) : null;
        return new ContractProgressRecord(
                a.fingerprint(),
                a.verb(),
                choosePath(a, b),
                a.declaringClass() != null ? a.declaringClass() : b.declaringClass(),
                earliest(a.declaredAt(), b.declaredAt()),
                earliest(a.implementedAt(), b.implementedAt()),
                earliest(a.stubbedAt(), b.stubbedAt()),
                earliest(a.verifiedAt(), b.verifiedAt()),
                latest(a.lastSeenAt(), b.lastSeenAt()),
                removedAt);
    }

    private String choosePath(ContractProgressRecord a, ContractProgressRecord b) {
        if (a.declaredAt() != null) {
            return a.path();
        }
        if (b.declaredAt() != null) {
            return b.path();
        }
        if (a.implementedAt() != null) {
            return a.path();
        }
        if (b.implementedAt() != null) {
            return b.path();
        }
        if (a.verifiedAt() != null) {
            return a.path();
        }
        if (b.verifiedAt() != null) {
            return b.path();
        }
        if (a.stubbedAt() != null) {
            return a.path();
        }
        if (b.stubbedAt() != null) {
            return b.path();
        }
        return a.path();
    }

    private Instant earliest(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }

    private Instant latest(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    /**
     * Compares two {@code major.minor.patch} version strings numerically - a non-numeric or
     * missing component is treated as {@code 0}, tolerant of a malformed value rather than
     * throwing, consistent with this class's general "never fail the build over the history
     * file's own content" posture.
     *
     * @return negative if {@code a} < {@code b}, positive if {@code a} > {@code b}, zero if equal
     */
    private int compareVersions(String a, String b) {
        int[] partsA = versionParts(a);
        int[] partsB = versionParts(b);
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(partsA[i], partsB[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private int[] versionParts(String version) {
        String[] segments = version.split("\\.");
        int[] parts = new int[3];
        for (int i = 0; i < 3 && i < segments.length; i++) {
            try {
                parts[i] = Integer.parseInt(segments[i]);
            } catch (NumberFormatException e) {
                parts[i] = 0;
            }
        }
        return parts;
    }

    /**
     * Parses {@code lines}' own first line as a header, tolerating every prior header shape this
     * class has ever written - the current {@code {"schemaVersion":"x.y.z","migrations":[...]}},
     * the legacy bare-integer {@code {"schemaVersion":1}}, and no header line at all - never
     * requiring one, per this class's own javadoc.
     */
    private Header parseHeader(List<String> lines) {
        if (lines.isEmpty()) {
            return new Header(BASELINE_SCHEMA_VERSION, List.of(), false);
        }
        String firstLine = lines.get(0);
        if (LEGACY_SCHEMA_VERSION_LINE.matcher(firstLine).matches()) {
            return new Header(BASELINE_SCHEMA_VERSION, List.of(), true);
        }
        Matcher matcher = SCHEMA_VERSION_LINE.matcher(firstLine);
        if (matcher.matches()) {
            return new Header(matcher.group(1), parseMigrations(matcher.group(2)), true);
        }
        return new Header(BASELINE_SCHEMA_VERSION, List.of(), false);
    }

    private List<SchemaMigration> parseMigrations(String migrationsArrayContent) {
        if (migrationsArrayContent == null || migrationsArrayContent.isBlank()) {
            return List.of();
        }
        List<SchemaMigration> migrations = new ArrayList<>();
        Matcher matcher = MIGRATION_ENTRY.matcher(migrationsArrayContent);
        while (matcher.find()) {
            try {
                migrations.add(new SchemaMigration(
                        matcher.group(1), matcher.group(2), Instant.parse(matcher.group(3))));
            } catch (DateTimeParseException e) {
                // A malformed migration entry only loses its own audit-trail visibility - never
                // the file's actual records, which this method has no part in parsing.
            }
        }
        return migrations;
    }

    private String headerJson(List<SchemaMigration> migrations) {
        StringBuilder json = new StringBuilder("{\"schemaVersion\":\"").append(CURRENT_SCHEMA_VERSION).append('"');
        if (!migrations.isEmpty()) {
            json.append(",\"migrations\":[");
            for (int i = 0; i < migrations.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                SchemaMigration migration = migrations.get(i);
                json.append("{\"fromVersion\":\"").append(migration.fromVersion()).append("\",")
                        .append("\"toVersion\":\"").append(migration.toVersion()).append("\",")
                        .append("\"migratedAt\":\"").append(migration.migratedAt()).append("\"}");
            }
            json.append(']');
        }
        return json.append('}').toString();
    }

    private List<String> readLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("apiDetectorCore: could not read contract history file: " + file, e);
        }
    }

    private String toJson(ContractProgressRecord record) {
        return "{"
                + "\"fingerprint\":\"" + record.fingerprint() + "\","
                + "\"verb\":\"" + record.verb().name() + "\","
                + "\"path\":\"" + escape(record.path()) + "\","
                + "\"declaringClass\":" + nullableStringJson(record.declaringClass()) + ","
                + "\"declaredAt\":" + instantJson(record.declaredAt()) + ","
                + "\"implementedAt\":" + instantJson(record.implementedAt()) + ","
                + "\"stubbedAt\":" + instantJson(record.stubbedAt()) + ","
                + "\"verifiedAt\":" + instantJson(record.verifiedAt()) + ","
                + "\"lastSeenAt\":" + instantJson(record.lastSeenAt()) + ","
                + "\"removedAt\":" + instantJson(record.removedAt())
                + "}";
    }

    private String nullableStringJson(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private String instantJson(Instant instant) {
        return instant == null ? "null" : "\"" + instant + "\"";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                i++;
                result.append(value.charAt(i));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private ContractProgressRecord parseLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new ContractProgressRecord(
                    matcher.group(1),
                    HttpVerb.valueOf(matcher.group(2)),
                    unescape(matcher.group(3)),
                    matcher.group(4) == null ? null : unescape(matcher.group(4)),
                    parseInstant(matcher.group(5)),
                    parseInstant(matcher.group(6)),
                    parseInstant(matcher.group(7)),
                    parseInstant(matcher.group(8)),
                    parseInstant(matcher.group(9)),
                    parseInstant(matcher.group(10)));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }

    private ContractProgressRecord parseLegacyLine(String line) {
        Matcher matcher = LEGACY_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new ContractProgressRecord(
                    matcher.group(1),
                    HttpVerb.valueOf(matcher.group(2)),
                    unescape(matcher.group(3)),
                    matcher.group(4) == null ? null : unescape(matcher.group(4)),
                    parseInstant(matcher.group(5)),
                    parseInstant(matcher.group(6)),
                    null,
                    parseInstant(matcher.group(7)),
                    parseInstant(matcher.group(8)),
                    parseInstant(matcher.group(9)));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }

    private Instant parseInstant(String jsonValue) {
        return "null".equals(jsonValue) ? null : Instant.parse(jsonValue.substring(1, jsonValue.length() - 1));
    }

    /**
     * A history file's own first-line header, tolerantly parsed - see {@link #parseHeader(List)}.
     *
     * @param schemaVersion    the file's schema version; {@value #BASELINE_SCHEMA_VERSION} when no
     *                         recognisable header is present
     * @param migrations       the file's own persisted migration audit trail; empty when none has
     *                         ever been recorded
     * @param headerLinePresent whether {@code lines}' first line was actually consumed as a
     *                         header - {@code false} means it's a data line instead, and the
     *                         caller must not skip it
     */
    private record Header(String schemaVersion, List<SchemaMigration> migrations, boolean headerLinePresent) {
    }
}
