package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.3: with the fixture hook left as its default, which does nothing, every generated test fails
 * against the double, with the message for a request the implementation accepted: the generated
 * tests do not pass vacuously against a double.
 */
@DisplayName("T15.3 No fixture, no pass")
class NoFixtureNoPassTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyTestFailsWithTheAcceptedMessage(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (DoubleServer server = new DoubleServer()) {
            run = suite.run(server, false, Harness.Client.SERVER, false);
        }

        assertThat(run.outcomes()).hasSize(suite.cases.size());
        assertThat(run.failed()).hasSize(suite.cases.size());
        assertThat(run.arranged()).isEmpty();
        assertThat(run.journal()).allSatisfy(served -> assertThat(served.servedBy()).isEqualTo(DoubleServer.CATCH_ALL));
        for (GeneratedSuite.Case c : suite.cases) {
            String accepted = Messages.accepted(c.json(), 200);
            assertThat(run.failed()).as(c.id())
                    .filteredOn(o -> o.testClass().equals(c.tests() + "Plain") && o.message().equals(accepted))
                    .hasSize(1);
        }
    }
}
