package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.8: strictness and formats reach the fallback. The valid fallback checks a body against the
 * TranscriberJ's own request body schema: with {@code strictRequests} on, a valid body with a member
 * nobody declared is answered by the invalid fallback; with it off, by the valid one. A format that
 * {@code validateFormats} names is enforced by its shape, although WireMock's {@code matchesJsonSchema}
 * asserts no format: a body whose {@code date} has another shape is answered by the invalid fallback
 * when {@code date} is named, and by the valid one when it is not.
 */
@DisplayName("T21.8 Strictness and formats reach the fallback")
class StrictnessTest {

    @Test
    void anUnknownMemberIsInvalidOnlyWhenRequestsAreStrict() {
        assertThat(served(Fixtures.USER_ACCOUNT)).isEqualTo("InitiateUserRegistration/fallback-invalid");
        assertThat(served(Fixtures.USER_ACCOUNT_LENIENT)).isEqualTo("InitiateUserRegistration/fallback-valid");
    }

    @Test
    void aNamedFormatIsEnforcedByItsShapeAndAnotherIsNot() {
        assertThat(servedWithDay(Fixtures.KEYWORDS, "2024-01-01")).isEqualTo("postKeywords/fallback-valid");
        assertThat(servedWithDay(Fixtures.KEYWORDS, "01/01/2024")).isEqualTo("postKeywords/fallback-invalid");
        assertThat(servedWithDay(Fixtures.KEYWORDS_UNCHECKED_FORMATS, "01/01/2024"))
                .isEqualTo("postKeywords/fallback-valid");
    }

    /** Which stub answers the full valid request of the keyword corpus, its {@code day} replaced. */
    private static String servedWithDay(Fixtures.Contract contract, String day) {
        GeneratedSuite suite = Suites.of(contract);
        Replay.Request full = suite.request(suite.find("PostKeywords", "success-204-required"));
        ObjectNode body = (ObjectNode) Oracle.JSON.readTree(full.body());
        body.put("day", day);
        Replay.Request changed = new Replay.Request(full.method(), full.pathTemplate(), full.pathParameters(),
                full.query(), full.headers(), full.contentType(), Oracle.JSON.writeValueAsString(body) + "\n");
        try (DoubleServer server = Variations.fallbacksOnly(suite)) {
            Replay.send(server, changed);
            return server.journal().get(0).servedBy();
        }
    }

    private static String served(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        Replay.Request full = suite.request(suite.find("InitiateUserRegistration", "success-202-required"));
        ObjectNode body = (ObjectNode) Oracle.JSON.readTree(full.body());
        body.put("undeclaredMember", "x");
        Replay.Request unknown = new Replay.Request(full.method(), full.pathTemplate(), full.pathParameters(),
                full.query(), full.headers(), full.contentType(), Oracle.JSON.writeValueAsString(body) + "\n");
        try (DoubleServer server = Variations.fallbacksOnly(suite)) {
            Replay.send(server, unknown);
            return server.journal().get(0).servedBy();
        }
    }
}
