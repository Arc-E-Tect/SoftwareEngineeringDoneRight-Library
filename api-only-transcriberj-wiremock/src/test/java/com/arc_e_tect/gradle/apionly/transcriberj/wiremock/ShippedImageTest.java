package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.2: the shipped image. The archive a consumer build packages, unpacked beside the README's
 * {@code Dockerfile}, builds an image of the official WireMock image of the version the emitter is
 * tested with; run with Testcontainers, it answers every case's request, replayed with the JDK
 * {@code HttpClient}, with the case's status, content type and body -- each served by the stub
 * named after its case, as the container's request journal shows. Docker must be running.
 */
@DisplayName("T21.2 The shipped image")
class ShippedImageTest {

    @Test
    void theImageAnswersEveryCaseAsTheContractSays() throws Exception {
        Path context = unpack(Consumers.firstArchive());
        Files.writeString(context.resolve("Dockerfile"), dockerfile());
        ImageFromDockerfile image = new ImageFromDockerfile("apionly/wiremock-stubs-test", true)
                .withFileFromPath(".", context)
                .withBuildArg("WIREMOCK_VERSION", System.getProperty("wiremock.version"));
        GeneratedSuite suite = Suites.of(Fixtures.USER_ACCOUNT);
        try (GenericContainer<?> stubs = new GenericContainer<>(image).withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/__admin/health").forPort(8080))) {
            stubs.start();
            String base = "http://" + stubs.getHost() + ":" + stubs.getMappedPort(8080);
            for (GeneratedSuite.Case c : suite.cases) {
                Replay.Response response = Replay.send(base, suite.request(c), Map.of());
                JsonNode mapping = Oracle.JSON.readTree(Files.readString(context.resolve(suite.mappingFile(c))));
                JsonNode expected = mapping.get("response");
                assertThat(response.status()).as(c.toString()).isEqualTo(c.status());
                assertThat(response.contentType()).as(c.toString())
                        .isEqualTo(expected.path("headers").path("Content-Type").stringValue(null));
                String body = expected.has("bodyFileName") ? Files.readString(context.resolve("__files")
                        .resolve(expected.get("bodyFileName").stringValue())) : "";
                assertThat(response.body()).as(c.toString()).isEqualTo(body);
            }
            assertThat(servedBy(base)).containsExactlyElementsOf(suite.cases.stream().map(suite::mappingName).toList());
        }
    }

    /** The README's {@code Dockerfile}, between its tags. */
    static String dockerfile() throws IOException {
        String readme = Files.readString(Path.of("README.adoc"));
        String tagged = readme.substring(readme.indexOf("# tag::dockerfile[]"), readme.indexOf("# end::dockerfile[]"));
        return tagged.substring(tagged.indexOf('\n') + 1);
    }

    /** The name of the stub that served each request, in the order received, from the container's journal. */
    private static List<String> servedBy(String base) throws Exception {
        HttpResponse<String> journal = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(base + "/__admin/requests")).build(),
                HttpResponse.BodyHandlers.ofString());
        List<String> out = new ArrayList<>();
        for (JsonNode request : Oracle.JSON.readTree(journal.body()).get("requests")) {
            out.add(request.path("stubMapping").path("name").stringValue(null));
        }
        Collections.reverse(out);
        return out;
    }

    private static Path unpack(byte[] archive) {
        try {
            Path root = Files.createTempDirectory("wiremock-image");
            try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
                for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                    Path to = root.resolve(e.getName());
                    if (e.isDirectory()) {
                        Files.createDirectories(to);
                    } else {
                        Files.createDirectories(to.getParent());
                        Files.write(to, zip.readAllBytes());
                    }
                }
            }
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
