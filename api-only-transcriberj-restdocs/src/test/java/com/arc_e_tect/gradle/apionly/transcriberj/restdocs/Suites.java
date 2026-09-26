package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

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

    private static final Map<String, GeneratedSuite> SUITES = new HashMap<>();
    private static Path directory;

    private Suites() {
    }

    /** A fixture contract's suite. */
    static synchronized GeneratedSuite of(Fixtures.Contract contract) {
        return SUITES.computeIfAbsent(contract.name(), name -> GeneratedSuite.of(contract, directory().resolve(name)));
    }

    private static Path directory() {
        if (directory == null) {
            try {
                directory = Files.createTempDirectory("restdocs-invalid-request-suites");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return directory;
    }
}
