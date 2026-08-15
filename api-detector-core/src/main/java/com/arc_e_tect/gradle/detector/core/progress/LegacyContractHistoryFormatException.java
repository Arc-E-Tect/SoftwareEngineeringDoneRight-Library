package com.arc_e_tect.gradle.detector.core.progress;

import java.io.File;

/**
 * Thrown by {@link ContractHistoryStore#load(File)} when {@code file} is written in the pre-
 * {@code stubbedAt} 9-field NDJSON format rather than the current 10-field format.
 *
 * <p>{@link ContractHistoryStore#load(File)} refuses to load a legacy-format file automatically,
 * rather than either silently discarding its content as malformed or silently treating its
 * {@code implementedAt} values as if they meant what that field means today. Under the current
 * format, {@code implementedAt} means "backed by a real {@code @RestController}" - but in a
 * legacy file, the same field may equally have come from a WireMock stub, since the two were not
 * distinguished before {@code stubbedAt} existed. Only a migration that re-scans the project's
 * current controller and stub sources can tell the two apart; see
 * {@link ContractHistoryStore#loadLegacy(File)} for the escape hatch such a migration uses to
 * read the old format.</p>
 */
public class LegacyContractHistoryFormatException extends RuntimeException {

    private final File file;

    /**
     * Creates a new {@code LegacyContractHistoryFormatException} for {@code file}.
     *
     * @param file the legacy-format contract history file that could not be loaded
     */
    public LegacyContractHistoryFormatException(File file) {
        super("contract history file " + file + " is in the pre-stubbedAt 9-field format and must be "
                + "migrated to the current 10-field format before it can be loaded");
        this.file = file;
    }

    /**
     * The legacy-format file that could not be loaded.
     *
     * @return the file
     */
    public File file() {
        return file;
    }
}
