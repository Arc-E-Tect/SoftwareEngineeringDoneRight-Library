package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.1: every REST Docs test the core's cases render passes against a WireMock double whose only
 * fixture is the generated stub its hook registers; one test runs per case in the core's report;
 * and the journal shows every request served by the stub named after its own case, none by the
 * catch-all.
 */
@DisplayName("T15.1 Everything passes against generated stubs")
class EverythingPassesTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyGeneratedTestPassesServedByItsOwnStub(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (DoubleServer server = new DoubleServer()) {
            run = suite.run(server);
        }
        assertServedByOwnStubs(suite, run);
    }

    /** What T15.1 asserts of a run, for the client variants too. */
    static void assertServedByOwnStubs(GeneratedSuite suite, GeneratedSuite.Run run) {
        assertThat(suite.cases).isNotEmpty();
        assertThat(run.failed()).extracting(GeneratedSuite.Outcome::message).isEmpty();
        assertThat(run.outcomes()).hasSize(suite.cases.size());
        int reported = 0;
        for (var operation : suite.report.get("invalidRequests")) reported += operation.get("cases").size();
        assertThat(run.outcomes()).hasSize(reported);

        assertThat(run.arranged()).hasSize(suite.cases.size())
                .containsExactlyInAnyOrderElementsOf(suite.cases.stream().map(suite::mappingName).toList());
        assertThat(run.journal()).hasSize(run.arranged().size());
        for (int i = 0; i < run.journal().size(); i++) {
            assertThat(run.journal().get(i).servedBy()).as("request %d, %s", i, run.journal().get(i))
                    .isEqualTo(run.arranged().get(i)).isNotEqualTo(DoubleServer.CATCH_ALL);
        }
    }
}
