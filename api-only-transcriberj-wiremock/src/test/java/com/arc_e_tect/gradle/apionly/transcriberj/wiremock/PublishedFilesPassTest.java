package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.1: the published files pass the contract tests. A WireMock server loading only the generated
 * files directory runs the REST Docs emitter's full generated suite, the double's hooks doing
 * nothing; every test passes, and the request journal shows each request served by the exact stub
 * named after its own case: exactness means stubs loaded together never answer each other's requests.
 */
@DisplayName("T21.1 The published files pass the contract tests")
class PublishedFilesPassTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyTestPassesServedByItsOwnExactStub(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (DoubleServer server = new DoubleServer(suite.files())) {
            run = suite.runPreloaded(server);
        }
        assertServedByOwnStubs(suite, run);
        assertThat(run.arranged()).as("the hooks register nothing").isEmpty();
    }

    /**
     * The one exception: a case whose request an earlier case of its operation sends too -- a
     * {@code PUT} answering {@code 200} or {@code 201} as the resource exists or not. Mapping files
     * hold no state, so the earlier case's stub answers both, and the later case's test fails, with
     * the status the earlier one expects.
     */
    @org.junit.jupiter.api.Test
    void aCaseSendingAnEarlierCasesRequestIsAnsweredByThatCasesStub() {
        GeneratedSuite suite = Suites.of(Fixtures.SUCCESS);
        GeneratedSuite.Run run;
        try (DoubleServer server = new DoubleServer(suite.files())) {
            run = suite.runPreloaded(server);
        }
        List<GeneratedSuite.Case> shadowed = shadowed(suite);
        assertThat(shadowed).extracting(GeneratedSuite.Case::id)
                .containsExactlyInAnyOrder("success-201-required", "success-201-full");
        assertThat(run.failed()).hasSameSizeAs(shadowed).allSatisfy(failed ->
                assertThat(failed.message()).contains("expected 201, received 200"));
        assertThat(suite.generated.wiremock().degraded()).extracting(d -> d.className()).containsExactlyInAnyOrder(
                shadowed.stream().map(suite::mappingFile).toArray(String[]::new));
    }

    /** The cases whose request an earlier case of the same operation sends too. */
    static List<GeneratedSuite.Case> shadowed(GeneratedSuite suite) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<GeneratedSuite.Case> out = new java.util.ArrayList<>();
        for (GeneratedSuite.Case c : suite.cases) {
            if (!seen.add(c.location() + " " + c.json().get("request"))) out.add(c);
        }
        return out;
    }

    /**
     * Every test passed, one per case, each request served by the stub named after its case --
     * but for a case whose request an earlier one sends, served by the earlier case's stub.
     */
    static void assertServedByOwnStubs(GeneratedSuite suite, GeneratedSuite.Run run) {
        assertThat(suite.cases).isNotEmpty();
        List<GeneratedSuite.Case> shadowed = shadowed(suite);
        assertThat(run.failed()).hasSameSizeAs(shadowed);
        if (shadowed.isEmpty()) assertThat(run.failed()).extracting(GeneratedSuite.Outcome::message).isEmpty();
        assertThat(run.outcomes()).hasSize(suite.cases.size());
        assertThat(run.journal()).hasSize(suite.cases.size());
        assertThat(run.journal()).extracting(DoubleServer.Served::servedBy)
                .containsExactlyInAnyOrderElementsOf(suite.cases.stream().map(c -> suite.mappingName(
                        shadowed.contains(c) ? earlier(suite, c) : c)).toList());
    }

    private static GeneratedSuite.Case earlier(GeneratedSuite suite, GeneratedSuite.Case c) {
        return suite.cases.stream().filter(e -> e.location().equals(c.location())
                && e.json().get("request").equals(c.json().get("request"))).findFirst().orElseThrow();
    }
}
