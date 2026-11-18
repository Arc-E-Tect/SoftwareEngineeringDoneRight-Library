package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Responses a request header selects: {@code Prefer: respond-async} selects {@code 202}. The
 * TranscriberJ sends each status the request that selects it, so no case sends another's request,
 * and the double gets a stub for every status: the {@code 202} stub answers the request that
 * prefers an asynchronous answer, and the {@code 200} stub, which requires {@code Prefer} absent,
 * answers the one that does not, and never the other.
 */
@DisplayName("Responses a request header selects")
class SelectedResponsesTest {

    private final GeneratedSuite suite = Suites.of(Fixtures.SELECTED);

    @Test
    void noCaseSendsAnotherCasesRequestSoEveryCaseHasItsStub() {
        assertThat(PublishedFilesPassTest.shadowed(suite)).isEmpty();
    }

    @Test
    void eachRequestIsAnsweredByTheStubOfTheStatusItSelects() {
        try (DoubleServer server = new DoubleServer(suite.files())) {
            for (String id : List.of("success-200-required", "success-200-full", "success-202-required",
                    "success-202-full")) {
                GeneratedSuite.Case c = suite.find("PostValidation", id);
                server.forgetRequests();
                Replay.Response response = Replay.send(server, suite.request(c));
                assertThat(server.journal().get(0).servedBy()).as(id).isEqualTo(suite.mappingName(c));
                assertThat(response.status()).as(id).isEqualTo(Integer.parseInt(id.substring(8, 11)));
            }
        }
    }

    /**
     * {@code Prefer: respond-async} without its {@code Idempotency-Key} is no case's request: the
     * {@code 200} stub, which requires {@code Prefer} absent, does not answer it, and neither does
     * any other exact stub; only a fallback does.
     */
    @Test
    void theStubOfTheOtherStatusDoesNotAnswerARequestThatPrefersAnAsynchronousAnswer() {
        GeneratedSuite.Case synchronous = suite.find("PostValidation", "success-200-required");
        try (DoubleServer server = new DoubleServer(suite.files())) {
            Replay.send(server, Replay.withHeader(suite.request(synchronous), "Prefer", "respond-async"));
            assertThat(server.journal().get(0).servedBy()).contains("/fallback-");
        }
    }
}
