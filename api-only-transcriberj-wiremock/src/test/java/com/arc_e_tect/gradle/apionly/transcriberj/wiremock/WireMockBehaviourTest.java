package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock.JsonSchemaVersion;
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
import static com.github.tomakehurst.wiremock.client.WireMock.and;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonSchema;
import static com.github.tomakehurst.wiremock.client.WireMock.not;
import static com.github.tomakehurst.wiremock.client.WireMock.or;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
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

    // --------------------------------------------- what the fallbacks and the Accept rule rely on

    private static int send(String method, String pathAndQuery, String body, String... headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(server.baseUrl() + pathAndQuery));
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        for (int i = 0; i < headers.length; i += 2) request.header(headers[i], headers[i + 1]);
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static void answer(MappingBuilder mapping) {
        server.stubFor(mapping.willReturn(aResponse().withStatus(400)));
    }

    @Test
    void absentOrMatchingMatchesAnAbsentHeaderAndAMatchingOneOnly() throws Exception {
        answer(get(urlPathEqualTo("/h")).withHeader("Accept", or(absent(), matching("(?i)application/json"))));
        assertThat(send("GET", "/h", null)).as("absent").isEqualTo(400);
        assertThat(send("GET", "/h", null, "Accept", "application/json")).as("matching").isEqualTo(400);
        assertThat(send("GET", "/h", null, "Accept", "text/csv")).as("other").isEqualTo(299);
    }

    /** Why "present and not acceptable" is written {@code and(matching(".*"), not(...))}. */
    @Test
    void aNegatedPatternAloneAlsoMatchesAnAbsentHeader() throws Exception {
        answer(get(urlPathEqualTo("/n")).withHeader("Accept", not(matching("(?i)application/json"))));
        assertThat(send("GET", "/n", null)).as("absent").isEqualTo(400);
        reset();
        answer(get(urlPathEqualTo("/a")).withHeader("Accept", and(matching(".*"), not(matching("(?i)application/json")))));
        assertThat(send("GET", "/a", null)).as("absent").isEqualTo(299);
        assertThat(send("GET", "/a", null, "Accept", "text/csv")).as("other").isEqualTo(400);
        assertThat(send("GET", "/a", null, "Accept", "application/json")).as("matching").isEqualTo(299);
    }

    /** Why a fallback does not check a path value that travels percent-encoded. */
    @Test
    void aPathParameterIsComparedAsItTravelsPercentEncoded() throws Exception {
        answer(get(urlPathTemplate("/users/{id}")).withPathParam("id", matching("[a-z ]+")));
        assertThat(send("GET", "/users/abc", null)).isEqualTo(400);
        assertThat(send("GET", "/users/a%20b", null)).as("decoded, it would match").isEqualTo(299);
        assertThat(send("GET", "/users/123", null)).isEqualTo(299);
        assertThat(send("GET", "/users/abc/def", null)).as("one segment only").isEqualTo(299);
    }

    @Test
    void aLiteralTemplateMatchesItsPathOnly() throws Exception {
        answer(get(urlPathTemplate("/users/me")));
        assertThat(send("GET", "/users/me", null)).isEqualTo(400);
        assertThat(send("GET", "/users/you", null)).isEqualTo(299);
    }

    private static final String SCHEMA = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
            + "\"type\":\"object\",\"required\":[\"e\"],\"properties\":{\"e\":{\"type\":\"string\","
            + "\"format\":\"email\"}},\"additionalProperties\":false}";

    /** What the valid fallback's body check asserts, and what it does not: {@code format}. */
    @Test
    void matchesJsonSchemaAssertsEveryKeywordButFormat() throws Exception {
        answer(post(urlPathEqualTo("/s")).withRequestBody(matchingJsonSchema(SCHEMA, JsonSchemaVersion.V202012)));
        assertThat(send("POST", "/s", "{\"e\":\"a@b.c\"}")).as("valid").isEqualTo(400);
        assertThat(send("POST", "/s", "{\"e\":\"a@b.c\",\"x\":1}")).as("unknown member").isEqualTo(299);
        assertThat(send("POST", "/s", "{}")).as("missing member").isEqualTo(299);
        assertThat(send("POST", "/s", null)).as("no body").isEqualTo(299);
        assertThat(send("POST", "/s", "{\"e\":\"not an email\"}")).as("format, not asserted").isEqualTo(400);
    }

    @Test
    void anAbsentOrValidBodyMatchesNoBodyAndAValidOneOnly() throws Exception {
        answer(post(urlPathEqualTo("/o")).withRequestBody(or(absent(),
                matchingJsonSchema(SCHEMA, JsonSchemaVersion.V202012))));
        assertThat(send("POST", "/o", null)).isEqualTo(400);
        assertThat(send("POST", "/o", "{\"e\":\"a@b.c\"}")).isEqualTo(400);
        assertThat(send("POST", "/o", "{}")).isEqualTo(299);
    }

    /** Why the tests serve every stub from a copy of the files directory. */
    @Test
    void resettingAServerDeletesTheMappingFilesOfItsWorkingDirectory(@org.junit.jupiter.api.io.TempDir java.nio.file.Path root)
            throws Exception {
        java.nio.file.Path mapping = java.nio.file.Files.createDirectories(root.resolve("mappings")).resolve("m.json");
        java.nio.file.Files.writeString(mapping, "{\"request\":{\"method\":\"GET\",\"urlPath\":\"/f\"},"
                + "\"response\":{\"status\":204}}");
        WireMockServer files = new WireMockServer(options().dynamicPort().usingFilesUnderDirectory(root.toString()));
        files.start();
        try {
            assertThat(files.getStubMappings()).hasSize(1);
            files.resetMappings();
            assertThat(mapping).doesNotExist();
        } finally {
            files.stop();
        }
    }
}
