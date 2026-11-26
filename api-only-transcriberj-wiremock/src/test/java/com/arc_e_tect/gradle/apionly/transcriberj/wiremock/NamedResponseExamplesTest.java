package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A response that declares named examples is answered with them: every stub of its status with the
 * default -- the one {@code x-transcriberj-examples} names, else the first -- in its content type,
 * in both formats. The {@code java} format offers a stub per example besides, which answers any
 * valid request with that example, for a test to choose an outcome. A response without examples is
 * answered with the generated {@code fullBody()}, as before.
 */
@DisplayName("Named response examples")
class NamedResponseExamplesTest {

    private static final String ACCEPTED = "{\"outcome\":\"accepted\",\"findings\":[]}";
    private static final String OUT_OF_STOCK_CHECK =
            "{\"outcome\":\"rejected\",\"findings\":[{\"rule\":\"OUT_OF_STOCK\",\"message\":\"The item is out of stock.\"}]}";
    private static final String CLOSED = "{\"title\":\"The shop is closed.\"}";
    private static final String OUT_OF_STOCK_PROBLEM = "{\"title\":\"The item is out of stock.\"}";

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static GeneratedSuite suite() {
        return Suites.of(Fixtures.EXAMPLES);
    }

    // ------------------------------------------------------------ files

    @Test
    void theFilesAnswerTheSuccessCaseAndTheValidFallbackWithTheFirstExample() throws Exception {
        GeneratedSuite suite = suite();
        try (DoubleServer server = new DoubleServer(suite.files())) {
            HttpResponse<String> exact = post(server, suite.request(suite.find("CheckOrder", "success-200-required")).body());
            HttpResponse<String> other = post(server, "{\"item\":\"a lamp\"}");

            assertAnswer(exact, 200, "application/json", ACCEPTED);
            assertAnswer(other, 200, "application/json", ACCEPTED);
            assertThat(server.journal()).extracting(DoubleServer.Served::servedBy)
                    .containsExactly("CheckOrder/success-200-required", "CheckOrder/fallback-valid");
        }
    }

    @Test
    void anExampleIsWrittenAsABodyFileOfItsOperationAndStatus() {
        Map<String, String> files = Fixtures.Generated.files(suite().files());

        assertThat(files).containsKey("__files/examples/CheckOrder/200-Accepted.json");
        assertThat(Oracle.JSON.readTree(files.get("__files/examples/CheckOrder/200-Accepted.json")))
                .isEqualTo(Oracle.JSON.readTree(ACCEPTED));
        JsonNode valid = Oracle.JSON.readTree(suite().mappingFiles().get("mappings/CheckOrder/fallback-valid.json"));
        assertThat(valid.get("response").get("bodyFileName").stringValue()).isEqualTo("examples/CheckOrder/200-Accepted.json");
        assertThat(valid.get("response").get("headers").get("Content-Type").stringValue()).isEqualTo("application/json");
    }

    @Test
    void aResponseWithoutExamplesIsAnsweredWithItsFullBodyAsBefore() {
        JsonNode valid = Oracle.JSON.readTree(suite().mappingFiles().get("mappings/ListOrders/fallback-valid.json"));

        assertThat(valid.get("response").get("bodyFileName").stringValue()).isEqualTo("examples/OrderListV1.json");
    }

    // ------------------------------------------------------------ java

    @Test
    void theJavaFormatAnswersTheSuccessCaseAndTheValidFallbackWithTheFirstExample() throws Exception {
        GeneratedSuite suite = suite();
        try (DoubleServer server = new DoubleServer()) {
            server.stub(suite.mapping(suite.find("CheckOrder", "success-200-required")));
            suite.fallbacks().forEach(server::stub);

            assertAnswer(post(server, suite.request(suite.find("CheckOrder", "success-200-required")).body()), 200, "application/json", ACCEPTED);
            assertAnswer(post(server, "{\"item\":\"a lamp\"}"), 200, "application/json", ACCEPTED);
        }
    }

