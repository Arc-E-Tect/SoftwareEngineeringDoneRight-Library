package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.8: for every case, the stub answers with the declared media type -- the first where two are
 * declared -- and a body the independent JSON Schema 2020-12 validator finds valid against the
 * declared response schema; where the declared response has no content, with no body and no
 * {@code Content-Type}.
 */
@DisplayName("T15.8 Response fidelity")
class ResponseFidelityTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyResponseIsWhatTheContractDeclares(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            for (GeneratedSuite.Case c : suite.cases) {
                server.reset();
                server.stub(suite.mapping(c));
                Replay.Response response = Replay.send(server, Replay.of(suite.invalid(c)));
                String declared = response(suite, c);
                JsonNode content = suite.oracle.document.at(declared).path("content");
                assertThat(response.status()).as(c.id()).isEqualTo(c.status());
                if (content.isMissingNode() || content.isEmpty()) {
                    assertThat(response.contentType()).as(c.id()).isNull();
                    assertThat(response.body()).as(c.id()).isEmpty();
                    continue;
                }
                String first = content.propertyNames().iterator().next();
                assertThat(response.contentType()).as(c.id()).isEqualTo(first);
                String schema = declared + "/content/" + escape(first) + "/schema";
                assertThat(suite.oracle.errors(schema, Oracle.JSON.readTree(response.body()))).as(c.id()).isEmpty();
            }
        }
    }

    @Test
    void theFixturesCoverTwoMediaTypesAndNoContent() {
        GeneratedSuite suite = Suites.of(Fixtures.RESPONSES);
        assertThat(suite.cases).anySatisfy(c -> assertThat(c.json().get("expectedContentTypes")).hasSize(2));
        assertThat(suite.cases).anySatisfy(c -> assertThat(c.json().get("expectedContentTypes")).isEmpty());
        assertThat(suite.cases).anySatisfy(c -> assertThat(c.json().get("responseBodyClass").isNull()).isFalse());
    }

    /** The pointer of the response the contract declares for a case, through a {@code $ref} to a component. */
    private static String response(GeneratedSuite suite, GeneratedSuite.Case c) {
        String pointer = c.location() + "/responses/" + c.status();
        JsonNode node = suite.oracle.document.at(pointer);
        while (node.has("$ref")) {
            pointer = node.get("$ref").stringValue().substring(1);
            node = suite.oracle.document.at(pointer);
        }
        return pointer;
    }

    private static String escape(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }
}
