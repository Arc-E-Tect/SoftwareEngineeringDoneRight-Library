package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.7: literal path segments win over templates. With every stub of a contract that has both
 * {@code /users/me} and {@code /users/{id}} loaded, a request to either is answered by its own
 * operation's stubs, exact and fallback -- although {@code me} is a valid {@code id} -- because the
 * more specific template's fallbacks have the higher priority.
 */
@DisplayName("T21.7 Path precedence")
class PathPrecedenceTest {

    @Test
    void eachPathIsAnsweredByItsOwnOperationsStubs() {
        GeneratedSuite suite = Suites.of(Fixtures.PRECEDENCE);
        Replay.Request me = suite.request(suite.find("GetMe", "success-200-required"));
        Replay.Request user = suite.request(suite.find("GetUser", "success-200-required"));
        try (DoubleServer server = new DoubleServer(suite.files())) {
            assertThat(served(server, me)).isEqualTo("getMe/success-200-required");
            assertThat(served(server, Replay.withHeader(me, "X-Session", "another"))).isEqualTo("getMe/fallback-valid");
            assertThat(served(server, Replay.withHeader(me, "X-Session", "ab"))).isEqualTo("getMe/fallback-invalid");
            assertThat(served(server, withoutHeaders(me))).as("the case that leaves the header out")
                    .isEqualTo("getMe/header-X-Session-required");

            assertThat(served(server, user)).isEqualTo("getUser/success-200-required");
            assertThat(served(server, path(user, "other"))).isEqualTo("getUser/fallback-valid");
            assertThat(served(server, path(user, "Me9"))).isEqualTo("getUser/fallback-invalid");
        }
    }

    @Test
    void theMoreSpecificTemplatesFallbacksComeFirst() {
        GeneratedSuite suite = Suites.of(Fixtures.PRECEDENCE);
        Map<String, com.github.tomakehurst.wiremock.stubbing.StubMapping> stubs = suite.stubMappings();
        for (String fallback : List.of("valid", "invalid")) {
            assertThat(stubs.get("getMe/fallback-" + fallback).getPriority())
                    .isLessThan(stubs.get("getUser/fallback-valid").getPriority());
        }
        assertThat(Plan.specificity("/users/me", "/users/{id}")).isNegative();
        assertThat(Plan.specificity("/users/{id}", "/users/me")).isPositive();
        assertThat(Plan.specificity("/a/{x}/c", "/a/{x}/{y}")).isNegative();
        assertThat(Plan.specificity("/a", "/a/b")).isNegative();
        assertThat(Plan.specificity("/a/b", "/a/c")).isNegative();
    }

    private static String served(DoubleServer server, Replay.Request request) {
        server.forgetRequests();
        Replay.send(server, request);
        return server.journal().get(0).servedBy();
    }

    private static Replay.Request path(Replay.Request request, String value) {
        return new Replay.Request(request.method(), request.pathTemplate(), List.of(value), request.query(),
                request.headers(), request.contentType(), request.body());
    }

    private static Replay.Request withoutHeaders(Replay.Request request) {
        return new Replay.Request(request.method(), request.pathTemplate(), request.pathParameters(), request.query(),
                List.of(), request.contentType(), request.body());
    }
}
