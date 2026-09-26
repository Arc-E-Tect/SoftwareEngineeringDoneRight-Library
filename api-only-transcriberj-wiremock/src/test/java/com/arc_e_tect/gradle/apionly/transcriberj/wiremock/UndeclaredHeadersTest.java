package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.6: headers the operation does not declare do not stop a stub from matching. Every case's
 * request, sent with the default headers the reference implementation's
 * {@code WireMockContractValidatorTest} gives its client -- {@code Content-Type} and {@code Accept}
 * of {@code application/json}, {@code Person-Agent} and {@code X-Forwarded-For} -- is still served
 * by its own stub: through the rendered tests, whose client is configured with them, and replayed.
 */
@DisplayName("T15.6 Undeclared headers do not break matching")
class UndeclaredHeadersTest {

    static final Map<String, String> REFERENCE_DEFAULTS = Map.of("Content-Type", "application/json",
            "Accept", "application/json", "Person-Agent", "API-Contract-Validator", "X-Forwarded-For", "203.0.113.7");

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theRenderedTestsPassWithTheReferenceDefaults(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (DoubleServer server = new DoubleServer()) {
            run = suite.run(server, true, Harness.Client.SERVER, true);
        }
        EverythingPassesTest.assertServedByOwnStubs(suite, run);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyReplayedRequestWithTheReferenceDefaultsIsServedByItsOwnStub(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            suite.cases.forEach(c -> server.stub(suite.mapping(c)));
            for (GeneratedSuite.Case c : suite.cases) {
                Replay.send(server, Replay.of(suite.invalid(c)), REFERENCE_DEFAULTS);
            }
            List<DoubleServer.Served> journal = server.journal();
            for (int i = 0; i < suite.cases.size(); i++) {
                assertThat(journal.get(i).servedBy()).isEqualTo(suite.mappingName(suite.cases.get(i)));
            }
        }
    }
}
