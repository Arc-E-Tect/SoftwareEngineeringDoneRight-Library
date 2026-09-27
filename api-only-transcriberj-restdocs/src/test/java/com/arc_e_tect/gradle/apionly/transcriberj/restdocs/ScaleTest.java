package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.13 and T20.12: the largest fixture's whole suite -- the keyword corpus, with a case of every
 * kind it derives: generated, compiled and run against the faithful server -- stays within its
 * budget, so that a real contract's dozens of cases per operation do not make a build unusable.
 * The README states the budget, and the time measured.
 */
@DisplayName("T14.13, T20.12 Scale")
class ScaleTest {

    /** The budget for generating, compiling and running the largest fixture's suite. */
    static final Duration BUDGET = Duration.ofSeconds(60);

    @TempDir
    Path directory;

    @Test
    void theLargestSuiteCompletesWithinItsBudget() {
        Fixtures.Contract largest = Fixtures.KEYWORDS;
        long start = System.nanoTime();
        GeneratedSuite suite = GeneratedSuite.of(largest, directory);
        long compiled = System.nanoTime();
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }
        long end = System.nanoTime();

        Duration total = Duration.ofNanos(end - start);
        System.out.printf("T20.12 %s: %d tests; generate and compile %d ms, run %d ms, total %d ms%n",
                largest.name(), run.outcomes().size(), (compiled - start) / 1_000_000, (end - compiled) / 1_000_000,
                total.toMillis());
        for (Fixtures.Contract other : Fixtures.ALL) {
            assertThat(Suites.of(other).cases.size()).as(other.name()).isLessThanOrEqualTo(suite.cases.size());
        }
        assertThat(run.failed()).isEmpty();
        assertThat(total).isLessThan(BUDGET);
    }
}
