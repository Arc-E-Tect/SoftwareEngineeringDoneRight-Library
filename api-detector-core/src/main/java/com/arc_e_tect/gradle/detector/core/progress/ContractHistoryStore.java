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
 * {@link #loadLegacy(File)} instead to read its content.</p>
 *
 * <p>{@link #save(File, Collection)} always writes a {@code {"schemaVersion":N}} marker as the
 * file's own first line, giving a future format change something to check against and a clean
 * place to branch reading logic on. Purely additive: a file written before this existed simply has
 * no such line, and both {@link #load(File)} and {@link #loadLegacy(File)} tolerate that by
 * treating a missing marker exactly like a present one - the marker is never required on read.</p>
 */
public class ContractHistoryStore {

    private static final Logger LOGGER = Logger.getLogger(ContractHistoryStore.class.getName());

    /**
     * The current schema version, written as the file's own first line by every {@link #save}. See
     * this class's own javadoc for why this is purely additive and never required on read.
     */
    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Pattern SCHEMA_VERSION_LINE = Pattern.compile("^\\{\"schemaVersion\":(\\d+)\\}$");

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
     * Loads the contract progress history from {@code file}.
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

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (i == 0 && SCHEMA_VERSION_LINE.matcher(line).matches()) {
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

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (i == 0 && SCHEMA_VERSION_LINE.matcher(line).matches()) {
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
     * {@link ContractProgressRecord#fingerprint()}, overwriting any existing content.
     *
     * @param file    the NDJSON history file to write
     * @param records the records to persist
     */
    public void save(File file, Collection<ContractProgressRecord> records) {
        List<ContractProgressRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(ContractProgressRecord::fingerprint));

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("{\"schemaVersion\":" + CURRENT_SCHEMA_VERSION + "}");
            for (ContractProgressRecord record : sorted) {
                writer.println(toJson(record));
            }
        } catch (IOException e) {
            throw new IllegalStateException("apiDetectorCore: could not write contract history file: " + file, e);
        }
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
}
