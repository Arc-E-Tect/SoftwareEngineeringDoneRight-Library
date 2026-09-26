package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.5: with every mapping registered, each operation's valid requests -- {@code requiredRequest()},
 * {@code fullRequest()} and, where the body is optional, {@code noBodyRequest()} -- reach the
 * catch-all, alone and with a project's extra headers: a generated stub never answers a valid
 * request, which the hand-written happy-path tests sharing the server send.
 */
@DisplayName("T15.5 The valid request matches nothing")
class ValidRequestTest {

    static final Map<String, String> PROJECT_HEADERS = Map.of("Accept", "application/json",
            "Authorization", "Bearer abc.def.ghi", "X-Forwarded-For", "203.0.113.7");

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyValidRequestReachesTheCatchAll(Fixtures.Contract contract) throws Exception {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            suite.cases.forEach(c -> server.stub(suite.mapping(c)));
            for (JsonNode operation : suite.operations()) {
                List<Object> valid = validRequests(suite, operation);
                assertThat(valid).as(operation.get("class").stringValue()).isNotEmpty();
                for (Object request : valid) {
                    for (Map<String, String> extra : List.of(Map.<String, String>of(), PROJECT_HEADERS)) {
                        Replay.Response response = Replay.send(server, Replay.Request.of(request), extra);
                        assertThat(response.catchAll()).as("%s %s", request, extra).isTrue();
                    }
                }
            }
            assertThat(server.journal()).allSatisfy(s -> assertThat(s.servedBy()).isEqualTo(DoubleServer.CATCH_ALL));
        }
    }

    /** An operation's valid requests; one the core degrades, and so cannot build, is left out. */
    private static List<Object> validRequests(GeneratedSuite suite, JsonNode operation) throws Exception {
        String casesClass = operation.get("class").stringValue();
        Class<?> type = suite.type(suite.settings.basePackage() + "."
                + casesClass.substring(0, casesClass.length() - "InvalidRequests".length()) + "Operation");
        List<Object> out = new ArrayList<>();
        for (String name : List.of("requiredRequest", "fullRequest", "noBodyRequest")) {
            Method method;
            try {
                method = type.getMethod(name);
            } catch (NoSuchMethodException e) {
                continue;
            }
            try {
                out.add(method.invoke(null));
            } catch (InvocationTargetException e) {
                assertThat(e.getCause()).isInstanceOf(UnsupportedOperationException.class);
            }
        }
        return out;
    }
}
