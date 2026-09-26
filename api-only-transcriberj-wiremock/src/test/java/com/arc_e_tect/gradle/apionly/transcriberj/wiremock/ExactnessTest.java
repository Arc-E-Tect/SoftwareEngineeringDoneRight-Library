package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.4: exactness, proven in O(n). Of several stubs matching a request at one priority, WireMock
 * answers with the one added most recently -- checked for the WireMock version the emitter manages
 * by {@link WireMockBehaviourTest#ofSeveralMatchingStubsAtOnePriorityTheMostRecentlyAddedAnswers()}.
 * So with every mapping registered, all at the same priority, once in canonical order and once in
 * reverse, a request that matched any stub but its own would be answered by that other stub in one
 * of the two orders. Each request is answered by its own stub in both: per operation, and with
 * every operation's mappings registered together.
 */
@DisplayName("T15.4 Exactness")
class ExactnessTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void eachRequestMatchesOnlyItsOwnStubWithinItsOperation(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            for (JsonNode operation : suite.operations()) {
                assertBothOrders(suite, server, suite.casesOf(operation));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void eachRequestMatchesOnlyItsOwnStubAcrossOperations(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            assertBothOrders(suite, server, suite.cases);
        }
    }

    private static void assertBothOrders(GeneratedSuite suite, DoubleServer server, List<GeneratedSuite.Case> cases) {
        List<GeneratedSuite.Case> reversed = new ArrayList<>(cases);
        Collections.reverse(reversed);
        for (List<GeneratedSuite.Case> order : List.of(cases, reversed)) {
            server.reset();
            order.forEach(c -> server.stub(suite.mapping(c)));
            for (GeneratedSuite.Case c : cases) Replay.send(server, Replay.of(suite.invalid(c)));
            List<DoubleServer.Served> journal = server.journal();
            assertThat(journal).hasSize(cases.size());
            for (int i = 0; i < cases.size(); i++) {
                assertThat(journal.get(i).servedBy()).as("%s, registered %s", cases.get(i).id(),
                        order == cases ? "in canonical order" : "in reverse").isEqualTo(suite.mappingName(cases.get(i)));
            }
        }
    }
}
