package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.13: the user-account contract's whole suite -- generated, compiled and run against the
 * validating server -- stays within its budget, so that a real contract's dozens of cases per
 * operation do not make a build unusable. The README states the budget.
 */
@DisplayName("T14.13 Scale")
class ScaleTest {

    /** The budget for generating, compiling and running the user-account suite. */
    static final Duration BUDGET = Duration.ofSeconds(60);

    @TempDir
    Path directory;

    @Test
    void theUserAccountSuiteCompletesWithinItsBudget() {
        long start = System.nanoTime();
        GeneratedSuite suite = GeneratedSuite.of(Fixtures.USER_ACCOUNT, directory);
        long compiled = System.nanoTime();
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }
        long end = System.nanoTime();

        Duration total = Duration.ofNanos(end - start);
        System.out.printf("T14.13 user-account: %d tests; generate and compile %d ms, run %d ms, total %d ms%n",
                run.outcomes().size(), (compiled - start) / 1_000_000, (end - compiled) / 1_000_000,
                total.toMillis());
        assertThat(run.failed()).isEmpty();
        assertThat(total).isLessThan(BUDGET);
    }
}
