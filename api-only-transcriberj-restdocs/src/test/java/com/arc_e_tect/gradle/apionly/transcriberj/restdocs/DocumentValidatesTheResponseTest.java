package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.5: {@code document()} validates the response against its declared schema, through the
 * response class's companion; where the contract declares no content, only the status is checked.
 */
@DisplayName("T14.5 document() really validates the response")
class DocumentValidatesTheResponseTest {

    private static GeneratedSuite.Run answeringOneCase(Fixtures.Contract contract, String caseId,
                                                       ContractServer.Answer answer, String expectedFailure) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Case chosen = suite.find(caseId);
        try (ContractServer server = new ContractServer(ValidatingServer.answering(suite, chosen.json(), r -> answer))) {
            GeneratedSuite.Run run = suite.run(server);
            if (expectedFailure == null) {
                assertThat(run.failed()).isEmpty();
            } else {
                assertThat(run.failed()).extracting(o -> o.testClass() + "#" + o.method())
                        .containsExactly(chosen.key(true));
                assertThat(run.failed().get(0).message()).contains(expectedFailure)
                        .doesNotContain("received " + answer.status());
            }
            return run;
        }
    }

    private static byte[] json(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void aBodyMissingARequiredProblemMemberFails() {
        answeringOneCase(Fixtures.TRANSMISSION, "body-count-minimum", new ContractServer.Answer(400,
                "application/problem+json", json("{\"title\":\"Bad\"}")), "status");
    }

    @Test
    void aMemberAClosedProblemDoesNotDeclareFails() {
        answeringOneCase(Fixtures.TRANSMISSION, "body-count-minimum", new ContractServer.Answer(400,
                "application/problem+json", json("{\"title\":\"Bad\",\"status\":400,\"detail\":\"x\"}")), "detail");
    }

    @Test
    void aWrongContentTypeFails() {
        answeringOneCase(Fixtures.TRANSMISSION, "body-count-minimum", new ContractServer.Answer(400,
                "text/plain", json("{\"title\":\"Bad\",\"status\":400}")), "application/problem+json");
    }

    @Test
    void noContentTypeFails() {
        answeringOneCase(Fixtures.TRANSMISSION, "body-count-minimum", new ContractServer.Answer(400, null, null),
                "received none");
    }

    @Test
    void theDeclaredStatusWithTheDeclaredBodyPasses() {
        answeringOneCase(Fixtures.TRANSMISSION, "body-count-minimum", new ContractServer.Answer(400,
                "application/problem+json", json("{\"title\":\"Bad\",\"status\":400}")), null);
    }

    @Test
    void aResponseDeclaredWithoutContentPassesWithNoBodyAssertion() {
        GeneratedSuite suite = Suites.of(Fixtures.DEGRADED);
        String id = suite.cases.get(0).id();
        assertThat(suite.cases.get(0).json().get("expectedContentTypes")).isEmpty();
        answeringOneCase(Fixtures.DEGRADED, id, new ContractServer.Answer(400, null, null), null);
        answeringOneCase(Fixtures.DEGRADED, id, new ContractServer.Answer(400, "text/plain", json("anything")), null);
    }
}