    @Test
    void aStubPerExampleAnswersAnyValidRequestWithThatExample() throws Exception {
        GeneratedSuite suite = suite();
        try (DoubleServer server = new DoubleServer()) {
            server.stub(example(suite, "CheckOrder", 200, "OutOfStock"));
            assertAnswer(post(server, "{\"item\":\"a lamp\"}"), 200, "application/json", OUT_OF_STOCK_CHECK);

            server.clear();
            server.stub(example(suite, "CheckOrder", "Closed"));
            assertAnswer(post(server, "{\"item\":\"a lamp\"}"), 422, "application/problem+json", CLOSED);

            server.clear();
            server.stub(example(suite, "CheckOrder", 422, "OutOfStock"));
            assertAnswer(post(server, "{\"item\":\"a lamp\"}"), 422, "application/problem+json", OUT_OF_STOCK_PROBLEM);
            assertThat(server.journal()).extracting(DoubleServer.Served::servedBy)
                    .containsExactly("CheckOrder/example-422-OutOfStock");
        }
    }

    @Test
    void aStubPerExampleMatchesOnlyAValidRequest() throws Exception {
        GeneratedSuite suite = suite();
        try (DoubleServer server = new DoubleServer()) {
            server.stub(example(suite, "CheckOrder", "Closed"));

            HttpResponse<String> invalid = post(server, "{\"item\":\"\"}");

            assertThat(invalid.headers().firstValue(DoubleServer.MARKER)).as("answered by the catch-all").contains("true");
        }
    }

    @Test
    void anExampleStubIsTriedBeforeTheFallbacksAndAfterNoExactStub() {
        MappingBuilder stub = example(suite(), "CheckOrder", "Closed");

        assertThat(stub.build().getPriority()).isEqualTo(1);
        assertThat(stub.build().getName()).isEqualTo("CheckOrder/example-422-Closed");
        assertThat(stub.build().getMetadata().getString("kind")).isEqualTo("EXAMPLE");
    }

    @Test
    void examplesListsEveryExampleOfAnOperationWithItsStatusAndWhichIsTheDefault() throws Exception {
        List<?> examples = (List<?>) suite().stubs().getMethod("examples", String.class).invoke(null, "CheckOrder");

        assertThat(examples).extracting(e -> field(e, "status") + " " + field(e, "name") + " " + field(e, "isDefault")
                        + " " + field(e, "contentType"))
                .containsExactly("200 Accepted true application/json", "200 OutOfStock false application/json",
                        "422 OutOfStock false application/problem+json", "422 Closed true application/problem+json");
        assertThat(Oracle.JSON.readTree((String) field(examples.get(0), "body"))).isEqualTo(Oracle.JSON.readTree(ACCEPTED));
        assertThat((List<?>) suite().stubs().getMethod("examples", String.class).invoke(null, "ListOrders")).isEmpty();
    }

    @Test
    void anUnknownOperationNameOrAnAmbiguousNameThrowsNamingWhatExists() {
        GeneratedSuite suite = suite();

        assertThatThrownBy(() -> example(suite, "PlaceOrder", "Closed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No operation PlaceOrder").hasMessageContaining("CheckOrder, ListOrders");
        assertThatThrownBy(() -> example(suite, "CheckOrder", "Open"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No example Open of CheckOrder")
                .hasMessageContaining("200 Accepted, 200 OutOfStock, 422 OutOfStock, 422 Closed");
        assertThatThrownBy(() -> example(suite, "CheckOrder", 200, "Closed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No example Closed of CheckOrder's 200");
        assertThatThrownBy(() -> example(suite, "CheckOrder", "OutOfStock"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CheckOrder has an example OutOfStock for 200 and 422")
                .hasMessageContaining("example(\"CheckOrder\", status, \"OutOfStock\")");
    }

    // ------------------------------------------------------------ helpers

    private static MappingBuilder example(GeneratedSuite suite, String operation, String name) {
        return invoke(suite, new Class<?>[]{String.class, String.class}, operation, name);
    }

    private static MappingBuilder example(GeneratedSuite suite, String operation, int status, String name) {
        return invoke(suite, new Class<?>[]{String.class, int.class, String.class}, operation, status, name);
    }

    private static MappingBuilder invoke(GeneratedSuite suite, Class<?>[] types, Object... arguments) {
        try {
            return (MappingBuilder) suite.stubs().getMethod("example", types).invoke(null, arguments);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object field(Object record, String accessor) {
        try {
            return record.getClass().getMethod(accessor).invoke(record);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static HttpResponse<String> post(DoubleServer server, String body) throws IOException, InterruptedException {
        return HTTP.send(HttpRequest.newBuilder(URI.create(server.baseUrl() + "/orders/checks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertAnswer(HttpResponse<String> response, int status, String contentType, String body) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).contains(contentType);
        assertThat(Oracle.JSON.readTree(response.body())).isEqualTo(Oracle.JSON.readTree(body));
    }
}
