package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.8 and T17a.3: every test documents its case under {@code <prefix>/<operationId>/<caseId>/},
 * and overriding the prefix moves the snippets. They are written because {@code document()} is
 * where the response is validated; nothing publishes them.
 */
@DisplayName("T14.8, T17a.3 Snippets")
class SnippetsTest {

    static final Map<String, GeneratedSuite.Run> RUNS = new HashMap<>();

    @BeforeAll
    static void runEverySuite() {
        for (Fixtures.Contract contract : Fixtures.ALL) {
            GeneratedSuite suite = Suites.of(contract);
            try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
                RUNS.put(contract.name(), suite.run(server));
            }
        }
    }

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyCaseIsDocumentedUnderItsOwnDirectory(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run = RUNS.get(contract.name());
        for (GeneratedSuite.Case c : suite.cases) {
            Path directory = run.snippets().resolve(suite.snippetDirectory(InvalidRequestTests.PREFIX, c));
            assertThat(directory.resolve("http-request.adoc")).as(c.id()).exists();
            assertThat(directory.resolve("http-response.adoc")).as(c.id()).exists();
        }
    }

    @Test
    void overridingThePrefixMovesTheSnippets() {
        GeneratedSuite suite = Suites.of(Fixtures.USER_ACCOUNT);
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server, true, "moved");
        }

        assertThat(run.failed()).isEmpty();
        assertThat(run.snippets().resolve(InvalidRequestTests.PREFIX)).doesNotExist();
        for (GeneratedSuite.Case c : suite.cases) {
            assertThat(run.snippets().resolve(suite.snippetDirectory("moved", c)).resolve("http-request.adoc"))
                    .exists();
        }
    }
}
