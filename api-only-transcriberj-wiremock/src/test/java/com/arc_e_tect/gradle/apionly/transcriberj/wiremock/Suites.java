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

    private static final Map<String, GeneratedSuite> SUITES = new HashMap<>();
    private static Path directory;

    private Suites() {
    }

    /** A fixture contract's suite, its stubs at the default priority. */
    static synchronized GeneratedSuite of(Fixtures.Contract contract) {
        return SUITES.computeIfAbsent(contract.name(),
                name -> GeneratedSuite.of(contract, null, directory().resolve(name)));
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
                directory = Files.createTempDirectory("wiremock-invalid-request-suites");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return directory;
    }
}
