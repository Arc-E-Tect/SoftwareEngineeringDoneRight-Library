package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The WireMock emitter's options, as a project gives them: every name and value checked, so that
 * a mistake fails generation rather than being ignored.
 *
 * @param format    how the stubs are written
 * @param priority  the priority of every exact stub; the fallbacks come after it
 * @param fallbacks whether the fallback stubs are written
 */
record Options(Format format, int priority, boolean fallbacks) {

    /** The option that chooses how the stubs are written. */
    static final String FORMAT = "format";

    /** The option that sets the exact stubs' priority. */
    static final String PRIORITY = "priority";

    /** The option that switches the fallback stubs on or off. */
    static final String FALLBACKS = "fallbacks";

    /** Every option, in the order the messages list them. */
    static final List<String> NAMES = List.of(FORMAT, PRIORITY, FALLBACKS);

    /** The priority the exact stubs have unless the option sets another: WireMock's highest. */
    static final int DEFAULT_PRIORITY = 1;

    /** How the stubs are written. */
    enum Format {

        /** WireMock mapping files, on no source set: the default. */
        FILES("files"),

        /** Java building the same mappings, compiled in the emitter's source sets. */
        JAVA("java");

        final String value;

        Format(String value) {
            this.value = value;
        }
    }

    /**
     * The options a project gave, or their defaults.
     *
     * @param options the options, by name
     * @return the options
     * @throws IllegalArgumentException for an option the emitter does not have, or a value it
     *                                  does not take
     */
    static Options of(Map<String, String> options) {
        for (String name : options.keySet()) {
            if (!NAMES.contains(name)) {
                throw new IllegalArgumentException("The WireMock emitter has no option '" + name
                        + "'; its options are " + String.join(", ", NAMES.stream().map(n -> "'" + n + "'").toList())
                        + ".");
            }
        }
        return new Options(format(options.get(FORMAT)), priority(options.get(PRIORITY)),
                fallbacks(options.get(FALLBACKS)));
    }

    private static Format format(String value) {
        if (value == null) return Format.FILES;
        for (Format format : Format.values()) {
            if (format.value.equals(value.trim().toLowerCase(Locale.ROOT))) return format;
        }
        throw new IllegalArgumentException("The WireMock emitter's option '" + FORMAT + "' is '" + value
                + "'; it must be 'files', for WireMock mapping files, or 'java', for Java building the same "
                + "mappings.");
    }

    private static int priority(String value) {
        if (value == null) return DEFAULT_PRIORITY;
        try {
            int priority = Integer.parseInt(value.trim());
            if (priority >= 1) return priority;
        } catch (NumberFormatException e) {
            // Reported below, with what is expected.
        }
        throw new IllegalArgumentException("The WireMock emitter's option '" + PRIORITY + "' is '" + value
                + "'; it must be a whole number of 1 or more, WireMock's highest priority being 1.");
    }

    private static boolean fallbacks(String value) {
        if (value == null) return true;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("The WireMock emitter's option '" + FALLBACKS + "' is '"
                    + value + "'; it must be 'true', to write the fallback stubs, or 'false'.");
        };
    }
}
