package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.7: values that need encoding match verbatim. Every case of the transmission contract -- whose
 * path, query, header and body values hold a space, {@code +}, {@code &}, {@code =} and {@code %},
 * the query a {@code /}, and all but the header a non-ASCII character -- sent as the REST Docs
 * emitter's rendered test sends it, is served by its own stub.
 *
 * <p>A repeated query parameter matches only with all of its values and no other. The core
 * gives no case one -- it generates no value for an array query parameter yet -- so the case is
 * built here, from the generated records. Its values' order is not compared: WireMock's
 * {@code havingExactly} ignores it, as {@link WireMockBehaviourTest} records, so a request with the
 * values in another order matches too.
 */
@DisplayName("T15.7 Verbatim values match")
class VerbatimValuesTest {

    @Test
    void everyTransmissionCaseIsServedByItsOwnStub() {
        GeneratedSuite suite = Suites.of(Fixtures.TRANSMISSION);
        JsonNode request = suite.find("body-count-minimum").json().get("request");
        assertThat(request.get("pathParameters").get(0).stringValue()).contains(" ", "+", "&", "=", "%", "é");
        assertThat(request.get("query").toString()).contains(" ", "+", "&", "=", "%", "/", "ü");
        assertThat(request.get("headers").toString()).contains(" ", "+", "&", "=", "%");
        assertThat(request.get("body").toString()).contains(" ", "+", "&", "=", "%", "ñ");

        GeneratedSuite.Run run;
        try (DoubleServer server = new DoubleServer()) {
            run = suite.run(server);
        }
        EverythingPassesTest.assertServedByOwnStubs(suite, run);
    }

    @Test
    void aRepeatedQueryParameterMatchesOnlyWithAllItsValues() throws Exception {
        GeneratedSuite suite = Suites.of(Fixtures.TRANSMISSION);
        String base = suite.settings.basePackage();
        Object repeated = invalid(suite, base, List.of("v", "1 &", "v", "2/é", "other", "x"));
        try (DoubleServer server = new DoubleServer()) {
            server.stub(suite.mapping(repeated));
            String own = "getRepeated/query-v-maxItems";

            assertThat(send(server, List.of("v", "1 &", "v", "2/é", "other", "x"))).isEqualTo(own);
            assertThat(send(server, List.of("v", "1 &", "other", "x"))).isEqualTo(DoubleServer.CATCH_ALL);
            assertThat(send(server, List.of("v", "1 &", "v", "2/é", "v", "3", "other", "x")))
                    .isEqualTo(DoubleServer.CATCH_ALL);
            assertThat(send(server, List.of("v", "1 &", "v", "2/é"))).isEqualTo(DoubleServer.CATCH_ALL);
            assertThat(send(server, List.of("v", "2/é", "v", "1 &", "other", "x"))).as("another order").isEqualTo(own);
        }
    }

    /** Sends a GET of {@code /repeated} with a query, as names and values in turn, saying which stub answered. */
    private static String send(DoubleServer server, List<String> query) {
        server.forgetRequests();
        Replay.send(server, new Replay.Request("GET", "/repeated", List.of(), pairs(query), List.of(), null, null));
        return server.journal().get(0).servedBy();
    }

    private static List<String[]> pairs(List<String> flat) {
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < flat.size(); i += 2) out.add(new String[]{flat.get(i), flat.get(i + 1)});
        return out;
    }

    /** A case of a GET of {@code /repeated} whose query repeats a name, as the generated records hold it. */
    private static Object invalid(GeneratedSuite suite, String base, List<String> query) throws Exception {
        Class<?> pair = suite.type(base + ".ContractRequest$Pair");
        Constructor<?> newPair = pair.getConstructor(String.class, String.class);
        List<Object> pairs = new ArrayList<>();
        for (int i = 0; i < query.size(); i += 2) pairs.add(newPair.newInstance(query.get(i), query.get(i + 1)));
        Class<?> request = suite.type(base + ".ContractRequest");
        Object contractRequest = request.getConstructor(String.class, String.class, List.class, List.class, List.class,
                String.class, String.class).newInstance("GET", "/repeated", List.of(), pairs, List.of(), null, null);
        Class<?> invalid = suite.type(base + ".InvalidRequestCase");
        return invalid.getConstructors()[0].newInstance("query-v-maxItems", "`v` has too many items", "query", "v",
                null, "maxItems", contractRequest, 400, List.of(), null, true, "getRepeated", List.of("v", "other"),
                List.of());
    }
}
