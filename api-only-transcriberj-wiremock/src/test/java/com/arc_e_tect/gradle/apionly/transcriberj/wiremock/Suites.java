package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Each fixture contract's suite, generated and compiled once for every test class that runs it:
 * compiling is what takes the time.
 */
final class Suites {

    private static final Map<Fixtures.Contract, GeneratedSuite> SUITES = new HashMap<>();
    private static Path directory;

    private Suites() {
    }

    /** A fixture contract's suite, its stubs with the default options. */
    static synchronized GeneratedSuite of(Fixtures.Contract contract) {
        return SUITES.computeIfAbsent(contract, c -> GeneratedSuite.of(c, Map.of(),
                directory().resolve(c.name() + (c.strict() ? "" : "-lenient") + "-" + String.join("-", c.formats()))));
    }

    /** A new directory for a test's own generation. */
    static synchronized Path directory(String name) {
        try {
            return Files.createTempDirectory(directory(), name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path directory() {
        if (directory == null) {
            try {
                directory = Files.createTempDirectory("wiremock-contract-suites");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return directory;
    }
}
