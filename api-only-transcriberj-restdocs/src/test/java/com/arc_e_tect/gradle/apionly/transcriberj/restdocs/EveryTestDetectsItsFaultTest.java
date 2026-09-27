package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T20.2: for each mutant of the faithful server, the whole suite of the kinds contract runs, and
 * exactly the tests the mutant's fault concerns fail, each with the message that points at it;
 * nothing else fails. The mutant that stops enforcing one constraint, for every constraint, is
 * {@link EveryTestDetectsItsViolationTest}.
 */
@DisplayName("T20.2 Every test detects its own fault")
class EveryTestDetectsItsFaultTest {

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

    private static String location(String operation) {
        return suite.cases.stream().filter(c -> c.tests().equals(operation + ContractTests.SUFFIX)).findFirst()
                .orElseThrow().operation().get("location").stringValue();
    }

    /** Runs the suite against a mutant, and checks that exactly the cases given fail, each with its message. */
    private static void onlyTheseFail(ValidatingServer mutant, List<GeneratedSuite.Case> expected,
                                      Function<JsonNode, String> message) {
        server.behave(mutant);
        GeneratedSuite.Run run = suite.run(server);

        assertThat(run.outcomes()).hasSize(suite.cases.size());
        assertThat(run.failed()).as("the failed tests, with their messages: %s", run.failed().stream()
                        .map(o -> o.method() + ": " + o.message()).toList())
                .extracting(o -> o.testClass() + "#" + o.method())
                .containsExactlyInAnyOrderElementsOf(expected.stream().map(c -> c.key(true)).toList());
        for (GeneratedSuite.Case c : expected) {
            assertThat(run.outcomes().get(c.key(true)).message()).as(c.id()).isEqualTo(message.apply(c.json()));
        }
    }

    @Test
    void rejectingOneOptionalMemberFailsThatOperationsFullSuccessCaseOnly() {
        GeneratedSuite.Case full = suite.find("CreateOrder", "success-201-full");
        onlyTheseFail(ValidatingServer.of(suite).rejectingMember(location("CreateOrder"), "/note"), List.of(full),
                Messages::successRejected);
    }

    @Test
    void answering201Where200IsDeclaredFailsThatSuccessCaseOnly() {
        GeneratedSuite.Case read = suite.find("GetOrder", "success-200-required");
        onlyTheseFail(ValidatingServer.of(suite).substituting(location("GetOrder"), 200, 201), List.of(read),
                c -> Messages.successOtherStatus(c, 201));
    }

    @Test
    void answering200ForAMissingResourceFailsThatNotFoundCaseOnly() {
        GeneratedSuite.Case notFound = suite.find("GetOrder", "not-found");
        onlyTheseFail(ValidatingServer.of(suite).findingMissing(location("GetOrder")), List.of(notFound),
                c -> Messages.notFoundFound(c, 200));
    }

    @Test
    void ignoringAcceptFailsEveryNotAcceptableCaseAndNothingElse() {
        List<GeneratedSuite.Case> cases = suite.cases("NOT_ACCEPTABLE");
        assertThat(cases).hasSizeGreaterThanOrEqualTo(2);
        onlyTheseFail(ValidatingServer.of(suite).ignoringAccept(), cases,
                c -> Messages.acceptIgnored(c, firstSuccess(c)));
    }

    @Test
    void acceptingAnyContentTypeFailsEveryUnsupportedMediaTypeCaseAndNothingElse() {
        List<GeneratedSuite.Case> cases = suite.cases("UNSUPPORTED_MEDIA_TYPE");
        assertThat(cases).hasSizeGreaterThanOrEqualTo(2);
        onlyTheseFail(ValidatingServer.of(suite).acceptingAnyContentType(), cases,
                c -> Messages.contentTypeAccepted(c, firstSuccess(c)));
    }

    @Test
    void aSuccessBodyMissingARequiredMemberFailsThatSuccessCaseOnlyThroughDocument() {
        GeneratedSuite.Case read = suite.find("GetOrder", "success-200-required");
        server.behave(ValidatingServer.answering(suite, read.json(), r -> new ContractServer.Answer(200,
                "application/json", "{\"item\":\"a\"}".getBytes(StandardCharsets.UTF_8))));
        GeneratedSuite.Run run = suite.run(server);

        assertThat(run.failed()).extracting(o -> o.testClass() + "#" + o.method()).containsExactly(read.key(true));
        assertThat(run.failed().get(0).message()).contains("[id]").doesNotContain("expected 200");
    }

    /** The first 2xx a case's operation declares, which a mutant serves a request it should refuse with. */
    private static int firstSuccess(JsonNode c) {
        String location = suite.cases.stream().filter(k -> k.json() == c).findFirst().orElseThrow().operation()
                .get("location").stringValue();
        for (String status : suite.oracle.document.at(location + "/responses").propertyNames()) {
            if (status.matches("2[0-9][0-9]")) return Integer.parseInt(status);
        }
        return 200;
    }
}
