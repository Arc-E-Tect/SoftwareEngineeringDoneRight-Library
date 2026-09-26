package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.6: an invalid request -- a required member missing, an unknown member, the wrong type -- is
 * never documented with {@code requestFields}, so {@code document()} never fails because of the
 * request: against the validating server every test passes, and every request is still
 * documented, through the {@code http-request} snippet.
 */
@DisplayName("T14.6 Invalid requests never trip document()")
class InvalidRequestsNeverTripDocumentTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void noTestFailsBecauseOfItsRequest(Fixtures.Contract contract) throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }

        assertThat(run.failed()).extracting(GeneratedSuite.Outcome::message).isEmpty();
        assertThat(suite.cases).extracting(c -> c.json().get("keyword").stringValue())
                .contains(contract == Fixtures.DEGRADED ? new String[]{"required", "type"}
                        : new String[]{"required", "type", "additionalProperties"});
        for (GeneratedSuite.Case c : suite.cases) {
            Path request = run.snippets().resolve(suite.snippetDirectory(InvalidRequestTests.PREFIX, c))
                    .resolve("http-request.adoc");
            assertThat(request).as(c.id()).exists();
            assertThat(request.resolveSibling("request-fields.adoc")).doesNotExist();
        }
        try (Stream<Path> sources = Files.list(suite.source("com/example/contract/restdocs"))) {
            for (Path source : sources.filter(p -> p.getFileName().toString()
                    .endsWith(InvalidRequestTests.SUFFIX + ".java")).toList()) {
                assertThat(Files.readString(source)).as(source.getFileName().toString())
                        .doesNotContain("requestFields");
            }
        }
    }
}
