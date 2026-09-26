package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.3: a test that receives another status than the one its case expects fails with a message
 * naming the case and the probable cause (S12).
 */
@DisplayName("T14.3 The S12 messages")
class StatusMessagesTest {

    static GeneratedSuite suite;
    static ContractServer server;

    @BeforeAll
    static void start() {
        suite = Suites.of(Fixtures.USER_ACCOUNT);
        server = new ContractServer(ValidatingServer.of(suite));
    }

    @AfterAll
    static void stop() {
        server.close();
    }

    static Stream<Arguments> answers() {
        byte[] problem = "{\"title\":\"x\"}".getBytes(StandardCharsets.UTF_8);
        return Stream.of(
                Arguments.of(404, (Function<JsonNode, String>) Messages::notFound, problem),
                Arguments.of(422, (Function<JsonNode, String>) Messages::domain, problem),
                Arguments.of(500, (Function<JsonNode, String>) c -> Messages.failed(c, 500), problem),
                Arguments.of(503, (Function<JsonNode, String>) c -> Messages.failed(c, 503), null),
                Arguments.of(201, (Function<JsonNode, String>) c -> Messages.accepted(c, 201), null),
                Arguments.of(418, (Function<JsonNode, String>) c -> Messages.subject(c, 418), problem));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("answers")
    void theTestFailsSayingWhereToLook(int status, Function<JsonNode, String> message, byte[] body) {
        GeneratedSuite.Case chosen = suite.find("InitiateUserRegistration", "body-username-maxLength");
        server.behave(ValidatingServer.answering(suite, chosen.json(),
                r -> new ContractServer.Answer(status, body == null ? null : "application/problem+json", body)));

        GeneratedSuite.Run run = suite.run(server);

        assertThat(run.failed()).extracting(o -> o.testClass() + "#" + o.method()).containsExactly(chosen.key(true));
        assertThat(run.failed().get(0).message()).isEqualTo(message.apply(chosen.json()));
        assertThat(run.failed().get(0).failure()).isInstanceOf(AssertionError.class);
    }
}
