package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.3 and T21.6: exactness, for every kind. Of several stubs matching a request at one priority,
 * WireMock answers with the one added most recently -- as {@link WireMockBehaviourTest} checks. So
 * with every exact stub of a contract registered, once in canonical order and once in reverse, a
 * request that matched any stub but its own would be answered by that other stub in one of the two
 * orders. Each request is answered by its own stub in both, across all operations together. With
 * the fallbacks loaded too, every case's request is still answered by its exact stub.
 */
@DisplayName("T21.3 Exactness, all kinds; T21.6 Exact beats fallback")
class ExactnessTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void eachRequestMatchesOnlyItsOwnStubInEitherOrder(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        assertBothOrders(suite, exact(suite));
    }

    /** The pairs most easily confused, each shown to exist in the fixtures and kept apart. */
    @Test
    void theClosestPairsAreKeptApart() {
        GeneratedSuite success = Suites.of(Fixtures.SUCCESS);
        List<GeneratedSuite.Case> pairs = new ArrayList<>(List.of(
                success.find("PostOptional", "success-201-required"), success.find("PostOptional", "success-201-full")));
        GeneratedSuite notFound = Suites.of(Fixtures.NOT_FOUND);
        GeneratedSuite negotiation = Suites.of(Fixtures.NEGOTIATION);
        assertThat(notFound.cases("SUCCESS")).isNotEmpty();
        assertThat(notFound.cases("NOT_FOUND")).isNotEmpty();
        assertThat(negotiation.cases("NOT_ACCEPTABLE")).isNotEmpty();
        assertThat(negotiation.cases("UNSUPPORTED_MEDIA_TYPE")).isNotEmpty();
        for (GeneratedSuite suite : List.of(notFound, negotiation)) {
            for (GeneratedSuite.Case c : suite.cases) {
                boolean hasSuccess = suite.cases.stream().anyMatch(s -> s.kind().equals("SUCCESS")
                        && s.location().equals(c.location()));
                if (!c.kind().equals("SUCCESS") && !c.kind().equals("INVALID_REQUEST")) assertThat(hasSuccess).isTrue();
            }
        }
        assertBothOrders(success, pairs);
        assertBothOrders(notFound, exact(notFound));
        assertBothOrders(negotiation, exact(negotiation));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void withTheFallbacksLoadedEveryCaseIsServedByItsExactStub(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer(suite.files())) {
            assertThat(server.stubs()).anyMatch(name -> name.contains("/fallback-"));
            for (GeneratedSuite.Case c : exact(suite)) {
                server.forgetRequests();
                Replay.send(server, suite.request(c));
                assertThat(server.journal().get(0).servedBy()).as(c.toString()).isEqualTo(suite.mappingName(c));
            }
        }
    }

    /** Every case with a mapping file: all but those whose request an earlier case sends. */
    static List<GeneratedSuite.Case> exact(GeneratedSuite suite) {
        List<GeneratedSuite.Case> shadowed = PublishedFilesPassTest.shadowed(suite);
        return suite.cases.stream().filter(c -> !shadowed.contains(c)).toList();
    }

    private static void assertBothOrders(GeneratedSuite suite, List<GeneratedSuite.Case> cases) {
        List<GeneratedSuite.Case> reversed = new ArrayList<>(cases);
        Collections.reverse(reversed);
        try (DoubleServer server = DoubleServer.bodiesOf(suite.files())) {
            for (List<GeneratedSuite.Case> order : List.of(cases, reversed)) {
                server.clear();
                for (GeneratedSuite.Case c : order) {
                    StubMapping mapping = suite.stubMapping(c);
                    server.stub(mapping);
                }
                for (GeneratedSuite.Case c : cases) Replay.send(server, suite.request(c));
                List<DoubleServer.Served> journal = server.journal();
                assertThat(journal).hasSize(cases.size());
                for (int i = 0; i < cases.size(); i++) {
                    assertThat(journal.get(i).servedBy()).as("%s, registered %s", cases.get(i),
                            order == cases ? "in canonical order" : "in reverse").isEqualTo(suite.mappingName(cases.get(i)));
                    assertThat(journal.get(i).status()).as(cases.get(i).toString()).isEqualTo(cases.get(i).status());
                }
            }
        }
    }
}
