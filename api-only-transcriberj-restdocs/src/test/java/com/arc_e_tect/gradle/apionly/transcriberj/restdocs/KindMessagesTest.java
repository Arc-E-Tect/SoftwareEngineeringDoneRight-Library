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
 * T20.9: a server that answers one chosen case of each kind with each status the README's table
 * names makes that case's test fail with that row's message, and any other status with the subject
 * alone: {@code <id> (<kind>: <description>): expected <status>, received <status>.}
 */
@DisplayName("T20.9 The messages, per kind")
class KindMessagesTest {

    static GeneratedSuite suite;
    static ContractServer server;

    @BeforeAll
    static void start() {
        suite = Suites.of(Fixtures.KINDS);
        server = new ContractServer(ValidatingServer.of(suite));
    }

    @AfterAll
    static void stop() {
        server.close();
    }

    static Stream<Arguments> rows() {
        String read = "GetOrder:success-200-required";
        String missing = "GetOrder:not-found";
        String refused = "GetOrder:not-acceptable";
        String unsupported = "CreateOrder:unsupported-media-type";
        return Stream.of(
                Arguments.of(read, 400, (Function<JsonNode, String>) Messages::successRejected),
                Arguments.of(read, 404, (Function<JsonNode, String>) Messages::successNotFound),
                Arguments.of(read, 409, (Function<JsonNode, String>) Messages::successConflict),
                Arguments.of(read, 202, (Function<JsonNode, String>) c -> Messages.successOtherStatus(c, 202)),
                Arguments.of(read, 500, (Function<JsonNode, String>) c -> Messages.successFailed(c, 500)),
                Arguments.of(read, 418, (Function<JsonNode, String>) c -> Messages.subject(c, 418)),
                Arguments.of(missing, 200, (Function<JsonNode, String>) c -> Messages.notFoundFound(c, 200)),
                Arguments.of(missing, 400, (Function<JsonNode, String>) Messages::notFoundRejected),
                Arguments.of(missing, 500, (Function<JsonNode, String>) c -> Messages.subject(c, 500)),
                Arguments.of(refused, 200, (Function<JsonNode, String>) c -> Messages.acceptIgnored(c, 200)),
                Arguments.of(refused, 404, (Function<JsonNode, String>) c -> Messages.subject(c, 404)),
                Arguments.of(unsupported, 201, (Function<JsonNode, String>) c -> Messages.contentTypeAccepted(c, 201)),
                Arguments.of(unsupported, 400, (Function<JsonNode, String>) Messages::parsedFirst),
                Arguments.of(unsupported, 500, (Function<JsonNode, String>) c -> Messages.subject(c, 500)));
    }

    @ParameterizedTest(name = "{0} answered {1}")
    @MethodSource("rows")
    void theTestFailsSayingWhereToLook(String chosen, int status, Function<JsonNode, String> message) {
        GeneratedSuite.Case c = suite.find(chosen.substring(0, chosen.indexOf(':')), chosen.substring(chosen.indexOf(':') + 1));
        byte[] problem = "{\"title\":\"x\",\"status\":1}".getBytes(StandardCharsets.UTF_8);
        server.behave(ValidatingServer.answering(suite, c.json(),
                r -> new ContractServer.Answer(status, "application/problem+json", problem)));

        GeneratedSuite.Run run = suite.run(server);

        assertThat(run.failed()).extracting(o -> o.testClass() + "#" + o.method()).containsExactly(c.key(true));
        assertThat(run.failed().get(0).message()).isEqualTo(message.apply(c.json()));
        assertThat(run.failed().get(0).failure()).isInstanceOf(AssertionError.class);
    }
}
