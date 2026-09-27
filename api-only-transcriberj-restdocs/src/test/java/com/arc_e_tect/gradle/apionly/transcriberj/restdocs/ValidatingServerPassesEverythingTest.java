package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.1 and T20.1: against a server that implements the contract faithfully -- validating every
 * request with a validator independent of the generator, negotiating content, and keeping its
 * resources in a store the tests' fixture arranges -- every generated test passes, and as many run
 * of each kind as the report has cases of it.
 */
@DisplayName("T14.1, T20.1 A faithful server passes everything")
class ValidatingServerPassesEverythingTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyGeneratedTestPasses(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        int cases = 0;
        java.util.Map<String, Integer> reported = new java.util.TreeMap<>();
        for (var operation : suite.report.get("contractCases")) {
            cases += operation.get("cases").size();
            operation.get("cases").forEach(c -> reported.merge(c.get("kind").stringValue(), 1, Integer::sum));
        }

        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }

        assertThat(run.failed()).as("the failed tests").extracting(o -> o.method() + ": " + o.message()).isEmpty();
        // Every request was rejected for its own case's fault, and for nothing else.
        ValidatingServer server = ValidatingServer.of(suite);
        RequestValidation validation = new RequestValidation(suite.oracle, suite.settings.strictRequests());
        for (GeneratedSuite.Case c : suite.cases("INVALID_REQUEST")) {
            ValidatingServer.Parsed parsed = server.parse(VerbatimTransmissionTest.received(run, c));
            List<RequestValidation.Problem> problems = validation.problems(parsed.request());
            assertThat(problems).as("the faults of %s", c.id()).isNotEmpty();
            assertThat(ValidatingServer.without(problems, c.json(), parsed)).as("the faults of %s besides its own", c.id())
                    .isEmpty();
        }
        assertThat(run.outcomes()).hasSize(cases);
        assertThat(cases).isEqualTo(suite.cases.size()).isPositive();
        java.util.Map<String, Integer> ranByKind = new java.util.TreeMap<>();
        for (GeneratedSuite.Case c : suite.cases) {
            assertThat(run.outcomes()).containsKey(c.key(true));
            ranByKind.merge(c.kind(), 1, Integer::sum);
        }
        assertThat(ranByKind).isEqualTo(reported);
    }
}
