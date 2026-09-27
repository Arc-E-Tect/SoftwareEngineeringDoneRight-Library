package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.4: the fallbacks answer valid requests. Valid requests that are not case requests -- the
 * full body with each optional member removed in turn, every other {@code enum} value, another valid
 * path value, an optional query parameter added -- each judged valid by the independent oracle
 * first, are served by their operation's valid fallback and answered with its first declared
 * {@code 2xx}. No valid request is ever answered with the invalid-request status: neither those,
 * nor the success cases' own.
 */
@DisplayName("T21.4 Fallbacks answer valid requests")
class FallbackValidTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyValidRequestIsServedByTheValidFallback(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        List<Variations.Variation> variations = Variations.valid(suite);
        try (DoubleServer server = Variations.fallbacksOnly(suite)) {
            for (Variations.Variation v : variations) {
                server.forgetRequests();
                Replay.Response response = Replay.send(server, v.request());
                String operation = suite.operationName(v.operation());
                assertThat(server.journal().get(0).servedBy()).as(v.toString()).isEqualTo(operation + "/fallback-valid");
                assertThat(response.status()).as(v.toString()).isEqualTo(firstSuccess(suite, v));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void noValidRequestIsAnsweredWithTheInvalidRequestStatus(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        List<Replay.Request> valid = new ArrayList<>();
        Variations.valid(suite).forEach(v -> valid.add(v.request()));
        suite.cases("SUCCESS").forEach(c -> valid.add(suite.request(c)));
        try (DoubleServer server = Variations.fallbacksOnly(suite)) {
            for (Replay.Request request : valid) {
                assertThat(Replay.send(server, request).status()).as(request.toString()).isNotEqualTo(400);
            }
        }
    }

    /** Each kind of variation is made, somewhere in the fixtures: the test is not vacuous. */
    @Test
    void everyKindOfVariationIsMade() {
        List<String> made = new ArrayList<>();
        for (Fixtures.Contract contract : Fixtures.ALL) {
            Variations.valid(Suites.of(contract)).forEach(v -> made.add(v.how()));
        }
        assertThat(made).anyMatch(h -> h.startsWith("without optional member"));
        assertThat(made).anyMatch(h -> h.contains(" = "));
        assertThat(made).anyMatch(h -> h.startsWith("another path value"));
        assertThat(made).anyMatch(h -> h.startsWith("optional query"));
    }

    /** The first {@code 2xx} the operation declares, read from the contract. */
    private static int firstSuccess(GeneratedSuite suite, Variations.Variation v) {
        var responses = suite.oracle.document.at(v.operation().get("location").stringValue() + "/responses");
        for (String status : responses.propertyNames()) {
            if (status.matches("2[0-9][0-9]")) return Integer.parseInt(status);
        }
        throw new IllegalStateException("No 2xx at " + v);
    }
}
