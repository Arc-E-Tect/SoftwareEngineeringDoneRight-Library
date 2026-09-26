package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.12: an operation whose body has an inline {@code oneOf} gets tests for its other cases, and
 * none for the degraded construct; and they pass against the validating server.
 */
@DisplayName("T14.12 Degraded operations")
class DegradedOperationsTest {

    @Test
    void theOtherCasesAreRenderedAndPass() {
        GeneratedSuite suite = Suites.of(Fixtures.DEGRADED);

        assertThat(suite.cases).isNotEmpty();
        assertThat(suite.cases).extracting(c -> c.json().get("pointer").stringValue(null))
                .noneMatch(p -> p != null && p.startsWith("/shape"));
        assertThat(suite.cases).extracting(c -> c.json().get("pointer").stringValue(null)).contains("/name");
        assertThat(suite.report.get("constraintCoverage").toString()).contains("inside a degraded construct");
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }
        assertThat(run.failed()).isEmpty();
        assertThat(run.outcomes()).hasSize(suite.cases.size());
    }
}
