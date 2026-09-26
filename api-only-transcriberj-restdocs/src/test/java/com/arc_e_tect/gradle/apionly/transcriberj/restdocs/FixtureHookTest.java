package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.7: {@code arrangeInvalidRequest} is called exactly once per test, with the test's case,
 * before its request is sent; and its default does nothing.
 */
@DisplayName("T14.7 The fixture hook")
class FixtureHookTest {

    @Test
    void theHookIsCalledOncePerTestWithItsCaseBeforeTheRequest() {
        GeneratedSuite suite = Suites.of(Fixtures.USER_ACCOUNT);
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }

        assertThat(run.arrangements()).hasSize(suite.cases.size());
        assertThat(run.received()).hasSize(suite.cases.size());
        for (GeneratedSuite.Case c : suite.cases) {
            List<Harness.Arrangement> calls = run.arrangements().stream()
                    .filter(a -> a.testClass().equals(c.tests() + "Recording") && a.caseId().equals(c.id())).toList();
            assertThat(calls).as(c.id()).hasSize(1);
        }
        // Tests run one at a time: every hook call is followed by exactly one request before the next call.
        List<Long> hooks = run.arrangements().stream().map(Harness.Arrangement::sequence).sorted().toList();
        List<ContractServer.Received> requests = run.received().stream()
                .sorted(Comparator.comparingLong(ContractServer.Received::sequence)).toList();
        for (int i = 0; i < hooks.size(); i++) {
            long next = i + 1 < hooks.size() ? hooks.get(i + 1) : Long.MAX_VALUE;
            assertThat(requests.get(i).sequence()).isGreaterThan(hooks.get(i)).isLessThan(next);
        }
    }

    @Test
    void theDefaultDoesNothing() throws IOException {
        GeneratedSuite suite = Suites.of(Fixtures.USER_ACCOUNT);
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server, false, null);
        }

        assertThat(run.failed()).isEmpty();
        assertThat(run.outcomes()).hasSize(suite.cases.size());
        assertThat(run.arrangements()).isEmpty();
        for (String tests : suite.interfaces) {
            assertThat(Files.readString(suite.source("com/example/contract/restdocs/" + tests + ".java")))
                    .contains("    default void arrangeInvalidRequest(InvalidRequestCase invalid) {\n    }\n");
        }
    }
}
