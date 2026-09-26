package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.1: against a server that validates every request against the contract, with a validator
 * independent of the generator, every generated test passes, and as many run as the report has
 * cases.
 */
@DisplayName("T14.1 A genuinely validating server passes everything")
class ValidatingServerPassesEverythingTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyGeneratedTestPasses(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        int cases = 0;
        for (var operation : suite.report.get("invalidRequests")) cases += operation.get("cases").size();

        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }

        assertThat(run.failed()).as("the failed tests").extracting(o -> o.method() + ": " + o.message()).isEmpty();
        // Every request was rejected for its own case's fault, and for nothing else.
        ValidatingServer server = ValidatingServer.of(suite);
        RequestValidation validation = new RequestValidation(suite.oracle, suite.settings.strictRequests());
        for (GeneratedSuite.Case c : suite.cases) {
            ValidatingServer.Parsed parsed = server.parse(VerbatimTransmissionTest.received(run, c));
            List<RequestValidation.Problem> problems = validation.problems(parsed.request());
            assertThat(problems).as("the faults of %s", c.id()).isNotEmpty();
            assertThat(ValidatingServer.without(problems, c.json(), parsed)).as("the faults of %s besides its own", c.id())
                    .isEmpty();
        }
        assertThat(run.outcomes()).hasSize(cases);
        assertThat(cases).isEqualTo(suite.cases.size()).isPositive();
    }
}
