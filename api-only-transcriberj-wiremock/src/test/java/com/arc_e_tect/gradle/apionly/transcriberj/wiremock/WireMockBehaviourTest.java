package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the generated stubs rely on WireMock for, checked against the WireMock version the
 * emitter manages rather than assumed: which of several matching stubs answers, what a path,
 * query and body pattern compare against, and what {@code absent()} means for a body.
 */
@DisplayName("The WireMock behaviour the stubs rely on")
class WireMockBehaviourTest {

    private static WireMockServer server;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeAll
    static void start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    @BeforeEach
    void reset() {
        server.resetAll();
        server.stubFor(any(anyUrl()).atPriority(10).willReturn(aResponse().withStatus(299)));
    }

    private static int status(MappingBuilder mapping, String method, String pathAndQuery, String body)
            throws IOException, InterruptedException {
        server.stubFor(mapping.willReturn(aResponse().withStatus(400)));
        return send(method, pathAndQuery, body);
    }

    private static int send(String method, String pathAndQuery, String body) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(server.baseUrl() + pathAndQuery));
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) request.header("Content-Type", "application/json");
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void ofSeveralMatchingStubsAtOnePriorityTheMostRecentlyAddedAnswers() throws Exception {
        server.stubFor(get(urlPathEqualTo("/a")).atPriority(1).willReturn(aResponse().withStatus(401)));
        server.stubFor(get(urlPathEqualTo("/a")).atPriority(1).willReturn(aResponse().withStatus(402)));
        assertThat(send("GET", "/a", null)).isEqualTo(402);
    }

    @Test
    void aPathIsComparedAsItTravelsPercentEncoded() throws Exception {
        assertThat(status(get(urlPathEqualTo("/items/a%20b%2Bc")), "GET", "/items/a%20b%2Bc", null)).isEqualTo(400);
        reset();
        assertThat(status(get(urlPathEqualTo("/items/a b+c")), "GET", "/items/a%20b%2Bc", null)).isEqualTo(299);
    }

    @Test
    void aQueryValueIsComparedDecoded() throws Exception {
        assertThat(status(get(urlPathEqualTo("/q")).withQueryParam("t", equalTo("x y+z&w=v%u/ü")), "GET",
                "/q?t=x%20y%2Bz%26w%3Dv%25u%2F%C3%BC", null)).isEqualTo(400);
    }

    @Test
    void havingExactlyRequiresEveryValueAndNoOther() throws Exception {
        MappingBuilder mapping = get(urlPathEqualTo("/r")).withQueryParam("v", havingExactly("1", "2"));
        assertThat(status(mapping, "GET", "/r?v=1&v=2", null)).isEqualTo(400);
        assertThat(send("GET", "/r?v=1", null)).isEqualTo(299);
        assertThat(send("GET", "/r?v=1&v=2&v=3", null)).isEqualTo(299);
    }

    /** Unlike what one might expect of "exactly": the values' order is not compared. */
    @Test
    void havingExactlyIgnoresTheOrderOfTheValues() throws Exception {
        MappingBuilder mapping = get(urlPathEqualTo("/r")).withQueryParam("v", havingExactly("1", "2"));
        assertThat(status(mapping, "GET", "/r?v=2&v=1", null)).isEqualTo(400);
    }

    @Test
    void anAbsentQueryParameterMustNotBeSentAtAll() throws Exception {
        MappingBuilder mapping = get(urlPathEqualTo("/p")).withQueryParam("page", absent());
        assertThat(status(mapping, "GET", "/p", null)).isEqualTo(400);
        assertThat(send("GET", "/p?page=1", null)).isEqualTo(299);
        assertThat(send("GET", "/p?page=", null)).isEqualTo(299);
    }

    @Test
    void anAbsentBodyMatchesOnlyARequestWithoutOne() throws Exception {
        MappingBuilder mapping = post(urlPathEqualTo("/b")).withRequestBody(absent());
        assertThat(status(mapping, "POST", "/b", null)).isEqualTo(400);
        assertThat(send("POST", "/b", "{}")).isEqualTo(299);
    }

    @Test
    void equalToJsonToleratesWhitespaceButNotExtraMembersOrReorderedArrays() throws Exception {
        MappingBuilder mapping = post(urlPathEqualTo("/j")).withRequestBody(equalToJson("{\"a\":[1,2]}\n", false, false));
        assertThat(status(mapping, "POST", "/j", "{ \"a\" : [ 1, 2 ] }")).isEqualTo(400);
        assertThat(send("POST", "/j", "{\"a\":[2,1]}")).isEqualTo(299);
        assertThat(send("POST", "/j", "{\"a\":[1,2],\"b\":0}")).isEqualTo(299);
        assertThat(send("POST", "/j", "{\"a\":[1,2,3]}")).isEqualTo(299);
    }

    /**
     * {@code 5} and {@code 5.0} are the same number to {@code equalToJson}. A case whose body differed from
     * the valid one only so would collide with it; {@code ExactnessTest} and {@code ValidRequestTest} show
     * that none does.
     */
    @Test
    void equalToJsonTakesAnIntegerAndTheSameNumberWrittenAsADecimalForOne() throws Exception {
        MappingBuilder mapping = post(urlPathEqualTo("/n")).withRequestBody(equalToJson("{\"n\":5}", false, false));
        assertThat(status(mapping, "POST", "/n", "{\"n\":5}")).isEqualTo(400);
        assertThat(send("POST", "/n", "{\"n\":5.0}")).isEqualTo(400);
        assertThat(send("POST", "/n", "{\"n\":5.5}")).isEqualTo(299);
    }
}
