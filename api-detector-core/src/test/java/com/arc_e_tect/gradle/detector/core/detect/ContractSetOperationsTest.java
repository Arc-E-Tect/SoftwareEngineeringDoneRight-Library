package com.arc_e_tect.gradle.detector.core.detect;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the generic {@link ContractSetOperations#difference(List, List)} correctly
 * reproduces both {@code ShadowApiFinder.findShadows} (endpoints on the left, described endpoints
 * on the right) and {@code MirageApiFinder.findMirages} (described endpoints on the left,
 * endpoints on the right), and that {@link ContractSetOperations#intersection(List, List)} is its
 * exact complement.
 */
@DisplayName("ContractSetOperations")
class ContractSetOperationsTest {

    // --- difference(): Shadow direction - difference(endpoints, described) ---

    @Test
    @DisplayName("difference(): returns no shadow endpoints when every endpoint is described")
    void differenceReturnsNoShadowsWhenEveryEndpointIsDescribed() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));

        assertThat(ContractSetOperations.difference(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("difference(): flags an endpoint whose path is not described at all")
    void differenceFlagsUndescribedPath() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/accounts"));

        assertThat(ContractSetOperations.difference(endpoints, described))
                .extracting(Endpoint::path).containsExactly("/users");
    }

    @Test
    @DisplayName("difference(): flags an endpoint whose path matches but verb does not")
    void differenceFlagsMismatchedVerbForShadowDirection() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.DELETE, "/users/{id}"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users/{id}"));

        assertThat(ContractSetOperations.difference(endpoints, described)).hasSize(1);
    }

    @Test
    @DisplayName("difference(): matches path variables regardless of their name")
    void differenceMatchesPathVariablesRegardlessOfNameForShadowDirection() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users/{id}"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users/{userId}"));

        assertThat(ContractSetOperations.difference(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("difference(): an ANY-verb endpoint is described as soon as any verb is documented for the path")
    void differenceAnyVerbEndpointDescribedByAnyDocumentedVerb() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.POST, "/users/{id}/summary"));

        assertThat(ContractSetOperations.difference(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("difference(): an ANY-verb endpoint is a shadow when no verb is documented for the path")
    void differenceAnyVerbEndpointIsShadowWhenPathUndescribed() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/other"));

        assertThat(ContractSetOperations.difference(endpoints, described)).hasSize(1);
    }

    // --- difference(): Mirage direction - difference(described, endpoints) ---

    @Test
    @DisplayName("difference(): returns no mirage endpoints when every described endpoint is implemented")
    void differenceReturnsNoMiragesWhenEveryEndpointIsImplemented() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));

        assertThat(ContractSetOperations.difference(described, endpoints)).isEmpty();
    }

    @Test
    @DisplayName("difference(): flags a described endpoint whose path is not implemented at all")
    void differenceFlagsUnimplementedPath() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/accounts"));

        assertThat(ContractSetOperations.difference(described, endpoints))
                .extracting(DescribedEndpoint::path).containsExactly("/users");
    }

    @Test
    @DisplayName("difference(): flags a described endpoint whose path matches but verb does not")
    void differenceFlagsMismatchedVerbForMirageDirection() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.DELETE, "/users/{id}"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users/{id}"));

        assertThat(ContractSetOperations.difference(described, endpoints)).hasSize(1);
    }

    @Test
    @DisplayName("difference(): matches path variables regardless of their name")
    void differenceMatchesPathVariablesRegardlessOfNameForMirageDirection() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users/{userId}"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users/{id}"));

        assertThat(ContractSetOperations.difference(described, endpoints)).isEmpty();
    }

    @Test
    @DisplayName("difference(): a described endpoint is implemented by a matching ANY-verb controller endpoint")
    void differenceDescribedEndpointImplementedByAnyVerbEndpoint() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.POST, "/users/{id}/summary"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));

        assertThat(ContractSetOperations.difference(described, endpoints)).isEmpty();
    }

    @Test
    @DisplayName("difference(): a described endpoint is a mirage when no controller endpoint matches its path")
    void differenceDescribedEndpointIsMirageWhenPathUnimplemented() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users/{id}/summary"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/other"));

        assertThat(ContractSetOperations.difference(described, endpoints)).hasSize(1);
    }

    // --- intersection(): the exact complement of difference() ---

    @Test
    @DisplayName("intersection(): returns the endpoint when it is described, for the Shadow direction")
    void intersectionReturnsEndpointWhenDescribedForShadowDirection() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));

        assertThat(ContractSetOperations.intersection(endpoints, described))
                .extracting(Endpoint::path).containsExactly("/users");
    }

    @Test
    @DisplayName("intersection(): is empty when the endpoint is not described, for the Shadow direction")
    void intersectionEmptyWhenUndescribedForShadowDirection() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/accounts"));

        assertThat(ContractSetOperations.intersection(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("intersection(): returns the described endpoint when it is implemented, for the Mirage direction")
    void intersectionReturnsDescribedWhenImplementedForMirageDirection() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));

        assertThat(ContractSetOperations.intersection(described, endpoints))
                .extracting(DescribedEndpoint::path).containsExactly("/users");
    }

    @Test
    @DisplayName("intersection(): is empty when the described endpoint is not implemented, for the Mirage direction")
    void intersectionEmptyWhenUnimplementedForMirageDirection() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/accounts"));

        assertThat(ContractSetOperations.intersection(described, endpoints)).isEmpty();
    }

    @Test
    @DisplayName("intersection(): honors an ANY-verb endpoint against any documented verb for the same path")
    void intersectionHonorsAnyVerbEndpoint() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.POST, "/users/{id}/summary"));

        assertThat(ContractSetOperations.intersection(endpoints, described)).hasSize(1);
    }

    private static Endpoint endpoint(HttpVerb verb, String path) {
        return new Endpoint(verb, path, "com.example.FooController", "foo()", "FooController.java", 1);
    }

    private static DescribedEndpoint described(HttpVerb verb, String path) {
        return new DescribedEndpoint(verb, path, "op", List.of());
    }
}
