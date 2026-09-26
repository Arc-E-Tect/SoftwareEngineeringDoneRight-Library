package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.4: what the server receives is exactly the case's request -- its path values, query,
 * headers, content type and body, decoded -- for every case of every fixture contract, including
 * values with a space, {@code +}, {@code &}, {@code =}, {@code %} and non-ASCII characters; and a
 * header value that is not US-ASCII, which no HTTP client sends as it is, fails the test before
 * anything is sent.
 */
@DisplayName("T14.4 Verbatim transmission")
class VerbatimTransmissionTest {

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

    @Test
    void theTransmissionContractsValuesCarryEveryCharacterThatNeedsEncoding() {
        GeneratedSuite suite = Suites.of(Fixtures.TRANSMISSION);
        JsonNode request = suite.find("body-count-minimum").json().get("request");
        assertThat(request.get("pathParameters").get(0).stringValue()).contains(" ", "+", "&", "=", "%", "é");
        assertThat(request.get("query").toString()).contains(" ", "+", "&", "=", "%", "ü");
        assertThat(request.get("headers").toString()).contains(" ", "+", "&", "=", "%");
        assertThat(request.get("body").toString()).contains(" ", "+", "&", "=", "%", "ñ");
        assertThat(RUNS.get("transmission").failed()).isEmpty();
    }

    @Test
    void aHeaderValueThatIsNotUsAsciiFailsTheTestBeforeSending(@TempDir Path directory) throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info: {title: T, version: 1.0.0}
                paths:
                  /h:
                    post:
                      operationId: postH
                      parameters:
                        - {name: X-Label, in: header, required: true, schema: {type: string, const: 'grün'}}
                        - {name: count, in: query, required: true, schema: {type: integer, minimum: 1}}
                      responses:
                        '204': {description: Stored.}
                        '400': {description: The request is invalid.}
                """);
        GeneratedSuite suite = GeneratedSuite.of("header", contract, directory.resolve("suite"));
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }

        assertThat(run.outcomes()).hasSize(suite.cases.size());
        List<GeneratedSuite.Case> nonAscii = suite.cases.stream()
                .filter(c -> c.json().get("request").get("headers").toString().contains("grün")).toList();
        assertThat(nonAscii).isNotEmpty();
        assertThat(run.received()).hasSize(suite.cases.size() - nonAscii.size());
        for (GeneratedSuite.Case c : nonAscii) {
            assertThat(run.outcomes().get(c.key(true)).message()).isEqualTo("Case " + c.id() + " ("
                    + c.json().get("description").stringValue() + "): header X-Label holds 'grün', which cannot be "
                    + "sent as it is: HTTP header values are US-ASCII. Give the header's value in the contract in "
                    + "US-ASCII.");
        }
    }

    @TestFactory
    Stream<DynamicTest> theServerReceivesExactlyTheCasesRequest() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Fixtures.Contract contract : Fixtures.ALL) {
            GeneratedSuite suite = Suites.of(contract);
            GeneratedSuite.Run run = RUNS.get(contract.name());
            for (GeneratedSuite.Case c : suite.cases) {
                tests.add(DynamicTest.dynamicTest(contract.name() + " " + c.id(), () ->
                        assertReceived(suite.request(c), received(run, c))));
            }
        }
        return tests.stream();
    }

    /** The request a case's test sent: the first the server received after the test's fixture hook ran. */
    static ContractServer.Received received(GeneratedSuite.Run run, GeneratedSuite.Case c) {
        Harness.Arrangement arrangement = run.arrangements().stream()
                .filter(a -> a.testClass().equals(c.tests() + "Recording") && a.caseId().equals(c.id()))
                .findFirst().orElseThrow();
        return run.received().stream().filter(r -> r.sequence() > arrangement.sequence())
                .min(java.util.Comparator.comparingLong(ContractServer.Received::sequence)).orElseThrow();
    }

    static void assertReceived(GeneratedSuite.Request expected, ContractServer.Received received) {
        assertThat(received.method()).isEqualTo(expected.method());

        Matcher m = Pattern.compile(expected.pathTemplate().replaceAll("\\{[^}]+}", "([^/]+)"))
                .matcher(received.rawPath());
        assertThat(m.matches()).as("%s matches %s", received.rawPath(), expected.pathTemplate()).isTrue();
        List<String> values = new ArrayList<>();
        for (int i = 1; i <= m.groupCount(); i++) values.add(ValidatingServer.decodePath(m.group(i)));
        assertThat(values).isEqualTo(expected.pathParameters());

        List<String> query = new ArrayList<>();
        if (received.rawQuery() != null) {
            // No character a server could read two ways travels unencoded.
            assertThat(received.rawQuery()).doesNotContain("+", " ");
            for (String pair : received.rawQuery().split("&")) {
                String[] nv = pair.split("=", 2);
                query.add(ValidatingServer.decodeQuery(nv[0]) + "=" + ValidatingServer.decodeQuery(nv[1]));
            }
        }
        assertThat(query).isEqualTo(expected.query());

        for (String header : expected.headers()) {
            String name = header.substring(0, header.indexOf('='));
            assertThat(name + "=" + ValidatingServer.header(received.header(name))).isEqualTo(header);
        }

        if (expected.body() == null) {
            assertThat(received.body()).isEmpty();
            assertThat(received.header("content-type")).isNull();
        } else {
            assertThat(received.header("content-type")).isEqualTo(expected.contentType());
            assertThat(received.body()).isEqualTo(expected.body().getBytes(StandardCharsets.UTF_8));
        }
    }

}
