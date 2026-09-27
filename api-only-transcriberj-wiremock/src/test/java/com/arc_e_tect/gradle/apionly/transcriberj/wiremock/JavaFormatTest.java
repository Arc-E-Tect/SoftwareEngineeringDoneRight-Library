package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code java} format as a contract-test double: the REST Docs emitter's generated suite, its
 * hooks registering {@code ContractStubs.mappingFor(contractCase)}, passes against a server holding
 * nothing else -- every test, including those whose request another case sends too, since each
 * hook registers its own case's stub just before its request -- through the in-process server,
 * the static client and a client instance alike. A consumer's own default headers do not stop a
 * stub from matching. The generated Java compiles with every lint on, without a warning.
 */
@DisplayName("The java format in contract tests")
class JavaFormatTest {

    /** Headers a consumer's client adds that no operation declares. */
    static final Map<String, String> CLIENT_DEFAULTS = Map.of("User-Agent", "contract-tests",
            "X-Forwarded-For", "203.0.113.7", "X-Request-Id", "f00");

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyTestPassesWithItsHookRegisteringItsStub(Fixtures.Contract contract) {
        for (Harness.Client client : Harness.Client.values()) {
            GeneratedSuite suite = Suites.of(contract);
            GeneratedSuite.Run run;
            try (DoubleServer server = new DoubleServer()) {
                run = suite.run(server, GeneratedSuite.Variant.REGISTERING, client, Map.of());
            }
            assertThat(run.failed()).as(client.name()).extracting(GeneratedSuite.Outcome::message).isEmpty();
            assertThat(run.outcomes()).hasSize(suite.cases.size());
            assertThat(run.arranged()).containsExactlyInAnyOrderElementsOf(
                    suite.cases.stream().map(suite::mappingName).toList());
            for (int i = 0; i < run.journal().size(); i++) {
                assertThat(run.journal().get(i).servedBy()).as(client + " " + run.journal().get(i))
                        .isEqualTo(run.arranged().get(i));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void aConsumersDefaultHeadersDoNotStopAStubFromMatching(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (DoubleServer server = new DoubleServer(suite.files())) {
            run = suite.run(server, GeneratedSuite.Variant.PRELOADED, Harness.Client.SERVER, CLIENT_DEFAULTS);
        }
        PublishedFilesPassTest.assertServedByOwnStubs(suite, run);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theGeneratedJavaCompilesWithoutAWarning(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        assertThat(suite.diagnostics).extracting(d -> d.getSource() + ": " + d.getMessage(java.util.Locale.ROOT))
                .filteredOn(d -> d.contains("wiremock")).isEmpty();
    }

    @Test
    void valuesThatNeedEncodingMatchVerbatim() {
        GeneratedSuite suite = Suites.of(Fixtures.TRANSMISSION);
        String request = suite.find("body-count-minimum").json().get("request").toString();
        assertThat(request).contains(" ", "+", "&", "=", "%", "é", "ü", "ñ");
        try (DoubleServer server = new DoubleServer(suite.files())) {
            PublishedFilesPassTest.assertServedByOwnStubs(suite, suite.runPreloaded(server));
        }
    }
}
