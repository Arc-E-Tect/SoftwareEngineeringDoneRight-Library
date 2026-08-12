package com.arc_e_tect.gradle.detector.core.openapi;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("OpenApiEndpointCollector")
class OpenApiEndpointCollectorTest {

    private final OpenApiEndpointCollector collector = new OpenApiEndpointCollector();

    @Test
    @DisplayName("collects every verb + path pair from a single-file document")
    void collectsFromSingleFile() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/single-file/openapi.yaml"));

        assertThat(endpoints)
                .extracting(DescribedEndpoint::verb, DescribedEndpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/users"),
                        tuple(HttpVerb.POST, "/users"),
                        tuple(HttpVerb.GET, "/users/{id}"),
                        tuple(HttpVerb.DELETE, "/users/{id}"));
    }

    @Test
    @DisplayName("collects the operationId of each operation")
    void collectsOperationId() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/single-file/openapi.yaml"));

        assertThat(endpoints)
                .filteredOn(e -> e.verb() == HttpVerb.GET && e.path().equals("/users"))
                .extracting(DescribedEndpoint::operationId)
                .containsExactly("listUsers");
    }

    @Test
    @DisplayName("collects the tags of each operation, in document order")
    void collectsTags() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/single-file/openapi.yaml"));

        assertThat(endpoints)
                .filteredOn(e -> e.verb() == HttpVerb.GET && e.path().equals("/users/{id}"))
                .extracting(DescribedEndpoint::tags)
                .containsExactly(List.of("Users", "Admin"));
    }

    @Test
    @DisplayName("returns an empty tag list for an operation with no tags")
    void returnsEmptyTagListWhenUntagged() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/single-file/openapi.yaml"));

        assertThat(endpoints)
                .filteredOn(e -> e.verb() == HttpVerb.POST && e.path().equals("/users"))
                .extracting(DescribedEndpoint::tags)
                .containsExactly(List.of());
    }

    @Test
    @DisplayName("follows a relative $ref to an external path-item document")
    void followsRelativeRef() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/with-ref/openapi.yaml"));

        assertThat(endpoints)
                .extracting(DescribedEndpoint::verb, DescribedEndpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/users"),
                        tuple(HttpVerb.POST, "/users"),
                        tuple(HttpVerb.GET, "/users/{id}"));
    }

    @Test
    @DisplayName("collects the operationId and tags of an operation reached through a $ref")
    void collectsOperationIdAndTagsThroughRef() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/with-ref/openapi.yaml"));

        assertThat(endpoints)
                .filteredOn(e -> e.verb() == HttpVerb.GET && e.path().equals("/users"))
                .extracting(DescribedEndpoint::operationId, DescribedEndpoint::tags)
                .containsExactly(tuple("listUsers", List.of("Users")));
    }

    @Test
    @DisplayName("collects every verb + path pair from a single-file OpenAPI 3.2 document")
    void collectsFromSingleFile32() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/single-file-3-2/openapi.yaml"));

        assertThat(endpoints)
                .extracting(DescribedEndpoint::verb, DescribedEndpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/items"),
                        tuple(HttpVerb.GET, "/items/{id}"));
    }

    @Test
    @DisplayName("follows a relative $ref from an OpenAPI 3.2 root document")
    void followsRelativeRef32() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/with-ref-3-2/openapi.yaml"));

        assertThat(endpoints)
                .extracting(DescribedEndpoint::verb, DescribedEndpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/items"),
                        tuple(HttpVerb.POST, "/items"),
                        tuple(HttpVerb.GET, "/items/{id}"));
    }

    @Test
    @DisplayName("throws when the document cannot be parsed")
    void throwsForUnparsableDocument(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir)
            throws Exception {
        File garbage = new File(tempDir.toFile(), "not-openapi.yaml");
        java.nio.file.Files.writeString(garbage.toPath(), "not: [valid, openapi: {{{");

        assertThatThrownBy(() -> collector.collect(garbage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed to parse OpenAPI document");
    }

    private static File resource(String name) {
        URL url = OpenApiEndpointCollectorTest.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException("Resource not found on classpath: " + name);
        }
        return new File(url.getFile());
    }
}
