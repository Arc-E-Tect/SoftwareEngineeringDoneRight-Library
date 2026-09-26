package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.10: one stub, one test. With every mapping registered up front but one, exactly one rendered
 * test fails: that case's. With every mapping registered but one answering {@code 200}, exactly
 * that case's test fails. No test passes on another case's stub.
 */
@DisplayName("T15.10 One stub, one test")
class OneStubOneTestTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void withoutACasesStubOnlyItsTestFails(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            for (GeneratedSuite.Case left : suite.cases) {
                server.reset();
                for (GeneratedSuite.Case c : suite.cases) {
                    if (c != left) server.stub(suite.mapping(c));
                }
                assertOnlyItsTestFails(suite, server, left);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void withACasesStubAnswering200OnlyItsTestFails(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            for (GeneratedSuite.Case changed : suite.cases) {
                server.reset();
                for (GeneratedSuite.Case c : suite.cases) {
                    server.stub(c != changed ? suite.mapping(c)
                            : suite.mapping(c).willReturn(aResponse().withStatus(200)));
                }
                assertOnlyItsTestFails(suite, server, changed);
            }
        }
    }

    private static void assertOnlyItsTestFails(GeneratedSuite suite, DoubleServer server, GeneratedSuite.Case c) {
        GeneratedSuite.Run run = suite.run(server, false, Harness.Client.SERVER, false);
        assertThat(run.outcomes()).hasSize(suite.cases.size());
        assertThat(run.failed()).as(c.id()).singleElement().satisfies(failed -> {
            assertThat(failed.testClass()).isEqualTo(c.tests() + "Plain");
            assertThat(failed.message()).isEqualTo(Messages.accepted(c.json(), 200));
        });
    }
}
